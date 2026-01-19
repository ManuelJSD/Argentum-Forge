package org.argentumforge.engine.game;

import org.argentumforge.engine.renderer.RGBColor;

/**
 * Gestiona el clima y los efectos de iluminación ambiental del motor.
 */
public enum Weather {
    INSTANCE;

    private final RGBColor manualColor;

    Weather() {
        this.manualColor = new RGBColor(Options.INSTANCE.getAmbientR(), Options.INSTANCE.getAmbientG(),
                Options.INSTANCE.getAmbientB());
    }

    public void update() {
        // Ya no es automatizado por el ciclo día/noche heredado.
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
