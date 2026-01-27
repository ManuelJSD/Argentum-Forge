package org.argentumforge.engine.utils;

/**
 * Utilidades para la gestión de archivos de mapa en el sistema de archivos.
 * <p>
 * Provee diálogos de selección de archivos para cargar y guardar mapas,
 * integrando la lógica con el {@link GameData} y las opciones de usuario.
 */
import org.argentumforge.engine.game.Options;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.awt.*;
import java.awt.event.ActionListener;
import org.argentumforge.engine.i18n.I18n;

public class MapFileUtils {

    /**
     * Abre un diálogo de selección de archivo para cargar un mapa.
     * 
     * @return true si se seleccionó y cargó un mapa correctamente, false en caso
     *         contrario.
     */
    public static boolean openAndLoadMap() {
        final File[] selectedFileBox = { null };
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception e) {
                    // Ignorar
                }

                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Seleccionar Mapa");
                fileChooser.setFileFilter(new FileNameExtensionFilter("Archivos de Mapa (*.map)", "map"));

                String lastPath = Options.INSTANCE.getLastMapPath();
                if (lastPath != null && !lastPath.isEmpty()) {
                    fileChooser.setCurrentDirectory(new File(lastPath));
                }

                int returnVal = fileChooser.showOpenDialog(null);

                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File f = fileChooser.getSelectedFile();
                    // Guardar el último directorio utilizado
                    Options.INSTANCE.setLastMapPath(f.getParent());
                    Options.INSTANCE.save();
                    selectedFileBox[0] = f;
                }
            });
        } catch (Exception e) {
            org.tinylog.Logger.error(e, "Error al abrir dialogo de seleccion de mapa");
        }

        if (selectedFileBox[0] != null) {
            // Cargar el mapa en el hilo principal (Render Thread) para evitar deadlocks
            GameData.loadMap(selectedFileBox[0].getAbsolutePath());
            org.argentumforge.engine.utils.editor.commands.CommandManager.getInstance().clearHistory();
            return true;
        }
        return false;
    }

    /**
     * Abre un diálogo de selección de archivo para guardar el mapa actual (Guardar
     * Como).
     */
    public static void saveMapAs() {
        final String[] selectedPath = { null };
        final MapManager.MapSaveOptions[] selectedOptions = { null };

        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception e) {
                    // Ignorar
                }

                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle(I18n.INSTANCE.get("menu.file.saveAs"));
                fileChooser.setFileFilter(new FileNameExtensionFilter("Archivos de Mapa (*.map)", "map"));

                String lastPath = org.argentumforge.engine.game.Options.INSTANCE.getLastMapPath();
                if (lastPath != null && !lastPath.isEmpty()) {
                    fileChooser.setCurrentDirectory(new File(lastPath));
                }

                // Obtener opciones actuales del contexto para preestablecer la UI
                MapManager.MapSaveOptions currentOpts = MapManager.MapSaveOptions.standard();
                MapContext context = GameData.getActiveContext();
                if (context != null && context.getSaveOptions() != null) {
                    currentOpts = context.getSaveOptions();
                }

                SaveOptionsPanel accessory = new SaveOptionsPanel(currentOpts);
                fileChooser.setAccessory(accessory);

                int returnVal = fileChooser.showSaveDialog(null);

                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File f = fileChooser.getSelectedFile();
                    String path = f.getAbsolutePath();

                    // Asegurar la extensión .map
                    if (!path.toLowerCase().endsWith(".map")) {
                        path += ".map";
                    }

                    selectedPath[0] = path;
                    selectedOptions[0] = accessory.getOptions();

                    // Guardar el último directorio utilizado
                    org.argentumforge.engine.game.Options.INSTANCE.setLastMapPath(new File(path).getParent());
                    org.argentumforge.engine.game.Options.INSTANCE.save();
                }
            });
        } catch (Exception e) {
            org.tinylog.Logger.error(e, "Error al guardar mapa");
        }

        if (selectedPath[0] != null && selectedOptions[0] != null) {
            // Guardar el mapa en el hilo principal con las opciones elegidas
            GameData.saveMap(selectedPath[0], selectedOptions[0]);
        }
    }

    /**
     * Guarda el mapa actual sin mostrar diálogo, si ya tiene ruta asociada.
     * Si no tiene ruta, llama a saveMapAs().
     */
    public static void quickSaveMap() {
        MapContext context = GameData.getActiveContext();
        if (context == null) {
            javax.swing.JOptionPane.showMessageDialog(null, I18n.INSTANCE.get("msg.noActiveMap"));
            return;
        }

        if (context.getFilePath() == null || context.getFilePath().isEmpty()) {
            saveMapAs();
        } else {
            // Guardar directamente con las opciones actuales del contexto
            GameData.saveMap(context.getFilePath(), context.getSaveOptions());
        }
    }

    /**
     * Panel de accesorios para JFileChooser que permite configurar el formato de
     * guardado.
     */
    private static class SaveOptionsPanel extends JPanel {
        private final JComboBox<String> presetCombo;
        private final JCheckBox headerCheck;
        private final JRadioButton shortIdxRadio;
        private final JRadioButton longIdxRadio;
        private final JSpinner versionSpinner;
        private boolean isUpdating = false;

        public SaveOptionsPanel(MapManager.MapSaveOptions initial) {
            setLayout(new GridBagLayout());
            setBorder(BorderFactory.createTitledBorder(I18n.INSTANCE.get("map.save.format.title")));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridx = 0;
            gbc.gridy = 0;

            // Presets
            add(new JLabel(I18n.INSTANCE.get("map.save.option.preset") + ":"), gbc);
            gbc.gridy++;
            presetCombo = new JComboBox<>(new String[] {
                    I18n.INSTANCE.get("map.save.format.standard"),
                    I18n.INSTANCE.get("map.save.format.extended"),
                    I18n.INSTANCE.get("map.save.format.aolibre"),
                    I18n.INSTANCE.get("map.save.option.custom")
            });
            add(presetCombo, gbc);

            gbc.gridy++;
            add(new JSeparator(), gbc);

            // Version
            gbc.gridy++;
            add(new JLabel(I18n.INSTANCE.get("map.save.option.version") + ":"), gbc);
            gbc.gridy++;
            versionSpinner = new JSpinner(new SpinnerNumberModel(initial.getVersion(), 0, 32767, 1));
            add(versionSpinner, gbc);

            // Header checkbox
            gbc.gridy++;
            headerCheck = new JCheckBox(I18n.INSTANCE.get("map.save.option.header"), initial.isIncludeHeader());
            add(headerCheck, gbc);

            // Index Size
            gbc.gridy++;
            add(new JLabel(I18n.INSTANCE.get("map.save.option.indices") + ":"), gbc);
            gbc.gridy++;
            shortIdxRadio = new JRadioButton(I18n.INSTANCE.get("map.save.option.indices.short"),
                    !initial.isUseLongIndices());
            shortIdxRadio.setToolTipText("Integer - VB6 Compatible (2 bytes)");
            longIdxRadio = new JRadioButton(I18n.INSTANCE.get("map.save.option.indices.long"),
                    initial.isUseLongIndices());
            longIdxRadio.setToolTipText("Long - Modern / 32-bit (4 bytes)");
            ButtonGroup bg = new ButtonGroup();
            bg.add(shortIdxRadio);
            bg.add(longIdxRadio);
            JPanel radioPanel = new JPanel(new GridLayout(1, 2));
            radioPanel.add(shortIdxRadio);
            radioPanel.add(longIdxRadio);
            add(radioPanel, gbc);

            // Logic to update UI based on Preset
            presetCombo.addActionListener(e -> {
                if (isUpdating)
                    return;
                isUpdating = true;
                int idx = presetCombo.getSelectedIndex();
                if (idx == 0) { // Standard
                    versionSpinner.setValue(1);
                    headerCheck.setSelected(true);
                    shortIdxRadio.setSelected(true);
                } else if (idx == 1) { // Extended
                    versionSpinner.setValue(1);
                    headerCheck.setSelected(false);
                    longIdxRadio.setSelected(true);
                } else if (idx == 2) { // AOLibre
                    versionSpinner.setValue(136);
                    headerCheck.setSelected(true);
                    longIdxRadio.setSelected(true);
                }
                isUpdating = false;
            });

            // If any individual component changes, switch preset to Custom
            ActionListener customListener = e -> {
                if (!isUpdating)
                    presetCombo.setSelectedIndex(3);
            };
            headerCheck.addActionListener(customListener);
            shortIdxRadio.addActionListener(customListener);
            longIdxRadio.addActionListener(customListener);
            versionSpinner.addChangeListener(e -> {
                if (!isUpdating)
                    presetCombo.setSelectedIndex(3);
            });

            // Initialize with correct preset
            if (initial.getVersion() == 1 && !initial.isUseLongIndices() && initial.isIncludeHeader()) {
                presetCombo.setSelectedIndex(0);
            } else if (initial.getVersion() == 1 && initial.isUseLongIndices() && !initial.isIncludeHeader()) {
                presetCombo.setSelectedIndex(1);
            } else if (initial.getVersion() == 136 && initial.isUseLongIndices() && initial.isIncludeHeader()) {
                presetCombo.setSelectedIndex(2);
            } else {
                presetCombo.setSelectedIndex(3);
            }
        }

        public MapManager.MapSaveOptions getOptions() {
            MapManager.MapSaveOptions opt = new MapManager.MapSaveOptions();
            opt.setVersion(((Number) versionSpinner.getValue()).shortValue());
            opt.setIncludeHeader(headerCheck.isSelected());
            opt.setUseLongIndices(longIdxRadio.isSelected());
            return opt;
        }
    }
}
