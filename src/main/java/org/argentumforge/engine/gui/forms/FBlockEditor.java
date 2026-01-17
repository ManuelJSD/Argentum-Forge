package org.argentumforge.engine.gui.forms;

import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import org.argentumforge.engine.utils.editor.Block;
import org.argentumforge.engine.renderer.RenderSettings;
import org.argentumforge.engine.gui.Theme;
import org.argentumforge.engine.gui.widgets.UIComponents;
import org.argentumforge.engine.i18n.I18n;
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
        ImGui.setNextWindowSize(230, 360, ImGuiCond.FirstUseEver);
        if (!ImGui.begin(I18n.INSTANCE.get("editor.block"), ImGuiWindowFlags.None)) {
            ImGui.end();
            return;
        }

        ImGui.text(I18n.INSTANCE.get("editor.block.blocks"));
        ImGui.separator();

        drawButtons();
        ImGui.separator();

        drawShowBlocksCheckbox();

        ImGui.end();
    }

    private void drawButtons() {
        int currentMode = block.getMode();

        // Botón Borrar (Destructivo - usa rojo)
        if (currentMode == 2) {
            ImGui.pushStyleColor(ImGuiCol.Button, Theme.COLOR_DANGER);
        }
        if (ImGui.button(I18n.INSTANCE.get("editor.block.eraseShort"), 65, 30)) {
            block.setMode(currentMode == 2 ? 0 : 2);
            if (block.getMode() != 0)
                options.getRenderSettings().setShowBlock(true);
        }
        if (currentMode == 2) {
            ImGui.popStyleColor();
        }

        ImGui.sameLine();

        // Botón Bloquear (Activo - usa verde)
        if (UIComponents.toggleButton(I18n.INSTANCE.get("editor.block.blockShort"), currentMode == 1, 65, 30)) {
            block.setMode(currentMode == 1 ? 0 : 1);
            if (block.getMode() != 0)
                options.getRenderSettings().setShowBlock(true);
        }

        ImGui.sameLine();

        // Botón Invertir (Primario - usa azul)
        if (currentMode == 3) {
            ImGui.pushStyleColor(ImGuiCol.Button, Theme.COLOR_PRIMARY);
        }
        if (ImGui.button(I18n.INSTANCE.get("editor.block.invertShort"), 65, 30)) {
            block.setMode(currentMode == 3 ? 0 : 3);
            if (block.getMode() != 0)
                options.getRenderSettings().setShowBlock(true);
        }
        if (currentMode == 3) {
            ImGui.popStyleColor();
        }

        ImGui.spacing();
        ImGui.text(I18n.INSTANCE.get("editor.block.shape"));
        if (ImGui.radioButton(I18n.INSTANCE.get("editor.surface.square"),
                block.getBrushShape() == Block.BrushShape.SQUARE)) {
            block.setBrushShape(Block.BrushShape.SQUARE);
        }
        ImGui.sameLine();
        if (ImGui.radioButton(I18n.INSTANCE.get("editor.surface.circle"),
                block.getBrushShape() == Block.BrushShape.CIRCLE)) {
            block.setBrushShape(Block.BrushShape.CIRCLE);
        }

        ImGui.spacing();
        ImGui.text(I18n.INSTANCE.get("editor.block.brush"));
        ImGui.sameLine();
        int currentBrush = block.getBrushSize();

        if (UIComponents.toggleButton("1x1", currentBrush == 1, 50, 25))
            block.setBrushSize(1);

        ImGui.sameLine();

        if (UIComponents.toggleButton("3x3", currentBrush == 3, 50, 25))
            block.setBrushSize(3);

        ImGui.sameLine();

        if (UIComponents.toggleButton("5x5", currentBrush == 5, 50, 25))
            block.setBrushSize(5);

        ImGui.spacing();
        ImGui.separator();
        ImGui.text(I18n.INSTANCE.get("editor.block.globalActions"));

        if (ImGui.button(I18n.INSTANCE.get("editor.block.blockBorders"), 210, 25)) {
            ImGui.openPopup(I18n.INSTANCE.get("editor.block.confirm.blockBorders.title"));
        }

        if (ImGui.button(I18n.INSTANCE.get("editor.block.blockAll"), 210, 25)) {
            ImGui.openPopup(I18n.INSTANCE.get("editor.block.confirm.blockAll.title"));
        }

        if (ImGui.button(I18n.INSTANCE.get("editor.block.clearBorders"), 210, 25)) {
            ImGui.openPopup(I18n.INSTANCE.get("editor.block.confirm.clearBorders.title"));
        }

        if (ImGui.button(I18n.INSTANCE.get("editor.block.clearAll"), 210, 25)) {
            ImGui.openPopup(I18n.INSTANCE.get("editor.block.confirm.clearAll.title"));
        }

        // --- Modales de Confirmación ---
        UIComponents.confirmDialog(
                I18n.INSTANCE.get("editor.block.confirm.blockBorders.title"),
                I18n.INSTANCE.get("editor.block.blockBorders"),
                I18n.INSTANCE.get("editor.block.confirm.blockBorders.msg"),
                () -> {
                    block.blockBorders();
                    options.getRenderSettings().setShowBlock(true);
                });

        UIComponents.confirmDialog(
                I18n.INSTANCE.get("editor.block.confirm.blockAll.title"),
                I18n.INSTANCE.get("editor.block.blockAll"),
                I18n.INSTANCE.get("editor.block.confirm.blockAll.msg"),
                () -> {
                    block.blockAll();
                    options.getRenderSettings().setShowBlock(true);
                });

        UIComponents.confirmDialog(
                I18n.INSTANCE.get("editor.block.confirm.clearBorders.title"),
                I18n.INSTANCE.get("editor.block.clearBorders"),
                I18n.INSTANCE.get("editor.block.confirm.clearBorders.msg"),
                () -> block.unblockBorders());

        UIComponents.confirmDialog(
                I18n.INSTANCE.get("editor.block.confirm.clearAll.title"),
                I18n.INSTANCE.get("editor.block.clearAll"),
                I18n.INSTANCE.get("editor.block.confirm.clearAll.msg"),
                () -> block.unblockAll());

        ImGui.separator();
        ImGui.text(I18n.INSTANCE.get("editor.block.opacity"));
        float[] opacity = { options.getRenderSettings().getBlockOpacity() };
        if (ImGui.sliderFloat("##opacity", opacity, 0.0f, 1.0f)) {
            options.getRenderSettings().setBlockOpacity(opacity[0]);
        }
    }

    private void drawShowBlocksCheckbox() {
        RenderSettings renderSettings = options.getRenderSettings();
        if (ImGui.checkbox(I18n.INSTANCE.get("editor.block.showBlocks"), renderSettings.getShowBlock())) {
            renderSettings.setShowBlock(!renderSettings.getShowBlock());
            options.save();
        }

        if (renderSettings.getShowBlock()) {
            ImGui.textDisabled(I18n.INSTANCE.get("editor.block.blocksDesc1"));
            ImGui.textDisabled(I18n.INSTANCE.get("editor.block.blocksDesc2"));
        }
    }
}
