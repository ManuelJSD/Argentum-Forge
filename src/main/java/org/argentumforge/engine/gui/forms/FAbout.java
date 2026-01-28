package org.argentumforge.engine.gui.forms;

import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import org.argentumforge.engine.gui.Theme;

public class FAbout extends Form {

    @Override
    public void render() {
        // Center window
        ImGui.setNextWindowPos(
                (org.argentumforge.engine.Window.INSTANCE.getWidth() - 400) / 2f,
                (org.argentumforge.engine.Window.INSTANCE.getHeight() - 350) / 2f,
                imgui.flag.ImGuiCond.Once);
        ImGui.setNextWindowSize(400, 350);

        int flags = ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoDocking;

        if (ImGui.begin("Acerca de Argentum Forge", flags)) {
            ImGui.spacing();

            // Title
            float windowWidth = ImGui.getWindowWidth();
            String title = "Argentum Forge";
            float titleWidth = ImGui.calcTextSize(title).x;
            ImGui.setCursorPosX((windowWidth - titleWidth) / 2f);
            ImGui.textColored(Theme.COLOR_PRIMARY, title);

            // Version
            String version = "Versión Beta 1.0.0";
            float verWidth = ImGui.calcTextSize(version).x;
            ImGui.setCursorPosX((windowWidth - verWidth) / 2f);
            ImGui.textColored(0.7f, 0.7f, 0.7f, 1.0f, version);

            ImGui.spacing();
            ImGui.separator();
            ImGui.spacing();

            // Credits
            ImGui.text("Desarrollado por:");
            ImGui.sameLine();
            ImGui.textColored(Theme.COLOR_ACCENT, "Lorwik");

            ImGui.spacing();
            ImGui.textWrapped(
                    "Agradecimientos especiales a la comunidad de Argentum Online por mantener viva la leyenda.");

            ImGui.spacing();
            ImGui.separator();
            ImGui.spacing();

            // Links / Info
            ImGui.text("Repositorio:");
            ImGui.textColored(0.4f, 0.8f, 1.0f, 1.0f, "https://github.com/Lorwik/Argentum-Forge");
            if (ImGui.isItemHovered()) {
                ImGui.setTooltip("Click para copiar URL"); // ImGui doesn't support clickable links easily without extra
                                                           // logic
                if (ImGui.isMouseClicked(0)) {
                    ImGui.setClipboardText("https://github.com/Lorwik/Argentum-Forge");
                }
            }

            ImGui.spacing();

            // License
            ImGui.textColored(Theme.COLOR_DANGER, "PROHIBIDO SU USO COMERCIAL");
            ImGui.textWrapped(
                    "Este software es libre y de código abierto bajo licencia MIT. Su venta o distribución comercial está estrictamente prohibida sin autorización expresa.");

            ImGui.spacing();
            ImGui.separator();
            ImGui.spacing();

            // Close Button
            if (ImGui.button("Cerrar", -1, 0)) { // -1 width = fill
                this.close();
            }
        }
        ImGui.end();
    }
}
