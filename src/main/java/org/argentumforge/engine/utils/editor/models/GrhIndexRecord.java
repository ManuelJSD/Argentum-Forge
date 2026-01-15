package org.argentumforge.engine.utils.editor.models;

/**
 * Representa una entrada individual en la biblioteca de GRHs.
 */
public class GrhIndexRecord {
    private String name;
    private int grhIndex;
    private int layer = 1;
    private boolean autoBlock = false;
    private int width = 1;
    private int height = 1;

    public GrhIndexRecord() {
    }

    public GrhIndexRecord(String name, int grhIndex) {
        this.name = name;
        this.grhIndex = grhIndex;
    }

    // Getters y Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getGrhIndex() {
        return grhIndex;
    }

    public void setGrhIndex(int grhIndex) {
        this.grhIndex = grhIndex;
    }

    public int getLayer() {
        return layer;
    }

    public void setLayer(int layer) {
        this.layer = layer;
    }

    public boolean isAutoBlock() {
        return autoBlock;
    }

    public void setAutoBlock(boolean autoBlock) {
        this.autoBlock = autoBlock;
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
}
