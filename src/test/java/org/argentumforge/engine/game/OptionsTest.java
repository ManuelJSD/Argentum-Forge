package org.argentumforge.engine.game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OptionsTest {

    private Options options;

    @BeforeEach
    void setUp() {
        options = Options.INSTANCE;
    }

    @Test
    @DisplayName("Should manage recent maps list correctly")
    void shouldManageRecentMaps() {
        // Arrange
        options.getRecentMaps().clear();
        String map1 = "map1.map";
        String map2 = "map2.map";

        // Act
        options.setLastMapPath(map1);
        options.setLastMapPath(map2);

        // Assert
        assertThat(options.getRecentMaps()).containsExactly(map2, map1);
        assertThat(options.getRecentMaps()).hasSize(2);
    }

    @Test
    @DisplayName("Should avoid duplicates in recent maps and move them to top")
    void shouldAvoidDuplicatesInRecentMaps() {
        // Arrange
        options.getRecentMaps().clear();
        options.setLastMapPath("map1.map");
        options.setLastMapPath("map2.map");

        // Act
        options.setLastMapPath("map1.map");

        // Assert
        assertThat(options.getRecentMaps()).containsExactly("map1.map", "map2.map");
        assertThat(options.getRecentMaps()).hasSize(2);
    }

    @Test
    @DisplayName("Should update basic settings correctly")
    void shouldUpdateSettings() {
        // Act
        options.setMusic(false);
        options.setLanguage("en_US");
        options.setScreenWidth(1920);

        // Assert
        assertThat(options.isMusic()).isFalse();
        assertThat(options.getLanguage()).isEqualTo("en_US");
        assertThat(options.getScreenWidth()).isEqualTo(1920);
    }

    @Test
    @DisplayName("Should manage ignored object types")
    void shouldManageIgnoredObjTypes() {
        // Act
        options.getIgnoredObjTypes().add(999);

        // Assert
        assertThat(options.getIgnoredObjTypes()).contains(999);

        options.getIgnoredObjTypes().remove(999);
        assertThat(options.getIgnoredObjTypes()).doesNotContain(999);
    }
}
