package org.argentumforge.engine.utils.inits;

import org.argentumforge.engine.game.models.Position;

/**
 * Datos de representación visual de los cuerpos de los personajes.
 * <p>
 * {@code BodyData} almacena las animaciones de movimiento (caminar) en las
 * cuatro direcciones cardinales y el desplazamiento relativo (offset) para la
 * posición de la cabeza.
 *
 * @see GrhInfo
 * @see IndexBodys
 */
public final class BodyData {

    private final GrhInfo[] walk = new GrhInfo[5];
    private final Position headOffset;

    public BodyData() {
        walk[1] = new GrhInfo();
        walk[2] = new GrhInfo();
        walk[3] = new GrhInfo();
        walk[4] = new GrhInfo();

        headOffset = new Position();
    }

    /**
     * Sirve para asignar a un personaje su body ya inicializado.
     */
    public BodyData(BodyData other) {
        walk[1] = new GrhInfo(other.walk[1]);
        walk[2] = new GrhInfo(other.walk[2]);
        walk[3] = new GrhInfo(other.walk[3]);
        walk[4] = new GrhInfo(other.walk[4]);

        headOffset = other.headOffset;
    }

    public GrhInfo getWalk(int index) {
        return walk[index];
    }

    public void setWalk(int index, GrhInfo walk) {
        this.walk[index] = walk;
    }

    public Position getHeadOffset() {
        return headOffset;
    }

}
