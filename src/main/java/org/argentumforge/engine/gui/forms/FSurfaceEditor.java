package org.argentumforge.engine.gui.forms;

import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImInt;
import org.argentumforge.engine.utils.editor.Surface;
import org.argentumforge.engine.utils.inits.GrhData;
import org.argentumforge.engine.utils.inits.GrhInfo;

import java.util.ArrayList;
import java.util.List;

import static org.argentumforge.engine.utils.GameData.*;

public class FSurfaceEditor extends Form {

    private int selectedGrhIndex = -1;      // índice seleccionado en la lista principal (grhData)
    private final ImInt selectedLayer = new ImInt(0); // índice seleccionado en el ComboBox de capas
    private final List<Integer> capas = new ArrayList<>(List.of(1, 2, 3, 4));

    private Surface surface;


    public FSurfaceEditor() {
        surface = Surface.getInstance();
        selectedLayer.set(0); // Comienza en la primera capa
    }

    @Override
    public void render() {
        ImGui.setNextWindowSize(210, 300, ImGuiCond.Always);
        ImGui.begin(this.getClass().getSimpleName(),
                ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoResize);

        ImGui.text("Superficies:");
        ImGui.separator();

        drawGrhList();
        ImGui.separator();

        drawCapasCombo();
        ImGui.separator();

        drawButtons();

        ImGui.end();
    }

    private void drawButtons() {
        if (ImGui.button("Borrar")) {
            surface.setMode(2);
        }

        ImGui.sameLine();

        if (ImGui.button("Insertar")) {
            surface.setMode(1);
            surface.setSurfaceIndex(selectedGrhIndex);
        }
    }

    private void drawGrhList() {
        ImGui.beginChild("GrhListChild", 0, 150, true);

        if (grhData != null) {
            for (int i = 1; i < grhData.length; i++) {
                GrhData g = grhData[i];
                if (g == null) continue;

                String label = String.format("GRH %d - %d frames", i, g.getNumFrames());
                if (ImGui.selectable(label, selectedGrhIndex == i)) {
                    selectedGrhIndex = i;
                    ImGui.setScrollHereY();
                }
            }
        } else {
            ImGui.textDisabled("grhData no cargado");
        }

        ImGui.endChild();

        if (selectedGrhIndex > 0 && grhData != null && grhData[selectedGrhIndex] != null) {
            GrhData g = grhData[selectedGrhIndex];
            /*ImGui.text("Detalles:");
            ImGui.text("Indice: " + selectedGrhIndex);
            ImGui.text("Frames: " + g.getNumFrames());
            ImGui.text("Pixel W: " + g.getPixelWidth());
            ImGui.text("Pixel H: " + g.getPixelHeight());*/
        }
    }

    private void drawCapasCombo() {
        ImGui.text("Capas:");
        String[] labels = capas.stream()
                .map(n -> "Capa " + n)
                .toArray(String[]::new);

        if (labels.length > 0) {
            // Si el usuario cambia la selección del combo...
            if (ImGui.combo("##capasCombo", selectedLayer, labels, labels.length)) {
                int capaSeleccionada = capas.get(selectedLayer.get());
                surface.setLayer(capaSeleccionada); // actualiza la capa activa directamente
            }
        } else {
            ImGui.textDisabled("Sin capas");
        }
    }

}