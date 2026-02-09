package org.argentumforge.engine.gui.forms;

import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiTreeNodeFlags;
import imgui.type.ImInt;
import org.argentumforge.engine.gui.PreviewUtils;
import org.argentumforge.engine.i18n.I18n;
import org.argentumforge.engine.utils.GameData;
import org.argentumforge.engine.utils.MapContext;
import org.argentumforge.engine.utils.editor.Selection;
import org.argentumforge.engine.utils.inits.MapData;
import org.argentumforge.engine.utils.AssetRegistry;
import org.argentumforge.engine.utils.editor.commands.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Inspector de Tiles mejorado y localizado con capacidad de EDICIÓN.
 * Permite modificar propiedades del mapa directamente con soporte de
 * Deshacer/Rehacer.
 */
public final class FTileInspector extends Form {

    private final Selection selection = Selection.getInstance();
    private final CommandManager commandManager = CommandManager.getInstance();

    // Estados temporales para edición (para evitar disparar comandos en cada
    // pulsación intermedia si fuera necesario,
    // aunque en ImGui solemos disparar al confirmar o al detectar cambio final).
    private final ImInt tempPart = new ImInt();
    private final ImInt tempTrig = new ImInt();
    private final ImInt tempNpc = new ImInt();
    private final ImInt tempObj = new ImInt();
    private final ImInt tempAmt = new ImInt();
    private final ImInt tempExtMap = new ImInt();
    private final ImInt tempExtX = new ImInt();
    private final ImInt tempExtY = new ImInt();

    @Override
    public void render() {
        if (!selection.isActive() || selection.getInspectedTileX() == -1) {
            return;
        }

        int x = selection.getInspectedTileX();
        int y = selection.getInspectedTileY();

        MapContext context = GameData.getActiveContext();
        if (context == null || context.getMapData() == null)
            return;

        MapData tile = context.getMapData()[x][y];

        ImGui.setNextWindowSize(420, 550, ImGuiCond.FirstUseEver);
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
                            ImGui.setTooltip(String.format(I18n.INSTANCE.get("tile.inspector.tooltip.grh"), grh));
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

                // Editor de ID de Capa
                ImGui.pushItemWidth(boxSize);
                ImInt lGrh = new ImInt(grh);
                if (!ImGui.isItemActive()) {
                    lGrh.set(grh);
                }
                if (ImGui.inputInt("##LGrh" + i, lGrh, 0, 0)) {
                    if (lGrh.get() != grh) {
                        int maxGrh = (AssetRegistry.grhData != null) ? AssetRegistry.grhData.length - 1 : 32000;
                        short val = (short) Math.max(0, Math.min(maxGrh, lGrh.get()));
                        commandManager.executeCommand(
                                new TileChangeCommand(context, x, y, i, (short) grh, val));
                    }
                }
                ImGui.popItemWidth();

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
                if (ImGui.button(I18n.INSTANCE.get("tile.inspector.blocked") + "##Toggle", 120, 25)) {
                    toggleBlock(context, x, y, blocked);
                }
                ImGui.popStyleColor();
            } else {
                ImGui.pushStyleColor(ImGuiCol.Button, 0.1f, 0.5f, 0.1f, 1.0f);
                if (ImGui.button(I18n.INSTANCE.get("tile.inspector.passable") + "##Toggle", 120, 25)) {
                    toggleBlock(context, x, y, blocked);
                }
                ImGui.popStyleColor();
            }

            ImGui.spacing();

            // Grilla de Atributos
            ImGui.columns(2, "AttribGrid", false);

            // Trigger
            ImGui.text(I18n.INSTANCE.get("tile.inspector.trigger"));
            ImGui.sameLine();
            ImGui.setNextItemWidth(80);
            if (!ImGui.isItemActive()) {
                tempTrig.set(tile.getTrigger());
            }
            if (ImGui.inputInt("##TrigID", tempTrig, 1, 5)) {
                if (tempTrig.get() != tile.getTrigger()) {
                    // Triggers typical range 0-6
                    short val = (short) Math.max(0, Math.min(6, tempTrig.get()));
                    Map<TriggerChangeCommand.TilePos, Short> oldS = new HashMap<>();
                    Map<TriggerChangeCommand.TilePos, Short> newS = new HashMap<>();
                    oldS.put(new TriggerChangeCommand.TilePos(x, y), (short) tile.getTrigger());
                    newS.put(new TriggerChangeCommand.TilePos(x, y), val);
                    commandManager.executeCommand(new TriggerChangeCommand(context, oldS, newS));
                }
            }

            ImGui.nextColumn();

            // Partícula
            ImGui.text(I18n.INSTANCE.get("tile.inspector.particle"));
            ImGui.sameLine();
            ImGui.setNextItemWidth(80);
            if (!ImGui.isItemActive()) {
                tempPart.set(tile.getParticleIndex());
            }
            if (ImGui.inputInt("##PartID", tempPart, 1, 5)) {
                if (tempPart.get() != tile.getParticleIndex()) {
                    int maxFx = (AssetRegistry.fxData != null) ? AssetRegistry.fxData.length - 1 : 1000;
                    int val = Math.max(0, Math.min(maxFx, tempPart.get()));
                    Map<ParticleChangeCommand.TilePos, Integer> oldS = new HashMap<>();
                    Map<ParticleChangeCommand.TilePos, Integer> newS = new HashMap<>();
                    oldS.put(new ParticleChangeCommand.TilePos(x, y), tile.getParticleIndex());
                    newS.put(new ParticleChangeCommand.TilePos(x, y), val);
                    commandManager.executeCommand(new ParticleChangeCommand(context, oldS, newS));
                }
            }

