package org.argentumforge.engine.utils;

import javax.swing.JFileChooser;
import java.io.File;

/**
 * Utilidad para mostrar selectores de archivos y carpetas.
 */
public class FileChooserUtil {

    /**
     * Muestra un diálogo para seleccionar una carpeta.
     * 
     * @param title Título del diálogo
     * @return Ruta absoluta de la carpeta seleccionada, o null si se canceló
     */
    public static String selectFolder(String title) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(title);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);

        int result = chooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile().getAbsolutePath();
        }
        return null;
    }
}
