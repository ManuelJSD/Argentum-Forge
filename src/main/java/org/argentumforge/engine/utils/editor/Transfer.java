package org.argentumforge.engine.utils.editor;

import org.argentumforge.engine.utils.MapContext;
import org.argentumforge.engine.utils.GameData;
import org.argentumforge.engine.utils.editor.commands.CommandManager;
import org.argentumforge.engine.utils.editor.commands.TransferChangeCommand;
import org.argentumforge.engine.game.console.Console;
import org.argentumforge.engine.game.console.FontStyle;
import org.argentumforge.engine.renderer.RGBColor;

/**
 * Gestor de estado para el editor de traslados (teleports).
 * <p>
 * Permite insertar y quitar traslados en el mapa, con soporte para
 * unión manual con mapas adyacentes y captura de coordenadas.
 */
public class Transfer {

    private static Transfer instance;

    // Estado de la herramienta
    private boolean isActive;

    // Modo: 0 = quitar, 1 = insertar
    private int mode;

    // Coordenadas de destino
    private int destinationMap;
    private int destinationX;
    private int destinationY;

    // Unión manual con mapas adyacentes
    private boolean manualUnion;

    // Constantes de límites del mapa
    private static final int MIN_MAP = 1;
    private static final int MAX_MAP = 9000;
    private static final int MIN_COORD = 1;
    private static final int MAX_COORD = 100;

    private Transfer() {
        this.isActive = false;
        this.mode = 0;
        this.destinationMap = 0;
        this.destinationX = 0;
        this.destinationY = 0;
        this.manualUnion = false;
    }

    public static Transfer getInstance() {
        if (instance == null) {
            instance = new Transfer();
        }
        return instance;
    }

    /**
     * Aplica o quita un traslado en la posición especificada.
     * 
     * @param x Coordenada X en el mapa
     * @param y Coordenada Y en el mapa
     */
    public void transfer_edit(MapContext context, int x, int y) {
        if (context == null)
            return;
        var mapData = context.getMapData();
        if (!isActive || mapData == null)
            return;

        // Validar límites del mapa
        if (x < 0 || x >= mapData.length || y < 0 || y >= mapData[0].length)
            return;

        if (mode == 1) {
            // Modo insertar
            if (!isValidDestination()) {
                System.err.println(
                        "Destino inválido: Mapa=" + destinationMap + " X=" + destinationX + " Y=" + destinationY);
                return;
            }

            int finalMap = destinationMap;
            int finalX = destinationX;
            int finalY = destinationY;

            // Si está activa la unión manual, calcular coordenadas automáticamente
            if (manualUnion) {
                finalX = destinationX;
                finalY = destinationY;

                // Get dynamic border sizes
                int halfWidth = GameData.options.getClientWidth() / 2;
                int halfHeight = GameData.options.getClientHeight() / 2;

                int borderRight = 100 - halfWidth + 1; // e.g. 100 - 8 + 1 = 93
                int borderLeft = halfWidth; // e.g. 8
                int borderBottom = 100 - halfHeight + 1; // e.g. 100 - 6 + 1 = 95
                int borderTop = halfHeight; // e.g. 6

                // Detectar en qué borde está el tile
                if (x >= borderRight) {
                    // Borde derecho -> destino en borde izquierdo
                    finalX = borderLeft + 1; // Logic check: if exit is at 93 (start of border), dest is 12?
                                             // VB6 original logic uses fixed 12 usually.
                                             // If we are at X=93, we go to MapE at X=?
                                             // If view is 17 wide (8 radius). X=93 is 8 tiles from edge.
                                             // Ideally we want user to appear at X=8??
                                             // Unhardcoding this logic implies dest is also dynamic?
                                             // Original code: if x >= BORDER_RIGHT(90) -> finalX = 12. (11+1)
                                             // So finalX should be borderLeft + 1.
                    finalX = borderLeft + 1;
                    finalY = y;
                } else if (x <= borderLeft) {
                    // Borde izquierdo -> destino en borde derecho (91)
                    // If borderLeft is 11, dest is 90?
                    // Original code: if x <= BORDER_LEFT(11) -> finalX = 91. (100 - 11 + 2?) or
                    // (100 - 9)?
                    // Logic: 100 - halfWidth.
                    // If halfWidth=11. 100-11 = 89?
                    // Let's stick to standard behavior:
                    // If I walk left into MapW, I appear at the right side of MapW.
                    // Right side start is borderRight.
                    // Original code used 91. BORDER_RIGHT was 90. So it put you at BORDER_RIGHT +
                    // 1.
                    finalX = borderRight;
                    finalY = y;
                }

                if (y >= borderBottom) {
                    // Borde inferior -> destino en borde superior
                    // Original: if y >= 91 -> finalY = 11. (BORDER_TOP + 1)
                    // BORDER_TOP was 10.
                    finalY = borderTop + 1;
                    finalX = x;
                } else if (y <= borderTop) {
                    // Borde superior -> destino en borde inferior
                    // Original: if y <= 10 -> finalY = 90.
                    // BORDER_BOTTOM was 91.
                    // So it puts you at BORDER_BOTTOM - 1.
                    finalY = borderBottom - 1;
                    finalX = x;
                }
            }

            // Aplicar traslado mediante comando (con soporte para undo/redo)
            int oldMap = mapData[x][y].getExitMap();
            int oldX = mapData[x][y].getExitX();
            int oldY = mapData[x][y].getExitY();

            if (oldMap != finalMap || oldX != finalX || oldY != finalY) {
                CommandManager.getInstance().executeCommand(
                        new TransferChangeCommand(
                                context, x, y,
                                oldMap, oldX, oldY,
                                finalMap, finalX, finalY));
            }
        } else {
            // Modo quitar
            int oldMap = mapData[x][y].getExitMap();
            int oldX = mapData[x][y].getExitX();
            int oldY = mapData[x][y].getExitY();

            if (oldMap != 0) {
                CommandManager.getInstance().executeCommand(
                        new TransferChangeCommand(
                                context, x, y,
                                oldMap, oldX, oldY,
                                0, 0, 0));
            }
        }
    }

