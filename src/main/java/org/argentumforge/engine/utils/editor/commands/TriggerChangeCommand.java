package org.argentumforge.engine.utils.editor.commands;

import org.argentumforge.engine.i18n.I18n;
import java.util.Map;

/**
 * Comando para registrar y revertir cambios en los triggers de múltiples tiles.
 */
public class TriggerChangeCommand extends AbstractCommand {
    @Override
    public String getName() {
        return I18n.INSTANCE.get("history.command.trigger");
    }

    private final Map<TilePos, Short> oldStates;
    private final Map<TilePos, Short> newStates;

    public static record TilePos(int x, int y) {
    }

    public TriggerChangeCommand(org.argentumforge.engine.utils.MapContext context, Map<TilePos, Short> oldStates,
            Map<TilePos, Short> newStates) {
        super(context);
        this.oldStates = oldStates;
        this.newStates = newStates;
    }

    @Override
    public void execute() {
        var mapData = context.getMapData();
        for (Map.Entry<TilePos, Short> entry : newStates.entrySet()) {
            mapData[entry.getKey().x][entry.getKey().y].setTrigger(entry.getValue());
        }
    }

    @Override
    public void undo() {
        var mapData = context.getMapData();
        for (Map.Entry<TilePos, Short> entry : oldStates.entrySet()) {
            mapData[entry.getKey().x][entry.getKey().y].setTrigger(entry.getValue());
        }
    }
}
