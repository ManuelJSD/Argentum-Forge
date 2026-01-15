package org.argentumforge.engine.utils.editor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.argentumforge.engine.utils.editor.models.TriggerData;
import org.tinylog.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TriggerManager {

    private static TriggerManager instance;

    private List<TriggerData> triggers;
    private final String TRIGGERS_FILE = "Triggers.json";

    private TriggerManager() {
        triggers = new ArrayList<>();
        loadTriggers();
    }

    public static TriggerManager getInstance() {
        if (instance == null) {
            instance = new TriggerManager();
        }
        return instance;
    }

    public void loadTriggers() {
        Path path = Paths.get(TRIGGERS_FILE);
        if (!Files.exists(path)) {
            // Create default triggers if file doesn't exist
            createDefaultTriggers();
            saveTriggers();
            return;
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            Gson gson = new Gson();
            triggers = gson.fromJson(reader, new TypeToken<List<TriggerData>>() {
            }.getType());
            if (triggers == null)
                triggers = new ArrayList<>();
            sortTriggers();
            Logger.info("Loaded " + triggers.size() + " triggers from " + TRIGGERS_FILE);
        } catch (IOException e) {
            Logger.error(e, "Error loading triggers from " + TRIGGERS_FILE);
        }
    }

    public void saveTriggers() {
        try (Writer writer = Files.newBufferedWriter(Paths.get(TRIGGERS_FILE))) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(triggers, writer);
        } catch (IOException e) {
            Logger.error(e, "Error saving triggers to " + TRIGGERS_FILE);
        }
    }

    private void createDefaultTriggers() {
        triggers.clear();
        triggers.add(new TriggerData(1, "Des layer 4"));
        triggers.add(new TriggerData(2, "Des layer 4 y AntiRespawn Npcs"));
        triggers.add(new TriggerData(3, "Posicion ilegal pa npcs"));
        triggers.add(new TriggerData(4, "Des layer 4 y No Combate"));
        triggers.add(new TriggerData(5, "Anti Piquete"));
        triggers.add(new TriggerData(6, "Zona de Combate"));
        triggers.add(new TriggerData(7, "Zona de Invocaciones"));
    }

    public List<TriggerData> getTriggers() {
        return triggers;
    }

    public void addTrigger(String name) {
        int newId = getNextId();
        triggers.add(new TriggerData(newId, name));
        sortTriggers();
        saveTriggers();
    }

    public void updateTrigger(int id, String newName) {
        for (TriggerData t : triggers) {
            if (t.getId() == id) {
                t.setName(newName);
                break;
            }
        }
        saveTriggers();
    }

    public void removeTrigger(int id) {
        triggers.removeIf(t -> t.getId() == id);
        saveTriggers();
    }

    public TriggerData getTrigger(int id) {
        for (TriggerData t : triggers) {
            if (t.getId() == id)
                return t;
        }
        return null;
    }

    private int getNextId() {
        int max = 0;
        for (TriggerData t : triggers) {
            if (t.getId() > max)
                max = t.getId();
        }
        return max + 1;
    }

    private void sortTriggers() {
        triggers.sort(Comparator.comparingInt(TriggerData::getId));
    }
}
