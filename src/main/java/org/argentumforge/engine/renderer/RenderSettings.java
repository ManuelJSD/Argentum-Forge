package org.argentumforge.engine.renderer;

public class RenderSettings {

    // Capas del 1 al 4
    private boolean[] showLayer = { true, true, true, false };
    // NPCs
    private boolean showNPCs = true;
    // Objetos (OBJs)
    private boolean showOJBs = true;
    // Triggers (Disparadores)
    private boolean showTriggers = false;
    // Traslados (Teleporks)
    private boolean showMapTransfer = true;
    // Bloqueos
    private boolean showBlock = true;
    private float blockOpacity = 0.5f;
    private float ghostOpacity = 0.5f;
    private boolean showGrid = false;
    private float[] gridColor = { 1.0f, 1.0f, 1.0f, 0.2f }; // Blanco semi-transparente por defecto

    public boolean[] getShowLayer() {
        return showLayer;
    }

    public void setShowLayer(boolean[] showLayer) {
        this.showLayer = showLayer;
    }

    public boolean getShowNPCs() {
        return showNPCs;
    }

    public void setShowNPCs(boolean showNPCs) {
        this.showNPCs = showNPCs;
    }

    public boolean getShowOJBs() {
        return showOJBs;
    }

    public void setShowOJBs(boolean showOJBs) {
        this.showOJBs = showOJBs;
    }

    public boolean getShowTriggers() {
        return showTriggers;
    }

    public void setShowTriggers(boolean showTriggers) {
        this.showTriggers = showTriggers;
    }

    public boolean getShowMapTransfer() {
        return showMapTransfer;
    }

    public void setShowMapTransfer(boolean showMapTransfer) {
        this.showMapTransfer = showMapTransfer;
    }

    public boolean getShowBlock() {
        return showBlock;
    }

    public void setShowBlock(boolean showBlock) {
        this.showBlock = showBlock;
    }

    public float getBlockOpacity() {
        return blockOpacity;
    }

    public void setBlockOpacity(float blockOpacity) {
        this.blockOpacity = blockOpacity;
    }

    public float getGhostOpacity() {
        return ghostOpacity;
    }

    public void setGhostOpacity(float ghostOpacity) {
        this.ghostOpacity = ghostOpacity;
    }

    public boolean isShowGrid() {
        return showGrid;
    }

    public void setShowGrid(boolean showGrid) {
        this.showGrid = showGrid;
    }

    public float[] getGridColor() {
        return gridColor;
    }

    public void setGridColor(float[] gridColor) {
        this.gridColor = gridColor;
    }

}
