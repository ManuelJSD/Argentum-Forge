package org.argentumforge.engine.scenes;

import org.argentumforge.engine.Window;
import org.argentumforge.engine.game.*;
import org.argentumforge.engine.game.models.Direction;
import org.argentumforge.engine.game.models.Key;
import org.argentumforge.engine.gui.ImGUISystem;
import org.argentumforge.engine.gui.forms.FMain;
import org.argentumforge.engine.gui.forms.FOptions;
import org.argentumforge.engine.listeners.KeyHandler;
import org.argentumforge.engine.listeners.MouseListener;
import org.argentumforge.engine.renderer.RenderSettings;
import org.argentumforge.engine.utils.editor.Surface;
import org.argentumforge.engine.utils.editor.Block;
import org.argentumforge.engine.utils.editor.Npc;
import org.argentumforge.engine.utils.editor.Obj;
import org.argentumforge.engine.utils.editor.Selection;
import org.argentumforge.engine.utils.editor.Trigger;
import org.argentumforge.engine.utils.editor.Transfer;
import org.argentumforge.engine.renderer.Texture;
import org.argentumforge.engine.Engine;
import org.argentumforge.engine.game.console.Console;
import org.argentumforge.engine.game.models.Character;
import org.argentumforge.engine.utils.AssetRegistry;
import org.argentumforge.engine.utils.inits.NpcData;
import org.argentumforge.engine.utils.inits.ObjData;
import org.argentumforge.engine.gui.forms.FTransferEditor;

import static org.argentumforge.engine.game.IntervalTimer.INT_SENTRPU;
import static org.argentumforge.engine.game.models.Character.drawCharacter;
import static org.argentumforge.engine.renderer.Drawn.drawTexture;
import static org.argentumforge.engine.renderer.Drawn.drawGrhIndex;
import static org.argentumforge.engine.scenes.Camera.*;
import static org.argentumforge.engine.utils.GameData.*;
import static org.argentumforge.engine.utils.AssetRegistry.*;
import static org.argentumforge.engine.utils.Time.deltaTime;
import static org.argentumforge.engine.utils.Time.timerTicksPerFrame;
import static org.argentumforge.engine.game.console.FontStyle.REGULAR;
import static org.lwjgl.glfw.GLFW.*;
import org.argentumforge.engine.renderer.RGBColor;
import org.argentumforge.engine.renderer.Drawn;
import org.argentumforge.engine.utils.editor.Selection.SelectedEntity;

/**
 * <p>
 * {@code GameScene} es la escena principal del editor, responsable de
 * renderizar el mapa
 * y gestionar las herramientas de edición en tiempo real.
 * <p>
 * Funcionalidades principales:
 * <ul>
 * <li>Renderizado del mapa con sus múltiples capas y visualización técnica
 * (bloqueos, traslados).</li>
 * <li>Gestión de las herramientas de edición (Superficies, Bloqueos, NPCs,
 * Objetos).</li>
 * <li>Visualización de NPCs y Objetos colocados en el mapa.</li>
 * <li>Control de cámara y navegación por el mundo.</li>
 * <li>Modo "Caminata" para previsualizar colisiones y movimiento.</li>
 * </ul>
 * <p>
 * El método {@link GameScene#render()} coordina el dibujado por capas para
 * garantizar
 * la correcta superposición de elementos visuales.
 *
 * @see Scene
 * @see FMain
 * @see Camera
 */

public final class GameScene extends Scene {

    /** Temporizador para controlar el intervalo de actualización de posición. */
    private final IntervalTimer intervalToUpdatePos = new IntervalTimer(INT_SENTRPU);

    /** Instancia del usuario actual (singleton). */
    private final User user = User.INSTANCE;

    /** Sistema de clima y color de ambiente. */
    private Weather weather;

    /** Acumulador de desplazamiento en X para animaciones suaves de movimiento. */
    private float offSetCounterX = 0;

    /** Acumulador de desplazamiento en Y para animaciones suaves de movimiento. */
    private float offSetCounterY = 0;

    /** Nivel de transparencia de la capa de techos (0.0f a 1.0f). */
    private float alphaCeiling = 1.0f;

    /** Formulario principal de la interfaz de usuario. */
    private FMain frmMain;

    /** Editor de superficies. */
    private Surface surface;

    /** Editor de bloqueos. */
    private Block block;

    /** Editor de NPCs. */
    private Npc npc;

    /** Editor de Objetos. */
    private Obj obj;

    /** Herramienta de seleccion y movimiento. */
    private Selection selection;

    /** Herramienta de Triggers. */
    private Trigger trigger;

    /** Herramienta de Traslados. */
    private Transfer transfer;

    /** Flag auxiliar para el borrado de capas (uso interno del editor). */
    private boolean DeleteLayer;

    private Texture whiteTexture;

    /**
     * Inicializa los componentes de la escena del juego.
     * Configura el tipo de escena de retorno, el clima, los editores y añade el
     * formulario principal a ImGui.
     */
    @Override
    public void init() {
        super.init();

        canChangeTo = SceneType.MAIN_SCENE;
        weather = Weather.INSTANCE;
        frmMain = new FMain();
        surface = Surface.getInstance();
        block = Block.getInstance();
        npc = Npc.getInstance();
        obj = Obj.getInstance();
        selection = Selection.getInstance();
        trigger = Trigger.getInstance();
        transfer = Transfer.getInstance();

        whiteTexture = new Texture();
        whiteTexture.createWhitePixel();

        ImGUISystem.INSTANCE.addFrm(frmMain);
    }

