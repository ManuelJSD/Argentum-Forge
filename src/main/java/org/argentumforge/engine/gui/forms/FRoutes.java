package org.argentumforge.engine.gui.forms;

import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImString;
import org.argentumforge.engine.game.Options;
import org.argentumforge.engine.gui.widgets.ImGuiFolderPicker;


import java.io.File;

public class FRoutes extends Form {
    private ImGuiFolderPicker folderPicker;
    private ImString pendingTarget;

    private final Options options = Options.INSTANCE;
    private final ImString graphicsPath = new ImString(options.getGraphicsPath(), 256);
    private final ImString datsPath = new ImString(options.getDatsPath(), 256);
    private final ImString initPath = new ImString(options.getInitPath(), 256);
    private final ImString musicPath = new ImString(options.getMusicPath(), 256);

    @Override
    public void render() {
        ImGui.setNextWindowSize(400, 220, ImGuiCond.Always);

        if (ImGui.begin(org.argentumforge.engine.i18n.I18n.INSTANCE.get("options.paths.title"),
                ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoResize)) {

            renderPathSelector(org.argentumforge.engine.i18n.I18n.INSTANCE.get("options.paths.graphics"), graphicsPath);
            renderPathSelector(org.argentumforge.engine.i18n.I18n.INSTANCE.get("options.paths.dats"), datsPath);
            renderPathSelector(org.argentumforge.engine.i18n.I18n.INSTANCE.get("options.paths.inits"), initPath);
            renderPathSelector(org.argentumforge.engine.i18n.I18n.INSTANCE.get("options.paths.music"), musicPath);

            ImGui.separator();

            if (ImGui.button(org.argentumforge.engine.i18n.I18n.INSTANCE.get("common.save"))) {
                options.setGraphicsPath(graphicsPath.get());
                options.setDatsPath(datsPath.get());
                options.setInitPath(initPath.get());
                options.setMusicPath(musicPath.get());
                options.save();
                org.argentumforge.engine.utils.GameData.init();
                this.close();
            }
            ImGui.sameLine();
            if (ImGui.button(org.argentumforge.engine.i18n.I18n.INSTANCE.get("common.cancel"))) {
                this.close();
            }


            ImGui.end();


            if (folderPicker != null && folderPicker.isOpen()) {
                folderPicker.render();

                File selected = folderPicker.getSelectedDir();
                if (selected != null && pendingTarget != null) {
                    pendingTarget.set(selected.getAbsolutePath());
                    pendingTarget = null;
                    folderPicker = null;
                }
            }

        }
    }

    private void renderPathSelector(String label, ImString path) {
        ImGui.text(label);
        ImGui.pushID(label);

        ImGui.pushItemWidth(280);
        ImGui.inputText("##path", path, ImGuiInputTextFlags.ReadOnly);
        ImGui.popItemWidth();

        ImGui.sameLine();

        if (ImGui.button("...")) {
            File startDir =
                    (path.get() != null && !path.get().isEmpty())
                            ? new File(path.get())
                            : new File(System.getProperty("user.home"));

            folderPicker = new ImGuiFolderPicker(startDir);
            folderPicker.open();
            pendingTarget = path;
        }

        ImGui.popID();
    }

}
