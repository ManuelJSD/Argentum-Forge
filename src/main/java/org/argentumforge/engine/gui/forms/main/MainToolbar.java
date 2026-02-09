package org.argentumforge.engine.gui.forms.main;

import imgui.ImGui;
import imgui.ImGuiViewport;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import org.argentumforge.engine.gui.ImGUISystem;
import org.argentumforge.engine.gui.Theme;
import org.argentumforge.engine.gui.forms.FMain;
import org.argentumforge.engine.gui.forms.Form;
import org.argentumforge.engine.i18n.I18n;

import org.argentumforge.engine.renderer.Texture;

/**
 * Componente para renderizar la barra de herramientas superior con botones de
 * acceso rápido.
 */
public class MainToolbar {

    private final FMain parent;
    private final Texture toolbarIcons;

    public MainToolbar(FMain parent) {
        this.parent = parent;
        // Cargar iconos de la barra de herramientas
        this.toolbarIcons = new Texture();
        this.toolbarIcons.loadTexture(toolbarIcons, "gui", "toolbar_icons.png", true);
    }

    public void render() {
        ImGuiViewport viewport = ImGui.getMainViewport();
        float toolbarHeight = 52.0f;

        // Anclar justo debajo del menú (WorkPos lo tiene en cuenta)
        ImGui.setNextWindowPos(viewport.getWorkPosX(), viewport.getWorkPosY());
        ImGui.setNextWindowSize(viewport.getWorkSizeX(), toolbarHeight);
        ImGui.setNextWindowViewport(viewport.getID());

        if (ImGui.begin("ToolBar", ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoBackground
                | ImGuiWindowFlags.NoSavedSettings | ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoMove
                | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoScrollWithMouse)) {
            drawEditorButtons();
        }
        ImGui.end();
    }

