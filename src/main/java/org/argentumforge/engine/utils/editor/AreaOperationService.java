package org.argentumforge.engine.utils.editor;

import org.argentumforge.engine.utils.MapContext;
import org.argentumforge.engine.utils.inits.MapData;
import org.argentumforge.engine.utils.editor.commands.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Servicio centralizado para ejecutar operaciones masivas (rellenar/eliminar)
 * sobre un área rectangular del mapa.
 * <p>
 * Cada operación genera un único comando atómico (o {@link MacroCommand})
 * para que pueda deshacerse con un solo Ctrl+Z.
 */
public final class AreaOperationService {

    private static final Random RANDOM = new Random();

    private AreaOperationService() {
        // Utilidad estática
    }

    // ─────────────────────────────────────────────
    // SUPERFICIES
    // ─────────────────────────────────────────────

    /**
     * Rellena las capas de superficie indicadas con el GRH dado.
     * Si el mosaico está activado en {@link Surface}, aplica la lógica
     * de mosaico para calcular el GRH correcto en cada tile.
     *
     * @param layers boolean[4] indicando qué capas (1-4, índice 0-3) afectar
     * @return cantidad de operaciones sobre tiles realizadas
     */
    public static int fillSurface(MapContext ctx, int x1, int y1, int x2, int y2,
            int grhIndex, boolean[] layers) {
        if (ctx == null || ctx.getMapData() == null || grhIndex <= 0)
            return 0;
        var mapData = ctx.getMapData();
        MacroCommand macro = new MacroCommand();
        int count = 0;

        // Leer configuración de mosaico desde Surface
        Surface surface = Surface.getInstance();
        boolean useMosaic = surface.isUseMosaic();
        int mosaicW = surface.getMosaicWidth();
        int mosaicH = surface.getMosaicHeight();
        boolean hasMosaic = useMosaic && (mosaicW > 1 || mosaicH > 1);

        for (int layerIdx = 0; layerIdx < 4; layerIdx++) {
            if (!layers[layerIdx])
                continue;
            int layer = layerIdx + 1; // capas 1-4

            Map<BulkTileChangeCommand.TilePos, Integer> oldTiles = new HashMap<>();
            Map<BulkTileChangeCommand.TilePos, Integer> newTiles = new HashMap<>();

            for (int x = x1; x <= x2; x++) {
                for (int y = y1; y <= y2; y++) {
                    if (!inBounds(mapData, x, y))
                        continue;
                    int current = mapData[x][y].getLayer(layer).getGrhIndex();

                    // Calcular GRH destino considerando mosaico
                    int targetGrh = grhIndex;
                    if (hasMosaic) {
                        int offsetX = x % mosaicW;
                        int offsetY = y % mosaicH;
                        targetGrh = grhIndex + (offsetY * mosaicW) + offsetX;
                    }

                    if (current != targetGrh) {
                        var pos = new BulkTileChangeCommand.TilePos(x, y);
                        oldTiles.put(pos, current);
                        newTiles.put(pos, targetGrh);
                        count++;
                    }
                }
            }

            if (!oldTiles.isEmpty()) {
                macro.addCommand(new BulkTileChangeCommand(ctx, layer, oldTiles, newTiles));
            }
        }

        executeIfNotEmpty(macro, count);
        return count;
    }

    /**
     * Rellena aleatoriamente las capas de superficie con el GRH dado.
     * Cada tile tiene una probabilidad (densidad) de ser rellenado.
     *
     * @param density probabilidad de relleno por tile (0.0 a 1.0)
     * @return cantidad de tiles afectados
     */
    public static int fillSurfaceRandom(MapContext ctx, int x1, int y1, int x2, int y2,
            int grhIndex, boolean[] layers, float density) {
        if (ctx == null || ctx.getMapData() == null || grhIndex <= 0)
            return 0;
        var mapData = ctx.getMapData();
        MacroCommand macro = new MacroCommand();
        int count = 0;

        for (int layerIdx = 0; layerIdx < 4; layerIdx++) {
            if (!layers[layerIdx])
                continue;
            int layer = layerIdx + 1;

            Map<BulkTileChangeCommand.TilePos, Integer> oldTiles = new HashMap<>();
            Map<BulkTileChangeCommand.TilePos, Integer> newTiles = new HashMap<>();

            for (int x = x1; x <= x2; x++) {
                for (int y = y1; y <= y2; y++) {
                    if (!inBounds(mapData, x, y))
                        continue;
                    if (RANDOM.nextFloat() > density)
                        continue;
                    int current = mapData[x][y].getLayer(layer).getGrhIndex();
                    if (current != grhIndex) {
                        var pos = new BulkTileChangeCommand.TilePos(x, y);
                        oldTiles.put(pos, current);
                        newTiles.put(pos, grhIndex);
                        count++;
                    }
                }
            }

            if (!oldTiles.isEmpty()) {
                macro.addCommand(new BulkTileChangeCommand(ctx, layer, oldTiles, newTiles));
            }
        }

        executeIfNotEmpty(macro, count);
        return count;
    }

