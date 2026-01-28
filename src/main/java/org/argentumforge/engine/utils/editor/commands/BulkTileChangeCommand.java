package org.argentumforge.engine.utils.editor.commands;

import org.argentumforge.engine.i18n.I18n;
import java.util.Map;
import java.util.Objects;
import org.argentumforge.engine.utils.GameData;
import static org.argentumforge.engine.utils.GameData.initGrh;

/**
 * Comando para cambios masivos de tiles (Relleno, Pinceles grandes).
 */
public class BulkTileChangeCommand implements Command {
    @Override
    public String getName() {
        return I18n.INSTANCE.get("history.command.bulk_tile");
    }

    private final int layer;
    private final Map<TilePos, Integer> oldTiles;
    private final Map<TilePos, Integer> newTiles;

    public static class TilePos {
        public final int x, y;

        public TilePos(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            TilePos tilePos = (TilePos) o;
            return x == tilePos.x && y == tilePos.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }

    public BulkTileChangeCommand(int layer, Map<TilePos, Integer> oldTiles, Map<TilePos, Integer> newTiles) {
        this.layer = layer;
        this.oldTiles = oldTiles;
        this.newTiles = newTiles;
    }

    @Override
    public void execute() {
        for (Map.Entry<TilePos, Integer> entry : newTiles.entrySet()) {
            TilePos pos = entry.getKey();
            int newGrh = entry.getValue();
            GameData.mapData[pos.x][pos.y].getLayer(layer).setGrhIndex(newGrh);
            initGrh(GameData.mapData[pos.x][pos.y].getLayer(layer), newGrh, true);
        }
    }

    @Override
    public void undo() {
        for (Map.Entry<TilePos, Integer> entry : oldTiles.entrySet()) {
            TilePos pos = entry.getKey();
            int oldGrh = entry.getValue();
            GameData.mapData[pos.x][pos.y].getLayer(layer).setGrhIndex(oldGrh);
            initGrh(GameData.mapData[pos.x][pos.y].getLayer(layer), oldGrh, true);
        }
    }

    @Override
    public int[] getAffectedBounds() {
        if (newTiles.isEmpty())
            return null;
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (TilePos pos : newTiles.keySet()) {
            if (pos.x < minX)
                minX = pos.x;
            if (pos.x > maxX)
                maxX = pos.x;
            if (pos.y < minY)
                minY = pos.y;
            if (pos.y > maxY)
                maxY = pos.y;
        }
        return new int[] { minX, minY, maxX, maxY };
    }
}
