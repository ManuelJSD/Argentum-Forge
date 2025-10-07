package org.aoclient.engine.scenes;

import org.aoclient.engine.listeners.KeyHandler;
import org.aoclient.engine.renderer.Surface;
import org.aoclient.engine.renderer.Texture;

import static org.aoclient.engine.renderer.Drawn.geometryBoxRenderGUI;
import static org.aoclient.engine.utils.Time.deltaTime;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER;

/**
 * <p>
 * {@code IntroScene} es la primera escena que se muestra al jugador y esta diseñada para presentar los logos, creditos iniciales
 * y elementos visuales de introduccion antes de llegar al menu principal.
 * <p>
 * Esta escena maneja un temporizador que controla la duracion de su visualizacion y los efectos de fade-in/fade-out para las
 * imagenes mostradas. La secuencia incluye varios elementos visuales en el siguiente orden:
 * <ul>
 * <li>Logo de ArgentumForge con efecto de aparicion gradual
 * <li>Serie de imagenes de presentacion que se muestran secuencialmente
 * <li>Efecto de transicion hacia la {@code MainScene} al finalizar la secuencia
 * </ul>
 * <p>
 * El usuario puede saltar esta escena en cualquier momento presionando la tecla {@code Enter}, lo que provoca una transicion
 * inmediata hacia {@code MainScene}. Esta funcionalidad se implementa en el metodo {@link IntroScene#keyEvents()}.
 *
 * @see Scene
 * @see MainScene
 */

public final class IntroScene extends Scene {

    private float timeScene = 5.0f; // 5 segundos de intro.
    private float alphaInterface;

    private Texture ArgentumForgeLogo;

    @Override
    public void init() {
        super.init();
        this.alphaInterface = 0.0f;
        this.canChangeTo = SceneType.MAIN_SCENE;
        this.ArgentumForgeLogo = Surface.INSTANCE.createTexture("gui.ao", "argentumforge", true);
    }

    @Override
    public void mouseEvents() {
        // nothing to do..
    }

    /**
     * En caso de apretar enter: cerrame la presentacion y mostrame el conectar.
     */
    @Override
    public void keyEvents() {
        if (KeyHandler.isKeyPressed(GLFW_KEY_ENTER)) close();
    }

    /**
     * Renderiza nuestra escena y actualiza el efecto y el tiempo de presentancion.
     */
    @Override
    public void render() {
        if (!visible) return; // Si no dejamos esto, es posible que al cerrarse la escena genere un NullPointerException.
        effectArgentumForge();
        checkEndScene();
    }

    /**
     * Prepara el cierre de la escena.
     */
    @Override
    public void close() {
        this.visible = false;
    }

    /**
     * Actualiza el tiempo total de la escena y al finalizar cambia de escena.
     */
    private void checkEndScene() {
        timeScene -= deltaTime;
        if (timeScene <= 0) close();
    }

    /**
     * Efecto de logo Argentum Forge
     */
    private void effectArgentumForge() {
        alphaInterface += 0.3f * deltaTime;
        geometryBoxRenderGUI(ArgentumForgeLogo, 0, 0, alphaInterface);
    }

}
