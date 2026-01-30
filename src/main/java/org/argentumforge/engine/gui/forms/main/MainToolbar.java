package org.argentumforge.engine.gui.forms.main;

import imgui.ImGui;
import imgui.ImGuiViewport;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import org.argentumforge.engine.game.console.Console;
import org.argentumforge.engine.game.console.FontStyle;
import org.argentumforge.engine.gui.ImGUISystem;
import org.argentumforge.engine.gui.Theme;
import org.argentumforge.engine.gui.forms.FMain;
import org.argentumforge.engine.gui.forms.Form;
import org.argentumforge.engine.i18n.I18n;
import org.argentumforge.engine.renderer.RGBColor;

import org.argentumforge.engine.renderer.Texture;
import org.argentumforge.engine.utils.editor.*;

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
        drawIconButton("Su", I18n.INSTANCE.get("toolbar.surface"), "FSurfaceEditor", parent.getSurfaceEditor(),
                btnSize, 0 * uvStep + zoom, 0 * uvStep + zoom, 1 * uvStep - zoom, 1 * uvStep - zoom, mapOpen);

        ImGui.sameLine();

        // Objetos (Fila 1, Col 1)
        drawIconButton("Ob", I18n.INSTANCE.get("menu.view.objects"), "FObjEditor", parent.getObjEditor(),
                btnSize, 1 * uvStep + zoom, 1 * uvStep + zoom, 2 * uvStep - zoom, 2 * uvStep - zoom, mapOpen);

        ImGui.sameLine();

        // NPCs (Fila 1, Col 0)
        drawIconButton("NP", I18n.INSTANCE.get("menu.view.npcs"), "FNpcEditor", parent.getNpcEditor(),
                btnSize, 0 * uvStep + zoom, 1 * uvStep + zoom, 1 * uvStep - zoom, 2 * uvStep - zoom, mapOpen);

        ImGui.sameLine();

        // Partículas (Fila 2, Col 0)
        drawIconButton("Pa", I18n.INSTANCE.get("menu.view.particles"), "FParticleEditor", parent.getParticleEditor(),
                btnSize, 0 * uvStep + zoom, 2 * uvStep + zoom, 1 * uvStep - zoom, 3 * uvStep - zoom, mapOpen);

        // --- SEPARADOR ---
        drawToolbarSeparator();

        // --- GRUPO 2: LÓGICA ---

        // Bloqueos (Fila 0, Col 1)
        drawIconButton("Bl", I18n.INSTANCE.get("menu.view.blocks"), "FBlockEditor", parent.getBlockEditor(),
                btnSize, 1 * uvStep + zoom, 0 * uvStep + zoom, 2 * uvStep - zoom, 1 * uvStep - zoom, mapOpen);

        ImGui.sameLine();

        // Triggers (Fila 0, Col 2)
        drawIconButton("Tg", I18n.INSTANCE.get("menu.view.triggers"), "FTriggerEditor", parent.getTriggerEditor(),
                btnSize, 2 * uvStep + zoom, 0 * uvStep + zoom, 3 * uvStep - zoom, 1 * uvStep - zoom, mapOpen);

        ImGui.sameLine();

        // Traslados (Fila 1, Col 2)
        drawIconButton("Tl", I18n.INSTANCE.get("menu.view.transfers"), "FTransferEditor", parent.getTransferEditor(),
                btnSize, 2 * uvStep + zoom, 1 * uvStep + zoom, 3 * uvStep - zoom, 2 * uvStep - zoom, mapOpen);

        // --- SEPARADOR ---
        drawToolbarSeparator();

        // --- GRUPO 3: UTILIDAD ---

        // Minimapa (Fila 2, Col 1)
        drawIconButton("MM", I18n.INSTANCE.get("menu.view.minimap"), "FMinimap", parent.getMinimap(),
                btnSize, 1 * uvStep + zoom, 2 * uvStep + zoom, 2 * uvStep - zoom, 3 * uvStep - zoom, mapOpen);

        ImGui.sameLine();

        // Botón Selección (Fila 2, Col 2)
        boolean selectionActive = Selection.getInstance().isActive();
        if (selectionActive) {
            ImGui.pushStyleColor(ImGuiCol.Button, Theme.COLOR_ACCENT);
        }

        ImGui.pushID("btnSelect");
        if (toolbarIcons.getId() > 0) {
            ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, 0, 0);
            if (!mapOpen)
                ImGui.beginDisabled();
            if (ImGui.imageButton("##selectButton", (long) toolbarIcons.getId(), btnSize, btnSize, 2 * uvStep + zoom,
                    2 * uvStep + zoom,
                    3 * uvStep - zoom,
                    3 * uvStep - zoom)) {
                toggleSelection();
            }
            if (!mapOpen)
                ImGui.endDisabled();
            ImGui.popStyleVar();
        } else {
            if (ImGui.button("Sel", btnSize, btnSize)) {
                toggleSelection();
            }
        }
        ImGui.popID();

        if (ImGui.isItemHovered()) {
            ImGui.setTooltip(I18n.INSTANCE.get("common.selection"));
        }
        if (selectionActive) {
            ImGui.popStyleColor();
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

    private void toggleSelection() {
        Selection sel = Selection.getInstance();
        sel.setActive(!sel.isActive());

        if (sel.isActive()) {
            Console.INSTANCE.addMsgToConsole(I18n.INSTANCE.get("msg.selectionModeOn"), FontStyle.REGULAR,
                    new RGBColor(0f, 1f, 0f));
            // Desactivar otros modos
            Surface.getInstance().setMode(0);
            Npc.getInstance().setMode(0);
            Obj.getInstance().setMode(0);
            Block.getInstance().setMode(0);
            Trigger.getInstance().setMode(0);
            Transfer.getInstance().setMode(0);
        } else {
            Console.INSTANCE.addMsgToConsole(I18n.INSTANCE.get("msg.selectionModeOff"), FontStyle.REGULAR,
                    new RGBColor(1f, 1f, 0f));
        }
    }

    private void drawIconButton(String fallbackLabel, String tooltip, String formName, Form formInstance,
            float size, float u0, float v0, float u1, float v1, boolean enabled) {
        if (!enabled)
            ImGui.beginDisabled();
        ImGui.pushID(formName);
        // Resaltar si está activo (visible)
        boolean isVisible = ImGUISystem.INSTANCE.isFormVisible(formName);
        if (isVisible) {
            ImGui.pushStyleColor(ImGuiCol.Button, Theme.COLOR_PRIMARY);
        }

        // Si la textura cargó correctamente, usar ImageButton, sino fallback a Texto
        if (toolbarIcons.getId() > 0) {
            // Eliminar padding para que el icono ocupe todo el botón
            ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, 0, 0);
            ImGui.pushID("##iconButton");
            if (ImGui.imageButton("##iconButton", (long) toolbarIcons.getId(), size, size, u0, v0, u1, v1)) {
                toggleForm(isVisible, formInstance);
            }
            ImGui.popID();
            ImGui.popStyleVar();
        } else {
            if (ImGui.button(fallbackLabel, size, size)) {
                toggleForm(isVisible, formInstance);
            }
        }

        if (isVisible) {
            ImGui.popStyleColor();
        }

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
}
