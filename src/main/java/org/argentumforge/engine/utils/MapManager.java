package org.argentumforge.engine.utils;

import org.argentumforge.engine.game.Options;
import org.argentumforge.engine.game.models.Character;
import org.argentumforge.engine.game.models.Direction;
import org.argentumforge.engine.renderer.Surface;
import org.argentumforge.engine.utils.inits.*;
import org.tinylog.Logger;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.argentumforge.engine.game.models.Character.eraseAllChars;
import org.argentumforge.engine.gui.DialogManager;
import java.io.File;

/**
 * Clase responsable de la gestión de archivos de mapa (.map, .inf, .dat).
 * 
 * Centraliza la lógica de carga, guardado e inicialización de los datos del
 * escenario, incluyendo capas de gráficos, bloqueos, triggers y entidades.
 */
public final class MapManager {

    private MapManager() {
        // Clase de utilidad
    }

    /**
     * Opciones detalladas para el guardado del mapa.
     */
    public static class MapSaveOptions {
        private short version = 1;
        private boolean useLongIndices = false;
        private boolean includeHeader = true;

        public static MapSaveOptions standard() {
            MapSaveOptions opt = new MapSaveOptions();
            opt.version = 1;
            opt.useLongIndices = false;
            opt.includeHeader = true;
            return opt;
        }

        public static MapSaveOptions aoLibre() {
            MapSaveOptions opt = new MapSaveOptions();
            opt.version = 136;
            opt.useLongIndices = true;
            opt.includeHeader = true;
            return opt;
        }

        public static MapSaveOptions extended() {
            MapSaveOptions opt = new MapSaveOptions();
            opt.version = 1;
            opt.useLongIndices = true;
            opt.includeHeader = true;
            return opt;
        }

        public short getVersion() {
            return version;
        }

        public void setVersion(short version) {
            this.version = version;
        }

        public boolean isUseLongIndices() {
            return useLongIndices;
        }

        public void setUseLongIndices(boolean useLongIndices) {
            this.useLongIndices = useLongIndices;
        }

        public boolean isIncludeHeader() {
            return includeHeader;
        }

        public void setIncludeHeader(boolean includeHeader) {
            this.includeHeader = includeHeader;
        }
    }

    /**
     * Carga un mapa por su número desde los recursos empaquetados.
     *
     * @param numMap Número del mapa a cargar.
     */
    public static void loadMap(int numMap) {
        String mapPath = resolveMapPath(numMap);
        if (mapPath != null) {
            loadMap(mapPath);
        } else {
            System.err.println("Map file not found for map " + numMap);
        }
    }

    /**
     * Resuelve la ruta absoluta del archivo de mapa dado su numero.
     * Busca en la carpeta del ultimo mapa abierto, config, o recursos por defecto.
     */
    public static String resolveMapPath(int numMap) {
        // 1. Check folder of last loaded map
        String lastPath = Options.INSTANCE.getLastMapPath();
        if (lastPath != null && !lastPath.isEmpty()) {
            File currentFile = new File(lastPath);
            String mapDir = currentFile.getParent();
            if (mapDir != null) {
                // Try "MapaX.map" and "mapaX.map"
                File try1 = new File(mapDir, "Mapa" + numMap + ".map");
                if (try1.exists())
                    return try1.getAbsolutePath();

                File try2 = new File(mapDir, "mapa" + numMap + ".map");
                if (try2.exists())
                    return try2.getAbsolutePath();
            }
        }

        // 2. Check configured maps path
        String configuredPath = Options.INSTANCE.getMapsPath();
        if (configuredPath != null && !configuredPath.isEmpty()) {
            File try1 = new File(configuredPath, "Mapa" + numMap + ".map");
            if (try1.exists())
                return try1.getAbsolutePath();

            File try2 = new File(configuredPath, "mapa" + numMap + ".map");
            if (try2.exists())
                return try2.getAbsolutePath();
        }

        // 3. Check default resources
        File def1 = new File("resources/maps/mapa" + numMap + ".map");
        if (def1.exists())
            return def1.getAbsolutePath();

        File def2 = new File("resources/maps/Mapa" + numMap + ".map"); // Just in case
        if (def2.exists())
            return def2.getAbsolutePath();

        // 4. Try CWD fallback
        File local = new File("mapa" + numMap + ".map");
        if (local.exists())
            return local.getAbsolutePath();

        return null; // Not found
    }

    /**
     * Estima las opciones de guardado originales basándose en los datos cargados.
     */
    public static MapSaveOptions detectSaveOptions(byte[] data) {
        MapSaveOptions options = new MapSaveOptions();
        if (data.length < 2)
            return options;

        short version = (short) ((data[0] & 0xFF) | ((data[1] & 0xFF) << 8));
        options.setVersion(version);

        options.setIncludeHeader(true); // Asumimos cabecera por defecto en carga estándar

        if (version == 136 || data.length > 50000) {
            options.setUseLongIndices(true);
        } else {
            options.setUseLongIndices(false);
        }
        return options;
    }

