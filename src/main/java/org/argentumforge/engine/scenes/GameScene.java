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

import static org.argentumforge.engine.game.IntervalTimer.INT_SENTRPU;
import static org.argentumforge.engine.game.models.Character.drawCharacter;
import static org.argentumforge.engine.renderer.Drawn.drawTexture;
import static org.argentumforge.engine.renderer.Drawn.drawGrhIndex;
import static org.argentumforge.engine.scenes.Camera.*;
import static org.argentumforge.engine.utils.GameData.*;
import static org.argentumforge.engine.utils.AssetRegistry.grhData;
import static org.argentumforge.engine.utils.Time.deltaTime;
import static org.argentumforge.engine.utils.Time.timerTicksPerFrame;
import static org.lwjgl.glfw.GLFW.*;

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

    /** Flag auxiliar para el borrado de capas (uso interno del editor). */
    private boolean DeleteLayer;

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
        if (!ImGUISystem.INSTANCE.isMainLast() && !ImGUISystem.INSTANCE.isFormVisible("FSurfaceEditor")
                && !ImGUISystem.INSTANCE.isFormVisible("FBlockEditor")
                && !ImGUISystem.INSTANCE.isFormVisible("FNpcEditor")
                && !ImGUISystem.INSTANCE.isFormVisible("FObjEditor")
                && !ImGUISystem.INSTANCE.isFormVisible("FMinimap"))
            return;

        // ¿Estamos haciendo clic dentro del área de renderizado del juego?
        if (inGameArea()) {
            if (MouseListener.mouseButtonDown(GLFW_MOUSE_BUTTON_LEFT)) {

                int x = getTileMouseX((int) MouseListener.getX() - POS_SCREEN_X);
                int y = getTileMouseY((int) MouseListener.getY() - POS_SCREEN_Y);

                // Editar superficies o bloqueos según el modo activo
                surface.surface_edit(x, y);
                block.block_edit(x, y);
                npc.npc_edit(x, y);
                obj.obj_edit(x, y);

            }

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
        if ((!ImGUISystem.INSTANCE.isMainLast() && !ImGUISystem.INSTANCE.isFormVisible("FSurfaceEditor")
                && !ImGUISystem.INSTANCE.isFormVisible("FBlockEditor")
                && !ImGUISystem.INSTANCE.isFormVisible("FNpcEditor")
                && !ImGUISystem.INSTANCE.isFormVisible("FObjEditor")
                && !ImGUISystem.INSTANCE.isFormVisible("FMinimap")))
            return;

        // Usando el metodo estatico de Key para obtener la tecla desde el codigo
        final Key key = Key.getKey(KeyHandler.getLastKeyPressed());

        checkWalkKeys();

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
        renderTranslationOverlays(renderSettings, pixelOffsetX, pixelOffsetY);
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
                            drawTexture(mapData[x][y].getObjGrh(),
                                    POS_SCREEN_X + camera.getScreenX() * TILE_PIXEL_SIZE + pixelOffsetX,
                                    POS_SCREEN_Y + camera.getScreenY() * TILE_PIXEL_SIZE + pixelOffsetY,
                                    true, true, false, 1.0f, weather.getWeatherColor());
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

                            drawTexture(mapData[x][y].getObjGrh(),
                                    POS_SCREEN_X + camera.getScreenX() * TILE_PIXEL_SIZE + pixelOffsetX,
                                    POS_SCREEN_Y + camera.getScreenY() * TILE_PIXEL_SIZE + pixelOffsetY,
                                    true, true, false, 1.0f, weather.getWeatherColor());
                        }
                    }
                }

                // Solo renderizar personajes cuando el modo caminata está activo, o renderizar
                // NPCs siempre
                if (mapData[x][y].getCharIndex() != 0) {
                    final int charIndex = mapData[x][y].getCharIndex();
                    final boolean isUserChar = charIndex == user.getUserCharIndex();
                    if (isUserChar) {
                        if (user.isWalkingmode()) {
                            drawCharacter(charIndex,
                                    POS_SCREEN_X + camera.getScreenX() * TILE_PIXEL_SIZE + pixelOffsetX,
                                    POS_SCREEN_Y + camera.getScreenY() * TILE_PIXEL_SIZE + pixelOffsetY,
                                    weather.getWeatherColor());
                        }
                    } else {
                        if (renderSettings.getShowNPCs()) {
                            drawCharacter(charIndex,
                                    POS_SCREEN_X + camera.getScreenX() * TILE_PIXEL_SIZE + pixelOffsetX,
                                    POS_SCREEN_Y + camera.getScreenY() * TILE_PIXEL_SIZE + pixelOffsetY,
                                    weather.getWeatherColor());
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

                    // Si el tile está bloqueado, dibujamos el Grh 4
                    if (mapData[x][y].getBlocked()) {
                        drawGrhIndex(grhBlock,
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
    private boolean inGameArea() {
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
    private byte getTileMouseX(int mouseX) {
        return (byte) (user.getUserPos().getX() + mouseX / TILE_PIXEL_SIZE - HALF_WINDOW_TILE_WIDTH);
    }

    /**
     * @param mouseY: Posicion X del mouse en la pantalla
     * @return: Devuelve la posicion en tile del eje Y del mouse. Se utiliza al
     *          hacer click izquierdo por el mapa, para
     *          interactuar con NPCs, etc.
     */
    private byte getTileMouseY(int mouseY) {
        return (byte) (user.getUserPos().getY() + mouseY / TILE_PIXEL_SIZE - HALF_WINDOW_TILE_HEIGHT);
    }

}
