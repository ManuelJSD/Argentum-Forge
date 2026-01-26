package org.argentumforge.engine.utils.editor;

import static org.argentumforge.engine.utils.GameData.mapData;

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

    // Límites de borde para unión manual
    private static final int BORDER_RIGHT = 90;
    private static final int BORDER_LEFT = 11;
    private static final int BORDER_BOTTOM = 91;
    private static final int BORDER_TOP = 10;

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
    public void transfer_edit(int x, int y) {
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

                // Detectar en qué borde está el tile
                if (x >= BORDER_RIGHT) {
                    // Borde derecho -> destino en borde izquierdo
                    finalX = 12;
                    finalY = y;
                } else if (x <= BORDER_LEFT) {
                    // Borde izquierdo -> destino en borde derecho
                    finalX = 91;
                    finalY = y;
                }

                if (y >= BORDER_BOTTOM) {
                    // Borde inferior -> destino en borde superior
                    finalY = 11;
                    finalX = x;
                } else if (y <= BORDER_TOP) {
                    // Borde superior -> destino en borde inferior
                    finalY = 90;
                    finalX = x;
                }
            }

            // Aplicar traslado mediante comando (con soporte para undo/redo)
            int oldMap = mapData[x][y].getExitMap();
            int oldX = mapData[x][y].getExitX();
            int oldY = mapData[x][y].getExitY();

            if (oldMap != finalMap || oldX != finalX || oldY != finalY) {
                org.argentumforge.engine.utils.editor.commands.CommandManager.getInstance().executeCommand(
                        new org.argentumforge.engine.utils.editor.commands.TransferChangeCommand(x, y,
                                oldMap, oldX, oldY,
                                finalMap, finalX, finalY));
            }
        } else {
            // Modo quitar
            int oldMap = mapData[x][y].getExitMap();
            int oldX = mapData[x][y].getExitX();
            int oldY = mapData[x][y].getExitY();

            if (oldMap != 0) {
                org.argentumforge.engine.utils.editor.commands.CommandManager.getInstance().executeCommand(
                        new org.argentumforge.engine.utils.editor.commands.TransferChangeCommand(x, y,
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
    public void autoUnionBorders(int north, int south, int east, int west) {
        if (mapData == null)
            return;

        boolean changed = false;

        // Norte (Arriba) -> Destino: Sur del mapa norte (y=90)
        if (north >= 0) {
            for (int x = 1; x <= 100; x++) {
                for (int y = 1; y <= BORDER_TOP; y++) {
                    applyAutoTransfer(x, y, north, (north == 0 ? 0 : x), (north == 0 ? 0 : 90));
                    changed = true;
                }
            }
        }

        // Sur (Abajo) -> Destino: Norte del mapa sur (y=11)
        if (south >= 0) {
            for (int x = 1; x <= 100; x++) {
                for (int y = BORDER_BOTTOM; y <= 100; y++) {
                    applyAutoTransfer(x, y, south, (south == 0 ? 0 : x), (south == 0 ? 0 : 11));
                    changed = true;
                }
            }
        }

        // Este (Derecha) -> Destino: Oeste del mapa este (x=12)
        if (east >= 0) {
            for (int x = BORDER_RIGHT; x <= 100; x++) {
                for (int y = 1; y <= 100; y++) {
                    applyAutoTransfer(x, y, east, (east == 0 ? 0 : 12), (east == 0 ? 0 : y));
                    changed = true;
                }
            }
        }

        // Oeste (Izquierda) -> Destino: Este del mapa oeste (x=91)
        if (west >= 0) {
            for (int x = 1; x <= BORDER_LEFT; x++) {
                for (int y = 1; y <= 100; y++) {
                    applyAutoTransfer(x, y, west, (west == 0 ? 0 : 91), (west == 0 ? 0 : y));
                    changed = true;
                }
            }
        }

        if (changed) {
            org.argentumforge.engine.game.console.Console.INSTANCE.addMsgToConsole(
                    "Unión automática completada.",
                    org.argentumforge.engine.game.console.FontStyle.BOLD,
                    new org.argentumforge.engine.renderer.RGBColor(0, 1, 0));
        }
    }

    /**
     * Aplica un traslado automáticamente si es diferente al actual.
     */
    private void applyAutoTransfer(int x, int y, int destMap, int destX, int destY) {
        int oldMap = mapData[x][y].getExitMap();
        int oldX = mapData[x][y].getExitX();
        int oldY = mapData[x][y].getExitY();

        if (oldMap != destMap || oldX != destX || oldY != destY) {
            org.argentumforge.engine.utils.editor.commands.CommandManager.getInstance().executeCommand(
                    new org.argentumforge.engine.utils.editor.commands.TransferChangeCommand(x, y,
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
