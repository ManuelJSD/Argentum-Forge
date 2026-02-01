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
    // Partículas
    private boolean showParticles = false;
    // Bloqueos
    private boolean showBlock = true;
    private float blockOpacity = 0.5f;
    private float ghostOpacity = 0.5f;
    private boolean showGrid = false;
    private float[] gridColor = { 1.0f, 1.0f, 1.0f, 0.2f }; // Blanco semi-transparente por defecto
    private boolean showNpcBreathing = true;
    private boolean disableAnimations = false;

    // Photo Mode
    private boolean photoModeActive = false;
    private boolean photoVignette = true;
    private boolean photoShadows = true;
    private boolean photoSoftShadows = true;
    private boolean photoWater = true;
    private float vignetteIntensity = 0.8f;

    // Viewport Overlay (Reference Frame)
    private boolean showViewportOverlay = false;
    private int viewportWidth = 17;
    private int viewportHeight = 15;
    private float[] viewportColor = { 1.0f, 1.0f, 1.0f, 0.5f }; // White semi-transparent

    // --- PHOTO MODE ULTRA ---
    private boolean photoBloom = false;
    private float bloomIntensity = 0.5f;
    private float photoBloomThreshold = 0.6f;

    private boolean photoDoF = false;
    private float dofFocus = 0.5f;
    private float dofRange = 0.2f;

    private boolean photoGrain = false;
    private float grainIntensity = 0.1f;

    private float photoExposure = 1.0f;
    private float photoContrast = 1.0f;
    private float photoSaturation = 1.0f;

    private float photoZoom = 1.0f;
    private boolean photoTimeStop = false;

    public enum ColorFilter {
        NONE, GRAYSCALE, SEPIA, VINTAGE, WARM, COLD
    }

    private ColorFilter photoColorFilter = ColorFilter.NONE;

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

    public boolean getShowParticles() {
        return showParticles;
    }

    public void setShowParticles(boolean showParticles) {
        this.showParticles = showParticles;
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

    public boolean isDisableAnimations() {
        return disableAnimations;
    }

    public void setDisableAnimations(boolean disableAnimations) {
        this.disableAnimations = disableAnimations;
    }

    public boolean isPhotoModeActive() {
        return photoModeActive;
    }

    public void setPhotoModeActive(boolean photoModeActive) {
        this.photoModeActive = photoModeActive;
    }

    public boolean isPhotoVignette() {
        return photoVignette;
    }

    public void setPhotoVignette(boolean photoVignette) {
        this.photoVignette = photoVignette;
    }

    public boolean isPhotoShadows() {
        return photoShadows;
    }

    public void setPhotoShadows(boolean photoShadows) {
        this.photoShadows = photoShadows;
    }

    public boolean isPhotoWater() {
        return photoWater;
    }

    public void setPhotoWater(boolean photoWater) {
        this.photoWater = photoWater;
    }

    public float getVignetteIntensity() {
        return vignetteIntensity;
    }

    public void setVignetteIntensity(float vignetteIntensity) {
        this.vignetteIntensity = vignetteIntensity;
    }

    public boolean isPhotoSoftShadows() {
        return photoSoftShadows;
    }

    public void setPhotoSoftShadows(boolean photoSoftShadows) {
        this.photoSoftShadows = photoSoftShadows;
    }

    public ColorFilter getPhotoColorFilter() {
        return photoColorFilter;
    }

    public void setPhotoColorFilter(ColorFilter photoColorFilter) {
        this.photoColorFilter = photoColorFilter;
    }

    // Getters and Setters for Ultra Features
    public boolean isPhotoBloom() {
        return photoBloom;
    }

    public void setPhotoBloom(boolean photoBloom) {
        this.photoBloom = photoBloom;
    }

    public float getBloomIntensity() {
        return bloomIntensity;
    }

    public void setBloomIntensity(float bloomIntensity) {
        this.bloomIntensity = bloomIntensity;
    }

    public float getPhotoBloomThreshold() {
        return photoBloomThreshold;
    }

    public void setPhotoBloomThreshold(float photoBloomThreshold) {
        this.photoBloomThreshold = photoBloomThreshold;
    }

    public boolean isPhotoDoF() {
        return photoDoF;
    }

    public void setPhotoDoF(boolean photoDoF) {
        this.photoDoF = photoDoF;
    }

    public float getDofFocus() {
        return dofFocus;
    }

    public void setDofFocus(float dofFocus) {
        this.dofFocus = dofFocus;
    }

    public float getDofRange() {
        return dofRange;
    }

    public void setDofRange(float dofRange) {
        this.dofRange = dofRange;
    }

    public boolean isPhotoGrain() {
        return photoGrain;
    }

    public void setPhotoGrain(boolean photoGrain) {
        this.photoGrain = photoGrain;
    }

    public float getGrainIntensity() {
        return grainIntensity;
    }

    public void setGrainIntensity(float grainIntensity) {
        this.grainIntensity = grainIntensity;
    }

    public float getPhotoExposure() {
        return photoExposure;
    }

    public void setPhotoExposure(float photoExposure) {
        this.photoExposure = photoExposure;
    }

    public float getPhotoContrast() {
        return photoContrast;
    }

    public void setPhotoContrast(float photoContrast) {
        this.photoContrast = photoContrast;
    }

    public float getPhotoSaturation() {
        return photoSaturation;
    }

    public void setPhotoSaturation(float photoSaturation) {
        this.photoSaturation = photoSaturation;
    }

    public float getPhotoZoom() {
        return photoZoom;
    }

    public void setPhotoZoom(float photoZoom) {
        this.photoZoom = photoZoom;
    }

    public boolean isPhotoTimeStop() {
        return photoTimeStop;
    }

    public void setPhotoTimeStop(boolean photoTimeStop) {
        this.photoTimeStop = photoTimeStop;
    }

    public void resetPhotoMode() {
        this.photoVignette = true;
        this.photoShadows = true;
        this.photoSoftShadows = true;
        this.photoWater = true;
        this.vignetteIntensity = 0.8f;
        this.photoBloom = false;
        this.bloomIntensity = 0.5f;
        this.photoBloomThreshold = 0.6f;
        this.photoDoF = false;
        this.dofFocus = 0.5f;
        this.dofRange = 0.2f;
        this.photoGrain = false;
        this.grainIntensity = 0.1f;
        this.photoExposure = 1.0f;
        this.photoContrast = 1.0f;
        this.photoSaturation = 1.0f;
        this.photoZoom = 1.0f;
        this.photoTimeStop = false;
        this.photoColorFilter = ColorFilter.NONE;
    }

    public boolean isShowViewportOverlay() {
        return showViewportOverlay;
    }

    public void setShowViewportOverlay(boolean showViewportOverlay) {
        this.showViewportOverlay = showViewportOverlay;
    }

    public int getViewportWidth() {
        return viewportWidth;
    }

    public void setViewportWidth(int viewportWidth) {
        this.viewportWidth = viewportWidth;
    }

    public int getViewportHeight() {
        return viewportHeight;
    }

    public void setViewportHeight(int viewportHeight) {
        this.viewportHeight = viewportHeight;
    }

    public float[] getViewportColor() {
        return viewportColor;
    }

    public void setViewportColor(float[] viewportColor) {
        this.viewportColor = viewportColor;
    }
}
