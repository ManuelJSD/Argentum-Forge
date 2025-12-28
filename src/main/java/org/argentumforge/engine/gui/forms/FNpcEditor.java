package org.argentumforge.engine.gui.forms;

import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import org.argentumforge.engine.utils.editor.Npc;
import org.argentumforge.engine.utils.inits.NpcData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.argentumforge.engine.utils.GameData.npcs;

public final class FNpcEditor extends Form {

    private int selectedNpcNumber = -1;
    private final Npc npcEditor;

    public FNpcEditor() {
        npcEditor = Npc.getInstance();

        if (npcs != null && !npcs.isEmpty()) {
            selectedNpcNumber = npcs.keySet().stream().min(Integer::compareTo).orElse(-1);
            if (selectedNpcNumber > 0) npcEditor.setNpcNumber(selectedNpcNumber);
        }
    }

    @Override
    public void render() {
        ImGui.setNextWindowSize(260, 320, ImGuiCond.Always);
        ImGui.begin(this.getClass().getSimpleName(), ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoResize);

        ImGui.text("NPCs:");
        ImGui.separator();

        drawNpcList();
        ImGui.separator();

        drawButtons();

        ImGui.end();
    }

    private void drawNpcList() {
        ImGui.beginChild("NpcListChild", 0, 220, true);

        if (npcs == null || npcs.isEmpty()) {
            ImGui.textDisabled("NPCs no cargados");
            ImGui.endChild();
            return;
        }

        List<Integer> keys = new ArrayList<>(npcs.keySet());
        keys.sort(Comparator.naturalOrder());

        for (Integer npcNumber : keys) {
            NpcData data = npcs.get(npcNumber);
            if (data == null) continue;

            String label = "NPC " + npcNumber + " - " + data.getName();
            if (ImGui.selectable(label, selectedNpcNumber == npcNumber)) {
                selectedNpcNumber = npcNumber;
                npcEditor.setNpcNumber(npcNumber);

                if (npcEditor.getMode() == 1) {
                    npcEditor.setNpcNumber(selectedNpcNumber);
                }
            }
        }

        ImGui.endChild();
    }

    private void drawButtons() {
        int activeColor = 0xFF00FF00; // verde
        int currentMode = npcEditor.getMode();

        boolean pushQuitar = false;
        if (currentMode == 2) {
            ImGui.pushStyleColor(ImGuiCol.Button, activeColor);
            pushQuitar = true;
        }
        if (ImGui.button("Quitar", 110, 30)) {
            if (currentMode == 2) {
                npcEditor.setMode(0);
            } else {
                npcEditor.setMode(2);
            }
        }
        if (pushQuitar) ImGui.popStyleColor();

        ImGui.sameLine();

        boolean pushColocar = false;
        boolean placeEnabled = selectedNpcNumber > 0;
        if (currentMode == 1) {
            ImGui.pushStyleColor(ImGuiCol.Button, activeColor);
            pushColocar = true;
        }
        if (!placeEnabled) ImGui.pushStyleColor(ImGuiCol.Button, 0x88888888);
        if (ImGui.button("Colocar", 110, 30)) {
            if (!placeEnabled) {
                // no hacer nada
            } else if (currentMode == 1) {
                npcEditor.setMode(0);
            } else {
                npcEditor.setMode(1);
                npcEditor.setNpcNumber(selectedNpcNumber);
            }
        }
        if (!placeEnabled) ImGui.popStyleColor();
        if (pushColocar) ImGui.popStyleColor();

        if (selectedNpcNumber > 0 && npcs != null) {
            NpcData selected = npcs.get(selectedNpcNumber);
            if (selected != null) {
                ImGui.separator();
                ImGui.textDisabled("Seleccionado:");
                ImGui.textDisabled("Nro: " + selected.getNumber());
                ImGui.textDisabled("Nombre: " + selected.getName());
                ImGui.textDisabled("Head: " + selected.getHead() + "  Body: " + selected.getBody());
            }
        }
    }
}
