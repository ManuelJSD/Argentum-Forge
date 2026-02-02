package org.argentumforge.engine.gui;

import org.tinylog.Logger;

import imgui.*;
import imgui.callback.ImStrConsumer;
import imgui.callback.ImStrSupplier;
import imgui.flag.*;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import org.argentumforge.engine.Window;
import org.argentumforge.engine.game.console.ImGuiFonts;
import org.argentumforge.engine.gui.forms.Form;
import org.argentumforge.engine.listeners.KeyHandler;
import org.argentumforge.engine.listeners.MouseListener;

import java.util.ArrayList;
import java.util.List;

import static org.argentumforge.engine.utils.Time.deltaTime;
import static org.lwjgl.glfw.GLFW.*;

/**
 * Sistema centralizado para la gestion de interfaces graficas de usuario basado
 * en {@code ImGui}.
 * <p>
 * Se encarga de inicializar, configurar y renderizar todos los elementos de la
 * interfaz de usuario utilizando la biblioteca
 * <i>Dear ImGui</i> adaptada para {@code LWJGL3}.
 * <p>
 * Administra el ciclo de vida de las distintas ventanas y formularios de la
 * interfaz, permitiendo mostrar, ocultar y gestionar
 * componentes de UI como dialogos, menus, paneles y otros elementos
 * interactivos. Mantiene una coleccion de formularios activos y
 * coordina su renderizado en cada fotograma.
 * <p>
 * Proporciona funcionalidad para la configuracion de estilos, fuentes y
 * comportamientos de la interfaz, y maneja la
 * sincronizacion entre los eventos de entrada (teclado y raton) entre el
 * sistema de ventanas GLFW y los componentes de ImGui,
 * asegurando una respuesta coherente de la interfaz.
 */

public enum ImGUISystem {

    INSTANCE;

    // LWJGL3 rendered itself (SHOULD be initialized)
    private final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();
    private final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();

    // Cursores del mouse proporcionados por GLFW
    private final long[] mouseCursors = new long[ImGuiMouseCursor.COUNT];

    // arreglo de ventanas gui
    private final List<Form> frms = new ArrayList<>();

    private Window window;

    private boolean showDebug = false;

    public boolean isShowDebug() {
        return showDebug;
    }

    public void setShowDebug(boolean showDebug) {
        this.showDebug = showDebug;
    }

    public void init() {
        this.window = org.argentumforge.engine.Engine.INSTANCE.getWindow();
        ImGui.createContext();

        /*
         * Aplicamos el estilo guardado en las opciones.
         */
        try {
            Theme.StyleType savedStyle = Theme.StyleType
                    .valueOf(org.argentumforge.engine.game.Options.INSTANCE.getVisualTheme());
            Theme.applyStyle(savedStyle);
        } catch (Exception e) {
            Theme.applyModernStyle();
        }

        // Iniciamos la configuracion de ImGuiIO
        final ImGuiIO io = ImGui.getIO();

        io.setIniFilename("resources/gui.ini"); // Guardamos en un archivo .ini
        io.setConfigFlags(ImGuiConfigFlags.NavEnableKeyboard | ImGuiConfigFlags.DockingEnable
                | ImGuiConfigFlags.ViewportsEnable); // Navegacion con el teclado, Docking y Viewports (Multi-monitor)
        io.setBackendFlags(ImGuiBackendFlags.HasMouseCursors); // Cursores del mouse para mostrar al cambiar el tamaño
                                                               // de las ventanas, etc.
        io.setBackendPlatformName("imgui_java_impl_glfw");
        io.setBackendRendererName("imgui_java_impl_lwjgl");

        // Teclado y raton.
        setMouseMapping();
        setupClipboard(io);
        // Configuracion de Fonts
        loadImGUIFonts(io);

        // Iniciamos ImGUI en OpenGL
        // Usamos installCallbacks = false para que no sobreescriba nuestros listeners,
        // nosotros los envolveremos manualmente en setupEngineCallbacksWrapper.
        imGuiGlfw.init(window.getWindow(), false);

        // Re-enlazamos nuestros listeners del motor envolviendo los de ImGui.
        // Esto es necesario porque imGuiGlfw.init(..., true) sobreescribe los callbacks
        // de Window.java.
        setupEngineCallbacksWrapper();

        imGuiGl3.init();
    }

