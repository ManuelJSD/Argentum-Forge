package org.argentumforge.engine.listeners;

import org.argentumforge.engine.Window;
import org.argentumforge.engine.game.*;
import org.argentumforge.engine.game.console.Console;
import org.argentumforge.engine.game.models.Direction;
import org.argentumforge.engine.game.models.Key;
import org.argentumforge.engine.gui.ImGUISystem;
import org.argentumforge.engine.renderer.RGBColor;
import org.argentumforge.engine.scenes.Camera;
import org.argentumforge.engine.utils.editor.*;

import static org.argentumforge.engine.game.console.FontStyle.REGULAR;
import static org.argentumforge.engine.scenes.Camera.*;
import static org.argentumforge.engine.utils.GameData.mapData;
import static org.lwjgl.glfw.GLFW.*;

/**
 * Gestiona la entrada del ratón y teclado para el editor de mapas.
 * Alivia la carga de GameScene centralizando la lógica de interacción.
 */
public class EditorInputManager {

    private final User user = User.INSTANCE;
    private final Surface surface = Surface.getInstance();
    private final Block block = Block.getInstance();
    private final Npc npc = Npc.getInstance();
    private final Obj obj = Obj.getInstance();
    private final Selection selection = Selection.getInstance();
    private final Trigger trigger = Trigger.getInstance();
    private final Transfer transfer = Transfer.getInstance();
    private final Particle particle = Particle.getInstance();
    private final Camera camera;

    public EditorInputManager(Camera camera) {
        this.camera = camera;
    }

    public void updateMouse() {
        if (imgui.ImGui.getIO().getWantCaptureMouse()) {
            if (!ImGUISystem.INSTANCE.isMainLast()) {
                return;
            }
        }

        if (inGameArea()) {
            int x = getTileMouseX((int) MouseListener.getX() - POS_SCREEN_X);
            int y = getTileMouseY((int) MouseListener.getY() - POS_SCREEN_Y);

            if (selection.isActive()) {
                handleSelection(x, y);
                return;
            }

            if (MouseListener.mouseButtonDown(GLFW_MOUSE_BUTTON_LEFT)) {
                surface.surface_edit(x, y);
                block.block_edit(x, y);
                npc.npc_edit(x, y);
                obj.obj_edit(x, y);
                trigger.trigger_edit(x, y);
                transfer.transfer_edit(x, y);
                particle.particle_edit(x, y);
            }

            if (MouseListener.mouseButtonReleased(GLFW_MOUSE_BUTTON_RIGHT)) {
                handleRightClick(x, y);
            }

            if (MouseListener.mouseButtonDoubleClick(GLFW_MOUSE_BUTTON_LEFT)) {
                handleDoubleClick(x, y);
            }
        }

        handleZoom();
    }

    private void handleSelection(int x, int y) {
        boolean multiSelectPressed = KeyHandler.isActionKeyPressed(Key.MULTI_SELECT);

        if (MouseListener.mouseButtonDown(GLFW_MOUSE_BUTTON_LEFT)) {
            if (!selection.isDragging() && !selection.isAreaSelecting()) {
                selection.tryGrab(x, y, multiSelectPressed);
            } else if (selection.isAreaSelecting()) {
                selection.updateAreaSelect(x, y);
            }
        } else if (MouseListener.mouseButtonReleased(GLFW_MOUSE_BUTTON_LEFT)) {
            if (selection.isDragging()) {
                selection.finalizeMove(x, y);
            } else if (selection.isAreaSelecting()) {
                selection.finalizeAreaSelect();
            }
        }
    }

