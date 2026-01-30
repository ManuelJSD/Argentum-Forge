package org.argentumforge.engine.utils;

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
        // Use File constructor to ensure correct separator for the OS
        String path = new java.io.File("maps", "map1.map").getAbsolutePath();
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
