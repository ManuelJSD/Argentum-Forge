package org.argentumforge.engine.utils.editor;

import org.argentumforge.engine.utils.GameData;
import org.argentumforge.engine.scenes.Camera;

/**
 * Clase de utilidad para transiciones de tile automáticas (auto-costas).
 * Usa máscaras de bits para determinar los tiles de borde correctos entre diferentes
 * tipos de terreno.
 */
import org.argentumforge.engine.utils.MapContext;

/**
 * Clase de utilidad para transiciones de tile automáticas (auto-costas).
 * Usa máscaras de bits para determinar los tiles de borde correctos entre
 * diferentes tipos de terreno.
 */
public class AutoTiler {

    /**
     * Aplica auto-costas entre dos tipos de terreno.
     * 
     * @param layer         Capa donde aplicar las costas
     * @param waterGrh      Índice GRH para tiles de agua
     * @param landGrh       Índice GRH para tiles de tierra
     * @param coastGrhStart Índice GRH inicial para tiles de costa (asume set de 16)
     */
    public static void applyCoasting(MapContext context, int layer, int waterGrh, int landGrh, int coastGrhStart) {
        if (context == null)
            return;
        var mapData = context.getMapData();
        if (mapData == null)
            return;

        java.util.Map<org.argentumforge.engine.utils.editor.commands.BulkTileChangeCommand.TilePos, Integer> oldTiles = new java.util.HashMap<>();
        java.util.Map<org.argentumforge.engine.utils.editor.commands.BulkTileChangeCommand.TilePos, Integer> newTiles = new java.util.HashMap<>();

        for (int x = Camera.XMinMapSize; x <= Camera.XMaxMapSize; x++) {
            for (int y = Camera.YMinMapSize; y <= Camera.YMaxMapSize; y++) {
                int currentGrh = mapData[x][y].getLayer(layer).getGrhIndex();

                // Solo procesar tiles de tierra que limitan con agua
                if (currentGrh == landGrh) {
                    int bitmask = calculateBitmask(context, x, y, layer, waterGrh);

                    if (bitmask > 0) {
                        // Este tile de tierra limita con agua, aplicar tile de costa
                        int coastTile = getCoastTile(coastGrhStart, bitmask);
                        oldTiles.put(
                                new org.argentumforge.engine.utils.editor.commands.BulkTileChangeCommand.TilePos(x, y),
                                currentGrh);
                        newTiles.put(
                                new org.argentumforge.engine.utils.editor.commands.BulkTileChangeCommand.TilePos(x, y),
                                coastTile);
                    }
                }
            }
        }

        if (!newTiles.isEmpty()) {
            org.argentumforge.engine.utils.editor.commands.CommandManager.getInstance().executeCommand(
                    new org.argentumforge.engine.utils.editor.commands.BulkTileChangeCommand(
                            GameData.getActiveContext(), layer, oldTiles,
                            newTiles));
        }
    }

    /**
     * Calcula máscara de bits de 4 direcciones para un tile.
     * Bit 0 (1) = Norte tiene agua
     * Bit 1 (2) = Este tiene agua
     * Bit 2 (4) = Sur tiene agua
     * Bit 3 (8) = Oeste tiene agua
     */
    private static int calculateBitmask(MapContext context, int x, int y, int layer, int waterGrh) {
        int mask = 0;
        var mapData = context.getMapData();

        // Norte
        if (y > Camera.YMinMapSize && mapData[x][y - 1].getLayer(layer).getGrhIndex() == waterGrh) {
            mask |= 1;
        }
        // Este
        if (x < Camera.XMaxMapSize && mapData[x + 1][y].getLayer(layer).getGrhIndex() == waterGrh) {
            mask |= 2;
        }
        // Sur
        if (y < Camera.YMaxMapSize && mapData[x][y + 1].getLayer(layer).getGrhIndex() == waterGrh) {
            mask |= 4;
        }
        // Oeste
        if (x > Camera.XMinMapSize && mapData[x - 1][y].getLayer(layer).getGrhIndex() == waterGrh) {
            mask |= 8;
        }

        return mask;
    }