    private void handleRightClick(int x, int y) {
        boolean valid = isValidTile(x, y);
        boolean active = transfer.isActive();
        int exitMap = (valid) ? mapData[x][y].getExitMap() : -1;

        // Permitir capturar si es válido Y (está activo O el editor está abierto)
        org.argentumforge.engine.gui.forms.FTransferEditor editor = (org.argentumforge.engine.gui.forms.FTransferEditor) ImGUISystem.INSTANCE
                .getForm(org.argentumforge.engine.gui.forms.FTransferEditor.class);
        boolean isEditorOpen = (editor != null);

        if (valid && exitMap > 0) {
            int destMap = mapData[x][y].getExitMap();
            int destX = mapData[x][y].getExitX();
            int destY = mapData[x][y].getExitY();

            Console.INSTANCE.addMsgToConsole(
                    "Traslado: Mapa " + destMap + " (" + destX + ", " + destY + ")",
                    REGULAR,
                    new RGBColor(0f, 1f, 0f));

            if (active || isEditorOpen) {
                transfer.captureCoordinates(destMap, destX, destY);
                if (editor != null) {
                    editor.updateInputFields(destMap, destX, destY);
                }
            }
        }
    }

    private void handleDoubleClick(int x, int y) {
        if (isValidTile(x, y) && mapData[x][y].getExitMap() > 0) {
            int destMap = mapData[x][y].getExitMap();
            int destX = mapData[x][y].getExitX();
            int destY = mapData[x][y].getExitY();

            String lastPath = org.argentumforge.engine.utils.GameData.options.getLastMapPath();
            java.io.File currentFile = new java.io.File(lastPath);
            String mapDir = currentFile.getParent();

            if (mapDir == null) {
                mapDir = org.argentumforge.engine.utils.GameData.options.getMapsPath();
            }

            String mapPath = mapDir + java.io.File.separator + "Mapa" + destMap + ".map";
            java.io.File mapFile = new java.io.File(mapPath);

            if (mapFile.exists()) {
                org.argentumforge.engine.utils.GameData.loadMap(mapPath);
                user.getUserPos().setX(destX);
                user.getUserPos().setY(destY);
                camera.update(destX, destY);

                Console.INSTANCE.addMsgToConsole(
                        "Navegado a Mapa " + destMap + " (" + destX + ", " + destY + ")",
                        REGULAR,
                        new RGBColor(0f, 1f, 1f));
            } else {
                Console.INSTANCE.addMsgToConsole(
                        "Error: No se encontró el mapa " + destMap + " en " + mapPath,
                        REGULAR,
                        new RGBColor(1f, 0f, 0f));
            }
        }
    }

    private void handleZoom() {
        float scrollY = MouseListener.getScrollY();
        if (scrollY != 0) {
            int newSize = Camera.TILE_PIXEL_SIZE + (int) (scrollY * 4);
            Camera.setTileSize(newSize);
        }
    }

    public void updateKeys() {
        ShortcutManager.getInstance().update();
        checkWalkKeys();
    }

    private void checkWalkKeys() {
        if (!user.isUserMoving()) {
            int keyCode = KeyHandler.getEffectiveMovementKey();
            if (keyCode != -1) {
                if (keyCode == Key.UP.getKeyCode())
                    user.moveTo(Direction.UP);
                else if (keyCode == Key.DOWN.getKeyCode())
                    user.moveTo(Direction.DOWN);
                else if (keyCode == Key.LEFT.getKeyCode())
                    user.moveTo(Direction.LEFT);
                else if (keyCode == Key.RIGHT.getKeyCode())
                    user.moveTo(Direction.RIGHT);
            }
        }
    }

    public static boolean inGameArea() {
        if (MouseListener.getX() < POS_SCREEN_X || MouseListener.getX() > POS_SCREEN_X + Window.SCREEN_WIDTH)
            return false;
        if (MouseListener.getY() < POS_SCREEN_Y || MouseListener.getY() > POS_SCREEN_Y + Window.SCREEN_HEIGHT)
            return false;
        return true;
    }

    public static int getTileMouseX(int mouseX) {
        return (User.INSTANCE.getUserPos().getX() + mouseX / Camera.TILE_PIXEL_SIZE - Camera.HALF_WINDOW_TILE_WIDTH);
    }

    public static int getTileMouseY(int mouseY) {
        return (User.INSTANCE.getUserPos().getY() + mouseY / Camera.TILE_PIXEL_SIZE - Camera.HALF_WINDOW_TILE_HEIGHT);
    }

    private boolean isValidTile(int x, int y) {
        return mapData != null && x >= 1 && x < mapData.length && y >= 1 && y < mapData[0].length;
    }
}
