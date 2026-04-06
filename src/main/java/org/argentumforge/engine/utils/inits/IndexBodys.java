package org.argentumforge.engine.utils.inits;

/**
 * Estructura de indexación para los gráficos de cuerpos.
 * <p>
 * {@code IndexBodys} se utiliza durante el proceso de carga para mapear los
 * índices de animación de caminar y los desplazamientos de cabeza definidos
 * en los archivos de inicialización.
 *
 * @see BodyData
 */
public final class IndexBodys {

    private int[] body = new int[5];
    private short headOffsetX;
    private short headOffsetY;

    public int getBody(int index) {
        return body[index];
    }

    public void setBody(int index, int body) {
        this.body[index] = body;
    }

    public short getHeadOffsetX() {
        return headOffsetX;
    }

    public void setHeadOffsetX(short headOffsetX) {
        this.headOffsetX = headOffsetX;
    }

    public short getHeadOffsetY() {
        return headOffsetY;
    }

    public void setHeadOffsetY(short headOffsetY) {
        this.headOffsetY = headOffsetY;
    }

}
