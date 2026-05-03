import com.mathworks.engine.MatlabEngine;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class Main extends JFrame {

    /* 1. Кольори / стиль */
    private final Color BACKGROUND_COLOR = new Color(236, 239, 244);
    private final Color PANEL_COLOR = new Color(243, 243, 243);
    private final Color TEXT_COLOR = new Color(34, 74, 112);
    private final Color BORDER_BLUE = new Color(32, 70, 109);

    /* 2. Шрифти */
    private final Font TITLE_FONT = new Font("Cambria", Font.BOLD, 22);
    private final Font SECTION_FONT = new Font("Cambria", Font.BOLD, 14);
    private final Font LABEL_FONT = new Font("Cambria", Font.BOLD, 14);
    private final Font VALUE_FONT = new Font("Cambria", Font.PLAIN, 14);
    private final Font BUTTON_FONT = new Font("Cambria", Font.BOLD, 14);
    private final Font STATUS_FONT = new Font("Cambria", Font.PLAIN, 13);

    /** Виклик MATLAB з Java */
    private MatlabEngine matlabEngine;
    private final String matlabFolder = "C:/Users/USER/Desktop/SDR_Project/matlab";
    private final String exportDir = "C:/Users/USER/Desktop/SDR_Project/java_export";

    /* 3. Блок вводу */
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

    /* 4. Блок результатів */
    private JLabel berValueLabel;
    private JLabel evmValueLabel;
    private JLabel snrEstValueLabel;
    private JLabel bitsUsedValueLabel;
    private JLabel channelUsedValueLabel;
    private JLabel modulationUsedValueLabel;
    private JLabel statusLabel;

    /* 5. Блок обраних параметрів */
    private JLabel selectedModulationLabel;
    private JLabel selectedChannelLabel;
    private JLabel selectedBitsLabel;
    private JLabel selectedSnrLabel;
    private JLabel selectedSpsLabel;
    private JLabel selectedRolloffLabel;
    private JLabel selectedFilterSpanLabel;
    private JLabel selectedDopplerLabel;
    private JLabel selectedKFactorLabel;
    private JLabel selectedImpulseProbLabel;
    private JLabel selectedImpulseAmpLabel;

    /* 6. Блок графіків */
    private JTabbedPane plotsTabbedPane;

    /* 7. Кнопка запуску */
    private JButton runButton;

    public Main() {
        setTitle("Програмний додаток для досліджень з використанням SDR трансиверів на базі AD9361");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 850);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(BACKGROUND_COLOR);

        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createMainPanel(), BorderLayout.CENTER);

        refreshAll();
        updateSelectedParametersPanel();
    }

    /**
     * Створює синю рамку для блоків.
     */
    private TitledBorder createStyledBorder(String title) {
        Border lineBorder = BorderFactory.createLineBorder(BORDER_BLUE, 1);
        TitledBorder border = new TitledBorder(lineBorder, title);
        border.setTitleColor(BORDER_BLUE);
        border.setTitleFont(SECTION_FONT);
        return border;
    }

    /**
     * Форматує рядок як: жирна назва + звичайне значення.
     */
    private String formatPair(String label, String value) {
        return "<html><span style='font-family:Cambria;'><b>" + label + "</b> " + value + "</span></html>";
    }

    /**
     * Верхня панель із заголовком.
     */
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BACKGROUND_COLOR);
        panel.setBorder(new EmptyBorder(10, 15, 10, 15));

        JLabel titleLabel = new JLabel(
                "<html><div style='text-align:center;'><b>Програмний додаток для досліджень з використанням SDR трансиверів на базі AD9361</b></div></html>",
                SwingConstants.CENTER
        );
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setForeground(TEXT_COLOR);

        panel.add(titleLabel, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Центральна частина вікна.
     */
    private JPanel createMainPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(0, 10, 10, 10));
        panel.setBackground(BACKGROUND_COLOR);

        panel.add(createControlPanel(), BorderLayout.WEST);
        panel.add(createPlotsPanel(), BorderLayout.CENTER);
        panel.add(createResultsPanel(), BorderLayout.EAST);

        return panel;
    }

    /**
     * Ліва панель з параметрами моделювання.
     */
    private JPanel createControlPanel() {
        JPanel outerPanel = new JPanel(new BorderLayout());
        outerPanel.setPreferredSize(new Dimension(320, 600));
        outerPanel.setBackground(BACKGROUND_COLOR);

        JPanel innerPanel = new JPanel();
        innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.Y_AXIS));
        innerPanel.setBorder(createStyledBorder("Параметри моделювання"));
        innerPanel.setBackground(PANEL_COLOR);

        modulationBox = new JComboBox<>(new String[]{
                "BPSK", "QPSK", "8PSK", "16QAM", "64QAM", "256QAM"
        });

        channelBox = new JComboBox<>(new String[]{
                "AWGN", "Rayleigh", "Rician", "Impulse", "Combined"
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

        innerPanel.add(createFieldPanel("Тип модуляції", modulationBox));
        innerPanel.add(createFieldPanel("Тип каналу", channelBox));
        innerPanel.add(createFieldPanel("Кількість бітів", bitsField));
        innerPanel.add(createFieldPanel("SNR, dB", snrField));
        innerPanel.add(createFieldPanel("Samples per symbol", spsField));
        innerPanel.add(createFieldPanel("Rolloff", rolloffField));
        innerPanel.add(createFieldPanel("Filter span", filterSpanField));
        innerPanel.add(createFieldPanel("Doppler shift", dopplerField));
        innerPanel.add(createFieldPanel("K-factor", kFactorField));
        innerPanel.add(createFieldPanel("Impulse probability", impulseProbField));
        innerPanel.add(createFieldPanel("Impulse amplitude", impulseAmpField));

        runButton = new JButton("Запустити моделювання");
        runButton.setFont(BUTTON_FONT);
        runButton.setForeground(TEXT_COLOR);
        runButton.addActionListener(e -> runSimulation());

        statusLabel = new JLabel("Статус: готово");
        statusLabel.setFont(STATUS_FONT);
        statusLabel.setForeground(TEXT_COLOR);
        statusLabel.setBorder(new EmptyBorder(8, 5, 5, 5));

        JPanel bottomBlock = new JPanel();
        bottomBlock.setLayout(new BoxLayout(bottomBlock, BoxLayout.Y_AXIS));
        bottomBlock.setBackground(BACKGROUND_COLOR);
        bottomBlock.add(runButton);
        bottomBlock.add(statusLabel);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BACKGROUND_COLOR);
        wrapper.add(innerPanel, BorderLayout.CENTER);
        wrapper.add(bottomBlock, BorderLayout.SOUTH);

        outerPanel.add(wrapper, BorderLayout.NORTH);
        return outerPanel;
    }

    /**
     * Окремий блок: підпис + поле/список.
     */
    private JPanel createFieldPanel(String labelText, JComponent component) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(new EmptyBorder(5, 8, 5, 8));
        panel.setBackground(PANEL_COLOR);

        JLabel label = new JLabel(labelText);
        label.setFont(LABEL_FONT);
        label.setForeground(TEXT_COLOR);

        component.setFont(VALUE_FONT);
        component.setForeground(TEXT_COLOR);

        panel.add(label, BorderLayout.NORTH);
        panel.add(component, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Центральна панель з вкладками графіків.
     */
    private JPanel createPlotsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(createStyledBorder("Графіки"));
        panel.setBackground(PANEL_COLOR);

        plotsTabbedPane = new JTabbedPane();
        plotsTabbedPane.setFont(VALUE_FONT);
        panel.add(plotsTabbedPane, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Права панель: результати + обрані параметри.
     */
    private JPanel createResultsPanel() {
        JPanel outerPanel = new JPanel();
        outerPanel.setPreferredSize(new Dimension(320, 650));
        outerPanel.setLayout(new BorderLayout());
        outerPanel.setBackground(BACKGROUND_COLOR);

        JPanel resultsPanel = new JPanel();
        resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.Y_AXIS));
        resultsPanel.setBorder(createStyledBorder("Результати"));
        resultsPanel.setBackground(PANEL_COLOR);

        modulationUsedValueLabel = createResultLabel(formatPair("Modulation:", "-"));
        channelUsedValueLabel = createResultLabel(formatPair("Channel:", "-"));
        bitsUsedValueLabel = createResultLabel(formatPair("Bits used:", "-"));
        snrEstValueLabel = createResultLabel(formatPair("SNR est:", "-"));
        berValueLabel = createResultLabel(formatPair("BER:", "-"));
        evmValueLabel = createResultLabel(formatPair("EVM RMS:", "-"));

        resultsPanel.add(Box.createVerticalStrut(10));
        resultsPanel.add(modulationUsedValueLabel);
        resultsPanel.add(channelUsedValueLabel);
        resultsPanel.add(bitsUsedValueLabel);
        resultsPanel.add(snrEstValueLabel);
        resultsPanel.add(berValueLabel);
        resultsPanel.add(evmValueLabel);

        JPanel selectedParamsPanel = new JPanel();
        selectedParamsPanel.setLayout(new BoxLayout(selectedParamsPanel, BoxLayout.Y_AXIS));
        selectedParamsPanel.setBorder(createStyledBorder("Обрані параметри"));
        selectedParamsPanel.setBackground(PANEL_COLOR);

        selectedModulationLabel = createResultLabel(formatPair("Модуляція:", "-"));
        selectedChannelLabel = createResultLabel(formatPair("Канал:", "-"));
        selectedBitsLabel = createResultLabel(formatPair("Кількість бітів:", "-"));
        selectedSnrLabel = createResultLabel(formatPair("SNR:", "-"));
        selectedSpsLabel = createResultLabel(formatPair("SPS:", "-"));
        selectedRolloffLabel = createResultLabel(formatPair("Rolloff:", "-"));
        selectedFilterSpanLabel = createResultLabel(formatPair("Filter span:", "-"));
        selectedDopplerLabel = createResultLabel(formatPair("Doppler shift:", "-"));
        selectedKFactorLabel = createResultLabel(formatPair("K-factor:", "-"));
        selectedImpulseProbLabel = createResultLabel(formatPair("Impulse probability:", "-"));
        selectedImpulseAmpLabel = createResultLabel(formatPair("Impulse amplitude:", "-"));

        selectedParamsPanel.add(Box.createVerticalStrut(10));
        selectedParamsPanel.add(selectedModulationLabel);
        selectedParamsPanel.add(selectedChannelLabel);
        selectedParamsPanel.add(selectedBitsLabel);
        selectedParamsPanel.add(selectedSnrLabel);
        selectedParamsPanel.add(selectedSpsLabel);
        selectedParamsPanel.add(selectedRolloffLabel);
        selectedParamsPanel.add(selectedFilterSpanLabel);
        selectedParamsPanel.add(selectedDopplerLabel);
        selectedParamsPanel.add(selectedKFactorLabel);
        selectedParamsPanel.add(selectedImpulseProbLabel);
        selectedParamsPanel.add(selectedImpulseAmpLabel);

        JPanel combinedPanel = new JPanel();
        combinedPanel.setLayout(new BoxLayout(combinedPanel, BoxLayout.Y_AXIS));
        combinedPanel.setBackground(BACKGROUND_COLOR);
        combinedPanel.add(resultsPanel);
        combinedPanel.add(Box.createVerticalStrut(10));
        combinedPanel.add(selectedParamsPanel);

        outerPanel.add(combinedPanel, BorderLayout.NORTH);

        return outerPanel;
    }

    /**
     * Один рядок у блоці результатів/параметрів.
     */
    private JLabel createResultLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(VALUE_FONT);
        label.setBorder(new EmptyBorder(8, 12, 8, 10));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setHorizontalAlignment(SwingConstants.LEFT);
        label.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        label.setForeground(TEXT_COLOR);
        return label;
    }

    /**
     * Запускає MATLAB Engine лише один раз.
     */
    private void ensureMatlabEngine() throws Exception {
        if (matlabEngine == null) {
            statusLabel.setText("Статус: запуск MATLAB Engine...");
            matlabEngine = MatlabEngine.startMatlab();
        }
    }

    /**
     * Оновлює блок "Обрані параметри".
     */
    private void updateSelectedParametersPanel() {
        selectedModulationLabel.setText(formatPair("Модуляція:", String.valueOf(modulationBox.getSelectedItem())));
        selectedChannelLabel.setText(formatPair("Канал:", String.valueOf(channelBox.getSelectedItem())));
        selectedBitsLabel.setText(formatPair("Кількість бітів:", bitsField.getText().trim()));
        selectedSnrLabel.setText(formatPair("SNR:", snrField.getText().trim()));
        selectedSpsLabel.setText(formatPair("SPS:", spsField.getText().trim()));
        selectedRolloffLabel.setText(formatPair("Rolloff:", rolloffField.getText().trim()));
        selectedFilterSpanLabel.setText(formatPair("Filter span:", filterSpanField.getText().trim()));
        selectedDopplerLabel.setText(formatPair("Doppler shift:", dopplerField.getText().trim()));
        selectedKFactorLabel.setText(formatPair("K-factor:", kFactorField.getText().trim()));
        selectedImpulseProbLabel.setText(formatPair("Impulse probability:", impulseProbField.getText().trim()));
        selectedImpulseAmpLabel.setText(formatPair("Impulse amplitude:", impulseAmpField.getText().trim()));
    }

    /**
     * Запускає моделювання у фоновому потоці.
     */
    private void runSimulation() {
        statusLabel.setText("Статус: виконується моделювання...");
        updateSelectedParametersPanel();

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                ensureMatlabEngine();

                matlabEngine.eval("cd('" + matlabFolder.replace("\\", "/") + "')");

                matlabEngine.feval(
                        0,
                        "run_sdr_simulation_engine",
                        modulationBox.getSelectedItem().toString(),
                        channelBox.getSelectedItem().toString(),
                        Double.parseDouble(bitsField.getText().trim()),
                        Double.parseDouble(snrField.getText().trim()),
                        Double.parseDouble(spsField.getText().trim()),
                        Double.parseDouble(rolloffField.getText().trim()),
                        Double.parseDouble(filterSpanField.getText().trim()),
                        Double.parseDouble(dopplerField.getText().trim()),
                        Double.parseDouble(kFactorField.getText().trim()),
                        Double.parseDouble(impulseProbField.getText().trim()),
                        Double.parseDouble(impulseAmpField.getText().trim()),
                        exportDir.replace("\\", "/")
                );

                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    refreshAll();
                    statusLabel.setText("Статус: моделювання завершено");
                } catch (Exception ex) {
                    statusLabel.setText("Статус: помилка моделювання");

                    JOptionPane.showMessageDialog(
                            Main.this,
                            "Помилка під час моделювання:\n" + ex.getMessage(),
                            "Помилка",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        };

        worker.execute();
    }

    /**
     * Повне оновлення інтерфейсу після моделювання.
     */
    private void refreshAll() {
        loadPlots();
        loadSummary();
    }

    /**
     * Завантажує графіки у вкладки.
     */
    private void loadPlots() {
        plotsTabbedPane.removeAll();

        plotsTabbedPane.addTab("Constellation", createImageTab(new File(exportDir, "constellation.png")));
        plotsTabbedPane.addTab("Spectrum", createImageTab(new File(exportDir, "spectrum.png")));
        plotsTabbedPane.addTab("BER vs SNR", createImageTab(new File(exportDir, "ber_vs_snr.png")));
        plotsTabbedPane.addTab("Eye Diagram", createImageTab(new File(exportDir, "eye_diagram.png")));
    }

    /**
     * Створює вкладку з картинкою.
     */
    private JScrollPane createImageTab(File imageFile) {
        JLabel label;

        if (imageFile.exists()) {
            ImageIcon icon = new ImageIcon(imageFile.getAbsolutePath());
            Image img = icon.getImage();
            Image scaledImg = img.getScaledInstance(600, 400, Image.SCALE_SMOOTH);
            label = new JLabel(new ImageIcon(scaledImg));
        } else {
            label = new JLabel(
                    "<html><center>Файл не знайдено:<br>" + imageFile.getAbsolutePath() + "</center></html>",
                    SwingConstants.CENTER
            );
            label.setForeground(Color.RED);
            label.setFont(new Font("Cambria", Font.PLAIN, 18));
        }

        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.CENTER);

        return new JScrollPane(label);
    }

    /**
     * Зчитує результати з summary.csv.
     */
    private void loadSummary() {
        File summaryFile = new File(exportDir, "summary.csv");

        if (!summaryFile.exists()) {
            modulationUsedValueLabel.setText(formatPair("Modulation:", "-"));
            channelUsedValueLabel.setText(formatPair("Channel:", "-"));
            bitsUsedValueLabel.setText(formatPair("Bits used:", "-"));
            snrEstValueLabel.setText(formatPair("SNR est:", "-"));
            berValueLabel.setText(formatPair("BER:", "-"));
            evmValueLabel.setText(formatPair("EVM RMS:", "-"));
            return;
        }

        Map<String, String> summaryMap = new LinkedHashMap<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(summaryFile), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", 2);
                if (parts.length == 2) {
                    summaryMap.put(parts[0].trim(), parts[1].trim());
                }
            }

            modulationUsedValueLabel.setText(formatPair("Modulation:", summaryMap.getOrDefault("Modulation", "-")));
            channelUsedValueLabel.setText(formatPair("Channel:", summaryMap.getOrDefault("Channel", "-")));
            bitsUsedValueLabel.setText(formatPair("Bits used:", summaryMap.getOrDefault("BitsUsed", "-")));
            snrEstValueLabel.setText(formatPair("SNR est:", summaryMap.getOrDefault("SNR_Est_dB", "-")));
            berValueLabel.setText(formatPair("BER:", summaryMap.getOrDefault("BER", "-")));
            evmValueLabel.setText(formatPair("EVM RMS:", summaryMap.getOrDefault("EVM_RMS_percent", "-")));

        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Не вдалося прочитати summary.csv\n" + e.getMessage(),
                    "Помилка",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    /**
     * Точка входу.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Main window = new Main();
            window.setVisible(true);
        });
    }
}