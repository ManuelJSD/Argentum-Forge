package org.argentumforge.engine.utils;

import org.argentumforge.engine.game.Options;
import org.argentumforge.engine.game.models.Character;
import org.argentumforge.engine.renderer.Surface;
import org.argentumforge.engine.utils.inits.*;
import org.tinylog.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

import static org.argentumforge.engine.game.models.Character.eraseAllChars;

/**
 * Repositorio central de recursos y datos del editor.
 * Maneja la carga de índices (GRH, NPCs, etc.) y mantiene el estado del mapa
 * activo (MapContext).
 */

public final class GameData {

    /** Datos de la rejilla del mapa actual. */
    public static MapData[][] mapData;

    /** Lista global de personajes activos en el mundo. */
    public static Character[] charList = new Character[10000 + 1];

    /** Propiedades generales del mapa actual (.dat). */
    public static MapProperties mapProperties = new MapProperties();
    /** Instancia de configuración del usuario. */
    public static Options options = Options.INSTANCE;

    private static final List<MapContext> openMaps = new ArrayList<>();
    private static MapContext activeContext = null;

    public static List<MapContext> getOpenMaps() {
        return openMaps;
    }

    public static MapContext getActiveContext() {
        return activeContext;
    }

    /**
     * Establece el contexto activo y sincroniza los campos estáticos para
     * compatibilidad heredada.
     */
    public static void setActiveContext(MapContext context) {
        if (context == null)
            return;

        // Guardar estado del contexto anterior si existe (y si es diferente al nuevo)
        if (activeContext != null && activeContext != context) {
            activeContext.setLastChar(org.argentumforge.engine.game.models.Character.lastChar);
        }

        activeContext = context;
        mapData = context.getMapData();
        mapProperties = context.getMapProperties();
        charList = context.getCharList();

        // Restaurar lastChar del nuevo contexto
        org.argentumforge.engine.game.models.Character.lastChar = context.getLastChar();

        if (!openMaps.contains(context)) {
            openMaps.add(context);
        }
        updateWindowTitle();
    }

    public static void clearActiveContext() {
        if (activeContext != null) {
            activeContext.setLastChar(org.argentumforge.engine.game.models.Character.lastChar);
        }
        activeContext = null;
        updateWindowTitle();
    }

    public static void updateWindowTitle() {
        if (activeContext != null) {
            String title = activeContext.getMapName();
            if (activeContext.isModified()) {
                title += " *";
            }
            org.argentumforge.engine.Window.INSTANCE.updateTitle(title);
        } else {
            org.argentumforge.engine.Window.INSTANCE.updateTitle("");
        }
    }

    public static void closeMap(MapContext context) {
        openMaps.remove(context);
        if (activeContext == context) {
            if (!openMaps.isEmpty()) {
                setActiveContext(openMaps.get(openMaps.size() - 1));
            } else {
                activeContext = null;
                mapData = null;
                org.argentumforge.engine.utils.MapManager.createEmptyMap(100, 100);
            }
        }
    }

    /** Tamaño del mapa */
    public static final int X_MIN_MAP_SIZE = 1;
    public static final int X_MAX_MAP_SIZE = 100;
    public static final int Y_MIN_MAP_SIZE = 1;
    public static final int Y_MAX_MAP_SIZE = 100;

    /** Lector de datos binarios persistente para la carga de recursos. */
    static BinaryDataReader reader;

