package org.argentumforge.engine.utils;

import org.argentumforge.engine.game.models.Character;
import org.argentumforge.engine.utils.inits.MapData;
import org.argentumforge.engine.utils.inits.MapProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MapContextTest {

    @Test
    @DisplayName("Should return 'Sin Título' when filePath is null")
    void shouldReturnDefaultNameWhenFilePathIsNull() {
        // Arrange
        MapContext context = new MapContext(null, null, null, null);

        // Act
        String name = context.getMapName();

        // Assert
        assertThat(name).isEqualTo("Sin Título");
    }

    @Test
    @DisplayName("Should return file name from path")
    void shouldReturnFileNameFromPath() {
        // Arrange
        String path = "C:\\maps\\map1.map";
        MapContext context = new MapContext(path, null, null, null);

        // Act
        String name = context.getMapName();

        // Assert
        assertThat(name).isEqualTo("map1.map");
    }

    @Test
    @DisplayName("Should keep modified state correctly")
    void shouldKeepModifiedState() {
        // Arrange
        MapContext context = new MapContext("", null, null, null);

        // Act & Assert
        assertThat(context.isModified()).isFalse();

        context.setModified(true);
        assertThat(context.isModified()).isTrue();
    }
}
