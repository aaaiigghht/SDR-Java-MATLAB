function run_sdr_simulation_engine( ...
    modType, channelType, numBits, snrDb, ...
    sps, rolloff, filterSpan, maxDopplerShift, ...
    kFactor, impulseProb, impulseAmp, outDir, ...
    modeType, sdrDevice, radioID, ipAddress, ...
    centerFrequency, txGain, rxGain, mappingType)
% MATLAB backend для Java GUI через MATLAB Engine API.
% Підтримує:
% - моделювання цифрової системи зв'язку;
% - Gray / Binary mapping;
% - канали AWGN, Rayleigh, Rician, Impulse, Combined;
% - SDR-режим для Pluto / AD9361-подібного підключення;
% - експорт графіків і CSV-файлів для Java GUI.

    close all;
    clc;
    rng('default');

    %% Значення за замовчуванням, якщо Java не передала частину параметрів
    if nargin < 13 || isempty(modeType)
        modeType = 'SIMULATION';
    end

    if nargin < 14 || isempty(sdrDevice)
        sdrDevice = 'Pluto';
    end

    if nargin < 15 || isempty(radioID)
        radioID = 'usb:0';
    end

    if nargin < 16 || isempty(ipAddress)
        ipAddress = '';
    end

    if nargin < 17 || isempty(centerFrequency)
        centerFrequency = 2.4e9;
    end

    if nargin < 18 || isempty(txGain)
        txGain = -10;
    end

    if nargin < 19 || isempty(rxGain)
        rxGain = 20;
    end

    if nargin < 20 || isempty(mappingType)
        mappingType = 'gray';
    end

    outDir = char(outDir);

    if ~exist(outDir, 'dir')
        mkdir(outDir);
    end

    %% Основні параметри моделювання
    cfg.modType = char(modType);
    cfg.mappingType = normalizeMappingType(mappingType);
    cfg.mappingLabel = getMappingLabel(cfg.mappingType);

    cfg.channelType = char(channelType);
    cfg.numBits = max(1, round(toDoubleOrDefault(numBits, 10000)));
    cfg.sps = max(1, round(toDoubleOrDefault(sps, 8)));
    cfg.rolloff = toDoubleOrDefault(rolloff, 0.35);
    cfg.filterSpan = max(1, round(toDoubleOrDefault(filterSpan, 10)));
    cfg.snrDb = toDoubleOrDefault(snrDb, 20);
    cfg.snrSweep = 0:2:24;

    cfg.impulseProb = toDoubleOrDefault(impulseProb, 0.01);
    cfg.impulseAmp = toDoubleOrDefault(impulseAmp, 5);
    cfg.maxDopplerShift = toDoubleOrDefault(maxDopplerShift, 0);
    cfg.kFactor = toDoubleOrDefault(kFactor, 5);

    cfg.fs = 1e6;
    cfg.showEyeDiagram = true;

    %% Визначення параметрів модуляції
    [M, bitsPerSym] = getModulationOrder(cfg.modType);

    numSymbols = floor(cfg.numBits / bitsPerSym);
    numBitsUsed = numSymbols * bitsPerSym;

    if numSymbols < 1
        error('Кількість бітів занадто мала для вибраної модуляції.');
    end

    txBits = randi([0 1], numBitsUsed, 1);

    txSymbols = modulateBits( ...
        txBits, ...
        cfg.modType, ...
        M, ...
        bitsPerSym, ...
        cfg.mappingType);

    %% Формування переданого сигналу
    rrc = rcosdesign(cfg.rolloff, cfg.filterSpan, cfg.sps, 'sqrt');

    txWaveform = upfirdn(txSymbols, rrc, cfg.sps, 1);
    txWaveform = txWaveform(:);

    if rms(txWaveform) > 0
        txWaveform = txWaveform / rms(txWaveform);
    end

    %% Канал: симуляція або реальний SDR
    if strcmpi(strtrim(char(modeType)), 'SDR')

        try
            disp('SDR MODE ENABLED');

            devStr = strtrim(char(sdrDevice));
            radioStr = strtrim(char(radioID));
            ipStr = strtrim(char(ipAddress));

            %% Вибір RadioID залежно від типу SDR
            switch upper(devStr)

                case 'PLUTO'
                    if isempty(radioStr)
                        radioStr = 'usb:0';
                    end

                case 'AD9361'
                    if isempty(ipStr)
                        error('Для AD9361/IP режиму потрібно задати ipAddress.');
                    end

                    if startsWith(lower(ipStr), 'ip:')
                        radioStr = ipStr;
                    else
                        radioStr = ['ip:' ipStr];
                    end

                otherwise
                    error('Непідтримуваний SDR-пристрій: %s', devStr);
            end

            f_carrier = toDoubleOrDefault(centerFrequency, 2.4e9);
            f_sample = double(cfg.fs);
            gain_tx = toDoubleOrDefault(txGain, -10);
            gain_rx = toDoubleOrDefault(rxGain, 20);

            %% Кількість семплів для прийому
            samples2receive = max(4096, ceil(1.2 * numel(txWaveform)));

            %% Масштабування сигналу перед передачею
            tx_data2transmitter = txWaveform(:);
            tx_data2transmitter = 0.8 * tx_data2transmitter / ...
                (max(abs(tx_data2transmitter)) + eps);

            %% Налаштування передавача
            TxDevice = sdrtx('Pluto', ...
                'RadioID', radioStr, ...
                'CenterFrequency', f_carrier, ...
                'BasebandSampleRate', f_sample, ...
                'Gain', gain_tx);

            TxDevice = setSDRPropertyIfExists(TxDevice, 'ChannelMapping', 1);
            TxDevice = setSDRPropertyIfExists(TxDevice, 'UseCustomFilter', false);
            TxDevice = setSDRPropertyIfExists(TxDevice, 'ShowAdvancedProperties', true);
            TxDevice = setSDRPropertyIfExists(TxDevice, 'FrequencyCorrection', 0);
            TxDevice = setSDRPropertyIfExists(TxDevice, 'DataSourceSelect', 'Input Port');

            %% Налаштування приймача
            RxDevice = sdrrx('Pluto', ...
                'RadioID', radioStr, ...
                'CenterFrequency', f_carrier, ...
                'BasebandSampleRate', f_sample, ...
                'OutputDataType', 'double', ...
                'SamplesPerFrame', samples2receive);

            RxDevice = setSDRPropertyIfExists(RxDevice, 'ChannelMapping', 1);
            RxDevice = setSDRPropertyIfExists(RxDevice, 'GainSource', 'Manual');
            RxDevice = setSDRPropertyIfExists(RxDevice, 'Gain', gain_rx);
            RxDevice = setSDRPropertyIfExists(RxDevice, 'EnableBurstMode', false);
            RxDevice = setSDRPropertyIfExists(RxDevice, 'UseCustomFilter', false);
            RxDevice = setSDRPropertyIfExists(RxDevice, 'ShowAdvancedProperties', true);
            RxDevice = setSDRPropertyIfExists(RxDevice, 'FrequencyCorrection', 0);
            RxDevice = setSDRPropertyIfExists(RxDevice, 'EnableQuadratureCorrection', true);
            RxDevice = setSDRPropertyIfExists(RxDevice, 'EnableRFDCCorrection', true);
            RxDevice = setSDRPropertyIfExists(RxDevice, 'EnableBasebandDCCorrection', true);

            %% Безперервна передача
            transmitRepeat(TxDevice, tx_data2transmitter);
            pause(0.5);

            %% Прийом сигналу
            rxWaveform = [];
            maxRxAttempts = 10;

            for attempt = 1:maxRxAttempts

                try
                    [data_in_1, datavalid, overflow] = RxDevice();
                catch
                    data_in_1 = RxDevice();
                    datavalid = true;
                    overflow = false;
                end

                isValid = all(datavalid(:) == 1);
                hasOverflow = any(overflow(:) ~= 0);

                if isValid && ~hasOverflow
                    disp('Received Data Valid No Overflow');
                    rxWaveform = data_in_1(:);
                    break;

                elseif ~isValid
                    disp('Received Data is NOT Valid');

                elseif hasOverflow
                    disp('Received data missing samples');
                end

                pause(0.05);
            end

            release(TxDevice);
            release(RxDevice);

            if isempty(rxWaveform)
                error('Не вдалося отримати валідний кадр від SDR-приймача.');
            end

            rxWaveform = rxWaveform(:);

            if rms(rxWaveform) > 0
                rxWaveform = rxWaveform / rms(rxWaveform);
            end

            chanInfo.name = ['Real SDR Link: ' devStr];

        catch ME

            if exist('TxDevice', 'var')
                try
                    release(TxDevice);
                catch
                end
            end

            if exist('RxDevice', 'var')
                try
                    release(RxDevice);
                catch
                end
            end

            warning('%s', ME.message);
            disp('SDR unavailable. Switching to simulation.');

            [rxWaveform, chanInfo] = applyChannel(txWaveform, cfg);
        end

    else
        %% Звичайний режим симуляції
        [rxWaveform, chanInfo] = applyChannel(txWaveform, cfg);
    end

    %% Прийом і узгоджена фільтрація
    rxMatched = upfirdn(rxWaveform, rrc, 1, 1);
    totalDelay = cfg.filterSpan * cfg.sps;

    if length(rxMatched) <= totalDelay
        error('Сигнал надто короткий після matched filtering.');
    end

    rxMatchedSync = rxMatched(totalDelay + 1:end);
    rxSymbols = rxMatchedSync(1:cfg.sps:end);
    rxSymbols = rxSymbols(1:min(length(rxSymbols), length(txSymbols)));

    if isempty(rxSymbols)
        error('Після синхронізації не залишилось символів для демодуляції.');
    end

    txSymbolsRef = txSymbols(1:length(rxSymbols));

    %% Демодуляція
    rxBits = demodulateSymbols( ...
        rxSymbols, ...
        cfg.modType, ...
        M, ...
        bitsPerSym, ...
        cfg.mappingType);

    minBitLen = min(length(txBits), length(rxBits));

    txBitsRef = txBits(1:minBitLen);
    rxBitsRef = rxBits(1:minBitLen);

    %% Розрахунок основних метрик
    [numErrors, ber] = biterr(txBitsRef, rxBitsRef);

    alpha = (txSymbolsRef' * rxSymbols) / ...
        (txSymbolsRef' * txSymbolsRef + 1e-15);

    txAligned = alpha * txSymbolsRef;
    errVec = rxSymbols - txAligned;

    evmRms = 100 * rms(errVec) / ...
        sqrt(mean(abs(txAligned).^2) + 1e-15);

    snrEst = 10 * log10( ...
        mean(abs(txAligned).^2) / ...
        (mean(abs(errVec).^2) + 1e-15));

    %% Порівняння результатів
    comparisonAvailable = false;
    comparisonMode = 'None';
    simMetrics = struct();
    comparisonData = {};
    chanInfoSimRef = struct();

    isSDRMode = strcmpi(strtrim(char(modeType)), 'SDR');
    isRealSDR = isfield(chanInfo, 'name') && contains(chanInfo.name, 'Real SDR Link');

    if isSDRMode && isRealSDR

        try
            disp('=== COMPARISON MODE: SDR vs SIMULATION ===');

            [rxWaveformSimRef, chanInfoSimRef] = applyChannel(txWaveform, cfg);

            simMetrics = calculateLinkMetrics( ...
                rxWaveformSimRef, ...
                rrc, ...
                cfg, ...
                txBits, ...
                txSymbols, ...
                M, ...
                bitsPerSym);

            comparisonData = {
                'Metric',              'Simulation',               'SDR',        'AbsoluteDifference';
                'BER',                 simMetrics.ber,              ber,          abs(simMetrics.ber - ber);
                'BitErrors',           simMetrics.numErrors,        numErrors,    abs(simMetrics.numErrors - numErrors);
                'SNR_Est_dB',          simMetrics.snrEst,           snrEst,       abs(simMetrics.snrEst - snrEst);
                'EVM_RMS_percent',     simMetrics.evmRms,           evmRms,       abs(simMetrics.evmRms - evmRms);
                'BitsUsed',            simMetrics.bitsUsed,         minBitLen,    abs(simMetrics.bitsUsed - minBitLen);
                'RxSymbolsUsed',       simMetrics.rxSymbolsUsed,    length(rxSymbols), abs(simMetrics.rxSymbolsUsed - length(rxSymbols))
            };

            writecell(comparisonData, fullfile(outDir, 'comparison_results.csv'));

            fCmp = figure('Visible', 'off', 'Color', 'w');

            subplot(3, 1, 1);
            bar([simMetrics.ber, ber]);
            set(gca, 'XTickLabel', {'Simulation', 'SDR'});
            ylabel('BER');
            title('BER: Simulation vs SDR');
            grid on;

            subplot(3, 1, 2);
            bar([simMetrics.snrEst, snrEst]);
            set(gca, 'XTickLabel', {'Simulation', 'SDR'});
            ylabel('SNR Est. (dB)');
            title('Estimated SNR: Simulation vs SDR');
            grid on;

            subplot(3, 1, 3);
            bar([simMetrics.evmRms, evmRms]);
            set(gca, 'XTickLabel', {'Simulation', 'SDR'});
            ylabel('EVM RMS (%)');
            title('EVM: Simulation vs SDR');
            grid on;

            exportgraphics(fCmp, fullfile(outDir, 'comparison_results.png'), 'Resolution', 200);
            close(fCmp);

            comparisonAvailable = true;
            comparisonMode = 'SDR vs Simulation';

        catch ME
            warning('SDR comparison calculation failed: %s', ME.message);
        end

    else

        try
            disp('=== COMPARISON MODE: SIMULATION CHANNELS ===');

            channelList = {'AWGN', 'Rayleigh', 'Rician', 'Impulse', 'Combined'};
            berMatrix = zeros(length(cfg.snrSweep), length(channelList));

            for ch = 1:length(channelList)
                cfgCmp = cfg;
                cfgCmp.channelType = channelList{ch};

                berMatrix(:, ch) = calculateBerCurveForChannel( ...
                    txWaveform, ...
                    rrc, ...
                    cfgCmp, ...
                    txBits, ...
                    txSymbols, ...
                    M, ...
                    bitsPerSym);
            end

            comparisonHeader = [{'SNR_dB'}, channelList];

            comparisonData = [
                comparisonHeader;
                num2cell([cfg.snrSweep(:), berMatrix])
            ];

            writecell(comparisonData, fullfile(outDir, 'comparison_results.csv'));

            fCmp = figure('Visible', 'off', 'Color', 'w');

            hold on;

            for ch = 1:length(channelList)
                berToPlot = berMatrix(:, ch);
                berToPlot(berToPlot <= 0) = realmin;

                semilogy( ...
                    cfg.snrSweep, ...
                    berToPlot, ...
                    '-o', ...
                    'LineWidth', 1.3, ...
                    'MarkerSize', 5);
            end

            hold off;

            grid on;
            xlabel('SNR (dB)');
            ylabel('BER');
            title(sprintf('Simulation comparison for %s modulation, %s mapping', ...
                cfg.modType, cfg.mappingLabel));
            legend(channelList, 'Location', 'southwest');

            exportgraphics(fCmp, fullfile(outDir, 'comparison_results.png'), 'Resolution', 200);
            close(fCmp);

            simMetrics.channelList = channelList;
            simMetrics.berMatrix = berMatrix;
            simMetrics.snrSweep = cfg.snrSweep;

            comparisonAvailable = true;
            comparisonMode = 'Simulation channels comparison';

        catch ME
            warning('Simulation comparison calculation failed: %s', ME.message);
        end
    end

    %% BER залежно від SNR для вибраного каналу
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

        if isempty(rxSymTmp)
            berCurve(k) = NaN;
            continue;
        end

        rxBitsTmp = demodulateSymbols( ...
            rxSymTmp, ...
            cfg.modType, ...
            M, ...
            bitsPerSym, ...
            cfg.mappingType);

        minLenTmp = min(length(txBits), length(rxBitsTmp));

        if minLenTmp == 0
            berCurve(k) = NaN;
        else
            [~, berCurve(k)] = biterr( ...
                txBits(1:minLenTmp), ...
                rxBitsTmp(1:minLenTmp));
        end
    end

    %% Графік сузір'я
    f1 = figure('Visible', 'off', 'Color', 'w');

    plot(real(rxSymbols), imag(rxSymbols), '.', 'MarkerSize', 6);
    hold on;
    plot(real(txSymbolsRef), imag(txSymbolsRef), 'ro', 'MarkerSize', 4, 'LineWidth', 1.0);
    hold off;

    grid on;
    axis equal;
    xlabel('In-Phase');
    ylabel('Quadrature');
    title(sprintf('Constellation: %s, %s mapping, Channel: %s, SNR = %.1f dB', ...
        cfg.modType, cfg.mappingLabel, cfg.channelType, cfg.snrDb));
    legend('Received symbols', 'Ideal symbols', 'Location', 'best');

    exportgraphics(f1, fullfile(outDir, 'constellation.png'), 'Resolution', 200);
    close(f1);

    %% Eye diagram
    f2 = figure('Visible', 'off', 'Color', 'w');

    manualEyeDiagram(real(rxMatchedSync), cfg.sps);
    title(sprintf('Eye Diagram: %s, %s mapping', cfg.modType, cfg.mappingLabel));

    exportgraphics(f2, fullfile(outDir, 'eye_diagram.png'), 'Resolution', 200);
    close(f2);

    %% Спектр прийнятого сигналу
    nfft = 4096;

    [pxx, f] = pwelch( ...
        rxWaveform, ...
        hamming(1024), ...
        512, ...
        nfft, ...
        cfg.fs, ...
        'centered');

    f3 = figure('Visible', 'off', 'Color', 'w');

    plot(f / 1e6, 10 * log10(pxx + eps), 'LineWidth', 1.2);
    grid on;
    xlabel('Frequency (MHz)');
    ylabel('PSD (dB/Hz)');
    title(sprintf('Received Signal Spectrum (%s channel)', cfg.channelType));

    exportgraphics(f3, fullfile(outDir, 'spectrum.png'), 'Resolution', 200);
    close(f3);

    %% BER vs SNR для вибраного каналу
    f4 = figure('Visible', 'off', 'Color', 'w');

    berCurveToPlot = berCurve;
    berCurveToPlot(berCurveToPlot <= 0) = realmin;

    semilogy(cfg.snrSweep, berCurveToPlot, '-o', 'LineWidth', 1.4, 'MarkerSize', 6);
    grid on;
    xlabel('SNR (dB)');
    ylabel('BER');
    title(sprintf('BER vs SNR for %s, %s mapping over %s channel', ...
        cfg.modType, cfg.mappingLabel, cfg.channelType));

    exportgraphics(f4, fullfile(outDir, 'ber_vs_snr.png'), 'Resolution', 200);
    close(f4);

    %% CSV-файли для Java
    writematrix([real(rxSymbols), imag(rxSymbols)], ...
        fullfile(outDir, 'constellation_points.csv'));

    writematrix([f(:), 10 * log10(pxx(:) + eps)], ...
        fullfile(outDir, 'spectrum.csv'));

    writematrix([cfg.snrSweep(:), berCurve(:)], ...
        fullfile(outDir, 'ber_curve.csv'));

    summaryData = {
        'Modulation', cfg.modType;
        'Mapping', cfg.mappingLabel;
        'Mapping_MATLAB', cfg.mappingType;
        'Channel', cfg.channelType;
        'BitsUsed', minBitLen;
        'SNR_Input_dB', cfg.snrDb;
        'SNR_Est_dB', snrEst;
        'BER', ber;
        'BitErrors', numErrors;
        'EVM_RMS_percent', evmRms;
        'ComparisonAvailable', comparisonAvailable;
        'ComparisonMode', comparisonMode
    };

    writecell(summaryData, fullfile(outDir, 'summary.csv'));

    %% Збереження повних результатів у MAT-файл
    results.cfg = cfg;
    results.txBits = txBitsRef;
    results.rxBits = rxBitsRef;
    results.txSymbols = txSymbolsRef;
    results.rxSymbols = rxSymbols;

    results.ber = ber;
    results.numErrors = numErrors;
    results.evmRms = evmRms;
    results.snrEst = snrEst;

    results.berCurve = berCurve;
    results.snrSweep = cfg.snrSweep;
    results.chanInfo = chanInfo;

    results.comparisonAvailable = comparisonAvailable;
    results.comparisonMode = comparisonMode;

    if comparisonAvailable
        results.comparisonMetrics = simMetrics;
        results.comparisonTable = comparisonData;

        if isSDRMode && isRealSDR
            results.chanInfoSimulationReference = chanInfoSimRef;
        end
    end

    save(fullfile(outDir, 'pluto_plus_link_engine_results.mat'), 'results');
end

%% ========================================================================
%  Допоміжні функції
%  ========================================================================

function [M, bitsPerSym] = getModulationOrder(modType)

    switch upper(strtrim(char(modType)))

        case 'BPSK'
            M = 2;

        case 'QPSK'
            M = 4;

        case '8PSK'
            M = 8;

        case '16QAM'
            M = 16;

        case '64QAM'
            M = 64;

        case '256QAM'
            M = 256;

        otherwise
            error('Непідтримуваний тип модуляції: %s', modType);
    end

    bitsPerSym = log2(M);
end

function sym = modulateBits(bits, modType, M, bitsPerSym, mappingType)

    bits = bits(:);

    bitMatrix = reshape(bits, bitsPerSym, []).';
    symbolsInt = bi2de(bitMatrix, 'left-msb');

    switch upper(strtrim(char(modType)))

        case 'BPSK'
            sym = pskmod(symbolsInt, M, 0, mappingType);

        case 'QPSK'
            sym = pskmod(symbolsInt, M, pi/4, mappingType);

        case '8PSK'
            sym = pskmod(symbolsInt, M, 0, mappingType);

        case {'16QAM', '64QAM', '256QAM'}
            sym = qammod(symbolsInt, M, mappingType, 'UnitAveragePower', true);

        otherwise
            error('Непідтримувана модуляція.');
    end

    sym = sym(:);
end

function bits = demodulateSymbols(sym, modType, M, bitsPerSym, mappingType)

    sym = sym(:);

    switch upper(strtrim(char(modType)))

        case 'BPSK'
            dataInt = pskdemod(sym, M, 0, mappingType);

        case 'QPSK'
            dataInt = pskdemod(sym, M, pi/4, mappingType);

        case '8PSK'
            dataInt = pskdemod(sym, M, 0, mappingType);

        case {'16QAM', '64QAM', '256QAM'}
            dataInt = qamdemod(sym, M, mappingType, 'UnitAveragePower', true);

        otherwise
            error('Непідтримувана демодуляція.');
    end

    bitMatrix = de2bi(dataInt, bitsPerSym, 'left-msb');
    bits = reshape(bitMatrix.', [], 1);
end

function [rx, info] = applyChannel(tx, cfg)

    tx = tx(:);
    info = struct();

    switch upper(strtrim(cfg.channelType))

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

function metrics = calculateLinkMetrics( ...
    rxWaveform, rrc, cfg, txBits, txSymbols, M, bitsPerSym)
% Розрахунок метрик для окремого прийнятого сигналу.
% Використовується для порівняння SDR-прийому із симуляційним сигналом.

    rxWaveform = rxWaveform(:);

    rxMatched = upfirdn(rxWaveform, rrc, 1, 1);
    totalDelay = cfg.filterSpan * cfg.sps;

    if length(rxMatched) <= totalDelay
        error('Сигнал надто короткий після matched filtering у calculateLinkMetrics.');
    end

    rxMatchedSync = rxMatched(totalDelay + 1:end);
    rxSymbolsLocal = rxMatchedSync(1:cfg.sps:end);
    rxSymbolsLocal = rxSymbolsLocal(1:min(length(rxSymbolsLocal), length(txSymbols)));

    if isempty(rxSymbolsLocal)
        error('Після синхронізації не залишилось символів для оцінювання метрик.');
    end

    txSymbolsRefLocal = txSymbols(1:length(rxSymbolsLocal));

    rxBitsLocal = demodulateSymbols( ...
        rxSymbolsLocal, ...
        cfg.modType, ...
        M, ...
        bitsPerSym, ...
        cfg.mappingType);

    minBitLenLocal = min(length(txBits), length(rxBitsLocal));

    txBitsRefLocal = txBits(1:minBitLenLocal);
    rxBitsRefLocal = rxBitsLocal(1:minBitLenLocal);

    [numErrorsLocal, berLocal] = biterr(txBitsRefLocal, rxBitsRefLocal);

    alphaLocal = (txSymbolsRefLocal' * rxSymbolsLocal) / ...
        (txSymbolsRefLocal' * txSymbolsRefLocal + 1e-15);

    txAlignedLocal = alphaLocal * txSymbolsRefLocal;
    errVecLocal = rxSymbolsLocal - txAlignedLocal;

    evmRmsLocal = 100 * rms(errVecLocal) / ...
        sqrt(mean(abs(txAlignedLocal).^2) + 1e-15);

    snrEstLocal = 10 * log10( ...
        mean(abs(txAlignedLocal).^2) / ...
        (mean(abs(errVecLocal).^2) + 1e-15));

    metrics.ber = berLocal;
    metrics.numErrors = numErrorsLocal;
    metrics.snrEst = snrEstLocal;
    metrics.evmRms = evmRmsLocal;
    metrics.bitsUsed = minBitLenLocal;
    metrics.rxSymbolsUsed = length(rxSymbolsLocal);
end

function berCurveLocal = calculateBerCurveForChannel( ...
    txWaveform, rrc, cfg, txBits, txSymbols, M, bitsPerSym)
% Рахує BER vs SNR для одного каналу.

    berCurveLocal = zeros(size(cfg.snrSweep));
    totalDelay = cfg.filterSpan * cfg.sps;

    for k = 1:length(cfg.snrSweep)

        cfgTmp = cfg;
        cfgTmp.snrDb = cfg.snrSweep(k);

        [rxWaveTmp, ~] = applyChannel(txWaveform, cfgTmp);
        rxMatchedTmp = upfirdn(rxWaveTmp, rrc, 1, 1);

        if length(rxMatchedTmp) <= totalDelay
            berCurveLocal(k) = NaN;
            continue;
        end

        rxMatchedTmp = rxMatchedTmp(totalDelay + 1:end);
        rxSymTmp = rxMatchedTmp(1:cfg.sps:end);
        rxSymTmp = rxSymTmp(1:min(length(rxSymTmp), length(txSymbols)));

        if isempty(rxSymTmp)
            berCurveLocal(k) = NaN;
            continue;
        end

        rxBitsTmp = demodulateSymbols( ...
            rxSymTmp, ...
            cfg.modType, ...
            M, ...
            bitsPerSym, ...
            cfg.mappingType);

        minLenTmp = min(length(txBits), length(rxBitsTmp));

        if minLenTmp == 0
            berCurveLocal(k) = NaN;
        else
            [~, berCurveLocal(k)] = biterr( ...
                txBits(1:minLenTmp), ...
                rxBitsTmp(1:minLenTmp));
        end
    end
end

function manualEyeDiagram(sig, sps)
% Ручна побудова eye diagram без eyediagram().
% Це стабільніше для експорту графіка в Java GUI.

    sig = sig(:);
    numTraces = min(200, floor(length(sig) / (2 * sps)));

    hold on;

    for k = 1:numTraces

        idx = (k - 1) * 2 * sps + 1 : k * 2 * sps;

        if idx(end) <= length(sig)
            t = linspace(-1, 1, 2 * sps);
            plot(t, sig(idx), 'Color', [0.2 0.5 0.9], 'LineWidth', 0.6);
        end
    end

    hold off;

    grid on;
    xlabel('Time / T');
    ylabel('Amplitude');
end

function obj = setSDRPropertyIfExists(obj, propName, propValue)
% Безпечне встановлення SDR-параметрів.
% Якщо властивості немає у конкретній версії MATLAB, код не падає.

    if isprop(obj, propName)

        try
            obj.(propName) = propValue;
        catch ME
            warning('Could not set SDR property "%s": %s', propName, ME.message);
        end
    end
end

function mapping = normalizeMappingType(mappingType)
% Перетворює значення з Java GUI у формат MATLAB:
% Gray / gray / grey -> 'gray'
% Binary / binary / bin -> 'bin'

    if nargin < 1 || isempty(mappingType)
        mapping = 'gray';
        return;
    end

    mappingRaw = lower(strtrim(char(mappingType)));

    switch mappingRaw

        case {'gray', 'grey'}
            mapping = 'gray';

        case {'binary', 'bin'}
            mapping = 'bin';

        otherwise
            warning('Невідомий тип відображення "%s". Використано Gray mapping.', mappingRaw);
            mapping = 'gray';
    end
end

function label = getMappingLabel(mappingType)
% Назва mapping для графіків і summary.csv.

    if strcmpi(mappingType, 'bin')
        label = 'Binary';
    else
        label = 'Gray';
    end
end

function val = toDoubleOrDefault(x, defaultVal)
% Перетворює параметр у double.
% Потрібно на випадок, якщо Java передасть числове значення як текст.

    if nargin < 1 || isempty(x)
        val = defaultVal;
        return;
    end

    if isnumeric(x)
        val = double(x);

    elseif ischar(x) || isstring(x)
        val = str2double(char(x));

        if isnan(val)
            val = defaultVal;
        end

    else
        try
            val = double(x);
        catch
            val = defaultVal;
        end
    end

    if isempty(val) || any(isnan(val))
        val = defaultVal;
    end

    if numel(val) > 1
        val = val(1);
    end
end
