package org.argentumforge.engine.gui.forms;

import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.flag.ImGuiCol;
import imgui.type.ImInt;
import imgui.type.ImString;
import org.argentumforge.engine.gui.PreviewUtils;
import org.argentumforge.engine.i18n.I18n;
import org.argentumforge.engine.utils.GameData;
import org.argentumforge.engine.utils.editor.Selection;
import org.argentumforge.engine.utils.inits.MapData;

/**
 * Inspector de Tiles mejorado y localizado.
 * Enfocado estrictamente en las propiedades de Argentum Forge.
 */
public final class FTileInspector extends Form {

    private final Selection selection = Selection.getInstance();

    @Override
    public void render() {
        if (!selection.isActive() || selection.getInspectedTileX() == -1) {
            return;
        }

        int x = selection.getInspectedTileX();
        int y = selection.getInspectedTileY();

        var context = GameData.getActiveContext();
        if (context == null || context.getMapData() == null)
            return;

        MapData tile = context.getMapData()[x][y];

        ImGui.setNextWindowSize(420, 480, ImGuiCond.FirstUseEver);
        String title = I18n.INSTANCE.get("tile.inspector.title") + " (" + x + "," + y + ")###FTileInspector";

        if (ImGui.begin(title, ImGuiWindowFlags.NoDocking | ImGuiWindowFlags.AlwaysAutoResize)) {

            // --- SECCIÓN CAPAS (LAYERS) ---
            ImGui.textColored(1.0f, 0.8f, 0.0f, 1.0f, I18n.INSTANCE.get("tile.inspector.layers"));
            ImGui.spacing();

            float boxSize = 85.0f;
            float spacing = 10.0f;

            ImGui.pushStyleVar(imgui.flag.ImGuiStyleVar.ChildRounding, 4.0f);

            for (int i = 1; i <= 4; i++) {
                ImGui.beginGroup();

                // Etiqueta de la capa
                String label = I18n.INSTANCE.get("tile.inspector.layer" + i);
                float textWidth = ImGui.calcTextSize(label).x;
                ImGui.setCursorPosX(ImGui.getCursorPosX() + (boxSize - textWidth) / 2);
                ImGui.textDisabled(label);

                int grh = tile.getLayer(i).getGrhIndex();

                ImGui.pushStyleColor(ImGuiCol.ChildBg, 0.12f, 0.12f, 0.12f, 1.0f);
                ImGui.pushStyleColor(ImGuiCol.Border, 0.25f, 0.25f, 0.25f, 1.0f);

                if (ImGui.beginChild("LayerBox" + i, boxSize, boxSize, true, ImGuiWindowFlags.NoScrollbar)) {
                    if (grh > 0) {
                        PreviewUtils.drawGrhFit(grh, boxSize - 12, boxSize - 12);
                        if (ImGui.isItemHovered()) {
                            ImGui.setTooltip("Grh: " + grh);
                        }
                    } else {
                        float emptyW = ImGui.calcTextSize(I18n.INSTANCE.get("tile.inspector.empty")).x;
                        float emptyH = ImGui.calcTextSize(I18n.INSTANCE.get("tile.inspector.empty")).y;
                        ImGui.setCursorPos((boxSize - emptyW) / 2, (boxSize - emptyH) / 2);
                        ImGui.textDisabled(I18n.INSTANCE.get("tile.inspector.empty"));
                    }
                }
                ImGui.endChild();
                ImGui.popStyleColor(2);

                ImGui.endGroup();
                if (i < 4)
                    ImGui.sameLine(0, spacing);
            }
            ImGui.popStyleVar();

            ImGui.dummy(0, 10);

            // --- SECCIÓN PROPIEDADES ---
            ImGui.textColored(1.0f, 0.8f, 0.0f, 1.0f, I18n.INSTANCE.get("tile.inspector.properties"));
            ImGui.separator();
            ImGui.spacing();

            // Bloqueo
            ImGui.text(I18n.INSTANCE.get("tile.inspector.blockStatus"));
            ImGui.sameLine();
            boolean blocked = tile.getBlocked();
            if (blocked) {
                ImGui.pushStyleColor(ImGuiCol.Button, 0.7f, 0.1f, 0.1f, 1.0f);
                ImGui.button(I18n.INSTANCE.get("tile.inspector.blocked"), 120, 25);
                ImGui.popStyleColor();
            } else {
                ImGui.pushStyleColor(ImGuiCol.Button, 0.1f, 0.5f, 0.1f, 1.0f);
                ImGui.button(I18n.INSTANCE.get("tile.inspector.passable"), 120, 25);
                ImGui.popStyleColor();
            }

            ImGui.spacing();

            // Grilla de Atributos
            ImGui.columns(2, "AttribGrid", false);

            // Trigger
            ImGui.text(I18n.INSTANCE.get("tile.inspector.trigger"));
            ImGui.sameLine();
            ImGui.setNextItemWidth(80);
            ImGui.inputInt("##TrigID", new ImInt(tile.getTrigger()), 0, 0, imgui.flag.ImGuiInputTextFlags.ReadOnly);

            ImGui.nextColumn();

            // Partícula
            ImGui.text(I18n.INSTANCE.get("tile.inspector.particle"));
            ImGui.sameLine();
            ImGui.setNextItemWidth(80);
            ImGui.inputInt("##PartID", new ImInt(tile.getParticleIndex()), 0, 0,
                    imgui.flag.ImGuiInputTextFlags.ReadOnly);

            ImGui.nextColumn();

            // NPC y Char
            ImGui.text(I18n.INSTANCE.get("tile.inspector.npc"));
            ImGui.sameLine();
            ImGui.setNextItemWidth(80);
            String npcTxt = tile.getNpcIndex() > 0 ? String.valueOf(tile.getNpcIndex()) : "-";
            ImGui.inputText("##NPCID", new ImString(npcTxt), imgui.flag.ImGuiInputTextFlags.ReadOnly);

            ImGui.nextColumn();

            ImGui.text(I18n.INSTANCE.get("tile.inspector.char"));
            ImGui.sameLine();
            ImGui.setNextItemWidth(80);
            ImGui.inputInt("##CharID", new ImInt(tile.getCharIndex()), 0, 0, imgui.flag.ImGuiInputTextFlags.ReadOnly);

            ImGui.nextColumn();

            // Objeto y Cantidad
            ImGui.text(I18n.INSTANCE.get("tile.inspector.obj"));
            ImGui.sameLine();
            ImGui.setNextItemWidth(80);
            int objGrh = tile.getObjGrh().getGrhIndex();
            String objTxt = objGrh > 0 ? String.valueOf(objGrh) : "-";
            ImGui.inputText("##ObjID", new ImString(objTxt), imgui.flag.ImGuiInputTextFlags.ReadOnly);

            ImGui.nextColumn();

            ImGui.text(I18n.INSTANCE.get("tile.inspector.amount"));
            ImGui.sameLine();
            ImGui.setNextItemWidth(80);
            ImGui.inputInt("##ObjAmount", new ImInt(tile.getObjAmount()), 0, 0,
                    imgui.flag.ImGuiInputTextFlags.ReadOnly);

            ImGui.columns(1);

            // --- SECCIÓN TRASLADO (TRANSFER) ---
            if (tile.getExitMap() > 0) {
                ImGui.spacing();
                ImGui.pushStyleColor(ImGuiCol.Header, 0.1f, 0.3f, 0.5f, 1.0f);
                if (ImGui.collapsingHeader(I18n.INSTANCE.get("tile.inspector.transfer"), ImGuiWindowFlags.NoCollapse)) {
                    ImGui.indent();
                    ImGui.textColored(0.4f, 0.8f, 1.0f, 1.0f,
                            String.format(I18n.INSTANCE.get("tile.inspector.target"),
                                    tile.getExitMap(), tile.getExitX(), tile.getExitY()));
                    ImGui.unindent();
                }
                ImGui.popStyleColor();
            }

            ImGui.dummy(0, 10);
            ImGui.separator();
            if (ImGui.button(I18n.INSTANCE.get("common.close"), -1, 30)) {
                this.close();
            }

            ImGui.end();
        }
    }
}
