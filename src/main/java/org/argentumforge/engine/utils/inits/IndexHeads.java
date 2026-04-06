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

    private int[] head = new int[5];

    public int getHead(int index) {
        return head[index];
    }

    public void setHead(int index, int head) {
        this.head[index] = head;
    }

}
