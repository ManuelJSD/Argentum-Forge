package org.argentumforge.engine.utils.editor.commands;

import org.argentumforge.engine.i18n.I18n;

/**
 * Comando para registrar y revertir cambios en los traslados de un tile.
 */
public class TransferChangeCommand extends AbstractCommand {
    @Override
    public String getName() {
        return I18n.INSTANCE.get("history.command.transfer");
    }

    private final int x, y;
    private final int oldExitMap, oldExitX, oldExitY;
    private final int newExitMap, newExitX, newExitY;

    public TransferChangeCommand(org.argentumforge.engine.utils.MapContext context, int x, int y,
            int oldExitMap, int oldExitX, int oldExitY,
            int newExitMap, int newExitX, int newExitY) {
        super(context);
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

    private void apply(int exitMap, int exitX, int exitY) {
        var mapData = context.getMapData();
        mapData[x][y].setExitMap(exitMap);
        mapData[x][y].setExitX(exitX);
        mapData[x][y].setExitY(exitY);
    }
}