    /**
     * Limpia (pone a 0) las capas de superficie indicadas.
     */
    public static int clearSurface(MapContext ctx, int x1, int y1, int x2, int y2,
            boolean[] layers) {
        if (ctx == null || ctx.getMapData() == null)
            return 0;
        var mapData = ctx.getMapData();
        MacroCommand macro = new MacroCommand();
        int count = 0;

        for (int layerIdx = 0; layerIdx < 4; layerIdx++) {
            if (!layers[layerIdx])
                continue;
            int layer = layerIdx + 1;

            Map<BulkTileChangeCommand.TilePos, Integer> oldTiles = new HashMap<>();
            Map<BulkTileChangeCommand.TilePos, Integer> newTiles = new HashMap<>();

            for (int x = x1; x <= x2; x++) {
                for (int y = y1; y <= y2; y++) {
                    if (!inBounds(mapData, x, y))
                        continue;
                    int current = mapData[x][y].getLayer(layer).getGrhIndex();
                    if (current != 0) {
                        var pos = new BulkTileChangeCommand.TilePos(x, y);
                        oldTiles.put(pos, current);
                        newTiles.put(pos, 0);
                        count++;
                    }
                }
            }

            if (!oldTiles.isEmpty()) {
                macro.addCommand(new BulkTileChangeCommand(ctx, layer, oldTiles, newTiles));
            }
        }

        executeIfNotEmpty(macro, count);
        return count;
    }

    // ─────────────────────────────────────────────
    // BLOQUEOS
    // ─────────────────────────────────────────────

    /**
     * Bloquea todos los tiles del área.
     */
    public static int fillBlocks(MapContext ctx, int x1, int y1, int x2, int y2) {
        return setBlocks(ctx, x1, y1, x2, y2, true, false);
    }

    /**
     * Desbloquea todos los tiles del área.
     */
    public static int clearBlocks(MapContext ctx, int x1, int y1, int x2, int y2) {
        return setBlocks(ctx, x1, y1, x2, y2, false, false);
    }

    /**
     * Invierte el estado de bloqueo de los tiles del área.
     */
    public static int invertBlocks(MapContext ctx, int x1, int y1, int x2, int y2) {
        return setBlocks(ctx, x1, y1, x2, y2, false, true);
    }