    private void drawEditorButtons() {
        ImGui.setCursorPos(10, 2); // Centrado verticalmente (52-48)/2

        // Configuración de estilo par botones
        float btnSize = 48;
        float uvStep = 1.0f / 3.0f; // 3x3 grid
        float zoom = 0.03f; // Ajuste fino (3%) para eliminar márgenes transparentes sin recortar el icono

        ImGui.pushStyleVar(ImGuiStyleVar.FrameBorderSize, 0); // Eliminar borde de ImGui

        // --- GRUPO 1: EDICIÓN (Visual) ---

        boolean mapOpen = !org.argentumforge.engine.utils.GameData.getOpenMaps().isEmpty();

        // Superficies (Fila 0, Col 0)
        drawIconButton(I18n.INSTANCE.get("toolbar.surface.short"), I18n.INSTANCE.get("toolbar.surface"),
                "FSurfaceEditor", parent.getSurfaceEditor(),
                btnSize, 0 * uvStep + zoom, 0 * uvStep + zoom, 1 * uvStep - zoom, 1 * uvStep - zoom, mapOpen);

        ImGui.sameLine();

        // Objetos (Fila 1, Col 1)
        drawIconButton(I18n.INSTANCE.get("toolbar.objects.short"), I18n.INSTANCE.get("menu.view.objects"), "FObjEditor",
                parent.getObjEditor(),
                btnSize, 1 * uvStep + zoom, 1 * uvStep + zoom, 2 * uvStep - zoom, 2 * uvStep - zoom, mapOpen);

        ImGui.sameLine();

        // NPCs (Fila 1, Col 0)
        drawIconButton(I18n.INSTANCE.get("toolbar.npcs.short"), I18n.INSTANCE.get("menu.view.npcs"), "FNpcEditor",
                parent.getNpcEditor(),
                btnSize, 0 * uvStep + zoom, 1 * uvStep + zoom, 1 * uvStep - zoom, 2 * uvStep - zoom, mapOpen);

        ImGui.sameLine();

        // Partículas (Fila 2, Col 0)
        drawIconButton(I18n.INSTANCE.get("toolbar.particles.short"), I18n.INSTANCE.get("menu.view.particles"),
                "FParticleEditor", parent.getParticleEditor(),
                btnSize, 0 * uvStep + zoom, 2 * uvStep + zoom, 1 * uvStep - zoom, 3 * uvStep - zoom, mapOpen);

        // --- SEPARADOR ---
        drawToolbarSeparator();

        // --- GRUPO 2: LÓGICA ---

        // Bloqueos (Fila 0, Col 1)
        drawIconButton(I18n.INSTANCE.get("toolbar.blocks.short"), I18n.INSTANCE.get("menu.view.blocks"), "FBlockEditor",
                parent.getBlockEditor(),
                btnSize, 1 * uvStep + zoom, 0 * uvStep + zoom, 2 * uvStep - zoom, 1 * uvStep - zoom, mapOpen);

        ImGui.sameLine();

        // Triggers (Fila 0, Col 2)
        drawIconButton(I18n.INSTANCE.get("toolbar.triggers.short"), I18n.INSTANCE.get("menu.view.triggers"),
                "FTriggerEditor", parent.getTriggerEditor(),
                btnSize, 2 * uvStep + zoom, 0 * uvStep + zoom, 3 * uvStep - zoom, 1 * uvStep - zoom, mapOpen);

        ImGui.sameLine();

        // Traslados (Fila 1, Col 2)
        drawIconButton(I18n.INSTANCE.get("toolbar.transfers.short"), I18n.INSTANCE.get("menu.view.transfers"),
                "FTransferEditor", parent.getTransferEditor(),
                btnSize, 2 * uvStep + zoom, 1 * uvStep + zoom, 3 * uvStep - zoom, 2 * uvStep - zoom, mapOpen);

        // --- SEPARADOR ---
        drawToolbarSeparator();

        // --- GRUPO 3: UTILIDAD ---

        // Minimapa (Fila 2, Col 1)
        drawIconButton(I18n.INSTANCE.get("toolbar.minimap.short"), I18n.INSTANCE.get("menu.view.minimap"), "FMinimap",
                parent.getMinimap(),
                btnSize, 1 * uvStep + zoom, 2 * uvStep + zoom, 2 * uvStep - zoom, 3 * uvStep - zoom, mapOpen);

        ImGui.sameLine();

        // Botón Inspector (Anteriormente Selección)
        boolean inspectorActive = org.argentumforge.engine.game.EditorController.INSTANCE.isInspectorMode();

        // Colores para el Inspector (Verde)
        int inspColor;
        int inspHover;
        int inspActive;

        if (inspectorActive) {
            inspColor = Theme.COLOR_ACCENT;
            inspHover = Theme.rgba(102, 187, 106, 255); // Verde más claro
            inspActive = Theme.rgba(56, 142, 60, 255); // Verde más oscuro
        } else {
            inspColor = Theme.rgba(45, 45, 45, 255);
            inspHover = Theme.rgba(70, 70, 70, 255);
            inspActive = Theme.rgba(90, 90, 90, 255);
        }

        ImGui.pushStyleColor(ImGuiCol.Button, inspColor);
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, inspHover);
        ImGui.pushStyleColor(ImGuiCol.ButtonActive, inspActive);

