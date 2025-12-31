package org.argentumforge.engine.gui.forms;

import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import org.argentumforge.engine.utils.editor.Obj;
import org.argentumforge.engine.utils.inits.ObjData;
import imgui.type.ImString;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.argentumforge.engine.utils.GameData.objs;

public final class FObjEditor extends Form {

    private int selectedObjNumber = -1;
    private final Obj objEditor;
    private final ImString searchFilter = new ImString(100);

    public FObjEditor() {
        objEditor = Obj.getInstance();

        if (objs != null && !objs.isEmpty()) {
            selectedObjNumber = objs.keySet().stream().min(Integer::compareTo).orElse(-1);
            if (selectedObjNumber > 0)
                objEditor.setObjNumber(selectedObjNumber);
        }
    }

    @Override
    public void render() {
        ImGui.setNextWindowSize(260, 360, ImGuiCond.Always);
        ImGui.begin(this.getClass().getSimpleName(), ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoResize);

        ImGui.text("Objetos:");
        ImGui.separator();

        ImGui.inputText("Buscar", searchFilter);
        ImGui.separator();

        drawObjList();
        ImGui.separator();

        drawButtons();

        ImGui.end();
    }

    private void drawObjList() {
        ImGui.beginChild("ObjListChild", 0, 220, true);

        if (objs == null || objs.isEmpty()) {
            ImGui.textDisabled("Objetos no cargados");
            ImGui.endChild();
            return;
        }

        List<Integer> keys = new ArrayList<>(objs.keySet());
        keys.sort(Comparator.naturalOrder());

        for (Integer objNumber : keys) {
            ObjData data = objs.get(objNumber);
            if (data == null)
                continue;

            String label = "OBJ " + objNumber + " - " + data.getName();
            if (!searchFilter.get().isEmpty() && !label.toLowerCase().contains(searchFilter.get().toLowerCase())) {
                continue;
            }

            if (ImGui.selectable(label, selectedObjNumber == objNumber)) {
                selectedObjNumber = objNumber;
                objEditor.setObjNumber(objNumber);

                if (objEditor.getMode() == 1) {
                    objEditor.setObjNumber(selectedObjNumber);
                }
            }
        }

        ImGui.endChild();
    }

    private void drawButtons() {
        int activeColor = 0xFF00FF00; // verde
        int currentMode = objEditor.getMode();

        boolean pushQuitar = false;
        if (currentMode == 2) {
            ImGui.pushStyleColor(ImGuiCol.Button, activeColor);
            pushQuitar = true;
        }
        if (ImGui.button("Quitar", 110, 30)) {
            if (currentMode == 2) {
                objEditor.setMode(0);
            } else {
                objEditor.setMode(2);
            }
        }
        if (pushQuitar)
            ImGui.popStyleColor();

        ImGui.sameLine();

        boolean pushColocar = false;
        boolean placeEnabled = selectedObjNumber > 0;
        if (currentMode == 1) {
            ImGui.pushStyleColor(ImGuiCol.Button, activeColor);
            pushColocar = true;
        }
        if (!placeEnabled)
            ImGui.pushStyleColor(ImGuiCol.Button, 0x88888888);
        if (ImGui.button("Colocar", 110, 30)) {
            if (!placeEnabled) {
                // no hacer nada
            } else if (currentMode == 1) {
                objEditor.setMode(0);
            } else {
                objEditor.setMode(1);
                objEditor.setObjNumber(selectedObjNumber);
            }
        }
        if (!placeEnabled)
            ImGui.popStyleColor();
        if (pushColocar)
            ImGui.popStyleColor();

        if (selectedObjNumber > 0 && objs != null) {
            ObjData selected = objs.get(selectedObjNumber);
            if (selected != null) {
                ImGui.separator();
                ImGui.textDisabled("Seleccionado:");
                ImGui.textDisabled("Nro: " + selected.getNumber());
                ImGui.textDisabled("Nombre: " + selected.getName());
                ImGui.textDisabled("GrhIndex: " + selected.getGrhIndex());
            }
        }
    }
}
