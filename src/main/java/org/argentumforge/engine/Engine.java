package org.argentumforge.engine;

import imgui.ImGui;
import org.argentumforge.engine.audio.Sound;
import org.argentumforge.engine.gui.ImGUISystem;
import org.argentumforge.engine.listeners.MouseListener;
import org.argentumforge.engine.managers.RenderManager;
import org.argentumforge.engine.managers.SceneManager;
import org.argentumforge.engine.renderer.BatchRenderer;
import org.argentumforge.engine.renderer.Surface;
import org.argentumforge.engine.scenes.Scene;
import org.argentumforge.engine.scenes.SceneType;
import org.argentumforge.engine.utils.GameData;
import org.argentumforge.engine.utils.Time;
import org.lwjgl.Version;
import org.tinylog.Logger;

import static org.argentumforge.engine.utils.GameData.options;
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
    public static final String VERSION = "1.0.0-beta5";
    /** Ventana principal del motor grafico. */
    private final Window window = new Window();

    public Window getWindow() {
        return window;
    }

    /** Sistema de interfaz grafica de usuario. */
    private final ImGUISystem guiSystem = ImGUISystem.INSTANCE;

    // Delegado a Managers
    public static BatchRenderer batch;

    // Mantenido por compatibilidad legacy, pero gestionado por RenderManager
    // Idealmente deberíamos eliminar este campo estático y forzar el uso de
    // RenderManager.INSTANCE.getBatch()
    // pero para evitar errores masivos de compilación por ahora, lo enlazamos.

    /** Flag que indica si el motor está esperando la configuración inicial. */

    public static boolean isPrgRun() {
        return prgRun;
    }

    public static Scene getCurrentScene() {
        return SceneManager.INSTANCE.getCurrentScene();
    }

    /**
     * Cierra la sesión de edición de forma segura.
     * Verifica cambios sin guardar y guarda las preferencias de usuario antes de
     * salir.
     */
    public static void closeClient() {
        org.argentumforge.engine.utils.MapManager.checkUnsavedChangesAsync(() -> {
            options.save();
            prgRun = false;
        }, null);
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
        window.setResizable(false); // Bloquear resize durante Wizard/Selector
        guiSystem.init();
        guiSystem.setViewportsEnabled(false); // Desactivar viewports en el arranque

        // Inicializar RenderManager
        RenderManager.INSTANCE.init();
        Engine.batch = RenderManager.INSTANCE.getBatch(); // Enlazar campo estático legacy

        // Inicializar idioma por defecto (Español) para que la UI tenga texto
        // Posteriormente, al cargar el perfil, se recargará el idioma configurado si es
        // distinto
        org.argentumforge.engine.i18n.I18n.INSTANCE.loadLanguage("es_ES");

        // Inicializar Surface aquí para evitar NPE en el loop principal si este arranca
        // antes de completeInitialization
        Surface.INSTANCE.init();

        if (!hasProfiles) {
            // Primera ejecución global: Wizard de creación
            org.argentumforge.engine.gui.forms.FSetupWizard wizard = new org.argentumforge.engine.gui.forms.FSetupWizard(
                    this::completeInitialization, true);
            ImGUISystem.INSTANCE.show(wizard);
        } else {
            // Ya existen perfiles: Selector
            org.argentumforge.engine.gui.forms.FProfileSelector selector = new org.argentumforge.engine.gui.forms.FProfileSelector(
                    this::completeInitialization);
            guiSystem.show(selector);
        }

    }

    /**
     * Completa la inicialización después del wizard o selector de perfil.
     */
    public void completeInitialization() {
        // En este punto, GameData ya ha sido inicializado por el formulario
        // correspondiente
        // y Window/GUI ya están inicializados por init()
        GameData.init();

        // Aplicar configuraciones de pantalla cargadas del perfil
        window.setVSync(options.isVsync());
        window.updateResolution(options.getScreenWidth(), options.getScreenHeight());

        // Surface ya inicializado en init()
        // batch ya inicializado vía RenderManager

        // Verificar recursos cargados
        if (!GameData.checkResources()) {
            ImGUISystem.INSTANCE.show(new org.argentumforge.engine.gui.forms.FRoutes());
        }

        // Si no hay mapa activo, crear uno nuevo
        if (org.argentumforge.engine.utils.GameData.getActiveContext() == null) {
            org.argentumforge.engine.utils.MapManager.createEmptyMap(100, 100);
        }

        if (getCurrentScene() != null)
            return;

        changeScene(SceneType.GAME_SCENE);

        // Habilitar Viewports y Docking tras el setup
        guiSystem.setViewportsEnabled(true);
        ImGui.getIO().addConfigFlags(imgui.flag.ImGuiConfigFlags.DockingEnable);
        window.setResizable(true); // Permitir redimensionar la ventana principal del editor

        // Verificar actualizaciones
        org.argentumforge.engine.utils.GithubReleaseChecker.checkForUpdates();
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

    public Engine() {
        INSTANCE = this;
    }

    public static Engine INSTANCE;

    public void requestProfileChange() {
        org.argentumforge.engine.utils.MapManager.checkUnsavedChangesAsync(this::performProfileChange, null);
    }

    private void performProfileChange() {
        SceneManager.INSTANCE.reset();

        guiSystem.closeAllFrms();
        guiSystem.setViewportsEnabled(false);
        window.setResizable(false); // Bloquear redimensionamiento durante la selección de perfil
        org.argentumforge.engine.gui.forms.FProfileSelector selector = new org.argentumforge.engine.gui.forms.FProfileSelector(
                this::completeInitialization);
        guiSystem.show(selector);
    }

    /**
     * Cierra los recursos y finaliza el funcionamiento del motor grafico.
     */
    private void close() {
        Sound.clearSounds();
        Sound.clearMusics();
        guiSystem.destroy();
        Surface.INSTANCE.shutdown();
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
                Scene currentScene = getCurrentScene();
                // Solo procesar escena si existe (puede ser null mientras se muestra el wizard)
                if (currentScene != null) {
                    glClearColor(currentScene.getBackground().getRed(), currentScene.getBackground().getGreen(),
                            currentScene.getBackground().getBlue(), 1.0f);
                } else {
                    // Color por defecto mientras se espera el wizard
                    glClearColor(0.12f, 0.12f, 0.12f, 1.0f);
                }
                glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

                if (Time.deltaTime >= 0) {
                    // El renderizado de ImGui ahora es manejado por el bucle del Engine
                    // (startFrame/endFrame)
                    org.argentumforge.engine.gui.ImGUISystem.INSTANCE.startFrame();

                    if (currentScene != null) {
                        Surface.INSTANCE.dispatchUploads();
                        RenderManager.INSTANCE.render(window);
                    }

                    org.argentumforge.engine.gui.ImGUISystem.INSTANCE.endFrame();
                }

                glfwSwapBuffers(window.getWindow());
                Time.updateTime();
            }

            MouseListener.resetReleasedButtons();

            MouseListener.resetReleasedButtons();

            // Procesar tareas del hilo principal
            synchronized (taskQueue) {
                while (!taskQueue.isEmpty()) {
                    try {
                        taskQueue.poll().run();
                    } catch (Exception e) {
                        Logger.error(e, "Error executing main thread task");
                    }
                }
            }

        }
    }

    private final java.util.Queue<Runnable> taskQueue = new java.util.LinkedList<>();

    /**
     * Programa una tarea para ejecutarse en el hilo principal (contexto GL) al
     * inicio
     * del siguiente frame.
     * Seguro para llamar desde cualquier hilo.
     */
    public void runOnMainThread(Runnable action) {
        synchronized (taskQueue) {
            taskQueue.add(action);
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
        SceneManager.INSTANCE.changeScene(scene);
    }
}