        ImGui.pushID("btnInspector");
        if (toolbarIcons.getId() > 0) {
            float padding = 4.0f; // 2 pixels per side
            ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, padding / 2, padding / 2);
            if (!mapOpen)
                ImGui.beginDisabled();
            // Reutilizamos el mismo icono de selección por ahora (o placeholder)
            // Coordenadas UV: 2, 2
            if (ImGui.imageButton("##inspectorButton", (long) toolbarIcons.getId(), btnSize - padding,
                    btnSize - padding, 2 * uvStep + zoom,
                    2 * uvStep + zoom,
                    3 * uvStep - zoom,
                    3 * uvStep - zoom)) {
                toggleInspector();
            }
            if (!mapOpen)
                ImGui.endDisabled();
            ImGui.popStyleVar();
        } else {
            if (ImGui.button(I18n.INSTANCE.get("toolbar.inspector"), btnSize, btnSize)) {
                toggleInspector();
            }
        }
        ImGui.popID();

        ImGui.popStyleColor(3);

        if (ImGui.isItemHovered()) {
            ImGui.setTooltip(I18n.INSTANCE.get("common.inspector")); // Fallback text if key missing
        }

        ImGui.popStyleVar(); // Pop BorderSize
    }

    private void drawToolbarSeparator() {
        ImGui.sameLine(0, 10);

        float x = ImGui.getCursorScreenPosX();
        float y = ImGui.getCursorScreenPosY();
        float h = 48; // Button height

        // Draw vertical line centered
        ImGui.getWindowDrawList().addLine(
                x, y + 8,
                x, y + h - 8,
                ImGui.getColorU32(ImGuiCol.Separator));

        ImGui.dummy(1, h); // Advance cursor
        ImGui.sameLine(0, 10);
    }

    private void toggleInspector() {
        org.argentumforge.engine.game.EditorController controller = org.argentumforge.engine.game.EditorController.INSTANCE;
        controller.setInspectorMode(!controller.isInspectorMode());
    }

    private void drawIconButton(String fallbackLabel, String tooltip, String formName, Form formInstance,
            float size, float u0, float v0, float u1, float v1, boolean enabled) {
        if (!enabled)
            ImGui.beginDisabled();
        ImGui.pushID(formName);

        // Determinar estado activo (visible)
        boolean isVisible = ImGUISystem.INSTANCE.isFormVisible(formName);

        // Definir colores según estado
        int colorButton;
        int colorHovered;
        int colorActive;

        if (isVisible) {
            // Estado ACTIVO (Ventana Abierta)
            colorButton = Theme.COLOR_PRIMARY;
            colorHovered = Theme.rgba(100, 181, 246, 255); // Azul más claro
            colorActive = Theme.rgba(21, 101, 192, 255); // Azul más oscuro
        } else {
            // Estado INACTIVO (Ventana Cerrada)
            colorButton = Theme.rgba(45, 45, 45, 255); // Fondo base
            colorHovered = Theme.rgba(70, 70, 70, 255); // Hover
            colorActive = Theme.rgba(90, 90, 90, 255); // Click
        }

        // Aplicar estilos de color
        ImGui.pushStyleColor(ImGuiCol.Button, colorButton);
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, colorHovered);
        ImGui.pushStyleColor(ImGuiCol.ButtonActive, colorActive);

        // Si la textura cargó correctamente, usar ImageButton, sino fallback a Texto
        if (toolbarIcons.getId() > 0) {
            // Usar padding pequeño para que se dibuje el fondo (Color) pero manteniendo el
            // icono grande
            float padding = 4.0f; // 2px por lado x 2
            ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, padding / 2, padding / 2);
            ImGui.pushID("##iconButton");
            if (ImGui.imageButton("##iconButton", (long) toolbarIcons.getId(), size - padding, size - padding, u0, v0,
                    u1, v1)) {
                toggleForm(isVisible, formInstance);
            }
            ImGui.popID();
            ImGui.popStyleVar();
        } else {
            if (ImGui.button(fallbackLabel, size, size)) {
                toggleForm(isVisible, formInstance);
            }
        }

        // Restaurar estilos
        ImGui.popStyleColor(3);

        if (ImGui.isItemHovered()) {
            ImGui.setTooltip(tooltip);
        }
        if (!enabled)
            ImGui.endDisabled();
        ImGui.popID();
    }

    private void toggleForm(boolean isVisible, Form formInstance) {
        if (isVisible) {
            ImGUISystem.INSTANCE.deleteFrmArray(formInstance);
        } else {
            ImGUISystem.INSTANCE.show(formInstance);
        }
    }

    public void cleanup() {
        if (toolbarIcons != null) {
            toolbarIcons.cleanup();
        }
    }
}