    /**
     * Inicializamos todos los datos almacenados en archivos.
     */
    public static void init() {
        for (int i = 0; i < charList.length; i++)
            charList[i] = new Character();

        reader = new BinaryDataReader();
        options.load();
        org.argentumforge.engine.i18n.I18n.INSTANCE.loadLanguage(options.getLanguage());

        if (checkResources()) {
            loadNpcs();
            loadObjs();
            loadMiniMapColors();
            loadGrhData();
            loadHeads();
            loadHelmets();
            loadBodys();
            loadWeapons();
            loadShields();
            loadFxs();

            // loadMessages(options.getLanguage()); -> Eliminado ya que el sistema de
            // mensajes fue borrado
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
        AssetRegistry.npcs = ResourceLoader.loadNpcs();

        if (AssetRegistry.npcs.isEmpty()) {
            final Path npcsPath = Path.of(options.getDatsPath(), "NPCs.dat");
            javax.swing.JOptionPane.showMessageDialog(null,
                    "No se encontró o no se pudo leer NPCs.dat en:\n" + npcsPath.toAbsolutePath() +
                            "\n\nPor favor, configure la ruta de Dats correctamente.",
                    "Error al cargar NPCs",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Carga las definiciones de Objetos desde el archivo externo especificado en
     * las opciones.
     * Parsea el archivo .dat (estilo INI) para extraer nombre y grhIndex.
     */
    private static void loadObjs() {
        AssetRegistry.objs = ResourceLoader.loadObjs();

        if (AssetRegistry.objs.isEmpty()) {
            final Path objsPath = Path.of(options.getDatsPath(), "OBJ.dat");
            javax.swing.JOptionPane.showMessageDialog(null,
                    "No se encontró o no se pudo leer OBJ.dat en:\n" + objsPath.toAbsolutePath() +
                            "\n\nPor favor, configure la ruta de Dats correctamente.",
                    "Error al cargar Objetos",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Carga los colores del minimapa desde MiniMap.dat si existe.
     * Formato: [GrhX] o [X] seguido de R, G, B.
     */
    private static void loadMiniMapColors() {
        // 1. Intentar cargar binario (más rápido y completo)
        Path binPath = Path.of("minimap.bin");
        if (Files.exists(binPath)) {
            try {
                byte[] bytes = Files.readAllBytes(binPath);
                java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(bytes);
                buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);

                int count = bytes.length / 4;
                for (int i = 0; i < count; i++) {
                    int color = buffer.getInt();
                    if (color != 0) {
                        // VB6 RGB Format: 0x00BBGGRR
                        int r = color & 0xFF;
                        int g = (color >> 8) & 0xFF;
                        int b = (color >> 16) & 0xFF;

                        // Pack manually to avoid ImGui crash. Revert to ABGR (Standard ImGui)
                        // Pack: (A << 24) | (B << 16) | (G << 8) | R
                        int packed = (0xFF << 24) | (b << 16) | (g << 8) | r;
                        AssetRegistry.minimapColors.put(i + 1, packed);
                    }
                }
                Logger.info("Cargados {} colores minimapa desde BIN.", count);
                return; // Éxito, no cargar .dat
            } catch (IOException e) {
                Logger.error(e, "Error al cargar minimap.bin");
            }
        }

        // 2. Fallback a MiniMap.dat (Legacy/Editable)
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
                        if (currentGrh != -1) {
                            int packed = (0xFF << 24) | (b << 16) | (g << 8) | r;
                            AssetRegistry.minimapColors.put(currentGrh, packed);
                        }
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
                if (currentGrh != -1) {
                    int packed = (0xFF << 24) | (b << 16) | (g << 8) | r;
                    AssetRegistry.minimapColors.put(currentGrh, packed);
                }
            }
            Logger.info("Cargados {} colores para el minimapa desde {}", AssetRegistry.minimapColors.size(),
                    minimapPath);
        } catch (IOException e) {
            Logger.error(e, "Error al leer MiniMap.dat");
        }
    }

    /**
     * Carga y parsea el archivo de índices de gráficos (graphics.ind).
     * Intenta detectar automáticamente el formato (Standard, Legacy Integer, Con
     * Cabecera).
     */
    private static void loadGrhData() {
        byte[] data = loadLocalInitFile("Graficos.ind", "Gráficos", true);
        if (data == null)
            return;

        // Intentar cargar con diferentes estrategias
        if (attemptLoadGrh(data, false, false)) { // Standard (Long Index, No Header)
            Logger.info("Formato Grh detectado: Standard (Long Index)");
            return;
        }

        if (attemptLoadGrh(data, true, false)) { // Header + Standard
            Logger.info("Formato Grh detectado: Custom Header + Standard (Long Index)");
            return;
        }

        if (attemptLoadGrh(data, false, true)) { // Legacy (Integer Index, No Header)
            Logger.info("Formato Grh detectado: Legacy (Integer Index)");
            return;
        }

        if (attemptLoadGrh(data, true, true)) { // Header + Legacy
            Logger.info("Formato Grh detectado: Custom Header + Legacy (Integer Index)");
            return;
        }

        Logger.error("No se pudo detectar el formato de Graficos.ind (o el archivo esta corrupto).");
        javax.swing.JOptionPane.showMessageDialog(null,
                "No se pudo cargar 'Graficos.ind'.\n\n" +
                        "El editor intentó detectar automáticamente el formato (0.13.0, 0.11.5, Con cabecera), pero falló.\n"
                        +
                        "Posibles causas:\n" +
                        "1. El archivo está corrupto o vacío.\n" +
                        "2. Es un formato muy antiguo o muy modificado no soportado.\n" +
                        "3. No es un archivo de índices de Argentum Online válido.",
                "Error de Formato en Graficos.ind",
                javax.swing.JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Intenta cargar los datos de Grh con una configuración específica.
     * 
     * @param data            Raw byte data
     * @param useHeaderOffset Si true, salta 263 bytes de cabecera custom
     * @param useIntegerIndex Si true, lee los indices como Short (2 bytes) en lugar
     *                        de Int
     * @return true si la carga fue exitosa, false si falló
     */
    private static boolean attemptLoadGrh(byte[] data, boolean useHeaderOffset, boolean useIntegerIndex) {
        BinaryDataReader reader = new BinaryDataReader();
        reader.init(data);

        try {
            if (useHeaderOffset) {
                // Cabecera: Desc(255) + CRC(4) + MagicWord(4) = 263 bytes
                if (data.length < 263)
                    return false;
                reader.skipBytes(263);
            }

            final int fileVersion = reader.readInt();
            final int grhCount = reader.readInt();

            // Sanity check básico
            if (grhCount <= 0 || grhCount > 200000)
                return false;

            GrhData[] tempGrhData = new GrhData[grhCount + 1];
            tempGrhData[0] = new GrhData();

            int processed = 0;
            while (reader.hasRemaining() && processed < grhCount) {
                int grh;
                if (useIntegerIndex) {
                    grh = reader.readShort() & 0xFFFF; // Unsigned Short
                } else {
                    grh = reader.readInt();
                }

                // Sanity check de índice
                if (grh <= 0 || grh >= tempGrhData.length) {
                    // Si el índice está fuera de rango pero la versión del archivo es vieja,
                    // a veces grhCount no coincide exactamente con el ID máximo.
                    // Pero para seguridad, si el ID es absurdo, asumimos formato incorrecto.
                    if (grh > 200000)
                        return false;

                    // Expandir array si es necesario (casos dinámicos raros) o fallar
                    if (grh >= tempGrhData.length)
                        return false;
                }

                tempGrhData[grh] = new GrhData();
                tempGrhData[grh].setNumFrames(reader.readShort());

                if (tempGrhData[grh].getNumFrames() <= 0)
                    return false;

                tempGrhData[grh].setFrames(new int[tempGrhData[grh].getNumFrames() + 1]);

                if (tempGrhData[grh].getNumFrames() > 1) {
                    for (int i = 1; i <= tempGrhData[grh].getNumFrames(); i++) {
                        tempGrhData[grh].setFrame(i, reader.readInt());
                        if (tempGrhData[grh].getFrame(i) <= 0)
                            return false;
                    }

                    tempGrhData[grh].setSpeed(reader.readFloat());
                    if (tempGrhData[grh].getSpeed() <= 0)
                        return false;

                    // Referenciamos temporalmente al frame 1, PERO debemos tener cuidado:
                    // Si el frame 1 apunta a un Grh que AUN NO LEIMOS, esto fallará
                    // (NullPointerException).
                    // En los formatos AO, los frames suelen apuntar a gráficos base (que ya
                    // deberían estar cargados o ser simples).
                    // Pero la lógica original accedía a `AssetRegistry.grhData[...]`.
                    // Aquí estamos llenando `tempGrhData`.
                    // Si el gráfico base no está en `tempGrhData` aún, crasheará.
                    // PERO: La lógica original asumía orden secuencial o que las referencias ya
                    // existen?
                    // EL ORIGINAL usaba `AssetRegistry.grhData`. Si `AssetRegistry.grhData[frame1]`
                    // es null, crash.
                    // Así que el orden de carga en el archivo IMPORTA.
                    // Para validar el formato, PODEMOS simplemente leer los bytes sin validar
                    // semántica profunda,
                    // O debemos simular la lectura.

                    // Si solo queremos validar formato, leeremos pixels width/height directamente
                    // del archivo si NO es animación?
                    // NO, el formato dice: si frames > 1, copiamos pixelWidth del frame(1).
                    // Esto es un problema para la validación pura porque requiere dependencias
                    // cruzadas.

                    // SOLUCIÓN: Para validar formato, capturamos NullPointerException también como
                    // fallo de formato?
                    // O mejor: Usamos un "Dummy" lookup o posponemos la validación semántica.
                    // Dado que estamos reemplazando `loadGrhData` real, necesitamos que funcione de
                    // verdad.

                    // Si el formato original fallaba por dependencias circulares, fallaba siempre.
                    // Asumiremos que el archivo está bien formado semánticamente si logramos leerlo
                    // sintácticamente.
                    // Para evitar NPE durante el "dry run" o carga real:
                    // Usaremos `tempGrhData` para los lookups.

                    int firstFrame = tempGrhData[grh].getFrame(1);
                    if (firstFrame > 0 && firstFrame < tempGrhData.length && tempGrhData[firstFrame] != null) {
                        tempGrhData[grh].setPixelHeight(tempGrhData[firstFrame].getPixelHeight());
                        tempGrhData[grh].setPixelWidth(tempGrhData[firstFrame].getPixelWidth());
                        tempGrhData[grh].setTileWidth(tempGrhData[firstFrame].getTileWidth());
                        tempGrhData[grh].setTileHeight(tempGrhData[firstFrame].getTileHeight());
                    } else {
                        // Si falla la referencia, técnicamente el archivo podría ser valido pero estar
                        // desordenado.
                        // Sin embargo, en AO standard los frames base siempre están definidos antes o
                        // son idx bajos.
                        // Si no podemos resolverlo ahora, ponemos valores por defecto o fallamos.
                        // Pero cuidado: Si ponemos 0, luego la app puede fallar.
                        // Vamos a capturar NPE.
                    }

                } else {
                    tempGrhData[grh].setFileNum(reader.readInt());
                    if (tempGrhData[grh].getFileNum() <= 0)
                        return false;

                    tempGrhData[grh].setsX(reader.readShort());
                    if (tempGrhData[grh].getsX() < 0)
                        return false;

                    tempGrhData[grh].setsY(reader.readShort());
                    if (tempGrhData[grh].getsY() < 0)
                        return false;

                    tempGrhData[grh].setPixelWidth(reader.readShort());
                    if (tempGrhData[grh].getPixelWidth() <= 0)
                        return false;

                    tempGrhData[grh].setPixelHeight(reader.readShort());
                    if (tempGrhData[grh].getPixelHeight() <= 0)
                        return false;

                    tempGrhData[grh].setTileWidth((float) tempGrhData[grh].getPixelWidth() / 32);
                    tempGrhData[grh].setTileHeight((float) tempGrhData[grh].getPixelHeight() / 32);
                    tempGrhData[grh].setFrame(1, grh);
                }

                processed++;
            }

            // Si llegamos aquí, la carga sintáctica fue exitosa.
            // Asignamos el resultado al registro global.
            AssetRegistry.grhData = tempGrhData;
            return true;

        } catch (Exception e) {
            // BufferUnderflow, IOException, NPE -> Asumimos formato incorrecto
            return false;
        }
    }

    /**
     * Helper to detect if a file has a 263-byte custom header based on file size
     * and entry count.
     * Uses heuristics to handle padding or junk at end of file.
     * 
     * @param data      Raw file data
     * @param entrySize Size in bytes of a single entry in the array
     * @return true if header is detected, false otherwise
     */
    private static boolean detectHeader(byte[] data, int entrySize) {
        if (data.length < 2)
            return false;

        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);

        // Strategy 1: Check if "No Header" structure matches valid size
        boolean validNoHeader = false;
        short countNoHeader = buffer.getShort(0);
        if (countNoHeader > 0) {
            long expectedSize = 2 + (long) countNoHeader * entrySize;
            // File must be at least this big. Allow up to 1KB junk/padding at end.
            if (data.length >= expectedSize && (data.length - expectedSize) < 1024) {
                validNoHeader = true;
            }
        }

        // Strategy 2: Check if "Header" structure matches valid size
        boolean validHeader = false;
        if (data.length >= 263 + 2) {
            short countWithHeader = buffer.getShort(263);
            if (countWithHeader > 0) {
                long expectedSize = 263 + 2 + (long) countWithHeader * entrySize;
                if (data.length >= expectedSize && (data.length - expectedSize) < 1024) {
                    validHeader = true;
                }
            }
        }

        // Decision logic
        if (validHeader && !validNoHeader)
            return true;
        if (!validHeader && validNoHeader)
            return false;

        // Ambiguous or neither valid by size. Fallback to Content Heuristic.
        // Headers usually start with text (ASCII strings like "Cabezas...",
        // "Argentum...").
        // Binary counts (short) usually have a 0 high-byte for counts < 256.
        // Or just check if first few bytes look like text.

        int printableAsciiCount = 0;
        for (int i = 0; i < Math.min(10, data.length); i++) {
            if (data[i] >= 32 && data[i] <= 126)
                printableAsciiCount++;
        }

        // If start is mostly text, it's likely a header
        if (printableAsciiCount > 8)
            return true;

        // Default to false (No header) if unsure, but for Cabezas/etc in Mods, Header
        // is common.
        // However, if we return false incorrectly, we crash.
        // If we return *true* incorrectly, we skip 263 bytes.

        // If we are here, strict size checks failed.
        // If header candidate looked plausible (count > 0) but size mismatch was large?

        return validHeader; // Prefer validHeader if it passed size check, otherwise false.
    }

    /**
     * Carga y almacena los datos de las cabezas desde el archivo "heads.ind".
     */
    private static void loadHeads() {
        byte[] data = loadLocalInitFile("Cabezas.ind", "Cabezas", true);
        if (data == null)
            return;

        boolean hasHeader = detectHeader(data, 8); // 4 shorts = 8 bytes

        try {
            reader.init(data);
            if (hasHeader)
                reader.skipBytes(263);

            final IndexHeads[] myHeads;
            final short numHeads = reader.readShort();
            AssetRegistry.headData = new HeadData[numHeads + 1];
            myHeads = new IndexHeads[numHeads + 1];

            AssetRegistry.headData[0] = new HeadData();
            for (int i = 1; i <= numHeads; i++) {
                myHeads[i] = new IndexHeads();
                myHeads[i].setHead(1, reader.readShort());
                myHeads[i].setHead(2, reader.readShort());
                myHeads[i].setHead(3, reader.readShort());
                myHeads[i].setHead(4, reader.readShort());

                AssetRegistry.headData[i] = new HeadData();
                if (myHeads[i].getHead(1) != 0) {
                    AssetRegistry.headData[i].setHead(1,
                            initGrh(AssetRegistry.headData[i].getHead(1), myHeads[i].getHead(1), false));
                    AssetRegistry.headData[i].setHead(2,
                            initGrh(AssetRegistry.headData[i].getHead(2), myHeads[i].getHead(2), false));
                    AssetRegistry.headData[i].setHead(3,
                            initGrh(AssetRegistry.headData[i].getHead(3), myHeads[i].getHead(3), false));
                    AssetRegistry.headData[i].setHead(4,
                            initGrh(AssetRegistry.headData[i].getHead(4), myHeads[i].getHead(4), false));
                }
            }
        } catch (Exception e) {
            Logger.error(e, "Error al cargar Cabezas.ind");
            javax.swing.JOptionPane.showMessageDialog(null,
                    "Error al cargar 'Cabezas.ind'.\n" +
                            "El archivo podría estar corrupto o ser de una versión no soportada.\n\n" +
                            "Detalle: " + e.getMessage(),
                    "Error de Formato en Cabezas",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
            AssetRegistry.headData = new HeadData[1]; // Evitar NPE posteriores
            AssetRegistry.headData[0] = new HeadData();
        }

    }

    /**
     * Carga y almacena los datos de los cascos desde el archivo "helmets.ind".
     */
    private static void loadHelmets() {
        byte[] data = loadLocalInitFile("Cascos.ind", "Cascos", true);
        if (data == null)
            return;

        boolean hasHeader = detectHeader(data, 8); // 4 shorts = 8 bytes

        try {
            reader.init(data);
            if (hasHeader)
                reader.skipBytes(263);

            final IndexHeads[] myHeads;
            final short numHeads = reader.readShort();
            AssetRegistry.helmetsData = new HeadData[numHeads + 1];
            myHeads = new IndexHeads[numHeads + 1];

            AssetRegistry.helmetsData[0] = new HeadData();
            for (int i = 1; i <= numHeads; i++) {
                myHeads[i] = new IndexHeads();
                myHeads[i].setHead(1, reader.readShort());
                myHeads[i].setHead(2, reader.readShort());
                myHeads[i].setHead(3, reader.readShort());
                myHeads[i].setHead(4, reader.readShort());

                AssetRegistry.helmetsData[i] = new HeadData();
                if (myHeads[i].getHead(1) != 0) {
                    AssetRegistry.helmetsData[i].setHead(1,
                            initGrh(AssetRegistry.helmetsData[i].getHead(1), myHeads[i].getHead(1), false));
                    AssetRegistry.helmetsData[i].setHead(2,
                            initGrh(AssetRegistry.helmetsData[i].getHead(2), myHeads[i].getHead(2), false));
                    AssetRegistry.helmetsData[i].setHead(3,
                            initGrh(AssetRegistry.helmetsData[i].getHead(3), myHeads[i].getHead(3), false));
                    AssetRegistry.helmetsData[i].setHead(4,
                            initGrh(AssetRegistry.helmetsData[i].getHead(4), myHeads[i].getHead(4), false));
                }
            }
        } catch (Exception e) {
            Logger.error(e, "Error al cargar Cascos.ind");
            javax.swing.JOptionPane.showMessageDialog(null,
                    "Error al cargar 'Cascos.ind'.\n" +
                            "El archivo podría estar corrupto o ser de una versión no soportada.\n\n" +
                            "Detalle: " + e.getMessage(),
                    "Error de Formato en Cascos",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
            AssetRegistry.helmetsData = new HeadData[1];
            AssetRegistry.helmetsData[0] = new HeadData();
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

        boolean hasHeader = detectHeader(data, 12); // Body(4 shorts) + Offset(2 shorts) = 12 bytes

        try {
            reader.init(data);
            if (hasHeader)
                reader.skipBytes(263);

            final IndexBodys[] myBodys;
            final short numBodys = reader.readShort();
            AssetRegistry.bodyData = new BodyData[numBodys + 1];
            myBodys = new IndexBodys[numBodys + 1];

            AssetRegistry.bodyData[0] = new BodyData();
            for (int i = 1; i <= numBodys; i++) {
                myBodys[i] = new IndexBodys();
                myBodys[i].setBody(1, reader.readShort());
                myBodys[i].setBody(2, reader.readShort());
                myBodys[i].setBody(3, reader.readShort());
                myBodys[i].setBody(4, reader.readShort());

                myBodys[i].setHeadOffsetX(reader.readShort());
                myBodys[i].setHeadOffsetY(reader.readShort());

                AssetRegistry.bodyData[i] = new BodyData();
                if (myBodys[i].getBody(1) != 0) {
                    AssetRegistry.bodyData[i].setWalk(1,
                            initGrh(AssetRegistry.bodyData[i].getWalk(1), myBodys[i].getBody(1), false));
                    AssetRegistry.bodyData[i].setWalk(2,
                            initGrh(AssetRegistry.bodyData[i].getWalk(2), myBodys[i].getBody(2), false));
                    AssetRegistry.bodyData[i].setWalk(3,
                            initGrh(AssetRegistry.bodyData[i].getWalk(3), myBodys[i].getBody(3), false));
                    AssetRegistry.bodyData[i].setWalk(4,
                            initGrh(AssetRegistry.bodyData[i].getWalk(4), myBodys[i].getBody(4), false));

                    AssetRegistry.bodyData[i].getHeadOffset().setX(myBodys[i].getHeadOffsetX());
                    AssetRegistry.bodyData[i].getHeadOffset().setY(myBodys[i].getHeadOffsetY());
                }
            }
        } catch (Exception e) {
            Logger.error(e, "Error al cargar Cuerpos.ind/Personajes.ind");
            javax.swing.JOptionPane.showMessageDialog(null,
                    "Error al cargar 'Cuerpos.ind' o 'Personajes.ind'.\n" +
                            "El archivo podría estar corrupto o ser de una versión no soportada.\n\n" +
                            "Detalle: " + e.getMessage(),
                    "Error de Formato en Cuerpos",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
            AssetRegistry.bodyData = new BodyData[1];
            AssetRegistry.bodyData[0] = new BodyData();
        }

    }

    /**
     * Carga y almacena los datos de las armas desde el archivo "arms.ind".
     */
    private static void loadWeapons() {
        byte[] data = loadLocalInitFile("Armas.ind", "Armas", false);
        if (data == null) {
            loadWeaponsFromDat();
            return;
        }

        boolean hasHeader = detectHeader(data, 8); // 4 shorts = 8 bytes

        try {
            reader.init(data);
            if (hasHeader)
                reader.skipBytes(263);

            final int numArms = reader.readShort();
            AssetRegistry.weaponData = new WeaponData[numArms + 1];

            AssetRegistry.weaponData[0] = new WeaponData();
            for (int loopc = 1; loopc <= numArms; loopc++) {
                AssetRegistry.weaponData[loopc] = new WeaponData();
                AssetRegistry.weaponData[loopc].setWeaponWalk(1,
                        initGrh(AssetRegistry.weaponData[loopc].getWeaponWalk(1), reader.readShort(), false));
                AssetRegistry.weaponData[loopc].setWeaponWalk(2,
                        initGrh(AssetRegistry.weaponData[loopc].getWeaponWalk(2), reader.readShort(), false));
                AssetRegistry.weaponData[loopc].setWeaponWalk(3,
                        initGrh(AssetRegistry.weaponData[loopc].getWeaponWalk(3), reader.readShort(), false));
                AssetRegistry.weaponData[loopc].setWeaponWalk(4,
                        initGrh(AssetRegistry.weaponData[loopc].getWeaponWalk(4), reader.readShort(), false));
            }
        } catch (Exception e) {
            Logger.error(e, "Error al cargar Armas.ind");
            javax.swing.JOptionPane.showMessageDialog(null,
                    "Error al cargar 'Armas.ind'.\n" +
                            "El archivo podría estar corrupto o ser de una versión no soportada.\n\n" +
                            "Detalle: " + e.getMessage(),
                    "Error de Formato en Armas",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
            AssetRegistry.weaponData = new WeaponData[1];
            AssetRegistry.weaponData[0] = new WeaponData();
        }

    }

    /**
     * Carga las definiciones de Armas desde Armas.dat (formato INI) si no se
     * encuentra la versión binaria.
     */
    private static void loadWeaponsFromDat() {
        Path weaponsPath = Path.of(options.getDatsPath(), "Armas.dat");
        if (!Files.exists(weaponsPath)) {
            // Intentar en InitPath como fallback
            weaponsPath = Path.of(options.getInitPath(), "Armas.dat");
            if (!Files.exists(weaponsPath)) {
                Logger.error("Armas.ind y Armas.dat no encontrados.");
                javax.swing.JOptionPane.showMessageDialog(null,
                        "No se encontró el archivo de armas (Armas.ind o Armas.dat).\n" +
                                "Por favor, configure las rutas correctamente.",
                        "Error al cargar Armas",
                        javax.swing.JOptionPane.ERROR_MESSAGE);
                AssetRegistry.weaponData = new WeaponData[1];
                AssetRegistry.weaponData[0] = new WeaponData();
                return;
            }
        }

        // Estructura temporal para almacenar las armas mientras leemos
        Map<Integer, WeaponData> tempWeapons = new HashMap<>();
        int maxWeaponId = 0;
        int numArmsFromInit = 0;

        try (BufferedReader br = Files.newBufferedReader(weaponsPath, StandardCharsets.ISO_8859_1)) {
            String line;
            WeaponData currentWeapon = null;
            int currentWeaponId = -1;

            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("'") || trimmed.startsWith("#") || trimmed.startsWith(";"))
                    continue;

                if (trimmed.startsWith("[") && trimmed.contains("]")) {
                    String section = trimmed.substring(1, trimmed.indexOf(']')).trim();

                    if (section.equalsIgnoreCase("INIT")) {
                        currentWeapon = null; // No estamos en un arma
                        continue;
                    }

                    if (section.regionMatches(true, 0, "ARMA", 0, 4)) {
                        String numPart = section.substring(4).trim();
                        try {
                            currentWeaponId = Integer.parseInt(numPart);
                            currentWeapon = new WeaponData();
                            tempWeapons.put(currentWeaponId, currentWeapon);
                            if (currentWeaponId > maxWeaponId)
                                maxWeaponId = currentWeaponId;
                        } catch (NumberFormatException e) {
                            currentWeapon = null;
                        }
                    } else {
                        currentWeapon = null;
                    }
                    continue;
                }

                int eq = trimmed.indexOf('=');
                if (eq <= 0)
                    continue;

                String key = trimmed.substring(0, eq).trim();
                String value = trimmed.substring(eq + 1).trim();
                // Remover comentarios integrados si los hay (ej: 4719 'norte)
                if (value.contains("'")) {
                    value = value.substring(0, value.indexOf('\'')).trim();
                }

                if (key.equalsIgnoreCase("NumArmas")) {
                    try {
                        numArmsFromInit = Integer.parseInt(value);
                    } catch (NumberFormatException ignored) {
                    }
                    continue;
                }

                if (currentWeapon == null)
                    continue;

                try {
                    short grhIndex = Short.parseShort(value);
                    if (key.equalsIgnoreCase("Dir1")) {
                        // Dir1 -> Norte (Índice 1)
                        initGrh(currentWeapon.getWeaponWalk(1), grhIndex, false);
                    } else if (key.equalsIgnoreCase("Dir2")) {
                        // Dir2 -> Este (Índice 2)
                        initGrh(currentWeapon.getWeaponWalk(2), grhIndex, false);
                    } else if (key.equalsIgnoreCase("Dir3")) {
                        // Dir3 -> Oeste (Índice 4) - Mapeo para llenar slots
                        initGrh(currentWeapon.getWeaponWalk(4), grhIndex, false);
                    } else if (key.equalsIgnoreCase("Dir4")) {
                        // Dir4 -> Sur (Índice 3) - Según formato AO
                        initGrh(currentWeapon.getWeaponWalk(3), grhIndex, false);
                    }
                } catch (NumberFormatException ignored) {
                }
            }

        } catch (IOException e) {
            Logger.error(e, "Error al leer Armas.dat desde: {}", weaponsPath.toAbsolutePath());
        }

        // Inicializar array final
        int finalSize = Math.max(numArmsFromInit, maxWeaponId);
        AssetRegistry.weaponData = new WeaponData[finalSize + 1];
        AssetRegistry.weaponData[0] = new WeaponData();

        for (Map.Entry<Integer, WeaponData> entry : tempWeapons.entrySet()) {
            int id = entry.getKey();
            if (id <= finalSize) {
                AssetRegistry.weaponData[id] = entry.getValue();
            }
        }

        // Llenar huecos con armas vacías para evitar nulls
        for (int i = 1; i <= finalSize; i++) {
            if (AssetRegistry.weaponData[i] == null) {
                AssetRegistry.weaponData[i] = new WeaponData();
            }
        }

        Logger.info("Cargadas {} armas desde {}", tempWeapons.size(), weaponsPath.toAbsolutePath());
    }

    /**
     * Carga y almacena los datos de los escudos desde el archivo "shields.ind".
     */
    private static void loadShields() {
        byte[] data = loadLocalInitFile("Escudos.ind", "Escudos", false);
        if (data == null) {
            loadShieldsFromDat();
            return;
        }

        boolean hasHeader = detectHeader(data, 8); // 4 shorts = 8 bytes

        try {
            reader.init(data);
            if (hasHeader)
                reader.skipBytes(263);

            final int numShields = reader.readShort();
            AssetRegistry.shieldData = new ShieldData[numShields + 1];

            AssetRegistry.shieldData[0] = new ShieldData();
            for (int loopc = 1; loopc <= numShields; loopc++) {
                AssetRegistry.shieldData[loopc] = new ShieldData();
                AssetRegistry.shieldData[loopc].setShieldWalk(1,
                        initGrh(AssetRegistry.shieldData[loopc].getShieldWalk(1), reader.readShort(), false));
                AssetRegistry.shieldData[loopc].setShieldWalk(2,
                        initGrh(AssetRegistry.shieldData[loopc].getShieldWalk(2), reader.readShort(), false));
                AssetRegistry.shieldData[loopc].setShieldWalk(3,
                        initGrh(AssetRegistry.shieldData[loopc].getShieldWalk(3), reader.readShort(), false));
                AssetRegistry.shieldData[loopc].setShieldWalk(4,
                        initGrh(AssetRegistry.shieldData[loopc].getShieldWalk(4), reader.readShort(), false));
            }
        } catch (Exception e) {
            Logger.error(e, "Error al cargar Escudos.ind");
            javax.swing.JOptionPane.showMessageDialog(null,
                    "Error al cargar 'Escudos.ind'.\n" +
                            "El archivo podría estar corrupto o ser de una versión no soportada.\n\n" +
                            "Detalle: " + e.getMessage(),
                    "Error de Formato en Escudos",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
            AssetRegistry.shieldData = new ShieldData[1];
            AssetRegistry.shieldData[0] = new ShieldData();
        }

    }

    /**
     * Carga las definiciones de Escudos desde Escudos.dat (formato INI) si no se
     * encuentra la versión binaria.
     */
    private static void loadShieldsFromDat() {
        Path shieldsPath = Path.of(options.getDatsPath(), "Escudos.dat");
        if (!Files.exists(shieldsPath)) {
            shieldsPath = Path.of(options.getInitPath(), "Escudos.dat");
            if (!Files.exists(shieldsPath)) {
                return; // Silencioso, no es critico
            }
        }

        Map<Integer, ShieldData> tempShields = new HashMap<>();
        int maxShieldId = 0;
        int numShieldsFromInit = 0;

        try (BufferedReader br = Files.newBufferedReader(shieldsPath, StandardCharsets.ISO_8859_1)) {
            String line;
            ShieldData currentShield = null;
            int currentShieldId = -1;

            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("'") || trimmed.startsWith("#") || trimmed.startsWith(";"))
                    continue;

                if (trimmed.startsWith("[") && trimmed.contains("]")) {
                    String section = trimmed.substring(1, trimmed.indexOf(']')).trim();

                    if (section.equalsIgnoreCase("INIT")) {
                        currentShield = null;
                        continue;
                    }

                    if (section.regionMatches(true, 0, "ESCUDO", 0, 6)) {
                        String numPart = section.substring(6).trim();
                        try {
                            currentShieldId = Integer.parseInt(numPart);
                            currentShield = new ShieldData();
                            tempShields.put(currentShieldId, currentShield);
                            if (currentShieldId > maxShieldId)
                                maxShieldId = currentShieldId;
                        } catch (NumberFormatException e) {
                            currentShield = null;
                        }
                    } else {
                        currentShield = null;
                    }
                    continue;
                }

                int eq = trimmed.indexOf('=');
                if (eq <= 0)
                    continue;

                String key = trimmed.substring(0, eq).trim();
                String value = trimmed.substring(eq + 1).trim();
                if (value.contains("'")) {
                    value = value.substring(0, value.indexOf('\'')).trim();
                }

                if (key.equalsIgnoreCase("NumEscudos")) {
                    try {
                        numShieldsFromInit = Integer.parseInt(value);
                    } catch (NumberFormatException ignored) {
                    }
                    continue;
                }

                if (currentShield == null)
                    continue;

                try {
                    short grhIndex = Short.parseShort(value);
                    if (key.equalsIgnoreCase("Dir1")) {
                        initGrh(currentShield.getShieldWalk(1), grhIndex, false);
                    } else if (key.equalsIgnoreCase("Dir2")) {
                        initGrh(currentShield.getShieldWalk(2), grhIndex, false);
                    } else if (key.equalsIgnoreCase("Dir3")) {
                        initGrh(currentShield.getShieldWalk(4), grhIndex, false);
                    } else if (key.equalsIgnoreCase("Dir4")) {
                        initGrh(currentShield.getShieldWalk(3), grhIndex, false);
                    }
                } catch (NumberFormatException ignored) {
                }
            }

        } catch (IOException e) {
            Logger.error(e, "Error al leer Escudos.dat desde: {}", shieldsPath.toAbsolutePath());
        }

        int finalSize = Math.max(numShieldsFromInit, maxShieldId);
        AssetRegistry.shieldData = new ShieldData[finalSize + 1];
        AssetRegistry.shieldData[0] = new ShieldData();

        for (Map.Entry<Integer, ShieldData> entry : tempShields.entrySet()) {
            int id = entry.getKey();
            if (id <= finalSize) {
                AssetRegistry.shieldData[id] = entry.getValue();
            }
        }

        for (int i = 1; i <= finalSize; i++) {
            if (AssetRegistry.shieldData[i] == null) {
                AssetRegistry.shieldData[i] = new ShieldData();
            }
        }

        Logger.info("Cargados {} escudos desde {}", tempShields.size(), shieldsPath.toAbsolutePath());
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
        MapManager.loadMap(filePath);
    }

    /**
     * Guarda el mapa actual en la ruta especificada.
     * Genera los archivos .map, .inf y .dat.
     *
     * @param filePath Ruta absoluta al fichero .map de destino.
     */
    public static void saveMap(String filePath) {
        MapManager.saveMap(filePath);
    }

    static short nextOpenChar() {
        for (short i = 1; i < charList.length; i++) {
            if (!charList[i].isActive()) {
                return i;
            }
        }
        return -1;
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
        MapManager.initMap(data);
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
        mapData = new MapData[X_MAX_MAP_SIZE + 1][Y_MAX_MAP_SIZE + 1];

        for (int y = Y_MIN_MAP_SIZE; y <= Y_MAX_MAP_SIZE; y++) {
            for (int x = X_MIN_MAP_SIZE; x <= X_MAX_MAP_SIZE; x++) {
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
        if (AssetRegistry.grhData != null && grhIndex >= 0 && grhIndex < AssetRegistry.grhData.length
                && AssetRegistry.grhData[grhIndex] != null) {
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
     * Carga los efectos visuales (FXs) desde el archivo "fxs.ind".
     */
    private static void loadFxs() {
        byte[] data = loadLocalInitFile("Fxs.ind", "Fxs", false);
        if (data == null) {
            loadFxsFromIni();
            return;
        }

        boolean hasHeader = detectHeader(data, 6); // Anim(2) + OffX(2) + OffY(2) = 6 bytes

        try {
            reader.init(data);
            if (hasHeader)
                reader.skipBytes(263);

            final short numFXs = reader.readShort();
            AssetRegistry.fxData = new FxData[numFXs + 1];

            for (int i = 1; i <= numFXs; i++) {
                AssetRegistry.fxData[i] = new FxData();
                AssetRegistry.fxData[i].setAnimacion(reader.readShort());
                AssetRegistry.fxData[i].setOffsetX(reader.readShort());
                AssetRegistry.fxData[i].setOffsetY(reader.readShort());
            }
        } catch (Exception e) {
            Logger.error(e, "Error al cargar Fxs.ind");
            javax.swing.JOptionPane.showMessageDialog(null,
                    "Error al cargar 'Fxs.ind'.\n" +
                            "El archivo podría estar corrupto o ser de una versión no soportada.\n\n" +
                            "Detalle: " + e.getMessage(),
                    "Error de Formato en Fxs",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
            AssetRegistry.fxData = new FxData[1];
            AssetRegistry.fxData[0] = new FxData(); // Evitar NPE
        }
    }

    private static void loadFxsFromIni() {
        Path fxsPath = Path.of(options.getInitPath(), "Fxs.ini");
        if (!Files.exists(fxsPath)) {
            Logger.error("Fxs.ind y Fxs.ini no encontrados.");
            javax.swing.JOptionPane.showMessageDialog(null,
                    "No se encontró el archivo de Fxs (Fxs.ind o Fxs.ini).\n" +
                            "Por favor, configure las rutas correctamente.",
                    "Error al cargar Fxs",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
            AssetRegistry.fxData = new FxData[1];
            AssetRegistry.fxData[0] = new FxData();
            return;
        }

        Map<Integer, FxData> tempFxs = new HashMap<>();
        int maxFxId = 0;
        int numFxsFromInit = 0;

        try (BufferedReader br = Files.newBufferedReader(fxsPath, StandardCharsets.ISO_8859_1)) {
            String line;
            FxData currentFx = null;
            int currentFxId = -1;

            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("'") || trimmed.startsWith("#") || trimmed.startsWith(";"))
                    continue;

                if (trimmed.startsWith("[") && trimmed.contains("]")) {
                    String section = trimmed.substring(1, trimmed.indexOf(']')).trim();

                    if (section.equalsIgnoreCase("INIT")) {
                        currentFx = null;
                        continue;
                    }

                    if (section.regionMatches(true, 0, "FX", 0, 2)) {
                        String numPart = section.substring(2).trim();
                        try {
                            currentFxId = Integer.parseInt(numPart);
                            currentFx = new FxData();
                            tempFxs.put(currentFxId, currentFx);
                            if (currentFxId > maxFxId)
                                maxFxId = currentFxId;
                        } catch (NumberFormatException e) {
                            currentFx = null;
                        }
                    } else {
                        currentFx = null;
                    }
                    continue;
                }

                int eq = trimmed.indexOf('=');
                if (eq <= 0)
                    continue;

                String key = trimmed.substring(0, eq).trim();
                String value = trimmed.substring(eq + 1).trim();
                if (value.contains("'")) {
                    value = value.substring(0, value.indexOf('\'')).trim();
                }

                if (key.equalsIgnoreCase("NumFxs")) {
                    try {
                        numFxsFromInit = Integer.parseInt(value);
                    } catch (NumberFormatException ignored) {
                    }
                    continue;
                }

                if (currentFx == null)
                    continue;

                try {
                    short val = Short.parseShort(value);
                    if (key.equalsIgnoreCase("Animacion")) {
                        currentFx.setAnimacion(val);
                    } else if (key.equalsIgnoreCase("OffsetX")) {
                        currentFx.setOffsetX(val);
                    } else if (key.equalsIgnoreCase("OffsetY")) {
                        currentFx.setOffsetY(val);
                    }
                } catch (NumberFormatException ignored) {
                }
            }

        } catch (IOException e) {
            Logger.error(e, "Error al leer Fxs.ini");
            javax.swing.JOptionPane.showMessageDialog(null,
                    "Error al leer 'Fxs.ini'.\nDetalle: " + e.getMessage(),
                    "Error de Lectura", javax.swing.JOptionPane.ERROR_MESSAGE);
        }

        int finalSize = Math.max(numFxsFromInit, maxFxId);
        if (finalSize < 1)
            finalSize = 1;

        AssetRegistry.fxData = new FxData[finalSize + 1];

        for (Map.Entry<Integer, FxData> entry : tempFxs.entrySet()) {
            int id = entry.getKey();
            if (id <= finalSize) {
                AssetRegistry.fxData[id] = entry.getValue();
            }
        }

        for (int i = 1; i <= finalSize; i++) {
            if (AssetRegistry.fxData[i] == null) {
                AssetRegistry.fxData[i] = new FxData();
                AssetRegistry.fxData[i].setAnimacion((short) 0);
            }
        }

        Logger.info("Cargados {} FXs desde {}", tempFxs.size(), fxsPath.toAbsolutePath());
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
            grh.setStarted(AssetRegistry.grhData[grh.getGrhIndex()].getNumFrames() > 1);

        if (grh.isStarted())
            grh.setLoops(-1);

        grh.setFrameCounter(1);
        // grh.setSpeed( AssetRegistry.grhData[grhIndex].getSpeed() );
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
