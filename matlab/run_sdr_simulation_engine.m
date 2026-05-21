function run_sdr_simulation_engine( ...
    modType, channelType, numBits, snrDb, ...
    sps, rolloff, filterSpan, maxDopplerShift, ...
    kFactor, impulseProb, impulseAmp, outDir, ...
    modeType, sdrDevice, ipAddress, ...
    centerFrequency, txGain, rxGain, radioID) 
% MATLAB backend for Java GUI through MATLAB Engine API

    close all;
    clc;
    rng('default');

    if ~exist(outDir, 'dir')
        mkdir(outDir);
    end

    %% Основні параметри
    cfg.modType = char(modType);
    cfg.channelType = char(channelType);
    cfg.numBits = double(numBits);
    cfg.sps = double(sps);
    cfg.rolloff = double(rolloff);
    cfg.filterSpan = double(filterSpan);
    cfg.snrDb = double(snrDb);
    cfg.snrSweep = 0:2:24;
    cfg.impulseProb = double(impulseProb);
    cfg.impulseAmp  = double(impulseAmp);
    cfg.maxDopplerShift = double(maxDopplerShift);
    cfg.kFactor = double(kFactor);
    cfg.fs = 1e6;
    cfg.showEyeDiagram = true;

    %% Параметри модуляції
    [M, bitsPerSym] = getModulationOrder(cfg.modType);

    numSymbols = floor(cfg.numBits / bitsPerSym);
    numBitsUsed = numSymbols * bitsPerSym;

    txBits = randi([0 1], numBitsUsed, 1);
    txSymbols = modulateBits(txBits, cfg.modType, M, bitsPerSym);

    %% Формування сигналу
    rrc = rcosdesign(cfg.rolloff, cfg.filterSpan, cfg.sps, 'sqrt');
    txWaveform = upfirdn(txSymbols, rrc, cfg.sps, 1);
    txWaveform = txWaveform / rms(txWaveform);

    %% Канал
    %% SDR / Simulation mode

if strcmpi(modeType, 'SDR')
    try
        disp('=== SDR MODE ENABLED ===');
        
        %% Pluto SDR

        if strcmpi(sdrDevice, 'Pluto')
            tx = sdrtx('Pluto');
            rx = sdrrx('Pluto');
            tx.RadioID = char(radioID);
            rx.RadioID = char(radioID);

        %% AD9361
        elseif strcmpi(sdrDevice, 'AD9361')

            tx = sdrtx('Pluto', ...
                'RadioID', char(ipAddress));
            rx = sdrrx('Pluto', ...
                'RadioID', char(ipAddress));
        else
            error('Unsupported SDR device.');

        end

        %% SDR parameters

        tx.CenterFrequency = double(centerFrequency);
        rx.CenterFrequency = double(centerFrequency);
        tx.BasebandSampleRate = cfg.fs;
        rx.BasebandSampleRate = cfg.fs;
        tx.Gain = double(txGain);
        rx.GainSource = 'Manual';
        rx.Gain = double(rxGain);
        rx.OutputDataType = 'double';

        %% SDR transmission
        transmitRepeat(tx, txWaveform);
        pause(0.5);
        rxWaveform = rx();
        release(tx);
        release(rx);
        rxWaveform = rxWaveform(:);
        chanInfo.name = 'Real SDR Link';
    catch ME
        warning(ME.message);
        disp('SDR unavailable. Switching to simulation.');
        [rxWaveform, chanInfo] = applyChannel(txWaveform, cfg);
    end
else

    %% Simulation mode
  [rxWaveform, chanInfo] = applyChannel(txWaveform, cfg);

