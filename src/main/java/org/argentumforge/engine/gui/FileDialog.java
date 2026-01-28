package org.argentumforge.engine.gui;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.nio.ByteBuffer;
import java.util.stream.Collectors;

/**
 * Wrapper seguro para TinyFileDialogs (LWJGL).
 * Provee diálogos nativos de sistema de archivos sin dependencia de Swing.
 */
public class FileDialog {

    /**
     * Muestra un diálogo para abrir archivo.
     *
     * @param title       Título de la ventana.
     * @param defaultPath Ruta por defecto.
     * @param filterDesc  Descripción del filtro (ej: "Archivos de Mapa (*.map)").
     * @param filters     Extensiones permitidas (ej: "*.map").
     * @return Ruta absoluta del archivo seleccionado o null si se canceló.
     */
    public static String showOpenDialog(String title, String defaultPath, String filterDesc, String... filters) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filtersBuffer = null;
            if (filters != null && filters.length > 0) {
                filtersBuffer = stack.mallocPointer(filters.length);
                for (String filter : filters) {
                    filtersBuffer.put(stack.UTF8(filter));
                }
                filtersBuffer.flip();
            }

            String result = TinyFileDialogs.tinyfd_openFileDialog(
                    title,
                    defaultPath,
                    filtersBuffer,
                    filterDesc,
                    false);

            return result;
        }
    }

    /**
     * Muestra un diálogo para guardar archivo.
     *
     * @param title       Título de la ventana.
     * @param defaultPath Ruta por defecto con nombre de archivo sugerido.
     * @param filterDesc  Descripción del filtro.
     * @param filters     Extensiones permitidas.
     * @return Ruta absoluta seleccionada o null si se canceló.
     */
    public static String showSaveDialog(String title, String defaultPath, String filterDesc, String... filters) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filtersBuffer = null;
            if (filters != null && filters.length > 0) {
                filtersBuffer = stack.mallocPointer(filters.length);
                for (String filter : filters) {
                    filtersBuffer.put(stack.UTF8(filter));
                }
                filtersBuffer.flip();
            }

            String result = TinyFileDialogs.tinyfd_saveFileDialog(
                    title,
                    defaultPath,
                    filtersBuffer,
                    filterDesc);

            return result;
        }
    }

    /**
     * Muestra un diálogo para seleccionar carpeta.
     *
     * @param title       Título de la ventana.
     * @param defaultPath Ruta por defecto.
     * @return Ruta absoluta de la carpeta seleccionada o null si se canceló.
     */
    public static String selectFolder(String title, String defaultPath) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            String result = TinyFileDialogs.tinyfd_selectFolderDialog(
                    title,
                    defaultPath);
            return result;
        }
    }
}