    /**
     * Captura las coordenadas de un traslado existente.
     * 
     * @param map Número de mapa
     * @param x   Coordenada X
     * @param y   Coordenada Y
     */
    public void captureCoordinates(int map, int x, int y) {
        this.destinationMap = map;
        this.destinationX = x;
        this.destinationY = y;
    }

    /**
     * Une automáticamente los bordes del mapa actual con mapas adyacentes.
     * 
     * @param north Mapa al norte (>=0 para aplicar, -1 para ignorar, 0 para borrar)
     * @param south Mapa al sur
     * @param east  Mapa al este
     * @param west  Mapa al oeste
     */
    public void autoUnionBorders(MapContext context, int north, int south, int east,
            int west) {
        if (context == null)
            return;
        var mapData = context.getMapData();
        if (mapData == null)
            return;

        boolean changed = false;

        // Calcular límites exactos igual que Block.java para consistencia
        int clientWidth = GameData.options.getClientWidth();
        int clientHeight = GameData.options.getClientHeight();

        int mapWidth = mapData.length;
        int mapHeight = mapData[0].length;

        // Volver a lógica estándar sin +3 (VB6 Style)
        int halfW = clientWidth / 2;
        int halfH = clientHeight / 2;

        int minXBorder = halfW; // Bloquea <= 8
        int maxXBorder = mapWidth - 1 - halfW; // Bloquea >= 93

        int minYBorder = halfH;
        int maxYBorder = mapHeight - 1 - halfH;

        // Líneas de Trigger (Debe ser el primer tile NO bloqueado)
        // West Block <= 8 -> Trigger en 9.
        int triggerX_West = minXBorder + 1;

        // East Block >= 93 -> Trigger en 92.
        int triggerX_East = maxXBorder - 1;

        int triggerY_North = minYBorder + 1;
        int triggerY_South = maxYBorder - 1;

        // Destinos
        // Nota: Los destinos suelen ser fijos o simétricos, pero para coincidir
        // exactamente con la posición inversa:
        int destY_North = triggerY_South;
        int destY_South = triggerY_North;
        int destX_East = triggerX_West;
        int destX_West = triggerX_East;

        // Norte (Arriba) -> Trigger en triggerY_North
        if (north >= 0) {
            for (int x = 1; x < mapWidth; x++) {
                if (mapData[x][triggerY_North] != null && !mapData[x][triggerY_North].getBlocked()) {
                    applyAutoTransfer(context, x, triggerY_North, north, (north == 0 ? 0 : x),
                            (north == 0 ? 0 : destY_North));
                    changed = true;
                }
            }
        }

        // Sur (Abajo) -> Trigger en triggerY_South
        if (south >= 0) {
            for (int x = 1; x < mapWidth; x++) {
                if (mapData[x][triggerY_South] != null && !mapData[x][triggerY_South].getBlocked()) {
                    applyAutoTransfer(context, x, triggerY_South, south, (south == 0 ? 0 : x),
                            (south == 0 ? 0 : destY_South));
                    changed = true;
                }
            }
        }

        // Este (Derecha) -> Trigger en triggerX_East
        if (east >= 0) {
            for (int y = 1; y < mapHeight; y++) {
                if (mapData[triggerX_East][y] != null && !mapData[triggerX_East][y].getBlocked()) {
                    applyAutoTransfer(context, triggerX_East, y, east, (east == 0 ? 0 : destX_East),
                            (east == 0 ? 0 : y));
                    changed = true;
                }
            }
        }

        // Oeste (Izquierda) -> Trigger en triggerX_West
        if (west >= 0) {
            for (int y = 1; y < mapHeight; y++) {
                if (mapData[triggerX_West][y] != null && !mapData[triggerX_West][y].getBlocked()) {
                    applyAutoTransfer(context, triggerX_West, y, west, (west == 0 ? 0 : destX_West),
                            (west == 0 ? 0 : y));
                    changed = true;
                }
            }
        }

        if (changed) {
            Console.INSTANCE.addMsgToConsole(
                    "Unión automática completada.",
                    FontStyle.BOLD,
                    new RGBColor(0, 1, 0));
        }
    }

