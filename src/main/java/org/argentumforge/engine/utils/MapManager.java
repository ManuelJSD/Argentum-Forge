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
            opt.includeHeader = false;
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
        Path mapPath = Path.of("resources", "maps", "mapa" + numMap + ".map");
        byte[] data = null;
        try {
            if (Files.exists(mapPath)) {
                data = Files.readAllBytes(mapPath);
            }
        } catch (IOException e) {
            System.err.println("Error reading map file: " + e.getMessage());
        }

        if (data == null) {
            System.err.println("Could not load mapa" + numMap + " data from: " + mapPath);
            return;
        }
        MapSaveOptions detectedOptions = detectSaveOptions(data);
        initMap(data, detectedOptions);
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
    public static boolean checkUnsavedChanges() {
        if (!hasUnsavedChanges())
            return true;

        int result = javax.swing.JOptionPane.showConfirmDialog(
                null,
                "Hay cambios sin guardar en el mapa actual.\n¿Desea guardarlos antes de continuar?",
                "Cambios sin guardar",
                javax.swing.JOptionPane.YES_NO_CANCEL_OPTION,
                javax.swing.JOptionPane.WARNING_MESSAGE);

        if (result == javax.swing.JOptionPane.YES_OPTION) {
            // Intentar guardar en la última ruta conocida
            String lastPath = org.argentumforge.engine.game.Options.INSTANCE.getLastMapPath();
            if (lastPath != null && !lastPath.isEmpty() && new java.io.File(lastPath).exists()) {
                saveMap(lastPath);
                return true;
            } else {
                javax.swing.JOptionPane.showMessageDialog(null,
                        "No se pudo autoguardar. Por favor, guarde el mapa manualmente.");
                return false;
            }
        } else if (result == javax.swing.JOptionPane.NO_OPTION) {
            return true; // Descartar cambios
        } else {
            return false; // Cancelar
        }
    }

    /**
     * Carga un mapa completo (capas, propiedades y entidades) desde una ruta.
     * 
     * @param filePath Ruta absoluta al archivo .map
     */
    public static void loadMap(String filePath) {
        // Si el mapa ya está abierto, simplemente lo activamos
        for (MapContext context : GameData.getOpenMaps()) {
            if (filePath.equals(context.getFilePath())) {
                GameData.setActiveContext(context);
                return;
            }
        }

        Logger.info("Cargando mapa desde: {}", filePath);

        // Guardar la ruta del último mapa para futuras sesiones y añadir al historial
        Options.INSTANCE.setLastMapPath(filePath);
        Options.INSTANCE.save();

        try {
            GameData.clearActiveContext();

            // Reiniciar estado para el nuevo mapa (sin borrar el anterior array)
            org.argentumforge.engine.game.models.Character.lastChar = 0;
            org.argentumforge.engine.utils.GameData.mapData = null;

            GameData.charList = new org.argentumforge.engine.game.models.Character[10001];
            for (int i = 0; i < GameData.charList.length; i++) {
                GameData.charList[i] = new org.argentumforge.engine.game.models.Character();
            }
            GameData.mapData = null;
            GameData.mapProperties = new MapProperties();

            // Cargar archivo principal de capas (.map)
            byte[] data = Files.readAllBytes(Path.of(filePath));
            MapSaveOptions detectedOptions = detectSaveOptions(data);
            initMap(data, detectedOptions);

            // Preparar para buscar archivos complementarios (.inf y .dat)
            String basePath = filePath.substring(0, filePath.lastIndexOf('.'));
            String datPath = basePath + ".dat";
            String infPath = basePath + ".inf";

            // Intentar cargar propiedades del mapa (.dat)
            if (Files.exists(Path.of(datPath))) {
                loadMapProperties(datPath);
            } else {
                GameData.mapProperties = new MapProperties();
                Logger.info("Archivo .dat no encontrado en {}, usando valores por defecto.", datPath);
            }

            // Intentar cargar información de entidades (.inf)
            if (Files.exists(Path.of(infPath))) {
                loadMapInfo(infPath);
            } else {
                Logger.info("Archivo .inf no encontrado en {}, saltando carga de entidades.", infPath);
            }

            // Crear el contexto y registrarlo
            MapContext context = new MapContext(filePath, GameData.mapData, GameData.mapProperties, GameData.charList);
            context.setLastChar(org.argentumforge.engine.game.models.Character.lastChar);
            context.setSaveOptions(detectedOptions);
            GameData.setActiveContext(context);

            // Reiniciar estado de modificaciones
            markAsSaved();
            org.argentumforge.engine.utils.editor.commands.CommandManager.getInstance().clearHistory();

            Logger.info("Mapa cargado exitosamente");
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

            Logger.info("Mapa guardado exitosamente en: {}", filePath);
            javax.swing.JOptionPane.showMessageDialog(null, "Mapa guardado correctamente.");
        } catch (IOException e) {
            Logger.error(e, "Error al guardar el mapa en: {}", filePath);
            javax.swing.JOptionPane.showMessageDialog(null, "Error al guardar el mapa:\n" + e.getMessage(), "Error",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
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
        org.argentumforge.engine.game.models.Character.lastChar = 0;
        GameData.charList = new org.argentumforge.engine.game.models.Character[10001];
        for (int i = 0; i < GameData.charList.length; i++) {
            GameData.charList[i] = new org.argentumforge.engine.game.models.Character();
        }

        // Inicializar rejilla de datos
        GameData.mapData = new MapData[width + 1][height + 1];
        for (int x = 0; x <= width; x++) {
            for (int y = 0; y <= height; y++) {
                GameData.mapData[x][y] = new MapData();
            }
        }

        // Resetear propiedades del mapa
        GameData.mapProperties = new MapProperties();

        MapContext context = new MapContext("", GameData.mapData, GameData.mapProperties, GameData.charList);
        context.setLastChar(org.argentumforge.engine.game.models.Character.lastChar);
        context.setSaveOptions(MapSaveOptions.extended());
        GameData.setActiveContext(context);

        // Reiniciar estado de modificaciones
        markAsSaved();
        org.argentumforge.engine.utils.editor.commands.CommandManager.getInstance().clearHistory();

        Logger.info("Mapa vacío creado correctamente");
    }

    /**
     * Procesa los datos binarios del archivo .map para inicializar la rejilla.
     * Lee cabeceras, flags de tile, capas de gráficos, bloqueos y triggers base.
     *
     * @param data    Contenido binario del archivo .map.
     * @param options Opciones de guardado inferidas.
     */
    static void initMap(byte[] data, MapSaveOptions options) {
        GameData.reader.init(data);

        GameData.mapData = new MapData[GameData.X_MAX_MAP_SIZE + 1][GameData.Y_MAX_MAP_SIZE + 1];
        for (int y = 0; y <= GameData.Y_MAX_MAP_SIZE; y++) {
            for (int x = 0; x <= GameData.X_MAX_MAP_SIZE; x++) {
                GameData.mapData[x][y] = new MapData();
            }
        }

        // Leer versión y saltar cabecera heredada de VB6
        if (!GameData.reader.hasRemaining(2))
            return;
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
                GameData.mapData[x][y].setBlocked(bloq == 1);

                // Capa 1 (Siempre presente)
                if (!GameData.reader.hasRemaining(indexSize))
                    break tileLoop;
                int grh1 = useLongIndices ? GameData.reader.readInt() : GameData.reader.readUnsignedShort();
                GameData.mapData[x][y].getLayer(1).setGrhIndex(grh1);
                GameData.mapData[x][y].setLayer(1,
                        GameData.initGrh(GameData.mapData[x][y].getLayer(1),
                                GameData.mapData[x][y].getLayer(1).getGrhIndex(), true));

                // Capa 2
                if ((byte) (byflags & 2) != 0) {
                    if (!GameData.reader.hasRemaining(indexSize))
                        break tileLoop;
                    int grh2 = useLongIndices ? GameData.reader.readInt() : GameData.reader.readUnsignedShort();
                    GameData.mapData[x][y].getLayer(2).setGrhIndex(grh2);
                    GameData.mapData[x][y].setLayer(2,
                            GameData.initGrh(GameData.mapData[x][y].getLayer(2),
                                    GameData.mapData[x][y].getLayer(2).getGrhIndex(), true));

                } else
                    GameData.mapData[x][y].getLayer(2).setGrhIndex(0);

                // Capa 3
                if ((byte) (byflags & 4) != 0) {
                    if (!GameData.reader.hasRemaining(indexSize))
                        break tileLoop;
                    int grh3 = useLongIndices ? GameData.reader.readInt() : GameData.reader.readUnsignedShort();
                    GameData.mapData[x][y].getLayer(3).setGrhIndex(grh3);
                    GameData.mapData[x][y].setLayer(3,
                            GameData.initGrh(GameData.mapData[x][y].getLayer(3),
                                    GameData.mapData[x][y].getLayer(3).getGrhIndex(), true));
                } else
                    GameData.mapData[x][y].getLayer(3).setGrhIndex(0);

                // Capa 4
                if ((byte) (byflags & 8) != 0) {
                    if (!GameData.reader.hasRemaining(indexSize))
                        break tileLoop;
                    int grh4 = useLongIndices ? GameData.reader.readInt() : GameData.reader.readUnsignedShort();
                    GameData.mapData[x][y].getLayer(4).setGrhIndex(grh4);
                    GameData.mapData[x][y].setLayer(4,
                            GameData.initGrh(GameData.mapData[x][y].getLayer(4),
                                    GameData.mapData[x][y].getLayer(4).getGrhIndex(), true));
                } else
                    GameData.mapData[x][y].getLayer(4).setGrhIndex(0);

                // Triggers
                if ((byte) (byflags & 16) != 0) {
                    if (!GameData.reader.hasRemaining(2))
                        break tileLoop;
                    GameData.mapData[x][y].setTrigger(GameData.reader.readUnsignedShort());
                } else
                    GameData.mapData[x][y].setTrigger(0);

                // Bit 5 (Valor 32): Partículas (AOLibre/Versiones nuevas)
                if ((byflags & 32) != 0) {
                    if (!GameData.reader.hasRemaining(2))
                        break tileLoop;
                    int pId = GameData.reader.readShort();
                    GameData.mapData[x][y].setParticleIndex(pId);
                    if (pId > 0)
                        particlesLoaded++;
                } else {
                    GameData.mapData[x][y].setParticleIndex(0);
                }

                GameData.mapData[x][y].getObjGrh().setGrhIndex(0);
            }
        }

        // Limpiar recursos de renderizado y entidades anteriores
        if (particlesLoaded > 0) {
            GameData.options.getRenderSettings().setShowParticles(true);
        }
        Surface.INSTANCE.deleteAllTextures();
        eraseAllChars();
    }

    /**
     * Carga las propiedades generales del mapa (nombre, música, zona) desde un .dat
     *
     * @param filePath Ruta absoluta al archivo .dat del mapa.
     */
    private static void loadMapProperties(String filePath) {
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

        GameData.mapProperties = props;
        Logger.info("Propiedades cargadas: Nombre='{}', Música={}, Zona='{}'", props.getName(), props.getMusicIndex(),
                props.getZona());
    }

    /**
     * Carga la información de entidades (NPCs, Objetos, Traslados) desde un .inf.
     * Recrea las entidades visuales en el editor.
     *
     * @param filePath Ruta absoluta al archivo .inf
     */
    private static void loadMapInfo(String filePath) {
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

                    if (GameData.mapData[x][y] == null)
                        continue;

                    byte flags = GameData.reader.readByte();

                    // Bit 1: Traslados (Exits)
                    if ((flags & 1) != 0) {
                        GameData.mapData[x][y].setExitMap(GameData.reader.readUnsignedShort());
                        GameData.mapData[x][y].setExitX(GameData.reader.readUnsignedShort());
                        GameData.mapData[x][y].setExitY(GameData.reader.readUnsignedShort());
                    }

                    // Bit 2: NPCs
                    if ((flags & 2) != 0) {
                        int npcIndex = GameData.reader.readUnsignedShort();
                        if (npcIndex > 0) {
                            GameData.mapData[x][y].setNpcIndex((short) npcIndex);
                            NpcData npc = AssetRegistry.npcs.get(npcIndex);
                            if (npc != null) {
                                Character.makeChar(GameData.nextOpenChar(), npc.getBody(), npc.getHead(),
                                        Direction.fromID(npc.getHeading()), x, y, 0, 0, 0);
                            }
                        }
                    }

                    // Bit 4: Objetos
                    if ((flags & 4) != 0) {
                        int objIndex = GameData.reader.readUnsignedShort();
                        int amount = GameData.reader.readUnsignedShort();

                        GameData.mapData[x][y].setObjIndex(objIndex);
                        GameData.mapData[x][y].setObjAmount(amount);

                        if (objIndex > 0) {
                            ObjData obj = AssetRegistry.objs.get(objIndex);
                            if (obj != null) {
                                GameData.initGrh(GameData.mapData[x][y].getObjGrh(), (short) obj.getGrhIndex(), false);
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

            for (int y = GameData.Y_MIN_MAP_SIZE; y <= GameData.Y_MAX_MAP_SIZE; y++) {
                for (int x = GameData.X_MIN_MAP_SIZE; x <= GameData.X_MAX_MAP_SIZE; x++) {
                    byte flags = 0;
                    if (GameData.mapData[x][y].getBlocked())
                        flags |= 1;
                    if (GameData.mapData[x][y].getLayer(2).getGrhIndex() > 0)
                        flags |= 2;
                    if (GameData.mapData[x][y].getLayer(3).getGrhIndex() > 0)
                        flags |= 4;
                    if (GameData.mapData[x][y].getLayer(4).getGrhIndex() > 0)
                        flags |= 8;
                    if (GameData.mapData[x][y].getTrigger() > 0)
                        flags |= 16;
                    if (GameData.mapData[x][y].getParticleIndex() > 0)
                        flags |= 32;

                    bodyBuf.put(flags);

                    // Capa 1
                    int grh1 = GameData.mapData[x][y].getLayer(1).getGrhIndex();
                    if (!useLongIndices) {
                        bodyBuf.putShort((short) grh1);
                    } else {
                        bodyBuf.putInt(grh1);
                    }

                    if ((flags & 2) != 0) {
                        int grh = GameData.mapData[x][y].getLayer(2).getGrhIndex();
                        if (!useLongIndices)
                            bodyBuf.putShort((short) grh);
                        else
                            bodyBuf.putInt(grh);
                    }
                    if ((flags & 4) != 0) {
                        int grh = GameData.mapData[x][y].getLayer(3).getGrhIndex();
                        if (!useLongIndices)
                            bodyBuf.putShort((short) grh);
                        else
                            bodyBuf.putInt(grh);
                    }
                    if ((flags & 8) != 0) {
                        int grh = GameData.mapData[x][y].getLayer(4).getGrhIndex();
                        if (!useLongIndices)
                            bodyBuf.putShort((short) grh);
                        else
                            bodyBuf.putInt(grh);
                    }
                    if ((flags & 16) != 0)
                        bodyBuf.putShort((short) GameData.mapData[x][y].getTrigger());

                    if ((flags & 32) != 0)
                        bodyBuf.putShort((short) GameData.mapData[x][y].getParticleIndex());
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
        try (PrintWriter writer = new PrintWriter(
                Files.newBufferedWriter(Path.of(filePath), StandardCharsets.ISO_8859_1))) {
            writer.println("[MAPA1]");
            writer.println("Name=" + GameData.mapProperties.getName());
            writer.println("MusicNum=" + GameData.mapProperties.getMusicIndex());
            writer.println("MagiaSinefecto=" + GameData.mapProperties.getMagiaSinEfecto());
            writer.println("NoEncriptarMP=" + GameData.mapProperties.getNoEncriptarMP());
            writer.println("Pk=" + GameData.mapProperties.getPlayerKiller());
            writer.println("Restringir="
                    + (GameData.mapProperties.getRestringir() == 0 ? "No" : GameData.mapProperties.getRestringir()));
            writer.println("BackUp=" + GameData.mapProperties.getBackup());
            writer.println("Zona=" + GameData.mapProperties.getZona());
            writer.println("Terreno=" + GameData.mapProperties.getTerreno());
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

            for (int y = GameData.Y_MIN_MAP_SIZE; y <= GameData.Y_MAX_MAP_SIZE; y++) {
                for (int x = GameData.X_MIN_MAP_SIZE; x <= GameData.X_MAX_MAP_SIZE; x++) {
                    byte flags = 0;
                    if (GameData.mapData[x][y].getExitMap() > 0)
                        flags |= 1; // Traslado
                    if (GameData.mapData[x][y].getNpcIndex() > 0)
                        flags |= 2; // NPC
                    if (GameData.mapData[x][y].getObjIndex() > 0)
                        flags |= 4; // Objeto

                    bodyBuf.put(flags);

                    if ((flags & 1) != 0) {
                        bodyBuf.putShort((short) GameData.mapData[x][y].getExitMap());
                        bodyBuf.putShort((short) GameData.mapData[x][y].getExitX());
                        bodyBuf.putShort((short) GameData.mapData[x][y].getExitY());
                    }
                    if ((flags & 2) != 0) {
                        bodyBuf.putShort((short) GameData.mapData[x][y].getNpcIndex());
                    }
                    if ((flags & 4) != 0) {
                        bodyBuf.putShort((short) GameData.mapData[x][y].getObjIndex());
                        bodyBuf.putShort((short) GameData.mapData[x][y].getObjAmount());
                    }
                }
            }
            bodyBuf.flip();
            channel.write(bodyBuf);
        }
    }

}
