package org.argentumforge.engine.utils.editor;

import java.util.ArrayList;
import java.util.List;
import org.argentumforge.engine.utils.GameData;

/**
 * Servicio de portapapeles para copiar, cortar y pegar entidades.
 */
public class Clipboard {

    public static class PasteSettings {
        public boolean[] layers = { true, true, true, true };
        public boolean blocked = true;
        public boolean npc = true;
        public boolean objects = true;
        public boolean triggers = true;
        public boolean transitions = true;
        public boolean particles = true;

        public void reset() {
            for (int i = 0; i < layers.length; i++)
                layers[i] = true;
            blocked = npc = objects = triggers = transitions = particles = true;
        }
    }

    private final PasteSettings settings = new PasteSettings();

    public PasteSettings getSettings() {
        return settings;
    }

    public static class ClipboardItem {
        public Selection.EntityType type;
        public int id;
        public int offsetX, offsetY; // Desplazamiento relativo al punto de referencia
        public int[] layers; // Para capturar las 4 capas de un tile
        public boolean blocked;
        public int trigger;
        public int exitMap, exitX, exitY;
        public int objIndex, objAmount;
        public int particleIndex;

        public ClipboardItem(Selection.EntityType type, int id, int offsetX, int offsetY) {
            this(type, id, offsetX, offsetY, null);
        }

        public ClipboardItem(Selection.EntityType type, int id, int offsetX, int offsetY, int[] layers) {
            this.type = type;
            this.id = id;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.layers = layers;
        }
    }

    private static volatile Clipboard instance;
    private static final Object lock = new Object();

    private final List<ClipboardItem> items = new ArrayList<>();

    private Clipboard() {
    }

    public static Clipboard getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new Clipboard();
                }
            }
        }
        return instance;
    }

    public void copy(List<Selection.SelectedEntity> selectedEntities, int refX, int refY) {
        var context = GameData.getActiveContext();
        if (context == null || context.getMapData() == null)
            return;
        var mapData = context.getMapData();

        items.clear();
        for (Selection.SelectedEntity se : selectedEntities) {
            ClipboardItem item = new ClipboardItem(se.type, se.id, se.x - refX, se.y - refY);

            if (se.type == Selection.EntityType.TILE) {
                var data = mapData[se.x][se.y];
                item.layers = new int[5];
                for (int i = 1; i <= 4; i++) {
                    item.layers[i] = data.getLayer(i).getGrhIndex();
                }
                item.blocked = data.getBlocked();
                item.trigger = data.getTrigger();
                item.exitMap = data.getExitMap();
                item.exitX = data.getExitX();
                item.exitY = data.getExitY();
                item.objIndex = data.getObjIndex();
                item.objAmount = data.getObjAmount();
                item.particleIndex = data.getParticleIndex();
            }
            items.add(item);
        }
    }

    public List<ClipboardItem> getItems() {
        return items;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}
