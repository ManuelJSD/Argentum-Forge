package org.argentumforge.engine.utils;

import org.argentumforge.engine.game.Options;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;

public class MapFileUtils {

    /**
     * Opens a file chooser dialog to select a map file and loads it if selected.
     * 
     * @return true if a map was successfully selected and loaded, false otherwise.
     */
    public static boolean openAndLoadMap() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Seleccionar Mapa");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Archivos de Mapa (*.map)", "map"));

        fileChooser.setCurrentDirectory(new File(Options.INSTANCE.getLastMapPath()));

        int result = fileChooser.showOpenDialog(null);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            // Save the last directory
            Options.INSTANCE.setLastMapPath(selectedFile.getParent());
            Options.INSTANCE.save();
            // Load the map
            GameData.loadMap(selectedFile.getAbsolutePath());
            return true;
        }

        return false;
    }

    /**
     * Opens a file chooser dialog to save the current map.
     */
    public static void saveMap() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar Mapa");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Archivos de Mapa (*.map)", "map"));

        String lastPath = Options.INSTANCE.getLastMapPath();
        if (lastPath != null && !lastPath.isEmpty()) {
            fileChooser.setCurrentDirectory(new File(lastPath));
        }

        int result = fileChooser.showSaveDialog(null);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String path = selectedFile.getAbsolutePath();

            // Ensure extension
            if (!path.toLowerCase().endsWith(".map")) {
                path += ".map";
            }

            // Save the last directory
            Options.INSTANCE.setLastMapPath(selectedFile.getParent());
            Options.INSTANCE.save();

            // Save the map
            GameData.saveMap(path);
        }
    }
}
