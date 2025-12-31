package org.argentumforge.engine.utils;

import org.argentumforge.engine.game.Options;
import org.argentumforge.engine.game.models.Character;
import org.argentumforge.engine.renderer.Surface;
import org.argentumforge.engine.utils.inits.*;
import org.argentumforge.engine.utils.inits.MapProperties;
import org.tinylog.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.argentumforge.engine.game.Messages.loadMessages;
import static org.argentumforge.engine.game.models.Character.eraseAllChars;
import static org.argentumforge.engine.renderer.FontRenderer.loadFonts;
import static org.argentumforge.scripts.Compressor.readResource;

/**
 * <p>
 * Clase central de almacenamiento y gestion de datos del juego.
 * {@code GameData} contiene las referencias a todos los recursos,
 * estados y configuraciones necesarios para el funcionamiento del cliente.
 * <p>
 * Esta clase es de tipo utilidad (utility class) y no debe ser instanciada.
 * Provee metodos estaticos para cargar, inicializar y
 * acceder a los diferentes elementos del juego como graficos, sonidos,
 * personajes, mapas y configuraciones.
 * <p>
 * Entre sus responsabilidades principales se encuentran:
 * <ul>
 * <li>Gestionar los arrays estaticos de datos del juego (cuerpos, cabezas,
 * graficos, etc.)
 * <li>Cargar los recursos desde archivos comprimidos
 * <li>Inicializar los datos necesarios al inicio del juego
 * <li>Proporcionar metodos de acceso a los datos del juego
 * <li>Mantener el estado global de elementos como personajes, mapas y efectos
 * </ul>
 * <p>
 * La clase implementa un sistema de carga secuencial de recursos durante la
 * inicializacion, garantizando que todos los datos
 * necesarios esten disponibles antes de que el juego comience a ejecutarse.
 */

public final class GameData {

    /** Datos de los cuerpos (animaciones de movimiento, offsets, etc). */
    public static BodyData[] bodyData;
    /** Datos de las cabezas. */
    public static HeadData[] headData;
    /** Datos de los cascos. */
    public static HeadData[] helmetsData;
    /** Datos de las armas. */
    public static WeaponData[] weaponData;
    /** Datos de los escudos. */
    public static ShieldData[] shieldData;
    /** Datos de los efectos visuales (FXs). */
    public static FxData[] fxData;
    /** Datos de los gráficos (definiciones de GRH, frames, velocidades, etc). */
    public static GrhData[] grhData;
    /** Datos de la rejilla del mapa actual. */
    public static MapData[][] mapData;
    /** Indica si llueve en cada mapa. */
    public static boolean[] bLluvia;
    /** Lista global de personajes activos en el mundo. */
    public static Character[] charList = new Character[10000 + 1];
    /** Mapa de definiciones de NPCs cargadas desde el archivo de datos. */
    public static Map<Integer, NpcData> npcs;
    /** Mapa de definiciones de Objetos cargadas desde el archivo de datos. */
    public static Map<Integer, ObjData> objs;
    public static Map<Integer, Integer> minimapColors = new HashMap<>();
    /** Propiedades generales del mapa actual (.dat). */
    public static MapProperties mapProperties = new MapProperties();
    /** Instancia de configuración del usuario. */
    public static Options options = Options.INSTANCE;
    /** Lector de datos binarios persistente para la carga de recursos. */
    private static BinaryDataReader reader;

    /**
     * Inicializamos todos los datos almacenados en archivos.
     */
    public static void init() {
        for (int i = 0; i < charList.length; i++)
            charList[i] = new Character();

        reader = new BinaryDataReader();
        options.load();

        if (checkResources()) {
            loadNpcs();
            loadObjs();
            loadMiniMapColors();
            loadGrhData();
            loadHeads();
            loadHelmets();
            loadBodys();
            // loadWeapons();
            // loadShields();
            loadFxs();
            loadFK();
            loadFonts();
            loadMessages(options.getLanguage());
        }
    }