end

    %% Прийом
    rxMatched = upfirdn(rxWaveform, rrc, 1, 1);
    totalDelay = cfg.filterSpan * cfg.sps;

    if length(rxMatched) <= totalDelay
        error('Сигнал надто короткий після matched filtering.');
    end

    rxMatchedSync = rxMatched(totalDelay + 1:end);
    rxSymbols = rxMatchedSync(1:cfg.sps:end);
    rxSymbols = rxSymbols(1:min(length(rxSymbols), length(txSymbols)));
    txSymbolsRef = txSymbols(1:length(rxSymbols));

    %% Демодуляція
    rxBits = demodulateSymbols(rxSymbols, cfg.modType, M, bitsPerSym);

    minBitLen = min(length(txBits), length(rxBits));
    txBitsRef = txBits(1:minBitLen);
    rxBitsRef = rxBits(1:minBitLen);

    %% Оцінка якості
    [numErrors, ber] = biterr(txBitsRef, rxBitsRef);

    alpha = (txSymbolsRef' * rxSymbols) / (txSymbolsRef' * txSymbolsRef + 1e-15);
    txAligned = alpha * txSymbolsRef;
    errVec = rxSymbols - txAligned;

    evmRms = 100 * rms(errVec) / sqrt(mean(abs(txAligned).^2) + 1e-15);
    snrEst = 10 * log10(mean(abs(txAligned).^2) / (mean(abs(errVec).^2) + 1e-15));

    %% BER залежно від SNR
    berCurve = zeros(size(cfg.snrSweep));

    for k = 1:length(cfg.snrSweep)
        cfgTmp = cfg;
        cfgTmp.snrDb = cfg.snrSweep(k);

        [rxWaveTmp, ~] = applyChannel(txWaveform, cfgTmp);
        rxMatchedTmp = upfirdn(rxWaveTmp, rrc, 1, 1);

        if length(rxMatchedTmp) <= totalDelay
            berCurve(k) = NaN;
            continue;
        end

        rxMatchedTmp = rxMatchedTmp(totalDelay + 1:end);
        rxSymTmp = rxMatchedTmp(1:cfg.sps:end);
        rxSymTmp = rxSymTmp(1:min(length(rxSymTmp), length(txSymbols)));

        rxBitsTmp = demodulateSymbols(rxSymTmp, cfg.modType, M, bitsPerSym);

        minLenTmp = min(length(txBits), length(rxBitsTmp));
        [~, berCurve(k)] = biterr(txBits(1:minLenTmp), rxBitsTmp(1:minLenTmp));
    end

    %% Побудова та збереження графіків
    f1 = figure('Visible', 'off', 'Color', 'w');
    plot(real(rxSymbols), imag(rxSymbols), '.', 'MarkerSize', 6);
    hold on;
    plot(real(txSymbolsRef), imag(txSymbolsRef), 'ro', 'MarkerSize', 4, 'LineWidth', 1.0);
    grid on;
    axis equal;
    xlabel('In-Phase');
    ylabel('Quadrature');
    title(sprintf('Constellation: %s, Channel: %s, SNR = %.1f dB', ...
        cfg.modType, cfg.channelType, cfg.snrDb));
    legend('Received symbols', 'Ideal symbols', 'Location', 'best');
    exportgraphics(f1, fullfile(outDir, 'constellation.png'), 'Resolution', 200);
    close(f1);

       % Eye diagram без окремого MATLAB-вікна
    f2 = figure('Visible', 'off', 'Color', 'w');
    manualEyeDiagram(real(rxMatchedSync), cfg.sps);
    title(sprintf('Eye Diagram (%s)', cfg.modType));
    exportgraphics(f2, fullfile(outDir, 'eye_diagram.png'), 'Resolution', 200);
    close(f2);
    nfft = 4096;
    [pxx, f] = pwelch(rxWaveform, hamming(1024), 512, nfft, cfg.fs, 'centered');

    f3 = figure('Visible', 'off', 'Color', 'w');
    plot(f / 1e6, 10 * log10(pxx + eps), 'LineWidth', 1.2);
    grid on;
    xlabel('Frequency (MHz)');
    ylabel('PSD (dB/Hz)');
    title(sprintf('Received Signal Spectrum (%s channel)', cfg.channelType));
    exportgraphics(f3, fullfile(outDir, 'spectrum.png'), 'Resolution', 200);
    close(f3);

    f4 = figure('Visible', 'off', 'Color', 'w');
    semilogy(cfg.snrSweep, berCurve, '-o', 'LineWidth', 1.4, 'MarkerSize', 6);
    grid on;
    xlabel('SNR (dB)');
    ylabel('BER');
    title(sprintf('BER vs SNR for %s over %s channel', cfg.modType, cfg.channelType));
    exportgraphics(f4, fullfile(outDir, 'ber_vs_snr.png'), 'Resolution', 200);
    close(f4);

    %% CSV
    writematrix([real(rxSymbols), imag(rxSymbols)], fullfile(outDir, 'constellation_points.csv'));
    writematrix([f(:), 10 * log10(pxx(:) + eps)], fullfile(outDir, 'spectrum.csv'));
    writematrix([cfg.snrSweep(:), berCurve(:)], fullfile(outDir, 'ber_curve.csv'));

    summaryData = {
        'Modulation', cfg.modType;
        'Channel', cfg.channelType;
        'BitsUsed', minBitLen;
        'SNR_Input_dB', cfg.snrDb;
        'SNR_Est_dB', snrEst;
        'BER', ber;
        'BitErrors', numErrors;
        'EVM_RMS_percent', evmRms
    };

    writecell(summaryData, fullfile(outDir, 'summary.csv'));

    results.cfg         = cfg;
    results.txBits      = txBitsRef;
    results.rxBits      = rxBitsRef;
    results.txSymbols   = txSymbolsRef;
    results.rxSymbols   = rxSymbols;
    results.ber         = ber;
    results.evmRms      = evmRms;
    results.snrEst      = snrEst;
    results.berCurve    = berCurve;
    results.snrSweep    = cfg.snrSweep;
    results.chanInfo    = chanInfo;

    save(fullfile(outDir, 'pluto_plus_link_engine_results.mat'), 'results');
end

function [M, bitsPerSym] = getModulationOrder(modType)
    switch upper(modType)
        case 'BPSK',   M = 2;
        case 'QPSK',   M = 4;
        case '8PSK',   M = 8;
        case '16QAM',  M = 16;
        case '64QAM',  M = 64;
        case '256QAM', M = 256;
        otherwise
            error('Непідтримуваний тип модуляції: %s', modType);
    end
    bitsPerSym = log2(M);
end

