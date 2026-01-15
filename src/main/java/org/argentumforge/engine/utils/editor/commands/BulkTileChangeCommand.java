package org.argentumforge.engine.utils.editor.commands;

import java.util.Map;
import java.util.Objects;
import org.argentumforge.engine.utils.GameData;
import static org.argentumforge.engine.utils.GameData.initGrh;

/**
 * Comando para cambios masivos de tiles (Relleno, Pinceles grandes).
 */
public class BulkTileChangeCommand implements Command {
    private final int layer;
    private final Map<TilePos, Short> oldTiles;
    private final Map<TilePos, Short> newTiles;

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

    public BulkTileChangeCommand(int layer, Map<TilePos, Short> oldTiles, Map<TilePos, Short> newTiles) {
        this.layer = layer;
        this.oldTiles = oldTiles;
        this.newTiles = newTiles;
    }

    @Override
    public void execute() {
        for (Map.Entry<TilePos, Short> entry : newTiles.entrySet()) {
            TilePos pos = entry.getKey();
            short newGrh = entry.getValue();
            GameData.mapData[pos.x][pos.y].getLayer(layer).setGrhIndex(newGrh);
            initGrh(GameData.mapData[pos.x][pos.y].getLayer(layer), newGrh, true);
        }
    }

    @Override
    public void undo() {
        for (Map.Entry<TilePos, Short> entry : oldTiles.entrySet()) {
            TilePos pos = entry.getKey();
            short oldGrh = entry.getValue();
            GameData.mapData[pos.x][pos.y].getLayer(layer).setGrhIndex(oldGrh);
            initGrh(GameData.mapData[pos.x][pos.y].getLayer(layer), oldGrh, true);
        }
    }
}
