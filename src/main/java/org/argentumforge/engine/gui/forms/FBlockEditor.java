package org.argentumforge.engine.gui.forms;

import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import org.argentumforge.engine.utils.editor.Block;
import org.argentumforge.engine.renderer.RenderSettings;
import static org.argentumforge.engine.utils.GameData.options;

/**
 * Editor de bloqueos del mapa.
 * Permite bloquear y desbloquear tiles, así como visualizar los bloqueos
 * existentes.
 */
public class FBlockEditor extends Form {

    private Block block;
    // private int activeMode = 0; // Removed local state to avoid desync

    public FBlockEditor() {
        block = Block.getInstance();
    }

    @Override
    public void render() {
        ImGui.setNextWindowSize(210, 160, ImGuiCond.Always);
        ImGui.begin(this.getClass().getSimpleName(),
                ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoResize);

        ImGui.text("Bloqueos:");
        ImGui.separator();

        drawButtons();
        ImGui.separator();

        drawShowBlocksCheckbox();

        ImGui.end();
    }

    private void drawButtons() {
        int normalColor = 0xFFFFFFFF; // blanco
        int activeColor = 0xFF00FF00; // verde

        int currentMode = block.getMode();

        // Botón Desbloquear
        boolean pushDesbloquear = false;
        if (currentMode == 2) {
            ImGui.pushStyleColor(ImGuiCol.Button, activeColor);
            pushDesbloquear = true;
        }
        if (ImGui.button("Desbloquear", 190, 30)) {
            if (currentMode == 2) {
                block.setMode(0);
            } else {
                block.setMode(2);
            }
        }
        if (pushDesbloquear)
            ImGui.popStyleColor();

        // Botón Bloquear
        boolean pushBloquear = false;
        if (currentMode == 1) {
            ImGui.pushStyleColor(ImGuiCol.Button, activeColor);
            pushBloquear = true;
        }
        if (ImGui.button("Bloquear", 190, 30)) {
            if (currentMode == 1) {
                block.setMode(0);
            } else {
                block.setMode(1);
            }
        }
        if (pushBloquear)
            ImGui.popStyleColor();
    }

    private void drawShowBlocksCheckbox() {
        RenderSettings renderSettings = options.getRenderSettings();
        if (ImGui.checkbox("Mostrar bloqueos", renderSettings.getShowBlock())) {
            renderSettings.setShowBlock(!renderSettings.getShowBlock());
            options.save();
        }

        if (renderSettings.getShowBlock()) {
            ImGui.textDisabled("Los tiles bloqueados se");
            ImGui.textDisabled("mostraran con un overlay rojo");
        }
    }
}
