package org.argentumforge.engine.utils.editor.commands;

import org.argentumforge.engine.i18n.I18n;
import org.argentumforge.engine.utils.GameData;
import static org.argentumforge.engine.utils.GameData.initGrh;

/**
 * Comando para cambiar todas las capas de un tile (superficie).
 */
public class SurfaceChangeCommand implements Command {
    @Override
    public String getName() {
        return I18n.INSTANCE.get("history.command.surface");
    }

    private final int x, y;
    private final int[] oldLayers;
    private final int[] newLayers;

    public SurfaceChangeCommand(int x, int y, int[] oldLayers, int[] newLayers) {
        this.x = x;
        this.y = y;
        this.oldLayers = oldLayers;
        this.newLayers = newLayers;
    }

    @Override
    public void execute() {
        applyLayers(newLayers);
    }

    @Override
    public void undo() {
        applyLayers(oldLayers);
    }

    private void applyLayers(int[] layers) {
        if (layers == null)
            return;
        for (int i = 1; i <= 4; i++) {
            GameData.mapData[x][y].getLayer(i).setGrhIndex(layers[i]);
            initGrh(GameData.mapData[x][y].getLayer(i), layers[i], true);
        }
    }
}
