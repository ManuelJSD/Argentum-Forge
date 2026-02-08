package org.argentumforge.engine.utils.editor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.argentumforge.engine.utils.editor.models.Prefab;
import org.tinylog.Logger;
import org.argentumforge.engine.utils.GameData;
import org.argentumforge.engine.utils.inits.MapData;
import org.argentumforge.engine.game.EditorController;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Singleton encargado de gestionar la carga, guardado y listado de
 * Prefabricados.
 */
public class PrefabManager {

    private static PrefabManager instance;
    private final List<Prefab> prefabs;
    private final String PREFABS_DIR = "assets/prefabs/";
    private final Gson gson;

    private PrefabManager() {
        this.prefabs = new ArrayList<>();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        ensureDirectoryExists();
        loadPrefabs();
    }

    public static synchronized PrefabManager getInstance() {
        if (instance == null) {
            instance = new PrefabManager();
        }
        return instance;
    }

    private void ensureDirectoryExists() {
        File dir = new File(PREFABS_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    /**
     * Carga todos los prefabs (.json) del directorio assets/prefabs.
     */
    public void loadPrefabs() {
        prefabs.clear();
        File dir = new File(PREFABS_DIR);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));

        if (files == null)
            return;

        for (File file : files) {
            try (FileReader reader = new FileReader(file)) {
                Prefab prefab = gson.fromJson(reader, Prefab.class);
                if (prefab != null) {
                    prefabs.add(prefab);
                }
            } catch (Exception e) {
                Logger.error(e, "Error cargando prefab: " + file.getName());
            }
        }
        Logger.info("Cargados " + prefabs.size() + " prefabs.");
    }

