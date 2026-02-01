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

import static org.argentumforge.engine.game.console.FontStyle.REGULAR;

/**
 * Controlador principal para acciones globales del editor.
 * Desacopla la lógica de negocio de la vista (FMain).
 */
public enum EditorController {
    INSTANCE;

    private boolean pasteModeActive = false;

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
        org.argentumforge.engine.utils.MapManager.checkUnsavedChangesAsync(() -> {
            org.argentumforge.engine.utils.MapManager.createEmptyMap(100, 100);
            org.argentumforge.engine.utils.GameData.updateWindowTitle();
            Console.INSTANCE.addMsgToConsole(I18n.INSTANCE.get("msg.mapCreated"),
                    org.argentumforge.engine.game.console.FontStyle.BOLD, new RGBColor(0, 1, 0));
        }, null);
    }

    public void loadMapAction() {
        org.argentumforge.engine.utils.MapFileUtils.openAndLoadMap();
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
                    org.argentumforge.engine.utils.GameData.getActiveContext(), clip.getItems(), tx, ty));

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
                org.argentumforge.engine.utils.GameData.getActiveContext(), sel.getSelectedEntities()));
        sel.getSelectedEntities().clear();
    }
}
