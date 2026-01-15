package org.argentumforge.engine.utils.editor.models;

public class TriggerData {
    private int id;
    private String name;

    public TriggerData() {
    }

    public TriggerData(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return id + " - " + name;
    }
}
