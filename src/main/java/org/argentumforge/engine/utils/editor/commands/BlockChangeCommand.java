package org.argentumforge.engine.utils.editor.commands;

import org.argentumforge.engine.i18n.I18n;
import java.util.Map;

/**
 * Comando para registrar y revertir cambios de bloqueos.
 * Soporta cambios en múltiples tiles (útil para pinceles).
 */
public class BlockChangeCommand extends AbstractCommand {
    @Override
    public String getName() {
        return I18n.INSTANCE.get("history.command.block");
    }

    private final Map<TilePos, Boolean> oldStates;
    private final Map<TilePos, Boolean> newStates;

    public static record TilePos(int x, int y) {
    }

    public BlockChangeCommand(org.argentumforge.engine.utils.MapContext context, Map<TilePos, Boolean> oldStates,
            Map<TilePos, Boolean> newStates) {
        super(context);
        this.oldStates = oldStates;
        this.newStates = newStates;
    }

    @Override
    public void execute() {
        var mapData = context.getMapData();
        for (Map.Entry<TilePos, Boolean> entry : newStates.entrySet()) {
            mapData[entry.getKey().x][entry.getKey().y].setBlocked(entry.getValue());
        }
    }

    @Override
    public void undo() {
        var mapData = context.getMapData();
        for (Map.Entry<TilePos, Boolean> entry : oldStates.entrySet()) {
            mapData[entry.getKey().x][entry.getKey().y].setBlocked(entry.getValue());
        }
    }

    @Override
    public int[] getAffectedBounds() {
        if (newStates.isEmpty())
            return null;
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (TilePos pos : newStates.keySet()) {
            if (pos.x() < minX)
                minX = pos.x();
            if (pos.x() > maxX)
                maxX = pos.x();
            if (pos.y() < minY)
                minY = pos.y();
            if (pos.y() > maxY)
                maxY = pos.y();
        }
        return new int[] { minX, minY, maxX, maxY };
    }
}
