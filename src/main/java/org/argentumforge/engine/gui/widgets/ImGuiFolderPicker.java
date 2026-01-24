package org.argentumforge.engine.gui.widgets;

import imgui.ImGui;
import imgui.type.ImString;

import java.io.File;

public class ImGuiFolderPicker {

    public enum Mode {
        FOLDER_ONLY,
        FOLDER_AND_FILE
    }

    private final Mode mode;

    private File currentDir;
    private File selectedDir;
    private File resultFile;
    private boolean open;

    // Solo para modo archivo
    private ImString fileName;
    private String extension;

    /* =========================
       CONSTRUCTORES
       ========================= */

    // 🔹 MODO CARPETA (FRoutes / Wizard)
    public ImGuiFolderPicker(File startDir) {
        this.mode = Mode.FOLDER_ONLY;
        this.currentDir = startDir != null ? startDir : defaultDir();
    }

    // 🔹 MODO ARCHIVO (Save / Export)
    public ImGuiFolderPicker(File startDir, String defaultName, String extension) {
        this.mode = Mode.FOLDER_AND_FILE;
        this.currentDir = startDir != null ? startDir : defaultDir();
        this.fileName = new ImString(defaultName, 256);
        this.extension = extension.startsWith(".") ? extension : "." + extension;
    }

    private File defaultDir() {
        return new File(System.getProperty("user.home"));
    }

    /* ========================= */

    public void open() {
        open = true;
    }

    public boolean isOpen() {
        return open;
    }

    // 🔹 PARA FRoutes / Wizard
    public File getSelectedDir() {
        return selectedDir;
    }

    // 🔹 PARA Save / Export
    public File getResultFile() {
        return resultFile;
    }

    /* ========================= */

    public void render() {
        if (!open) return;

        ImGui.setNextWindowSize(520, 420);
        if (!ImGui.begin("Seleccionar carpeta")) {
            ImGui.end();
            return;
        }

        ImGui.textWrapped(currentDir.getAbsolutePath());
        ImGui.separator();

        // Subir
        if (currentDir.getParentFile() != null) {
            if (ImGui.button("..")) {
                currentDir = currentDir.getParentFile();
            }
        }

        ImGui.separator();

        File[] dirs = currentDir.listFiles(File::isDirectory);
        if (dirs != null) {
            java.util.Arrays.sort(dirs, java.util.Comparator.comparing(File::getName));
            for (File dir : dirs) {
                if (ImGui.selectable(dir.getName() + "/")) {
                    currentDir = dir;
                }
            }
        }

        ImGui.separator();

        // =========================
        // INPUT NOMBRE (solo archivo)
        // =========================
        if (mode == Mode.FOLDER_AND_FILE) {
            ImGui.text("Nombre de archivo:");
            ImGui.inputText("##filename", fileName);
            ImGui.separator();
        }

        // =========================
        // BOTONES
        // =========================
        if (ImGui.button("Aceptar", 120, 0)) {

            if (mode == Mode.FOLDER_ONLY) {
                selectedDir = currentDir;
                open = false;
            } else {
                String name = fileName.get().trim();
                if (!name.isEmpty()) {
                    if (!name.toLowerCase().endsWith(extension)) {
                        name += extension;
                    }
                    resultFile = new File(currentDir, name);
                    open = false;
                }
            }
        }

        ImGui.sameLine();

        if (ImGui.button("Cancelar", 120, 0)) {
            open = false;
        }

        ImGui.end();
    }
}
