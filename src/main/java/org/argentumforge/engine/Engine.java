package org.argentumforge.engine;

import org.argentumforge.engine.audio.Sound;
import org.argentumforge.engine.gui.ImGUISystem;
import org.argentumforge.engine.listeners.KeyHandler;
import org.argentumforge.engine.listeners.MouseListener;
import org.argentumforge.engine.renderer.BatchRenderer;
import org.argentumforge.engine.renderer.Surface;
import org.argentumforge.engine.scenes.*;
import org.argentumforge.engine.utils.GameData;
import org.argentumforge.engine.utils.Time;
import org.lwjgl.Version;
import org.tinylog.Logger;

import static org.argentumforge.engine.scenes.SceneType.INTRO_SCENE;
import static org.argentumforge.engine.utils.GameData.options;
import static org.argentumforge.engine.utils.Time.deltaTime;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.opengl.GL11.*;

/**
 * Motor gráfico principal.
 * Gestiona el ciclo de vida de la aplicación: inicialización de recursos
 * (LWJGL, ImGui),
 * bucle principal de renderizado y lógica, y limpieza al cerrar.
 */

public final class Engine {

    /** Flag que indica si el programa esta corriendo. */
    private static boolean prgRun = true;
    /** Versión del programa. */
    public static final String VERSION = "1.0.0-beta";
    /** Ventana principal del motor grafico. */
    private final Window window = Window.INSTANCE;
    /** Sistema de interfaz grafica de usuario. */
    private final ImGUISystem guiSystem = ImGUISystem.INSTANCE;
    /** Escena actual que esta siendo renderizada y actualizada en el motor. */
    private static Scene currentScene;
    /** Renderizador por lotes para el dibujado eficiente de graficos. */
    public static BatchRenderer batch;
    /** Flag que indica si el motor está esperando la configuración inicial. */
    private static boolean isWaitingForSetup = false;

    public static Scene getCurrentScene() {
        return currentScene;
    }

    /**
     * Cierra la sesión de edición de forma segura.
     * Verifica cambios sin guardar y guarda las preferencias de usuario antes de
     * salir.
     */
    public static void closeClient() {
        if (!org.argentumforge.engine.utils.MapManager.checkUnsavedChanges()) {
            return;
        }
        options.save();
        prgRun = false;
    }

    /**
     * Inicializa los componentes esenciales del motor grafico.
     */
    public void init() {
        Logger.info("Starting LWJGL {}!", Version.getVersion());
        Logger.info("Running on {} / v{} [{}]", System.getProperty("os.name"), System.getProperty("os.version"),
                System.getProperty("os.arch"));
        Logger.info("Java version: {}", System.getProperty("java.version"));

        // Cargar perfiles
        org.argentumforge.engine.utils.ProfileManager.INSTANCE.load();
        boolean hasProfiles = org.argentumforge.engine.utils.ProfileManager.INSTANCE.hasProfiles();

        // Inicializar Window y GUI siempre
        window.init();
        guiSystem.init();

        // Inicializar idioma por defecto (Español) para que la UI tenga texto
        // Posteriormente, al cargar el perfil, se recargará el idioma configurado si es
        // distinto
        org.argentumforge.engine.i18n.I18n.INSTANCE.loadLanguage("es_ES");

        if (!hasProfiles) {
            // Primera ejecución global: Wizard de creación
            org.argentumforge.engine.gui.forms.FSetupWizard wizard = new org.argentumforge.engine.gui.forms.FSetupWizard(
                    this::completeInitialization, true);
            ImGUISystem.INSTANCE.show(wizard);
        } else {
            // Ya existen perfiles: Selector
            org.argentumforge.engine.gui.forms.FProfileSelector selector = new org.argentumforge.engine.gui.forms.FProfileSelector(
                    this::completeInitialization);
            ImGUISystem.INSTANCE.show(selector);
        }

        isWaitingForSetup = true;
    }