    private static int setBlocks(MapContext ctx, int x1, int y1, int x2, int y2,
            boolean target, boolean invert) {
        if (ctx == null || ctx.getMapData() == null)
            return 0;
        var mapData = ctx.getMapData();

        Map<BlockChangeCommand.TilePos, Boolean> oldStates = new HashMap<>();
        Map<BlockChangeCommand.TilePos, Boolean> newStates = new HashMap<>();

        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                if (!inBounds(mapData, x, y))
                    continue;
                boolean current = mapData[x][y].getBlocked();
                boolean next = invert ? !current : target;
                if (current != next) {
                    var pos = new BlockChangeCommand.TilePos(x, y);
                    oldStates.put(pos, current);
                    newStates.put(pos, next);
                }
            }
        }

        if (!oldStates.isEmpty()) {
            CommandManager.getInstance().executeCommand(
                    new BlockChangeCommand(ctx, oldStates, newStates));
        }
        return oldStates.size();
    }

    // ─────────────────────────────────────────────
    // TRIGGERS
    // ─────────────────────────────────────────────

    /**
     * Rellena triggers con el ID indicado.
     */
    public static int fillTriggers(MapContext ctx, int x1, int y1, int x2, int y2,
            int triggerId) {
        return setTriggers(ctx, x1, y1, x2, y2, (short) triggerId);
    }

    /**
     * Limpia triggers (pone a 0).
     */
    public static int clearTriggers(MapContext ctx, int x1, int y1, int x2, int y2) {
        return setTriggers(ctx, x1, y1, x2, y2, (short) 0);
    }

    private static int setTriggers(MapContext ctx, int x1, int y1, int x2, int y2,
            short target) {
        if (ctx == null || ctx.getMapData() == null)
            return 0;
        var mapData = ctx.getMapData();

        Map<TriggerChangeCommand.TilePos, Short> oldStates = new HashMap<>();
        Map<TriggerChangeCommand.TilePos, Short> newStates = new HashMap<>();

        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                if (!inBounds(mapData, x, y))
                    continue;
                short current = (short) mapData[x][y].getTrigger();
                if (current != target) {
                    var pos = new TriggerChangeCommand.TilePos(x, y);
                    oldStates.put(pos, current);
                    newStates.put(pos, target);
                }
            }
        }

        if (!oldStates.isEmpty()) {
            CommandManager.getInstance().executeCommand(
                    new TriggerChangeCommand(ctx, oldStates, newStates));
        }
        return oldStates.size();
    }

    // ─────────────────────────────────────────────
    // OBJETOS
    // ─────────────────────────────────────────────

    /**
     * Coloca el objeto (por GRH) en todos los tiles del área.
     */
    public static int fillObjects(MapContext ctx, int x1, int y1, int x2, int y2,
            int objGrhIndex) {
        if (ctx == null || ctx.getMapData() == null || objGrhIndex <= 0)
            return 0;
        var mapData = ctx.getMapData();
        MacroCommand macro = new MacroCommand();

        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                if (!inBounds(mapData, x, y))
                    continue;
                int oldGrh = mapData[x][y].getObjGrh().getGrhIndex();
                if (oldGrh != objGrhIndex) {
                    macro.addCommand(new ObjChangeCommand(ctx, x, y, oldGrh, objGrhIndex));
                }
            }
        }

        if (!macro.getCommands().isEmpty()) {
            CommandManager.getInstance().executeCommand(macro);
        }
        return macro.getCommands().size();
    }

    /**
     * Elimina objetos de todos los tiles del área.
     */
    public static int clearObjects(MapContext ctx, int x1, int y1, int x2, int y2) {
        if (ctx == null || ctx.getMapData() == null)
            return 0;
        var mapData = ctx.getMapData();
        MacroCommand macro = new MacroCommand();

        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                if (!inBounds(mapData, x, y))
                    continue;
                int oldGrh = mapData[x][y].getObjGrh().getGrhIndex();
                if (oldGrh != 0) {
                    macro.addCommand(new ObjChangeCommand(ctx, x, y, oldGrh, 0));
                }
            }
        }

        if (!macro.getCommands().isEmpty()) {
            CommandManager.getInstance().executeCommand(macro);
        }
        return macro.getCommands().size();
    }

    // ─────────────────────────────────────────────
    // PARTÍCULAS
    // ─────────────────────────────────────────────

    /**
     * Coloca partículas con el ID indicado en el área.
     */
    public static int fillParticles(MapContext ctx, int x1, int y1, int x2, int y2,
            int particleId) {
        return setParticles(ctx, x1, y1, x2, y2, particleId);
    }

    /**
     * Elimina partículas del área.
     */
    public static int clearParticles(MapContext ctx, int x1, int y1, int x2, int y2) {
        return setParticles(ctx, x1, y1, x2, y2, 0);
    }

    private static int setParticles(MapContext ctx, int x1, int y1, int x2, int y2,
            int target) {
        if (ctx == null || ctx.getMapData() == null)
            return 0;
        var mapData = ctx.getMapData();

        Map<ParticleChangeCommand.TilePos, Integer> oldStates = new HashMap<>();
        Map<ParticleChangeCommand.TilePos, Integer> newStates = new HashMap<>();

        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                if (!inBounds(mapData, x, y))
                    continue;
                int current = mapData[x][y].getParticleIndex();
                if (current != target) {
                    var pos = new ParticleChangeCommand.TilePos(x, y);
                    oldStates.put(pos, current);
                    newStates.put(pos, target);
                }
            }
        }

        if (!oldStates.isEmpty()) {
            CommandManager.getInstance().executeCommand(
                    new ParticleChangeCommand(ctx, oldStates, newStates));
        }
        return oldStates.size();
    }

    // ─────────────────────────────────────────────
    // TRASLADOS
    // ─────────────────────────────────────────────

    /**
     * Elimina traslados (teleports) del área.
     */
    public static int clearTransfers(MapContext ctx, int x1, int y1, int x2, int y2) {
        if (ctx == null || ctx.getMapData() == null)
            return 0;
        var mapData = ctx.getMapData();
        MacroCommand macro = new MacroCommand();

        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                if (!inBounds(mapData, x, y))
                    continue;
                int exitMap = mapData[x][y].getExitMap();
                int exitX = mapData[x][y].getExitX();
                int exitY = mapData[x][y].getExitY();
                if (exitMap != 0 || exitX != 0 || exitY != 0) {
                    macro.addCommand(new TransferChangeCommand(
                            ctx, x, y, exitMap, exitX, exitY, 0, 0, 0));
                }
            }
        }

        if (!macro.getCommands().isEmpty()) {
            CommandManager.getInstance().executeCommand(macro);
        }
        return macro.getCommands().size();
    }

    // ─────────────────────────────────────────────
    // NPCs
    // ─────────────────────────────────────────────

    /**
     * Coloca el NPC indicado en todos los tiles del área.
     */
    public static int fillNpcs(MapContext ctx, int x1, int y1, int x2, int y2,
            int npcIndex) {
        if (ctx == null || ctx.getMapData() == null || npcIndex <= 0)
            return 0;
        var mapData = ctx.getMapData();
        MacroCommand macro = new MacroCommand();

        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                if (!inBounds(mapData, x, y))
                    continue;
                int oldNpc = mapData[x][y].getNpcIndex();
                if (oldNpc != npcIndex) {
                    macro.addCommand(new NpcChangeCommand(ctx, x, y, oldNpc, npcIndex));
                }
            }
        }

        if (!macro.getCommands().isEmpty()) {
            CommandManager.getInstance().executeCommand(macro);
        }
        return macro.getCommands().size();
    }

    /**
     * Coloca el NPC indicado de forma aleatoria en el área.
     *
     * @param density probabilidad de colocar un NPC por tile (0.0 a 1.0)
     * @return cantidad de NPCs colocados
     */
    public static int fillNpcsRandom(MapContext ctx, int x1, int y1, int x2, int y2,
            int npcIndex, float density) {
        if (ctx == null || ctx.getMapData() == null || npcIndex <= 0)
            return 0;
        var mapData = ctx.getMapData();
        MacroCommand macro = new MacroCommand();

        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                if (!inBounds(mapData, x, y))
                    continue;
                if (RANDOM.nextFloat() > density)
                    continue;
                int oldNpc = mapData[x][y].getNpcIndex();
                if (oldNpc != npcIndex) {
                    macro.addCommand(new NpcChangeCommand(ctx, x, y, oldNpc, npcIndex));
                }
            }
        }

        if (!macro.getCommands().isEmpty()) {
            CommandManager.getInstance().executeCommand(macro);
        }
        return macro.getCommands().size();
    }

    /**
     * Elimina NPCs del área.
     */
    public static int clearNpcs(MapContext ctx, int x1, int y1, int x2, int y2) {
        if (ctx == null || ctx.getMapData() == null)
            return 0;
        var mapData = ctx.getMapData();
        MacroCommand macro = new MacroCommand();

        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                if (!inBounds(mapData, x, y))
                    continue;
                int npcIdx = mapData[x][y].getNpcIndex();
                if (npcIdx != 0) {
                    macro.addCommand(new NpcChangeCommand(ctx, x, y, npcIdx, 0));
                }
            }
        }

        if (!macro.getCommands().isEmpty()) {
            CommandManager.getInstance().executeCommand(macro);
        }
        return macro.getCommands().size();
    }

    // ─────────────────────────────────────────────
    // UTILIDADES
    // ─────────────────────────────────────────────

    private static boolean inBounds(MapData[][] mapData, int x, int y) {
        return x >= 0 && x < mapData.length && y >= 0 && y < mapData[0].length
                && mapData[x][y] != null;
    }

    private static void executeIfNotEmpty(MacroCommand macro, int count) {
        if (count > 0 && !macro.getCommands().isEmpty()) {
            CommandManager.getInstance().executeCommand(macro);
        }
    }
}
