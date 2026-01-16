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
            initMap(data);

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
        Logger.info("Guardando mapa en: {}", filePath);

        try {
            // Guardar datos de capas (.map)
            saveMapData(filePath);

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

        // Crear contexto sin ruta (Sin Título)
        // Crear contexto sin ruta (Sin Título)
        MapContext context = new MapContext("", GameData.mapData, GameData.mapProperties, GameData.charList);
        context.setLastChar(org.argentumforge.engine.game.models.Character.lastChar);
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
     * @param data Contenido binario del archivo .map.
     */
    static void initMap(byte[] data) {
        GameData.reader.init(data);

        GameData.mapData = new MapData[GameData.X_MAX_MAP_SIZE + 1][GameData.Y_MAX_MAP_SIZE + 1];

        // Leer versión y saltar cabecera heredada de VB6
        final short mapversion = GameData.reader.readShort();
        GameData.reader.skipBytes(263);

        byte byflags;
        byte bloq;

        // Saltar campos no utilizados en el editor
        GameData.reader.readShort();
        GameData.reader.readShort();
        GameData.reader.readShort();
        GameData.reader.readShort();

        GameData.mapData[0][0] = new MapData();

        for (int y = 1; y <= 100; y++) {
            for (int x = 1; x <= 100; x++) {
                GameData.mapData[x][y] = new MapData();

                byflags = GameData.reader.readByte();

                // Bit 1: Bloqueo
                bloq = (byte) (byflags & 1);
                GameData.mapData[x][y].setBlocked(bloq == 1);

                // Capa 1 (Siempre presente)
                GameData.mapData[x][y].getLayer(1).setGrhIndex(GameData.reader.readShort());
                GameData.mapData[x][y].setLayer(1,
                        GameData.initGrh(GameData.mapData[x][y].getLayer(1),
                                GameData.mapData[x][y].getLayer(1).getGrhIndex(), true));

                // Capa 2
                if ((byte) (byflags & 2) != 0) {
                    GameData.mapData[x][y].getLayer(2).setGrhIndex(GameData.reader.readShort());
                    GameData.mapData[x][y].setLayer(2,
                            GameData.initGrh(GameData.mapData[x][y].getLayer(2),
                                    GameData.mapData[x][y].getLayer(2).getGrhIndex(), true));

                } else
                    GameData.mapData[x][y].getLayer(2).setGrhIndex(0);

                // Capa 3
                if ((byte) (byflags & 4) != 0) {
                    GameData.mapData[x][y].getLayer(3).setGrhIndex(GameData.reader.readShort());
                    GameData.mapData[x][y].setLayer(3,
                            GameData.initGrh(GameData.mapData[x][y].getLayer(3),
                                    GameData.mapData[x][y].getLayer(3).getGrhIndex(), true));
                } else
                    GameData.mapData[x][y].getLayer(3).setGrhIndex(0);

                // Capa 4
                if ((byte) (byflags & 8) != 0) {
                    GameData.mapData[x][y].getLayer(4).setGrhIndex(GameData.reader.readShort());
                    GameData.mapData[x][y].setLayer(4,
                            GameData.initGrh(GameData.mapData[x][y].getLayer(4),
                                    GameData.mapData[x][y].getLayer(4).getGrhIndex(), true));
                } else
                    GameData.mapData[x][y].getLayer(4).setGrhIndex(0);

                // Triggers
                if ((byte) (byflags & 16) != 0)
                    GameData.mapData[x][y].setTrigger(GameData.reader.readShort());
                else
                    GameData.mapData[x][y].setTrigger(0);

                GameData.mapData[x][y].getObjGrh().setGrhIndex(0);
            }
        }

        // Limpiar recursos de renderizado y entidades anteriores
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

                    byte flags = GameData.reader.readByte();

                    // Bit 1: Traslados (Exits)
                    if ((flags & 1) != 0) {
                        GameData.mapData[x][y].setExitMap(GameData.reader.readShort());
                        GameData.mapData[x][y].setExitX(GameData.reader.readShort());
                        GameData.mapData[x][y].setExitY(GameData.reader.readShort());
                    }

                    // Bit 2: NPCs
                    if ((flags & 2) != 0) {
                        short npcIndex = GameData.reader.readShort();
                        if (npcIndex < 0)
                            npcIndex = 0;

                        if (npcIndex > 0) {
                            GameData.mapData[x][y].setNpcIndex(npcIndex);
                            NpcData npc = AssetRegistry.npcs.get((int) npcIndex);
                            if (npc != null) {
                                Character.makeChar(GameData.nextOpenChar(), npc.getBody(), npc.getHead(),
                                        Direction.fromID(npc.getHeading()), x, y, 0, 0, 0);
                            }
                        }
                    }

                    // Bit 4: Objetos
                    if ((flags & 4) != 0) {
                        int objIndex = GameData.reader.readShort();
                        int amount = GameData.reader.readShort();

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
    private static void saveMapData(String filePath) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            java.nio.channels.FileChannel channel = fos.getChannel();

            // 1. Cabecera (273 bytes)
            java.nio.ByteBuffer headerBuf = java.nio.ByteBuffer.allocate(273);
            headerBuf.order(java.nio.ByteOrder.LITTLE_ENDIAN);

            headerBuf.putShort((short) 1); // Versión del mapa
            headerBuf.put(new byte[263]); // Relleno cabecera
            headerBuf.putShort((short) 0);
            headerBuf.putShort((short) 0);
            headerBuf.putShort((short) 0);
            headerBuf.putShort((short) 0);

            headerBuf.flip();
            channel.write(headerBuf);

            // 2. Datos de tiles
            java.nio.ByteBuffer bodyBuf = java.nio.ByteBuffer.allocate(110000);
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

                    bodyBuf.put(flags);
                    bodyBuf.putShort((short) GameData.mapData[x][y].getLayer(1).getGrhIndex());

                    if ((flags & 2) != 0)
                        bodyBuf.putShort((short) GameData.mapData[x][y].getLayer(2).getGrhIndex());
                    if ((flags & 4) != 0)
                        bodyBuf.putShort((short) GameData.mapData[x][y].getLayer(3).getGrhIndex());
                    if ((flags & 8) != 0)
                        bodyBuf.putShort((short) GameData.mapData[x][y].getLayer(4).getGrhIndex());
                    if ((flags & 16) != 0)
                        bodyBuf.putShort(GameData.mapData[x][y].getTrigger());
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
                        bodyBuf.putShort(GameData.mapData[x][y].getExitMap());
                        bodyBuf.putShort(GameData.mapData[x][y].getExitX());
                        bodyBuf.putShort(GameData.mapData[x][y].getExitY());
                    }
                    if ((flags & 2) != 0) {
                        bodyBuf.putShort(GameData.mapData[x][y].getNpcIndex());
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
