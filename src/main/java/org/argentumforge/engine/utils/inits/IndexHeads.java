package org.argentumforge.engine.utils.inits;

/**
 * Estructura de indexación para los gráficos de cabezas.
 * <p>
 * {@code IndexHeads} almacena los índices de los gráficos correspondientes a
 * las cabezas en las cuatro orientaciones posibles.
 *
 * @see HeadData
 */
public final class IndexHeads {

    private short[] head = new short[5];

    public short getHead(int index) {
        return head[index];
    }

    public void setHead(int index, short head) {
        this.head[index] = head;
    }

}