    /**
     * Guarda un prefab en disco formato JSON.
     * 
     * @param prefab El prefab a guardar.
     * @return true si se guardó correctamente.
     */
    public boolean savePrefab(Prefab prefab) {
        if (prefab.getName() == null || prefab.getName().isEmpty()) {
            Logger.error("Intento de guardar prefab sin nombre.");
            return false;
        }

        // Sanitizar nombre para usar como archivo
        String filename = prefab.getName().replaceAll("[^a-zA-Z0-9.-]", "_") + ".json";
        File file = new File(PREFABS_DIR, filename);

        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(prefab, writer);

            // Actualizar lista en memoria si es nuevo
            if (!prefabs.contains(prefab)) {
                prefabs.add(prefab);
            }
            return true;
        } catch (IOException e) {
            Logger.error(e, "Error guardando prefab: " + filename);
            return false;
        }
    }

    /**
     * Elimina un prefab del disco y de la memoria.
     */
    public boolean deletePrefab(Prefab prefab) {
        if (prefab == null)
            return false;

        String filename = prefab.getName().replaceAll("[^a-zA-Z0-9.-]", "_") + ".json";
        File file = new File(PREFABS_DIR, filename);

        boolean deleted = false;
        if (file.exists()) {
            deleted = file.delete();
        }

        prefabs.remove(prefab);
        return deleted;
    }

    public List<Prefab> getAllPrefabs() {
        return prefabs;
    }

    public List<String> getCategories() {
        return prefabs.stream()
                .map(Prefab::getCategory)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    public List<Prefab> getPrefabsByCategory(String category) {
        if (category == null || category.isEmpty())
            return prefabs;
        return prefabs.stream()
                .filter(p -> category.equals(p.getCategory()))
                .collect(Collectors.toList());
    }

    /**
     * Actualiza un prefab existente. Si cambia el nombre, renombra el archivo.
     */
    public boolean updatePrefab(Prefab prefab, String newName, String newCategory) {
        if (prefab == null || newName == null || newName.isEmpty())
            return false;

        String oldFilename = prefab.getName().replaceAll("[^a-zA-Z0-9.-]", "_") + ".json";
        String newFilename = newName.replaceAll("[^a-zA-Z0-9.-]", "_") + ".json";

        File oldFile = new File(PREFABS_DIR, oldFilename);
        File newFile = new File(PREFABS_DIR, newFilename);

        // Si cambia el nombre (y por ende el archivo)
        if (!oldFilename.equals(newFilename)) {
            if (newFile.exists()) {
                Logger.error("Error actualizando prefab: Ya existe un archivo con el nombre " + newFilename);
                return false;
            }

            if (oldFile.exists()) {
                if (!oldFile.renameTo(newFile)) {
                    Logger.error("Error renombrando archivo de prefab.");
                    return false;
                }
            }
        }

        // Actualizar datos en memoria
        prefab.setName(newName);
        prefab.setCategory(newCategory);

        // Guardar cambios (sobreescribir el archivo nuevo/actual)
        return savePrefab(prefab);
    }

    /**
     * Crea un objeto Prefab a partir de un área del mapa actual.
     */
    public static Prefab createPrefabFromMap(String name, String category, int minX, int minY, int width, int height,
            Prefab.PrefabFeatures features) {
        var context = GameData.getActiveContext();
        if (context == null || context.getMapData() == null) {
            return null;
        }

        MapData[][] mapData = context.getMapData();
        Prefab prefab = new Prefab(name, category, width, height);
        prefab.setFeatures(features);

        // Iterar sobre el área seleccionada
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int mapX = minX + x;
                int mapY = minY + y;

                // Verificar límites del mapa
                if (mapX < 1 || mapX >= mapData.length || mapY < 1 || mapY >= mapData[0].length) {
                    continue;
                }

                MapData tile = mapData[mapX][mapY];
                Prefab.PrefabCell cell = new Prefab.PrefabCell(x, y);
                boolean hasContent = false;

                if (features.layer1 && tile.getLayer(1).getGrhIndex() > 0) {
                    cell.layerGrhs[1] = tile.getLayer(1).getGrhIndex();
                    hasContent = true;
                }
                if (features.layer2 && tile.getLayer(2).getGrhIndex() > 0) {
                    cell.layerGrhs[2] = tile.getLayer(2).getGrhIndex();
                    hasContent = true;
                }
                if (features.layer3 && tile.getLayer(3).getGrhIndex() > 0) {
                    cell.layerGrhs[3] = tile.getLayer(3).getGrhIndex();
                    hasContent = true;
                }
                if (features.layer4 && tile.getLayer(4).getGrhIndex() > 0) {
                    cell.layerGrhs[4] = tile.getLayer(4).getGrhIndex();
                    hasContent = true;
                }

                if (features.block && tile.getBlocked()) {
                    cell.blocked = true;
                    hasContent = true;
                }

                if (features.triggers && tile.getTrigger() > 0) {
                    cell.trigger = tile.getTrigger();
                    hasContent = true;
                }

                if (features.objects && tile.getObjGrh().getGrhIndex() > 0) {
                    cell.objIndex = tile.getObjIndex();
                    cell.objAmount = tile.getObjAmount();
                    // Wait, MapData stores ObjGrh separately?
                    // Yes, tile.getObjGrh() is the graphic.
                    // But let's confirm if we need to store the GrhIndex of the object manually
                    // or if it's derived from objIndex (item ID).
                    // Usually ObjIndex is likely the Item ID (OBJ.DAT) and ObjGrh is the visual.
                    // The game reconstructs Grh from ObjIndex usually.
                    // But MapData has both.
                    // In Prefab we probably just need ObjIndex (ID) and Amount.
                    // For now let's store ObjIndex and Amount. The system should resolve GRH later.
                    // Actually, tile.getObjGrh().getGrhIndex() is useful to know IF there is an
                    // object.
                    hasContent = true;
                }

                if (features.npcs && tile.getNpcIndex() > 0) {
                    cell.npcIndex = tile.getNpcIndex();
                    hasContent = true;
                }

                if (features.particles && tile.getParticleIndex() > 0) {
                    cell.particleIndex = tile.getParticleIndex();
                    hasContent = true;
                }

                // Solo guardamos la celda si tiene algo relevante para ahorrar espacio
                if (hasContent) {
                    prefab.addCell(cell);
                }
            }
        }

        return prefab;
    }

    /**
     * Prepara el sistema para pegar el prefab seleccionado.
     * Convierte el Prefab a items del Clipboard y activa el modo de pegado.
     */
    public void pastePrefab(Prefab prefab) {
        if (prefab == null)
            return;

        Clipboard clip = Clipboard.getInstance();
        clip.getItems().clear();

        int centerX = prefab.getWidth() / 2;
        int centerY = prefab.getHeight() / 2;

        for (Prefab.PrefabCell cell : prefab.getData()) {
            // Asumimos offsets relativos al origen (0,0) del prefab
            // Ajustamos para que el "anchor" sea el centro del prefab, no la esquina
            // superior izquierda
            int offX = cell.x - centerX;
            int offY = cell.y - centerY;

            Clipboard.ClipboardItem item = new Clipboard.ClipboardItem(
                    Selection.EntityType.TILE,
                    0,
                    offX,
                    offY);

            // Copiar datos de la celda al item del clipboard
            item.layers = new int[5];
            System.arraycopy(cell.layerGrhs, 0, item.layers, 0, 5);

            item.blocked = cell.blocked;
            item.trigger = cell.trigger;
            item.objIndex = cell.objIndex;
            item.objAmount = cell.objAmount;
            item.particleIndex = cell.particleIndex;
            // Exits no están en el prefab por ahora, defaults a 0

            clip.getItems().add(item);
        }

        // Activar modo pegar
        EditorController.INSTANCE.pasteSelection();
    }
}