    /**
     * Envuelve los callbacks de ImGui para llamar también a nuestros listeners del
     * motor.
     */
    private void setupEngineCallbacksWrapper() {
        final long win = window.getWindow();

        // El binding de imgui-java no permite "encadenar" fácilmente,
        // así que reimplementamos la llamada a nuestros listeners aquí.
        // ImGui ya tiene sus callbacks instalados, nosotros simplemente añadimos los
        // nuestros.

        glfwSetKeyCallback(win, (w, k, s, a, m) -> {
            imGuiGlfw.keyCallback(w, k, s, a, m);
            KeyHandler.keyCallback(w, k, s, a, m);
        });

        glfwSetMouseButtonCallback(win, (w, b, a, m) -> {
            imGuiGlfw.mouseButtonCallback(w, b, a, m);
            MouseListener.mouseButtonCallback(w, b, a, m);
        });

        glfwSetCursorPosCallback(win, (w, x, y) -> {
            imGuiGlfw.cursorPosCallback(w, x, y);
            MouseListener.mousePosCallback(w, x, y);
        });

        glfwSetScrollCallback(win, (w, x, y) -> {
            imGuiGlfw.scrollCallback(w, x, y);
            MouseListener.mouseScrollCallback(w, x, y);
        });

        glfwSetCharCallback(win, (w, c) -> {
            imGuiGlfw.charCallback(w, c);
        });

        // Otros callbacks necesarios para ImGui viewports y gestion de foco
        glfwSetWindowFocusCallback(win, (w, f) -> {
            imGuiGlfw.windowFocusCallback(w, f);
        });

        glfwSetCursorEnterCallback(win, (w, e) -> {
            imGuiGlfw.cursorEnterCallback(w, e);
        });
    }

    private void setMouseMapping() {
        mouseCursors[ImGuiMouseCursor.Arrow] = glfwCreateStandardCursor(GLFW_ARROW_CURSOR);
        mouseCursors[ImGuiMouseCursor.TextInput] = glfwCreateStandardCursor(GLFW_IBEAM_CURSOR);
        mouseCursors[ImGuiMouseCursor.ResizeAll] = glfwCreateStandardCursor(GLFW_ARROW_CURSOR);
        mouseCursors[ImGuiMouseCursor.ResizeNS] = glfwCreateStandardCursor(GLFW_VRESIZE_CURSOR);
        mouseCursors[ImGuiMouseCursor.ResizeEW] = glfwCreateStandardCursor(GLFW_HRESIZE_CURSOR);
        mouseCursors[ImGuiMouseCursor.ResizeNESW] = glfwCreateStandardCursor(GLFW_ARROW_CURSOR);
        mouseCursors[ImGuiMouseCursor.ResizeNWSE] = glfwCreateStandardCursor(GLFW_ARROW_CURSOR);
        mouseCursors[ImGuiMouseCursor.Hand] = glfwCreateStandardCursor(GLFW_HAND_CURSOR);
        mouseCursors[ImGuiMouseCursor.NotAllowed] = glfwCreateStandardCursor(GLFW_ARROW_CURSOR);
    }

    private void setupClipboard(ImGuiIO io) {
        io.setSetClipboardTextFn(new ImStrConsumer() {
            @Override
            public void accept(final String s) {
                glfwSetClipboardString(window.getWindow(), s);
            }
        });

        io.setGetClipboardTextFn(new ImStrSupplier() {
            @Override
            public String get() {
                return glfwGetClipboardString(window.getWindow());
            }
        });
    }

    private void loadImGUIFonts(ImGuiIO io) {
        final ImFontAtlas fontAtlas = io.getFonts();

        // Agregamos la tipografia por defecto, que es 'ProggyClean.ttf, 13px'
        fontAtlas.addFontDefault();

        // La creación de ImFontConfig asignará memoria nativa...
        final ImFontConfig fontConfig = new ImFontConfig();

        // Todas las fuentes agregadas mientras este modo está activado se fusionarán
        // con la fuente agregada anteriormente
        fontConfig.setMergeMode(true);
        fontConfig.setPixelSnapH(true);
        fontConfig.setGlyphRanges(fontAtlas.getGlyphRangesCyrillic());

        fontConfig.setMergeMode(false);
        fontConfig.setPixelSnapH(false);

        fontConfig.setRasterizerMultiply(1.2f);

        ImGuiFonts.fontRegular = fontAtlas.addFontFromFileTTF("resources/fonts/LiberationSans-Regular.ttf", 13);
        ImGuiFonts.fontBold = fontAtlas.addFontFromFileTTF("resources/fonts/LiberationSans-Bold.ttf", 13);
        ImGuiFonts.fontItalic = fontAtlas.addFontFromFileTTF("resources/fonts/LiberationSans-Italic.ttf", 13);
        ImGuiFonts.fontBoldItalic = fontAtlas.addFontFromFileTTF("resources/fonts/LiberationSans-BoldItalic.ttf", 13);

        fontConfig.destroy();
    }

    public void destroy() {
        imGuiGl3.shutdown();
        imGuiGlfw.shutdown();
        ImGui.destroyContext();
    }

