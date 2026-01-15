package org.argentumforge.engine.utils.editor.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa una categoría que agrupa múltiples registros de GRH.
 */
public class GrhCategory {
    private String name;
    private List<GrhIndexRecord> records = new ArrayList<>();

    public GrhCategory() {
    }

    public GrhCategory(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<GrhIndexRecord> getRecords() {
        return records;
    }

    public void setRecords(List<GrhIndexRecord> records) {
        this.records = records;
    }

    public void addRecord(GrhIndexRecord record) {
        this.records.add(record);
    }
}
