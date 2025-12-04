package org.argentumforge.engine.scenes;

import org.argentumforge.engine.renderer.RGBColor;

/**
 * Clase abstracta que define la estructura base para las escenas del juego.
 * <p>
 * En Argentum Online, una escena representa una pantalla o estado especifico
 * del juego como la introduccion, menu principal o la
 * pantalla de juego principal. Esta clase proporciona la infraestructura
 * compartida para todas las escenas, mientras que cada
 * escena concreta implementa su comportamiento especifico.
 * <p>
 * El sistema de escenas permite la transicion fluida entre diferentes estados
 * del juego, donde cada escena maneja su propio ciclo
 * de vida, incluyendo inicializacion, eventos de entrada, renderizado y cierre.
 * Las escenas pueden indicar a cual otra escena
 * pueden cambiar, facilitando el flujo de navegacion del usuario.
 * <p>
 * Escenas principales implementadas:
 * <ul>
 * <li>{@code IntroScene} - Pantalla de presentacion inicial
 * <li>{@code MainScene} - Menu principal con opciones de conexion
 * <li>{@code GameScene} - Escena principal de juego donde ocurre la accion
 * </ul>
 *
 * @see SceneType
 * @see IntroScene
 * @see MainScene
 * @see GameScene
 */

public abstract class Scene {

    protected RGBColor background;
    protected Camera camera;
    protected boolean visible = false;
    protected SceneType canChangeTo; // posible cambio de escena.

    /**
     * Inicializa la escena. No se crea un constructor ya que cada escena distinta
     * puede estar compuesta por distintos
     * atributos...
     */
    public void init() {
        this.visible = true;
        this.camera = new Camera();
        this.background = new RGBColor(0.0f, 0.0f, 0.0f);
    }

    /**
     * Escucha cada del mouse, segun lo definido en el MouseListener.
     */
    public abstract void mouseEvents();

    /**
     * Escucha cada evento del teclado, segun lo definido en el KeyHandler.
     */
    public abstract void keyEvents();

    public abstract void render();

    public abstract void close();

    public RGBColor getBackground() {
        return background;
    }

    public boolean isVisible() {
        return visible;
    }

    public SceneType getChangeScene() {
        return canChangeTo;
    }

    /**
     * Devuelve el ancho preferido para esta escena.
     * Por defecto, usa la resolución configurada por el usuario.
     * Las escenas pueden sobrescribir este método para usar una resolución fija.
     */
    public int getPreferredWidth() {
        return org.argentumforge.engine.game.Options.INSTANCE.getScreenWidth();
    }

    /**
     * Devuelve la altura preferida para esta escena.
     * Por defecto, usa la resolución configurada por el usuario.
     * Las escenas pueden sobrescribir este método para usar una resolución fija.
     */
    public int getPreferredHeight() {
        return org.argentumforge.engine.game.Options.INSTANCE.getScreenHeight();
    }

}
