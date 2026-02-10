package org.argentumforge.engine.utils.editor;

import org.argentumforge.engine.utils.MapContext;
import org.argentumforge.engine.utils.editor.commands.BlockChangeCommand;
import org.argentumforge.engine.utils.editor.commands.CommandManager;
import org.argentumforge.engine.utils.GameData;
import org.argentumforge.engine.game.console.Console;
import org.argentumforge.engine.game.console.FontStyle;
import org.argentumforge.engine.renderer.RGBColor;
import java.util.HashMap;
import java.util.Map;

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
    public void block_edit(MapContext context, int x, int y) {
        if (mode <= 0 || context == null)
            return;

        var mapData = context.getMapData();
        if (mapData == null)
            return;

        Map<BlockChangeCommand.TilePos, Boolean> oldStates = new HashMap<>();
        Map<BlockChangeCommand.TilePos, Boolean> newStates = new HashMap<>();

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
                        BlockChangeCommand.TilePos pos = new BlockChangeCommand.TilePos(
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
                            context, oldStates, newStates));
        }
    }

    /**
     * Bloquea los bordes del mapa según la lógica original de VB6.
     * Basado en un área de visión estándar de 13x11 (o similar configurable).
     */
    public void blockBorders(MapContext context) {
        applyGlobalAction(context, true, true);
    }

    /**
     * Limpia los bloqueos de los bordes del mapa.
     */
    public void unblockBorders(MapContext context) {
        applyGlobalAction(context, false, true);
    }

    /**
     * Bloquea todos los tiles del mapa.
     */
    public void blockAll(MapContext context) {
        applyGlobalAction(context, true, false);
    }

    /**
     * Desbloquea todos los tiles del mapa.
     */
    public void unblockAll(MapContext context) {
        applyGlobalAction(context, false, false);
    }

    private void applyGlobalAction(MapContext context, boolean targetState, boolean bordersOnly) {
        if (context == null)
            return;
        var mapData = context.getMapData();
        if (mapData == null)
            return;

        Map<BlockChangeCommand.TilePos, Boolean> oldStates = new HashMap<>();
        Map<BlockChangeCommand.TilePos, Boolean> newStates = new HashMap<>();

        int mapWidth = mapData.length;
        int mapHeight = mapData[0].length;

        int clientWidth = GameData.options.getClientWidth();
        int clientHeight = GameData.options.getClientHeight();

        // Debug Log
        Console.INSTANCE.addMsgToConsole(
                "DEBUG: Blocking Borders (Client: " + clientWidth + "x" + clientHeight + ") -> H:" + (clientWidth / 2)
                        + " V:" + (clientHeight / 2),
                FontStyle.ITALIC,
                new RGBColor(1, 1, 0));

        // Ajuste: Revertir offset manual y usar lógica exacta de VB6
        // VB6: MinXBorder = XMinMapSize + (ClienteWidth / 2) -> Block if X < MinXBorder
        // VB6: MaxXBorder = XMaxMapSize - (ClienteWidth / 2) -> Block if X > MaxXBorder

        // Suponiendo mapa 1..100
        // Ajuste Definitivo: Lógica basada en cantidad exacta de tiles (Cero-based)
        // Ejemplo Width=17 -> halfW=8. Queremos bloquear 8 tiles exactos.
        // Indices: 0, 1, 2, 3, 4, 5, 6, 7. (Total 8).
        // Condición: x < halfW.

        // Derecha: Queremos bloquear 8 tiles exactos desde el final.
        // Array Length 100 (0..99).
        // Indices: 92, 93, 94, 95, 96, 97, 98, 99. (Total 8).
        // Condición: x >= (mapWidth - halfW).

        int halfW = clientWidth / 2;
        int halfH = clientHeight / 2;

        int minXBorder = halfW;
        int maxXBorder = mapWidth - 1 - halfW;

        int minYBorder = halfH;
        int maxYBorder = mapHeight - 1 - halfH;

        for (int x = 0; x < mapData.length; x++) {
            for (int y = 0; y < mapData[0].length; y++) {
                if (mapData[x][y] != null) {
                    // Condición corregida para 1-based rendering: <= y >=
                    // Ejemplo Leff: x <= 8 (1..8) -> 8 tiles.
                    boolean isBorder = x <= minXBorder || x >= maxXBorder || y <= minYBorder || y >= maxYBorder;
                    if (!bordersOnly || isBorder) {
                        boolean current = mapData[x][y].getBlocked();
                        if (current != targetState) {
                            BlockChangeCommand.TilePos pos = new BlockChangeCommand.TilePos(
                                    x, y);
                            oldStates.put(pos, current);
                            newStates.put(pos, targetState);
                        }
                    }
                }
            }
        }

        if (!oldStates.isEmpty()) {
            CommandManager.getInstance().executeCommand(
                    new BlockChangeCommand(
                            context, oldStates, newStates));
        }
    }
}
