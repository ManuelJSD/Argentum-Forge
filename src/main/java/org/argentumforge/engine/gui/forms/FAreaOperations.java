package org.argentumforge.engine.gui.forms;

import java.util.List;
import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImInt;
import org.argentumforge.engine.gui.DialogManager;
import org.argentumforge.engine.i18n.I18n;
import org.argentumforge.engine.utils.GameData;
import org.argentumforge.engine.utils.MapContext;
import org.argentumforge.engine.utils.editor.*;

/**
 * Formulario para ejecutar operaciones masivas (rellenar/eliminar)
 * sobre un área seleccionada del mapa.
 * <p>
 * Permite operar sobre superficies, bloqueos, triggers, objetos,
 * partículas, traslados y NPCs, con soporte para mosaico y modo aleatorio.
 */
public class FAreaOperations extends Form {

    /** Fuente del área: 0 = selección actual, 1 = manual, 2 = todo el mapa */
    private int areaSource = 0;

    /** Coordenadas manuales */
    private final ImInt manualX1 = new ImInt(0);
    private final ImInt manualY1 = new ImInt(0);
    private final ImInt manualX2 = new ImInt(99);
    private final ImInt manualY2 = new ImInt(99);

    /** Capas a afectar (para superficies) */
    private boolean layer1 = true, layer2 = false, layer3 = false, layer4 = false;

    /** Modo aleatorio */
    private boolean randomMode = false;
    private final float[] density = { 0.25f };

    @Override
    public void render() {
        ImGui.setNextWindowSize(400, 580, imgui.flag.ImGuiCond.FirstUseEver);

        int flags = ImGuiWindowFlags.NoCollapse;
        if (ImGui.begin(I18n.INSTANCE.get("editor.area.title") + "###FAreaOperations", flags)) {

            MapContext context = GameData.getActiveContext();
            boolean hasMap = context != null && context.getMapData() != null;

            if (!hasMap) {
                ImGui.textDisabled(I18n.INSTANCE.get("msg.noActiveMap"));
                ImGui.end();
                return;
            }

            // ── Fuente del área ──
            drawAreaSource();
            ImGui.separator();

            // ── Modo aleatorio ──
            drawRandomModeToggle();
            ImGui.separator();

            // Resolver bounds según fuente
            int[] bounds = resolveBounds(context);

            if (bounds == null && areaSource == 0) {
                ImGui.textColored(1.0f, 0.8f, 0.2f, 1.0f,
                        I18n.INSTANCE.get("editor.area.noSelection"));
                ImGui.separator();
            }

            boolean enabled = bounds != null;

            // ── Superficies ──
            drawSurfaceSection(context, bounds, enabled);
            ImGui.separator();

            // ── Bloqueos ──
            drawBlockSection(context, bounds, enabled);
            ImGui.separator();

            // ── Triggers ──
            drawTriggerSection(context, bounds, enabled);
            ImGui.separator();

            // ── Objetos ──
            drawObjectSection(context, bounds, enabled);
            ImGui.separator();

            // ── Partículas ──
            drawParticleSection(context, bounds, enabled);
            ImGui.separator();

            // ── Traslados ──
            drawTransferSection(context, bounds, enabled);
            ImGui.separator();

            // ── NPCs ──
            drawNpcSection(context, bounds, enabled);
        }
        ImGui.end();
    }

    // ═════════════════════════════════════════════════
    // FUENTE DEL ÁREA
    // ═════════════════════════════════════════════════

    private void drawAreaSource() {
        ImGui.text(I18n.INSTANCE.get("editor.area.source"));

        if (ImGui.radioButton(I18n.INSTANCE.get("editor.area.source.selection"), areaSource == 0)) {
            areaSource = 0;
        }
        ImGui.sameLine();
        if (ImGui.radioButton(I18n.INSTANCE.get("editor.area.source.manual"), areaSource == 1)) {
            areaSource = 1;
        }
        ImGui.sameLine();
        if (ImGui.radioButton(I18n.INSTANCE.get("editor.area.source.allMap"), areaSource == 2)) {
            areaSource = 2;
        }

        if (areaSource == 1) {
            ImGui.indent();

            // Fila 1: X1 / Y1
            ImGui.text("X1:");
            ImGui.sameLine();
            ImGui.setNextItemWidth(80);
            ImGui.inputInt("##manualX1", manualX1);
            ImGui.sameLine();
            ImGui.text("Y1:");
            ImGui.sameLine();
            ImGui.setNextItemWidth(80);
            ImGui.inputInt("##manualY1", manualY1);

            // Fila 2: X2 / Y2
            ImGui.text("X2:");
            ImGui.sameLine();
            ImGui.setNextItemWidth(80);
            ImGui.inputInt("##manualX2", manualX2);
            ImGui.sameLine();
            ImGui.text("Y2:");
            ImGui.sameLine();
            ImGui.setNextItemWidth(80);
            ImGui.inputInt("##manualY2", manualY2);

            ImGui.unindent();
        }
    }

