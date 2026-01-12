package org.argentumforge.engine.game;

import org.argentumforge.engine.renderer.RGBColor;
import org.argentumforge.engine.game.Options;

public enum Weather {
    INSTANCE;

    private final RGBColor manualColor;

    Weather() {
        this.manualColor = new RGBColor(Options.INSTANCE.getAmbientR(), Options.INSTANCE.getAmbientG(),
                Options.INSTANCE.getAmbientB());
    }

    public void update() {
        // No longer automated
    }

    public void setAmbientColor(float r, float g, float b) {
        Options.INSTANCE.setAmbientR(r);
        Options.INSTANCE.setAmbientG(g);
        Options.INSTANCE.setAmbientB(b);
        manualColor.setRed(r);
        manualColor.setGreen(g);
        manualColor.setBlue(b);
        Options.INSTANCE.save();
    }

    public RGBColor getWeatherColor() {
        return manualColor;
    }

}
