package org.argentumforge.engine.listeners;

import org.argentumforge.engine.Window;
import org.argentumforge.engine.game.*;
import org.argentumforge.engine.game.console.Console;
import org.argentumforge.engine.game.models.Direction;
import org.argentumforge.engine.game.models.Key;
import org.argentumforge.engine.gui.ImGUISystem;
import org.argentumforge.engine.gui.components.ContextMenu;
import org.argentumforge.engine.renderer.RGBColor;
import org.argentumforge.engine.scenes.Camera;
import org.argentumforge.engine.utils.editor.*;
import org.argentumforge.engine.audio.Sound;
import org.argentumforge.engine.utils.GameData;
import java.io.File;

import org.argentumforge.engine.gui.DialogManager;

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

    // Transfer Ignore Logic
    private boolean transferIgnored = false;
    private int ignoredX = -1;
    private int ignoredY = -1;

    // Prevent multiple dialogs
    private boolean isTransferDialogActive = false;

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

        if (valid) {
            ContextMenu.open(x, y);
        }

        // Old Logic (Optional: Keep it if user wants immediate feedback when NO menu is
        // desired?)
        // Giving priority to menu as per request "Menu Contextual [...] al hacer clic
        // derecho"
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

        if (KeyHandler.isKeyJustPressed(GLFW_KEY_M)) {
            if (Sound.isMusicPlaying()) {
                Sound.stopMusic();
                Console.INSTANCE.addMsgToConsole("Musica detenida.", REGULAR, new RGBColor(1f, 1f, 1f));
            } else {
                int musicNum = GameData.mapProperties.getMusicIndex();
                if (musicNum <= 0) {
                    Console.INSTANCE.addMsgToConsole("El mapa no tiene musica configurada.", REGULAR,
                            new RGBColor(1f, 0f, 0f));
                } else {
                    String musicPath = GameData.options.getMusicPath();
                    File oggFile = new File(musicPath, musicNum + ".ogg");
                    File midFile = new File(musicPath, musicNum + ".mid");
                    File midiFile = new File(musicPath, musicNum + ".midi");

                    String fileToPlay = null;
                    if (oggFile.exists())
                        fileToPlay = musicNum + ".ogg";
                    else if (midFile.exists())
                        fileToPlay = musicNum + ".mid";
                    else if (midiFile.exists())
                        fileToPlay = musicNum + ".midi";

                    if (fileToPlay != null) {
                        Sound.playMusic(fileToPlay);
                        Console.INSTANCE.addMsgToConsole("Reproduciendo musica...", REGULAR, new RGBColor(0f, 1f, 0f));
                    } else {
                        Console.INSTANCE.addMsgToConsole(
                                "La musica configurada (" + musicNum + ") no se encuentra en la carpeta de musica.",
                                REGULAR, new RGBColor(1f, 0f, 0f));
                    }
                }
            }
        }

        // Speed shortcuts
        if (!imgui.ImGui.getIO().getWantCaptureKeyboard()) {
            // Keypad + or Numpad Add to increase speed
            if (KeyHandler.isKeyPressed(GLFW_KEY_KP_ADD)) {
                org.argentumforge.engine.game.Options.INSTANCE.increaseSpeed();
            }
            // Keypad - or Numpad Subtract to decrease speed
            if (KeyHandler.isKeyPressed(GLFW_KEY_KP_SUBTRACT)) {
                org.argentumforge.engine.game.Options.INSTANCE.decreaseSpeed();
            }
        }

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

                // Check for Map Transfer (Trigger 1) after move
                if (user.isWalkingmode()) {
                    org.argentumforge.engine.game.models.Character userChar = org.argentumforge.engine.utils.GameData.charList[user
                            .getUserCharIndex()];
                    // Ensure userChar is valid before accessing
                    if (userChar != null) {
                        int x = userChar.getPos().getX();
                        int y = userChar.getPos().getY();
                        // Access mapData from GameData, ensure bounds
                        // FIX: inMapBounds checks BORDERS, we need valid map coordinates (1-100)
                        if (org.argentumforge.engine.utils.GameData.mapData != null && x >= 1 && x <= 100 && y >= 1
                                && y <= 100) {
                            int trig = org.argentumforge.engine.utils.GameData.mapData[x][y].getTrigger();
                            // org.tinylog.Logger.info("WalkMode Check: Pos=" + x + "," + y + " Trig=" +
                            // trig);
                            // Debug Console Output (Non-blocking)
                            // Only log if Trigger is found to avoid spam, or log every step if needed.
                            // User asked for console debug.
                            if (trig != 0) {
                                org.tinylog.Logger.info("Debug: Pos=(" + x + "," + y + ") Trigger=" + trig);
                            }
                            // Also log if we are stepping on what looks like a transfer (visual check?) -
                            // No, rely on data.

                            // if (trig != 0) {
                            // Also log if we are stepping on what looks like a transfer (visual check?) -
                            // No, rely on data.

                            // CHECK TRANSFER (Independent of Trigger value)
                            // 1. Reset Ignore if we moved off the ignored tile
                            if (transferIgnored && (x != ignoredX || y != ignoredY)) {
                                transferIgnored = false;
                                ignoredX = -1;
                                ignoredY = -1;
                            }

                            int destMap = org.argentumforge.engine.utils.GameData.mapData[x][y].getExitMap();
                            if (destMap > 0) {
                                org.tinylog.Logger.info("Transfer Found! DestMap=" + destMap);

                                // 2. Show Dialog ONLY if not ignored
                                if (!transferIgnored && !isTransferDialogActive) {
                                    isTransferDialogActive = true;
                                    org.argentumforge.engine.listeners.KeyHandler.resetInputs(); // Stop walking

                                    DialogManager.getInstance().showConfirm(
                                            "Traslado de Mapa",
                                            "¿Desea viajar al Mapa " + destMap + "?",
                                            () -> {
                                                isTransferDialogActive = false;

                                                // Capture Exit Coordinates BEFORE loading new map
                                                int destX = org.argentumforge.engine.utils.GameData.mapData[x][y]
                                                        .getExitX();
                                                int destY = org.argentumforge.engine.utils.GameData.mapData[x][y]
                                                        .getExitY();

                                                // Update User Map ID
                                                user.setUserMap((short) destMap);

                                                // Resolve Path & Load
                                                String mapPath = org.argentumforge.engine.utils.MapManager
                                                        .resolveMapPath(destMap);

                                                if (mapPath != null) {
                                                    user.removeInstanceFromMap();
                                                    org.argentumforge.engine.utils.MapManager.loadMap(mapPath);
                                                } else {
                                                    org.argentumforge.engine.game.console.Console.INSTANCE
                                                            .addMsgToConsole(
                                                                    "Error: No se encontró el mapa " + destMap
                                                                            + " (o sus variantes)",
                                                                    org.argentumforge.engine.game.console.FontStyle.REGULAR,
                                                                    new org.argentumforge.engine.renderer.RGBColor(1f,
                                                                            0f, 0f));
                                                    return;
                                                }

                                                // Update User Pos
                                                user.getUserPos().setX(destX);
                                                user.getUserPos().setY(destY);

                                                // Update Camera
                                                camera.update(destX, destY);

                                                // Refresh Character (places at new pos)
                                                user.refreshUserCharacter();

                                                // Ensure movement state is clean
                                                user.resetMovement();
                                                org.argentumforge.engine.listeners.KeyHandler.resetInputs();
                                            },
                                            () -> {
                                                isTransferDialogActive = false;
                                                // User said NO - IGNORE this tile until we move away
                                                transferIgnored = true;
                                                ignoredX = x;
                                                ignoredY = y;
                                                org.argentumforge.engine.listeners.KeyHandler.resetInputs();
                                                org.tinylog.Logger.info(
                                                        "Transfer Cancelled. Ignoring tile (" + x + "," + y
                                                                + ") until move.");
                                            });
                                }
                            }
                        }
                    }
                }
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
