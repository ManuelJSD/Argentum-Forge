package org.argentumforge.engine.gui.forms;

import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import org.argentumforge.engine.utils.editor.Npc;
import org.argentumforge.engine.utils.inits.NpcData;
import imgui.type.ImString;
import org.argentumforge.engine.game.console.Console;
import org.argentumforge.engine.game.console.FontStyle;
import org.argentumforge.engine.renderer.RGBColor;
import org.argentumforge.engine.gui.Theme;
import org.argentumforge.engine.gui.widgets.UIComponents;
import org.argentumforge.engine.i18n.I18n;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.argentumforge.engine.utils.AssetRegistry.npcs;

/**
 * Formulario de edición y colocación de NPCs en el mapa.
 * 
 * Permite buscar NPCs por nombre o ID, previsualizar su información
 * y alternar entre los modos de colocar o quitar NPCs en la rejilla del mapa.
 */
public final class FNpcEditor extends Form {

    private int selectedNpcNumber = -1;
    private final Npc npcEditor;
    private final ImString searchFilter = new ImString(100);
    private boolean isGridView = false; // Toggle para vista dual
    private int itemsPerPage = 50;
    private int currentPage = 0;

    public FNpcEditor() {
        npcEditor = Npc.getInstance();

        if (npcs != null && !npcs.isEmpty()) {
            selectedNpcNumber = npcs.keySet().stream().min(Integer::compareTo).orElse(-1);
            if (selectedNpcNumber > 0)
                npcEditor.setNpcNumber(selectedNpcNumber);
        }
    }

    @Override
    public void render() {
        ImGui.setNextWindowSize(350, 550, ImGuiCond.FirstUseEver);
        if (ImGui.begin(I18n.INSTANCE.get("editor.npc"), ImGuiWindowFlags.None)) {

            // Sincronizar selección si el editor lógico cambió (por cuenta gotas)
            if (npcEditor.getNpcNumber() != selectedNpcNumber) {
                selectedNpcNumber = npcEditor.getNpcNumber();
            }

            ImGui.text(I18n.INSTANCE.get("editor.npc.npcs"));
            ImGui.sameLine(ImGui.getWindowWidth() - 110);
            if (ImGui.button(
                    isGridView ? I18n.INSTANCE.get("editor.npc.viewList") : I18n.INSTANCE.get("editor.npc.viewGrid"))) {
                isGridView = !isGridView;
                currentPage = 0;
            }
            ImGui.separator();

            ImGui.pushItemWidth(-1);
            if (ImGui.inputTextWithHint("##BuscarNPC", I18n.INSTANCE.get("editor.npc.searchHint"), searchFilter)) {
                currentPage = 0;
            }
            ImGui.popItemWidth();
            ImGui.spacing();

            if (isGridView) {
                drawNpcGrid();
            } else {
                drawNpcList();
                ImGui.separator();
                drawPreview();
            }
            ImGui.separator();

            drawButtons();
        }
        ImGui.end();
    }

    private void drawPreview() {
        ImGui.text(I18n.INSTANCE.get("editor.npc.preview"));
        if (ImGui.beginChild("NpcPreview", 0, 100, true)) {
            if (selectedNpcNumber > 0 && npcs != null) {
                org.argentumforge.engine.utils.inits.NpcData data = npcs.get(selectedNpcNumber);
                if (data != null) {
                    org.argentumforge.engine.gui.PreviewUtils.drawNpc(data.getBody(), data.getHead(), 1.2f);
                    ImGui.sameLine();
                    ImGui.beginGroup();
                    ImGui.text("NPC " + data.getNumber());
                    ImGui.text(data.getName());
                    ImGui.textDisabled(I18n.INSTANCE.get("editor.npc.body") + " " + data.getBody() + " "
                            + I18n.INSTANCE.get("editor.npc.head") + " " + data.getHead());
                    ImGui.endGroup();
                }
            } else {
                ImGui.textDisabled(I18n.INSTANCE.get("editor.npc.previewNone"));
            }
        }
        ImGui.endChild();
    }

    private void drawNpcGrid() {
        ImGui.beginChild("NpcGridChild", 0, 300, true);
        if (npcs == null || npcs.isEmpty()) {
            ImGui.textDisabled(I18n.INSTANCE.get("editor.npc.notLoaded"));
            ImGui.endChild();
            return;
        }

        List<Integer> filteredKeys = getFilteredNpcs();

        // Paginación simple en grid
        int total = filteredKeys.size();
        int maxPages = Math.max(0, (total - 1) / itemsPerPage);
        if (currentPage > maxPages)
            currentPage = maxPages;

        ImGui.text(I18n.INSTANCE.get("editor.npc.page") + " " + (currentPage + 1) + "/" + (maxPages + 1));
        ImGui.sameLine();
        if (ImGui.button("<##prevGrid") && currentPage > 0)
            currentPage--;
        ImGui.sameLine();
        if (ImGui.button(">##nextGrid") && currentPage < maxPages)
            currentPage++;

        int start = currentPage * itemsPerPage;
        int end = Math.min(start + itemsPerPage, total);

        if (ImGui.beginTable("NpcGridTable", 4, imgui.flag.ImGuiTableFlags.SizingFixedFit)) {
            for (int i = start; i < end; i++) {
                int npcNum = filteredKeys.get(i);
                org.argentumforge.engine.utils.inits.NpcData data = npcs.get(npcNum);

                ImGui.tableNextColumn();
                ImGui.pushID(npcNum);

                float startX = ImGui.getCursorPosX();
                float startY = ImGui.getCursorPosY();

                boolean isSelected = (selectedNpcNumber == npcNum);
                if (isSelected)
                    ImGui.pushStyleColor(ImGuiCol.Header, 0.4f, 0.4f, 0.1f, 0.6f);

                // Celda clicable (64x64)
                if (ImGui.selectable("##npcSel" + npcNum, isSelected, 0, 64, 64)) {
                    selectNpc(npcNum);
                }

                if (isSelected)
                    ImGui.popStyleColor();

                // Dibujar el NPC encima de la celda
                ImGui.setCursorPos(startX, startY);
                org.argentumforge.engine.gui.PreviewUtils.drawNpc(data.getBody(), data.getHead(), 0.8f);

                if (ImGui.isItemHovered()) {
                    ImGui.setTooltip("NPC " + npcNum + "\n" + data.getName());
                }

                ImGui.popID();
            }
            ImGui.endTable();
        }
        ImGui.endChild();
    }

