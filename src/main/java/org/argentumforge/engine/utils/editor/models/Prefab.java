package org.argentumforge.engine.utils.editor.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa una estructura prefabricada (Prefab) que puede ser guardada y
 * reutilizada.
 * Contiene información sobre tiles, objetos, NPCs y otros elementos del mapa.
 */
public class Prefab {

    private String name;
    private String category;
    private String author;
    private int width;
    private int height;
    private PrefabFeatures features;
    private List<PrefabCell> data;

    public Prefab() {
        this.data = new ArrayList<>();
        this.features = new PrefabFeatures();
    }

    public Prefab(String name, String category, int width, int height) {
        this.name = name;
        this.category = category;
        this.width = width;
        this.height = height;
        this.data = new ArrayList<>();
        this.features = new PrefabFeatures();
        this.author = System.getProperty("user.name");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public PrefabFeatures getFeatures() {
        return features;
    }

    public void setFeatures(PrefabFeatures features) {
        this.features = features;
    }

    public List<PrefabCell> getData() {
        return data;
    }

    public void setData(List<PrefabCell> data) {
        this.data = data;
    }

    public void addCell(PrefabCell cell) {
        this.data.add(cell);
    }

    /**
     * Opciones sobre qué incluir en el prefab.
     */
    public static class PrefabFeatures {
        public boolean layer1 = true;
        public boolean layer2 = true;
        public boolean layer3 = true;
        public boolean layer4 = true;
        public boolean block = true;
        public boolean triggers = true;
        public boolean npcs = true;
        public boolean objects = true;
        public boolean particles = true;
        // public boolean exits = false; // Por defecto desactivado
    }

    /**
     * Representa los datos de una celda individual dentro del prefab.
     * Las coordenadas x, y son relativas al origen del prefab (0,0).
     */
    public static class PrefabCell {
        public int x;
        public int y;

        // Layers 1-4. Usamos un array de 5 para mantener consistencia con indices
        // 1-based si se desea,
        // o mapeamos 0->1. Aquí guardaremos indices de GRH.
        // Index 0 vacio, 1=Capa1, etc.
        public int[] layerGrhs = new int[5];

        public boolean blocked;
        public int trigger;

        public int objIndex;
        public int objAmount;

        public int npcIndex;
        public int particleIndex;

        public PrefabCell() {
        }

        public PrefabCell(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
