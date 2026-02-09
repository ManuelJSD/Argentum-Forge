package org.argentumforge.engine.gui.forms;

import imgui.ImGui;

import org.argentumforge.engine.gui.Theme;

public class FAbout extends Form {

    @Override
    public void render() {
        // Center window
        ImGui.setNextWindowPos(
                (org.argentumforge.engine.Engine.INSTANCE.getWindow().getWidth() - 400) / 2f,
                (org.argentumforge.engine.Engine.INSTANCE.getWindow().getHeight() - 290) / 2f,
                imgui.flag.ImGuiCond.Once);
        ImGui.setNextWindowSize(400, 290, imgui.flag.ImGuiCond.Always);

        int flags = imgui.flag.ImGuiWindowFlags.NoCollapse | imgui.flag.ImGuiWindowFlags.NoResize
                | imgui.flag.ImGuiWindowFlags.NoDocking | imgui.flag.ImGuiWindowFlags.AlwaysAutoResize;

        if (ImGui.begin(org.argentumforge.engine.i18n.I18n.INSTANCE.get("about.title"), flags)) {
            ImGui.spacing();

            // Title
            float windowWidth = ImGui.getWindowWidth();
            String title = "Argentum Forge";
            float titleWidth = ImGui.calcTextSize(title).x;
            ImGui.setCursorPosX((windowWidth - titleWidth) / 2f);
            ImGui.textColored(Theme.COLOR_PRIMARY, title);

            // Version
            String version = "Versión " + org.argentumforge.engine.Engine.VERSION;
            float verWidth = ImGui.calcTextSize(version).x;
            ImGui.setCursorPosX((windowWidth - verWidth) / 2f);
            ImGui.textColored(0.7f, 0.7f, 0.7f, 1.0f, version);

            ImGui.spacing();
            ImGui.separator();
            ImGui.spacing();

            // Credits
            ImGui.text(org.argentumforge.engine.i18n.I18n.INSTANCE.get("about.developed"));
            ImGui.sameLine();
            ImGui.textColored(Theme.COLOR_ACCENT, "Lorwik");

            ImGui.spacing();
            ImGui.textWrapped(
                    "Agradecimientos especiales a la comunidad de Argentum Online por mantener viva la leyenda.");

            ImGui.spacing();
            ImGui.separator();
            ImGui.spacing();

            // Links / Info
            ImGui.text(org.argentumforge.engine.i18n.I18n.INSTANCE.get("about.repo"));
            ImGui.textColored(0.4f, 0.8f, 1.0f, 1.0f, "https://github.com/ManuelJSD/Argentum-Forge");
            if (ImGui.isItemHovered()) {
                ImGui.setTooltip(org.argentumforge.engine.i18n.I18n.INSTANCE.get("about.copy")); // ImGui doesn't
                                                                                                 // support clickable
                                                                                                 // links easily without
                                                                                                 // extra
                // logic
                if (ImGui.isMouseClicked(0)) {
                    ImGui.setClipboardText("https://github.com/ManuelJSD/Argentum-Forge");
                }
            }

            ImGui.spacing();

            // License
            ImGui.textColored(Theme.COLOR_DANGER,
                    org.argentumforge.engine.i18n.I18n.INSTANCE.get("about.license.title"));
            ImGui.textWrapped(
                    org.argentumforge.engine.i18n.I18n.INSTANCE.get("about.license.body"));

            ImGui.spacing();
            ImGui.separator();
            ImGui.spacing();

            // Close Button
            if (ImGui.button(org.argentumforge.engine.i18n.I18n.INSTANCE.get("common.close"), -1, 0)) { // -1 width =
                                                                                                        // fill
                this.close();
            }
        }
        ImGui.end();
    }
}
