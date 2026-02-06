package org.argentumforge.engine.utils;

import org.argentumforge.engine.game.Options;
import org.argentumforge.engine.utils.inits.NpcData;
import org.argentumforge.engine.utils.inits.ObjData;
import org.tinylog.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Clase encargada de la carga de recursos de datos (.dat).
 * Provee métodos para parsear archivos de configuración de NPCs y Objetos.
 */
public final class ResourceLoader {

    private ResourceLoader() {
        // Clase de utilidad
    }

    /**
     * Crea un BufferedReader detectando automáticamente la codificación del
     * archivo.
     * Soporta UTF-8 (con/sin BOM), UTF-16LE, UTF-16BE y ANSI (ISO-8859-1).
     *
     * @param path Ruta del archivo a leer.
     * @return BufferedReader configurado con la codificación correcta.
     * @throws IOException Si ocurre un error de lectura.
     */
    private static BufferedReader createReader(Path path) throws IOException {
        byte[] bom = new byte[4];
        try (java.io.InputStream is = Files.newInputStream(path)) {
            int read = is.read(bom, 0, 4);
            if (read < 2) {
                // Archivo muy pequeño, asumimos ANSI
                return Files.newBufferedReader(path, StandardCharsets.ISO_8859_1);
            }
        }

        // Detección de BOM
        if ((bom[0] & 0xFF) == 0xEF && (bom[1] & 0xFF) == 0xBB && (bom[2] & 0xFF) == 0xBF) {
            // UTF-8 BOM
            return Files.newBufferedReader(path, StandardCharsets.UTF_8);
        } else if ((bom[0] & 0xFF) == 0xFF && (bom[1] & 0xFF) == 0xFE) {
            // UTF-16 LE
            return Files.newBufferedReader(path, StandardCharsets.UTF_16LE);
        } else if ((bom[0] & 0xFF) == 0xFE && (bom[1] & 0xFF) == 0xFF) {
            // UTF-16 BE
            return Files.newBufferedReader(path, StandardCharsets.UTF_16BE);
        } else {
            // Sin BOM:
            // Intentamos detectar si es UTF-8 válido o ANSI.
            // Para simplificar y mantener compatibilidad con AO, si no tiene BOM asumimos
            // ISO-8859-1 (ANSI) que es el estándar de Windows heredado.
            // Solo si fallara mucho podríamos intentar decodificar como UTF-8, pero ANSI es
            // lo seguro.
            return Files.newBufferedReader(path, StandardCharsets.ISO_8859_1);
        }
    }

    /**
     * Carga las definiciones de NPCs desde el archivo NPCs.dat.
     * 
     * @return Un mapa con las definiciones de NPCs indexadas por su número.
     */
    public static Map<Integer, NpcData> loadNpcs() {
        final Path npcsPath = Path.of(Options.INSTANCE.getDatsPath(), "NPCs.dat");
        final Map<Integer, NpcData> result = new HashMap<>();

        if (!Files.exists(npcsPath)) {
            Logger.error("No se encontró NPCs.dat en la ruta: {}", npcsPath.toAbsolutePath());
            return result;
        }

        NpcData currentNpc = null;
        try (BufferedReader br = createReader(npcsPath)) {
            String line;
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                // Limpiar posibles caracteres nulos o basura del BOM si quedan
                if (trimmed.length() > 0 && trimmed.charAt(0) == 65279)
                    trimmed = trimmed.substring(1);

                if (trimmed.isEmpty() || trimmed.startsWith("'") || trimmed.startsWith("#") || trimmed.startsWith(";"))
                    continue;

                if (trimmed.startsWith("[") && trimmed.contains("]")) {
                    String section = trimmed.substring(1, trimmed.indexOf(']')).trim();
                    if (section.regionMatches(true, 0, "NPC", 0, 3)) {
                        try {
                            int npcNumber = Integer.parseInt(section.substring(3).trim());
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
            Logger.error(e, "No se pudo leer NPCs.dat desde la ruta: {}", npcsPath.toAbsolutePath());
        }

        Logger.info("Cargadas {} definiciones de NPCs", result.size());
        return result;
    }

    /**
     * Carga las definiciones de Objetos desde el archivo OBJ.dat.
     * 
     * @return Un mapa con las definiciones de Objetos indexadas por su número.
     */
    public static Map<Integer, ObjData> loadObjs() {
        final Path objsPath = Path.of(Options.INSTANCE.getDatsPath(), "OBJ.dat");
        final Map<Integer, ObjData> result = new HashMap<>();

        if (!Files.exists(objsPath)) {
            Logger.error("No se encontró OBJ.dat en la ruta: {}", objsPath.toAbsolutePath());
            return result;
        }

        ObjData currentObj = null;
        try (BufferedReader br = createReader(objsPath)) {
            String line;
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                // Limpiar BOM residual si existe
                if (trimmed.length() > 0 && trimmed.charAt(0) == 65279)
                    trimmed = trimmed.substring(1);

                if (trimmed.isEmpty() || trimmed.startsWith("'") || trimmed.startsWith("#") || trimmed.startsWith(";"))
                    continue;

                if (trimmed.startsWith("[") && trimmed.contains("]")) {
                    String section = trimmed.substring(1, trimmed.indexOf(']')).trim();
                    if (section.regionMatches(true, 0, "OBJ", 0, 3)) {
                        try {
                            int objNumber = Integer.parseInt(section.substring(3).trim());
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
                } else if (key.equalsIgnoreCase("ObjType") || key.equalsIgnoreCase("Type")) {
                    try {
                        currentObj.setType(Integer.parseInt(value));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        } catch (IOException e) {
            Logger.error(e, "No se pudo leer OBJ.dat desde la ruta: {}", objsPath.toAbsolutePath());
        }

        Logger.info("Cargadas {} definiciones de Objetos", result.size());
        return result;
    }

}
