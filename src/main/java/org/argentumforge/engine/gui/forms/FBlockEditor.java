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
        ImGui.setNextWindowSize(230, 360, ImGuiCond.Always);
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
        if (ImGui.button("Borr", 65, 30)) {
            block.setMode(currentMode == 2 ? 0 : 2);
            if (block.getMode() != 0)
                options.getRenderSettings().setShowBlock(true);
        }
        if (pushDesbloquear)
            ImGui.popStyleColor();

        ImGui.sameLine();

        // Botón Bloquear
        boolean pushBloquear = false;
        if (currentMode == 1) {
            ImGui.pushStyleColor(ImGuiCol.Button, activeColor);
            pushBloquear = true;
        }
        if (ImGui.button("Bloq", 65, 30)) {
            block.setMode(currentMode == 1 ? 0 : 1);
            if (block.getMode() != 0)
                options.getRenderSettings().setShowBlock(true);
        }
        if (pushBloquear)
            ImGui.popStyleColor();

        ImGui.sameLine();

        // Botón Invertir
        boolean pushInvertir = false;
        if (currentMode == 3) {
            ImGui.pushStyleColor(ImGuiCol.Button, activeColor);
            pushInvertir = true;
        }
        if (ImGui.button("Inv", 65, 30)) {
            block.setMode(currentMode == 3 ? 0 : 3);
            if (block.getMode() != 0)
                options.getRenderSettings().setShowBlock(true);
        }
        if (pushInvertir)
            ImGui.popStyleColor();

        ImGui.spacing();
        ImGui.text("Pincel:");
        ImGui.sameLine();
        int currentBrush = block.getBrushSize();

        if (currentBrush == 1)
            ImGui.pushStyleColor(ImGuiCol.Button, activeColor);
        if (ImGui.button("1x1", 50, 25))
            block.setBrushSize(1);
        if (currentBrush == 1)
            ImGui.popStyleColor();

        ImGui.sameLine();

        if (currentBrush == 3)
            ImGui.pushStyleColor(ImGuiCol.Button, activeColor);
        if (ImGui.button("3x3", 50, 25))
            block.setBrushSize(3);
        if (currentBrush == 3)
            ImGui.popStyleColor();

        ImGui.sameLine();

        if (currentBrush == 5)
            ImGui.pushStyleColor(ImGuiCol.Button, activeColor);
        if (ImGui.button("5x5", 50, 25))
            block.setBrushSize(5);
        if (currentBrush == 5)
            ImGui.popStyleColor();

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
        if (ImGui.beginPopupModal("Confirmar Bloquear Bordes", ImGuiWindowFlags.AlwaysAutoResize)) {
            ImGui.text("Se bloquearán todos los bordes externos.\n¿Continuar?");
            if (ImGui.button("Sí", 120, 0)) {
                block.blockBorders();
                options.getRenderSettings().setShowBlock(true);
                ImGui.closeCurrentPopup();
            }
            ImGui.sameLine();
            if (ImGui.button("No", 120, 0)) {
                ImGui.closeCurrentPopup();
            }
            ImGui.endPopup();
        }

        if (ImGui.beginPopupModal("Confirmar Bloquear Todo", ImGuiWindowFlags.AlwaysAutoResize)) {
            ImGui.text("Se bloqueará TODO el mapa.\n¿Continuar?");
            if (ImGui.button("Sí", 120, 0)) {
                block.blockAll();
                options.getRenderSettings().setShowBlock(true);
                ImGui.closeCurrentPopup();
            }
            ImGui.sameLine();
            if (ImGui.button("Cancelar", 120, 0)) {
                ImGui.closeCurrentPopup();
            }
            ImGui.endPopup();
        }

        if (ImGui.beginPopupModal("Confirmar Limpiar Bordes", ImGuiWindowFlags.AlwaysAutoResize)) {
            ImGui.text("Se desbloquearán todos los bordes externos.\n¿Continuar?");
            if (ImGui.button("Sí", 120, 0)) {
                block.unblockBorders();
                ImGui.closeCurrentPopup();
            }
            ImGui.sameLine();
            if (ImGui.button("No", 120, 0)) {
                ImGui.closeCurrentPopup();
            }
            ImGui.endPopup();
        }

        if (ImGui.beginPopupModal("Confirmar Limpiar Todo", ImGuiWindowFlags.AlwaysAutoResize)) {
            ImGui.text("Se desbloqueará TODO el mapa.\nEsta acción no se puede deshacer.\n¿Estás seguro?");
            if (ImGui.button("Sí, limpiar", 120, 0)) {
                block.unblockAll();
                ImGui.closeCurrentPopup();
            }
            ImGui.sameLine();
            if (ImGui.button("Cancelar", 120, 0)) {
                ImGui.closeCurrentPopup();
            }
            ImGui.endPopup();
        }

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
