package org.argentumforge.engine.utils.inits;

public final class ObjData {

    private int number;
    private String name = "";
    private int grhIndex;

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
}
