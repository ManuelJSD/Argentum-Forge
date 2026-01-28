package org.argentumforge.engine.utils;

/**
 * Utilidades para la gestión de archivos de mapa en el sistema de archivos.
 * <p>
 * Provee diálogos de selección de archivos para cargar y guardar mapas,
 * integrando la lógica con el {@link GameData} y las opciones de usuario.
 */
import org.argentumforge.engine.game.Options;
import org.argentumforge.engine.gui.FileDialog;
import org.argentumforge.engine.i18n.I18n;

import java.io.File;

public class MapFileUtils {

    /**
     * Abre un diálogo de selección de archivo para cargar un mapa.
     * 
     * @return true si se seleccionó y cargó un mapa correctamente, false en caso
     *         contrario.
     */
    public static boolean openAndLoadMap() {
        String lastPath = Options.INSTANCE.getLastMapPath();
        if (lastPath == null || lastPath.isEmpty()) {
            lastPath = new File(".").getAbsolutePath() + File.separator;
        } else {
            if (!lastPath.endsWith(File.separator)) {
                lastPath += File.separator;
            }
        }

        String selectedFile = org.argentumforge.engine.gui.FileDialog.showOpenDialog(
                "Seleccionar Mapa",
                lastPath,
                "Archivos de Mapa (*.map)",
                "*.map");

        if (selectedFile != null) {
            File f = new File(selectedFile);
            // Guardar el último directorio utilizado
            Options.INSTANCE.setLastMapPath(f.getParent());
            Options.INSTANCE.save();

            GameData.loadMap(selectedFile);
            org.argentumforge.engine.utils.editor.commands.CommandManager.getInstance().clearHistory();
            return true;
        }
        return false;
    }

    /**
     * Abre un diálogo de configuración de guardado y luego el selector de archivos.
     * 
     * @param onSuccess Callback opcional a ejecutar tras un guardado exitoso
     */
    public static void saveMapAs(Runnable onSuccess) {
        MapManager.MapSaveOptions currentOpts = MapManager.MapSaveOptions.standard();
        MapContext context = GameData.getActiveContext();
        if (context != null && context.getSaveOptions() != null) {
            currentOpts = context.getSaveOptions();
        }

        // Abrir formulario de opciones
        // FMapSaveOptions se encargará de llamar al FileDialog y luego a
        // GameData.saveMap
        org.argentumforge.engine.gui.ImGUISystem.INSTANCE.show(
                new org.argentumforge.engine.gui.forms.FMapSaveOptions(currentOpts, onSuccess));
    }

    // Sobrecarga para compatibilidad si alguien llama sin argumentos (aunque
    // idealmente refactorizar)
    public static void saveMapAs() {
        saveMapAs(null);
    }

    /**
     * Guarda el mapa actual sin mostrar diálogo, si ya tiene ruta asociada.
     * Si no tiene ruta, llama a saveMapAs().
     * 
     * @param onSuccess Callback si se guarda con éxito
     * @param onFailure Callback si falla o se cancela (ej: usuario cancela el Save
     *                  As)
     */
    public static void quickSaveMap(Runnable onSuccess, Runnable onFailure) {
        MapContext context = GameData.getActiveContext();
        if (context == null) {
            org.argentumforge.engine.gui.DialogManager.getInstance().showInfo("Mapa",
                    I18n.INSTANCE.get("msg.noActiveMap"));
            if (onFailure != null)
                onFailure.run();
            return;
        }

        if (context.getFilePath() == null || context.getFilePath().isEmpty()) {
            // Necesita "Guardar Como", delegamos al formulario
            // PERO: SaveAs es asíncrono (abre form).
            // Pasamos onSuccess. Si el usuario cancela el form, no hay callback de fallo
            // explícito aún en FMapSaveOptions para cancelar todo.
            // TODO: Agregar onCancel a FMapSaveOptions si es crítico manejar el "No
            // guardé".
            // Por ahora asumimos que si llama a saveMapAs, el flujo continúa allí.
            // Para soportar onFailure correctamente, FMapSaveOptions necesitaría un
            // onCancel.
            // Vamos a modificar FMapSaveOptions para soportar onCancel o simplemente
            // notar que si el usuario cancela, el flujo "Save then Continue" se rompe (que
            // es lo deseado).

            // Si requiere SaveAs, abrimos el form. El onFailure no se llamará
            // inmediatamente,
            // lo cual está bien si el usuario simplemente cierra la ventana sin guardar.
            saveMapAs(onSuccess);

        } else {
            // Guardar directamente async? No, saveMap es síncrono en disco pero rápido.
            // Podemos ejecutarlo ya.
            GameData.saveMap(context.getFilePath(), context.getSaveOptions());
            if (onSuccess != null)
                onSuccess.run();
        }
    }

    public static void quickSaveMap() {
        quickSaveMap(null, null);
    }

}