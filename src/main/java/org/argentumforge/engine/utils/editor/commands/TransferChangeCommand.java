package org.argentumforge.engine.utils.editor.commands;

import static org.argentumforge.engine.utils.GameData.mapData;

/**
 * Comando para registrar y revertir cambios en los traslados de un tile.
 */
public class TransferChangeCommand implements Command {
    private final int x, y;
    private final short oldExitMap, oldExitX, oldExitY;
    private final short newExitMap, newExitX, newExitY;

    public TransferChangeCommand(int x, int y,
            short oldExitMap, short oldExitX, short oldExitY,
            short newExitMap, short newExitX, short newExitY) {
        this.x = x;
        this.y = y;
        this.oldExitMap = oldExitMap;
        this.oldExitX = oldExitX;
        this.oldExitY = oldExitY;
        this.newExitMap = newExitMap;
        this.newExitX = newExitX;
        this.newExitY = newExitY;
    }

    @Override
    public void execute() {
        apply(newExitMap, newExitX, newExitY);
    }

    @Override
    public void undo() {
        apply(oldExitMap, oldExitX, oldExitY);
    }

    private void apply(short exitMap, short exitX, short exitY) {
        mapData[x][y].setExitMap(exitMap);
        mapData[x][y].setExitX(exitX);
        mapData[x][y].setExitY(exitY);
    }
}