    /**
     * Marca el mapa como modificado para el prompt de guardado.
     */
    public static void markAsModified() {
        MapContext context = GameData.getActiveContext();
        if (context != null) {
            context.setModified(true);
            GameData.updateWindowTitle();
        }
    }

    /**
     * Verifica si hay cambios sin guardar.
     */
    public static boolean hasUnsavedChanges() {
        MapContext context = GameData.getActiveContext();
        return context != null && context.isModified();
    }

    /**
     * Limpia la marca de modificaciones.
     */
    public static void markAsSaved() {
        MapContext context = GameData.getActiveContext();
        if (context != null) {
            context.setModified(false);
            GameData.updateWindowTitle();
        }
    }

    /**
     * Verifica si hay cambios sin guardar y pregunta al usuario si desea continuar.
     * 
     * @return true si es seguro continuar, false si el usuario canceló.
     */
    /**
     * Verifica si hay cambios sin guardar y ejecuta callback.
     * Reemplazo asincrónico para checkUnsavedChanges bloqueante.
     * 
     * @param onContinue Runnable a ejecutar si se puede continuar (ya sea guardando
     *                   o descartando).
     * @param onCancel   Runnable a ejecutar si el usuario cancela la acción.
     */
    public static void checkUnsavedChangesAsync(Runnable onContinue, Runnable onCancel) {
        if (!hasUnsavedChanges()) {
            if (onContinue != null)
                onContinue.run();
            return;
        }

        DialogManager.getInstance().showYesNoCancel(
                "Cambios sin guardar",
                "Hay cambios sin guardar en el mapa actual.\n¿Desea guardarlos antes de continuar?",
                () -> {
                    // YES -> Save then Continue
                    MapFileUtils.quickSaveMap(
                            () -> { // On Success
                                if (onContinue != null)
                                    onContinue.run();
                            },
                            () -> { // On Failure
                                if (onCancel != null)
                                    onCancel.run();
                            });
                },
                () -> {
                    // NO -> Discard changes (just continue)
                    if (onContinue != null)
                        onContinue.run();
                },
                () -> {
                    // CANCEL
                    if (onCancel != null)
                        onCancel.run();
                });
    }

    /**
     * @deprecated Use {@link #checkUnsavedChangesAsync(Runnable, Runnable)}
     *             instead.
     *             This method now always returns false to avoid blocking, but logs
     *             a warning.
     */
    @Deprecated
    public static boolean checkUnsavedChanges() {
        Logger.warn("checkUnsavedChanges() called synchronously! Use async version.");
        return true; // Forzamos true para no bloquear, asumiendo "descartar" si se llama por error.
    }

    /**
     * Carga un mapa completo (capas, propiedades y entidades) desde una ruta.
     * 
     * @param filePath Ruta absoluta al archivo .map
     */
    /**
     * Carga un mapa de forma asíncrona para no bloquear la interfaz.
     * Muestra un modal de carga durante el proceso.
     *
     * @param filePath   Ruta absoluta al archivo .map
     * @param onComplete Callback opcional al finalizar
     */
    public static void loadMapAsync(String filePath, Runnable onComplete) {
        org.argentumforge.engine.gui.components.LoadingModal.getInstance()
                .show("Cargando mapa " + new File(filePath).getName() + "...");

        java.util.concurrent.CompletableFuture.runAsync(() -> {
            Logger.info("Inicio de carga asíncrona del mapa: {}", filePath);
            try {
                // FASE 1: Carga Pesada (Background)
                // Leemos y parseamos todos los datos SIN tocar OpenGL ni estado global critico

                GameData.clearActiveContext(); // Seguro? Preferiblemente no tocar static aqui, pero clearActiveContext
                                               // es ligero.
                // Mejor crear el contexto nuevo y reemplazar al final.

                Character[] newCharList = new Character[10001];
                for (int i = 0; i < newCharList.length; i++) {
                    newCharList[i] = new Character();
                }

                byte[] data = Files.readAllBytes(Path.of(filePath));
                MapSaveOptions detectedOptions = detectSaveOptions(data);
                MapData[][] newMapData = initMap(data, detectedOptions);
                int particlesLoaded = countParticles(newMapData); // Helper nuevo o inferido

                // Reserve User Slot
                int userCharIdx = org.argentumforge.engine.game.User.INSTANCE.getUserCharIndex();
                if (userCharIdx <= 0)
                    userCharIdx = 1;
                newCharList[userCharIdx].setActive(true);

                String basePath = filePath.substring(0, filePath.lastIndexOf('.'));
                String datPath = basePath + ".dat";
                String infPath = basePath + ".inf";

                MapProperties newMapProperties;
                if (Files.exists(Path.of(datPath))) {
                    newMapProperties = loadMapProperties(datPath);
                } else {
                    newMapProperties = new MapProperties();
                }

                if (Files.exists(Path.of(infPath))) {
                    loadMapInfo(infPath, newMapData, newCharList);
                }

                MapContext context = new MapContext(filePath, newMapData, newMapProperties, newCharList);
                context.setLastChar((short) 0);
                context.setSaveOptions(detectedOptions);

                // Pasar resultados a fase 2
                MapLoadingResult result = new MapLoadingResult(context, particlesLoaded);

                // FASE 2: Aplicación (Main Thread) -> Usamos un Task Queue en Engine o
                // bloqueamos un frame
                org.argentumforge.engine.Engine.INSTANCE.runOnMainThread(() -> {
                    applyMap(result);
                    // Actualizar opciones persistentes
                    Options.INSTANCE.setLastMapPath(filePath);
                    Options.INSTANCE.save();

                    if (onComplete != null)
                        onComplete.run();
                    org.argentumforge.engine.gui.components.LoadingModal.getInstance().hide();
                    Logger.info("Carga asíncrona completada.");
                });

            } catch (Exception e) {
                Logger.error(e, "Error en carga asíncrona de mapa");
                org.argentumforge.engine.Engine.INSTANCE.runOnMainThread(() -> {
                    org.argentumforge.engine.gui.components.LoadingModal.getInstance().hide();
                    DialogManager.getInstance().showError("Error", "No se pudo cargar el mapa:\n" + e.getMessage());
                });
            }
        });
    }

