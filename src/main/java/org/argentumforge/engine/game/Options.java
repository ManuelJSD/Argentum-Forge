package org.argentumforge.engine.game;

import org.argentumforge.engine.renderer.RenderSettings;
import org.tinylog.Logger;

import java.io.*;

/**
 * Sistema de configuracion del juego que permite cargar y guardar opciones en
 * {@code options.ini}.
 */

public enum Options {

    INSTANCE; // Implementacion del patron Singleton de Joshua Bloch (considerada la mejor)

    private static final String OPTIONS_FILE_PATH = "resources/options.ini";

    private boolean music = true;
    private boolean sound = true;
    private boolean fullscreen = false;
    private boolean vsync = true;
    private boolean cursorGraphic = true;
    private String language = "es_ES";
    private String graphicsPath = "resources/graficos";
    private String datsPath = "resources/dats";
    private String initPath = "resources/inits";
    private String musicPath = "resources/musica";
    private int screenWidth = 1366;
    private int screenHeight = 768;
    private int clientWidth = 13;
    private int clientHeight = 11;
    private String lastMapPath = ".";
    private float ambientR = 1.0f;
    private float ambientG = 1.0f;
    private float ambientB = 1.0f;
    private java.util.List<String> recentMaps = new java.util.ArrayList<>();
    private static final int MAX_RECENT_MAPS = 10;
    private java.util.Set<Integer> ignoredObjTypes = new java.util.HashSet<>(
            java.util.Arrays.asList(4, 6, 8, 10, 15, 20, 22, 27, 28));

    private final RenderSettings renderSettings = new RenderSettings();

