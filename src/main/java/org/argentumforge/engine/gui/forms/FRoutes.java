package org.argentumforge.engine.gui.forms;

import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImString;
import org.argentumforge.engine.game.Options;

import javax.swing.*;
import java.io.File;

public class FRoutes extends Form {

    private final Options options = Options.INSTANCE;
    private final ImString graphicsPath = new ImString(options.getGraphicsPath(), 256);
    private final ImString datsPath = new ImString(options.getDatsPath(), 256);
    private final ImString initPath = new ImString(options.getInitPath(), 256);
    private final ImString musicPath = new ImString(options.getMusicPath(), 256);

    @Override
    public void render() {
        ImGui.setNextWindowFocus();
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
        }
    }

    private void renderPathSelector(String label, ImString path) {
        ImGui.text(label);
        ImGui.pushID(label);
        if (ImGui.inputText("##" + label, path, ImGuiInputTextFlags.ReadOnly)) {
            // Read only field
        }
        ImGui.sameLine();
        if (ImGui.button("...")) {
            String selectedPath = selectFolder(path.get());
            if (selectedPath != null) {
                path.set(selectedPath);
            }
        }
        ImGui.popID();
    }

    private String selectFolder(String currentPath) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setCurrentDirectory(new File(currentPath.isEmpty() ? "." : currentPath));
        fileChooser.setDialogTitle(org.argentumforge.engine.i18n.I18n.INSTANCE.get("options.paths.selectFolder"));

        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            return fileChooser.getSelectedFile().getAbsolutePath();
        }
        return null;
    }

}
