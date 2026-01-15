package org.argentumforge.engine.utils.editor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.argentumforge.engine.utils.editor.models.GrhCategory;
import org.argentumforge.engine.utils.editor.models.GrhIndexRecord;
import org.tinylog.Logger;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestiona la carga y guardado de la biblioteca de GRHs en formato JSON.
 */
public class GrhLibraryManager {
    private static final String LIBRARY_FILENAME = "grh_library.json";
    private static GrhLibraryManager instance;
    private final Gson gson;
    private List<GrhCategory> categories = new ArrayList<>();

    private GrhLibraryManager() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        load();
    }

    public static GrhLibraryManager getInstance() {
        if (instance == null) {
            instance = new GrhLibraryManager();
        }
        return instance;
    }

    public List<GrhCategory> getCategories() {
        return categories;
    }

    public void load() {
        File file = new File(LIBRARY_FILENAME);
        if (!file.exists()) {
            createDefaultLibrary();
            save();
            return;
        }

        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            Type listType = new TypeToken<ArrayList<GrhCategory>>() {
            }.getType();
            categories = gson.fromJson(reader, listType);
            if (categories == null)
                categories = new ArrayList<>();
        } catch (IOException e) {
            Logger.error(e, "Error al cargar la biblioteca de GRHs");
            createDefaultLibrary();
        }
    }

    public void save() {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(LIBRARY_FILENAME), StandardCharsets.UTF_8)) {
            gson.toJson(categories, writer);
        } catch (IOException e) {
            Logger.error(e, "Error al guardar la biblioteca de GRHs");
        }
    }

    private void createDefaultLibrary() {
        categories = new ArrayList<>();

        GrhCategory surfaces = new GrhCategory("Superficies");
        surfaces.addRecord(new GrhIndexRecord("Pasto 1", 1));
        surfaces.addRecord(new GrhIndexRecord("Pasto 2", 2));
        categories.add(surfaces);

        GrhCategory walls = new GrhCategory("Paredes");
        GrhIndexRecord wall1 = new GrhIndexRecord("Pared Piedra", 100);
        wall1.setLayer(3);
        wall1.setAutoBlock(true);
        walls.addRecord(wall1);
        categories.add(walls);

        GrhCategory decor = new GrhCategory("Decoracion");
        categories.add(decor);
    }
}
