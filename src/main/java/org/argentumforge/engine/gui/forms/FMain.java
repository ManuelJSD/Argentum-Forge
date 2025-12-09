package org.argentumforge.engine.gui.forms;

import imgui.ImGui;
import imgui.flag.*;
import org.argentumforge.engine.Window;
import org.argentumforge.engine.game.console.Console;
import org.argentumforge.engine.gui.ImGUISystem;

import static org.argentumforge.engine.utils.Time.FPS;

/**
 * Formulario principal que proporciona la interfaz de usuario durante la
 * partida activa.
 * <p>
 * La clase {@code FMain} representa la pantalla principal que se muestra una
 * vez que el usuario ha iniciado sesión correctamente
 * y está jugando activamente. Esta clase extiende {@link Form} y actúa como el
 * núcleo de la interfaz gráfica durante el gameplay.
 * <p>
 * Funcionalidades principales:
 * <ul>
 * <li>Muestra estadísticas del personaje (vida, maná, energía, hambre, sed,
 * etc.).</li>
 * <li>Permite la gestión del inventario y hechizos, alternando entre ambas
 * vistas.</li>
 * <li>Visualiza oro, experiencia y nivel del personaje.</li>
 * <li>Integra una consola de mensajes del sistema y un chat para comunicación
 * con otros jugadores.</li>
 * <li>Incluye botones para acceder a habilidades, estadísticas, clanes,
 * opciones y manejo de oro.</li>
 * <li>Permite el cierre y minimizado de la ventana principal del cliente.</li>
 * <li>Gestiona la entrada de texto para comandos y chat, y procesa las
 * interacciones del usuario.</li>
 * <li>Comunica acciones relevantes con el servidor mediante el sistema de
 * protocolos de red.</li>
 * </ul>
 * <p>
 * La clase está organizada en métodos privados que separan la lógica de
 * renderizado en secciones específicas para mejorar la legibilidad y el
 * mantenimiento.
 * <p>
 * <b>Nota:</b> Todos los elementos gráficos se dibujan usando ImGui y se
 * posicionan manualmente según el layout de la interfaz.
 */

public final class FMain extends Form {

    private static final int TRANSPARENT_COLOR = ImGui.getColorU32(0f, 0f, 0f, 0f);

    private FSurfaceEditor surfaceEditor;
    private FBlockEditor blockEditor;

    public FMain() {
        surfaceEditor = new FSurfaceEditor();
        blockEditor = new FBlockEditor();
    }

    @Override
    public void render() {
        ImGui.setNextWindowSize(Window.INSTANCE.getWidth() + 10, Window.INSTANCE.getHeight() + 5, ImGuiCond.Always);
        ImGui.setNextWindowPos(-5, -1, ImGuiCond.Once);

        ImGui.begin(this.getClass().getSimpleName(), ImGuiWindowFlags.NoTitleBar |
                ImGuiWindowFlags.NoMove |
                ImGuiWindowFlags.NoFocusOnAppearing |
                ImGuiWindowFlags.NoDecoration |
                ImGuiWindowFlags.NoBackground |
                ImGuiWindowFlags.NoResize |
                ImGuiWindowFlags.NoSavedSettings |
                ImGuiWindowFlags.NoBringToFrontOnFocus);

        this.renderFPS();
        this.drawButtons();
        Console.INSTANCE.drawConsole();
        ImGui.end();
    }

    /**
     * Dibuja un botón invisible en la posición y tamaño indicados. Devuelve true si
     * fue presionado.
     */
    private boolean drawButton(int x, int y, int w, int h, String label) {
        ImGui.setCursorPos(x, y);
        return ImGui.invisibleButton(label, w, h);
    }

    // FPS
    private void renderFPS() {
        final String txtFPS = String.valueOf(FPS);
        ImGui.setCursorPos(448, 4);
        ImGui.pushStyleVar(ImGuiStyleVar.SelectableTextAlign, 0.5f, 0.5f);
        ImGui.pushStyleColor(ImGuiCol.HeaderHovered, TRANSPARENT_COLOR);
        ImGui.pushStyleColor(ImGuiCol.HeaderActive, TRANSPARENT_COLOR);
        ImGui.selectable(txtFPS, false, ImGuiSelectableFlags.None, 28, 10);
        ImGui.popStyleColor();
        ImGui.popStyleColor();
        ImGui.popStyleVar();
    }

    // Botones principales
    private void drawButtons() {
        /*-=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=-
                         WINDOWS CONTROLS BUTTONS
        -=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=-*/
        /*
         * if (drawButton(775, 3, 17, 17, "close")) {
         * playSound(SND_CLICK);
         * Engine.closeClient();
         * }
         * if (drawButton(755, 3, 17, 17, "minimizar")) {
         * playSound(SND_CLICK);
         * Window.INSTANCE.minimizar();
         * }
         */

        // Botones de control de editores
        drawEditorButtons();
    }

    /**
     * Dibuja los botones para mostrar/ocultar los editores de superficies y
     * bloqueos.
     */
    private void drawEditorButtons() {
        ImGui.setCursorPos(10, 10);

        // Botón Superficies
        if (ImGui.button("Superficies", 100, 25)) {
            if (ImGUISystem.INSTANCE.isFormVisible("FSurfaceEditor")) {
                ImGUISystem.INSTANCE.deleteFrmArray(surfaceEditor);
            } else {
                ImGUISystem.INSTANCE.show(surfaceEditor);
            }
        }

        ImGui.sameLine();

        // Botón Bloqueos
        if (ImGui.button("Bloqueos", 100, 25)) {
            if (ImGUISystem.INSTANCE.isFormVisible("FBlockEditor")) {
                ImGUISystem.INSTANCE.deleteFrmArray(blockEditor);
            } else {
                ImGUISystem.INSTANCE.show(blockEditor);
            }
        }

        ImGui.sameLine();

        // Botón Opciones
        if (ImGui.button("Opciones", 100, 25)) {
            ImGUISystem.INSTANCE.show(new FOptions());
        }
    }

}
