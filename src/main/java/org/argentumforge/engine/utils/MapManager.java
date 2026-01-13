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
 * Clase responsable de las operaciones de gestión de mapas.
 */
public final class MapManager {
    private MapManager() {
        // Clase de utilidad
    }

    /**
     * Carga un mapa desde la ruta especificada.
     */
    public static void loadMap(String filePath) {
        Logger.info("Loading map from: {}", filePath);

        // Guardar la ruta del último mapa
        Options.INSTANCE.setLastMapPath(filePath);
        Options.INSTANCE.save();
        // Limpiar personajes existentes
        eraseAllChars();
        try {
            // Cargar archivo principal de capas (.map)
            byte[] data = Files.readAllBytes(Path.of(filePath));
            initMap(data);
            // Preparar para buscar archivos compañeros (.inf y .dat)
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
                Logger.info("Archivo .inf no encontrado en {}, saltando la carga de entidades.", infPath);
            }
            Logger.info("Map loaded successfully");
        } catch (IOException e) {
            Logger.error(e, "Could not load map from path: {}", filePath);
        }
    }

    /**
     * Guarda el mapa actual en la ruta especificada.
     */
    public static void saveMap(String filePath) {
        Logger.info("Saving map to: {}", filePath);

        try {
            // Guardar .map
            saveMapData(filePath);
            String basePath = filePath.substring(0, filePath.lastIndexOf('.'));
            String datPath = basePath + ".dat";
            String infPath = basePath + ".inf";
            // Guardar .dat
            saveMapProperties(datPath);
            // Guardar .inf
            saveMapInfo(infPath);
            Logger.info("Mapa guardado exitosamente en: {}", filePath);
            javax.swing.JOptionPane.showMessageDialog(null, "Mapa guardado correctamente.");
        } catch (IOException e) {
            Logger.error(e, "Error al guardar el mapa: {}", filePath);
            javax.swing.JOptionPane.showMessageDialog(null, "Error al guardar el mapa:\n" + e.getMessage(), "Error",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Crea un mapa vacío con las dimensiones especificadas.
     */
    public static void createEmptyMap(int width, int height) {
        Logger.info("Creating empty map: {}x{}", width, height);
        // Validar dimensiones
        if (width < GameData.X_MIN_MAP_SIZE || width > GameData.X_MAX_MAP_SIZE ||
                height < GameData.Y_MIN_MAP_SIZE || height > GameData.Y_MAX_MAP_SIZE) {
            Logger.error("Invalid map dimensions: {}x{}", width, height);
            return;
        }
        // Limpiar personajes
        eraseAllChars();
        // Crear nuevo mapa
        GameData.mapData = new MapData[width + 1][height + 1];
        for (int x = 0; x <= width; x++) {
            for (int y = 0; y <= height; y++) {
                GameData.mapData[x][y] = new MapData();
            }
        }
        // Resetear propiedades
        GameData.mapProperties = new MapProperties();
        Logger.info("Empty map created successfully");
    }

    /**
     * Método interno para procesar el parseo de los datos binarios de un mapa.
     * Lee cabeceras, flags, capas, bloqueos y triggers.
     *
     * @param data Datos binarios del mapa.
     */
    static void initMap(byte[] data) {
        GameData.reader.init(data);

        GameData.mapData = new MapData[GameData.X_MAX_MAP_SIZE + 1][GameData.Y_MAX_MAP_SIZE + 1];

        final short mapversion = GameData.reader.readShort();
        GameData.reader.skipBytes(263); // cabecera.

        byte byflags;

        GameData.reader.readShort();
        GameData.reader.readShort();
        GameData.reader.readShort();
        GameData.reader.readShort();

        byte bloq;

        GameData.mapData[0][0] = new MapData();

        for (int y = 1; y <= 100; y++) {
            for (int x = 1; x <= 100; x++) {
                GameData.mapData[x][y] = new MapData();

                byflags = GameData.reader.readByte();
                bloq = (byte) (byflags & 1);
                GameData.mapData[x][y].setBlocked(bloq == 1);

                GameData.mapData[x][y].getLayer(1).setGrhIndex(GameData.reader.readShort());
                GameData.mapData[x][y].setLayer(1,
                        GameData.initGrh(GameData.mapData[x][y].getLayer(1),
                                GameData.mapData[x][y].getLayer(1).getGrhIndex(), true));

                if ((byte) (byflags & 2) != 0) {
                    GameData.mapData[x][y].getLayer(2).setGrhIndex(GameData.reader.readShort());
                    GameData.mapData[x][y].setLayer(2,
                            GameData.initGrh(GameData.mapData[x][y].getLayer(2),
                                    GameData.mapData[x][y].getLayer(2).getGrhIndex(), true));

                } else
                    GameData.mapData[x][y].getLayer(2).setGrhIndex(0);

                if ((byte) (byflags & 4) != 0) {
                    GameData.mapData[x][y].getLayer(3).setGrhIndex(GameData.reader.readShort());
                    GameData.mapData[x][y].setLayer(3,
                            GameData.initGrh(GameData.mapData[x][y].getLayer(3),
                                    GameData.mapData[x][y].getLayer(3).getGrhIndex(), true));
                } else
                    GameData.mapData[x][y].getLayer(3).setGrhIndex(0);

                if ((byte) (byflags & 8) != 0) {
                    GameData.mapData[x][y].getLayer(4).setGrhIndex(GameData.reader.readShort());
                    GameData.mapData[x][y].setLayer(4,
                            GameData.initGrh(GameData.mapData[x][y].getLayer(4),
                                    GameData.mapData[x][y].getLayer(4).getGrhIndex(), true));
                } else
                    GameData.mapData[x][y].getLayer(4).setGrhIndex(0);

                if ((byte) (byflags & 16) != 0)
                    GameData.mapData[x][y].setTrigger(GameData.reader.readShort());
                else
                    GameData.mapData[x][y].setTrigger(0);

                GameData.mapData[x][y].getObjGrh().setGrhIndex(0);
            }
        }

        // Liberar memoria
        Surface.INSTANCE.deleteAllTextures();
        eraseAllChars();
    }

    /**
     * Carga las propiedades generales del mapa desde un archivo .dat.
     *
     * @param filePath Ruta absoluta al archivo .dat
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

                // Saltamos las cabeceras de sección [MAPA1]
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
                    Logger.warn("Error parseando valor '{}' para la clave '{}' en el mapa.", value, key);
                }
            }
        } catch (IOException e) {
            Logger.error(e, "Error leyendo el archivo .dat del mapa: {}", filePath);
        }

        GameData.mapProperties = props;
        Logger.info("Propiedades cargadas: Name={}, Music={}, Zona={}", props.getName(), props.getMusicIndex(),
                props.getZona());
    }

    /**
     * Carga la información de entidades (NPCs, Objetos, Triggers) desde un archivo
     * .inf.
     *
     * @param filePath Ruta absoluta al archivo .inf
     */
    private static void loadMapInfo(String filePath) {
        try {
            byte[] data = Files.readAllBytes(Path.of(filePath));
            GameData.reader.init(data);

            // Cabecera inf (5 integers in VB6 = 10 bytes)
            GameData.reader.readShort();
            GameData.reader.readShort();
            GameData.reader.readShort();
            GameData.reader.readShort();
            GameData.reader.readShort();

            // Load arrays
            for (int y = GameData.Y_MIN_MAP_SIZE; y <= GameData.Y_MAX_MAP_SIZE; y++) {
                for (int x = GameData.X_MIN_MAP_SIZE; x <= GameData.X_MAX_MAP_SIZE; x++) {
                    if (!GameData.reader.hasRemaining())
                        break;

                    // .inf file
                    byte flags = GameData.reader.readByte();

                    // If ByFlags And 1 Then (Exits)
                    if ((flags & 1) != 0) {
                        GameData.mapData[x][y].setExitMap(GameData.reader.readShort());
                        GameData.mapData[x][y].setExitX(GameData.reader.readShort());
                        GameData.mapData[x][y].setExitY(GameData.reader.readShort());
                    }

                    // If ByFlags And 2 Then (NPCs)
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

                    // If ByFlags And 4 Then (Objects)
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
                                Logger.warn("Object definition not found for index: {}", objIndex);
                            }
                        }
                    }

                }
            }

        } catch (IOException e) {
            Logger.error(e, "Error loading map info from: {}", filePath);
        }
    }

    private static void saveMapData(String filePath) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            java.nio.channels.FileChannel channel = fos.getChannel();

            // 1. Header
            java.nio.ByteBuffer headerBuf = java.nio.ByteBuffer.allocate(273);
            headerBuf.order(java.nio.ByteOrder.LITTLE_ENDIAN);

            headerBuf.putShort((short) 1); // Map Version
            headerBuf.put(new byte[263]); // Cabecera vacia
            headerBuf.putShort((short) 0);
            headerBuf.putShort((short) 0);
            headerBuf.putShort((short) 0);
            headerBuf.putShort((short) 0);

            headerBuf.flip();
            channel.write(headerBuf);

            // 2. Map Data
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

    private static void saveMapInfo(String filePath) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            java.nio.channels.FileChannel channel = fos.getChannel();

            // Header: 5 shorts = 10 bytes
            java.nio.ByteBuffer headerBuf = java.nio.ByteBuffer.allocate(10);
            headerBuf.order(java.nio.ByteOrder.LITTLE_ENDIAN);
            headerBuf.putShort((short) 0);
            headerBuf.putShort((short) 0);
            headerBuf.putShort((short) 0);
            headerBuf.putShort((short) 0);
            headerBuf.putShort((short) 0);
            headerBuf.flip();
            channel.write(headerBuf);

            // Body
            // Max size per cell unknown but bounded.
            // Flag (1) + Exit(2+2+2) + NPC(2) + Obj(2+2) = 13 bytes max
            java.nio.ByteBuffer bodyBuf = java.nio.ByteBuffer.allocate(130000);
            bodyBuf.order(java.nio.ByteOrder.LITTLE_ENDIAN);

            for (int y = GameData.Y_MIN_MAP_SIZE; y <= GameData.Y_MAX_MAP_SIZE; y++) {
                for (int x = GameData.X_MIN_MAP_SIZE; x <= GameData.X_MAX_MAP_SIZE; x++) {
                    byte flags = 0;
                    if (GameData.mapData[x][y].getExitMap() > 0)
                        flags |= 1; // Exit
                    if (GameData.mapData[x][y].getNpcIndex() > 0)
                        flags |= 2; // NPC
                    if (GameData.mapData[x][y].getObjIndex() > 0)
                        flags |= 4; // Obj

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