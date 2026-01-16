package org.argentumforge.engine.utils.editor;

import static org.argentumforge.engine.utils.GameData.mapData;

public class Trigger {

    public enum BrushShape {
        SQUARE, CIRCLE
    }

    private static Trigger instance;

    // Herramienta
    private boolean isActive;
    // 0: seleccionar, 1: pintar
    private int mode;
    private int selectedTriggerId = 0;

    private int brushSize = 1;
    private BrushShape brushShape = BrushShape.SQUARE;

    private Trigger() {
    }

    public static Trigger getInstance() {
        if (instance == null) {
            instance = new Trigger();
        }
        return instance;
    }

    public void trigger_edit(int x, int y) {
        if (!isActive || mapData == null)
            return;

        int offset = brushSize / 2;
        double radiusSq = Math.pow(brushSize / 2.0, 2);

        for (int i = x - offset; i <= x + offset; i++) {
            for (int j = y - offset; j <= y + offset; j++) {
                // Validar limites
                if (i >= 0 && i < mapData.length && j >= 0 && j < mapData[0].length) {

                    if (brushShape == BrushShape.CIRCLE) {
                        double dx = i - x;
                        double dy = j - y;
                        if (dx * dx + dy * dy > radiusSq)
                            continue;
                    }

                    // Aplicar trigger mediante comando (soporta undo/redo y dirty flag)
                    short oldTrigger = (short) mapData[i][j].getTrigger();
                    if (oldTrigger != (short) selectedTriggerId) {
                        org.argentumforge.engine.utils.editor.commands.CommandManager.getInstance().executeCommand(
                                new org.argentumforge.engine.utils.editor.commands.TriggerChangeCommand(i, j,
                                        oldTrigger, (short) selectedTriggerId));
                    }
                }
            }
        }
    }

    public void setActive(boolean active) {
        this.isActive = active;
        if (!active) {
            selectedTriggerId = 0;
        }
    }

    public boolean isActive() {
        return isActive;
    }

    public void setSelectedTriggerId(int id) {
        this.selectedTriggerId = id;
    }

    public int getSelectedTriggerId() {
        return selectedTriggerId;
    }

    public int getBrushSize() {
        return brushSize;
    }

    public void setBrushSize(int brushSize) {
        this.brushSize = brushSize;
    }

    public BrushShape getBrushShape() {
        return brushShape;
    }

    public void setBrushShape(BrushShape brushShape) {
        this.brushShape = brushShape;
    }
}
