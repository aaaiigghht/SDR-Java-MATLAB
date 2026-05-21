import com.mathworks.engine.MatlabEngine;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
public class Main extends JFrame {
/* =========================================
   COLORS
 ========================================= */
    private final Color BACKGROUND_COLOR = new Color(236, 239, 244);
    private final Color PANEL_COLOR = new Color(243, 243, 243);
    private final Color TEXT_COLOR = new Color(34, 74, 112);
    private final Color BORDER_BLUE = new Color(32, 70, 109);

/* =========================================
   FONTS
 ========================================= */
    private final Font TITLE_FONT =
            new Font("Cambria", Font.BOLD, 22);
    private final Font SECTION_FONT =
            new Font("Cambria", Font.BOLD, 14);
    private final Font LABEL_FONT =
            new Font("Cambria", Font.BOLD, 12);
    private final Font VALUE_FONT =
            new Font("Cambria", Font.PLAIN, 12);
    private final Font BUTTON_FONT =
            new Font("Cambria", Font.BOLD, 14);
/* =========================================
   MATLAB
 ========================================= */

    private MatlabEngine matlabEngine;
    private final String matlabFolder =
            "C:/Users/USER/Desktop/SDR_Project/matlab";
    private final String exportDir =
            "C:/Users/USER/Desktop/SDR_Project/java_export";
/* =========================================
   MODELING PARAMETERS
 ========================================= */
    private JComboBox<String> modulationBox;
    private JComboBox<String> channelBox;
    private JTextField bitsField;
    private JTextField snrField;
    private JTextField spsField;
    private JTextField rolloffField;
    private JTextField filterSpanField;
    private JTextField dopplerField;
    private JTextField kFactorField;
    private JTextField impulseProbField;
    private JTextField impulseAmpField;
/* =========================================
   SDR PARAMETERS
 ========================================= */
    private JComboBox<String> modeBox;
    private JComboBox<String> sdrDeviceBox;
    private JTextField ipField;
    private JTextField frequencyField;
    private JTextField txGainField;
    private JTextField rxGainField;
    private JTextField radioIdField;
/* =========================================
   RESULTS
 ========================================= */
    private JLabel berLabel;
    private JLabel evmLabel;
    private JLabel snrEstLabel;
    private JLabel bitsUsedLabel;
/* =========================================
   SELECTED PARAMETERS
 ========================================= */
    private JLabel selectedModulationLabel;
    private JLabel selectedChannelLabel;
    private JLabel selectedBitsLabel;
    private JLabel selectedSnrLabel;
    private JLabel selectedSpsLabel;
    private JLabel selectedModeLabel;
    private JLabel selectedDeviceLabel;
    private JLabel selectedFrequencyLabel;
    private JLabel selectedIpLabel;
    private JLabel selectedTxGainLabel;
    private JLabel selectedRxGainLabel;
    private JLabel selectedRadioIdLabel;
/* =========================================
   STATUS
 ========================================= */
    private JLabel statusLabel;
/* =========================================
   PLOTS
 ========================================= */
    private JTabbedPane plotsTabs;
    public Main() {
        setTitle(
                "Програмний додаток для досліджень SDR трансиверів на базі AD9361"
        );
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1500, 860);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BACKGROUND_COLOR);
        setLayout(new BorderLayout(10, 10));
        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createMainPanel(), BorderLayout.CENTER);
        refreshAll();
    }
    /* ========================================================= */
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BACKGROUND_COLOR);
        panel.setBorder(new EmptyBorder(10, 10, 5, 10));
        JLabel title = new JLabel(
                "<html><center><b>Програмний додаток для досліджень SDR трансиверів на базі AD9361</b></center></html>",
                SwingConstants.CENTER
        );
        title.setFont(TITLE_FONT);
        title.setForeground(TEXT_COLOR);
        panel.add(title, BorderLayout.CENTER);
        return panel;
    }
    /* ========================================================= */
    private JPanel createMainPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(BACKGROUND_COLOR);
        panel.setBorder(new EmptyBorder(0, 10, 10, 10));
        panel.add(createControlPanel(), BorderLayout.WEST);
        panel.add(createPlotsPanel(), BorderLayout.CENTER);
        panel.add(createResultsPanel(), BorderLayout.EAST);
        return panel;
    }

    /* ========================================================= */

    private JPanel createControlPanel() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setPreferredSize(new Dimension(370, 760));
        outer.setBackground(BACKGROUND_COLOR);
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(BACKGROUND_COLOR);
    /* =========================================
       MODELING PANEL
     ========================================= */
        JPanel modelingPanel =
                new JPanel(new GridLayout(0, 2, 8, 8));
        modelingPanel.setBorder(
                createStyledBorder("Параметри моделювання")
        );
        modelingPanel.setBackground(PANEL_COLOR);
        modulationBox = new JComboBox<>(new String[]{
                "BPSK",
                "QPSK",
                "8PSK",
                "16QAM",
                "64QAM",
                "256QAM"
        });
        channelBox = new JComboBox<>(new String[]{
                "AWGN",
                "Rayleigh",
                "Rician",
                "Impulse",
                "Combined"
        });
        bitsField = new JTextField("48000");
        snrField = new JTextField("18");
        spsField = new JTextField("8");
        rolloffField = new JTextField("0.35");
        filterSpanField = new JTextField("10");
        dopplerField = new JTextField("5");
        kFactorField = new JTextField("6");
        impulseProbField = new JTextField("0.0005");
        impulseAmpField = new JTextField("4.0");
        modelingPanel.add(createCompactField("Модуляція", modulationBox));
        modelingPanel.add(createCompactField("Канал", channelBox));
        modelingPanel.add(createCompactField("Bits", bitsField));
        modelingPanel.add(createCompactField("SNR", snrField));
        modelingPanel.add(createCompactField("SPS", spsField));
        modelingPanel.add(createCompactField("Rolloff", rolloffField));
        modelingPanel.add(createCompactField("Filter span", filterSpanField));
        modelingPanel.add(createCompactField("Doppler", dopplerField));
        modelingPanel.add(createCompactField("K-factor", kFactorField));
        modelingPanel.add(createCompactField("Impulse prob", impulseProbField));
        modelingPanel.add(createCompactField("Impulse amp", impulseAmpField));
    /* =========================================
       SDR PANEL
     ========================================= */
        JPanel sdrPanel =
                new JPanel(new GridLayout(0, 2, 8, 8));
        sdrPanel.setBorder(
                createStyledBorder("SDR Налаштування")
        );
        sdrPanel.setBackground(PANEL_COLOR);
        modeBox = new JComboBox<>(new String[]{
                "Simulation Mode",
                "SDR Transceiver Mode"
        });
        sdrDeviceBox = new JComboBox<>(new String[]{
                "Pluto",
                "AD9361"
        });
        ipField = new JTextField("192.168.2.1");
        frequencyField = new JTextField("2.4e9");
        txGainField = new JTextField("-10");
        rxGainField = new JTextField("20");
        radioIdField = new JTextField("usb:0");
        sdrPanel.add(createCompactField("Mode", modeBox));
        sdrPanel.add(createCompactField("SDR Device", sdrDeviceBox));
        sdrPanel.add(createCompactField("IP Address", ipField));
        sdrPanel.add(createCompactField("Frequency", frequencyField));
        sdrPanel.add(createCompactField("TX Gain", txGainField));
        sdrPanel.add(createCompactField("RX Gain", rxGainField));
        sdrPanel.add(createCompactField("Radio ID", radioIdField));
        modeBox.addActionListener(e -> updateSdrFieldsState());
        updateSdrFieldsState();
    /* =========================================
       BUTTON PANEL
     ========================================= */
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(BACKGROUND_COLOR);
        buttonPanel.setLayout(new BoxLayout(
                buttonPanel,
                BoxLayout.Y_AXIS
        ));
        JButton runButton =
                new JButton("Запустити моделювання");
        runButton.setFont(BUTTON_FONT);
        runButton.setForeground(TEXT_COLOR);
        runButton.setFocusPainted(false);
        runButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        runButton.setPreferredSize(
                new Dimension(230, 42)
        );
        runButton.setMaximumSize(
                new Dimension(230, 42)
        );
        runButton.addActionListener(e -> runSimulation());
        statusLabel = new JLabel("Статус: готово");
        statusLabel.setForeground(TEXT_COLOR);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonPanel.add(Box.createVerticalStrut(10));
        buttonPanel.add(runButton);
        buttonPanel.add(Box.createVerticalStrut(10));
        buttonPanel.add(statusLabel);
        content.add(modelingPanel);
        content.add(Box.createVerticalStrut(12));
        content.add(sdrPanel);
        content.add(Box.createVerticalStrut(15));
        content.add(buttonPanel);
        outer.add(content, BorderLayout.NORTH);
        return outer;
    }

    /* ========================================================= */
    private JPanel createPlotsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(PANEL_COLOR);
        panel.setBorder(createStyledBorder("Графіки"));
        plotsTabs = new JTabbedPane();
        plotsTabs.setFont(VALUE_FONT);
        panel.add(plotsTabs, BorderLayout.CENTER);
        return panel;
    }
    /* ========================================================= */

    private JPanel createResultsPanel() {
        JPanel outer = new JPanel();
        outer.setPreferredSize(new Dimension(330, 760));
        outer.setBackground(BACKGROUND_COLOR);
        outer.setLayout(new BoxLayout(
                outer,
                BoxLayout.Y_AXIS
        ));
    /* =========================================
       RESULTS PANEL
     ========================================= */

        JPanel resultsPanel = new JPanel();
        resultsPanel.setLayout(new BoxLayout(
                resultsPanel,
                BoxLayout.Y_AXIS
        ));
        resultsPanel.setBackground(PANEL_COLOR);
        resultsPanel.setBorder(
                createStyledBorder("Результати")
        );
        bitsUsedLabel = createResultLabel("Bits used: -");
        berLabel = createResultLabel("BER: -");
        evmLabel = createResultLabel("EVM RMS: -");
        snrEstLabel = createResultLabel("SNR est: -");
        resultsPanel.add(Box.createVerticalStrut(10));
        resultsPanel.add(bitsUsedLabel);
        resultsPanel.add(berLabel);
        resultsPanel.add(evmLabel);
        resultsPanel.add(snrEstLabel);

    /* =========================================
       SELECTED PARAMETERS PANEL
     ========================================= */
        JPanel selectedPanel = new JPanel();
        selectedPanel.setLayout(new BoxLayout(
                selectedPanel,
                BoxLayout.Y_AXIS
        ));
        selectedPanel.setBackground(PANEL_COLOR);
        selectedPanel.setBorder(
                createStyledBorder("Обрані параметри")
        );
        selectedModulationLabel =
                createResultLabel("Modulation: -");
        selectedChannelLabel =
                createResultLabel("Channel: -");
        selectedBitsLabel =
                createResultLabel("Bits: -");
        selectedSnrLabel =
                createResultLabel("SNR: -");
        selectedSpsLabel =
                createResultLabel("SPS: -");
        selectedModeLabel =
                createResultLabel("Mode: -");
        selectedDeviceLabel =
                createResultLabel("SDR Device: -");
        selectedFrequencyLabel =
                createResultLabel("Frequency: -");
        selectedIpLabel =
                createResultLabel("IP Address: -");
        selectedTxGainLabel =
                createResultLabel("TX Gain: -");
        selectedRxGainLabel =
                createResultLabel("RX Gain: -");
        selectedRadioIdLabel =
                createResultLabel("Radio ID: -");
        selectedPanel.add(Box.createVerticalStrut(10));
        selectedPanel.add(selectedModulationLabel);
        selectedPanel.add(selectedChannelLabel);
        selectedPanel.add(selectedBitsLabel);
        selectedPanel.add(selectedSnrLabel);
        selectedPanel.add(selectedSpsLabel);
        selectedPanel.add(Box.createVerticalStrut(10));
        selectedPanel.add(selectedModeLabel);
        selectedPanel.add(selectedDeviceLabel);
        selectedPanel.add(selectedFrequencyLabel);
        selectedPanel.add(selectedIpLabel);
        selectedPanel.add(selectedTxGainLabel);
        selectedPanel.add(selectedRxGainLabel);
        selectedPanel.add(selectedRadioIdLabel);
        outer.add(resultsPanel);
        outer.add(Box.createVerticalStrut(12));
        outer.add(selectedPanel);
        return outer;
    }

    /* ========================================================= */

    private JPanel createCompactField(
            String labelText,
            JComponent component
    ) {
        JPanel panel = new JPanel(new BorderLayout(3, 3));
        panel.setBackground(PANEL_COLOR);
        JLabel label = new JLabel(labelText);
        label.setFont(LABEL_FONT);
        label.setForeground(TEXT_COLOR);
        component.setFont(VALUE_FONT);
        component.setPreferredSize(
                new Dimension(120, 26)
        );
        panel.add(label, BorderLayout.NORTH);
        panel.add(component, BorderLayout.CENTER);
        return panel;
    }

    /* ========================================================= */

    private JLabel createResultLabel(String text) {

        JLabel label = new JLabel(text);
        label.setFont(
                new Font("Cambria", Font.PLAIN, 14)
        );
        label.setForeground(TEXT_COLOR);
        label.setBorder(
                new EmptyBorder(6, 12, 6, 12)
        );
        return label;
    }

    /* ========================================================= */

    private TitledBorder createStyledBorder(String title) {
        Border border =
                BorderFactory.createLineBorder(
                        BORDER_BLUE,
                        1
                );
        TitledBorder titledBorder =
                new TitledBorder(border, title);
        titledBorder.setTitleColor(BORDER_BLUE);
        titledBorder.setTitleFont(SECTION_FONT);
        return titledBorder;
    }

    /* ========================================================= */

    private void updateSdrFieldsState() {
        boolean enabled =
                modeBox.getSelectedItem()
                        .toString()
                        .contains("SDR");
        sdrDeviceBox.setEnabled(enabled);
        ipField.setEnabled(enabled);
        frequencyField.setEnabled(enabled);
        txGainField.setEnabled(enabled);
        rxGainField.setEnabled(enabled);
        radioIdField.setEnabled(enabled);
    }

    /* ========================================================= */

    private void updateSelectedParameters() {

    /* =========================
       MODELING PARAMETERS
     ========================= */
        selectedModulationLabel.setText(
                "Modulation: "
                        + modulationBox.getSelectedItem()
        );
        selectedChannelLabel.setText(
                "Channel: "
                        + channelBox.getSelectedItem()
        );
        selectedBitsLabel.setText(
                "Bits: "
                        + bitsField.getText()
        );
        selectedSnrLabel.setText(
                "SNR: "
                        + snrField.getText()
        );
        selectedSpsLabel.setText(
                "SPS: "
                        + spsField.getText()
        );

    /* =========================

SDR PARAMETERS========================= */
        boolean isSdrMode =
                modeBox.getSelectedItem()
                        .toString()
                        .contains("SDR");
        if (isSdrMode) {
            selectedModeLabel.setVisible(true);
            selectedDeviceLabel.setVisible(true);
            selectedFrequencyLabel.setVisible(true);
            selectedIpLabel.setVisible(true);
            selectedTxGainLabel.setVisible(true);
            selectedRxGainLabel.setVisible(true);
            selectedRadioIdLabel.setVisible(true);
            selectedModeLabel.setText(
                    "Mode: "
                            + modeBox.getSelectedItem()
            );
            selectedDeviceLabel.setText(
                    "SDR Device: "
                            + sdrDeviceBox.getSelectedItem()
            );
            selectedFrequencyLabel.setText(
                    "Frequency: "
                            + frequencyField.getText()
            );
            selectedIpLabel.setText(
                    "IP Address: "
                            + ipField.getText()
            );
            selectedTxGainLabel.setText(
                    "TX Gain: "
                            + txGainField.getText()
            );
            selectedRxGainLabel.setText(
                    "RX Gain: "
                            + rxGainField.getText()
            );
            selectedRadioIdLabel.setText(
                    "Radio ID: "
                            + radioIdField.getText()
            );

        } else {
            selectedModeLabel.setVisible(false);
            selectedDeviceLabel.setVisible(false);
            selectedFrequencyLabel.setVisible(false);
            selectedIpLabel.setVisible(false);
            selectedTxGainLabel.setVisible(false);
            selectedRxGainLabel.setVisible(false);
            selectedRadioIdLabel.setVisible(false);
        }
    }
    /* ========================================================= */

    private void ensureMatlabEngine() throws Exception {
        if (matlabEngine == null) {
            statusLabel.setText(
                    "Статус: запуск MATLAB..."
            );
            matlabEngine = MatlabEngine.startMatlab();
        }
    }

    /* ========================================================= */
    private void runSimulation() {
        statusLabel.setText(
                "Статус: моделювання..."
        );
        updateSelectedParameters();
        SwingWorker<Void, Void> worker =
                new SwingWorker<>() {

                    @Override
                    protected Void doInBackground()
                            throws Exception {
                        ensureMatlabEngine();
                        matlabEngine.eval(
                                "cd('"
                                        + matlabFolder.replace("\\", "/")
                                        + "')"
                        );
                        matlabEngine.feval(
                                0,
                                "run_sdr_simulation_engine",
                                modulationBox.getSelectedItem().toString(),
                                channelBox.getSelectedItem().toString(),
                                Double.parseDouble(
                                        bitsField.getText()
                                ),
                                Double.parseDouble(
                                        snrField.getText()
                                ),
                                Double.parseDouble(
                                        spsField.getText()
                                ),
                                Double.parseDouble(
                                        rolloffField.getText()
                                ),
                                Double.parseDouble(
                                        filterSpanField.getText()
                                ),
                                Double.parseDouble(
                                        dopplerField.getText()
                                ),
                                Double.parseDouble(
                                        kFactorField.getText()
                                ),
                                Double.parseDouble(
                                        impulseProbField.getText()
                                ),
                                Double.parseDouble(
                                        impulseAmpField.getText()
                                ),
                                exportDir.replace("\\", "/"),

        /* ==============================
           SDR PARAMETERS
         ============================== */

                                modeBox.getSelectedItem().toString(),
                                sdrDeviceBox.getSelectedItem().toString(),
                                ipField.getText(),
                                Double.parseDouble(
                                        frequencyField.getText()
                                ),
                                Double.parseDouble(
                                        txGainField.getText()
                                ),
                                Double.parseDouble(
                                        rxGainField.getText()
                                ),
                                radioIdField.getText()
                        );
                        return null;
                    }

                    @Override
                    protected void done() {
                        try {
                            get();
                            refreshAll();
                            statusLabel.setText(
                                    "Статус: завершено"
                            );
                        } catch (Exception ex) {

                            statusLabel.setText(
                                    "Статус: помилка"
                            );
                            JOptionPane.showMessageDialog(
                                    Main.this,
                                    ex.getMessage(),
                                    "Помилка",
                                    JOptionPane.ERROR_MESSAGE
                            );
                        }
                    }
                };

        worker.execute();
    }

    /* ========================================================= */

    private void refreshAll() {
        loadPlots();
        loadSummary();
    }
    /* ========================================================= */

    private void loadPlots() {
        plotsTabs.removeAll();
        plotsTabs.addTab(
                "Constellation",
                createImageTab(
                        new File(
                                exportDir,
                                "constellation.png"
                        )
                )
        );
        plotsTabs.addTab(
                "Spectrum",
                createImageTab(
                        new File(
                                exportDir,
                                "spectrum.png"
                        )
                )
        );
        plotsTabs.addTab(
                "BER vs SNR",
                createImageTab(
                        new File(
                                exportDir,
                                "ber_vs_snr.png"
                        )
                )
        );
        plotsTabs.addTab(
                "Eye Diagram",
                createImageTab(
                        new File(
                                exportDir,
                                "eye_diagram.png"
                        )
                )
        );
    }

    /* ========================================================= */

    private JScrollPane createImageTab(File file) {
        JLabel label;
        if (file.exists()) {
            ImageIcon icon =
                    new ImageIcon(
                            file.getAbsolutePath()
                    );
            Image scaled =
                    icon.getImage().getScaledInstance(
                            720,
                            470,
                            Image.SCALE_SMOOTH
                    );
            label = new JLabel(
                    new ImageIcon(scaled)
            );
        } else {
            label = new JLabel(
                    "Файл не знайдено",
                    SwingConstants.CENTER
            );
            label.setForeground(Color.RED);
        }
        return new JScrollPane(label);
    }

    /* ========================================================= */

    private void loadSummary() {
        File summary =
                new File(exportDir, "summary.csv");
        if (!summary.exists()) {
            return;
        }
        Map<String, String> map =
                new LinkedHashMap<>();
        try (BufferedReader reader =
                     new BufferedReader(
                             new InputStreamReader(
                                     new FileInputStream(summary),
                                     StandardCharsets.UTF_8
                             )
                     )) {

            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts =
                        line.split(",", 2);
                if (parts.length == 2) {
                    map.put(
                            parts[0].trim(),
                            parts[1].trim()
                    );
                }
            }
            bitsUsedLabel.setText(
                    "Bits used: "
                            + map.getOrDefault(
                            "BitsUsed",
                            "-"
                    )
            );
            berLabel.setText(
                    "BER: "
                            + map.getOrDefault(
                            "BER",
                            "-"
                    )
            );
            evmLabel.setText(
                    "EVM RMS: "
                            + map.getOrDefault(
                            "EVM_RMS_percent",
                            "-"
                    )
            );
            snrEstLabel.setText(
                    "SNR est: "
                            + map.getOrDefault(
                            "SNR_Est_dB",
                            "-"
                    )
            );
        } catch (Exception ex) {

            JOptionPane.showMessageDialog(
                    this,
                    ex.getMessage()
            );
        }
    }
    /* ========================================================= */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Main window = new Main();
            window.setVisible(true);
        });
    }
}
