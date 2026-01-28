package org.argentumforge.engine.utils.editor.commands;

import org.argentumforge.engine.i18n.I18n;
import static org.argentumforge.engine.utils.GameData.initGrh;
import static org.argentumforge.engine.utils.GameData.mapData;

/**
 * Comando para colocar o quitar objetos decorativos.
 */
public class ObjChangeCommand implements Command {
    @Override
    public String getName() {
        return I18n.INSTANCE.get("history.command.obj");
    }

    private final int x, y;
    private final int oldObjGrh;
    private final int newObjGrh;

    public ObjChangeCommand(int x, int y, int oldObjGrh, int newObjGrh) {
        this.x = x;
        this.y = y;
        this.oldObjGrh = oldObjGrh;
        this.newObjGrh = newObjGrh;
    }

    @Override
    public void execute() {
        apply(newObjGrh);
    }

    @Override
    public void undo() {
        apply(oldObjGrh);
    }

    private void apply(int grhIndex) {
        mapData[x][y].getObjGrh().setGrhIndex(0);
        if (grhIndex > 0) {
            initGrh(mapData[x][y].getObjGrh(), (short) grhIndex, false);
        }
    }
}
