package org.argentumforge.engine.gui.forms;

import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImString;
import org.argentumforge.engine.game.Options;

import java.io.File;

public class FRoutes extends Form {

    private final Options options = Options.INSTANCE;
    private final ImString graphicsPath = new ImString(options.getGraphicsPath(), 256);
    private final ImString datsPath = new ImString(options.getDatsPath(), 256);
    private final ImString initPath = new ImString(options.getInitPath(), 256);
    private final ImString musicPath = new ImString(options.getMusicPath(), 256);

    private Runnable onComplete;
    private Runnable onCancel;

    public FRoutes() {
        this(null, null);
    }

    public FRoutes(Runnable onComplete) {
        this(onComplete, null);
    }

    public FRoutes(Runnable onComplete, Runnable onCancel) {
        this.onComplete = onComplete;
        this.onCancel = onCancel;
    }

    @Override
    public void render() {
        ImGui.setNextWindowFocus();
        int windowWidth = 400;
        int windowHeight = 220;
        ImGui.setNextWindowPos(
                (org.argentumforge.engine.Engine.INSTANCE.getWindow().getWidth() - windowWidth) / 2f,
                (org.argentumforge.engine.Engine.INSTANCE.getWindow().getHeight() - windowHeight) / 2f,
                ImGuiCond.Always);
        ImGui.setNextWindowSize(windowWidth, windowHeight, ImGuiCond.Always);

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

                if (onComplete != null) {
                    onComplete.run();
                }

                this.close();
            }
            ImGui.sameLine();
            if (ImGui.button(org.argentumforge.engine.i18n.I18n.INSTANCE.get("common.cancel"))) {
                if (onCancel != null) {
                    onCancel.run();
                }
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
        if (currentPath == null || currentPath.isEmpty()) {
            currentPath = new File(".").getAbsolutePath();
        }

        return org.argentumforge.engine.gui.FileDialog.selectFolder(
                org.argentumforge.engine.i18n.I18n.INSTANCE.get("options.paths.selectFolder"),
                currentPath);
    }

}