    /**
     * Mapea máscara de bits a índice de tile de costa.
     * Asume un set de costas de 16 tiles organizado como:
     * 0-3: Bordes simples (N, E, S, O)
     * 4-7: Esquinas (NE, SE, SO, NO)
     * 8-15: Transiciones complejas
     */
    private static int getCoastTile(int baseGrh, int bitmask) {
        // Mapeo simplificado - puede necesitar ajustes según tu tileset
        switch (bitmask) {
            case 1:
                return (baseGrh + 0); // North edge
            case 2:
                return (baseGrh + 1); // East edge
            case 4:
                return (baseGrh + 2); // South edge
            case 8:
                return (baseGrh + 3); // West edge
            case 3:
                return (baseGrh + 4); // NE corner
            case 6:
                return (baseGrh + 5); // SE corner
            case 12:
                return (baseGrh + 6); // SW corner
            case 9:
                return (baseGrh + 7); // NW corner
            case 5:
                return (baseGrh + 8); // N+S edges
            case 10:
                return (baseGrh + 9); // E+W edges
            case 7:
                return (baseGrh + 10); // N+E+S
            case 14:
                return (baseGrh + 11); // E+S+W
            case 13:
                return (baseGrh + 12); // S+W+N
            case 11:
                return (baseGrh + 13); // W+N+E
            case 15:
                return (baseGrh + 14); // All sides (island)
            default:
                return (baseGrh + 15); // Fallback
        }
    }

    /**
     * Aplica variación de mosaico aleatoria a tiles de un tipo específico.
     * 
     * @param layer        Capa donde aplicar mosaico
     * @param baseGrh      Índice GRH base
     * @param mosaicWidth  Ancho del patrón de mosaico
     * @param mosaicHeight Alto del patrón de mosaico
     */
    public static void applyMosaic(MapContext context, int layer, int baseGrh, int mosaicWidth, int mosaicHeight) {
        if (context == null)
            return;
        var mapData = context.getMapData();
        if (mapData == null || mosaicWidth <= 1 || mosaicHeight <= 1)
            return;

        java.util.Map<org.argentumforge.engine.utils.editor.commands.BulkTileChangeCommand.TilePos, Integer> oldTiles = new java.util.HashMap<>();
        java.util.Map<org.argentumforge.engine.utils.editor.commands.BulkTileChangeCommand.TilePos, Integer> newTiles = new java.util.HashMap<>();

        for (int x = Camera.XMinMapSize; x <= Camera.XMaxMapSize; x++) {
            for (int y = Camera.YMinMapSize; y <= Camera.YMaxMapSize; y++) {
                int currentGrh = mapData[x][y].getLayer(layer).getGrhIndex();

                if (currentGrh == baseGrh) {
                    // Aplicar patrón de mosaico
                    int relX = x % mosaicWidth;
                    int relY = y % mosaicHeight;
                    int mosaicGrh = (baseGrh + (relY * mosaicWidth) + relX);

                    if (currentGrh != mosaicGrh) {
                        oldTiles.put(
                                new org.argentumforge.engine.utils.editor.commands.BulkTileChangeCommand.TilePos(x, y),
                                currentGrh);
                        newTiles.put(
                                new org.argentumforge.engine.utils.editor.commands.BulkTileChangeCommand.TilePos(x, y),
                                mosaicGrh);
                    }
                }
            }
        }

        if (!newTiles.isEmpty()) {
            org.argentumforge.engine.utils.editor.commands.CommandManager.getInstance().executeCommand(
                    new org.argentumforge.engine.utils.editor.commands.BulkTileChangeCommand(
                            GameData.getActiveContext(), layer, oldTiles,
                            newTiles));
        }
    }
}
