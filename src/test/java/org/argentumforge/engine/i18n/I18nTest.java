package org.argentumforge.engine.i18n;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class I18nTest {

    private I18n i18n;

    @BeforeEach
    void setUp() {
        i18n = I18n.INSTANCE;
        i18n.loadLanguage("es_ES");
    }

    @Test
    @DisplayName("Should return the translation for a given key")
    void shouldReturnTranslation() {
        // es_ES.properties contains menu.file=Archivo
        String translation = i18n.get("menu.file");
        assertThat(translation).isEqualTo("Archivo");
    }

    @Test
    @DisplayName("Should return the key itself if translation is missing")
    void shouldReturnKeyIfMissing() {
        String key = "non.existent.key";
        String translation = i18n.get(key);
        assertThat(translation).isEqualTo(key);
    }

    @Test
    @DisplayName("Should support formatted strings")
    void shouldSupportFormatting() {
        // es_ES.properties contains editor.transfer.destInfo=Destino: Mapa %d (%d, %d)
        String translation = i18n.get("editor.transfer.destInfo", 1, 50, 50);
        assertThat(translation).isEqualTo("Destino: Mapa 1 (50, 50)");
    }

    @Test
    @DisplayName("Should return raw template if formatting fails")
    void shouldReturnRawTemplateOnFormatError() {
        // Try to format a string requiring integers with a string
        String translation = i18n.get("editor.transfer.destInfo", "invalid");
        assertThat(translation).contains("%d");
    }

    @Test
    @DisplayName("Should return correct language names")
    void shouldReturnLanguageNames() {
        assertThat(i18n.getLanguageName("es_ES")).isEqualTo("Español");
        assertThat(i18n.getLanguageName("en_US")).isEqualTo("English");
        assertThat(i18n.getLanguageName("pt_BR")).isEqualTo("Português (Brasil)");
        assertThat(i18n.getLanguageName("unknown")).isEqualTo("unknown");
    }
}
