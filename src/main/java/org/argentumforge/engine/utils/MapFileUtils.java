package org.argentumforge.engine.utils;

/**
 * Utilidades para la gestión de archivos de mapa en el sistema de archivos.
 * <p>
 * Provee diálogos de selección de archivos para cargar y guardar mapas,
 * integrando la lógica con el {@link GameData} y las opciones de usuario.
 */
import org.argentumforge.engine.game.Options;
import org.argentumforge.engine.i18n.I18n;
import org.argentumforge.engine.gui.FileDialog;
import org.argentumforge.engine.gui.ImGUISystem;
import org.argentumforge.engine.gui.DialogManager;
import org.argentumforge.engine.gui.forms.FMapSaveOptions;
import org.argentumforge.engine.utils.editor.commands.CommandManager;

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
            File pathFile = new File(lastPath);
            if (pathFile.isFile()) {
                lastPath = pathFile.getParent();
            }
            if (!lastPath.endsWith(File.separator)) {
                lastPath += File.separator;
            }
        }

        String ext = MapManager.getActiveFormat().getExtension();
        String desc = MapManager.getActiveFormat().getDescription();
        String filter = "*" + ext;

        String selectedFile = FileDialog.showOpenDialog(
                "Seleccionar Mapa",
                lastPath,
                desc + " (" + filter + ")",
                filter);

        if (selectedFile != null) {
            File f = new File(selectedFile);
            // Guardar el último directorio utilizado
            Options.INSTANCE.setLastMapPath(f.getParent());
            Options.INSTANCE.save();

            MapManager.loadMapAsync(selectedFile, null);
            CommandManager.getInstance().clearHistory();
            return true;
        }
        return false;
    }

    /**
     * Abre un diálogo de configuración de guardado y luego el selector de archivos.
     * 
     * @param onSuccess Callback opcional a ejecutar tras un guardado exitoso
     * @param onFailure Callback opcional si falla o se cancela
     */
    public static void saveMapAs(Runnable onSuccess, Runnable onFailure) {
        MapManager.MapSaveOptions currentOpts = MapManager.MapSaveOptions.standard();
        MapContext context = GameData.getActiveContext();
        if (context != null && context.getSaveOptions() != null) {
            currentOpts = context.getSaveOptions();
        }

        // Abrir formulario de opciones
        // FMapSaveOptions se encargará de llamar al FileDialog y luego a
        // GameData.saveMap
        ImGUISystem.INSTANCE.show(
                new FMapSaveOptions(currentOpts, onSuccess, onFailure));
    }

    public static void saveMapAs(Runnable onSuccess) {
        saveMapAs(onSuccess, null);
    }

    // Sobrecarga para compatibilidad si alguien llama sin argumentos (aunque
    // idealmente refactorizar)
    public static void saveMapAs() {
        saveMapAs(null, null);
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
            DialogManager.getInstance().showInfo("Mapa",
                    I18n.INSTANCE.get("msg.noActiveMap"));
            if (onFailure != null)
                onFailure.run();
            return;
        }

        if (context.getFilePath() == null || context.getFilePath().isEmpty()) {
            // Necesita "Guardar Como", delegamos al formulario con callbacks
            saveMapAs(onSuccess, onFailure);

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