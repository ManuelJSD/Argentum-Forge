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
        if (ImGui.begin("Editor de NPCs", ImGuiWindowFlags.None)) {

            // Sincronizar selección si el editor lógico cambió (por cuenta gotas)
            if (npcEditor.getNpcNumber() != selectedNpcNumber) {
                selectedNpcNumber = npcEditor.getNpcNumber();
            }

            ImGui.text("NPCs:");
            ImGui.sameLine(ImGui.getWindowWidth() - 100);
            if (ImGui.button(isGridView ? "Ver Lista" : "Ver Rejilla")) {
                isGridView = !isGridView;
                currentPage = 0;
            }
            ImGui.separator();

            ImGui.pushItemWidth(-1);
            if (ImGui.inputTextWithHint("##BuscarNPC", "ID o Nombre...", searchFilter)) {
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
        ImGui.text("Previsualizacion:");
        if (ImGui.beginChild("NpcPreview", 0, 100, true)) {
            if (selectedNpcNumber > 0 && npcs != null) {
                org.argentumforge.engine.utils.inits.NpcData data = npcs.get(selectedNpcNumber);
                if (data != null) {
                    org.argentumforge.engine.gui.PreviewUtils.drawNpc(data.getBody(), data.getHead(), 1.2f);
                    ImGui.sameLine();
                    ImGui.beginGroup();
                    ImGui.text("NPC " + data.getNumber());
                    ImGui.text(data.getName());
                    ImGui.textDisabled("Body: " + data.getBody() + " Head: " + data.getHead());
                    ImGui.endGroup();
                }
            } else {
                ImGui.textDisabled("Selecciona un NPC para ver su previa");
            }
        }
        ImGui.endChild();
    }

    private void drawNpcGrid() {
        ImGui.beginChild("NpcGridChild", 0, 300, true);
        if (npcs == null || npcs.isEmpty()) {
            ImGui.textDisabled("NPCs no cargados");
            ImGui.endChild();
            return;
        }

        List<Integer> filteredKeys = getFilteredNpcs();

        // Paginación simple en grid
        int total = filteredKeys.size();
        int maxPages = Math.max(0, (total - 1) / itemsPerPage);
        if (currentPage > maxPages)
            currentPage = maxPages;

        ImGui.text("Pag: " + (currentPage + 1) + "/" + (maxPages + 1));
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
            ImGui.textDisabled("NPCs no cargados");
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
            Console.INSTANCE.addMsgToConsole("Modo: Colocar NPC (" + selectedNpcNumber + ") activado.",
                    FontStyle.REGULAR, new RGBColor(1, 1, 1));
        }

        // Si ya estamos en Colocar, actualizamos el número por si acaso
        if (npcEditor.getMode() == 1) {
            npcEditor.setNpcNumber(selectedNpcNumber);
        }
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
                Console.INSTANCE.addMsgToConsole("Modo: Quitar NPC activado. Clic en el mapa para eliminar.",
                        FontStyle.REGULAR, new RGBColor(1, 1, 1));
            }
        }
        if (pushQuitar)
            ImGui.popStyleColor();

        ImGui.sameLine();

        boolean pushCapturar = false;
        if (currentMode == 3) {
            ImGui.pushStyleColor(ImGuiCol.Button, activeColor);
            pushCapturar = true;
        }
        if (ImGui.button("Capturar", 60, 30)) {
            if (currentMode == 3) {
                npcEditor.setMode(0);
            } else {
                npcEditor.setMode(3);
                Console.INSTANCE.addMsgToConsole(
                        "Modo: Capturar NPC activado. Clic en un NPC del mapa para seleccionarlo.",
                        FontStyle.REGULAR, new RGBColor(1, 1, 1));
            }
        }
        if (pushCapturar)
            ImGui.popStyleColor();

        ImGui.sameLine();

        boolean pushColocar = false;
        boolean placeEnabled = selectedNpcNumber > 0;
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
                npcEditor.setMode(0);
            } else {
                npcEditor.setMode(1);
                npcEditor.setNpcNumber(selectedNpcNumber);
                Console.INSTANCE.addMsgToConsole(
                        "Modo: Colocar NPC (" + selectedNpcNumber + ") activado. Clic en el mapa para colocar.",
                        FontStyle.REGULAR, new RGBColor(1, 1, 1));
            }
        }
        if (!placeEnabled)
            ImGui.popStyleColor();
        if (pushColocar)
            ImGui.popStyleColor();

        if (selectedNpcNumber > 0 && npcs != null) {
            NpcData selected = npcs.get(selectedNpcNumber);
            if (selected != null) {
                ImGui.separator();
                ImGui.textDisabled("Seleccionado:");
                ImGui.textDisabled("Nro: " + selected.getNumber());
                ImGui.textDisabled("Nombre: " + selected.getName());
                ImGui.textDisabled("Cabeza: " + selected.getHead() + "  Cuerpo: " + selected.getBody());
            }
        }
    }
}
