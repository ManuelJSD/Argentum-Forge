package org.argentumforge.engine.utils.editor;

import static org.argentumforge.engine.utils.GameData.mapData;

/**
 * Gestor de estado para la herramienta de selección y movimiento.
 */
public class Selection {

    public enum EntityType {
        NONE, NPC, OBJECT
    }

    private static volatile Selection instance;
    private static final Object lock = new Object();

    private boolean active = false;
    private boolean dragging = false;
    private int sourceX, sourceY;
    private EntityType selectedEntityType = EntityType.NONE;
    private int selectedId; // npcIndex o GrhIndex del objeto

    private Selection() {
    }

    public static Selection getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new Selection();
                }
            }
        }
        return instance;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        if (!active) {
            cancelDrag();
        }
    }

    public boolean isDragging() {
        return dragging;
    }

    public int getSourceX() {
        return sourceX;
    }

    public int getSourceY() {
        return sourceY;
    }

    public EntityType getSelectedEntityType() {
        return selectedEntityType;
    }

    public int getSelectedId() {
        return selectedId;
    }

    /**
     * Intenta agarrar una entidad en la posición dada.
     */
    public boolean tryGrab(int x, int y) {
        if (mapData == null || x < 0 || x >= mapData.length || y < 0 || y >= mapData[0].length)
            return false;

        // Prioridad NPC > Objeto
        if (mapData[x][y].getNpcIndex() > 0) {
            selectedEntityType = EntityType.NPC;
            selectedId = mapData[x][y].getNpcIndex();
            sourceX = x;
            sourceY = y;
            dragging = true;
            return true;
        } else if (mapData[x][y].getObjGrh().getGrhIndex() > 0) {
            selectedEntityType = EntityType.OBJECT;
            selectedId = mapData[x][y].getObjGrh().getGrhIndex();
            sourceX = x;
            sourceY = y;
            dragging = true;
            return true;
        }

        return false;
    }

    public void cancelDrag() {
        dragging = false;
        selectedEntityType = EntityType.NONE;
        selectedId = 0;
    }

    public void finalizeMove(int destX, int destY) {
        if (!dragging || selectedEntityType == EntityType.NONE)
            return;

        // Si es el mismo lugar, ignorar
        if (sourceX == destX && sourceY == destY) {
            cancelDrag();
            return;
        }

        // Ejecutar comando de movimiento
        org.argentumforge.engine.utils.editor.commands.CommandManager.getInstance().executeCommand(
                new org.argentumforge.engine.utils.editor.commands.MoveEntityCommand(
                        sourceX, sourceY, destX, destY, selectedEntityType, selectedId));

        cancelDrag();
    }
}