    /**
     * Actualiza la lógica y renderiza la escena.
     * Maneja la actualización del clima, los temporizadores, el desplazamiento
     * suave del personaje
     * y delega el renderizado del mapa a {@link #renderScreen}.
     */
    @Override
    public void render() {

        if (!visible)
            return;

        weather.update();
        intervalToUpdatePos.update();

        if (user.isUserMoving()) {
            if (user.getAddToUserPos().getX() != 0) {
                offSetCounterX -= charList[user.getUserCharIndex()].getWalkingSpeed() * user.getAddToUserPos().getX()
                        * timerTicksPerFrame;
                if (Math.abs(offSetCounterX) >= Math.abs(TILE_PIXEL_SIZE * user.getAddToUserPos().getX())) {
                    offSetCounterX = 0;
                    user.getAddToUserPos().setX(0);
                    user.setUserMoving(false);
                }
            }

            if (user.getAddToUserPos().getY() != 0) {
                offSetCounterY -= charList[user.getUserCharIndex()].getWalkingSpeed() * user.getAddToUserPos().getY()
                        * timerTicksPerFrame;
                if (Math.abs(offSetCounterY) >= Math.abs(TILE_PIXEL_SIZE * user.getAddToUserPos().getY())) {
                    offSetCounterY = 0;
                    user.getAddToUserPos().setY(0);
                    user.setUserMoving(false);
                }
            }
        }

        renderScreen(user.getUserPos().getX() - user.getAddToUserPos().getX(),
                user.getUserPos().getY() - user.getAddToUserPos().getY(),
                (int) (offSetCounterX), (int) (offSetCounterY));
    }

