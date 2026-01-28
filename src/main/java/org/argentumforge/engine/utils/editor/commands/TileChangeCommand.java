package org.argentumforge.engine.utils.editor.commands;

import org.argentumforge.engine.i18n.I18n;
import org.argentumforge.engine.utils.inits.GrhInfo;
import static org.argentumforge.engine.utils.GameData.mapData;
import static org.argentumforge.engine.utils.GameData.initGrh;

/**
 * Comando para registrar y revertir cambios en las capas de superficie (1-4).
 */
public class TileChangeCommand implements Command {
    @Override
    public String getName() {
        return I18n.INSTANCE.get("history.command.tile");
    }

    private final int x, y, layer;
    private final short oldGrhIndex;
    private final short newGrhIndex;

    public TileChangeCommand(int x, int y, int layer, short oldGrhIndex, short newGrhIndex) {
        this.x = x;
        this.y = y;
        this.layer = layer;
        this.oldGrhIndex = oldGrhIndex;
        this.newGrhIndex = newGrhIndex;
    }

    @Override
    public void execute() {
        apply(newGrhIndex);
    }

    @Override
    public void undo() {
        apply(oldGrhIndex);
    }

    private void apply(short grhIndex) {
        GrhInfo layerGrh = mapData[x][y].getLayer(layer);
        layerGrh.setGrhIndex(grhIndex);
        initGrh(layerGrh, grhIndex, true);
    }

    @Override
    public int[] getAffectedBounds() {
        return new int[] { x, y, x, y };
    }
}