    // ═════════════════════════════════════════════════
    // MODO ALEATORIO
    // ═════════════════════════════════════════════════

    private void drawRandomModeToggle() {
        if (ImGui.checkbox(I18n.INSTANCE.get("editor.area.randomMode"), randomMode)) {
            randomMode = !randomMode;
        }

        if (randomMode) {
            ImGui.sameLine();
            ImGui.setNextItemWidth(120);
            ImGui.sliderFloat(I18n.INSTANCE.get("editor.area.density"), density, 0.01f, 1.0f, "%.0f%%");
        }
    }

    private int[] resolveBounds(MapContext context) {
        switch (areaSource) {
            case 0: // Selección actual
                return Selection.getInstance().getSelectionBounds();
            case 1: // Manual
                return new int[] { manualX1.get(), manualY1.get(), manualX2.get(), manualY2.get() };
            case 2: // Todo el mapa
                var mapData = context.getMapData();
                return new int[] { 0, 0, mapData.length - 1, mapData[0].length - 1 };
            default:
                return null;
        }
    }

    /**
     * @return Lista de tiles (x,y) si la selección actual tiene tiles individuales,
     *         null si es selección rectangular o no hay selección
     */
    private List<int[]> getSelectedTiles() {
        if (areaSource != 0) // Solo para selección actual
            return null;

        var selection = Selection.getInstance();
        var entities = selection.getSelectedEntities();

        if (entities == null || entities.isEmpty())
            return null;

        // Recolectar tiles únicos (solo TILE type)
        List<int[]> tiles = new java.util.ArrayList<>();
        for (var entity : entities) {
            if (entity.type == Selection.EntityType.TILE) {
                tiles.add(new int[] { entity.x, entity.y });
            }
        }

        return tiles.isEmpty() ? null : tiles;
    }

    // ═════════════════════════════════════════════════
    // SECCIONES
    // ═════════════════════════════════════════════════

    private boolean[] getLayersToAffect() {
        return new boolean[] { layer1, layer2, layer3, layer4 };
    }

    private void drawSurfaceSection(MapContext ctx, int[] bounds, boolean enabled) {
        ImGui.text(I18n.INSTANCE.get("editor.area.surfaces"));

        // Checkboxes de capas
        ImGui.indent();
        ImGui.text(I18n.INSTANCE.get("editor.area.layers") + ":");
        if (ImGui.checkbox("1##layer", layer1))
            layer1 = !layer1;
        ImGui.sameLine();
        if (ImGui.checkbox("2##layer", layer2))
            layer2 = !layer2;
        ImGui.sameLine();
        if (ImGui.checkbox("3##layer", layer3))
            layer3 = !layer3;
        ImGui.sameLine();
        if (ImGui.checkbox("4##layer", layer4))
            layer4 = !layer4;
        ImGui.unindent();

        Surface surface = Surface.getInstance();
        int surfaceGrh = surface.getSurfaceIndex();
        boolean useMosaic = surface.isUseMosaic();
        int mW = surface.getMosaicWidth();
        int mH = surface.getMosaicHeight();
        boolean hasMosaic = useMosaic && (mW > 1 || mH > 1);

        // Etiqueta del botón con info de mosaico
        String fillLabel = I18n.INSTANCE.get("editor.area.fill") + " (GRH: " + surfaceGrh;
        if (hasMosaic) {
            fillLabel += " | " + I18n.INSTANCE.get("editor.area.mosaic") + " " + mW + "x" + mH;
        }
        fillLabel += ")";

        // Botón rellenar (o aleatorio)
        ImGui.beginDisabled(!enabled || surfaceGrh <= 0);
        if (randomMode) {
            String rndLabel = I18n.INSTANCE.get("editor.area.fillRandom") + " (GRH: " + surfaceGrh + ")";
            if (ImGui.button(rndLabel + "##fillSurfaceRnd")) {
                int count = fillSurfaceWithSelectionSupport(ctx, bounds, surfaceGrh, true, density[0]);
                showResult(count);
            }
        } else {
            if (ImGui.button(fillLabel + "##fillSurface")) {
                int count = fillSurfaceWithSelectionSupport(ctx, bounds, surfaceGrh, false, 0);
                showResult(count);
            }
        }
        ImGui.endDisabled();

        ImGui.sameLine();

        ImGui.beginDisabled(!enabled);
        if (ImGui.button(I18n.INSTANCE.get("editor.area.clear") + "##clearSurface")) {
            int count = AreaOperationService.clearSurface(ctx,
                    bounds[0], bounds[1], bounds[2], bounds[3],
                    getLayersToAffect());
            showResult(count);
        }
        ImGui.endDisabled();
    }

