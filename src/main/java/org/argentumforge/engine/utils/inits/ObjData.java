package org.argentumforge.engine.utils.inits;

/**
 * Modelo de datos para un objeto del juego.
 * <p>
 * {@code ObjData} almacena la información básica de un ítem u objeto cargado
 * desde OBJ.dat, incluyendo su nombre y el índice del gráfico (GRH) que lo
 * representa.
 */
public final class ObjData {

    private int number;
    private String name = "";
    private int grhIndex;
    private int type;

    public ObjData() {
    }

    public ObjData(int number) {
        this.number = number;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getGrhIndex() {
        return grhIndex;
    }

    public void setGrhIndex(int grhIndex) {
        this.grhIndex = grhIndex;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
