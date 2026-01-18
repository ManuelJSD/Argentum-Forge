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
    private boolean showNpcBreathing = true;

    // Minimap filters
    private boolean[] minimapLayers = { true, false, false, false };
    private boolean showMinimapNPCs = true;
    private boolean showMinimapExits = true;
    private boolean showMinimapTriggers = true;
    private boolean showMinimapBlocks = false;

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

    public boolean isShowNpcBreathing() {
        return showNpcBreathing;
    }

    public void setShowNpcBreathing(boolean showNpcBreathing) {
        this.showNpcBreathing = showNpcBreathing;
    }

    public boolean[] getMinimapLayers() {
        return minimapLayers;
    }

    public void setMinimapLayers(boolean[] minimapLayers) {
        this.minimapLayers = minimapLayers;
    }

    public boolean isShowMinimapNPCs() {
        return showMinimapNPCs;
    }

    public void setShowMinimapNPCs(boolean showMinimapNPCs) {
        this.showMinimapNPCs = showMinimapNPCs;
    }

    public boolean isShowMinimapExits() {
        return showMinimapExits;
    }

    public void setShowMinimapExits(boolean showMinimapExits) {
        this.showMinimapExits = showMinimapExits;
    }

    public boolean isShowMinimapTriggers() {
        return showMinimapTriggers;
    }

    public void setShowMinimapTriggers(boolean showMinimapTriggers) {
        this.showMinimapTriggers = showMinimapTriggers;
    }

    public boolean isShowMinimapBlocks() {
        return showMinimapBlocks;
    }

    public void setShowMinimapBlocks(boolean showMinimapBlocks) {
        this.showMinimapBlocks = showMinimapBlocks;
    }

}
