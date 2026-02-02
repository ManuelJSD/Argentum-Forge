package org.argentumforge.engine.utils.editor.commands;

import org.argentumforge.engine.i18n.I18n;

/**
 * Comando para registrar y revertir cambios en la cantidad de un objeto en un
 * tile.
 */
public class ObjAmountChangeCommand extends AbstractCommand {
    @Override
    public String getName() {
        return I18n.INSTANCE.get("history.command.obj"); // Reutilizamos la etiqueta de objeto
    }

    private final int x, y;
    private final int oldAmount;
    private final int newAmount;

    public ObjAmountChangeCommand(org.argentumforge.engine.utils.MapContext context, int x, int y, int oldAmount,
            int newAmount) {
        super(context);
        this.x = x;
        this.y = y;
        this.oldAmount = oldAmount;
        this.newAmount = newAmount;
    }

    @Override
    public void execute() {
        context.getMapData()[x][y].setObjAmount(newAmount);
    }

    @Override
    public void undo() {
        context.getMapData()[x][y].setObjAmount(oldAmount);
    }
}
