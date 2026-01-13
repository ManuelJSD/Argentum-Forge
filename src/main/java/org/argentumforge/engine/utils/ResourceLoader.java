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
 */
public final class ResourceLoader {
    private ResourceLoader() {
        // Clase de utilidad
    }

    /**
     * Carga las definiciones de NPCs desde el archivo NPCs.dat.
     */
    public static Map<Integer, NpcData> loadNpcs() {
        final Path npcsPath = Path.of(Options.INSTANCE.getDatsPath(), "NPCs.dat");
        final Map<Integer, NpcData> result = new HashMap<>();
        if (!Files.exists(npcsPath)) {
            Logger.error("NPCs.dat not found at path: {}", npcsPath.toAbsolutePath());
            return result;
        }
        NpcData currentNpc = null;
        try (BufferedReader br = Files.newBufferedReader(npcsPath, StandardCharsets.ISO_8859_1)) {
            String line;
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
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
                if (currentNpc == null) continue;
                int eq = trimmed.indexOf('=');
                if (eq <= 0) continue;
                String key = trimmed.substring(0, eq).trim();
                String value = trimmed.substring(eq + 1).trim();
                if (key.equalsIgnoreCase("Name")) {
                    currentNpc.setName(value);
                } else if (key.equalsIgnoreCase("Head")) {
                    try { currentNpc.setHead(Integer.parseInt(value)); } catch (NumberFormatException ignored) {}
                } else if (key.equalsIgnoreCase("Body")) {
                    try { currentNpc.setBody(Integer.parseInt(value)); } catch (NumberFormatException ignored) {}
                }
            }
        } catch (IOException e) {
            Logger.error(e, "Could not read NPCs.dat from path: {}", npcsPath.toAbsolutePath());
        }
        Logger.info("Loaded {} NPC definitions", result.size());
        return result;
    }

    /**
     * Carga las definiciones de Objetos desde el archivo OBJ.dat.
     */
    public static Map<Integer, ObjData> loadObjs() {
        final Path objsPath = Path.of(Options.INSTANCE.getDatsPath(), "OBJ.dat");
        final Map<Integer, ObjData> result = new HashMap<>();
        if (!Files.exists(objsPath)) {
            Logger.error("OBJ.dat not found at path: {}", objsPath.toAbsolutePath());
            return result;
        }
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
                if (currentObj == null) continue;
                int eq = trimmed.indexOf('=');
                if (eq <= 0) continue;
                String key = trimmed.substring(0, eq).trim();
                String value = trimmed.substring(eq + 1).trim();
                if (key.equalsIgnoreCase("Name")) {
                    currentObj.setName(value);
                } else if (key.equalsIgnoreCase("GrhIndex")) {
                    try { currentObj.setGrhIndex(Integer.parseInt(value)); } catch (NumberFormatException ignored) {}
                }
            }
        } catch (IOException e) {
            Logger.error(e, "Could not read OBJ.dat from path: {}", objsPath.toAbsolutePath());
        }
        Logger.info("Loaded {} OBJ definitions", result.size());
        return result;
    }

}