    private static class MapLoadingResult {
        MapContext context;
        int particlesLoaded;

        public MapLoadingResult(MapContext c, int p) {
            context = c;
            particlesLoaded = p;
        }
    }

    private static void applyMap(MapLoadingResult result) {
        GameData.setActiveContext(result.context);

        // Limpiar texturas viejas (OpenGL)
        Surface.INSTANCE.deleteAllTextures();

        // Configurar particulas
        if (result.particlesLoaded > 0) {
            GameData.options.getRenderSettings().setShowParticles(true);
        }

        MapManager.markAsSaved();
        org.argentumforge.engine.utils.editor.commands.CommandManager.getInstance().clearHistory();

        // Teleport Usuario
        org.argentumforge.engine.game.User user = org.argentumforge.engine.game.User.INSTANCE;
        if (user.getUserPos().getX() == 0 || user.getUserPos().getY() == 0) {
            user.teleport(50, 50);
        }
    }

    private static int countParticles(MapData[][] map) {
        int count = 0;
        for (int x = 1; x < map.length; x++) {
            for (int y = 1; y < map[0].length; y++) {
                if (map[x][y].getParticleIndex() > 0)
                    count++;
            }
        }
        return count;
    }

    /**
     * Carga un mapa completo de forma síncrona (Legacy).
     * 
     * @deprecated Use loadMapAsync en su lugar.
     */
    public static void loadMap(String filePath) {
        Logger.info("Cargando mapa desde: {}", filePath);

        // Guardar la ruta del último mapa para futuras sesiones y añadir al historial
        Options.INSTANCE.setLastMapPath(filePath);
        Options.INSTANCE.save();

        try {
            GameData.clearActiveContext();

            // Reiniciar estado para el nuevo mapa (sin borrar el anterior array)
            // org.argentumforge.engine.game.models.Character.lastChar = 0; // Handled in
            // context or locally

            Character[] newCharList = new Character[10001];
            for (int i = 0; i < newCharList.length; i++) {
                newCharList[i] = new org.argentumforge.engine.game.models.Character();
            }

            MapProperties newMapProperties = new MapProperties();

            // Cargar archivo principal de capas (.map)
            // Cargar archivo principal de capas (.map)
            byte[] data = Files.readAllBytes(Path.of(filePath));
            MapSaveOptions detectedOptions = detectSaveOptions(data);
            MapData[][] newMapData = initMap(data, detectedOptions);

            // Reserve User Slot (Index 1 usually) AFTER initMap (which wipes chars) but
            // BEFORE loading entities
            // We only mark it active to prevent NPCs from taking it. We do NOT fetch
            // position or write to map yet.
            int userCharIdx = org.argentumforge.engine.game.User.INSTANCE.getUserCharIndex();
            if (userCharIdx <= 0)
                userCharIdx = 1;
            newCharList[userCharIdx].setActive(true);

            // Preparar para buscar archivos complementarios (.inf y .dat)
            String basePath = filePath.substring(0, filePath.lastIndexOf('.'));
            String datPath = basePath + ".dat";
            String infPath = basePath + ".inf";

            // Intentar cargar propiedades del mapa (.dat)
            // Intentar cargar propiedades del mapa (.dat)
            if (Files.exists(Path.of(datPath))) {
                // Warning: loadMapProperties currently modifies GameData.mapProperties static.
                // We should refactor it or copy values?
                // For now, let's update loadMapProperties later or assume it sets the static
                // one which we might still use temporarily,
                // BUT we want to pass 'newMapProperties' to context.
                // Let's manually parse or update loadMapProperties?
                // Better: Update loadMapProperties to return MapProperties.
                newMapProperties = loadMapProperties(datPath);
            } else {
                newMapProperties = new MapProperties();
                Logger.info("Archivo .dat no encontrado en {}, usando valores por defecto.", datPath);
            }

            // Intentar cargar información de entidades (.inf)
            // Intentar cargar información de entidades (.inf)
            // loadMapInfo needs mapData and charList. It currently uses static.
            // We need to pass them. Refactor loadMapInfo signature.
            // For now, we will defer loadMapInfo call or pass arguments?
            // Refactoring loadMapInfo to accept (infPath, mapData, charList).
            if (Files.exists(Path.of(infPath))) {
                loadMapInfo(infPath, newMapData, newCharList);
            } else {
                Logger.info("Archivo .inf no encontrado en {}, saltando carga de entidades.", infPath);
            }

            // Crear el contexto y registrarlo
            // Crear el contexto y registrarlo
            MapContext context = new MapContext(filePath, newMapData, newMapProperties, newCharList);
            context.setLastChar((short) 0); // Reset lastChar locally
            context.setSaveOptions(detectedOptions);
            GameData.setActiveContext(context);

            // Reiniciar estado de modificaciones
            markAsSaved();
            org.argentumforge.engine.utils.editor.commands.CommandManager.getInstance().clearHistory();

            Logger.info("Mapa cargado exitosamente");

            // Si el usuario está en (0,0), moverlo a (50,50)
            org.argentumforge.engine.game.User user = org.argentumforge.engine.game.User.INSTANCE;
            if (user.getUserPos().getX() == 0 || user.getUserPos().getY() == 0) {
                user.teleport(50, 50);
            }
        } catch (IOException e) {
            Logger.error(e, "No se pudo cargar el mapa desde: {}", filePath);
        }
    }