    /**
     * Completa la inicialización después del wizard o selector de perfil.
     */
    private void completeInitialization() {
        // En este punto, GameData ya ha sido inicializado por el formulario
        // correspondiente
        // y Window/GUI ya están inicializados por init()
        GameData.init();

        Surface.INSTANCE.init();
        batch = new BatchRenderer();

        // Verificar recursos cargados
        if (!GameData.checkResources()) {
            ImGUISystem.INSTANCE.show(new org.argentumforge.engine.gui.forms.FRoutes());
        }

        changeScene(INTRO_SCENE);
        isWaitingForSetup = false;
    }

    /**
     * Inicia la ejecución del motor.
     * Secuencia: {@code init()} -> {@code loop()} -> {@code close()}.
     */
    public void start() {
        init();
        loop();
        close();
    }

    /**
     * Cierra los recursos y finaliza el funcionamiento del motor grafico.
     */
    private void close() {
        Sound.clearSounds();
        Sound.clearMusics();
        guiSystem.destroy();
        window.close();
    }

    /**
     * Bucle principal de la aplicación.
     * Se ejecuta continuamente mientras {@code prgRun} sea true.
     * Tareas principales por frame:
     * - Procesar eventos de ventana (Input).
     * - Limpiar buffer de pantalla.
     * - Renderizar escena y GUI si la ventana está activa.
     * - Actualizar timers y deltaTime.
     * - Gestionar eventos de mouse y teclado.
     */
    private void loop() {
        Time.initTime();

        while (prgRun) {
            glfwPollEvents();

            if (!window.isMinimized()) {
                // Solo procesar escena si existe (puede ser null mientras se muestra el wizard)
                if (currentScene != null) {
                    glClearColor(currentScene.getBackground().getRed(), currentScene.getBackground().getGreen(),
                            currentScene.getBackground().getBlue(), 1.0f);
                } else {
                    // Color por defecto mientras se espera el wizard
                    glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
                }
                glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

                if (deltaTime >= 0)
                    render();

                glfwSwapBuffers(window.getWindow());
                Time.updateTime();
            }

            MouseListener.resetReleasedButtons();

            // MODO EDITOR: Comunicación con servidor deshabilitada
            // Depues de realizar cualquier accion, se comunica con el servidor para
            // informarle de esta accion...

            // Si hay algo para enviar, lo envia (escribe lo que envia el cliente al
            // servidor)
            // Connection.INSTANCE.write();
            // Si hay algo para recibir, lo recibe (lee lo que recibe el cliente del
            // servidor)
            // Connection.INSTANCE.read();

        }
    }

    /**
     * Cambia la escena actual del juego a una nueva basada en el tipo de escena
     * proporcionado.
     * <p>
     * Inicializa la nueva escena una vez que se ha creado.
     *
     * @param scene tipo de escena
     */
    private void changeScene(SceneType scene) {
        switch (scene) {
            case INTRO_SCENE -> currentScene = new IntroScene();
            case GAME_SCENE -> currentScene = new GameScene();
            case MAIN_SCENE -> currentScene = new MainScene();
        }
        currentScene.init();

        // Ajustar la resolución de la ventana según las preferencias de la escena
        int preferredWidth = currentScene.getPreferredWidth();
        int preferredHeight = currentScene.getPreferredHeight();

        // Solo actualizar si la resolución es diferente a la actual
        if (preferredWidth != Window.INSTANCE.getWidth() || preferredHeight != Window.INSTANCE.getHeight()) {
            Window.INSTANCE.updateResolution(preferredWidth, preferredHeight);
        }
    }

    /**
     * Gestiona el renderizado del frame actual.
     * Verifica si es necesario cambiar de escena (si la actual no es visible)
     * y delega el dibujado a la escena activa y al sistema ImGui.
     */
    private void render() {
        // Si no hay escena (ej: esperando el wizard), solo renderizar GUI
        if (currentScene == null) {
            guiSystem.renderGUI();
            return;
        }

        Window.INSTANCE.setupGameProjection();

        if (!currentScene.isVisible())
            changeScene(currentScene.getChangeScene());

        batch.begin();
        currentScene.mouseEvents();
        currentScene.keyEvents();
        currentScene.render();
        batch.end();
        guiSystem.renderGUI();

        Sound.renderMusic();
        KeyHandler.update();
    }

}
