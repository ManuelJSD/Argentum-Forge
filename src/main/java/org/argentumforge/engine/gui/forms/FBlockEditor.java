package org.argentumforge.engine.gui.forms;

import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import org.argentumforge.engine.utils.editor.Block;
import org.argentumforge.engine.renderer.RenderSettings;
import org.argentumforge.engine.gui.Theme;
import org.argentumforge.engine.gui.widgets.UIComponents;
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
        if (!ImGui.begin("Editor de Bloqueos", ImGuiWindowFlags.None)) {
            ImGui.end();
            return;
        }

        ImGui.text("Bloqueos:");
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
        if (ImGui.button("Borr", 65, 30)) {
            block.setMode(currentMode == 2 ? 0 : 2);
            if (block.getMode() != 0)
                options.getRenderSettings().setShowBlock(true);
        }
        if (currentMode == 2) {
            ImGui.popStyleColor();
        }

        ImGui.sameLine();

        // Botón Bloquear (Activo - usa verde)
        if (UIComponents.toggleButton("Bloq", currentMode == 1, 65, 30)) {
            block.setMode(currentMode == 1 ? 0 : 1);
            if (block.getMode() != 0)
                options.getRenderSettings().setShowBlock(true);
        }

        ImGui.sameLine();

        // Botón Invertir (Primario - usa azul)
        if (currentMode == 3) {
            ImGui.pushStyleColor(ImGuiCol.Button, Theme.COLOR_PRIMARY);
        }
        if (ImGui.button("Inv", 65, 30)) {
            block.setMode(currentMode == 3 ? 0 : 3);
            if (block.getMode() != 0)
                options.getRenderSettings().setShowBlock(true);
        }
        if (currentMode == 3) {
            ImGui.popStyleColor();
        }

        ImGui.spacing();
        ImGui.text("Forma:");
        if (ImGui.radioButton("Cuadrado", block.getBrushShape() == Block.BrushShape.SQUARE)) {
            block.setBrushShape(Block.BrushShape.SQUARE);
        }
        ImGui.sameLine();
        if (ImGui.radioButton("Circulo", block.getBrushShape() == Block.BrushShape.CIRCLE)) {
            block.setBrushShape(Block.BrushShape.CIRCLE);
        }

        ImGui.spacing();
        ImGui.text("Pincel:");
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
        ImGui.text("Acciones Globales:");

        if (ImGui.button("Bloquear Bordes", 210, 25)) {
            ImGui.openPopup("Confirmar Bloquear Bordes");
        }

        if (ImGui.button("Bloquear Todo", 210, 25)) {
            ImGui.openPopup("Confirmar Bloquear Todo");
        }

        if (ImGui.button("Limpiar Bordes", 210, 25)) {
            ImGui.openPopup("Confirmar Limpiar Bordes");
        }

        if (ImGui.button("Limpiar Todo", 210, 25)) {
            ImGui.openPopup("Confirmar Limpiar Todo");
        }

        // --- Modales de Confirmación ---
        UIComponents.confirmDialog(
                "Confirmar Bloquear Bordes",
                "Bloquear Bordes",
                "Se bloquearán todos los bordes externos.\n¿Continuar?",
                () -> {
                    block.blockBorders();
                    options.getRenderSettings().setShowBlock(true);
                });

        UIComponents.confirmDialog(
                "Confirmar Bloquear Todo",
                "Bloquear Todo",
                "Se bloqueará TODO el mapa.\n¿Continuar?",
                () -> {
                    block.blockAll();
                    options.getRenderSettings().setShowBlock(true);
                });

        UIComponents.confirmDialog(
                "Confirmar Limpiar Bordes",
                "Limpiar Bordes",
                "Se desbloquearán todos los bordes externos.\n¿Continuar?",
                () -> block.unblockBorders());

        UIComponents.confirmDialog(
                "Confirmar Limpiar Todo",
                "Limpiar Todo",
                "Se desbloqueará TODO el mapa.\nEsta acción no se puede deshacer.\n¿Estás seguro?",
                () -> block.unblockAll());

        ImGui.separator();
        ImGui.text("Opacidad:");
        float[] opacity = { options.getRenderSettings().getBlockOpacity() };
        if (ImGui.sliderFloat("##opacity", opacity, 0.0f, 1.0f)) {
            options.getRenderSettings().setBlockOpacity(opacity[0]);
        }
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
