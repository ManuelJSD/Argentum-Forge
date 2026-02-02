package org.argentumforge.engine.utils.editor;

import java.util.ArrayList;
import java.util.List;
import org.argentumforge.engine.utils.GameData;
import org.argentumforge.engine.utils.inits.MapData;
// import static org.argentumforge.engine.utils.GameData.mapData; // Removed static import

/**
 * Gestor de estado para la herramienta de selección y movimiento.
 * Soporta selección múltiple y rectangular (marquee).
 */
public class Selection {

    public enum EntityType {
        NONE, NPC, OBJECT, TILE
    }

    /**
     * Representa una entidad seleccionada individualmente.
     */
    public static class SelectedEntity {
        public EntityType type;
        public int id;
        public int x, y;
        public int offsetX, offsetY; // Desplazamiento relativo al punto de agarre durante el arrastre

        public SelectedEntity(EntityType type, int id, int x, int y) {
            this.type = type;
            this.id = id;
            this.x = x;
            this.y = y;
        }
    }

    private static volatile Selection instance;
    private static final Object lock = new Object();

    private boolean active = false;
    private boolean dragging = false;
    private boolean areaSelecting = false;

    private int marqueeStartX, marqueeStartY;
    private int marqueeEndX, marqueeEndY;

    private int inspectedTileX = -1;
    private int inspectedTileY = -1;

    private final List<SelectedEntity> selectedEntities = new ArrayList<>();

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
            selectedEntities.clear();
            inspectedTileX = -1;
            inspectedTileY = -1;
        }
    }

    public boolean isDragging() {
        return dragging;
    }

    public boolean isAreaSelecting() {
        return areaSelecting;
    }

    public List<SelectedEntity> getSelectedEntities() {
        return selectedEntities;
    }

    public int getMarqueeStartX() {
        return marqueeStartX;
    }

    public int getMarqueeStartY() {
        return marqueeStartY;
    }

    public int getMarqueeEndX() {
        return marqueeEndX;
    }

    public int getMarqueeEndY() {
        return marqueeEndY;
    }

    public int getInspectedTileX() {
        return inspectedTileX;
    }

    public int getInspectedTileY() {
        return inspectedTileY;
    }

    /**
     * Intenta agarrar una entidad o iniciar selección de área.
     * 
     * @param x           Coordenada X del tile
     * @param y           Coordenada Y del tile
     * @param ctrlPressed Si se está pulsando Ctrl (para añadir a selección)
     */
    public void tryGrab(int x, int y, boolean multiSelectPressed) {
        var context = GameData.getActiveContext();
        if (context == null || context.getMapData() == null)
            return;
        var mapData = context.getMapData();

        if (x < 0 || x >= mapData.length || y < 0 || y >= mapData[0].length)
            return;

        inspectedTileX = x;
        inspectedTileY = y;

        // ¿Hemos pinchado en algo ya seleccionado?
        SelectedEntity target = findSelectedAt(x, y);

        if (target != null) {
            if (multiSelectPressed) {
                // Toggle: Si ya está seleccionado y pulsamos Shift, lo deseleccionamos
                selectedEntities.remove(target);
            } else {
                // Iniciar arrastre de todo el grupo
                startDrag(x, y);
            }
        } else {
            // Intentar seleccionar algo nuevo
            SelectedEntity newEntity = getAt(mapData, x, y);
            if (newEntity != null) {
                if (!multiSelectPressed)
                    selectedEntities.clear();
                selectedEntities.add(newEntity);
                startDrag(x, y);
            } else {
                // Pinchar en vacío (o tile si no hay nada más y no es multiSelect)
                if (!multiSelectPressed) {
                    selectedEntities.clear();
                    // Seleccionar el tile como entidad base
                    SelectedEntity tile = new SelectedEntity(EntityType.TILE, 0, x, y);
                    selectedEntities.add(tile);
                    startDrag(x, y);
                } else {
                    // Iniciar selección rectángular
                    startAreaSelect(x, y);
                }
            }
        }

    }

    private SelectedEntity findSelectedAt(int x, int y) {
        for (SelectedEntity se : selectedEntities) {
            if (se.x == x && se.y == y)
                return se;
        }
        return null;
    }

    private SelectedEntity getAt(MapData[][] mapData, int x, int y) {
        if (mapData[x][y].getNpcIndex() > 0) {
            return new SelectedEntity(EntityType.NPC, mapData[x][y].getNpcIndex(), x, y);
        } else if (mapData[x][y].getObjGrh().getGrhIndex() > 0) {
            return new SelectedEntity(EntityType.OBJECT, mapData[x][y].getObjGrh().getGrhIndex(), x, y);
        } else {
            return null;
        }
    }

    private void startDrag(int anchorX, int anchorY) {
        dragging = true;
        for (SelectedEntity se : selectedEntities) {
            se.offsetX = se.x - anchorX;
            se.offsetY = se.y - anchorY;
        }
    }

    public void startAreaSelect(int x, int y) {
        areaSelecting = true;
        marqueeStartX = x;
        marqueeStartY = y;
        marqueeEndX = x;
        marqueeEndY = y;
    }

    public void updateAreaSelect(int x, int y) {
        marqueeEndX = x;
        marqueeEndY = y;
    }

    public void finalizeAreaSelect() {
        int x1 = Math.min(marqueeStartX, marqueeEndX);
        int x2 = Math.max(marqueeStartX, marqueeEndX);
        int y1 = Math.min(marqueeStartY, marqueeEndY);
        int y2 = Math.max(marqueeStartY, marqueeEndY);

        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                // En selección de área capturamos TODO lo que haya en el tile (prioridad
                // NPC/Obj, pero el Tile en sí también se cuenta)
                // Para simplificar, en área seleccionamos el Tile que contiene todo.
                if (findSelectedAt(x, y) == null) {
                    selectedEntities.add(new SelectedEntity(EntityType.TILE, 0, x, y));

                    // Si además hay NPC u Objeto, los añadimos también para que se muevan/borren
                    // correctamente?
                    // En realidad, si seleccionamos el TILE, el BulkMove debería mover TODO el
                    // contenido del tile.
                    // Vamos a simplificar: Marquee selecciona TILES.
                }
            }
        }
        areaSelecting = false;
    }

    public void cancelDrag() {
        dragging = false;
        areaSelecting = false;
    }

    public void finalizeMove(int destX, int destY) {
        if (!dragging || selectedEntities.isEmpty()) {
            cancelDrag();
            return;
        }

        // Ejecutar comando de movimiento masivo
        org.argentumforge.engine.utils.editor.commands.CommandManager.getInstance().executeCommand(
                new org.argentumforge.engine.utils.editor.commands.BulkEntityMoveCommand(
                        org.argentumforge.engine.utils.GameData.getActiveContext(), selectedEntities, destX,
                        destY));

        cancelDrag();
    }
}
