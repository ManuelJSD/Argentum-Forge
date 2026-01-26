package org.argentumforge.engine.gui.widgets;

import imgui.ImGui;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

import imgui.ImGui;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

public class ImGuiFilePicker {

    private File currentDir;
    private File selectedFile;
    private boolean open;
    private final String extension; // ej: "map"

    public ImGuiFilePicker(File startDir, String extension) {
        this.currentDir = startDir != null
                ? startDir
                : new File(System.getProperty("user.home"));
        this.extension = extension;
    }

    public void open() {
        open = true;
        selectedFile = null;
    }

    public boolean isOpen() {
        return open;
    }

    public boolean hasResult() {
        return selectedFile != null;
    }

    public File getSelectedFile() {
        return selectedFile;
    }

    public void render() {
        if (!open) return;

        ImGui.setNextWindowSize(520, 420);
        if (!ImGui.begin("Seleccionar archivo")) {
            ImGui.end();
            return;
        }

        ImGui.textWrapped(currentDir.getAbsolutePath());
        ImGui.separator();

        // Subir nivel
        if (currentDir.getParentFile() != null) {
            if (ImGui.button("..")) {
                currentDir = currentDir.getParentFile();
            }
        }

        ImGui.separator();

        File[] files = currentDir.listFiles();
        if (files != null) {
            Arrays.sort(files, Comparator.comparing(File::getName));

            for (File f : files) {
                if (f.isDirectory()) {
                    if (ImGui.selectable("[DIR] " + f.getName())) {
                        currentDir = f;
                    }
                } else if (f.getName().toLowerCase().endsWith("." + extension)) {
                    if (ImGui.selectable(f.getName())) {
                        selectedFile = f;
                        open = false;
                    }
                }
            }
        }

        ImGui.separator();

        if (ImGui.button("Cancelar")) {
            open = false;
        }

        ImGui.end();
    }
}