    private void drawBlockSection(MapContext ctx, int[] bounds, boolean enabled) {
        ImGui.text(I18n.INSTANCE.get("editor.area.blocks"));

        ImGui.beginDisabled(!enabled);
        if (ImGui.button(I18n.INSTANCE.get("editor.area.fill") + "##fillBlocks")) {
            showResult(AreaOperationService.fillBlocks(ctx,
                    bounds[0], bounds[1], bounds[2], bounds[3]));
        }
        ImGui.sameLine();
        if (ImGui.button(I18n.INSTANCE.get("editor.area.clear") + "##clearBlocks")) {
            showResult(AreaOperationService.clearBlocks(ctx,
                    bounds[0], bounds[1], bounds[2], bounds[3]));
        }
        ImGui.sameLine();
        if (ImGui.button(I18n.INSTANCE.get("editor.area.invert") + "##invertBlocks")) {
            showResult(AreaOperationService.invertBlocks(ctx,
                    bounds[0], bounds[1], bounds[2], bounds[3]));
        }
        ImGui.endDisabled();
    }

    private void drawTriggerSection(MapContext ctx, int[] bounds, boolean enabled) {
        int triggerId = Trigger.getInstance().getSelectedTriggerId();
        ImGui.text(I18n.INSTANCE.get("editor.area.triggers"));

        ImGui.beginDisabled(!enabled || triggerId <= 0);
        if (ImGui.button(I18n.INSTANCE.get("editor.area.fill") + " (ID: " + triggerId + ")##fillTriggers")) {
            showResult(AreaOperationService.fillTriggers(ctx,
                    bounds[0], bounds[1], bounds[2], bounds[3], triggerId));
        }
        ImGui.endDisabled();

        ImGui.sameLine();

        ImGui.beginDisabled(!enabled);
        if (ImGui.button(I18n.INSTANCE.get("editor.area.clear") + "##clearTriggers")) {
            showResult(AreaOperationService.clearTriggers(ctx,
                    bounds[0], bounds[1], bounds[2], bounds[3]));
        }
        ImGui.endDisabled();
    }

    private void drawObjectSection(MapContext ctx, int[] bounds, boolean enabled) {
        int objNum = Obj.getInstance().getObjNumber();
        ImGui.text(I18n.INSTANCE.get("editor.area.objects"));

        ImGui.beginDisabled(!enabled || objNum <= 0);
        if (ImGui.button(I18n.INSTANCE.get("editor.area.fill") + " (Obj: " + objNum + ")##fillObjects")) {
            var objs = org.argentumforge.engine.utils.AssetRegistry.objs;
            if (objs != null && objs.containsKey(objNum)) {
                int grhIdx = objs.get(objNum).getGrhIndex();
                showResult(AreaOperationService.fillObjects(ctx,
                        bounds[0], bounds[1], bounds[2], bounds[3], grhIdx));
            }
        }
        ImGui.endDisabled();

        ImGui.sameLine();

        ImGui.beginDisabled(!enabled);
        if (ImGui.button(I18n.INSTANCE.get("editor.area.clear") + "##clearObjects")) {
            showResult(AreaOperationService.clearObjects(ctx,
                    bounds[0], bounds[1], bounds[2], bounds[3]));
        }
        ImGui.endDisabled();
    }

    private void drawParticleSection(MapContext ctx, int[] bounds, boolean enabled) {
        int particleId = Particle.getInstance().getSelectedParticleId();
        ImGui.text(I18n.INSTANCE.get("editor.area.particles"));

        ImGui.beginDisabled(!enabled || particleId <= 0);
        if (ImGui.button(I18n.INSTANCE.get("editor.area.fill") + " (ID: " + particleId + ")##fillParticles")) {
            showResult(AreaOperationService.fillParticles(ctx,
                    bounds[0], bounds[1], bounds[2], bounds[3], particleId));
        }
        ImGui.endDisabled();

        ImGui.sameLine();

        ImGui.beginDisabled(!enabled);
        if (ImGui.button(I18n.INSTANCE.get("editor.area.clear") + "##clearParticles")) {
            showResult(AreaOperationService.clearParticles(ctx,
                    bounds[0], bounds[1], bounds[2], bounds[3]));
        }
        ImGui.endDisabled();
    }

    private void drawTransferSection(MapContext ctx, int[] bounds, boolean enabled) {
        ImGui.text(I18n.INSTANCE.get("editor.area.transfers"));

        ImGui.beginDisabled(!enabled);
        if (ImGui.button(I18n.INSTANCE.get("editor.area.clear") + "##clearTransfers")) {
            showResult(AreaOperationService.clearTransfers(ctx,
                    bounds[0], bounds[1], bounds[2], bounds[3]));
        }
        ImGui.endDisabled();
    }

