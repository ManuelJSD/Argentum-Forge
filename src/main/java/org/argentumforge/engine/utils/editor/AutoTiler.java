package org.argentumforge.engine.utils.editor;

import org.argentumforge.engine.utils.GameData;
import org.argentumforge.engine.scenes.Camera;

/**
 * Utility class for automatic tile transitions (auto-coasting).
 * Uses bitmasking to determine correct edge tiles between different terrain
 * types.
 */
import org.argentumforge.engine.utils.MapContext;

/**
 * Utility class for automatic tile transitions (auto-coasting).
 * Uses bitmasking to determine correct edge tiles between different terrain
 * types.
 */
public class AutoTiler {

    /**
     * Applies auto-coasting between two terrain types.
     * 
     * @param layer         Layer to apply coasting on
     * @param waterGrh      GRH index for water tiles
     * @param landGrh       GRH index for land tiles
     * @param coastGrhStart Starting GRH index for coast tiles (assumes 16-tile set)
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

                // Only process land tiles that border water
                if (currentGrh == landGrh) {
                    int bitmask = calculateBitmask(context, x, y, layer, waterGrh);

                    if (bitmask > 0) {
                        // This land tile borders water, apply coast tile
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
     * Calculates 4-directional bitmask for a tile.
     * Bit 0 (1) = North has water
     * Bit 1 (2) = East has water
     * Bit 2 (4) = South has water
     * Bit 3 (8) = West has water
     */
    private static int calculateBitmask(MapContext context, int x, int y, int layer, int waterGrh) {
        int mask = 0;
        var mapData = context.getMapData();

        // North
        if (y > Camera.YMinMapSize && mapData[x][y - 1].getLayer(layer).getGrhIndex() == waterGrh) {
            mask |= 1;
        }
        // East
        if (x < Camera.XMaxMapSize && mapData[x + 1][y].getLayer(layer).getGrhIndex() == waterGrh) {
            mask |= 2;
        }
        // South
        if (y < Camera.YMaxMapSize && mapData[x][y + 1].getLayer(layer).getGrhIndex() == waterGrh) {
            mask |= 4;
        }
        // West
        if (x > Camera.XMinMapSize && mapData[x - 1][y].getLayer(layer).getGrhIndex() == waterGrh) {
            mask |= 8;
        }

        return mask;
    }

    /**
     * Maps bitmask to coast tile index.
     * Assumes a 16-tile coast set arranged as:
     * 0-3: Single edges (N, E, S, W)
     * 4-7: Corners (NE, SE, SW, NW)
     * 8-15: Complex transitions
     */
    private static int getCoastTile(int baseGrh, int bitmask) {
        // Simplified mapping - you may need to adjust based on your tileset
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
     * Applies random mosaic variation to tiles of a specific type.
     * 
     * @param layer        Layer to apply mosaic on
     * @param baseGrh      Base GRH index
     * @param mosaicWidth  Width of mosaic pattern
     * @param mosaicHeight Height of mosaic pattern
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
                    // Apply tiling pattern
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
