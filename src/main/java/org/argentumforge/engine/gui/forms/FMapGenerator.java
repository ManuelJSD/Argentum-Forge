package org.argentumforge.engine.gui.forms;

import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImInt;
import org.argentumforge.engine.utils.GameData;
import org.argentumforge.engine.utils.editor.commands.BulkTileChangeCommand;
import org.argentumforge.engine.utils.editor.commands.BulkTileChangeCommand.TilePos;
import org.argentumforge.engine.utils.editor.commands.CommandManager;
import org.argentumforge.engine.utils.editor.GrhLibraryManager;
import org.argentumforge.engine.utils.editor.models.GrhCategory;
import org.argentumforge.engine.utils.editor.models.GrhIndexRecord;
import org.argentumforge.engine.utils.editor.PerlinNoise;
import org.argentumforge.engine.scenes.Camera;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FMapGenerator extends Form {

    // Biome categories
    private int selectedWaterCategory = -1;
    private int selectedBeachCategory = -1;
    private int selectedGrassCategory = -1;
    private int selectedForestCategory = -1;

    private String[] categoryNames;
    private List<GrhCategory> categories;

    // Generation parameters
    private final ImInt seed = new ImInt((int) (System.currentTimeMillis() % Integer.MAX_VALUE));
    private final ImInt scale = new ImInt(20); // Noise scale (lower = larger features)
    private final ImInt octaves = new ImInt(4);
    private final float[] persistence = { 0.5f };

    // Biome thresholds
    private final float[] waterLevel = { 0.3f }; // Below this = water
    private final float[] beachLevel = { 0.4f }; // Between water and beach = beach
    private final float[] forestDensity = { 0.6f }; // Above this in grass = forest

    public FMapGenerator() {
        loadCategories();
    }

    private void loadCategories() {
        categories = GrhLibraryManager.getInstance().getCategories();
        categoryNames = categories.stream()
                .map(GrhCategory::getName)
                .toArray(String[]::new);

        // Auto-detect biome categories
        for (int i = 0; i < categories.size(); i++) {
            String name = categories.get(i).getName().toLowerCase();
            if (name.contains("agua") && selectedWaterCategory == -1) {
                selectedWaterCategory = i;
            }
            if ((name.contains("arena") || name.contains("playa")) && selectedBeachCategory == -1) {
                selectedBeachCategory = i;
            }
            if (name.contains("pasto") && !name.contains("costa") && selectedGrassCategory == -1) {
                selectedGrassCategory = i;
            }
            if (name.contains("bosque") || name.contains("forest") && selectedForestCategory == -1) {
                selectedForestCategory = i;
            }
        }

        // Fallback: use pasto for beach if no beach category
        if (selectedBeachCategory == -1)
            selectedBeachCategory = selectedGrassCategory;
    }

    @Override
    public void render() {
        ImGui.setNextWindowSize(420, 580, imgui.flag.ImGuiCond.Once);
        if (ImGui.begin("Generador Procedural (Minecraft-Style)", ImGuiWindowFlags.NoResize)) {

            ImGui.text("=== Biomas ===");
            ImGui.separator();

            renderBiomeSelector("Agua:", selectedWaterCategory, "##water");
            renderBiomeSelector("Playa:", selectedBeachCategory, "##beach");
            renderBiomeSelector("Pasto:", selectedGrassCategory, "##grass");
            renderBiomeSelector("Bosque:", selectedForestCategory, "##forest");

            ImGui.spacing();
            ImGui.text("=== Parámetros de Generación ===");
            ImGui.separator();

            ImGui.text("Seed:");
            ImGui.sameLine();
            ImGui.pushItemWidth(200);
            ImGui.inputInt("##seed", seed);
            ImGui.popItemWidth();
            ImGui.sameLine();
            if (ImGui.button("Random##seed")) {
                seed.set((int) (System.currentTimeMillis() % Integer.MAX_VALUE));
            }

            ImGui.sliderInt("Escala", scale.getData(), 5, 50);
            if (ImGui.isItemHovered()) {
                ImGui.setTooltip("Menor = características más grandes");
            }

            ImGui.sliderInt("Octavas", octaves.getData(), 1, 8);
            if (ImGui.isItemHovered()) {
                ImGui.setTooltip("Más octavas = más detalle");
            }

            ImGui.sliderFloat("Persistencia", persistence, 0.1f, 0.9f);
            if (ImGui.isItemHovered()) {
                ImGui.setTooltip("Controla la influencia de octavas altas");
            }

            ImGui.spacing();
            ImGui.text("=== Niveles de Bioma ===");
            ImGui.separator();

            ImGui.sliderFloat("Nivel de Agua", waterLevel, 0.0f, 1.0f);
            ImGui.sliderFloat("Nivel de Playa", beachLevel, waterLevel[0], 1.0f);
            ImGui.sliderFloat("Densidad Bosque", forestDensity, 0.0f, 1.0f);

            ImGui.spacing();
            ImGui.separator();
            ImGui.spacing();

            if (ImGui.button("Generar Mundo", 400, 50)) {
                generateWorld();
            }

            ImGui.textWrapped(
                    "Este generador usa Perlin Noise multicapa para crear terreno procedural con biomas variados.");

        }
        ImGui.end();
    }

    private void renderBiomeSelector(String label, int currentIdx, String id) {
        ImGui.text(label);
        ImGui.sameLine();
        ImGui.pushItemWidth(300);
        ImInt idx = new ImInt(currentIdx);
        if (ImGui.combo(id, idx, categoryNames)) {
            // Update selection based on id
            if (id.equals("##water"))
                selectedWaterCategory = idx.get();
            else if (id.equals("##beach"))
                selectedBeachCategory = idx.get();
            else if (id.equals("##grass"))
                selectedGrassCategory = idx.get();
            else if (id.equals("##forest"))
                selectedForestCategory = idx.get();
        }
        ImGui.popItemWidth();
    }

    private void generateWorld() {
        if (selectedWaterCategory < 0 || selectedGrassCategory < 0) {
            org.argentumforge.engine.gui.DialogManager.getInstance().showInfo("Generador de Mapa",
                    "Por favor selecciona al menos Agua y Pasto.");
            return;
        }

        // Initialize Perlin Noise
        PerlinNoise heightNoise = new PerlinNoise(seed.get());
        PerlinNoise moistureNoise = new PerlinNoise(seed.get() + 1000);

        int width = Camera.XMaxMapSize + 1;
        int height = Camera.YMaxMapSize + 1;

        // Generate height and moisture maps
        double[][] heightMap = new double[width][height];
        double[][] moistureMap = new double[width][height];

        double scaleVal = scale.get() / 100.0;

        for (int x = 1; x < width; x++) {
            for (int y = 1; y < height; y++) {
                // Height determines water/land
                heightMap[x][y] = heightNoise.octaveNoise(
                        x * scaleVal, y * scaleVal, octaves.get(), persistence[0]);

                // Moisture determines grass/forest
                moistureMap[x][y] = moistureNoise.octaveNoise(
                        x * scaleVal * 0.7, y * scaleVal * 0.7, 3, 0.6);

                // Normalize to 0-1
                heightMap[x][y] = (heightMap[x][y] + 1) / 2.0;
                moistureMap[x][y] = (moistureMap[x][y] + 1) / 2.0;
            }
        }

        // Apply to map
        applyBiomes(heightMap, moistureMap, width, height);
    }

    private void applyBiomes(double[][] heightMap, double[][] moistureMap, int width, int height) {
        var context = GameData.getActiveContext();
        if (context == null || context.getMapData() == null)
            return;
        var mapData = context.getMapData();

        Map<TilePos, Integer> oldTiles = new HashMap<>();
        Map<TilePos, Integer> newTiles = new HashMap<>();

        // Get biome records
        GrhIndexRecord waterRec = categories.get(selectedWaterCategory).getRecords().get(0);
        GrhIndexRecord beachRec = selectedBeachCategory >= 0 ? categories.get(selectedBeachCategory).getRecords().get(0)
                : null;
        GrhIndexRecord grassRec = categories.get(selectedGrassCategory).getRecords().get(0);
        GrhIndexRecord forestRec = selectedForestCategory >= 0
                ? categories.get(selectedForestCategory).getRecords().get(0)
                : grassRec;

        for (int x = Camera.XMinMapSize; x <= Camera.XMaxMapSize; x++) {
            for (int y = Camera.YMinMapSize; y <= Camera.YMaxMapSize; y++) {
                if (x >= width || y >= height)
                    continue;

                double h = heightMap[x][y];
                double m = moistureMap[x][y];

                // Determine biome
                GrhIndexRecord biome;
                if (h < waterLevel[0]) {
                    biome = waterRec;
                } else if (h < beachLevel[0] && beachRec != null) {
                    biome = beachRec;
                } else if (m > forestDensity[0]) {
                    biome = forestRec;
                } else {
                    biome = grassRec;
                }

                // Apply mosaic pattern
                int relX = x % biome.getWidth();
                int relY = y % biome.getHeight();
                int newGrh = (biome.getGrhIndex() + (relY * biome.getWidth()) + relX);

                int currentGrh = mapData[x][y].getLayer(1).getGrhIndex();
                if (currentGrh != newGrh) {
                    oldTiles.put(new TilePos(x, y), currentGrh);
                    newTiles.put(new TilePos(x, y), newGrh);
                }
            }
        }

        if (!newTiles.isEmpty()) {
            CommandManager.getInstance()
                    .executeCommand(new BulkTileChangeCommand(GameData.getActiveContext(), 1, oldTiles, newTiles));
        }
    }
}
