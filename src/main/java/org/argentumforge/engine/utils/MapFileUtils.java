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

public class MapFileUtils {

    /**
     * Abre un diálogo de selección de archivo para cargar un mapa.
     * 
     * @return true si se seleccionó y cargó un mapa correctamente, false en caso
     *         contrario.
     */
    public static boolean openAndLoadMap() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Seleccionar Mapa");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Archivos de Mapa (*.map)", "map"));

        fileChooser.setCurrentDirectory(new File(Options.INSTANCE.getLastMapPath()));

        int result = fileChooser.showOpenDialog(null);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            // Guardar el último directorio utilizado
            Options.INSTANCE.setLastMapPath(selectedFile.getParent());
            Options.INSTANCE.save();
            // Cargar el mapa
            GameData.loadMap(selectedFile.getAbsolutePath());
            return true;
        }

        return false;
    }

    /**
     * Abre un diálogo de selección de archivo para guardar el mapa actual.
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

            // Asegurar la extensión .map
            if (!path.toLowerCase().endsWith(".map")) {
                path += ".map";
            }

            // Guardar el último directorio utilizado
            Options.INSTANCE.setLastMapPath(selectedFile.getParent());
            Options.INSTANCE.save();

            // Guardar el mapa
            GameData.saveMap(path);
        }
    }
}
