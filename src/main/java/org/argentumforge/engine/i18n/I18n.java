package org.argentumforge.engine.i18n;

import org.tinylog.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Sistema de internacionalización (i18n) para Argentum Forge.
 * Gestiona la carga y acceso a traducciones desde archivos .properties.
 */
public class I18n {

    public static final I18n INSTANCE = new I18n();

    private static final String LANG_DIR = "lang";
    private static final String DEFAULT_LOCALE = "es_ES";

    private Properties translations;
    private String currentLocale;

    private I18n() {
        translations = new Properties();
        currentLocale = DEFAULT_LOCALE;
    }

    /**
     * Carga un archivo de idioma.
     * 
     * @param locale Código de idioma (ej: "es_ES", "en_US")
     */
    public void loadLanguage(String locale) {
        translations.clear();
        boolean loadedAny = false;

        // 1. Base: Cargar desde Classpath (dentro del JAR)
        String resourcePath = "/" + LANG_DIR + "/" + locale + ".properties";
        if (loadFromResource(resourcePath)) {
            loadedAny = true;
        }

        // 2. Dev: Cargar desde resources/lang (Entorno de desarrollo IDE)
        Path devPath = Paths.get("resources", LANG_DIR, locale + ".properties");
        if (Files.exists(devPath)) {
            if (loadFromFile(devPath)) {
                loadedAny = true;
            }
        }

        // 3. User/Runtime: Cargar desde lang/ (Carpeta junto al ejecutable o raíz del
        // proyecto)
        Path localPath = Paths.get(LANG_DIR, locale + ".properties");
        if (Files.exists(localPath)) {
            // Evitar cargar dos veces si apunta al mismo archivo físico que devPath
            boolean isSame = false;
            try {
                if (Files.exists(devPath)
                        && localPath.toAbsolutePath().normalize().equals(devPath.toAbsolutePath().normalize())) {
                    isSame = true;
                }
            } catch (Exception e) {
                /* ignore */ }

            if (!isSame) {
                if (loadFromFile(localPath)) {
                    loadedAny = true;
                }
            }
        }

        if (loadedAny) {
            currentLocale = locale;
            Logger.info("Language loaded successfully: {}", locale);
        } else {
            // Fallback
            if (!locale.equals(DEFAULT_LOCALE)) {
                Logger.warn("Language {} not found in any source. Falling back to default: {}", locale, DEFAULT_LOCALE);
                loadLanguage(DEFAULT_LOCALE);
            } else {
                Logger.error("CRITICAL: Default language {} could not be found!", DEFAULT_LOCALE);
            }
        }
    }

    private boolean loadFromFile(Path path) {
        try (InputStream is = new FileInputStream(path.toFile());
                InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            translations.load(reader);
            Logger.debug("Merged language from file: {}", path.toAbsolutePath());
            return true;
        } catch (IOException e) {
            Logger.error(e, "Failed to load language file: {}", path);
            return false;
        }
    }

    private boolean loadFromResource(String path) {
        try (InputStream is = I18n.class.getResourceAsStream(path)) {
            if (is != null) {
                try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    translations.load(reader);
                    Logger.debug("Merged language from classpath: {}", path);
                    return true;
                }
            }
        } catch (IOException e) {
            Logger.error(e, "Failed to load language resource: {}", path);
        }
        return false;
    }

    /**
     * Obtiene una traducción por su clave.
     * 
     * @param key Clave de traducción
     * @return Texto traducido, o la clave si no existe
     */
    public String get(String key) {
        return translations.getProperty(key, key);
    }

    /**
     * Obtiene una traducción con formato.
     * 
     * @param key  Clave de traducción
     * @param args Argumentos para formatear
     * @return Texto traducido y formateado
     */
    public String get(String key, Object... args) {
        String template = get(key);
        try {
            return String.format(template, args);
        } catch (IllegalFormatException e) {
            Logger.warn("Invalid format for key: {}", key);
            return template;
        }
    }

    /**
     * Obtiene el locale actual.
     */
    public String getCurrentLocale() {
        return currentLocale;
    }

    /**
     * Escanea el directorio de idiomas y retorna los locales disponibles.
     * 
     * @return Lista de códigos de idioma disponibles
     */
    public List<String> getAvailableLanguages() {
        Set<String> languages = new HashSet<>();

        // 1. Agregar idiomas por defecto (que sabemos que están en el JAR)
        languages.add("es_ES");
        languages.add("en_US");
        languages.add("pt_BR");

        // 2. Escanear carpeta local para encontrar nuevos o custom
        Path langDir = Paths.get(LANG_DIR);

        if (Files.exists(langDir)) {
            try {
                Files.list(langDir)
                        .filter(p -> p.toString().endsWith(".properties"))
                        .map(p -> p.getFileName().toString().replace(".properties", ""))
                        .forEach(languages::add);
            } catch (IOException e) {
                Logger.error(e, "Failed to scan language directory");
            }
        } else {
            try {
                Files.createDirectories(langDir);
            } catch (IOException e) {
                Logger.error(e, "Failed to create lang directory");
            }
        }

        return languages.stream().sorted().collect(Collectors.toList());
    }

    /**
     * Obtiene el nombre legible de un locale.
     * 
     * @param locale Código de idioma
     * @return Nombre del idioma
     */
    public String getLanguageName(String locale) {
        switch (locale) {
            case "es_ES":
                return "Español";
            case "en_US":
                return "English";
            case "pt_BR":
                return "Português (Brasil)";
            case "fr_FR":
                return "Français";
            case "de_DE":
                return "Deutsch";
            default:
                return locale;
        }
    }
}
