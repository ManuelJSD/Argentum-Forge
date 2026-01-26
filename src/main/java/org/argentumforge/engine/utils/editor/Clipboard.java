package org.argentumforge.engine.utils.editor;

import java.util.ArrayList;
import java.util.List;

/**
 * Servicio de portapapeles para copiar, cortar y pegar entidades.
 */
public class Clipboard {

    public static class ClipboardItem {
        public Selection.EntityType type;
        public int id;
        public int offsetX, offsetY; // Desplazamiento relativo al punto de referencia
        public int[] layers; // Para capturar las 4 capas de un tile

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
        items.clear();
        for (Selection.SelectedEntity se : selectedEntities) {
            int[] layers = null;
            if (se.type == Selection.EntityType.TILE) {
                layers = new int[5]; // Usamos 1-4 para coincidir con MapData
                for (int i = 1; i <= 4; i++) {
                    layers[i] = org.argentumforge.engine.utils.GameData.mapData[se.x][se.y].getLayer(i).getGrhIndex();
                }
            }
            items.add(new ClipboardItem(se.type, se.id, se.x - refX, se.y - refY, layers));
        }
    }

    public List<ClipboardItem> getItems() {
        return items;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}