    private void drawNpcList() {
        ImGui.beginChild("NpcListChild", 0, 200, true);

        if (npcs == null || npcs.isEmpty()) {
            ImGui.textDisabled(I18n.INSTANCE.get("editor.npc.notLoaded"));
            ImGui.endChild();
            return;
        }

        List<Integer> filteredKeys = getFilteredNpcs();

        for (Integer npcNumber : filteredKeys) {
            NpcData data = npcs.get(npcNumber);
            String label = "NPC " + npcNumber + " - " + data.getName();

            if (ImGui.selectable(label, selectedNpcNumber == npcNumber)) {
                selectNpc(npcNumber);
            }
        }

        ImGui.endChild();
    }

    private List<Integer> getFilteredNpcs() {
        List<Integer> filtered = new ArrayList<>();
        String filter = searchFilter.get().toLowerCase();

        List<Integer> keys = new ArrayList<>(npcs.keySet());
        keys.sort(Comparator.naturalOrder());

        for (Integer key : keys) {
            NpcData data = npcs.get(key);
            if (filter.isEmpty()) {
                filtered.add(key);
                continue;
            }

            // Búsqueda por ID
            if (String.valueOf(key).contains(filter)) {
                filtered.add(key);
                continue;
            }

            // Búsqueda por nombre
            if (data.getName().toLowerCase().contains(filter)) {
                filtered.add(key);
            }
        }
        return filtered;
    }

    private void selectNpc(int npcNum) {
        selectedNpcNumber = npcNum;
        npcEditor.setNpcNumber(npcNum);

        // Autoseleccionar modo Colocar si no está ya en Colocar o Quitar
        if (npcEditor.getMode() != 1 && npcEditor.getMode() != 2) {
            npcEditor.setMode(1);
            Console.INSTANCE.addMsgToConsole(I18n.INSTANCE.get("msg.npc.placeMode", selectedNpcNumber),
                    FontStyle.REGULAR, new RGBColor(1, 1, 1));
        }

        // Si ya estamos en Colocar, actualizamos el número por si acaso
        if (npcEditor.getMode() == 1) {
            npcEditor.setNpcNumber(selectedNpcNumber);
        }
    }

    private void drawButtons() {
        int currentMode = npcEditor.getMode();

        // Botón Quitar (Destructivo)
        if (currentMode == 2) {
            ImGui.pushStyleColor(ImGuiCol.Button, Theme.COLOR_DANGER);
        }
        if (ImGui.button(I18n.INSTANCE.get("editor.npc.remove"), 110, 30)) {
            npcEditor.setMode(currentMode == 2 ? 0 : 2);
            if (npcEditor.getMode() == 2) {
                Console.INSTANCE.addMsgToConsole(I18n.INSTANCE.get("msg.npc.removeMode"),
                        FontStyle.REGULAR, new RGBColor(1, 1, 1));
            }
        }
        if (currentMode == 2) {
            ImGui.popStyleColor();
        }

        ImGui.sameLine();

        if (UIComponents.toggleButton(I18n.INSTANCE.get("editor.npc.capture"), currentMode == 3, 60, 30)) {
            npcEditor.setMode(currentMode == 3 ? 0 : 3);
            if (npcEditor.getMode() == 3) {
                Console.INSTANCE.addMsgToConsole(
                        I18n.INSTANCE.get("msg.npc.captureMode"),
                        FontStyle.REGULAR, new RGBColor(1, 1, 1));
            }
        }

        ImGui.sameLine();

        if (UIComponents.toggleButton(I18n.INSTANCE.get("editor.npc.place"), currentMode == 1 && selectedNpcNumber > 0,
                110, 30)) {
            npcEditor.setMode(currentMode == 1 ? 0 : 1);
            if (npcEditor.getMode() == 1) {
                npcEditor.setNpcNumber(selectedNpcNumber);
                Console.INSTANCE.addMsgToConsole(
                        I18n.INSTANCE.get("msg.npc.placeMode", selectedNpcNumber),
                        FontStyle.REGULAR, new RGBColor(1, 1, 1));
            }
        }

        if (selectedNpcNumber > 0 && npcs != null) {
            NpcData selected = npcs.get(selectedNpcNumber);
            if (selected != null) {
                ImGui.separator();
                ImGui.textDisabled(I18n.INSTANCE.get("editor.npc.selected"));
                ImGui.textDisabled(I18n.INSTANCE.get("editor.npc.number") + " " + selected.getNumber());
                ImGui.textDisabled(I18n.INSTANCE.get("editor.npc.name") + " " + selected.getName());
                ImGui.textDisabled(I18n.INSTANCE.get("editor.npc.head") + " " + selected.getHead() + " "
                        + I18n.INSTANCE.get("editor.npc.body") + " " + selected.getBody());
            }
        }
    }
}
