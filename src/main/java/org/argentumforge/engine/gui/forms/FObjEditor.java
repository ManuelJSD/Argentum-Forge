package org.argentumforge.engine.gui.forms;

import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import org.argentumforge.engine.utils.editor.Obj;
import org.argentumforge.engine.utils.inits.ObjData;
import imgui.type.ImString;
import org.argentumforge.engine.game.console.Console;
import org.argentumforge.engine.game.console.FontStyle;
import org.argentumforge.engine.renderer.RGBColor;
import org.argentumforge.engine.gui.Theme;
import org.argentumforge.engine.gui.widgets.UIComponents;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.argentumforge.engine.utils.AssetRegistry.objs;

/**
 * Formulario de selección y previsualización de objetos para el editor.
 * 
 * Permite listar todos los objetos cargados en el AssetRegistry, buscarlos por
 * nombre o ID, y alternar entre vista de lista o rejilla visual.
 */
public final class FObjEditor extends Form {

    private int selectedObjNumber = -1;
    private final Obj objEditor;
    private final ImString searchFilter = new ImString(100);
    private boolean isGridView = false;
    private int itemsPerPage = 50;
    private int currentPage = 0;

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
        ImGui.setNextWindowSize(350, 550, ImGuiCond.FirstUseEver);
        if (ImGui.begin("Editor de Objetos", ImGuiWindowFlags.None)) {

            // Sincronizar selección si cambió externamente (por pick)
            if (objEditor.getObjNumber() != selectedObjNumber) {
                selectedObjNumber = objEditor.getObjNumber();
            }

            ImGui.text("Objetos:");
            ImGui.sameLine(ImGui.getWindowWidth() - 100);
            if (ImGui.button(isGridView ? "Ver Lista" : "Ver Rejilla")) {
                isGridView = !isGridView;
                currentPage = 0;
            }
            ImGui.separator();

            ImGui.pushItemWidth(-1);
            if (ImGui.inputTextWithHint("##BuscarObj", "ID o Nombre...", searchFilter)) {
                currentPage = 0;
            }
            ImGui.popItemWidth();
            ImGui.spacing();

            if (isGridView) {
                drawObjGrid();
            } else {
                drawObjList();
                ImGui.separator();
                drawPreview();
            }
            ImGui.separator();

            drawButtons();
        }
        ImGui.end();
    }

    private void drawPreview() {
        ImGui.text("Previsualizacion:");
        if (ImGui.beginChild("ObjPreview", 0, 100, true)) {
            if (selectedObjNumber > 0 && objs != null) {
                ObjData data = objs.get(selectedObjNumber);
                if (data != null && data.getGrhIndex() > 0) {
                    org.argentumforge.engine.gui.PreviewUtils.drawGrh(data.getGrhIndex(), 1.5f);
                    ImGui.sameLine();
                    ImGui.beginGroup();
                    ImGui.text("OBJ " + data.getNumber());
                    ImGui.text(data.getName());
                    ImGui.textDisabled("Grh: " + data.getGrhIndex());
                    ImGui.endGroup();
                }
            } else {
                ImGui.textDisabled("Selecciona un objeto para ver su previa");
            }
        }
        ImGui.endChild();
    }

    private void drawObjGrid() {
        ImGui.beginChild("ObjGridChild", 0, 300, true);
        if (objs == null || objs.isEmpty()) {
            ImGui.textDisabled("Objetos no cargados");
            ImGui.endChild();
            return;
        }

        List<Integer> filteredKeys = getFilteredObjs();

        int total = filteredKeys.size();
        int maxPages = Math.max(0, (total - 1) / itemsPerPage);
        if (currentPage > maxPages)
            currentPage = maxPages;

        ImGui.text("Pag: " + (currentPage + 1) + "/" + (maxPages + 1));
        ImGui.sameLine();
        if (ImGui.button("<##prevObjGrid") && currentPage > 0)
            currentPage--;
        ImGui.sameLine();
        if (ImGui.button(">##nextObjGrid") && currentPage < maxPages)
            currentPage++;

        int start = currentPage * itemsPerPage;
        int end = Math.min(start + itemsPerPage, total);

        if (ImGui.beginTable("ObjGridTable", 4, imgui.flag.ImGuiTableFlags.SizingFixedFit)) {
            for (int i = start; i < end; i++) {
                int objNum = filteredKeys.get(i);
                ObjData data = objs.get(objNum);

                ImGui.tableNextColumn();
                ImGui.pushID(objNum);

                float startX = ImGui.getCursorPosX();
                float startY = ImGui.getCursorPosY();

                boolean isSelected = (selectedObjNumber == objNum);
                if (isSelected)
                    ImGui.pushStyleColor(ImGuiCol.Header, 0.4f, 0.4f, 0.1f, 0.6f);

                if (ImGui.selectable("##objSel" + objNum, isSelected, 0, 64, 64)) {
                    selectObj(objNum);
                }

                if (isSelected)
                    ImGui.popStyleColor();

                // Dibujar el objeto centrado y ajustado en la celda
                ImGui.setCursorPos(startX, startY);
                if (data.getGrhIndex() > 0) {
                    org.argentumforge.engine.gui.PreviewUtils.drawGrhFit(data.getGrhIndex(), 64, 64);
                }

                if (ImGui.isItemHovered()) {
                    ImGui.setTooltip("OBJ " + objNum + "\n" + data.getName());
                }

                ImGui.popID();
            }
            ImGui.endTable();
        }
        ImGui.endChild();
    }

    private void drawObjList() {
        ImGui.beginChild("ObjListChild", 0, 200, true);

        if (objs == null || objs.isEmpty()) {
            ImGui.textDisabled("Objetos no cargados");
            ImGui.endChild();
            return;
        }

        List<Integer> filteredKeys = getFilteredObjs();

        for (Integer objNum : filteredKeys) {
            ObjData data = objs.get(objNum);
            String label = "OBJ " + objNum + " - " + data.getName();

            if (ImGui.selectable(label, selectedObjNumber == objNum)) {
                selectObj(objNum);
            }
        }

        ImGui.endChild();
    }

    private List<Integer> getFilteredObjs() {
        List<Integer> filtered = new ArrayList<>();
        String filter = searchFilter.get().toLowerCase();

        List<Integer> keys = new ArrayList<>(objs.keySet());
        keys.sort(Comparator.naturalOrder());

        for (Integer key : keys) {
            ObjData data = objs.get(key);
            if (filter.isEmpty()) {
                filtered.add(key);
                continue;
            }

            if (String.valueOf(key).contains(filter)) {
                filtered.add(key);
                continue;
            }

            if (data.getName().toLowerCase().contains(filter)) {
                filtered.add(key);
            }
        }
        return filtered;
    }

    private void selectObj(int objNum) {
        selectedObjNumber = objNum;
        objEditor.setObjNumber(objNum);

        // Autoseleccionar modo Colocar si no está en Colocar o Quitar o Capturar
        if (objEditor.getMode() < 1 || objEditor.getMode() > 3) {
            objEditor.setMode(1);
            Console.INSTANCE.addMsgToConsole("Modo: Colocar Objeto (" + selectedObjNumber + ") activado.",
                    FontStyle.REGULAR, new RGBColor(1, 1, 1));
        }

        if (objEditor.getMode() == 1) {
            objEditor.setObjNumber(selectedObjNumber);
        }
    }

    private void drawButtons() {
        int currentMode = objEditor.getMode();

        // Botón Quitar (Destructivo)
        if (currentMode == 2) {
            ImGui.pushStyleColor(ImGuiCol.Button, Theme.COLOR_DANGER);
        }
        if (ImGui.button("Quitar", 85, 30)) {
            objEditor.setMode(currentMode == 2 ? 0 : 2);
            if (objEditor.getMode() == 2) {
                Console.INSTANCE.addMsgToConsole("Modo: Quitar Objeto activado.", FontStyle.REGULAR,
                        new RGBColor(1, 1, 1));
            }
        }
        if (currentMode == 2) {
            ImGui.popStyleColor();
        }

        ImGui.sameLine();

        if (UIComponents.toggleButton("Capturar", currentMode == 3, 85, 30)) {
            objEditor.setMode(currentMode == 3 ? 0 : 3);
            if (objEditor.getMode() == 3) {
                Console.INSTANCE.addMsgToConsole("Modo: Capturar Objeto activado.", FontStyle.REGULAR,
                        new RGBColor(1, 1, 1));
            }
        }

        ImGui.sameLine();

        if (UIComponents.toggleButton("Colocar", currentMode == 1 && selectedObjNumber > 0, 85, 30)) {
            objEditor.setMode(currentMode == 1 ? 0 : 1);
            if (objEditor.getMode() == 1) {
                objEditor.setObjNumber(selectedObjNumber);
                Console.INSTANCE.addMsgToConsole("Modo: Colocar Objeto (" + selectedObjNumber + ") activado.",
                        FontStyle.REGULAR, new RGBColor(1, 1, 1));
            }
        }

        if (selectedObjNumber > 0) {
            ObjData selected = objs.get(selectedObjNumber);
            if (selected != null) {
                ImGui.separator();
                ImGui.textDisabled("Seleccionado: OBJ " + selected.getNumber());
                ImGui.textDisabled("Nombre: " + selected.getName());
            }
        }
    }
}
