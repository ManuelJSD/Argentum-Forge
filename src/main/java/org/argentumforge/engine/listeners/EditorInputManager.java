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
import org.argentumforge.engine.utils.inits.MapData;
import java.io.File;

import org.argentumforge.engine.gui.DialogManager;

import static org.argentumforge.engine.game.console.FontStyle.REGULAR;
import static org.argentumforge.engine.scenes.Camera.*;
// import static org.argentumforge.engine.utils.GameData.mapData; // Removed static import
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

    // Lógica de Ignorar Traslados
    private boolean transferIgnored = false;
    private int ignoredX = -1;
    private int ignoredY = -1;

    // Prevenir diálogos múltiples
    private boolean isTransferDialogActive = false;

    public EditorInputManager(Camera camera) {
        this.camera = camera;
    }

    public void updateMouse() {
        if (imgui.ImGui.getIO().getWantCaptureMouse() || ImGUISystem.INSTANCE.isFormVisible("FBindKeys")) {
            // Bloquear interacción con el mapa si estamos sobre un widget (botón, menú,
            // etc.)
            // o si una ventana que no sea el editor principal tiene el foco.
            if (imgui.ImGui.isAnyItemHovered() || imgui.ImGui.isAnyItemActive() || !ImGUISystem.INSTANCE.isMainLast()) {
                return;
            }
        }

        if (inGameArea()) {
            handleZoom();
            int x = getTileMouseX((int) MouseListener.getX() - POS_SCREEN_X);
            int y = getTileMouseY((int) MouseListener.getY() - POS_SCREEN_Y);

            // 1. Double Click (Prioridad Navegación)
            if (MouseListener.mouseButtonDoubleClick(GLFW_MOUSE_BUTTON_LEFT)) {
                handleDoubleClick(x, y);
                return;
            }

            // 2. Right Click (Menú Contextual o Captura de Herramientas)
            if (MouseListener.mouseButtonReleased(GLFW_MOUSE_BUTTON_RIGHT)) {
                handleRightClick(x, y);
                return;
            }

            // Lógica de Selección siempre activa por defecto (Si no hay herramientas de
            // edición activas)
            // Y si no estamos en modo pegar
            if (!hasActiveInsertOrDeleteTool() && !EditorController.INSTANCE.isPasteModeActive()) {
                selection.setActive(true);
            } else {
                selection.setActive(false);
            }

            if (selection.isActive()) {
                handleSelection(x, y);
                return;
            }

            if (EditorController.INSTANCE.isPasteModeActive()) {
                if (MouseListener.mouseButtonJustPressed(GLFW_MOUSE_BUTTON_LEFT)) {
                    EditorController.INSTANCE.pasteSelection();
                }
                return; // Evitar pintar con otras herramientas mientras pegamos
            }

            if (MouseListener.mouseButtonJustPressed(GLFW_MOUSE_BUTTON_LEFT)) {
                surface.startBatch();
            }

            if (MouseListener.mouseButtonDown(GLFW_MOUSE_BUTTON_LEFT)) {
                surface.surface_edit(GameData.getActiveContext(), x, y);
                block.block_edit(GameData.getActiveContext(), x, y);
                npc.npc_edit(GameData.getActiveContext(), x, y);
                obj.obj_edit(GameData.getActiveContext(), x, y);
                trigger.trigger_edit(GameData.getActiveContext(), x, y);
                transfer.transfer_edit(GameData.getActiveContext(), x, y);
                particle.particle_edit(GameData.getActiveContext(), x, y);
            }

            if (MouseListener.mouseButtonReleased(GLFW_MOUSE_BUTTON_LEFT)) {
                surface.endBatch();
            }
        }
    }

    private void handleSelection(int x, int y) {
        boolean multiSelectPressed = KeyHandler
                .isActionKeyPressed(org.argentumforge.engine.game.models.Key.MULTI_SELECT);
        boolean inspectorMode = org.argentumforge.engine.game.EditorController.INSTANCE.isInspectorMode();

        // En modo inspector, forzamos selección simple (no multi)
        if (inspectorMode) {
            multiSelectPressed = false;
        }

        if (MouseListener.mouseButtonDown(GLFW_MOUSE_BUTTON_LEFT)) {
            if (!selection.isDragging() && !selection.isAreaSelecting()) {
                selection.tryGrab(x, y, multiSelectPressed);

                // Solo abrir inspector si el modo Inspector está activo
                if (inspectorMode) {
                    selection.cancelDrag(); // En modo inspector nunca arrastramos, solo seleccionamos
                    if (!ImGUISystem.INSTANCE.isFormVisible("FTileInspector")) {
                        ImGUISystem.INSTANCE.show(new org.argentumforge.engine.gui.forms.FTileInspector());
                    }
                }
            } else if (selection.isAreaSelecting()) {
                if (!inspectorMode) {
                    selection.updateAreaSelect(x, y);
                }
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
            // Verificar Captura de Herramienta 'Traslados' antes de abrir menú
            // Check form directly, allow capture even if tool mode is not active yet
            var frm = ImGUISystem.INSTANCE.getForm(org.argentumforge.engine.gui.forms.FTransferEditor.class);
            if (frm != null) {
                int currentMap = user.getUserMap();
                frm.updateInputFields(currentMap, x, y);
                Console.INSTANCE.addMsgToConsole(
                        "Destino de traslado capturado: Mapa " + currentMap + " (" + x + "," + y + ")",
                        REGULAR, new RGBColor(0f, 1f, 0f));
                return; // Consumir evento, no abrir menú
            }

            ContextMenu.open(x, y);
        }
    }

    private void handleDoubleClick(int x, int y) {
        if (org.argentumforge.engine.utils.MapManager.isMapLoading())
            return;

        var context = GameData.getActiveContext();
        if (context == null)
            return;
        var mapData = context.getMapData();

        if (isValidTile(x, y) && mapData[x][y].getExitMap() > 0) {
            int destMap = mapData[x][y].getExitMap();
            int destX = mapData[x][y].getExitX();
            int destY = mapData[x][y].getExitY();

            String mapPath = org.argentumforge.engine.utils.MapManager.resolveMapPath(destMap);

            if (mapPath != null) {
                java.io.File mapFile = new java.io.File(mapPath);
                if (mapFile.exists()) {
                    user.removeInstanceFromMap();
                    user.setUserMap((short) destMap);
                    org.argentumforge.engine.utils.MapManager.loadMapAsync(mapPath, () -> {
                        user.getUserPos().setX(destX);
                        user.getUserPos().setY(destY);
                        camera.update(destX, destY);

                        // Alinear con pasos de seguridad del modo caminar:
                        user.refreshUserCharacter();
                        user.resetMovement();
                        org.argentumforge.engine.listeners.KeyHandler.resetInputs();

                        Console.INSTANCE.addMsgToConsole(
                                "Navegado a Mapa " + destMap + " (" + destX + ", " + destY + ")",
                                REGULAR,
                                new RGBColor(0f, 1f, 1f));
                    });
                } else {
                    Console.INSTANCE.addMsgToConsole(
                            "Error: No se encontró el mapa " + destMap + " en " + mapPath,
                            REGULAR,
                            new RGBColor(1f, 0f, 0f));
                }
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
        if (ImGUISystem.INSTANCE.isFormVisible("FBindKeys"))
            return;

        ShortcutManager.getInstance().update();

        if (KeyHandler.isKeyJustPressed(GLFW_KEY_M)) {
            if (Sound.isMusicPlaying()) {
                Sound.stopMusic();
                Console.INSTANCE.addMsgToConsole("Musica detenida.", REGULAR, new RGBColor(1f, 1f, 1f));
            } else {
                var context = GameData.getActiveContext();
                if (context == null)
                    return;
                int musicNum = context.getMapProperties().getMusicIndex();
                if (musicNum <= 0) {
                    Console.INSTANCE.addMsgToConsole("El mapa no tiene musica configurada.", REGULAR,
                            new RGBColor(1f, 0f, 0f));
                } else {
                    String musicPath = org.argentumforge.engine.game.Options.INSTANCE.getMusicPath();
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

        if (KeyHandler.isActionKeyJustPressed(Key.RELOAD_MAP)) {
            org.argentumforge.engine.game.EditorController.INSTANCE.reloadMap();
        }

        // Atajos de velocidad
        if (!imgui.ImGui.getIO().getWantCaptureKeyboard()) {
            // Teclado numérico + para aumentar velocidad
            if (KeyHandler.isKeyPressed(GLFW_KEY_KP_ADD)) {
                org.argentumforge.engine.game.Options.INSTANCE.increaseSpeed();
            }
            // Teclado numérico - para disminuir velocidad
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

                // Comprobar Traslado de Mapa (Trigger 1) después de mover
                if (user.isWalkingmode()) {
                    var context = GameData.getActiveContext();
                    if (context == null)
                        return;

                    var charList = context.getCharList();
                    int userIdx = user.getUserCharIndex();

                    if (userIdx > 0 && userIdx < charList.length) {
                        org.argentumforge.engine.game.models.Character userChar = charList[userIdx];

                        // Asegurar que userChar sea válido antes de acceder
                        if (userChar != null) {
                            int x = userChar.getPos().getX();
                            int y = userChar.getPos().getY();
                            // Acceder a mapData desde GameData, asegurar límites
                            var mapData = context.getMapData();

                            if (mapData != null && x >= 1 && x <= 100 && y >= 1 && y <= 100) {
                                int trig = mapData[x][y].getTrigger();
                                // Registro de depuración si se encuentra trigger
                                if (trig != 0) {
                                    org.tinylog.Logger.info("Debug: Pos=(" + x + "," + y + ") Trigger=" + trig);
                                }

                                // COMPROBAR TRASLADO
                                // 1. Restablecer Ignorar si nos movimos de la casilla ignorada
                                if (transferIgnored && (x != ignoredX || y != ignoredY)) {
                                    transferIgnored = false;
                                    ignoredX = -1;
                                    ignoredY = -1;
                                }

                                int destMap = mapData[x][y].getExitMap();
                                if (destMap > 0) {
                                    org.tinylog.Logger.info("Transfer Found! DestMap=" + destMap);

                                    // 2. Mostrar Diálogo SOLO si no está ignorado
                                    if (!transferIgnored && !isTransferDialogActive) {
                                        isTransferDialogActive = true;
                                        org.argentumforge.engine.listeners.KeyHandler.resetInputs(); // Detener caminata

                                        DialogManager.getInstance().showConfirm(
                                                "Traslado de Mapa",
                                                "¿Desea viajar al Mapa " + destMap + "?",
                                                () -> {
                                                    isTransferDialogActive = false;

                                                    int destX = mapData[x][y].getExitX();
                                                    int destY = mapData[x][y].getExitY();

                                                    // Actualizar ID de Mapa del Usuario
                                                    user.setUserMap((short) destMap);

                                                    // Resolver Ruta y Cargar
                                                    String mapPath = org.argentumforge.engine.utils.MapManager
                                                            .resolveMapPath(destMap);

                                                    if (mapPath != null) {
                                                        user.removeInstanceFromMap();
                                                        org.argentumforge.engine.utils.MapManager.loadMapAsync(mapPath,
                                                                () -> {
                                                                    // Actualizar Posición del Usuario
                                                                    user.getUserPos().setX(destX);
                                                                    user.getUserPos().setY(destY);

                                                                    // Actualizar Cámara
                                                                    camera.update(destX, destY);

                                                                    // Refrescar Personaje (coloca en nueva posición)
                                                                    user.refreshUserCharacter();

                                                                    // Asegurar que el estado de movimiento esté limpio
                                                                    user.resetMovement();
                                                                    org.argentumforge.engine.listeners.KeyHandler
                                                                            .resetInputs();
                                                                });
                                                    } else {
                                                        org.argentumforge.engine.game.console.Console.INSTANCE
                                                                .addMsgToConsole(
                                                                        "Error: No se encontró el mapa " + destMap
                                                                                + " (o sus variantes)",
                                                                        org.argentumforge.engine.game.console.FontStyle.REGULAR,
                                                                        new org.argentumforge.engine.renderer.RGBColor(
                                                                                1f,
                                                                                0f, 0f));
                                                        return;
                                                    }
                                                },
                                                () -> {
                                                    isTransferDialogActive = false;
                                                    // Usuario dijo NO - IGNORAR esta casilla hasta que nos movamos
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
        var context = GameData.getActiveContext();
        if (context != null && context.getMapData() != null) {
            MapData[][] mapData = context.getMapData();
            return x >= 1 && x < mapData.length && y >= 1 && y < mapData[0].length;
        }
        return false;
    }

    /**
     * Verifica si alguna herramienta tiene un modo activo de insertar (1) o
     * eliminar (2).
     * Cuando hay herramientas activas, Shift+clic NO debe activar la selección.
     */
    private boolean hasActiveInsertOrDeleteTool() {
        // Incluimos mode 3 (Pick/Capturar) para que la selección no interfiera al
        // capturar entidades
        return surface.getMode() != 0 ||
                npc.getMode() != 0 ||
                obj.getMode() != 0 ||
                block.getMode() != 0 ||
                trigger.getMode() != 0 ||
                transfer.getMode() != 0 ||
                particle.getMode() != 0;
    }
}