    /**
     * Carga las opciones.
     * <p>
     * Si el archivo existe y puede leerse, se recorren sus lineas para extraer las
     * opciones y sus valores, que luego son cargados
     * en las propiedades correspondientes. En caso de que el archivo no exista o
     * ocurra un error al leerlo, se genera una nueva
     * configuracion con valores predeterminados.
     */
    public void load() {
        try (BufferedReader reader = new BufferedReader(new FileReader(OPTIONS_FILE_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("=");
                if (parts.length == 2) {
                    String option = parts[0].trim();
                    String value = parts[1].trim();
                    load(option, value);
                }
            }
        } catch (IOException e) {
            Logger.error(
                    "El archivo {} no fue encontrado o no pudo leerse, se creó uno nuevo con la configuración por defecto.",
                    OPTIONS_FILE_PATH);
            save();
        }
    }

    /**
     * Guarda las opciones.
     */
    public void save() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(OPTIONS_FILE_PATH))) {
            write(writer, "Music", music);
            write(writer, "Sound", sound);
            write(writer, "GraphicsPath", graphicsPath);
            write(writer, "DatsPath", datsPath);
            write(writer, "InitPath", initPath);
            write(writer, "MusicPath", musicPath);
            write(writer, "Fullscreen", fullscreen);
            write(writer, "VSYNC", vsync);
            write(writer, "CursorGraphic", cursorGraphic);
            write(writer, "Language", language);
            write(writer, "ScreenWidth", screenWidth);
            write(writer, "ScreenHeight", screenHeight);
            write(writer, "ClientWidth", clientWidth);
            write(writer, "ClientHeight", clientHeight);
            write(writer, "LastMapPath", lastMapPath);
            write(writer, "AmbientR", ambientR);
            write(writer, "AmbientG", ambientG);
            write(writer, "AmbientB", ambientB);

            write(writer, "RenderLayer1", renderSettings.getShowLayer()[0]);
            write(writer, "RenderLayer2", renderSettings.getShowLayer()[1]);
            write(writer, "RenderLayer3", renderSettings.getShowLayer()[2]);
            write(writer, "RenderLayer4", renderSettings.getShowLayer()[3]);
            write(writer, "RenderShowNPCs", renderSettings.getShowNPCs());
            write(writer, "RenderShowObjects", renderSettings.getShowOJBs());
            write(writer, "RenderShowTriggers", renderSettings.getShowTriggers());
            write(writer, "RenderShowTranslation", renderSettings.getShowMapTransfer());
            write(writer, "RenderShowBlock", renderSettings.getShowBlock());
            write(writer, "RenderBlockOpacity", renderSettings.getBlockOpacity());
            write(writer, "RenderGhostOpacity", renderSettings.getGhostOpacity());
            write(writer, "RenderShowGrid", renderSettings.isShowGrid());
            write(writer, "RenderShowNpcBreathing", renderSettings.isShowNpcBreathing());

            for (int i = 0; i < recentMaps.size(); i++) {
                write(writer, "Recent" + (i + 1), recentMaps.get(i));
            }

            // Save Ignored Obj Types
            // Save as comma separated string for compactness
            String ignoredStr = ignoredObjTypes.stream().map(String::valueOf)
                    .collect(java.util.stream.Collectors.joining(","));
            write(writer, "IgnoredObjTypes", ignoredStr);
        } catch (IOException e) {
            Logger.error("¡No se pudo escribir en el archivo options.ini!");
        }
    }

    public RenderSettings getRenderSettings() {
        return renderSettings;
    }

    public boolean isMusic() {
        return music;
    }

    public void setMusic(boolean music) {
        this.music = music;
    }

    public boolean isSound() {
        return sound;
    }

    public void setSound(boolean sound) {
        this.sound = sound;
    }

    public boolean isFullscreen() {
        return fullscreen;
    }

    public void setFullscreen(boolean fullscreen) {
        this.fullscreen = fullscreen;
    }

    public boolean isVsync() {
        return vsync;
    }

    public void setVsync(boolean vsync) {
        this.vsync = vsync;
    }

    public boolean isCursorGraphic() {
        return cursorGraphic;
    }

    public void setCursorGraphic(boolean cursorGraphic) {
        this.cursorGraphic = cursorGraphic;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getGraphicsPath() {
        return graphicsPath;
    }

    public void setGraphicsPath(String graphicsPath) {
        this.graphicsPath = graphicsPath;
    }

    public String getDatsPath() {
        return datsPath;
    }

    public String getMapsPath() {
        return datsPath;
    }

    public void setDatsPath(String datsPath) {
        this.datsPath = datsPath;
    }

    public String getInitPath() {
        return initPath;
    }

    public void setInitPath(String initPath) {
        this.initPath = initPath;
    }

    public String getMusicPath() {
        return musicPath;
    }

    public void setMusicPath(String musicPath) {
        this.musicPath = musicPath;
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public void setScreenWidth(int screenWidth) {
        this.screenWidth = screenWidth;
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    public void setScreenHeight(int screenHeight) {
        this.screenHeight = screenHeight;
    }

    public int getClientWidth() {
        return clientWidth;
    }

    public void setClientWidth(int clientWidth) {
        this.clientWidth = clientWidth;
    }

    public int getClientHeight() {
        return clientHeight;
    }

    public void setClientHeight(int clientHeight) {
        this.clientHeight = clientHeight;
    }

    public String getLastMapPath() {
        return lastMapPath;
    }

    public void setLastMapPath(String lastMapPath) {
        this.lastMapPath = lastMapPath;
        addRecentMap(lastMapPath);
    }

    public java.util.List<String> getRecentMaps() {
        return recentMaps;
    }

    private void addRecentMap(String path) {
        if (path == null || path.isEmpty())
            return;
        recentMaps.remove(path);
        recentMaps.add(0, path);
        if (recentMaps.size() > MAX_RECENT_MAPS) {
            recentMaps.remove(recentMaps.size() - 1);
        }
    }

    /**
     * Escribe una opcion con su valor asociado en un objeto {@code BufferedWriter}.
     *
     * @param writer objeto {@code BufferedWriter} que sera utilizado para escribir
     *               la opcion
     * @param option nombre de la opcion
     * @param value  valor de la opcion
     */
    private void write(BufferedWriter writer, String option, Object value) throws IOException {
        writer.write(option + " = " + value);
        writer.newLine();
    }

    /**
     * Carga una opcion.
     *
     * @param option nombre de la opcion
     * @param value  valor de la opcion
     */
    private void load(String option, String value) {
        switch (option) {
            case "Music" -> music = Boolean.parseBoolean(value);
            case "Sound" -> sound = Boolean.parseBoolean(value);
            case "GraphicsPath" -> graphicsPath = value;
            case "DatsPath", "MapsPath" -> datsPath = value;
            case "InitPath" -> initPath = value;
            case "MusicPath" -> musicPath = value;
            case "Fullscreen" -> fullscreen = Boolean.parseBoolean(value);
            case "VSYNC" -> vsync = Boolean.parseBoolean(value);
            case "CursorGraphic" -> cursorGraphic = Boolean.parseBoolean(value);
            case "Language" -> {
                if ("es".equalsIgnoreCase(value)) {
                    language = "es_ES";
                } else if ("en".equalsIgnoreCase(value)) {
                    language = "en_US";
                } else {
                    language = value;
                }
            }
            case "ScreenWidth" -> screenWidth = Integer.parseInt(value);
            case "ScreenHeight" -> screenHeight = Integer.parseInt(value);
            case "ClientWidth" -> clientWidth = Integer.parseInt(value);
            case "ClientHeight" -> clientHeight = Integer.parseInt(value);
            case "LastMapPath" -> lastMapPath = value;
            case "AmbientR" -> ambientR = Float.parseFloat(value);
            case "AmbientG" -> ambientG = Float.parseFloat(value);
            case "AmbientB" -> ambientB = Float.parseFloat(value);
            case "RenderLayer1" -> renderSettings.getShowLayer()[0] = Boolean.parseBoolean(value);
            case "RenderLayer2" -> renderSettings.getShowLayer()[1] = Boolean.parseBoolean(value);
            case "RenderLayer3" -> renderSettings.getShowLayer()[2] = Boolean.parseBoolean(value);
            case "RenderLayer4" -> renderSettings.getShowLayer()[3] = Boolean.parseBoolean(value);
            case "RenderShowNPCs" -> renderSettings.setShowNPCs(Boolean.parseBoolean(value));
            case "RenderShowObjects" -> renderSettings.setShowOJBs(Boolean.parseBoolean(value));
            case "RenderShowTriggers" -> renderSettings.setShowTriggers(Boolean.parseBoolean(value));
            case "RenderShowTranslation" -> renderSettings.setShowMapTransfer(Boolean.parseBoolean(value));
            case "RenderShowBlock" -> renderSettings.setShowBlock(Boolean.parseBoolean(value));
            case "RenderBlockOpacity" -> renderSettings.setBlockOpacity(Float.parseFloat(value));
            case "RenderGhostOpacity" -> renderSettings.setGhostOpacity(Float.parseFloat(value));
            case "RenderShowGrid" -> renderSettings.setShowGrid(Boolean.parseBoolean(value));
            case "RenderShowNpcBreathing" -> renderSettings.setShowNpcBreathing(Boolean.parseBoolean(value));
            case "IgnoredObjTypes" -> {
                ignoredObjTypes.clear();
                if (!value.isEmpty()) {
                    java.util.Arrays.stream(value.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .map(Integer::parseInt)
                            .forEach(ignoredObjTypes::add);
                }
            }
            default -> {
                if (option.startsWith("Recent")) {
                    if (!recentMaps.contains(value) && new java.io.File(value).exists()) {
                        recentMaps.add(value);
                    }
                } else {
                    Logger.warn("Opción desconocida ignorada: {}", option);
                }
            }
        }
    }

    public float getAmbientR() {
        return ambientR;
    }

    public void setAmbientR(float ambientR) {
        this.ambientR = ambientR;
    }

    public float getAmbientG() {
        return ambientG;
    }

    public void setAmbientG(float ambientG) {
        this.ambientG = ambientG;
    }

    public float getAmbientB() {
        return ambientB;
    }

    public void setAmbientB(float ambientB) {
        this.ambientB = ambientB;
    }

    public java.util.Set<Integer> getIgnoredObjTypes() {
        return ignoredObjTypes;
    }
}