    private void drawNpcSection(MapContext ctx, int[] bounds, boolean enabled) {
        int npcNum = Npc.getInstance().getNpcNumber();
        ImGui.text(I18n.INSTANCE.get("editor.area.npcs"));

        // Rellenar NPCs (normal o aleatorio)
        ImGui.beginDisabled(!enabled || npcNum <= 0);
        if (randomMode) {
            String rndLabel = I18n.INSTANCE.get("editor.area.fillRandom") + " (NPC: " + npcNum + ")";
            if (ImGui.button(rndLabel + "##fillNpcsRnd")) {
                showResult(AreaOperationService.fillNpcsRandom(ctx,
                        bounds[0], bounds[1], bounds[2], bounds[3],
                        npcNum, density[0]));
            }
        } else {
            if (ImGui.button(I18n.INSTANCE.get("editor.area.fill") + " (NPC: " + npcNum + ")##fillNpcs")) {
                showResult(AreaOperationService.fillNpcs(ctx,
                        bounds[0], bounds[1], bounds[2], bounds[3], npcNum));
            }
        }
        ImGui.endDisabled();

        ImGui.sameLine();

        ImGui.beginDisabled(!enabled);
        if (ImGui.button(I18n.INSTANCE.get("editor.area.clear") + "##clearNpcs")) {
            showResult(AreaOperationService.clearNpcs(ctx,
                    bounds[0], bounds[1], bounds[2], bounds[3]));
        }
        ImGui.endDisabled();
    }

    // ═════════════════════════════════════════════════
    // UTILIDADES
    // ═════════════════════════════════════════════════

    private int fillSurfaceWithSelectionSupport(MapContext ctx, int[] bounds, int grhIndex, boolean random,
            float density) {
        List<int[]> selectedTiles = getSelectedTiles();
        if (selectedTiles != null) {
            var mapData = ctx.getMapData();
            org.argentumforge.engine.utils.editor.commands.MacroCommand macro = new org.argentumforge.engine.utils.editor.commands.MacroCommand();
            int count = 0;
            Surface surface = Surface.getInstance();
            boolean useMosaic = surface.isUseMosaic();
            int mosaicW = surface.getMosaicWidth(), mosaicH = surface.getMosaicHeight();
            boolean hasMosaic = useMosaic && (mosaicW > 1 || mosaicH > 1);
            for (int layerIdx = 0; layerIdx < 4; layerIdx++) {
                boolean[] layers = getLayersToAffect();
                if (!layers[layerIdx])
                    continue;
                int layer = layerIdx + 1;
                java.util.Map<org.argentumforge.engine.utils.editor.commands.BulkTileChangeCommand.TilePos, Integer> oldTiles = new java.util.HashMap<>();
                java.util.Map<org.argentumforge.engine.utils.editor.commands.BulkTileChangeCommand.TilePos, Integer> newTiles = new java.util.HashMap<>();
                for (int[] tile : selectedTiles) {
                    int x = tile[0], y = tile[1];
                    if (x < 0 || x >= mapData.length || y < 0 || y >= mapData[0].length)
                        continue;
                    if (random && Math.random() > density)
                        continue;
                    int current = mapData[x][y].getLayer(layer).getGrhIndex();
                    int targetGrh = hasMosaic ? grhIndex + ((y % mosaicH) * mosaicW) + (x % mosaicW) : grhIndex;
                    if (current != targetGrh) {
                        var pos = new org.argentumforge.engine.utils.editor.commands.BulkTileChangeCommand.TilePos(x,
                                y);
                        oldTiles.put(pos, current);
                        newTiles.put(pos, targetGrh);
                        count++;
                    }
                }
                if (!oldTiles.isEmpty())
                    macro.addCommand(new org.argentumforge.engine.utils.editor.commands.BulkTileChangeCommand(ctx,
                            layer, oldTiles, newTiles));
            }
            if (count > 0 && !macro.getCommands().isEmpty())
                org.argentumforge.engine.utils.editor.commands.CommandManager.getInstance().executeCommand(macro);
            return count;
        } else {
            return random
                    ? AreaOperationService.fillSurfaceRandom(ctx, bounds[0], bounds[1], bounds[2], bounds[3], grhIndex,
                            getLayersToAffect(), density)
                    : AreaOperationService.fillSurface(ctx, bounds[0], bounds[1], bounds[2], bounds[3], grhIndex,
                            getLayersToAffect());
        }
    }

    private void showResult(int count) {
        if (count > 0) {
            DialogManager.getInstance().showInfo(
                    I18n.INSTANCE.get("editor.area.title"),
                    I18n.INSTANCE.get("editor.area.result", count));
        } else {
            DialogManager.getInstance().showInfo(
                    I18n.INSTANCE.get("editor.area.title"),
                    I18n.INSTANCE.get("editor.area.noTiles"));
        }
    }
}