    /**
     * Escucha los eventos del mouse.
     */
    @Override
    public void mouseEvents() {
        // Bloqueamos si ImGui está capturando el ratón activamente (sobre una ventana o
        // widget)
        // EXCEPTO si solo la ventana principal FMain está activa, permitiendo
        // click-through al mapa.
        if (imgui.ImGui.getIO().getWantCaptureMouse()) {
            if (!ImGUISystem.INSTANCE.isMainLast()) {
                return;
            }
        }

        // ¿Estamos haciendo clic dentro del área de renderizado del juego?
        if (inGameArea()) {
            int x = getTileMouseX((int) MouseListener.getX() - POS_SCREEN_X);
            int y = getTileMouseY((int) MouseListener.getY() - POS_SCREEN_Y);

            // Herramienta de Selección (Drag & Drop + Marquee)
            if (selection.isActive()) {
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
                return; // Prioridad absoluta
            }

            if (MouseListener.mouseButtonDown(GLFW_MOUSE_BUTTON_LEFT)) {
                // Editar superficies o bloqueos según el modo activo
                surface.surface_edit(x, y);
                block.block_edit(x, y);
                npc.npc_edit(x, y);
                obj.obj_edit(x, y);
                trigger.trigger_edit(x, y);
                transfer.transfer_edit(x, y);
            }

            // Clic derecho para capturar coordenadas de traslado
            if (MouseListener.mouseButtonDown(GLFW_MOUSE_BUTTON_RIGHT)) {
                if (x >= 1 && x <= 100 && y >= 1 && y <= 100 && transfer.isActive() && mapData[x][y].getExitMap() > 0) {
                    int destMap = mapData[x][y].getExitMap();
                    int destX = mapData[x][y].getExitX();
                    int destY = mapData[x][y].getExitY();

                    transfer.captureCoordinates(destMap, destX, destY);
                    System.out.println("Traslado capturado: Mapa=" + destMap + " X=" + destX + " Y=" + destY);

                    // Actualizar campos del editor si está abierto
                    org.argentumforge.engine.gui.forms.FTransferEditor editor = (org.argentumforge.engine.gui.forms.FTransferEditor) ImGUISystem.INSTANCE
                            .getForm(org.argentumforge.engine.gui.forms.FTransferEditor.class);
                    if (editor != null) {
                        editor.updateInputFields(destMap, destX, destY);
                    }
                }
            }

            // Doble clic para navegar al mapa de destino
            if (MouseListener.mouseButtonDoubleClick(GLFW_MOUSE_BUTTON_LEFT)) {
                if (x >= 1 && x <= 100 && y >= 1 && y <= 100 && mapData[x][y].getExitMap() > 0) {
                    int destMap = mapData[x][y].getExitMap();
                    int destX = mapData[x][y].getExitX();
                    int destY = mapData[x][y].getExitY();

                    System.out.println("Navegando a traslado: Mapa=" + destMap + " X=" + destX + " Y=" + destY);

                    // Cargar el mapa de destino en la misma ubicación que el mapa actual
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

                        // Posicionar al usuario y la cámara en las coordenadas de destino
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
        }

        // Manejar Zoom con la rueda del ratón
        float scrollY = MouseListener.getScrollY();
        if (scrollY != 0) {
            int newSize = Camera.TILE_PIXEL_SIZE + (int) (scrollY * 4);
            Camera.setTileSize(newSize);
        }
    }

    /**
     * Escucha los eventos del teclado.
     */
    @Override
    public void keyEvents() {
        this.checkBindedKeys();
    }

    /**
     * Cierre de la escena.
     */
    @Override
    public void close() {
        visible = false;
    }

    /**
     * Verifica las teclas especiales bindeadas (Debug, Opciones, Modo Caminata,
     * etc).
     * También delega la verificación de teclas de movimiento a
     * {@link #checkWalkKeys}.
     */
    private void checkBindedKeys() {
        // Bloqueamos keyboard solo si hay un campo de texto activo (ej. buscador de la
        // paleta)
        if (imgui.ImGui.getIO().getWantTextInput())
            return;

        // Usando el metodo estatico de Key para obtener la tecla desde el codigo
        final Key key = Key.getKey(KeyHandler.getLastKeyPressed());

        checkWalkKeys();

        // Atajos de Undo/Redo (Ctrl+Z, Ctrl+Y)
        if (KeyHandler.isKeyPressed(GLFW_KEY_LEFT_CONTROL) || KeyHandler.isKeyPressed(GLFW_KEY_RIGHT_CONTROL)) {

            if (KeyHandler.isKeyJustPressed(GLFW_KEY_Z)) {
                org.argentumforge.engine.utils.editor.commands.CommandManager.getInstance().undo();
            } else if (KeyHandler.isKeyJustPressed(GLFW_KEY_Y)) {
                org.argentumforge.engine.utils.editor.commands.CommandManager.getInstance().redo();
            } else if (KeyHandler.isKeyJustPressed(GLFW_KEY_0)) {
                Camera.setTileSize(32);
            }
        }

        if (key == null)
            return; // ni me gasto si la tecla presionada no existe en nuestro bind.

        if (KeyHandler.isActionKeyJustPressed(key)) {

            switch (key) {
                case DEBUG_SHOW:
                    ImGUISystem.INSTANCE.setShowDebug(!ImGUISystem.INSTANCE.isShowDebug());
                    break;
                case SHOW_OPTIONS:
                    ImGUISystem.INSTANCE.show(new FOptions());
                    break;
                case TOGGLE_WALKING_MODE:
                    user.setWalkingmode(!user.isWalkingmode());
                    break;
                case EXIT_GAME:
                    break;
                default:
                    break;
            }
        }

    }

    /**
     * Verifica las teclas de movimiento presionadas y mueve al usuario en la
     * dirección correspondiente.
     * Solo procesa el movimiento si el usuario no se está moviendo actualmente.
     */
    private void checkWalkKeys() {
        if (!user.isUserMoving()) {

            if (KeyHandler.getEffectiveMovementKey() != -1) {
                int keyCode = KeyHandler.getEffectiveMovementKey();
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

    /**
     * Coordina el proceso completo de renderizado de la pantalla.
     * Actualiza la cámara y renderiza todas las capas del mapa en orden, seguido de
     * los diálogos,
     * techos, overlays de bloqueos y efectos climáticos.
     *
     * @param tileX        Posición X en tiles (epicentro de la cámara)
     * @param tileY        Posición Y en tiles (epicentro de la cámara)
     * @param pixelOffsetX Desplazamiento fino en X (píxeles)
     * @param pixelOffsetY Desplazamiento fino en Y (píxeles)
     */
    private void renderScreen(int tileX, int tileY, int pixelOffsetX, int pixelOffsetY) {
        camera.update(tileX, tileY);

        RenderSettings renderSettings = options.getRenderSettings();

        renderFirstLayer(renderSettings, pixelOffsetX, pixelOffsetY);
        renderSecondLayer(renderSettings, pixelOffsetX, pixelOffsetY);
        renderThirdLayer(renderSettings, pixelOffsetX, pixelOffsetY);
        renderFourthLayer(renderSettings, pixelOffsetX, pixelOffsetY);
        renderBlockOverlays(renderSettings, pixelOffsetX, pixelOffsetY);
        // renderTriggerOverlays removed - called via FMain/ImGui now
        renderTranslationOverlays(renderSettings, pixelOffsetX, pixelOffsetY);
        renderEditorPreviews(pixelOffsetX, pixelOffsetY);
    }

    /**
     * Renderiza la primera capa del mapa (capa base/suelo).
     * Esta es la capa más baja y contiene los gráficos del terreno base.
     *
     * @param pixelOffsetX Desplazamiento en píxeles en el eje X para animaciones de
     *                     movimiento
     * @param pixelOffsetY Desplazamiento en píxeles en el eje Y para animaciones de
     *                     movimiento
     */
    private void renderFirstLayer(RenderSettings renderSettings, final int pixelOffsetX, final int pixelOffsetY) {
        // Si la visualización de la Capa 1 esta activa...
        if (renderSettings.getShowLayer()[0]) {
            camera.setScreenY(camera.getMinYOffset() - TILE_BUFFER_SIZE);
            for (int y = camera.getMinY(); y <= camera.getMaxY(); y++) {
                camera.setScreenX(camera.getMinXOffset() - TILE_BUFFER_SIZE);
                for (int x = camera.getMinX(); x <= camera.getMaxX(); x++) {
                    if (mapData[x][y].getLayer(1).getGrhIndex() != 0) {
                        drawTexture(mapData[x][y].getLayer(1),
                                POS_SCREEN_X + camera.getScreenX() * TILE_PIXEL_SIZE + pixelOffsetX,
                                POS_SCREEN_Y + camera.getScreenY() * TILE_PIXEL_SIZE + pixelOffsetY,
                                true, true, false, 1.0f, weather.getWeatherColor());
                    }

                    camera.incrementScreenX();
                }
                camera.incrementScreenY();
            }
        }
    }

    /**
     * Renderiza la segunda capa del mapa.
     * Incluye elementos decorativos y objetos de tamaño 32x32 píxeles.
     *
     * @param pixelOffsetX Desplazamiento en píxeles en el eje X para animaciones de
     *                     movimiento
     * @param pixelOffsetY Desplazamiento en píxeles en el eje Y para animaciones de
     *                     movimiento
     */
    private void renderSecondLayer(RenderSettings renderSettings, final int pixelOffsetX, final int pixelOffsetY) {
        if (!renderSettings.getShowLayer()[1] && !renderSettings.getShowOJBs())
            return;

        camera.setScreenY(camera.getMinYOffset() - TILE_BUFFER_SIZE);
        for (int y = camera.getMinY(); y <= camera.getMaxY(); y++) {
            camera.setScreenX(camera.getMinXOffset() - TILE_BUFFER_SIZE);
            for (int x = camera.getMinX(); x <= camera.getMaxX(); x++) {
                if (renderSettings.getShowLayer()[1]) {
                    if (mapData[x][y].getLayer(2).getGrhIndex() != 0) {
                        drawTexture(mapData[x][y].getLayer(2),
                                POS_SCREEN_X + camera.getScreenX() * TILE_PIXEL_SIZE + pixelOffsetX,
                                POS_SCREEN_Y + camera.getScreenY() * TILE_PIXEL_SIZE + pixelOffsetY,
                                true, true, false, 1.0f, weather.getWeatherColor());
                    }
                }

                if (renderSettings.getShowOJBs()) {
                    if (mapData[x][y].getObjGrh().getGrhIndex() != 0) {
                        if (grhData[mapData[x][y].getObjGrh().getGrhIndex()].getPixelWidth() == TILE_PIXEL_SIZE &&
                                grhData[mapData[x][y].getObjGrh().getGrhIndex()].getPixelHeight() == TILE_PIXEL_SIZE) {

                            // No renderizar el objeto original si se está arrastrando desde esta posición
                            boolean isDragged = false;
                            if (selection.isDragging()) {
                                for (SelectedEntity se : selection.getSelectedEntities()) {
                                    if (se.x == x && se.y == y && se.type == Selection.EntityType.OBJECT) {
                                        isDragged = true;
                                        break;
                                    }
                                }
                            }

                            if (!isDragged) {
                                drawTexture(mapData[x][y].getObjGrh(),
                                        POS_SCREEN_X + camera.getScreenX() * TILE_PIXEL_SIZE + pixelOffsetX,
                                        POS_SCREEN_Y + camera.getScreenY() * TILE_PIXEL_SIZE + pixelOffsetY,
                                        true, true, false, 1.0f, weather.getWeatherColor());
                            }
                        }
                    }
                }

                camera.incrementScreenX();
            }
            camera.incrementScreenY();
        }
    }

    /**
     * Renderiza la tercera capa del mapa.
     * Incluye personajes, NPCs y objetos de tamaño mayor a 32x32 píxeles.
     * En modo cámara libre, oculta el personaje del usuario pero mantiene visibles
     * los NPCs.
     *
     * @param pixelOffsetX Desplazamiento en píxeles en el eje X para animaciones de
     *                     movimiento
     * @param pixelOffsetY Desplazamiento en píxeles en el eje Y para animaciones de
     *                     movimiento
     */
    private void renderThirdLayer(RenderSettings renderSettings, final int pixelOffsetX, final int pixelOffsetY) {
        // LAYER 3, CHARACTERS & OBJECTS > 32x32
        camera.setScreenY(camera.getMinYOffset() - TILE_BUFFER_SIZE);
        for (int y = camera.getMinY(); y <= camera.getMaxY(); y++) {
            camera.setScreenX(camera.getMinXOffset() - TILE_BUFFER_SIZE);
            for (int x = camera.getMinX(); x <= camera.getMaxX(); x++) {

                if (renderSettings.getShowOJBs()) {
                    if (mapData[x][y].getObjGrh().getGrhIndex() != 0) {
                        if (grhData[mapData[x][y].getObjGrh().getGrhIndex()].getPixelWidth() != TILE_PIXEL_SIZE &&
                                grhData[mapData[x][y].getObjGrh().getGrhIndex()].getPixelHeight() != TILE_PIXEL_SIZE) {

                            // No renderizar el objeto original si se está arrastrando desde esta posición
                            boolean isDragged = false;
                            if (selection.isDragging()) {
                                for (SelectedEntity se : selection.getSelectedEntities()) {
                                    if (se.x == x && se.y == y && se.type == Selection.EntityType.OBJECT) {
                                        isDragged = true;
                                        break;
                                    }
                                }
                            }

                            if (!isDragged) {
                                drawTexture(mapData[x][y].getObjGrh(),
                                        POS_SCREEN_X + camera.getScreenX() * TILE_PIXEL_SIZE + pixelOffsetX,
                                        POS_SCREEN_Y + camera.getScreenY() * TILE_PIXEL_SIZE + pixelOffsetY,
                                        true, true, false, 1.0f, weather.getWeatherColor());
                            }
                        }
                    }
                }

                // Solo renderizar personajes cuando el modo caminata está activo, o renderizar
                // NPCs siempre
                if (mapData[x][y].getCharIndex() != 0) {
                    final int charIndex = mapData[x][y].getCharIndex();

                    // No renderizar el NPC original si se está arrastrando desde esta posición
                    boolean isDragged = false;
                    if (selection.isDragging()) {
                        for (SelectedEntity se : selection.getSelectedEntities()) {
                            if (se.x == x && se.y == y && se.type == Selection.EntityType.NPC) {
                                isDragged = true;
                                break;
                            }
                        }
                    }

                    if (!isDragged) {
                        final boolean isUserChar = charIndex == user.getUserCharIndex();
                        if (isUserChar) {
                            if (user.isWalkingmode()) {
                                drawCharacter(charIndex,
                                        POS_SCREEN_X + camera.getScreenX() * TILE_PIXEL_SIZE + pixelOffsetX,
                                        POS_SCREEN_Y + camera.getScreenY() * TILE_PIXEL_SIZE + pixelOffsetY,
                                        1.0f,
                                        weather.getWeatherColor());
                            }
                        } else {
                            if (renderSettings.getShowNPCs()) {
                                drawCharacter(charIndex,
                                        POS_SCREEN_X + camera.getScreenX() * TILE_PIXEL_SIZE + pixelOffsetX,
                                        POS_SCREEN_Y + camera.getScreenY() * TILE_PIXEL_SIZE + pixelOffsetY,
                                        1.0f,
                                        weather.getWeatherColor());
                            }
                        }
                    }
                }

                if (renderSettings.getShowLayer()[2]) {
                    if (mapData[x][y].getLayer(3).getGrhIndex() != 0) {
                        drawTexture(mapData[x][y].getLayer(3),
                                POS_SCREEN_X + camera.getScreenX() * TILE_PIXEL_SIZE + pixelOffsetX,
                                POS_SCREEN_Y + camera.getScreenY() * TILE_PIXEL_SIZE + pixelOffsetY,
                                true, true, false, 1.0f, weather.getWeatherColor());
                    }
                }

                camera.incrementScreenX();
            }
            camera.incrementScreenY();
        }
    }

    /**
     * Renderiza la cuarta capa del mapa (techos).
     * Esta capa se desvanece cuando el usuario está debajo de un techo para mejorar
     * la visibilidad.
     * El nivel de transparencia es controlado por {@code alphaCeiling}.
     *
     * @param pixelOffsetX Desplazamiento en píxeles en el eje X para animaciones de
     *                     movimiento
     * @param pixelOffsetY Desplazamiento en píxeles en el eje Y para animaciones de
     *                     movimiento
     */
    private void renderFourthLayer(RenderSettings renderSettings, final int pixelOffsetX, final int pixelOffsetY) {
        if (renderSettings.getShowLayer()[3]) {
            this.checkEffectCeiling();
            if (alphaCeiling > 0.0f) {
                camera.setScreenY(camera.getMinYOffset() - TILE_BUFFER_SIZE);
                for (int y = camera.getMinY(); y <= camera.getMaxY(); y++) {
                    camera.setScreenX(camera.getMinXOffset() - TILE_BUFFER_SIZE);
                    for (int x = camera.getMinX(); x <= camera.getMaxX(); x++) {

                        if (mapData[x][y].getLayer(4).getGrhIndex() > 0) {
                            drawTexture(mapData[x][y].getLayer(4),
                                    POS_SCREEN_X + camera.getScreenX() * TILE_PIXEL_SIZE + pixelOffsetX,
                                    POS_SCREEN_Y + camera.getScreenY() * TILE_PIXEL_SIZE + pixelOffsetY,
                                    true, true, false, alphaCeiling, weather.getWeatherColor());
                        }

                        camera.incrementScreenX();
                    }
                    camera.incrementScreenY();
                }
            }
        }
    }

    /**
     * Renderiza overlays rojos sobre los tiles bloqueados del
     * mapa.
     * Solo se renderiza cuando el modo de visualización de bloqueos está activado.
     */
    private void renderBlockOverlays(RenderSettings renderSettings, final int pixelOffsetX, final int pixelOffsetY) {
        if (renderSettings.getShowBlock()) {
            // Grafico que vamos a utilizar para representar los bloqueos
            int grhBlock = 4;

            camera.setScreenY(camera.getMinYOffset() - TILE_BUFFER_SIZE);
            for (int y = camera.getMinY(); y <= camera.getMaxY(); y++) {
                camera.setScreenX(camera.getMinXOffset() - TILE_BUFFER_SIZE);
                for (int x = camera.getMinX(); x <= camera.getMaxX(); x++) {

                    // Si el tile está bloqueado, dibujamos el Grh 4 con opacidad variable
                    if (mapData[x][y].getBlocked()) {
                        org.argentumforge.engine.renderer.Drawn.drawGrhIndex(grhBlock,
                                POS_SCREEN_X + camera.getScreenX() * TILE_PIXEL_SIZE + pixelOffsetX,
                                POS_SCREEN_Y + camera.getScreenY() * TILE_PIXEL_SIZE + pixelOffsetY,
                                renderSettings.getBlockOpacity(),
                                null);
                    }

                    camera.incrementScreenX();
                }
                camera.incrementScreenY();
            }
        }
    }

    /**
     * Renderiza overlays amarillos sobre los tiles con triggers.
     * Muestra el ID del trigger si es posible.
     */
    public void renderImGuiOverlays() {
        RenderSettings renderSettings = org.argentumforge.engine.game.Options.INSTANCE.getRenderSettings();
        // Renderizar si la opcion esta activa O si la herramienta de edicion esta
        // activa
        if (org.argentumforge.engine.utils.editor.Trigger.getInstance().isActive()
                || renderSettings.getShowTriggers()) {

            // Recalcular offsets para ImGui overlay
            int pixelOffsetX = (int) offSetCounterX;
            int pixelOffsetY = (int) offSetCounterY;

            camera.setScreenY(camera.getMinYOffset() - TILE_BUFFER_SIZE);

            // Usamos el listado de dibujo de fondo para que los números queden detrás de
            // las ventanas UI
            imgui.ImDrawList drawList = imgui.ImGui.getBackgroundDrawList();

            for (int y = camera.getMinY(); y <= camera.getMaxY(); y++) {
                camera.setScreenX(camera.getMinXOffset() - TILE_BUFFER_SIZE);
                for (int x = camera.getMinX(); x <= camera.getMaxX(); x++) {

                    if (mapData[x][y].getTrigger() > 0) {
                        int screenX = POS_SCREEN_X + camera.getScreenX() * TILE_PIXEL_SIZE + pixelOffsetX;
                        int screenY = POS_SCREEN_Y + camera.getScreenY() * TILE_PIXEL_SIZE + pixelOffsetY;

                        String idText = String.valueOf(mapData[x][y].getTrigger());
                        float textWidth = imgui.ImGui.calcTextSize(idText).x;
                        float textX = screenX + (TILE_PIXEL_SIZE - textWidth) / 2;
                        float textY = screenY + (TILE_PIXEL_SIZE - 14) / 2;

                        // Sombra Negra (offset +1)
                        drawList.addText(textX + 1, textY + 1, 0xFF000000, idText);
                        // Texto Blanco (con opacidad completa)
                        drawList.addText(textX, textY, 0xFFFFFFFF, idText);
                    }
                    camera.incrementScreenX();
                }
                camera.incrementScreenY();
            }
        }
    }

    /**
     * Renderiza overlays rojos sobre los tiles de traslado del mapa
     * Solo se renderiza cuando el modo de visualización de traslados está activado.
     */
    private void renderTranslationOverlays(RenderSettings renderSettings, final int pixelOffsetX,
            final int pixelOffsetY) {
        if (renderSettings.getShowMapTransfer()) {
            // Grafico que vamos a utilizar para representar los traslados
            int grhTrans = 3;

            camera.setScreenY(camera.getMinYOffset() - TILE_BUFFER_SIZE);
            for (int y = camera.getMinY(); y <= camera.getMaxY(); y++) {
                camera.setScreenX(camera.getMinXOffset() - TILE_BUFFER_SIZE);
                for (int x = camera.getMinX(); x <= camera.getMaxX(); x++) {

                    // Si el tile está bloqueado, dibujamos el Grh 4
                    if (mapData[x][y].getExitMap() > 0) {
                        drawGrhIndex(grhTrans,
                                POS_SCREEN_X + camera.getScreenX() * TILE_PIXEL_SIZE + pixelOffsetX,
                                POS_SCREEN_Y + camera.getScreenY() * TILE_PIXEL_SIZE + pixelOffsetY,
                                null);
                    }

                    camera.incrementScreenX();
                }
                camera.incrementScreenY();
            }
        }
    }

    /**
     * Detecta si el usuario esta debajo del techo. Si es asi, se desvanecera y en
     * caso contrario re aparece.
     */
    private void checkEffectCeiling() {
        if (user.isUnderCeiling()) {
            if (alphaCeiling > 0.0f)
                alphaCeiling -= 0.5f * deltaTime;
        } else {
            if (alphaCeiling < 1.0f)
                alphaCeiling += 0.5f * deltaTime;
        }
    }

    /**
     * Detecta si tenemos el mouse adentro del "render MainViewPic".
     */
    public static boolean inGameArea() {
        if (MouseListener.getX() < POS_SCREEN_X || MouseListener.getX() > POS_SCREEN_X + Window.SCREEN_WIDTH)
            return false;
        if (MouseListener.getY() < POS_SCREEN_Y || MouseListener.getY() > POS_SCREEN_Y + Window.SCREEN_HEIGHT)
            return false;
        return true;
    }

    /**
     * @param mouseX: Posicion X del mouse en la pantalla
     * @return: Devuelve la posicion en tile del eje X del mouse. Se utiliza al
     *          hacer click izquierdo por el mapa, para
     *          interactuar con NPCs, etc.
     */
    public static int getTileMouseX(int mouseX) {
        return (User.INSTANCE.getUserPos().getX() + mouseX / Camera.TILE_PIXEL_SIZE - Camera.HALF_WINDOW_TILE_WIDTH);
    }

    /**
     * @param mouseY: Posicion X del mouse en la pantalla
     * @return: Devuelve la posicion en tile del eje Y del mouse. Se utiliza al
     *          hacer click izquierdo por el mapa, para
     *          interactuar con NPCs, etc.
     */
    public static int getTileMouseY(int mouseY) {
        return (User.INSTANCE.getUserPos().getY() + mouseY / Camera.TILE_PIXEL_SIZE - Camera.HALF_WINDOW_TILE_HEIGHT);
    }

    /**
     * Renderiza una previsualización de lo que el usuario está a punto de colocar.
     */
    private void renderEditorPreviews(int pixelOffsetX, int pixelOffsetY) {
        if (!inGameArea())
            return;

        int mouseX = (int) MouseListener.getX() - POS_SCREEN_X;
        int mouseY = (int) MouseListener.getY() - POS_SCREEN_Y;
        int tileX = getTileMouseX(mouseX);
        int tileY = getTileMouseY(mouseY);

        // Previsualizar Superficies
        if (surface.getMode() == 1 && surface.getSurfaceIndex() > 0) {
            int half = surface.getBrushSize() / 2;
            float alpha = options.getRenderSettings().getGhostOpacity();

            if (surface.isUseMosaic() && (surface.getMosaicWidth() > 1 || surface.getMosaicHeight() > 1)) {
                // Modo Estampado
                for (int dx = 0; dx < surface.getMosaicWidth(); dx++) {
                    for (int dy = 0; dy < surface.getMosaicHeight(); dy++) {
                        int mapX = tileX + dx;
                        int mapY = tileY + dy;
                        drawPreviewGrh((short) (surface.getSurfaceIndex() + (dy * surface.getMosaicWidth()) + dx), mapX,
                                mapY, pixelOffsetX, pixelOffsetY, alpha);
                    }
                }
            } else {
                // Modo Pincel Normal
                for (int i = tileX - half; i <= tileX + half; i++) {
                    for (int j = tileY - half; j <= tileY + half; j++) {
                        if (surface.getBrushShape() == Surface.BrushShape.CIRCLE
                                && (Math.pow(i - tileX, 2) + Math.pow(j - tileY, 2) > Math.pow(half, 2)))
                            continue;

                        short targetGrh = (short) surface.getSurfaceIndex();
                        if (surface.getMosaicWidth() > 1 || surface.getMosaicHeight() > 1) {
                            targetGrh = (short) (surface.getSurfaceIndex()
                                    + ((j % surface.getMosaicHeight()) * surface.getMosaicWidth())
                                    + (i % surface.getMosaicWidth()));
                        }
                        drawPreviewGrh(targetGrh, i, j, pixelOffsetX, pixelOffsetY, alpha);
                    }
                }
            }
        }

        // Previsualizar NPCs
        if (npc.getMode() == 1 && npc.getNpcNumber() > 0) {
            NpcData data = AssetRegistry.npcs.get(npc.getNpcNumber());
            if (data != null) {
                int screenX = POS_SCREEN_X
                        + (tileX - camera.getMinX() + camera.getMinXOffset() - TILE_BUFFER_SIZE) * TILE_PIXEL_SIZE
                        + pixelOffsetX;
                int screenY = POS_SCREEN_Y
                        + (tileY - camera.getMinY() + camera.getMinYOffset() - TILE_BUFFER_SIZE) * TILE_PIXEL_SIZE
                        + pixelOffsetY;
                Character.drawCharacterGhost(data.getBody(), data.getHead(),
                        screenX, screenY, options.getRenderSettings().getGhostOpacity(), weather.getWeatherColor());
            }
        }

        // Renderizar Resaltado de Selección
        if (selection.isActive() && !selection.getSelectedEntities().isEmpty()) {
            for (SelectedEntity se : selection.getSelectedEntities()) {
                int sTileX = se.x;
                int sTileY = se.y;

                int screenX = POS_SCREEN_X
                        + (sTileX - camera.getMinX() + camera.getMinXOffset() - TILE_BUFFER_SIZE) * TILE_PIXEL_SIZE
                        + pixelOffsetX;
                int screenY = POS_SCREEN_Y
                        + (sTileY - camera.getMinY() + camera.getMinYOffset() - TILE_BUFFER_SIZE) * TILE_PIXEL_SIZE
                        + pixelOffsetY;

                Engine.batch.draw(whiteTexture, screenX, screenY, 0, 0, 1, 1, TILE_PIXEL_SIZE, TILE_PIXEL_SIZE, true,
                        0.2f,
                        new RGBColor(0.0f, 1.0f, 0.0f));
            }
        }

        // Previsualizar Objetos
        if (obj.getMode() == 1 && obj.getObjNumber() > 0) {
            ObjData data = AssetRegistry.objs.get(obj.getObjNumber());
            if (data != null) {
                drawPreviewGrh((short) data.getGrhIndex(), tileX, tileY, pixelOffsetX, pixelOffsetY,
                        options.getRenderSettings().getGhostOpacity());
            }
        }

        // Previsualizar Arrastre (Seleccion Múltiple)
        if (selection.isDragging()) {
            for (SelectedEntity se : selection.getSelectedEntities()) {
                int dragTileX = tileX + se.offsetX;
                int dragTileY = tileY + se.offsetY;

                if (se.type == Selection.EntityType.NPC) {
                    NpcData data = AssetRegistry.npcs.get(se.id);
                    if (data != null) {
                        int screenX = POS_SCREEN_X
                                + (dragTileX - camera.getMinX() + camera.getMinXOffset() - TILE_BUFFER_SIZE)
                                        * TILE_PIXEL_SIZE
                                + pixelOffsetX;
                        int screenY = POS_SCREEN_Y
                                + (dragTileY - camera.getMinY() + camera.getMinYOffset() - TILE_BUFFER_SIZE)
                                        * TILE_PIXEL_SIZE
                                + pixelOffsetY;
                        Character.drawCharacterGhost(data.getBody(),
                                data.getHead(),
                                screenX, screenY, options.getRenderSettings().getGhostOpacity(),
                                weather.getWeatherColor());
                    }
                } else if (se.type == Selection.EntityType.OBJECT) {
                    drawPreviewGrh((short) se.id, dragTileX, dragTileY, pixelOffsetX, pixelOffsetY,
                            options.getRenderSettings().getGhostOpacity());
                } else if (se.type == Selection.EntityType.TILE) {
                    // Si arrastramos un TILE, deberíamos ver sus capas?
                    // En Selection.tryGrab para TILE id es 0.
                    // Si queremos fantasmas de tiles arrastrados, necesitaríamos los datos de capas
                    // en SelectedEntity.
                    // Para simplificar ahora, dibujamos un recuadro fantasma.
                    int screenX = POS_SCREEN_X
                            + (dragTileX - camera.getMinX() + camera.getMinXOffset() - TILE_BUFFER_SIZE)
                                    * TILE_PIXEL_SIZE
                            + pixelOffsetX;
                    int screenY = POS_SCREEN_Y
                            + (dragTileY - camera.getMinY() + camera.getMinYOffset() - TILE_BUFFER_SIZE)
                                    * TILE_PIXEL_SIZE
                            + pixelOffsetY;
                    Engine.batch.draw(whiteTexture, screenX, screenY, 0, 0, 1, 1, TILE_PIXEL_SIZE, TILE_PIXEL_SIZE,
                            true,
                            0.3f, new RGBColor(1.0f, 1.0f, 1.0f));
                }
            }
        }

        // Renderizar Cuadro de Selección (Marquee)
        if (selection.isAreaSelecting()) {
            int x1 = selection.getMarqueeStartX();
            int y1 = selection.getMarqueeStartY();
            int x2 = selection.getMarqueeEndX();
            int y2 = selection.getMarqueeEndY();

            int minX = Math.min(x1, x2);
            int maxX = Math.max(x1, x2);
            int minY = Math.min(y1, y2);
            int maxY = Math.max(y1, y2);

            int screenX = POS_SCREEN_X
                    + (minX - camera.getMinX() + camera.getMinXOffset() - TILE_BUFFER_SIZE) * TILE_PIXEL_SIZE
                    + pixelOffsetX;
            int screenY = POS_SCREEN_Y
                    + (minY - camera.getMinY() + camera.getMinYOffset() - TILE_BUFFER_SIZE) * TILE_PIXEL_SIZE
                    + pixelOffsetY;
            int width = (maxX - minX + 1) * TILE_PIXEL_SIZE;
            int height = (maxY - minY + 1) * TILE_PIXEL_SIZE;

            Engine.batch.draw(whiteTexture, screenX, screenY, 0, 0, 1, 1, width, height, true, 0.3f,
                    new RGBColor(0.2f, 0.5f, 1.0f));
        }
    }

    private void drawPreviewGrh(short grhIndex, int tileX, int tileY, int pixelOffsetX, int pixelOffsetY, float alpha) {
        if (mapData == null || tileX < 0 || tileX >= mapData.length || tileY < 0 || tileY >= mapData[0].length)
            return;

        int screenX = POS_SCREEN_X
                + (tileX - camera.getMinX() + camera.getMinXOffset() - TILE_BUFFER_SIZE) * TILE_PIXEL_SIZE
                + pixelOffsetX;
        int screenY = POS_SCREEN_Y
                + (tileY - camera.getMinY() + camera.getMinYOffset() - TILE_BUFFER_SIZE) * TILE_PIXEL_SIZE
                + pixelOffsetY;

        // Aplicar centrado para GRHs grandes (mismo sistema que Drawn.drawTexture)
        if (grhIndex > 0 && grhIndex < grhData.length && grhData[grhIndex] != null) {
            if (grhData[grhIndex].getTileWidth() != 1.0f) {
                screenX = screenX - (int) (grhData[grhIndex].getTileWidth() * TILE_PIXEL_SIZE / 2)
                        + TILE_PIXEL_SIZE / 2;
            }
            if (grhData[grhIndex].getTileHeight() != 1.0f) {
                screenY = screenY - (int) (grhData[grhIndex].getTileHeight() * TILE_PIXEL_SIZE) + TILE_PIXEL_SIZE;
            }
        }

        drawGrhIndex(grhIndex, screenX, screenY, alpha,
                weather.getWeatherColor());
    }
}