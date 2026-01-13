package org.argentumforge.engine.game;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Gestiona mensajes de texto en distintos idiomas segun la region.
 */

public class Messages {

    private static final String MESSAGE_PREFIX = "MENSAJE_"; // consola
    private static final Map<MessageKey, String> messageCache = new HashMap<>();

    private Messages() {
    }

    /**
     * Carga los mensajes del idioma actual desde el archivo de recursos.
     */
    public static void loadMessages(String region) {

        messageCache.clear();

        String filename = "strings_" + region + ".ini";

        try {
            Path resources = Paths.get("resources", filename);
            if (!Files.exists(resources)) {
                System.err.println("¡No se pudo encontrar el archivo " + filename + "!");
                return;
            }

            // Leer el archivo con UTF-8
            try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(resources),
                    StandardCharsets.UTF_8)) {
                Properties properties = new Properties();
                properties.load(reader);

                for (MessageKey key : MessageKey.values()) {
                    String value = properties.getProperty(MESSAGE_PREFIX + key.name());
                    if (value != null && !value.isEmpty()) {
                        messageCache.put(key, value);
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("Error al cargar los mensajes: " + e.getMessage());
        }
    }

    public static String get(MessageKey key) {
        return messageCache.get(key);
    }

    /**
     * Mensajes.
     */
    public enum MessageKey {
        ENTRAR_PARTY,
        NO_VES_NADA_INTERESANTE,
        HOGAR,
        HOGAR_CANCEL,
        IR_HOGAR,
        TRY_CONNECT,
        ENTER_USER_PASS,
        ACCEPT,
        CLOSE,
        PASS_NOT_MATCH,
        COMPLETE_ALL_FIELDS,
        INVALID_NICK,
        INVALID_PASS,
        INVALID_EMAIL
    }

}