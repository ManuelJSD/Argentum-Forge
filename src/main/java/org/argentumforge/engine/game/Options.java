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

    private String configPath = "resources/options.ini";

    private boolean fullscreen = false;
    private boolean vsync = true;
    private boolean dockingEnabled = true;
    private String visualTheme = "MODERN";

    private String language = "es_ES";
    private String graphicsPath = "";
    private String datsPath = "";
    private String initPath = "";
    private String musicPath = "";
    private int screenWidth = 1366;
    private int screenHeight = 768;
    private int clientWidth = 17;
    private int clientHeight = 13;
    private String lastMapPath = ".";
    private float ambientR = 1.0f;
    private float ambientG = 1.0f;
    private float ambientB = 1.0f;
    private int moveSpeedNormal = 32;
    private int moveSpeedWalk = 8;
    private java.util.List<String> recentMaps = new java.util.ArrayList<>();
    private static final int MAX_RECENT_MAPS = 10;
    private boolean autoSaveEnabled = false;
    private int autoSaveIntervalMinutes = 5;
    private java.util.Set<Integer> ignoredObjTypes = new java.util.HashSet<>(
            java.util.Arrays.asList(4, 6, 8, 10, 15, 20, 22, 27, 28));

    // Updates
    private boolean checkPreReleases = true;
    private String ignoredUpdateTag = "";

    // Appearance
    private int userBody = 1;
    private int userHead = 4;
    private int userWaterBody = 84;

    private final RenderSettings renderSettings = new RenderSettings();

    // Console Options
    private int consoleWidth = 555;
    private int consoleHeight = 98;
    private float consoleFontSize = 1.0f; // Scale
    private float consoleOpacity = 0.5f; // Background opacity
    private boolean consoleShowTimestamps = true;
    // Console Colors (RGBA)
    private float[] consoleColorInfo = { 1.0f, 1.0f, 1.0f, 1.0f }; // White
    private float[] consoleColorWarning = { 1.0f, 1.0f, 0.0f, 1.0f }; // Yellow
    private float[] consoleColorError = { 1.0f, 0.2f, 0.2f, 1.0f }; // Red
    private float[] consoleColorCommand = { 0.0f, 1.0f, 1.0f, 1.0f }; // Cyan

    public void setConfigPath(String path) {
        this.configPath = path;
    }

    public String getConfigPath() {
        return configPath;
    }

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
        File file = new File(configPath);
        // Asegurar que exista la carpeta padre si es una ruta compleja
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }

        // Resetear a valores por defecto antes de cargar
        // Esto evita que opciones del perfil anterior "se filtren" al nuevo
        resetToDefaults();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
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
                    configPath);
            save(); // Guarda defaults en el nuevo path
        }
    }

    /**
     * Resetea todas las opciones a sus valores por defecto.
     * Vital al cambiar de perfil.
     */
    private void resetToDefaults() {
        graphicsPath = "";
        datsPath = "";
        initPath = "";
        musicPath = "";

        // UI Defaults
        fullscreen = false;
        vsync = true;
        dockingEnabled = true;
        visualTheme = "MODERN";
        language = "es_ES";

        // Resolution
        screenWidth = 1366;
        screenHeight = 768;
        clientWidth = 17;
        clientHeight = 13;

        // Map
        lastMapPath = ".";
        recentMaps.clear();

        // Ambient
        ambientR = 1.0f;
        ambientG = 1.0f;
        ambientB = 1.0f;

        // Gameplay
        moveSpeedNormal = 32;
        moveSpeedWalk = 8;
        autoSaveEnabled = false;
        autoSaveIntervalMinutes = 5;

        // User
        userBody = 1;
        userHead = 4;
        userWaterBody = 84;

        // Ignored Objs
        ignoredObjTypes.clear();
        ignoredObjTypes.addAll(java.util.Arrays.asList(4, 6, 8, 10, 15, 20, 22, 27, 28));

        // Updates
        checkPreReleases = true;
        ignoredUpdateTag = "";

        // Reset RenderSettings
        renderSettings.resetToDefaults();

        // Console Defaults
        consoleWidth = 555;
        consoleHeight = 98;
        consoleFontSize = 1.0f;
        consoleOpacity = 0.5f;
        consoleShowTimestamps = true;
        consoleColorInfo = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };
        consoleColorWarning = new float[] { 1.0f, 1.0f, 0.0f, 1.0f };
        consoleColorError = new float[] { 1.0f, 0.2f, 0.2f, 1.0f };
        consoleColorCommand = new float[] { 0.0f, 1.0f, 1.0f, 1.0f };
    }

    /**
     * Verifica si es la primera ejecución del editor (para este perfil).
     * 
     * @return true si el archivo de config no existe, false en caso contrario
     */
    public boolean isFirstRun() {
        File optionsFile = new File(configPath);
        return !optionsFile.exists();
    }

    /**
     * Guarda las opciones.
     */
    /**
     * Guarda las opciones.
     */
    public void save() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(configPath))) {

            write(writer, "GraphicsPath", graphicsPath);
            write(writer, "DatsPath", datsPath);
            write(writer, "InitPath", initPath);
            write(writer, "MusicPath", musicPath);
            write(writer, "Fullscreen", fullscreen);
            write(writer, "VSYNC", vsync);
            write(writer, "DockingEnabled", dockingEnabled);
            write(writer, "VisualTheme", visualTheme);

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
            write(writer, "RenderBlockIndicatorStyle", renderSettings.getBlockIndicatorStyle().name());
            write(writer, "RenderTransferIndicatorStyle", renderSettings.getTransferIndicatorStyle().name());
            write(writer, "RenderGhostOpacity", renderSettings.getGhostOpacity());
            write(writer, "RenderShowGrid", renderSettings.isShowGrid());
            write(writer, "RenderGridColorR", renderSettings.getGridColor()[0]);
            write(writer, "RenderGridColorG", renderSettings.getGridColor()[1]);
            write(writer, "RenderGridColorB", renderSettings.getGridColor()[2]);
            write(writer, "RenderGridColorA", renderSettings.getGridColor()[3]);

            write(writer, "RenderShowMajorGrid", renderSettings.isShowMajorGrid());
            write(writer, "RenderGridMajorInterval", renderSettings.getGridMajorInterval());
            write(writer, "RenderGridMajorColorR", renderSettings.getGridMajorColor()[0]);
            write(writer, "RenderGridMajorColorG", renderSettings.getGridMajorColor()[1]);
            write(writer, "RenderGridMajorColorB", renderSettings.getGridMajorColor()[2]);
            write(writer, "RenderGridMajorColorA", renderSettings.getGridMajorColor()[3]);
            write(writer, "RenderAdaptiveGrid", renderSettings.isAdaptiveGrid());

            write(writer, "RenderShowNpcBreathing", renderSettings.isShowNpcBreathing());
            write(writer, "RenderDisableAnimations", renderSettings.isDisableAnimations());
            write(writer, "PhotoVignette", renderSettings.isPhotoVignette());
            write(writer, "PhotoShadows", renderSettings.isPhotoShadows());
            write(writer, "PhotoWater", renderSettings.isPhotoWater());
            write(writer, "VignetteIntensity", renderSettings.getVignetteIntensity());

            write(writer, "ViewportOverlayEnabled", renderSettings.isShowViewportOverlay());
            write(writer, "ViewportWidth", renderSettings.getViewportWidth());
            write(writer, "ViewportHeight", renderSettings.getViewportHeight());
            write(writer, "ViewportColorR", renderSettings.getViewportColor()[0]);
            write(writer, "ViewportColorG", renderSettings.getViewportColor()[1]);
            write(writer, "ViewportColorB", renderSettings.getViewportColor()[2]);
            write(writer, "ViewportColorA", renderSettings.getViewportColor()[3]);

            write(writer, "AutoSaveEnabled", autoSaveEnabled);
            write(writer, "AutoSaveInterval", autoSaveIntervalMinutes);

            write(writer, "MoveSpeedNormal", moveSpeedNormal);
            write(writer, "MoveSpeedWalk", moveSpeedWalk);

            write(writer, "UserBody", userBody);
            write(writer, "UserHead", userHead);
            write(writer, "UserWaterBody", userWaterBody);

            for (int i = 0; i < recentMaps.size(); i++) {
                write(writer, "Recent" + (i + 1), recentMaps.get(i));
            }

            // Save Ignored Obj Types
            // Save as comma separated string for compactness
            String ignoredStr = ignoredObjTypes.stream().map(String::valueOf)
                    .collect(java.util.stream.Collectors.joining(","));
            write(writer, "IgnoredObjTypes", ignoredStr);

            write(writer, "CheckPreReleases", checkPreReleases);
            write(writer, "CheckPreReleases", checkPreReleases);
            write(writer, "IgnoredUpdateTag", ignoredUpdateTag);

            // Console
            write(writer, "ConsoleWidth", consoleWidth);
            write(writer, "ConsoleHeight", consoleHeight);
            write(writer, "ConsoleFontSize", consoleFontSize);
            write(writer, "ConsoleOpacity", consoleOpacity);
            write(writer, "ConsoleShowTimestamps", consoleShowTimestamps);

            write(writer, "ConsoleColorInfo R", consoleColorInfo[0]);
            write(writer, "ConsoleColorInfo G", consoleColorInfo[1]);
            write(writer, "ConsoleColorInfo B", consoleColorInfo[2]);

            write(writer, "ConsoleColorWarning R", consoleColorWarning[0]);
            write(writer, "ConsoleColorWarning G", consoleColorWarning[1]);
            write(writer, "ConsoleColorWarning B", consoleColorWarning[2]);

            write(writer, "ConsoleColorError R", consoleColorError[0]);
            write(writer, "ConsoleColorError G", consoleColorError[1]);
            write(writer, "ConsoleColorError B", consoleColorError[2]);

            write(writer, "ConsoleColorCommand R", consoleColorCommand[0]);
            write(writer, "ConsoleColorCommand G", consoleColorCommand[1]);
            write(writer, "ConsoleColorCommand B", consoleColorCommand[2]);
        } catch (IOException e) {
            Logger.error("¡No se pudo escribir en el archivo de opciones: {}!", configPath);
        }
    }

    public RenderSettings getRenderSettings() {
        return renderSettings;
    }

    private boolean music = true;
    private boolean sound = true;

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

    public boolean isDockingEnabled() {
        return dockingEnabled;
    }

    public void setDockingEnabled(boolean dockingEnabled) {
        this.dockingEnabled = dockingEnabled;
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

    public int getMoveSpeedNormal() {
        return moveSpeedNormal;
    }

    public void setMoveSpeedNormal(int moveSpeedNormal) {
        this.moveSpeedNormal = moveSpeedNormal;
    }

    public int getMoveSpeedWalk() {
        return moveSpeedWalk;
    }

    public void setMoveSpeedWalk(int moveSpeedWalk) {
        this.moveSpeedWalk = moveSpeedWalk;
    }

    public void increaseSpeed() {
        if (org.argentumforge.engine.game.User.INSTANCE.isWalkingmode()) {
            moveSpeedWalk = Math.min(moveSpeedWalk + 1, 32);
        } else {
            moveSpeedNormal = Math.min(moveSpeedNormal + 1, 128);
        }
    }

    public void decreaseSpeed() {
        if (org.argentumforge.engine.game.User.INSTANCE.isWalkingmode()) {
            moveSpeedWalk = Math.max(moveSpeedWalk - 1, 1);
        } else {
            moveSpeedNormal = Math.max(moveSpeedNormal - 1, 1);
        }
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

            case "GraphicsPath" -> graphicsPath = value;
            case "DatsPath", "MapsPath" -> datsPath = value;
            case "InitPath" -> initPath = value;
            case "MusicPath" -> musicPath = value;
            case "Fullscreen" -> fullscreen = Boolean.parseBoolean(value);
            case "VSYNC" -> vsync = Boolean.parseBoolean(value);
            case "DockingEnabled" -> dockingEnabled = Boolean.parseBoolean(value);
            case "VisualTheme" -> visualTheme = value;

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
            case "RenderGridColorR" -> renderSettings.getGridColor()[0] = Float.parseFloat(value);
            case "RenderGridColorG" -> renderSettings.getGridColor()[1] = Float.parseFloat(value);
            case "RenderGridColorB" -> renderSettings.getGridColor()[2] = Float.parseFloat(value);
            case "RenderGridColorA" -> renderSettings.getGridColor()[3] = Float.parseFloat(value);

            case "RenderShowMajorGrid" -> renderSettings.setShowMajorGrid(Boolean.parseBoolean(value));
            case "RenderGridMajorInterval" -> renderSettings.setGridMajorInterval(Integer.parseInt(value));
            case "RenderGridMajorColorR" -> renderSettings.getGridMajorColor()[0] = Float.parseFloat(value);
            case "RenderGridMajorColorG" -> renderSettings.getGridMajorColor()[1] = Float.parseFloat(value);
            case "RenderGridMajorColorB" -> renderSettings.getGridMajorColor()[2] = Float.parseFloat(value);
            case "RenderGridMajorColorA" -> renderSettings.getGridMajorColor()[3] = Float.parseFloat(value);
            case "RenderAdaptiveGrid" -> renderSettings.setAdaptiveGrid(Boolean.parseBoolean(value));

            case "RenderShowNpcBreathing" -> renderSettings.setShowNpcBreathing(Boolean.parseBoolean(value));
            case "RenderDisableAnimations" -> renderSettings.setDisableAnimations(Boolean.parseBoolean(value));
            case "PhotoVignette" -> renderSettings.setPhotoVignette(Boolean.parseBoolean(value));
            case "PhotoShadows" -> renderSettings.setPhotoShadows(Boolean.parseBoolean(value));
            case "PhotoWater" -> renderSettings.setPhotoWater(Boolean.parseBoolean(value));
            case "VignetteIntensity" -> renderSettings.setVignetteIntensity(Float.parseFloat(value));
            case "ViewportOverlayEnabled" -> renderSettings.setShowViewportOverlay(Boolean.parseBoolean(value));
            case "ViewportWidth" -> renderSettings.setViewportWidth(Integer.parseInt(value));
            case "ViewportHeight" -> renderSettings.setViewportHeight(Integer.parseInt(value));
            case "ViewportColorR" -> renderSettings.getViewportColor()[0] = Float.parseFloat(value);
            case "ViewportColorG" -> renderSettings.getViewportColor()[1] = Float.parseFloat(value);
            case "ViewportColorB" -> renderSettings.getViewportColor()[2] = Float.parseFloat(value);
            case "ViewportColorA" -> renderSettings.getViewportColor()[3] = Float.parseFloat(value);
            case "RenderBlockIndicatorStyle" -> renderSettings.setBlockIndicatorStyle(
                    org.argentumforge.engine.renderer.RenderSettings.IndicatorStyle.valueOf(value));
            case "RenderTransferIndicatorStyle" -> renderSettings.setTransferIndicatorStyle(
                    org.argentumforge.engine.renderer.RenderSettings.IndicatorStyle.valueOf(value));
            case "AutoSaveEnabled" -> autoSaveEnabled = Boolean.parseBoolean(value);
            case "AutoSaveInterval" -> autoSaveIntervalMinutes = Integer.parseInt(value);
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
            case "MoveSpeedNormal" -> moveSpeedNormal = Integer.parseInt(value);
            case "MoveSpeedWalk" -> moveSpeedWalk = Integer.parseInt(value);
            case "UserBody" -> userBody = Integer.parseInt(value);
            case "UserHead" -> userHead = Integer.parseInt(value);
            case "UserWaterBody" -> userWaterBody = Integer.parseInt(value);
            case "CheckPreReleases" -> checkPreReleases = Boolean.parseBoolean(value);
            case "IgnoredUpdateTag" -> ignoredUpdateTag = value;

            // Console
            case "ConsoleWidth" -> consoleWidth = Integer.parseInt(value);
            case "ConsoleHeight" -> consoleHeight = Integer.parseInt(value);
            case "ConsoleFontSize" -> consoleFontSize = Float.parseFloat(value);
            case "ConsoleOpacity" -> consoleOpacity = Float.parseFloat(value);
            case "ConsoleShowTimestamps" -> consoleShowTimestamps = Boolean.parseBoolean(value);

            case "ConsoleColorInfo R" -> consoleColorInfo[0] = Float.parseFloat(value);
            case "ConsoleColorInfo G" -> consoleColorInfo[1] = Float.parseFloat(value);
            case "ConsoleColorInfo B" -> consoleColorInfo[2] = Float.parseFloat(value);

            case "ConsoleColorWarning R" -> consoleColorWarning[0] = Float.parseFloat(value);
            case "ConsoleColorWarning G" -> consoleColorWarning[1] = Float.parseFloat(value);
            case "ConsoleColorWarning B" -> consoleColorWarning[2] = Float.parseFloat(value);

            case "ConsoleColorError R" -> consoleColorError[0] = Float.parseFloat(value);
            case "ConsoleColorError G" -> consoleColorError[1] = Float.parseFloat(value);
            case "ConsoleColorError B" -> consoleColorError[2] = Float.parseFloat(value);

            case "ConsoleColorCommand R" -> consoleColorCommand[0] = Float.parseFloat(value);
            case "ConsoleColorCommand G" -> consoleColorCommand[1] = Float.parseFloat(value);
            case "ConsoleColorCommand B" -> consoleColorCommand[2] = Float.parseFloat(value);
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

    public int getUserBody() {
        return userBody;
    }

    public void setUserBody(int userBody) {
        this.userBody = userBody;
    }

    public int getUserHead() {
        return userHead;
    }

    public void setUserHead(int userHead) {
        this.userHead = userHead;
    }

    public int getUserWaterBody() {
        return userWaterBody;
    }

    public void setUserWaterBody(int userWaterBody) {
        this.userWaterBody = userWaterBody;
    }

    public boolean isAutoSaveEnabled() {
        return autoSaveEnabled;
    }

    public void setAutoSaveEnabled(boolean autoSaveEnabled) {
        this.autoSaveEnabled = autoSaveEnabled;
    }

    public int getAutoSaveIntervalMinutes() {
        return autoSaveIntervalMinutes;
    }

    public void setAutoSaveIntervalMinutes(int autoSaveIntervalMinutes) {
        this.autoSaveIntervalMinutes = autoSaveIntervalMinutes;
    }

    public String getVisualTheme() {
        return visualTheme;
    }

    public void setVisualTheme(String visualTheme) {
        this.visualTheme = visualTheme;
    }

    public boolean isCheckPreReleases() {
        return checkPreReleases;
    }

    public void setCheckPreReleases(boolean checkPreReleases) {
        this.checkPreReleases = checkPreReleases;
    }

    public String getIgnoredUpdateTag() {
        return ignoredUpdateTag;
    }

    public void setIgnoredUpdateTag(String ignoredUpdateTag) {
        this.ignoredUpdateTag = ignoredUpdateTag;
    }

    public int getConsoleWidth() {
        return consoleWidth;
    }

    public void setConsoleWidth(int consoleWidth) {
        this.consoleWidth = consoleWidth;
    }

    public int getConsoleHeight() {
        return consoleHeight;
    }

    public void setConsoleHeight(int consoleHeight) {
        this.consoleHeight = consoleHeight;
    }

    public float getConsoleFontSize() {
        return consoleFontSize;
    }

    public void setConsoleFontSize(float consoleFontSize) {
        this.consoleFontSize = consoleFontSize;
    }

    public float getConsoleOpacity() {
        return consoleOpacity;
    }

    public void setConsoleOpacity(float consoleOpacity) {
        this.consoleOpacity = consoleOpacity;
    }

    public boolean isConsoleShowTimestamps() {
        return consoleShowTimestamps;
    }

    public void setConsoleShowTimestamps(boolean consoleShowTimestamps) {
        this.consoleShowTimestamps = consoleShowTimestamps;
    }

    public float[] getConsoleColorInfo() {
        return consoleColorInfo;
    }

    public void setConsoleColorInfo(float[] consoleColorInfo) {
        this.consoleColorInfo = consoleColorInfo;
    }

    public float[] getConsoleColorWarning() {
        return consoleColorWarning;
    }

    public void setConsoleColorWarning(float[] consoleColorWarning) {
        this.consoleColorWarning = consoleColorWarning;
    }

    public float[] getConsoleColorError() {
        return consoleColorError;
    }

    public void setConsoleColorError(float[] consoleColorError) {
        this.consoleColorError = consoleColorError;
    }

    public float[] getConsoleColorCommand() {
        return consoleColorCommand;
    }

    public void setConsoleColorCommand(float[] consoleColorCommand) {
        this.consoleColorCommand = consoleColorCommand;
    }

    public void resetConsoleToDefaults() {
        consoleWidth = 555;
        consoleHeight = 98;
        consoleFontSize = 1.0f;
        consoleOpacity = 0.5f;
        consoleShowTimestamps = true;
        consoleColorInfo = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };
        consoleColorWarning = new float[] { 1.0f, 1.0f, 0.0f, 1.0f };
        consoleColorError = new float[] { 1.0f, 0.2f, 0.2f, 1.0f };
        consoleColorCommand = new float[] { 0.0f, 1.0f, 1.0f, 1.0f };
    }
}
