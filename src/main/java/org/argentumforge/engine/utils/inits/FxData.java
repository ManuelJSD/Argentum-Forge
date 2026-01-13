package org.argentumforge.engine.utils.inits;

/**
 * Datos de definición de efectos visuales (FX).
 * <p>
 * {@code FxData} contiene la referencia a la animación (índice GRH) y los
 * desplazamientos (offsets) necesarios para posicionar correctamente el efecto
 * sobre un personaje u objeto.
 *
 * @see GrhInfo
 */
public final class FxData {

    private short Animacion;
    private short OffsetX;
    private short OffsetY;

    public short getAnimacion() {
        return Animacion;
    }

    public void setAnimacion(short animacion) {
        Animacion = animacion;
    }

    public short getOffsetX() {
        return OffsetX;
    }

    public void setOffsetX(short offsetX) {
        OffsetX = offsetX;
    }

    public short getOffsetY() {
        return OffsetY;
    }

    public void setOffsetY(short offsetY) {
        OffsetY = offsetY;
    }

}