    /**
     * Aplica un traslado automáticamente si es diferente al actual.
     */
    private void applyAutoTransfer(MapContext context, int x, int y, int destMap,
            int destX, int destY) {
        if (context == null)
            return;
        var mapData = context.getMapData();
        int oldMap = mapData[x][y].getExitMap();
        int oldX = mapData[x][y].getExitX();
        int oldY = mapData[x][y].getExitY();

        if (oldMap != destMap || oldX != destX || oldY != destY) {
            CommandManager.getInstance().executeCommand(
                    new TransferChangeCommand(
                            context, x, y,
                            oldMap, oldX, oldY,
                            destMap, destX, destY));
        }
    }

    /**
     * Valida que las coordenadas de destino sean válidas.
     * 
     * @return true si son válidas, false en caso contrario
     */
    private boolean isValidDestination() {
        if (destinationMap < MIN_MAP || destinationMap > MAX_MAP)
            return false;
        if (destinationX < MIN_COORD || destinationX > MAX_COORD)
            return false;
        if (destinationY < MIN_COORD || destinationY > MAX_COORD)
            return false;
        return true;
    }

    // Getters y Setters

    public void setActive(boolean active) {
        this.isActive = active;
        if (!active) {
            mode = 0;
        }
    }

    public boolean isActive() {
        return isActive;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public int getMode() {
        return mode;
    }

    public void setDestination(int map, int x, int y) {
        this.destinationMap = map;
        this.destinationX = x;
        this.destinationY = y;
    }

    public int getDestinationMap() {
        return destinationMap;
    }

    public int getDestinationX() {
        return destinationX;
    }

    public int getDestinationY() {
        return destinationY;
    }

    public void setManualUnion(boolean manualUnion) {
        this.manualUnion = manualUnion;
    }

    public boolean isManualUnion() {
        return manualUnion;
    }
}
