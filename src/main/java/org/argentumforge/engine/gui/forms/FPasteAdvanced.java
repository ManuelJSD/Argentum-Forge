package org.argentumforge.engine.gui.forms;

import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import org.argentumforge.engine.game.EditorController;
import org.argentumforge.engine.i18n.I18n;
import org.argentumforge.engine.utils.editor.Clipboard;

public class FPasteAdvanced extends Form {

    private final Clipboard.PasteSettings settings;

    private boolean firstRun = true;

    public FPasteAdvanced() {
        this.settings = Clipboard.getInstance().getSettings();
    }

    @Override
    public void render() {
        if (firstRun) {
            ImGui.setNextWindowFocus();
            firstRun = false;
        }
        ImGui.setNextWindowSize(350, 320, ImGuiCond.Always);

        if (ImGui.begin(I18n.INSTANCE.get("options.paste.title"),
                ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoResize)) {

            ImGui.textDisabled(I18n.INSTANCE.get("options.paste.layers"));
            ImGui.separator();

            ImGui.columns(2, "PasteLayers", false);
            for (int i = 0; i < 4; i++) {
                if (ImGui.checkbox(I18n.INSTANCE.get("options.paste.layer") + " " + (i + 1), settings.layers[i])) {
                    settings.layers[i] = !settings.layers[i];
                }
                ImGui.nextColumn();
            }
            ImGui.columns(1);

            ImGui.dummy(0, 10);
            ImGui.textDisabled(I18n.INSTANCE.get("options.paste.attributes"));
            ImGui.separator();

            ImGui.columns(2, "PasteAttrs", false);
            if (ImGui.checkbox(I18n.INSTANCE.get("options.paste.blocked"), settings.blocked)) {
                settings.blocked = !settings.blocked;
            }
            ImGui.nextColumn();
            if (ImGui.checkbox(I18n.INSTANCE.get("options.paste.npc"), settings.npc)) {
                settings.npc = !settings.npc;
            }
            ImGui.nextColumn();
            if (ImGui.checkbox(I18n.INSTANCE.get("options.paste.objects"), settings.objects)) {
                settings.objects = !settings.objects;
            }
            ImGui.nextColumn();
            if (ImGui.checkbox(I18n.INSTANCE.get("options.paste.triggers"), settings.triggers)) {
                settings.triggers = !settings.triggers;
            }
            ImGui.nextColumn();
            if (ImGui.checkbox(I18n.INSTANCE.get("options.paste.transitions"), settings.transitions)) {
                settings.transitions = !settings.transitions;
            }
            ImGui.nextColumn();
            if (ImGui.checkbox(I18n.INSTANCE.get("options.paste.particles"), settings.particles)) {
                settings.particles = !settings.particles;
            }
            ImGui.columns(1);

            ImGui.dummy(0, 20);
            ImGui.separator();
            ImGui.dummy(0, 5);

            float buttonWidth = (ImGui.getWindowWidth() - 30) / 2;
            if (ImGui.button(I18n.INSTANCE.get("options.paste.commit"), buttonWidth, 35)) {
                EditorController.INSTANCE.pasteSelection();
                close();
            }
            ImGui.sameLine();
            if (ImGui.button(I18n.INSTANCE.get("options.paste.cancel"), buttonWidth, 35)) {
                close();
            }

            ImGui.end();
        }
    }
}