            ImGui.nextColumn();

            // NPC
            ImGui.text(I18n.INSTANCE.get("tile.inspector.npc"));
            ImGui.sameLine();
            ImGui.setNextItemWidth(80);
            if (!ImGui.isItemActive()) {
                tempNpc.set(tile.getNpcIndex());
            }
            if (ImGui.inputInt("##NPCID", tempNpc, 1, 10)) {
                if (tempNpc.get() != tile.getNpcIndex()) {
                    int val = Math.max(0, tempNpc.get());
                    commandManager
                            .executeCommand(new NpcChangeCommand(context, x, y, tile.getNpcIndex(), val));
                }
            }

            ImGui.nextColumn();

            // Char (Solo lectura, se deriva del NPC o usuario)
            ImGui.text(I18n.INSTANCE.get("tile.inspector.char"));
            ImGui.sameLine();
            ImGui.setNextItemWidth(80);
            ImGui.textDisabled(String.valueOf(tile.getCharIndex()));

            ImGui.nextColumn();

            // Objeto
            ImGui.text(I18n.INSTANCE.get("tile.inspector.obj"));
            ImGui.sameLine();
            ImGui.setNextItemWidth(80);
            int currentObjGrh = tile.getObjGrh().getGrhIndex();
            tempObj.set(currentObjGrh);
            if (ImGui.inputInt("##ObjID", tempObj, 1, 10)) {
                if (tempObj.get() != currentObjGrh) {
                    int val = Math.max(0, tempObj.get());
                    commandManager.executeCommand(new ObjChangeCommand(context, x, y, currentObjGrh, val));
                }
            }

            ImGui.nextColumn();

            // Cantidad Objeto
            ImGui.text(I18n.INSTANCE.get("tile.inspector.amount"));
            ImGui.sameLine();
            ImGui.setNextItemWidth(80);
            if (!ImGui.isItemActive()) {
                tempAmt.set(tile.getObjAmount());
            }
            if (ImGui.inputInt("##ObjAmount", tempAmt, 1, 10)) {
                if (tempAmt.get() != tile.getObjAmount()) {
                    int val = Math.max(1, tempAmt.get());
                    commandManager.executeCommand(
                            new ObjAmountChangeCommand(context, x, y, tile.getObjAmount(), val));
                }
            }

            ImGui.columns(1);

            // --- SECCIÓN TRASLADO (TRANSFER) ---
            ImGui.spacing();
            ImGui.pushStyleColor(ImGuiCol.Header, 0.1f, 0.3f, 0.5f, 1.0f);
            if (ImGui.collapsingHeader(I18n.INSTANCE.get("tile.inspector.transfer"), ImGuiTreeNodeFlags.DefaultOpen)) {
                ImGui.indent();

                ImGui.text(I18n.INSTANCE.get("tile.inspector.map"));
                ImGui.sameLine();
                ImGui.setNextItemWidth(60);
                if (!ImGui.isItemActive()) {
                    tempExtMap.set(tile.getExitMap());
                }
                boolean mChanged = ImGui.inputInt("##ExitMap", tempExtMap, 0, 0);

                ImGui.sameLine();
                ImGui.text(I18n.INSTANCE.get("tile.inspector.x"));
                ImGui.sameLine();
                ImGui.setNextItemWidth(50);
                if (!ImGui.isItemActive()) { // Added safety check
                    tempExtX.set(tile.getExitX());
                }
                boolean xChanged = ImGui.inputInt("##ExitX", tempExtX, 0, 0);

                ImGui.sameLine();
                ImGui.text(I18n.INSTANCE.get("tile.inspector.y"));
                ImGui.sameLine();
                ImGui.setNextItemWidth(50);
                if (!ImGui.isItemActive()) {
                    tempExtY.set(tile.getExitY());
                }
                boolean yChanged = ImGui.inputInt("##ExitY", tempExtY, 0, 0);

                if (mChanged || xChanged || yChanged) {
                    short mMap = (short) Math.max(0, tempExtMap.get());
                    short mX = (short) Math.max(0, Math.min(100, tempExtX.get()));
                    short mY = (short) Math.max(0, Math.min(100, tempExtY.get()));
                    commandManager.executeCommand(new TransferChangeCommand(context, x, y,
                            tile.getExitMap(), tile.getExitX(), tile.getExitY(),
                            mMap, mX, mY));
                }

                ImGui.unindent();
            }
            ImGui.popStyleColor();

            ImGui.dummy(0, 10);
            ImGui.separator();
            if (ImGui.button(I18n.INSTANCE.get("common.close"), -1, 30)) {
                this.close();
            }

            ImGui.end();
        }
    }

    private void toggleBlock(MapContext context, int x, int y, boolean current) {
        Map<BlockChangeCommand.TilePos, Boolean> oldS = new HashMap<>();
        Map<BlockChangeCommand.TilePos, Boolean> newS = new HashMap<>();
        oldS.put(new BlockChangeCommand.TilePos(x, y), current);
        newS.put(new BlockChangeCommand.TilePos(x, y), !current);
        commandManager.executeCommand(new BlockChangeCommand(context, oldS, newS));
    }
}
