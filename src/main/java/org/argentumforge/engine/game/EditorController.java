package org.argentumforge.engine.game;

import org.argentumforge.engine.utils.editor.Clipboard;
import org.argentumforge.engine.utils.editor.Selection;
import org.argentumforge.engine.utils.editor.commands.CommandManager;
import org.argentumforge.engine.utils.editor.commands.DeleteEntitiesCommand;
import org.argentumforge.engine.utils.editor.commands.PasteEntitiesCommand;
import org.argentumforge.engine.listeners.MouseListener;
import org.argentumforge.engine.listeners.EditorInputManager;
import org.argentumforge.engine.scenes.Camera;
import org.argentumforge.engine.game.console.Console;
import org.argentumforge.engine.renderer.RGBColor;
import org.argentumforge.engine.i18n.I18n;
import org.argentumforge.engine.gui.forms.Form;
import org.argentumforge.engine.gui.ImGUISystem;
import org.argentumforge.engine.gui.forms.FTileInspector;
import org.argentumforge.engine.utils.MapManager;
import org.argentumforge.engine.utils.GameData;
import org.argentumforge.engine.game.console.FontStyle;
import org.argentumforge.engine.utils.MapFileUtils;
import org.argentumforge.engine.gui.DialogManager;

import static org.argentumforge.engine.game.console.FontStyle.REGULAR;

/**
 * Controlador principal para acciones globales del editor.
 * Desacopla la lógica de negocio de la vista (FMain).
 */
public enum EditorController {
    INSTANCE;

    private boolean pasteModeActive = false;
    private boolean inspectorMode = false;

    public void setInspectorMode(boolean inspectorMode) {
        this.inspectorMode = inspectorMode;
        if (inspectorMode) {
            Console.INSTANCE.addMsgToConsole("Modo Inspector activado. Clic en tile para ver info.", REGULAR,
                    new RGBColor(0, 1, 0));
        } else {
            Console.INSTANCE.addMsgToConsole("Modo Inspector desactivado.", REGULAR,
                    new RGBColor(1f, 1f, 0f));

            // Cerrar la ventana del inspector si está abierta
            Form inspector = ImGUISystem.INSTANCE
                    .getForm(FTileInspector.class);
            if (inspector != null) {
                ImGUISystem.INSTANCE.deleteFrmArray(inspector);
            }
        }
    }

    public boolean isInspectorMode() {
        return inspectorMode;
    }

    public void setPasteModeActive(boolean pasteModeActive) {
        this.pasteModeActive = pasteModeActive;
        if (pasteModeActive) {
            Selection.getInstance().setActive(false); // Deshabilitar selección al pegar
        }
    }

    public boolean isPasteModeActive() {
        return pasteModeActive;
    }

    public void newMap() {
        MapManager.checkUnsavedChangesAsync(() -> {
            MapManager.createEmptyMap(100, 100);
            GameData.updateWindowTitle();
            Console.INSTANCE.addMsgToConsole(I18n.INSTANCE.get("msg.mapCreated"),
                    FontStyle.BOLD, new RGBColor(0, 1, 0));
        }, null);
    }

    public void loadMapAction() {
        MapFileUtils.openAndLoadMap();
    }

    public void copySelection() {
        Selection sel = Selection.getInstance();
        if (sel.getSelectedEntities().isEmpty()) {
            Console.INSTANCE.addMsgToConsole("Error: No hay nada seleccionado para copiar.", REGULAR,
                    new RGBColor(1f, 0.5f, 0f));
            return;
        }

        // Usar la primera entidad como referencia para el offset
        int refX = sel.getSelectedEntities().get(0).x;
        int refY = sel.getSelectedEntities().get(0).y;

        Clipboard.getInstance().copy(sel.getSelectedEntities(), refX, refY);
        Console.INSTANCE.addMsgToConsole(
                I18n.INSTANCE.get("msg.clipboard.copied", sel.getSelectedEntities().size()),
                REGULAR,
                new RGBColor(0f, 1f, 1f));
    }

    public void cutSelection() {
        Selection sel = Selection.getInstance();
        if (sel.getSelectedEntities().isEmpty())
            return;

        copySelection();
        deleteSelection();
        Console.INSTANCE.addMsgToConsole(
                I18n.INSTANCE.get("msg.clipboard.cut", sel.getSelectedEntities().size()),
                REGULAR,
                new RGBColor(1f, 0.5f, 0f));
    }

    public void pasteSelection() {
        Clipboard clip = Clipboard.getInstance();
        if (clip.isEmpty()) {
            Console.INSTANCE.addMsgToConsole("Error: El portapapeles está vacío.", REGULAR,
                    new RGBColor(1f, 0f, 0f));
            return;
        }

        if (pasteModeActive) {
            // Si ya estamos en modo paste, el clic en el mapa debería ejecutarlo
            if (!EditorInputManager.inGameArea())
                return;

            int mx = (int) MouseListener.getX() - Camera.POS_SCREEN_X;
            int my = (int) MouseListener.getY() - Camera.POS_SCREEN_Y;
            int tx = EditorInputManager.getTileMouseX(mx);
            int ty = EditorInputManager.getTileMouseY(my);

            CommandManager.getInstance().executeCommand(new PasteEntitiesCommand(
                    GameData.getActiveContext(), clip.getItems(), tx, ty));

            // Si no pulsamos Shift, salimos del modo paste tras un clic?
            // Para "fluidez", mejor quedarnos en modo paste hasta que Escape o cambie
            // herramienta
        } else {
            setPasteModeActive(true);
            Console.INSTANCE.addMsgToConsole("Modo Pegar activado (Esc para cancelar)", REGULAR,
                    new RGBColor(1f, 1f, 0f));
        }
    }

    public void deleteSelection() {
        Selection sel = Selection.getInstance();
        if (sel.getSelectedEntities().isEmpty())
            return;

        CommandManager.getInstance().executeCommand(new DeleteEntitiesCommand(
                GameData.getActiveContext(), sel.getSelectedEntities()));
        sel.getSelectedEntities().clear();
    }

    public void reloadMap() {
        if (MapManager.isMapLoading())
            return;

        var context = GameData.getActiveContext();
        if (context == null) {
            Console.INSTANCE.addMsgToConsole("Error: No hay ningún mapa abierto para recargar.", REGULAR,
                    new RGBColor(1f, 0f, 0f));
            return;
        }

        Runnable doReload = () -> {
            // Usar forceReload = true para indicar a MapManager que debe reemplazar el
            // contexto existente
            MapManager.loadMapAsync(context.getFilePath(), true, () -> {
                Console.INSTANCE.addMsgToConsole(I18n.INSTANCE.get("msg.mapReloaded"),
                        FontStyle.BOLD, new RGBColor(0, 1, 0));
            });
        };

        if (context.isModified()) {
            DialogManager.getInstance().showConfirm(
                    I18n.INSTANCE.get("dialog.reload.title"),
                    I18n.INSTANCE.get("dialog.reload.msg"),
                    doReload,
                    null);
        } else {
            doReload.run();
        }
    }
}
