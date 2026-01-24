package org.argentumforge.engine.utils;

/**
 * Utilidades para la gestión de archivos de mapa en el sistema de archivos.
 * <p>
 * Provee diálogos de selección de archivos para cargar y guardar mapas,
 * integrando la lógica con el {@link GameData} y las opciones de usuario.
 */
import org.argentumforge.engine.game.Options;
import org.argentumforge.engine.gui.widgets.ImGuiFilePicker;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;

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
     * Abre un diálogo de selección de archivo para guardar el mapa actual.
     */
    public static void saveMap() {
        final File[] selectedFileBox = { null };
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception e) {
                    // Ignorar
                }

                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Guardar Mapa");
                fileChooser.setFileFilter(new FileNameExtensionFilter("Archivos de Mapa (*.map)", "map"));

                String lastPath = Options.INSTANCE.getLastMapPath();
                if (lastPath != null && !lastPath.isEmpty()) {
                    fileChooser.setCurrentDirectory(new File(lastPath));
                }

                int returnVal = fileChooser.showSaveDialog(null);

                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File f = fileChooser.getSelectedFile();
                    String path = f.getAbsolutePath();

                    // Asegurar la extensión .map
                    if (!path.toLowerCase().endsWith(".map")) {
                        path += ".map";
                    }

                    // Guardar el último directorio utilizado
                    Options.INSTANCE.setLastMapPath(f.getParent());
                    Options.INSTANCE.save();

                    // Usamos el archivo con la extensión corregida
                    selectedFileBox[0] = new File(path);
                }
            });
        } catch (Exception e) {
            org.tinylog.Logger.error(e, "Error al guardar mapa");
        }

        if (selectedFileBox[0] != null) {
            // Guardar el mapa en el hilo principal
            GameData.saveMap(selectedFileBox[0].getAbsolutePath());
        }
    }
}
