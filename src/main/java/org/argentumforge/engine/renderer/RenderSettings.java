package org.argentumforge.engine.renderer;

public class RenderSettings {

    // Capas del 1 al 4
    private boolean[] showLayer = {true, true, true, false};
    // NPC's
    private boolean showNPCs = true;
    // OBJ's
    private boolean showOJBs = true;
    // Triggers
    private boolean showTriggers = false;
    // Traslados
    private boolean showMapTransfer = true;
    // Bloqueos
    private boolean showBlock = true;

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

    public void setShowMapTransfer(boolean showTranslation) {
        this.showMapTransfer = showMapTransfer;
    }

    public boolean getShowBlock() {
        return showBlock;
    }

    public void setShowBlock(boolean showBlock) {
        this.showBlock = showBlock;
    }

}
