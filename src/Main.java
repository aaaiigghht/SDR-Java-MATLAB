import com.mathworks.engine.MatlabEngine;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class Main extends JFrame {

    //Кольори інтерфейсу


    private final Color BACKGROUND_COLOR = new Color(236, 239, 244);
    private final Color PANEL_COLOR = new Color(243, 243, 243);
    private final Color TEXT_COLOR = new Color(34, 74, 112);
    private final Color BORDER_BLUE = new Color(32, 70, 109);

    //Шрифти 

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

    //MATLAB

    private MatlabEngine matlabEngine;

    private final String matlabFolder =
            "C:/Users/USER/Desktop/SDR_Project/matlab";

    private final String exportDir =
            "C:/Users/USER/Desktop/SDR_Project/java_export";

    // Параметри моделювання 

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

    // Параметри SDR

    private JComboBox<String> modeBox;
    private JComboBox<String> sdrDeviceBox;

    private JTextField ipField;
    private JTextField frequencyField;
    private JTextField txGainField;
    private JTextField rxGainField;
    private JTextField radioIdField;

    // Основні результати 

    private JLabel berLabel;
    private JLabel evmLabel;
    private JLabel snrEstLabel;
    private JLabel bitsUsedLabel;
    private JLabel comparisonModeLabel;

    // Обрані параметри 

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

    // Статус програми 

    private JLabel statusLabel;

    // Вкладки з графіками 

    private JTabbedPane plotsTabs;

    public Main() {
        setTitle(
                "Програмний додаток для досліджень з використанням SDR трансиверів на базі AD9361"
        );

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1500, 860);
        setLocationRelativeTo(null);

        getContentPane().setBackground(BACKGROUND_COLOR);
        setLayout(new BorderLayout(10, 10));

        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createMainPanel(), BorderLayout.CENTER);

        updateSelectedParameters();
        refreshAll();
    }

    // Верхня частина вікна з назвою програми 

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

    // Основна частина програми: параметри, графіки, результати 

    private JPanel createMainPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        panel.setBackground(BACKGROUND_COLOR);
        panel.setBorder(new EmptyBorder(0, 10, 10, 10));

        panel.add(createControlPanel(), BorderLayout.WEST);
        panel.add(createPlotsPanel(), BorderLayout.CENTER);
        panel.add(createResultsPanel(), BorderLayout.EAST);

        return panel;
    }

    // Ліва панель з параметрами моделювання та SDR

    private JPanel createControlPanel() {
        JPanel outer = new JPanel(new BorderLayout());

        outer.setPreferredSize(new Dimension(370, 760));
        outer.setBackground(BACKGROUND_COLOR);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(BACKGROUND_COLOR);

        // Панель параметрів моделювання 

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

        // Панель параметрів SDR

        JPanel sdrPanel =
                new JPanel(new GridLayout(0, 2, 8, 8));

        sdrPanel.setBorder(
                createStyledBorder("SDR налаштування")
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

        modeBox.addActionListener(e -> {
            updateSdrFieldsState();
            updateSelectedParameters();
        });

        updateSdrFieldsState();

        //Кнопка запуску дослідження 

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(BACKGROUND_COLOR);

        buttonPanel.setLayout(new BoxLayout(
                buttonPanel,
                BoxLayout.Y_AXIS
        ));

        JButton runButton =
                new JButton("Запустити дослідження");

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

    // Центральна панель з графіками 

    private JPanel createPlotsPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        panel.setBackground(PANEL_COLOR);
        panel.setBorder(createStyledBorder("Графіки та порівняння"));

        plotsTabs = new JTabbedPane();
        plotsTabs.setFont(VALUE_FONT);

        panel.add(plotsTabs, BorderLayout.CENTER);

        return panel;
    }

    // Права панель з числовими результатами 

    private JPanel createResultsPanel() {
        JPanel outer = new JPanel();

        outer.setPreferredSize(new Dimension(330, 760));
        outer.setBackground(BACKGROUND_COLOR);

        outer.setLayout(new BoxLayout(
                outer,
                BoxLayout.Y_AXIS
        ));

        // Панель результатів 

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
        comparisonModeLabel = createResultLabel("Comparison: -");

        resultsPanel.add(Box.createVerticalStrut(10));
        resultsPanel.add(bitsUsedLabel);
        resultsPanel.add(berLabel);
        resultsPanel.add(evmLabel);
        resultsPanel.add(snrEstLabel);
        resultsPanel.add(comparisonModeLabel);

        // Панель обраних параметрів 

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

    // Невелике поле з підписом і компонентом 

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

    // Створення текстових результатів справа 

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

    // Стиль рамок для блоків 

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

    // Вмикаємо SDR-поля тільки тоді, коли вибраний SDR-режим

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

    // Оновлення блоку з вибраними параметрами 

    private void updateSelectedParameters() {
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

        boolean isSdrMode =
                modeBox.getSelectedItem()
                        .toString()
                        .contains("SDR");

        selectedModeLabel.setVisible(true);
        selectedModeLabel.setText(
                "Mode: "
                        + modeBox.getSelectedItem()
        );

        selectedDeviceLabel.setVisible(isSdrMode);
        selectedFrequencyLabel.setVisible(isSdrMode);
        selectedIpLabel.setVisible(isSdrMode);
        selectedTxGainLabel.setVisible(isSdrMode);
        selectedRxGainLabel.setVisible(isSdrMode);
        selectedRadioIdLabel.setVisible(isSdrMode);

        if (isSdrMode) {
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
        }
    }

   // Запуск MATLAB Engine

    private void ensureMatlabEngine() throws Exception {
        if (matlabEngine == null) {
            statusLabel.setText(
                    "Статус: запуск MATLAB..."
            );

            matlabEngine = MatlabEngine.startMatlab();
        }
    }

    //Основний запуск дослідження

    private void runSimulation() {
        statusLabel.setText(
                "Статус: виконання розрахунку..."
        );

        updateSelectedParameters();

        SwingWorker<Void, Void> worker =
                new SwingWorker<>() {

                    @Override
                    protected Void doInBackground()
                            throws Exception {

                        ensureMatlabEngine();

   // Перед запуском видаляємо старі файли,щоб Java не показувала результат від попереднього запуску
                        deleteOldOutputFiles();

                        matlabEngine.eval(
                                "cd('"
                                        + matlabFolder.replace("\\", "/")
                                        + "')"
                        );

   //У MATLAB передаємо коротке значення режиму. Для симуляції це Simulation, для SDR це SDR
                        boolean isSdrMode =
                                modeBox.getSelectedItem()
                                        .toString()
                                        .contains("SDR");

                        String matlabMode =
                                isSdrMode ? "SDR" : "Simulation";

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

                                /*
                                 SDR-параметри передаються завжди.
                                 У Simulation Mode MATLAB їх не використовує.
                                 */
                                matlabMode,
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
                            refreshAll();

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

    //Видалення старих файлів перед новим запуском

    private void deleteOldOutputFiles() {
        File folder = new File(exportDir);

        if (!folder.exists()) {
            folder.mkdirs();
        }

        String[] files = {
                "constellation.png",
                "spectrum.png",
                "ber_vs_snr.png",
                "eye_diagram.png",
                "comparison_results.png",

                "summary.csv",
                "constellation_points.csv",
                "spectrum.csv",
                "ber_curve.csv",
                "comparison_results.csv",

                "pluto_plus_link_engine_results.mat"
        };

        for (String fileName : files) {
            File file = new File(folder, fileName);

            if (file.exists()) {
                file.delete();
            }
        }
    }

    //Повне оновлення інтерфейсу після MATLAB


    private void refreshAll() {
        loadPlots();
        loadSummary();
    }

    //Завантаження вкладок із графіками

    private void loadPlots() {
        plotsTabs.removeAll();

        plotsTabs.addTab(
                "Constellation",
                createImageTab(
                        new File(
                                exportDir,
                                "constellation.png"
                        ),
                        "Файл constellation.png не знайдено"
                )
        );

        plotsTabs.addTab(
                "Spectrum",
                createImageTab(
                        new File(
                                exportDir,
                                "spectrum.png"
                        ),
                        "Файл spectrum.png не знайдено"
                )
        );

        plotsTabs.addTab(
                "BER vs SNR",
                createImageTab(
                        new File(
                                exportDir,
                                "ber_vs_snr.png"
                        ),
                        "Файл ber_vs_snr.png не знайдено"
                )
        );

        plotsTabs.addTab(
                "Eye Diagram",
                createImageTab(
                        new File(
                                exportDir,
                                "eye_diagram.png"
                        ),
                        "Файл eye_diagram.png не знайдено"
                )
        );

        plotsTabs.addTab(
                "Порівняння",
                createComparisonTab()
        );
    }

    //Вкладка зі звичайним зображенням

    private JScrollPane createImageTab(
            File file,
            String missingText
    ) {
        JLabel label = createImageLabel(
                file,
                missingText,
                720,
                470
        );

        return new JScrollPane(label);
    }

    /* 
       Вкладка порівняння.
       У Simulation Mode показується порівняння каналів.
       У SDR Mode показується порівняння SDR і моделі.
      */

    private JPanel createComparisonTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));

        panel.setBackground(PANEL_COLOR);
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));

        File comparisonImage =
                new File(exportDir, "comparison_results.png");

        File comparisonCsv =
                new File(exportDir, "comparison_results.csv");

        JLabel imageLabel = createImageLabel(
                comparisonImage,
                "Файл comparison_results.png не знайдено. Спочатку запустіть дослідження.",
                720,
                330
        );

        JTable comparisonTable = new JTable();
        comparisonTable.setFont(VALUE_FONT);
        comparisonTable.setRowHeight(24);
        comparisonTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        if (comparisonCsv.exists()) {
            loadCsvToTable(comparisonCsv, comparisonTable);
        } else {
            DefaultTableModel model = new DefaultTableModel();

            model.addColumn("Параметр");
            model.addColumn("Значення");

            model.addRow(new Object[]{
                    "Стан",
                    "Файл comparison_results.csv не знайдено"
            });

            model.addRow(new Object[]{
                    "Можлива причина",
                    "MATLAB ще не створив файл порівняння"
            });

            model.addRow(new Object[]{
                    "Simulation Mode",
                    "Порівнюються канали AWGN, Rayleigh, Rician, Impulse, Combined"
            });

            model.addRow(new Object[]{
                    "SDR Mode",
                    "Порівнюється реальний SDR-прийом із математичною моделлю"
            });

            comparisonTable.setModel(model);
        }

        JScrollPane imageScroll =
                new JScrollPane(imageLabel);

        JScrollPane tableScroll =
                new JScrollPane(comparisonTable);

        JSplitPane splitPane =
                new JSplitPane(
                        JSplitPane.VERTICAL_SPLIT,
                        imageScroll,
                        tableScroll
                );

        splitPane.setResizeWeight(0.65);
        splitPane.setOneTouchExpandable(true);

        panel.add(splitPane, BorderLayout.CENTER);

        return panel;
    }

    //Створення JLabel для графіків

    private JLabel createImageLabel(
            File file,
            String missingText,
            int width,
            int height
    ) {
        JLabel label;

        if (file.exists()) {
            ImageIcon icon =
                    new ImageIcon(
                            file.getAbsolutePath()
                    );

            Image scaled =
                    icon.getImage().getScaledInstance(
                            width,
                            height,
                            Image.SCALE_SMOOTH
                    );

            label = new JLabel(
                    new ImageIcon(scaled),
                    SwingConstants.CENTER
            );

        } else {
            label = new JLabel(
                    missingText,
                    SwingConstants.CENTER
            );

            label.setForeground(Color.RED);
            label.setFont(VALUE_FONT);
        }

        return label;
    }

    //Читання CSV-файлу в таблицю

    private void loadCsvToTable(
            File csvFile,
            JTable table
    ) {
        DefaultTableModel model =
                new DefaultTableModel();

        try (BufferedReader reader =
                     new BufferedReader(
                             new InputStreamReader(
                                     new FileInputStream(csvFile),
                                     StandardCharsets.UTF_8
                             )
                     )) {

            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                //MATLAB зберігає CSV 

                String[] values =
                        line.split(",", -1);

                for (int i = 0; i < values.length; i++) {
                    values[i] = values[i].trim();
                }

                if (isFirstLine) {
                    for (String column : values) {
                        model.addColumn(column);
                    }

                    isFirstLine = false;

                } else {
                    model.addRow(values);
                }
            }

            table.setModel(model);

        } catch (Exception ex) {
            DefaultTableModel errorModel =
                    new DefaultTableModel();

            errorModel.addColumn("Помилка");
            errorModel.addColumn("Опис");

            errorModel.addRow(new Object[]{
                    "CSV",
                    ex.getMessage()
            });

            table.setModel(errorModel);
        }
    }

    //Завантаження summary.csv у праву панель

    private void loadSummary() {
        File summary =
                new File(exportDir, "summary.csv");

        if (!summary.exists()) {
            bitsUsedLabel.setText("Bits used: -");
            berLabel.setText("BER: -");
            evmLabel.setText("EVM RMS: -");
            snrEstLabel.setText("SNR est: -");
            comparisonModeLabel.setText("Comparison: -");
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

            comparisonModeLabel.setText(
                    "Comparison: "
                            + map.getOrDefault(
                            "ComparisonMode",
                            "-"
                    )
            );

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    ex.getMessage(),
                    "Помилка читання summary.csv",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    // Точка входу в програму

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Main window = new Main();
            window.setVisible(true);
        });
    }
}