    /**
     * Guarda el estado actual del mapa en disco, generando los archivos .map, .inf
     * y .dat.
     * 
     * @param filePath Ruta absoluta al archivo .map de destino.
     */
    public static void saveMap(String filePath) {
        // Usar formato del contexto si existe, de lo contrario STANDARD
        MapSaveOptions options = MapSaveOptions.standard();
        MapContext context = GameData.getActiveContext();
        if (context != null && context.getSaveOptions() != null) {
            options = context.getSaveOptions();
        }
        saveMap(filePath, options);
    }

    /**
     * Guarda el estado actual del mapa en disco en el formato especificado.
     * 
     * @param filePath Ruta absoluta al archivo .map de destino.
     * @param options  Opciones de guardado.
     */
    public static void saveMap(String filePath, MapSaveOptions options) {
        Logger.info("Guardando mapa en: {} (V:{}, LongIndices: {}, Header: {})",
                filePath, options.getVersion(), options.isUseLongIndices(), options.isIncludeHeader());

        MapContext context = GameData.getActiveContext();
        if (context != null) {
            context.setSaveOptions(options);
            context.setFilePath(filePath);
        }

        try {
            // Guardar datos de capas (.map)
            saveMapData(filePath, options);

            String basePath = filePath.substring(0, filePath.lastIndexOf('.'));
            String datPath = basePath + ".dat";
            String infPath = basePath + ".inf";

            // Guardar propiedades generales (.dat)
            saveMapProperties(datPath);

            // Guardar información de entidades y triggers (.inf)
            saveMapInfo(infPath);

            // Reiniciar estado de modificaciones
            markAsSaved();

            if (context != null) {
                context.setSavedUndoStackSize(context.getUndoStack().size());
            }

            Logger.info("Mapa guardado exitosamente en: {}", filePath);
            DialogManager.getInstance().showInfo("Guardar Mapa", "Mapa guardado correctamente.");
        } catch (IOException e) {
            Logger.error(e, "Error al guardar el mapa en: {}", filePath);
            DialogManager.getInstance().showError("Error", "Error al guardar el mapa:\n" + e.getMessage());
        }
    }

    /**
     * Inicializa un mapa vacío con las dimensiones especificadas.
     * 
     * @param width  Ancho del mapa (normalmente 100).
     * @param height Alto del mapa (normalmente 100).
     */
    public static void createEmptyMap(int width, int height) {
        Logger.info("Creando mapa vacío de {}x{}", width, height);

        // Validar dimensiones legales
        if (width < GameData.X_MIN_MAP_SIZE || width > GameData.X_MAX_MAP_SIZE ||
                height < GameData.Y_MIN_MAP_SIZE || height > GameData.Y_MAX_MAP_SIZE) {
            Logger.error("Dimensiones de mapa inválidas: {}x{}", width, height);
            return;
        }

        // Guardar estado del contexto actual si existe y desvincularlo
        GameData.clearActiveContext();

        // Reiniciar estado
        Character[] newCharList = new Character[10001];
        for (int i = 0; i < newCharList.length; i++) {
            newCharList[i] = new org.argentumforge.engine.game.models.Character();
        }

        // Inicializar rejilla de datos
        MapData[][] newMapData = new MapData[width + 1][height + 1];
        for (int x = 0; x <= width; x++) {
            for (int y = 0; y <= height; y++) {
                MapData cell = new MapData();
                // Inicializar Capa 1 con Grh 1 (Césped) por defecto
                GameData.initGrh(cell.getLayer(1), 1, true);
                newMapData[x][y] = cell;
            }
        }

        // Resetear propiedades del mapa
        MapProperties newMapProperties = new MapProperties();

        MapContext context = new MapContext("", newMapData, newMapProperties, newCharList);
        context.setLastChar((short) 0);
        context.setSaveOptions(MapSaveOptions.extended());
        GameData.setActiveContext(context);

        // Limpiar recursos de renderizado anteriores
        Surface.INSTANCE.deleteAllTextures();
        org.argentumforge.engine.game.models.Character.eraseAllChars();

        // Reiniciar estado de modificaciones
        markAsSaved();
        org.argentumforge.engine.utils.editor.commands.CommandManager.getInstance().clearHistory();

        Logger.info("Mapa vacío creado ({}x{})", width, height);

        // Teletransportar al usuario al centro (o 50,50)
        org.argentumforge.engine.game.User.INSTANCE.teleport(50, 50);
    }

