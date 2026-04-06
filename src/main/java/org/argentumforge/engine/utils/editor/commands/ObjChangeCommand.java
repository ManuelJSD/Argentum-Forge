package org.argentumforge.engine.utils.editor.commands;

import org.argentumforge.engine.i18n.I18n;
import static org.argentumforge.engine.utils.GameData.initGrh;

/**
 * Comando para colocar o quitar objetos decorativos.
 */
public class ObjChangeCommand extends AbstractCommand {
    @Override
    public String getName() {
        return I18n.INSTANCE.get("history.command.obj");
    }

    private final int x, y;
    private final int oldObjGrh, newObjGrh;
    private final int oldObjIndex, newObjIndex;
    private final int oldObjAmount, newObjAmount;

    public ObjChangeCommand(org.argentumforge.engine.utils.MapContext context, int x, int y,
            int oldObjGrh, int newObjGrh,
            int oldObjIndex, int newObjIndex,
            int oldObjAmount, int newObjAmount) {
        super(context);
        this.x = x;
        this.y = y;
        this.oldObjGrh = oldObjGrh;
        this.newObjGrh = newObjGrh;
        this.oldObjIndex = oldObjIndex;
        this.newObjIndex = newObjIndex;
        this.oldObjAmount = oldObjAmount;
        this.newObjAmount = newObjAmount;
    }

    @Override
    public void execute() {
        apply(newObjGrh, newObjIndex, newObjAmount);
    }

    @Override
    public void undo() {
        apply(oldObjGrh, oldObjIndex, oldObjAmount);
    }

    private void apply(int grhIndex, int objIndex, int objAmount) {
        var mapData = context.getMapData();
        mapData[x][y].setObjIndex(objIndex);
        mapData[x][y].setObjAmount(objAmount);
        mapData[x][y].getObjGrh().setGrhIndex(0);
        if (grhIndex > 0) {
            initGrh(mapData[x][y].getObjGrh(), grhIndex, false);
        }
    }
}
