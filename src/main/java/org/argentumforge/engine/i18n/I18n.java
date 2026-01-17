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
        Path langFile = Paths.get(LANG_DIR, locale + ".properties");

        if (!Files.exists(langFile)) {
            Logger.warn("Language file not found: {}. Using default: {}", locale, DEFAULT_LOCALE);
            locale = DEFAULT_LOCALE;
            langFile = Paths.get(LANG_DIR, locale + ".properties");
        }

        if (!Files.exists(langFile)) {
            Logger.error("Default language file not found either: {}", langFile);
            return;
        }

        try (InputStream is = new FileInputStream(langFile.toFile());
                InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            translations.clear();
            translations.load(reader);
            currentLocale = locale;
            Logger.info("Loaded language: {}", locale);
        } catch (IOException e) {
            Logger.error(e, "Failed to load language file: {}", langFile);
        }
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
        Path langDir = Paths.get(LANG_DIR);

        if (!Files.exists(langDir)) {
            try {
                Files.createDirectories(langDir);
            } catch (IOException e) {
                Logger.error(e, "Failed to create lang directory");
                return Collections.singletonList(DEFAULT_LOCALE);
            }
        }

        try {
            return Files.list(langDir)
                    .filter(p -> p.toString().endsWith(".properties"))
                    .map(p -> p.getFileName().toString().replace(".properties", ""))
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            Logger.error(e, "Failed to scan language directory");
            return Collections.singletonList(DEFAULT_LOCALE);
        }
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