    /**
     * Procesa los datos binarios del archivo .map para inicializar la rejilla.
     * Lee cabeceras, flags de tile, capas de gráficos, bloqueos y triggers base.
     *
     * @param data    Contenido binario del archivo .map.
     * @param options Opciones de guardado inferidas.
     */
    static MapData[][] initMap(byte[] data, MapSaveOptions options) {
        GameData.reader.init(data);

        MapData[][] newMapData = new MapData[GameData.X_MAX_MAP_SIZE + 1][GameData.Y_MAX_MAP_SIZE + 1];
        for (int y = 0; y <= GameData.Y_MAX_MAP_SIZE; y++) {
            for (int x = 0; x <= GameData.X_MAX_MAP_SIZE; x++) {
                newMapData[x][y] = new MapData();
            }
        }

        // Leer versión y saltar cabecera heredada de VB6
        if (!GameData.reader.hasRemaining(2))
            return newMapData;
        final short mapversion = GameData.reader.readShort();

        // Debug logging for AOLibre analysis
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(data.length, 32); i++) { // Más bytes para ver firma
            sb.append(String.format("%02X ", data[i]));
        }
        Logger.info("Mapa: tamaño={}, versión={}, bytes=[{}]", data.length, mapversion, sb.toString().trim());

        if (options.isIncludeHeader() && GameData.reader.hasRemaining(263)) {
            GameData.reader.skipBytes(263);
        }

        boolean useLongIndices = options.isUseLongIndices();
        int indexSize = useLongIndices ? 4 : 2;

        byte byflags;
        byte bloq;

        // Saltar campos no utilizados en el editor
        if (GameData.reader.hasRemaining(8)) {
            GameData.reader.readShort();
            GameData.reader.readShort();
            GameData.reader.readShort();
            GameData.reader.readShort();
        }

        int particlesLoaded = 0;

        tileLoop: for (int y = 1; y <= 100; y++) {
            for (int x = 1; x <= 100; x++) {
                if (!GameData.reader.hasRemaining(1))
                    break tileLoop;

                int currentPos = GameData.reader.getPosition();
                byflags = GameData.reader.readByte();

                if (y == 1 && x <= 5) {
                    Logger.info("DEBUG Tile ({},{}): Pos={}, Flags={}", x, y, currentPos, byflags);
                }

                // Bit 1: Bloqueo
                bloq = (byte) (byflags & 1);
                newMapData[x][y].setBlocked(bloq == 1);

                // Capa 1 (Siempre presente)
                if (!GameData.reader.hasRemaining(indexSize))
                    break tileLoop;
                int grh1 = useLongIndices ? GameData.reader.readInt() : GameData.reader.readUnsignedShort();
                newMapData[x][y].getLayer(1).setGrhIndex(grh1);
                newMapData[x][y].setLayer(1,
                        GameData.initGrh(newMapData[x][y].getLayer(1),
                                newMapData[x][y].getLayer(1).getGrhIndex(), true));

                // Capa 2
                if ((byte) (byflags & 2) != 0) {
                    if (!GameData.reader.hasRemaining(indexSize))
                        break tileLoop;
                    int grh2 = useLongIndices ? GameData.reader.readInt() : GameData.reader.readUnsignedShort();
                    newMapData[x][y].getLayer(2).setGrhIndex(grh2);
                    newMapData[x][y].setLayer(2,
                            GameData.initGrh(newMapData[x][y].getLayer(2),
                                    newMapData[x][y].getLayer(2).getGrhIndex(), true));

                } else
                    newMapData[x][y].getLayer(2).setGrhIndex(0);

                // Capa 3
                if ((byte) (byflags & 4) != 0) {
                    if (!GameData.reader.hasRemaining(indexSize))
                        break tileLoop;
                    int grh3 = useLongIndices ? GameData.reader.readInt() : GameData.reader.readUnsignedShort();
                    newMapData[x][y].getLayer(3).setGrhIndex(grh3);
                    newMapData[x][y].setLayer(3,
                            GameData.initGrh(newMapData[x][y].getLayer(3),
                                    newMapData[x][y].getLayer(3).getGrhIndex(), true));
                } else
                    newMapData[x][y].getLayer(3).setGrhIndex(0);

                // Capa 4
                if ((byte) (byflags & 8) != 0) {
                    if (!GameData.reader.hasRemaining(indexSize))
                        break tileLoop;
                    int grh4 = useLongIndices ? GameData.reader.readInt() : GameData.reader.readUnsignedShort();
                    newMapData[x][y].getLayer(4).setGrhIndex(grh4);
                    newMapData[x][y].setLayer(4,
                            GameData.initGrh(newMapData[x][y].getLayer(4),
                                    newMapData[x][y].getLayer(4).getGrhIndex(), true));
                } else
                    newMapData[x][y].getLayer(4).setGrhIndex(0);

                // Triggers
                if ((byte) (byflags & 16) != 0) {
                    if (!GameData.reader.hasRemaining(2))
                        break tileLoop;
                    newMapData[x][y].setTrigger(GameData.reader.readUnsignedShort());
                } else
                    newMapData[x][y].setTrigger(0);

                // Bit 5 (Valor 32): Partículas (AOLibre/Versiones nuevas)
                if ((byflags & 32) != 0) {
                    if (!GameData.reader.hasRemaining(2))
                        break tileLoop;
                    int pId = GameData.reader.readShort();
                    newMapData[x][y].setParticleIndex(pId);
                } else {
                    newMapData[x][y].setParticleIndex(0);
                }

                newMapData[x][y].getObjGrh().setGrhIndex(0);
            }
        }

