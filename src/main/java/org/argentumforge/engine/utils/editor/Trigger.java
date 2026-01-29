package org.argentumforge.engine.utils.editor;

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

    public void trigger_edit(org.argentumforge.engine.utils.MapContext context, int x, int y) {
        if (context == null)
            return;
        var mapData = context.getMapData();
        if (!isActive || mapData == null)
            return;

        java.util.Map<org.argentumforge.engine.utils.editor.commands.TriggerChangeCommand.TilePos, Short> oldStates = new java.util.HashMap<>();
        java.util.Map<org.argentumforge.engine.utils.editor.commands.TriggerChangeCommand.TilePos, Short> newStates = new java.util.HashMap<>();

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
                        org.argentumforge.engine.utils.editor.commands.TriggerChangeCommand.TilePos pos = new org.argentumforge.engine.utils.editor.commands.TriggerChangeCommand.TilePos(
                                i, j);
                        oldStates.put(pos, oldTrigger);
                        newStates.put(pos, (short) selectedTriggerId);
                    }
                }
            }
        }

        if (!oldStates.isEmpty()) {
            org.argentumforge.engine.utils.editor.commands.CommandManager.getInstance().executeCommand(
                    new org.argentumforge.engine.utils.editor.commands.TriggerChangeCommand(
                            context, oldStates, newStates));
        }
    }

    public void setActive(boolean active) {
        this.isActive = active;
        if (!active) {
            selectedTriggerId = 0;
            this.mode = 0;
        }
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
        if (mode > 0) {
            this.isActive = true;
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
