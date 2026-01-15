package org.argentumforge.engine.utils.editor.commands;

import static org.argentumforge.engine.utils.GameData.mapData;

/**
 * Comando para registrar y revertir cambios en los triggers de un tile.
 */
public class TriggerChangeCommand implements Command {
    private final int x, y;
    private final short oldTrigger;
    private final short newTrigger;

    public TriggerChangeCommand(int x, int y, short oldTrigger, short newTrigger) {
        this.x = x;
        this.y = y;
        this.oldTrigger = oldTrigger;
        this.newTrigger = newTrigger;
    }

    @Override
    public void execute() {
        mapData[x][y].setTrigger(newTrigger);
    }

    @Override
    public void undo() {
        mapData[x][y].setTrigger(oldTrigger);
    }
}
