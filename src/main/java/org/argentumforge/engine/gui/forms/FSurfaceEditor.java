package org.argentumforge.engine.gui.forms;

import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImInt;
import org.argentumforge.engine.utils.editor.Surface;
import org.argentumforge.engine.utils.inits.GrhData;

import java.util.ArrayList;
import java.util.List;

import static org.argentumforge.engine.utils.GameData.*;

public class FSurfaceEditor extends Form {

    private int selectedGrhIndex = -1;      // índice seleccionado en la lista principal (grhData)
    private final ImInt selectedLayer = new ImInt(0); // índice seleccionado en el ComboBox de capas
    private final List<Integer> capas = new ArrayList<>(List.of(1, 2, 3, 4));

    private Surface surface;
    private int activeMode = 0; // 0 = ninguno, 1 = insertar, 2 = borrar

    public FSurfaceEditor() {
        surface = Surface.getInstance();

        // Seleccionar automáticamente el primer GRH si existe
        if (grhData != null && grhData.length > 1) {
            selectedGrhIndex = 1;
        } else {
            selectedGrhIndex = -1;
        }

        selectedLayer.set(0); // Primera capa
    }

    @Override
    public void render() {
        ImGui.setNextWindowSize(210, 260, ImGuiCond.Always);
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
        int normalColor = 0xFFFFFFFF; // blanco
        int activeColor = 0xFF00FF00; // verde

        // Botón Borrar
        boolean pushBorrar = false;
        if (activeMode == 2) {
            ImGui.pushStyleColor(ImGuiCol.Button, activeColor);
            pushBorrar = true;
        }
        if (ImGui.button("Borrar")) {
            if (activeMode == 2) {
                activeMode = 0;
                surface.setMode(0);
            } else {
                activeMode = 2;
                surface.setMode(2);
            }
        }
        if (pushBorrar) ImGui.popStyleColor();

        ImGui.sameLine();

        // Botón Insertar
        boolean pushInsertar = false;
        boolean insertEnabled = selectedGrhIndex > 0 && grhData != null;
        if (activeMode == 1) {
            ImGui.pushStyleColor(ImGuiCol.Button, activeColor);
            pushInsertar = true;
        }
        // Deshabilitar botón si no hay GRH
        if (!insertEnabled) ImGui.pushStyleColor(ImGuiCol.Button, 0x88888888); // gris
        if (ImGui.button("Insertar")) {
            if (!insertEnabled) {
                // no hacer nada
            } else if (activeMode == 1) {
                activeMode = 0;
                surface.setMode(0);
            } else {
                activeMode = 1;
                surface.setMode(1);
                surface.setSurfaceIndex(selectedGrhIndex);
            }
        }
        if (!insertEnabled) ImGui.popStyleColor();
        if (pushInsertar) ImGui.popStyleColor();
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

                    // Si Insertar está activo, actualizamos surfaceIndex al seleccionar otro GRH
                    if (activeMode == 1) {
                        surface.setSurfaceIndex(selectedGrhIndex);
                    }
                }
            }
        } else {
            ImGui.textDisabled("grhData no cargado");
        }

        ImGui.endChild();
    }

    private void drawCapasCombo() {
        ImGui.text("Capas:");
        String[] labels = capas.stream()
                .map(n -> "Capa " + n)
                .toArray(String[]::new);

        if (labels.length > 0) {
            if (ImGui.combo("##capasCombo", selectedLayer, labels, labels.length)) {
                int capaSeleccionada = capas.get(selectedLayer.get());
                surface.setLayer(capaSeleccionada); // actualiza la capa activa directamente
            }
        } else {
            ImGui.textDisabled("Sin capas");
        }
    }
}