function sym = modulateBits(bits, modType, M, bitsPerSym)
    bits = bits(:);
    bitMatrix = reshape(bits, bitsPerSym, []).';
    symbolsInt = bi2de(bitMatrix, 'left-msb');

    switch upper(modType)
        case 'BPSK'
            sym = pskmod(symbolsInt, M, 0, 'gray');
        case 'QPSK'
            sym = pskmod(symbolsInt, M, pi/4, 'gray');
        case '8PSK'
            sym = pskmod(symbolsInt, M, 0, 'gray');
        case {'16QAM','64QAM','256QAM'}
            sym = qammod(symbolsInt, M, 'gray', 'UnitAveragePower', true);
        otherwise
            error('Непідтримувана модуляція.');
    end
    sym = sym(:);
end

function bits = demodulateSymbols(sym, modType, M, bitsPerSym)
    switch upper(modType)
        case 'BPSK'
            dataInt = pskdemod(sym, M, 0, 'gray');
        case 'QPSK'
            dataInt = pskdemod(sym, M, pi/4, 'gray');
        case '8PSK'
            dataInt = pskdemod(sym, M, 0, 'gray');
        case {'16QAM','64QAM','256QAM'}
            dataInt = qamdemod(sym, M, 'gray', 'UnitAveragePower', true);
        otherwise
            error('Непідтримувана демодуляція.');
    end

    bitMatrix = de2bi(dataInt, bitsPerSym, 'left-msb');
    bits = reshape(bitMatrix.', [], 1);
end

function [rx, info] = applyChannel(tx, cfg)
    tx = tx(:);
    info = struct();

    switch upper(cfg.channelType)
        case 'AWGN'
            rx = awgn(tx, cfg.snrDb, 'measured');
            info.name = 'AWGN';

        case 'RAYLEIGH'
            rayChan = comm.RayleighChannel( ...
                'SampleRate', cfg.fs, ...
                'PathDelays', 0, ...
                'AveragePathGains', 0, ...
                'MaximumDopplerShift', cfg.maxDopplerShift, ...
                'NormalizePathGains', true, ...
                'PathGainsOutputPort', true);

            [faded, pathGains] = rayChan(tx);
            eqSig = faded ./ (pathGains + 1e-12);
            rx = awgn(eqSig, cfg.snrDb, 'measured');
            info.name = 'Rayleigh';
            info.pathGains = pathGains;

        case 'RICIAN'
            ricChan = comm.RicianChannel( ...
                'SampleRate', cfg.fs, ...
                'PathDelays', 0, ...
                'AveragePathGains', 0, ...
                'KFactor', cfg.kFactor, ...
                'MaximumDopplerShift', cfg.maxDopplerShift, ...
                'NormalizePathGains', true, ...
                'PathGainsOutputPort', true);

            [faded, pathGains] = ricChan(tx);
            eqSig = faded ./ (pathGains + 1e-12);
            rx = awgn(eqSig, cfg.snrDb, 'measured');
            info.name = 'Rician';
            info.pathGains = pathGains;

        case 'IMPULSE'
            rx = awgn(tx, cfg.snrDb, 'measured');
            impulseMask = rand(size(rx)) < cfg.impulseProb;
            impulseNoise = cfg.impulseAmp * ...
                (randn(size(rx)) + 1j * randn(size(rx))) / sqrt(2);
            rx = rx + impulseMask .* impulseNoise;
            info.name = 'Impulse + AWGN';
            info.impulseCount = sum(impulseMask);

        case 'COMBINED'
            rayChan = comm.RayleighChannel( ...
                'SampleRate', cfg.fs, ...
                'PathDelays', 0, ...
                'AveragePathGains', 0, ...
                'MaximumDopplerShift', cfg.maxDopplerShift, ...
                'NormalizePathGains', true, ...
                'PathGainsOutputPort', true);

            [faded, pathGains] = rayChan(tx);
            eqSig = faded ./ (pathGains + 1e-12);
            rx = awgn(eqSig, cfg.snrDb, 'measured');

            impulseMask = rand(size(rx)) < cfg.impulseProb;
            impulseNoise = cfg.impulseAmp * ...
                (randn(size(rx)) + 1j * randn(size(rx))) / sqrt(2);
            rx = rx + impulseMask .* impulseNoise;

            info.name = 'Rayleigh + AWGN + Impulse';
            info.pathGains = pathGains;
            info.impulseCount = sum(impulseMask);

        otherwise
            error('Непідтримуваний тип каналу: %s', cfg.channelType);
    end
end
function manualEyeDiagram(sig, sps)
% Ручна побудова eye diagram без eyediagram()

    numTraces = min(200, floor(length(sig) / (2 * sps)));

    hold on;
    for k = 1:numTraces
        idx = (k - 1) * 2 * sps + 1 : k * 2 * sps;
        if idx(end) <= length(sig)
            t = linspace(-1, 1, 2 * sps);
            plot(t, sig(idx), 'Color', [0.2 0.5 0.9 0.25], 'LineWidth', 0.6);
        end
    end
    hold off;

    grid on;
    xlabel('Time / T');
    ylabel('Amplitude');
end