        // Limpiar recursos de renderizado y entidades anteriores
        // Particles check moved to applyMap
        // Surface.INSTANCE.deleteAllTextures(); // Moved to applyMap (Main Thread)
        // eraseAllChars(); // No longer needed/possible as we assume fresh list

        return newMapData;
    }

    /**
     * Carga las propiedades generales del mapa (nombre, música, zona) desde un .dat
     *
     * @param filePath Ruta absoluta al archivo .dat del mapa.
     * @return MapProperties objeto con las propiedades cargadas.
     */
    private static MapProperties loadMapProperties(String filePath) {
        Logger.info("Cargando propiedades del mapa desde: {}", filePath);
        MapProperties props = new MapProperties();

        try (BufferedReader br = Files.newBufferedReader(Path.of(filePath), StandardCharsets.ISO_8859_1)) {
            String line;
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("'") || trimmed.startsWith("#") || trimmed.startsWith(";"))
                    continue;

                // Ignorar cabeceras de sección del archivo .ini
                if (trimmed.startsWith("[") && trimmed.contains("]"))
                    continue;

                int eq = trimmed.indexOf('=');
                if (eq <= 0)
                    continue;

                String key = trimmed.substring(0, eq).trim();
                String value = trimmed.substring(eq + 1).trim();

                try {
                    if (key.equalsIgnoreCase("Name")) {
                        props.setName(value);
                    } else if (key.equalsIgnoreCase("MusicNum")) {
                        props.setMusicIndex(Integer.parseInt(value));
                    } else if (key.equalsIgnoreCase("MagiaSinefecto")) {
                        props.setMagiaSinEfecto(Integer.parseInt(value));
                    } else if (key.equalsIgnoreCase("NoEncriptarMP")) {
                        props.setNoEncriptarMP(Integer.parseInt(value));
                    } else if (key.equalsIgnoreCase("Terreno")) {
                        props.setTerreno(value);
                    } else if (key.equalsIgnoreCase("Zona")) {
                        props.setZona(value);
                    } else if (key.equalsIgnoreCase("Restringir")) {
                        props.setRestringir(value.equalsIgnoreCase("No") ? 0 : Integer.parseInt(value));
                    } else if (key.equalsIgnoreCase("BackUp")) {
                        props.setBackup(Integer.parseInt(value));
                    } else if (key.equalsIgnoreCase("Pk")) {
                        props.setPlayerKiller(Integer.parseInt(value));
                    }
                } catch (NumberFormatException e) {
                    Logger.warn("Error parseando valor '{}' para la clave '{}' en el .dat del mapa.", value, key);
                }
            }
        } catch (IOException e) {
            Logger.error(e, "Error leyendo el archivo .dat del mapa: {}", filePath);
        }

        GameData.mapProperties = props; // Keep redundant static update for safety? Or remove?
        // Let's keep it for now but return the object.
        Logger.info("Propiedades cargadas: Nombre='{}', Música={}, Zona='{}'", props.getName(), props.getMusicIndex(),
                props.getZona());
        return props;
    }

    /**
     * Carga la información de entidades (NPCs, Objetos, Traslados) desde un .inf.
     * Recrea las entidades visuales en el editor.
     *
     * @param filePath Ruta absoluta al archivo .inf
     * @param mapData  Matriz de datos del mapa.
     * @param charList Lista de personajes.
     */
    private static void loadMapInfo(String filePath, MapData[][] mapData, Character[] charList) {
        try {
            byte[] data = Files.readAllBytes(Path.of(filePath));
            GameData.reader.init(data);

            // Saltar cabecera del .inf (10 bytes heredados)
            GameData.reader.skipBytes(10);

            // Recorrer el mapa para cargar la información extendida
            for (int y = GameData.Y_MIN_MAP_SIZE; y <= GameData.Y_MAX_MAP_SIZE; y++) {
                for (int x = GameData.X_MIN_MAP_SIZE; x <= GameData.X_MAX_MAP_SIZE; x++) {
                    if (!GameData.reader.hasRemaining())
                        break;

                    if (mapData[x][y] == null)
                        continue;

                    byte flags = GameData.reader.readByte();

                    // Bit 1: Traslados (Exits)
                    if ((flags & 1) != 0) {
                        mapData[x][y].setExitMap(GameData.reader.readUnsignedShort());
                        mapData[x][y].setExitX(GameData.reader.readUnsignedShort());
                        mapData[x][y].setExitY(GameData.reader.readUnsignedShort());
                    }

                    // Bit 2: NPCs
                    if ((flags & 2) != 0) {
                        int npcIndex = GameData.reader.readUnsignedShort();
                        if (npcIndex > 0) {
                            mapData[x][y].setNpcIndex(npcIndex);
                            NpcData npc = AssetRegistry.npcs.get(npcIndex);
                            if (npc != null) {
                                // Find next open char manually in the local list
                                int openCharIndex = 0;
                                for (int i = 1; i < charList.length; i++) {
                                    if (!charList[i].isActive()) {
                                        openCharIndex = i;
                                        break;
                                    }
                                }

                                if (openCharIndex > 0) {
                                    Character chr = charList[openCharIndex];
                                    chr.setActive(true);
                                    chr.setHeading(Direction.fromID(npc.getHeading()));
                                    chr.getPos().setX(x);
                                    chr.getPos().setY(y);

                                    int bodyIdx = npc.getBody();
                                    if (bodyIdx > 0 && bodyIdx < AssetRegistry.bodyData.length
                                            && AssetRegistry.bodyData[bodyIdx] != null) {
                                        chr.setBody(new BodyData(AssetRegistry.bodyData[bodyIdx]));
                                    }

                                    int headIdx = npc.getHead();
                                    if (headIdx > 0 && headIdx < AssetRegistry.headData.length
                                            && AssetRegistry.headData[headIdx] != null) {
                                        chr.setHead(new HeadData(AssetRegistry.headData[headIdx]));
                                    }

                                    // Update map
                                    mapData[x][y].setCharIndex((short) openCharIndex);
                                }
                            }
                        }
                    }

                    // Bit 4: Objetos
                    if ((flags & 4) != 0) {
                        int objIndex = GameData.reader.readUnsignedShort();
                        int amount = GameData.reader.readUnsignedShort();

                        mapData[x][y].setObjIndex(objIndex);
                        mapData[x][y].setObjAmount(amount);

                        if (objIndex > 0) {
                            ObjData obj = AssetRegistry.objs.get(objIndex);
                            if (obj != null) {
                                GameData.initGrh(mapData[x][y].getObjGrh(), obj.getGrhIndex(), false);
                            } else {
                                Logger.warn("Definición de objeto no encontrada para el índice: {}", objIndex);
                            }
                        }
                    }

                }
            }

        } catch (IOException e) {
            Logger.error(e, "Error al cargar información extendida (.inf) desde: {}", filePath);
        }
    }

    /**
     * Serializa las capas y bloqueos del mapa en formato binario (.map).
     */
    private static void saveMapData(String filePath, MapSaveOptions options) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            java.nio.channels.FileChannel channel = fos.getChannel();

            // 1. Cabecera (273 bytes) si está habilitada
            if (options.isIncludeHeader()) {
                java.nio.ByteBuffer headerBuf = java.nio.ByteBuffer.allocate(273);
                headerBuf.order(java.nio.ByteOrder.LITTLE_ENDIAN);

                headerBuf.putShort(options.getVersion()); // Versión del mapa
                headerBuf.put(new byte[263]); // Relleno cabecera
                headerBuf.putShort((short) 0);
                headerBuf.putShort((short) 0);
                headerBuf.putShort((short) 0);
                headerBuf.putShort((short) 0);

                headerBuf.flip();
                channel.write(headerBuf);
            }

            // 2. Datos de tiles
            boolean useLongIndices = options.isUseLongIndices();
            // Aumentamos buffer para evitar overflow (150KB para short, 250KB para long)
            int estimatedSize = useLongIndices ? 250000 : 150000;
            java.nio.ByteBuffer bodyBuf = java.nio.ByteBuffer.allocate(estimatedSize);
            bodyBuf.order(java.nio.ByteOrder.LITTLE_ENDIAN);

            MapContext context = GameData.getActiveContext();
            if (context == null || context.getMapData() == null)
                return;
            MapData[][] mapData = context.getMapData();

            for (int y = GameData.Y_MIN_MAP_SIZE; y <= GameData.Y_MAX_MAP_SIZE; y++) {
                for (int x = GameData.X_MIN_MAP_SIZE; x <= GameData.X_MAX_MAP_SIZE; x++) {
                    byte flags = 0;
                    if (mapData[x][y].getBlocked())
                        flags |= 1;
                    if (mapData[x][y].getLayer(2).getGrhIndex() > 0)
                        flags |= 2;
                    if (mapData[x][y].getLayer(3).getGrhIndex() > 0)
                        flags |= 4;
                    if (mapData[x][y].getLayer(4).getGrhIndex() > 0)
                        flags |= 8;
                    if (mapData[x][y].getTrigger() > 0)
                        flags |= 16;
                    if (mapData[x][y].getParticleIndex() > 0)
                        flags |= 32;

                    bodyBuf.put(flags);

                    // Capa 1
                    int grh1 = mapData[x][y].getLayer(1).getGrhIndex();
                    if (!useLongIndices) {
                        bodyBuf.putShort((short) grh1);
                    } else {
                        bodyBuf.putInt(grh1);
                    }

                    if ((flags & 2) != 0) {
                        int grh = mapData[x][y].getLayer(2).getGrhIndex();
                        if (!useLongIndices)
                            bodyBuf.putShort((short) grh);
                        else
                            bodyBuf.putInt(grh);
                    }
                    if ((flags & 4) != 0) {
                        int grh = mapData[x][y].getLayer(3).getGrhIndex();
                        if (!useLongIndices)
                            bodyBuf.putShort((short) grh);
                        else
                            bodyBuf.putInt(grh);
                    }
                    if ((flags & 8) != 0) {
                        int grh = mapData[x][y].getLayer(4).getGrhIndex();
                        if (!useLongIndices)
                            bodyBuf.putShort((short) grh);
                        else
                            bodyBuf.putInt(grh);
                    }
                    if ((flags & 16) != 0)
                        bodyBuf.putShort((short) mapData[x][y].getTrigger());

                    if ((flags & 32) != 0)
                        bodyBuf.putShort((short) mapData[x][y].getParticleIndex());
                }
            }

            bodyBuf.flip();
            channel.write(bodyBuf);
        }
    }

    /**
     * Serializa las propiedades del mapa en formato de texto (.dat).
     */
    private static void saveMapProperties(String filePath) throws IOException {
        MapContext context = GameData.getActiveContext();
        if (context == null)
            return;
        MapProperties props = context.getMapProperties();

        try (PrintWriter writer = new PrintWriter(
                Files.newBufferedWriter(Path.of(filePath), StandardCharsets.ISO_8859_1))) {
            writer.println("[MAPA1]");
            writer.println("Name=" + props.getName());
            writer.println("MusicNum=" + props.getMusicIndex());
            writer.println("MagiaSinefecto=" + props.getMagiaSinEfecto());
            writer.println("NoEncriptarMP=" + props.getNoEncriptarMP());
            writer.println("Pk=" + props.getPlayerKiller());
            writer.println("Restringir="
                    + (props.getRestringir() == 0 ? "No" : props.getRestringir()));
            writer.println("BackUp=" + props.getBackup());
            writer.println("Zona=" + props.getZona());
            writer.println("Terreno=" + props.getTerreno());
        }
    }

    /**
     * Serializa la información de entidades (.inf) en formato binario.
     */
    private static void saveMapInfo(String filePath) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            java.nio.channels.FileChannel channel = fos.getChannel();

            // Cabecera (10 bytes iniciales)
            java.nio.ByteBuffer headerBuf = java.nio.ByteBuffer.allocate(10);
            headerBuf.order(java.nio.ByteOrder.LITTLE_ENDIAN);
            headerBuf.putShort((short) 0);
            headerBuf.putShort((short) 0);
            headerBuf.putShort((short) 0);
            headerBuf.putShort((short) 0);
            headerBuf.putShort((short) 0);
            headerBuf.flip();
            channel.write(headerBuf);

            // Datos de entidades por celda
            java.nio.ByteBuffer bodyBuf = java.nio.ByteBuffer.allocate(130000);
            bodyBuf.order(java.nio.ByteOrder.LITTLE_ENDIAN);

            MapContext context = GameData.getActiveContext();
            if (context == null || context.getMapData() == null)
                return;
            MapData[][] mapData = context.getMapData();

            for (int y = GameData.Y_MIN_MAP_SIZE; y <= GameData.Y_MAX_MAP_SIZE; y++) {
                for (int x = GameData.X_MIN_MAP_SIZE; x <= GameData.X_MAX_MAP_SIZE; x++) {
                    byte flags = 0;
                    if (mapData[x][y].getExitMap() > 0)
                        flags |= 1; // Traslado
                    if (mapData[x][y].getNpcIndex() > 0)
                        flags |= 2; // NPC
                    if (mapData[x][y].getObjIndex() > 0)
                        flags |= 4; // Objeto

                    bodyBuf.put(flags);

                    if ((flags & 1) != 0) {
                        bodyBuf.putShort((short) mapData[x][y].getExitMap());
                        bodyBuf.putShort((short) mapData[x][y].getExitX());
                        bodyBuf.putShort((short) mapData[x][y].getExitY());
                    }
                    if ((flags & 2) != 0) {
                        bodyBuf.putShort((short) mapData[x][y].getNpcIndex());
                    }
                    if ((flags & 4) != 0) {
                        bodyBuf.putShort((short) mapData[x][y].getObjIndex());
                        bodyBuf.putShort((short) mapData[x][y].getObjAmount());
                    }
                }
            }
            bodyBuf.flip();
            channel.write(bodyBuf);
        }
    }

}
