package org.argentumforge.engine.gui.forms;

import imgui.ImGui;
import imgui.type.ImInt;
import org.argentumforge.engine.gui.widgets.UIComponents;
import org.argentumforge.engine.i18n.I18n;
import org.argentumforge.engine.utils.editor.Particle;

public class FParticleEditor extends Form {

    private final ImInt particleId = new ImInt(0);
    private final ImInt brushSize = new ImInt(1);

    public FParticleEditor() {
    }

    @Override
    public void render() {
        // Auto-activar visualización de partículas
        if (!org.argentumforge.engine.utils.GameData.options.getRenderSettings().getShowParticles()) {
            org.argentumforge.engine.utils.GameData.options.getRenderSettings().setShowParticles(true);
        }

        ImGui.setNextWindowSize(200, 180);
        if (ImGui.begin(I18n.INSTANCE.get("menu.view.particles"))) {
            Particle particleTool = Particle.getInstance();

            boolean isInsertMode = particleTool.getMode() == 1 && particleTool.getSelectedParticleId() > 0;
            boolean isDeleteMode = particleTool.getMode() == 1 && particleTool.getSelectedParticleId() == 0;

            if (UIComponents.toggleButton(I18n.INSTANCE.get("common.insert"), isInsertMode)) {
                particleTool.setMode(1);
                if (particleId.get() <= 0)
                    particleId.set(1);
                particleTool.setSelectedParticleId(particleId.get());
            }

            ImGui.sameLine();

            if (UIComponents.toggleButton(I18n.INSTANCE.get("common.delete"), isDeleteMode)) {
                particleTool.setMode(1);
                particleTool.setSelectedParticleId(0);
            }

            ImGui.separator();

            ImGui.text(I18n.INSTANCE.get("menu.view.particles") + " ID:");
            if (ImGui.inputInt("##partId", particleId)) {
                if (particleId.get() < 0)
                    particleId.set(0);

                if (particleId.get() > 0) {
                    // Si el usuario cambia el input a > 0, asumimos modo insertar
                    particleTool.setMode(1);
                    particleTool.setSelectedParticleId(particleId.get());
                } else {
                    // Si pone 0, es modo borrar
                    particleTool.setMode(1);
                    particleTool.setSelectedParticleId(0);
                }
            }

            ImGui.text(I18n.INSTANCE.get("surface.brushSize") + ":");
            if (ImGui.sliderInt("##brushSize", brushSize.getData(), 1, 9)) {
                particleTool.setBrushSize(brushSize.get());
            }

            ImGui.separator();

            if (particleTool.getMode() == 1) {
                if (particleTool.getSelectedParticleId() > 0) {
                    ImGui.textColored(0xFF00FF00, "Modo: INSERTAR (ID: " + particleTool.getSelectedParticleId() + ")");
                } else {
                    ImGui.textColored(0xFF0000FF, "Modo: ELIMINAR");
                }
            } else {
                ImGui.textColored(0xFFAAAAAA, "Modo: INACTIVO");
            }

            ImGui.end();
        }
    }
}