    /**
     * Verifica si existen los archivos esenciales para el funcionamiento del motor.
     * 
     * @return true si los archivos existen, false si falta alguno.
     */
    public static boolean checkResources() {
        // Graficos.ind en InitPath
        if (!Files.exists(Path.of(options.getInitPath(), "Graficos.ind")))
            return false;
        // NPCs.dat en DatsPath
        if (!Files.exists(Path.of(options.getDatsPath(), "NPCs.dat")))
            return false;
        // OBJ.dat en DatsPath
        if (!Files.exists(Path.of(options.getDatsPath(), "OBJ.dat")))
            return false;

        return true;
    }

    /**
     * Carga las definiciones de NPCs desde el archivo externo especificado en las
     * opciones.
     * Parsea el archivo .dat (estilo INI) para extraer nombres, cuerpos y cabezas.
     */
    private static void loadNpcs() {
        final Path npcsPath = Path.of(options.getDatsPath(), "NPCs.dat");

        if (!Files.exists(npcsPath)) {
            Logger.error("NPCs.dat not found at path: {}", npcsPath.toAbsolutePath());
            javax.swing.JOptionPane.showMessageDialog(null,
                    "No se encontró el archivo NPCs.dat en:\n" + npcsPath.toAbsolutePath() +
                            "\n\nPor favor, configure la ruta de Dats correctamente.",
                    "Error al cargar NPCs",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
            npcs = new HashMap<>();
            return;
        }

        final Map<Integer, NpcData> result = new HashMap<>();
        NpcData currentNpc = null;

        try (BufferedReader br = Files.newBufferedReader(npcsPath, StandardCharsets.ISO_8859_1)) {
            String line;
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty())
                    continue;
                if (trimmed.startsWith("'"))
                    continue;
                if (trimmed.startsWith("#"))
                    continue;
                if (trimmed.startsWith(";"))
                    continue;

                if (trimmed.startsWith("[") && trimmed.contains("]")) {
                    String section = trimmed.substring(1, trimmed.indexOf(']')).trim();
                    if (section.regionMatches(true, 0, "NPC", 0, 3)) {
                        String numPart = section.substring(3).trim();
                        try {
                            int npcNumber = Integer.parseInt(numPart);
                            currentNpc = new NpcData(npcNumber);
                            result.put(npcNumber, currentNpc);
                        } catch (NumberFormatException e) {
                            currentNpc = null;
                        }
                    } else {
                        currentNpc = null;
                    }
                    continue;
                }

                if (currentNpc == null)
                    continue;
                int eq = trimmed.indexOf('=');
                if (eq <= 0)
                    continue;

                String key = trimmed.substring(0, eq).trim();
                String value = trimmed.substring(eq + 1).trim();

                if (key.equalsIgnoreCase("Name")) {
                    currentNpc.setName(value);
                } else if (key.equalsIgnoreCase("Head")) {
                    try {
                        currentNpc.setHead(Integer.parseInt(value));
                    } catch (NumberFormatException ignored) {
                    }
                } else if (key.equalsIgnoreCase("Body")) {
                    try {
                        currentNpc.setBody(Integer.parseInt(value));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        } catch (IOException e) {
            Logger.error(e, "Could not read NPCs.dat from path: {}", npcsPath.toAbsolutePath());
        }

        npcs = result;
        Logger.info("Loaded {} NPC definitions from {}", npcs.size(), npcsPath.toAbsolutePath());
    }

    /**
     * Carga las definiciones de Objetos desde el archivo externo especificado en
     * las opciones.
     * Parsea el archivo .dat (estilo INI) para extraer nombre y grhIndex.
     */
    private static void loadObjs() {
        final Path objsPath = Path.of(options.getDatsPath(), "OBJ.dat");

        if (!Files.exists(objsPath)) {
            Logger.error("OBJ.dat not found at path: {}", objsPath.toAbsolutePath());
            javax.swing.JOptionPane.showMessageDialog(null,
                    "No se encontró el archivo OBJ.dat en:\n" + objsPath.toAbsolutePath() +
                            "\n\nPor favor, configure la ruta de Dats correctamente.",
                    "Error al cargar Objetos",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
            objs = new HashMap<>();
            return;
        }

        final Map<Integer, ObjData> result = new HashMap<>();
        ObjData currentObj = null;

        try (BufferedReader br = Files.newBufferedReader(objsPath, StandardCharsets.ISO_8859_1)) {
            String line;
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("'") || trimmed.startsWith("#") || trimmed.startsWith(";"))
                    continue;

                if (trimmed.startsWith("[") && trimmed.contains("]")) {
                    String section = trimmed.substring(1, trimmed.indexOf(']')).trim();
                    if (section.regionMatches(true, 0, "OBJ", 0, 3)) {
                        String numPart = section.substring(3).trim();
                        try {
                            int objNumber = Integer.parseInt(numPart);
                            currentObj = new ObjData(objNumber);
                            result.put(objNumber, currentObj);
                        } catch (NumberFormatException e) {
                            currentObj = null;
                        }
                    } else {
                        currentObj = null;
                    }
                    continue;
                }

                if (currentObj == null)
                    continue;
                int eq = trimmed.indexOf('=');
                if (eq <= 0)
                    continue;

                String key = trimmed.substring(0, eq).trim();
                String value = trimmed.substring(eq + 1).trim();

                if (key.equalsIgnoreCase("Name")) {
                    currentObj.setName(value);
                } else if (key.equalsIgnoreCase("GrhIndex")) {
                    try {
                        currentObj.setGrhIndex(Integer.parseInt(value));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        } catch (IOException e) {
            Logger.error(e, "Could not read OBJ.dat from path: {}", objsPath.toAbsolutePath());
        }

        objs = result;
        Logger.info("Loaded {} OBJ definitions from {}", objs.size(), objsPath.toAbsolutePath());
    }

    /**
     * Carga los colores del minimapa desde MiniMap.dat si existe.
     * Formato: [GrhX] o [X] seguido de R, G, B.
     */
    private static void loadMiniMapColors() {
        final Path minimapPath = Path.of(options.getInitPath(), "MiniMap.dat");

        if (!Files.exists(minimapPath)) {
            Logger.info("MiniMap.dat no encontrado en {}. Se usará la heurística por defecto.",
                    minimapPath.toAbsolutePath());
            return;
        }

        try (BufferedReader br = Files.newBufferedReader(minimapPath, StandardCharsets.ISO_8859_1)) {
            String line;
            int currentGrh = -1;
            int r = 0, g = 0, b = 0;

            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("'") || trimmed.startsWith("#") || trimmed.startsWith(";"))
                    continue;

                if (trimmed.startsWith("[") && trimmed.contains("]")) {
                    // Guardar el anterior antes de empezar uno nuevo
                    if (currentGrh != -1) {
                        minimapColors.put(currentGrh, imgui.ImGui.getColorU32(r / 255f, g / 255f, b / 255f, 1.0f));
                    }

                    String section = trimmed.substring(1, trimmed.indexOf(']')).trim();
                    String numPart = section.replace("Grh", "");
                    try {
                        currentGrh = Integer.parseInt(numPart);
                        r = 0;
                        g = 0;
                        b = 0; // Reset
                    } catch (NumberFormatException e) {
                        currentGrh = -1;
                    }
                    continue;
                }

                if (currentGrh == -1)
                    continue;

                int eq = trimmed.indexOf('=');
                if (eq <= 0)
                    continue;

                String key = trimmed.substring(0, eq).trim();
                String value = trimmed.substring(eq + 1).trim();

                try {
                    if (key.equalsIgnoreCase("R"))
                        r = Integer.parseInt(value);
                    else if (key.equalsIgnoreCase("G"))
                        g = Integer.parseInt(value);
                    else if (key.equalsIgnoreCase("B"))
                        b = Integer.parseInt(value);
                } catch (NumberFormatException ignored) {
                }
            }
            // Guardar el último
            if (currentGrh != -1) {
                minimapColors.put(currentGrh, imgui.ImGui.getColorU32(r / 255f, g / 255f, b / 255f, 1.0f));
            }
            Logger.info("Cargados {} colores para el minimapa desde {}", minimapColors.size(), minimapPath);
        } catch (IOException e) {
            Logger.error(e, "Error al leer MiniMap.dat");
        }
    }

    /**
     * Carga y parsea el archivo de índices de gráficos (graphics.ind).
     * Reconstruye la jerarquía de animaciones y frames de GRH.
     */
    private static void loadGrhData() {
        byte[] data = loadLocalInitFile("Graficos.ind", "Gráficos", true);
        if (data == null)
            return;

        try {
            reader.init(data);

            final int fileVersion = reader.readInt();
            final int grhCount = reader.readInt();

            grhData = new GrhData[grhCount + 1];

            int grh = 0;
            grhData[0] = new GrhData();

            while (grh < grhCount) {
                grh = reader.readInt();

                grhData[grh] = new GrhData();
                grhData[grh].setNumFrames(reader.readShort());

                if (grhData[grh].getNumFrames() <= 0)
                    throw new IOException("getFrame(frame) ERROR IN THE GRHINDEX: " + grh);

                grhData[grh].setFrames(new int[grhData[grh].getNumFrames() + 1]);

                if (grhData[grh].getNumFrames() > 1) {
                    for (int i = 1; i <= grhData[grh].getNumFrames(); i++) {
                        grhData[grh].setFrame(i, reader.readInt());
                        if (grhData[grh].getFrame(i) <= 0)
                            throw new IOException("getFrame(frame) ERROR IN THE GRHINDEX: " + grh);
                    }

                    grhData[grh].setSpeed(reader.readFloat());
                    if (grhData[grh].getSpeed() <= 0)
                        throw new IOException("getSpeed ERROR IN THE GRHINDEX: " + grh);

                    grhData[grh].setPixelHeight(grhData[grhData[grh].getFrame(1)].getPixelHeight());

                    if (grhData[grh].getPixelHeight() <= 0)
                        throw new IOException("getPixelHeight ERROR IN THE GRHINDEX: " + grh);

                    grhData[grh].setPixelWidth(grhData[grhData[grh].getFrame(1)].getPixelWidth());
                    if (grhData[grh].getPixelWidth() <= 0)
                        throw new IOException("getPixelWidth ERROR IN THE GRHINDEX: " + grh);

                    grhData[grh].setTileWidth(grhData[grhData[grh].getFrame(1)].getTileWidth());
                    if (grhData[grh].getTileWidth() <= 0)
                        throw new IOException("getTileWidth ERROR IN THE GRHINDEX: " + grh);

                    grhData[grh].setTileHeight(grhData[grhData[grh].getFrame(1)].getTileHeight());
                    if (grhData[grh].getTileHeight() <= 0)
                        throw new IOException("getTileHeight ERROR IN THE GRHINDEX: " + grh);

                } else {
                    grhData[grh].setFileNum(reader.readInt());
                    if (grhData[grh].getFileNum() <= 0)
                        throw new IOException("getFileNum ERROR IN THE GRHINDEX: " + grh);

                    grhData[grh].setsX(reader.readShort());
                    if (grhData[grh].getsX() < 0)
                        throw new IOException("getsX ERROR IN THE GRHINDEX: " + grh);

                    grhData[grh].setsY(reader.readShort());
                    if (grhData[grh].getsY() < 0)
                        throw new IOException("getsY ERROR IN THE GRHINDEX: " + grh);

                    grhData[grh].setPixelWidth(reader.readShort());
                    if (grhData[grh].getPixelWidth() <= 0)
                        throw new IOException("getPixelWidth ERROR IN THE GRHINDEX: " + grh);

                    grhData[grh].setPixelHeight(reader.readShort());
                    if (grhData[grh].getPixelHeight() <= 0)
                        throw new IOException("getPixelHeight ERROR IN THE GRHINDEX: " + grh);

                    grhData[grh].setTileWidth((float) grhData[grh].getPixelWidth() / 32);
                    grhData[grh].setTileHeight((float) grhData[grh].getPixelHeight() / 32);
                    grhData[grh].setFrame(1, grh);
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Carga y almacena los datos de las cabezas desde el archivo "heads.ind".
     */
    private static void loadHeads() {
        byte[] data = loadLocalInitFile("Cabezas.ind", "Cabezas", true);
        if (data == null)
            return;

        reader.init(data);
        reader.skipBytes(263);

        final IndexHeads[] myHeads;
        final short numHeads = reader.readShort();
        headData = new HeadData[numHeads + 1];
        myHeads = new IndexHeads[numHeads + 1];

        headData[0] = new HeadData();
        for (int i = 1; i <= numHeads; i++) {
            myHeads[i] = new IndexHeads();
            myHeads[i].setHead(1, reader.readShort());
            myHeads[i].setHead(2, reader.readShort());
            myHeads[i].setHead(3, reader.readShort());
            myHeads[i].setHead(4, reader.readShort());

            headData[i] = new HeadData();
            if (myHeads[i].getHead(1) != 0) {
                headData[i].setHead(1, initGrh(headData[i].getHead(1), myHeads[i].getHead(1), false));
                headData[i].setHead(2, initGrh(headData[i].getHead(2), myHeads[i].getHead(2), false));
                headData[i].setHead(3, initGrh(headData[i].getHead(3), myHeads[i].getHead(3), false));
                headData[i].setHead(4, initGrh(headData[i].getHead(4), myHeads[i].getHead(4), false));
            }
        }

    }

    /**
     * Carga y almacena los datos de los cascos desde el archivo "helmets.ind".
     */
    private static void loadHelmets() {
        byte[] data = loadLocalInitFile("Cascos.ind", "Cascos", true);
        if (data == null)
            return;

        reader.init(data);
        reader.skipBytes(263);

        final IndexHeads[] myHeads;
        final short numHeads = reader.readShort();
        helmetsData = new HeadData[numHeads + 1];
        myHeads = new IndexHeads[numHeads + 1];

        helmetsData[0] = new HeadData();
        for (int i = 1; i <= numHeads; i++) {
            myHeads[i] = new IndexHeads();
            myHeads[i].setHead(1, reader.readShort());
            myHeads[i].setHead(2, reader.readShort());
            myHeads[i].setHead(3, reader.readShort());
            myHeads[i].setHead(4, reader.readShort());

            helmetsData[i] = new HeadData();
            if (myHeads[i].getHead(1) != 0) {
                helmetsData[i].setHead(1, initGrh(helmetsData[i].getHead(1), myHeads[i].getHead(1), false));
                helmetsData[i].setHead(2, initGrh(helmetsData[i].getHead(2), myHeads[i].getHead(2), false));
                helmetsData[i].setHead(3, initGrh(helmetsData[i].getHead(3), myHeads[i].getHead(3), false));
                helmetsData[i].setHead(4, initGrh(helmetsData[i].getHead(4), myHeads[i].getHead(4), false));
            }
        }

    }

    /**
     * Carga y almacena los datos de los cuerpos desde el archivo "bodys.ind".
     */
    private static void loadBodys() {
        // Intentamos cargar Personajes.ind primero sin mostrar error, si no existe
        // probamos Cuerpos.ind con error
        byte[] data = loadLocalInitFile("Personajes.ind", "Personajes", false);
        if (data == null) {
            data = loadLocalInitFile("Cuerpos.ind", "Cuerpos", true);
        }
        if (data == null)
            return;

        reader.init(data);
        reader.skipBytes(263);

        final IndexBodys[] myBodys;
        final short numBodys = reader.readShort();
        bodyData = new BodyData[numBodys + 1];
        myBodys = new IndexBodys[numBodys + 1];

        bodyData[0] = new BodyData();
        for (int i = 1; i <= numBodys; i++) {
            myBodys[i] = new IndexBodys();
            myBodys[i].setBody(1, reader.readShort());
            myBodys[i].setBody(2, reader.readShort());
            myBodys[i].setBody(3, reader.readShort());
            myBodys[i].setBody(4, reader.readShort());

            myBodys[i].setHeadOffsetX(reader.readShort());
            myBodys[i].setHeadOffsetY(reader.readShort());

            bodyData[i] = new BodyData();
            if (myBodys[i].getBody(1) != 0) {
                bodyData[i].setWalk(1, initGrh(bodyData[i].getWalk(1), myBodys[i].getBody(1), false));
                bodyData[i].setWalk(2, initGrh(bodyData[i].getWalk(2), myBodys[i].getBody(2), false));
                bodyData[i].setWalk(3, initGrh(bodyData[i].getWalk(3), myBodys[i].getBody(3), false));
                bodyData[i].setWalk(4, initGrh(bodyData[i].getWalk(4), myBodys[i].getBody(4), false));

                bodyData[i].getHeadOffset().setX(myBodys[i].getHeadOffsetX());
                bodyData[i].getHeadOffset().setY(myBodys[i].getHeadOffsetY());
            }
        }

    }

    /**
     * Carga y almacena los datos de las armas desde el archivo "arms.ind".
     */
    private static void loadWeapons() {
        byte[] data = loadLocalInitFile("Armas.ind", "Armas", true);
        if (data == null)
            return;

        reader.init(data);

        final int numArms = reader.readShort();
        weaponData = new WeaponData[numArms + 1];

        weaponData[0] = new WeaponData();
        for (int loopc = 1; loopc <= numArms; loopc++) {
            weaponData[loopc] = new WeaponData();
            weaponData[loopc].setWeaponWalk(1, initGrh(weaponData[loopc].getWeaponWalk(1), reader.readShort(), false));
            weaponData[loopc].setWeaponWalk(2, initGrh(weaponData[loopc].getWeaponWalk(2), reader.readShort(), false));
            weaponData[loopc].setWeaponWalk(3, initGrh(weaponData[loopc].getWeaponWalk(3), reader.readShort(), false));
            weaponData[loopc].setWeaponWalk(4, initGrh(weaponData[loopc].getWeaponWalk(4), reader.readShort(), false));
        }

    }

    /**
     * Carga y almacena los datos de los escudos desde el archivo "shields.ind".
     */
    private static void loadShields() {
        byte[] data = loadLocalInitFile("Escudos.ind", "Escudos", true);
        if (data == null)
            return;

        reader.init(data);

        final int numShields = reader.readShort();
        shieldData = new ShieldData[numShields + 1];

        shieldData[0] = new ShieldData();
        for (int loopc = 1; loopc <= numShields; loopc++) {
            shieldData[loopc] = new ShieldData();
            shieldData[loopc].setShieldWalk(1, initGrh(shieldData[loopc].getShieldWalk(1), reader.readShort(), false));
            shieldData[loopc].setShieldWalk(2, initGrh(shieldData[loopc].getShieldWalk(2), reader.readShort(), false));
            shieldData[loopc].setShieldWalk(3, initGrh(shieldData[loopc].getShieldWalk(3), reader.readShort(), false));
            shieldData[loopc].setShieldWalk(4, initGrh(shieldData[loopc].getShieldWalk(4), reader.readShort(), false));
        }

    }

    /**
     * Carga los datos de un mapa desde una ruta de archivo específica.
     * Útil para el modo editor cuando se cargan archivos .map externos.
     * Al seleccionar un .map, intenta cargar automáticamente archivos .dat e .inf
     * asociados.
     *
     * @param filePath Ruta absoluta al archivo .map
     */
    public static void loadMap(String filePath) {
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
                mapProperties = new MapProperties(); // Reset a valores por defecto si no existe
                Logger.info("Archivo .dat no encontrado en {}, usando valores por defecto.", datPath);
            }

            // Intentar cargar información de entidades (.inf)
            if (Files.exists(Path.of(infPath))) {
                loadMapInfo(infPath);
            } else {
                Logger.info("Archivo .inf no encontrado en {}, saltando la carga de entidades.", infPath);
            }

        } catch (IOException e) {
            System.err.println("Could not load map from path: " + filePath);
            e.printStackTrace();
        }
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

        mapProperties = props;
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
        Logger.info("Cargando información de entidades desde: {}", filePath);
        // TODO: Implementar parser de .inf para poblar mapData
    }

    /**
     * Carga un mapa por su número desde los recursos empaquetados.
     *
     * @param numMap Número del mapa a cargar.
     */
    public static void loadMap(int numMap) {
        byte[] data = readResource("resources/maps.ao", "mapa" + numMap);
        if (data == null) {
            System.err.println("Could not load mapa" + numMap + " data!");
            return;
        }
        initMap(data);
    }

    /**
     * Crea un mapa vacío de 100x100 tiles con un tile de base predeterminado (GRH
     * 1).
     */
    public static void createEmptyMap() {
        createEmptyMap((short) 1);
    }

    /**
     * Crea un mapa vacío de 100x100 tiles utilizando el GRH especificado para la
     * primera capa.
     *
     * @param baseLayer1GrhIndex Índice del GRH a usar como suelo base.
     */
    public static void createEmptyMap(short baseLayer1GrhIndex) {
        mapData = new MapData[101][101];
        mapData[0][0] = new MapData();

        for (int y = 1; y <= 100; y++) {
            for (int x = 1; x <= 100; x++) {
                MapData cell = new MapData();
                cell.setBlocked(false);
                cell.setTrigger(0);
                cell.setCharIndex(0);
                cell.setNpcIndex((short) 0);

                initGrhOrReset(cell.getLayer(1), baseLayer1GrhIndex, true);
                initGrhOrReset(cell.getLayer(2), (short) 0, true);
                initGrhOrReset(cell.getLayer(3), (short) 0, true);
                initGrhOrReset(cell.getLayer(4), (short) 0, true);
                initGrhOrReset(cell.getObjGrh(), (short) 0, false);

                mapData[x][y] = cell;
            }
        }

        Surface.INSTANCE.deleteAllTextures();
        eraseAllChars();
    }

    private static void initGrhOrReset(GrhInfo grh, short grhIndex, boolean started) {
        if (grhData != null && grhIndex >= 0 && grhIndex < grhData.length && grhData[grhIndex] != null) {
            initGrh(grh, grhIndex, started);
            return;
        }

        grh.setGrhIndex(grhIndex);
        grh.setStarted(false);
        grh.setLoops(0);
        grh.setFrameCounter(1);
        grh.setSpeed(0.4f);
    }

    /**
     * Método interno para procesar el parseo de los datos binarios de un mapa.
     * Lee cabeceras, flags, capas, bloqueos y triggers.
     *
     * @param data Datos binarios del mapa.
     */
    private static void initMap(byte[] data) {
        reader.init(data);

        mapData = new MapData[101][101];

        final short mapversion = reader.readShort();
        reader.skipBytes(263); // cabecera.

        byte byflags;

        // Falta implementar el mapInfo xd....
        reader.readShort();
        reader.readShort();
        reader.readShort();
        reader.readShort();

        byte bloq;

        mapData[0][0] = new MapData();

        for (int y = 1; y <= 100; y++) {
            for (int x = 1; x <= 100; x++) {
                mapData[x][y] = new MapData();

                byflags = reader.readByte();
                bloq = (byte) (byflags & 1);
                mapData[x][y].setBlocked(bloq == 1);

                mapData[x][y].getLayer(1).setGrhIndex(reader.readShort());
                mapData[x][y].setLayer(1,
                        initGrh(mapData[x][y].getLayer(1), mapData[x][y].getLayer(1).getGrhIndex(), true));

                if ((byte) (byflags & 2) != 0) {
                    mapData[x][y].getLayer(2).setGrhIndex(reader.readShort());
                    mapData[x][y].setLayer(2,
                            initGrh(mapData[x][y].getLayer(2), mapData[x][y].getLayer(2).getGrhIndex(), true));

                } else
                    mapData[x][y].getLayer(2).setGrhIndex(0);

                if ((byte) (byflags & 4) != 0) {
                    mapData[x][y].getLayer(3).setGrhIndex(reader.readShort());
                    mapData[x][y].setLayer(3,
                            initGrh(mapData[x][y].getLayer(3), mapData[x][y].getLayer(3).getGrhIndex(), true));
                } else
                    mapData[x][y].getLayer(3).setGrhIndex(0);

                if ((byte) (byflags & 8) != 0) {
                    mapData[x][y].getLayer(4).setGrhIndex(reader.readShort());
                    mapData[x][y].setLayer(4,
                            initGrh(mapData[x][y].getLayer(4), mapData[x][y].getLayer(4).getGrhIndex(), true));
                } else
                    mapData[x][y].getLayer(4).setGrhIndex(0);

                if ((byte) (byflags & 16) != 0)
                    mapData[x][y].setTrigger(reader.readShort());
                else
                    mapData[x][y].setTrigger(0);

                mapData[x][y].getObjGrh().setGrhIndex(0);
            }
        }

        // Liberar memoria
        Surface.INSTANCE.deleteAllTextures();
        eraseAllChars();
    }

    /**
     * Carga los efectos visuales (FXs) desde el archivo "fxs.ind".
     */
    private static void loadFxs() {
        byte[] data = loadLocalInitFile("Fxs.ind", "Fxs", true);
        if (data == null)
            return;

        reader.init(data);
        reader.skipBytes(263);

        final short numFXs = reader.readShort();
        fxData = new FxData[numFXs + 1];

        for (int i = 1; i <= numFXs; i++) {
            fxData[i] = new FxData();
            fxData[i].setAnimacion(reader.readShort());
            fxData[i].setOffsetX(reader.readShort());
            fxData[i].setOffsetY(reader.readShort());
        }
    }

    /**
     * Carga los indicadores de lluvia por mapa desde el archivo "fk.ind".
     */
    private static void loadFK() {
        byte[] data = loadLocalInitFile("Fk.ind", "Lluvia (Fk)", true);
        if (data == null)
            return;

        reader.init(data);
        reader.skipBytes(263);

        final short Nu = reader.readShort();
        bLluvia = new boolean[Nu + 1];

        bLluvia[0] = false;
        for (int i = 1; i <= Nu; i++)
            bLluvia[i] = reader.readByte() == 1;

    }

    /**
     * Inicializa una estructura GrhInfo asociándola al índice de gráfico
     * correspondiente.
     * Configura si la animación debe comenzar iniciada y resetea contadores.
     *
     * @param grh      Estructura a inicializar.
     * @param grhIndex Índice del gráfico en grhData.
     * @param started  Si la animación (si tiene >1 frame) debe comenzar
     *                 reproduciéndose.
     * @return La estructura GrhInfo inicializada.
     */
    public static GrhInfo initGrh(GrhInfo grh, short grhIndex, boolean started) {
        if (grh == null)
            throw new NullPointerException("Se esta intentando incializar un GrhInfo nulo...");

        grh.setGrhIndex(grhIndex);
        grh.setStarted(false);
        grh.setLoops(0);

        if (started)
            grh.setStarted(grhData[grh.getGrhIndex()].getNumFrames() > 1);

        if (grh.isStarted())
            grh.setLoops(-1);

        grh.setFrameCounter(1);
        // grh.setSpeed( grhData[grhIndex].getSpeed() );
        grh.setSpeed(0.4f);

        return grh;
    }

    /**
     * Carga un archivo de inicialización desde la ruta local configurada.
     * Muestra un mensaje de error si el archivo no existe.
     *
     * @param fileName     Nombre del archivo (ej: "Graficos.ind")
     * @param friendlyName Nombre amigable para el mensaje de error (ej: "Gráficos")
     * @return El contenido del archivo en bytes o null si falla.
     */
    private static byte[] loadLocalInitFile(String fileName, String friendlyName, boolean showError) {
        final Path filePath = Path.of(options.getInitPath(), fileName);

        if (!Files.exists(filePath)) {
            if (showError) {
                Logger.error("{} no encontrado en la ruta: {}", fileName, filePath.toAbsolutePath());
                javax.swing.JOptionPane.showMessageDialog(null,
                        "No se encontró el archivo " + fileName + " en:\n" + filePath.toAbsolutePath() +
                                "\n\nPor favor, configure la ruta de Inits correctamente.",
                        "Error al cargar " + friendlyName,
                        javax.swing.JOptionPane.ERROR_MESSAGE);
            }
            return null;
        }

        try {
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            Logger.error(e, "Error al leer {}", fileName);
            return null;
        }
    }

}