    public void startFrame() {
        final ImGuiIO io = ImGui.getIO();
        io.setDeltaTime(deltaTime);

        if (window.isCursorCrosshair()) {
            glfwSetCursor(window.getWindow(), glfwCreateStandardCursor(GLFW_CROSSHAIR_CURSOR));
        }

        // IMPORTANT!!
        // Any Dear ImGui code SHOULD go between NewFrame()/Render() methods
        imGuiGlfw.newFrame();
        imGuiGl3.newFrame();
        ImGui.newFrame();
    }

    public void endFrame() {
        try {
            renderFrms();
            DialogManager.getInstance().render();
            org.argentumforge.engine.gui.components.LoadingModal.getInstance().render();
        } catch (Exception e) {
            Logger.error(e, "Error rendering ImGui components. Continuing to prevent native crash.");
        }

        ImGui.render();

        // After ImGui#render call we provide draw data into LWJGL3 renderer.
        imGuiGl3.renderDrawData(ImGui.getDrawData());

        if (ImGui.getIO().hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)) {
            final long backupWindowPtr = glfwGetCurrentContext();
            ImGui.updatePlatformWindows();
            ImGui.renderPlatformWindowsDefault();
            glfwMakeContextCurrent(backupWindowPtr);
        }

        MouseListener.endFrame();
    }

    /**
     * @deprecated Use startFrame() and endFrame() for better lifecycle control.
     */
    public void renderGUI() {
        startFrame();

        if (showDebug) {
            ImGui.setNextWindowPos(5, 25, ImGuiCond.Always);
            ImGui.begin("InputDebug", ImGuiWindowFlags.NoDecoration | ImGuiWindowFlags.AlwaysAutoResize |
                    ImGuiWindowFlags.NoFocusOnAppearing | ImGuiWindowFlags.NoNav | ImGuiWindowFlags.NoInputs);
            ImGui.text("dt=" + deltaTime);
            ImGui.text("io.WantCaptureMouse=" + ImGui.getIO().getWantCaptureMouse());
            ImGui.text("ImGui.isMouseDown(0)=" + ImGui.isMouseDown(0) + " clicked=" + ImGui.isMouseClicked(0));
            ImGui.text("glfw L=" + (glfwGetMouseButton(window.getWindow(), GLFW_MOUSE_BUTTON_1) == GLFW_PRESS));
            ImGui.text(
                    String.format("mousePos=%.1f, %.1f", ImGui.getIO().getMousePosX(), ImGui.getIO().getMousePosY()));
            ImGui.end();
        }

        endFrame();
    }

    private void renderFrms() {

        for (int i = 0; i < frms.size(); i++)
            frms.get(i).render();

        // ImGui.showDemoWindow();
    }

    public void show(Form frm) {
        boolean exits = false;
        // esta abierto?
        for (int i = 0; i < frms.size(); i++) {
            if (frms.get(i).getClass().getSimpleName().equals(frm.getClass().getSimpleName())) {
                frms.set(i, frm); // bueno remplazamos su contenido (esto si es que tenemos un frmMessage).
                exits = true;
                break;
            }
        }
        if (!exits)
            addFrm(frm);
    }

    public boolean isFormVisible(String fromClass) {
        boolean visible = false;
        for (Form frm : frms) {
            if (frm.getClass().getSimpleName().equals(fromClass)) {
                visible = true;
                break;
            }
        }
        return visible;
    }

    /**
     * Checkea si el frm solicitiado es el ultimo de nuestro array de frms.
     */
    public boolean isMainLast() {
        if (frms.size() <= 0)
            return false;

        return frms.get(frms.size() - 1).getClass().getSimpleName().equals("FMain");
    }

    public void deleteFrmArray(Form frm) {
        frms.remove(frm);
    }

    public void addFrm(Form e) {
        frms.add(e);
    }

    public void closeAllFrms() {
        frms.clear();
    }

    /**
     * Devuelve una copia de la lista de formularios activos.
     */
    public java.util.List<Form> getActiveForms() {
        return new java.util.ArrayList<>(frms);
    }

    /**
     * Obtiene una instancia de un formulario específico por su clase.
     * 
     * @param formClass Clase del formulario a buscar
     * @return La instancia del formulario si está visible, null en caso contrario
     */
    public <T extends Form> T getForm(Class<T> formClass) {
        for (Form frm : frms) {
            if (formClass.isInstance(frm)) {
                return formClass.cast(frm);
            }
        }
        return null;
    }

    public void setViewportsEnabled(boolean enabled) {
        ImGuiIO io = ImGui.getIO();
        if (enabled) {
            io.addConfigFlags(ImGuiConfigFlags.ViewportsEnable);
        } else {
            io.removeConfigFlags(ImGuiConfigFlags.ViewportsEnable);
        }
    }

    public ImGuiImplGlfw getImGuiGlfw() {
        return imGuiGlfw;
    }
}
