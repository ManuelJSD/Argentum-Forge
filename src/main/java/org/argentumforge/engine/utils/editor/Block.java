package org.argentumforge.engine.utils.editor;

import static org.argentumforge.engine.utils.GameData.mapData;

/**
 * Clase singleton para gestionar la edición de bloqueos en el mapa.
 * Permite activar/desactivar bloqueos en tiles específicos del mapa.
 */
public class Block {

    private static volatile Block instance;
    private static final Object lock = new Object();

    public enum BrushShape {
        SQUARE, CIRCLE
    }

    private int mode; // 0 = ninguno, 1 = bloquear, 2 = desbloquear, 3 = invertir
    private int brushSize = 1; // 1, 3, 5...
    private BrushShape brushShape = BrushShape.SQUARE;

    private Block() {
        this.mode = 0;
    }

    public static Block getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new Block();
                }
            }
        }
        return instance;
    }

    // Para testing
    public static void resetInstance() {
        synchronized (lock) {
            instance = null;
        }
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
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

    /**
     * Edita el estado de bloqueo de un tile en las coordenadas especificadas.
     * 
     * @param x coordenada X del tile
     * @param y coordenada Y del tile
     */
    public void block_edit(int x, int y) {
        if (mode <= 0 || mapData == null)
            return;

        java.util.Map<org.argentumforge.engine.utils.editor.commands.BlockChangeCommand.TilePos, Boolean> oldStates = new java.util.HashMap<>();
        java.util.Map<org.argentumforge.engine.utils.editor.commands.BlockChangeCommand.TilePos, Boolean> newStates = new java.util.HashMap<>();

        int offset = brushSize / 2;
        double radiusSq = Math.pow(brushSize / 2.0, 2);

        for (int i = x - offset; i <= x + offset; i++) {
            for (int j = y - offset; j <= y + offset; j++) {
                if (i >= 0 && i < mapData.length && j >= 0 && j < mapData[0].length && mapData[i][j] != null) {

                    if (brushShape == BrushShape.CIRCLE) {
                        double dx = i - x;
                        double dy = j - y;
                        if (dx * dx + dy * dy > radiusSq)
                            continue;
                    }
                    boolean current = mapData[i][j].getBlocked();
                    boolean next = current;

                    if (mode == 1) {
                        next = true;
                    } else if (mode == 2) {
                        next = false;
                    } else if (mode == 3) {
                        next = !current;
                    }

                    if (next != current) {
                        org.argentumforge.engine.utils.editor.commands.BlockChangeCommand.TilePos pos = new org.argentumforge.engine.utils.editor.commands.BlockChangeCommand.TilePos(
                                i, j);
                        oldStates.put(pos, current);
                        newStates.put(pos, next);
                    }
                }
            }
        }

        if (!oldStates.isEmpty()) {
            org.argentumforge.engine.utils.editor.commands.CommandManager.getInstance().executeCommand(
                    new org.argentumforge.engine.utils.editor.commands.BlockChangeCommand(
                            org.argentumforge.engine.utils.GameData.getActiveContext(), oldStates, newStates));
        }
    }

    /**
     * Bloquea los bordes del mapa según la lógica original de VB6.
     * Basado en un área de visión estándar de 13x11 (o similar configurable).
     */
    public void blockBorders() {
        applyGlobalAction(true, true);
    }

    /**
     * Limpia los bloqueos de los bordes del mapa.
     */
    public void unblockBorders() {
        applyGlobalAction(false, true);
    }

    /**
     * Bloquea todos los tiles del mapa.
     */
    public void blockAll() {
        applyGlobalAction(true, false);
    }

    /**
     * Desbloquea todos los tiles del mapa.
     */
    public void unblockAll() {
        applyGlobalAction(false, false);
    }

    private void applyGlobalAction(boolean targetState, boolean bordersOnly) {
        if (mapData == null)
            return;

        java.util.Map<org.argentumforge.engine.utils.editor.commands.BlockChangeCommand.TilePos, Boolean> oldStates = new java.util.HashMap<>();
        java.util.Map<org.argentumforge.engine.utils.editor.commands.BlockChangeCommand.TilePos, Boolean> newStates = new java.util.HashMap<>();

        int clientWidth = org.argentumforge.engine.utils.GameData.options.getClientWidth();
        int clientHeight = org.argentumforge.engine.utils.GameData.options.getClientHeight();

        int minXBorder = clientWidth / 2;
        int maxXBorder = mapData.length - (clientWidth / 2) - 1;
        int minYBorder = clientHeight / 2;
        int maxYBorder = mapData[0].length - (clientHeight / 2) - 1;

        for (int x = 0; x < mapData.length; x++) {
            for (int y = 0; y < mapData[0].length; y++) {
                if (mapData[x][y] != null) {
                    boolean isBorder = x <= minXBorder || x >= maxXBorder || y <= minYBorder || y >= maxYBorder;
                    if (!bordersOnly || isBorder) {
                        boolean current = mapData[x][y].getBlocked();
                        if (current != targetState) {
                            org.argentumforge.engine.utils.editor.commands.BlockChangeCommand.TilePos pos = new org.argentumforge.engine.utils.editor.commands.BlockChangeCommand.TilePos(
                                    x, y);
                            oldStates.put(pos, current);
                            newStates.put(pos, targetState);
                        }
                    }
                }
            }
        }

        if (!oldStates.isEmpty()) {
            org.argentumforge.engine.utils.editor.commands.CommandManager.getInstance().executeCommand(
                    new org.argentumforge.engine.utils.editor.commands.BlockChangeCommand(
                            org.argentumforge.engine.utils.GameData.getActiveContext(), oldStates, newStates));
        }
    }
}
