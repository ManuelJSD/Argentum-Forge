package org.argentumforge.engine.utils;

import org.argentumforge.engine.utils.MapManager.MapSaveOptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MapManagerTest {

    @Test
    @DisplayName("Should create standard save options")
    void shouldCreateStandardSaveOptions() {
        // Act
        MapSaveOptions options = MapSaveOptions.standard();

        // Assert
        assertThat(options).isNotNull();
        assertThat(options.getVersion()).isEqualTo((short) 1);
        assertThat(options.isUseLongIndices()).isFalse();
        assertThat(options.isIncludeHeader()).isTrue();
    }

    @Test
    @DisplayName("Should create AO Libre save options")
    void shouldCreateAOLibreSaveOptions() {
        // Act
        MapSaveOptions options = MapSaveOptions.aoLibre();

        // Assert
        assertThat(options).isNotNull();
        assertThat(options.isUseLongIndices()).isTrue();
        assertThat(options.getVersion()).isEqualTo((short) 136); // AO Libre version
    }

    @Test
    @DisplayName("Should create extended save options")
    void shouldCreateExtendedSaveOptions() {
        // Act
        MapSaveOptions options = MapSaveOptions.extended();

        // Assert
        assertThat(options).isNotNull();
        assertThat(options.isUseLongIndices()).isTrue();
        assertThat(options.getVersion()).isEqualTo((short) 1); // Same as standard but with long indices
        assertThat(options.isIncludeHeader()).isTrue();
    }

    @Test
    @DisplayName("Should allow modifying save options")
    void shouldAllowModifyingSaveOptions() {
        // Arrange
        MapSaveOptions options = MapSaveOptions.standard();

        // Act
        options.setVersion((short) 3);
        options.setUseLongIndices(true);
        options.setIncludeHeader(false);

        // Assert
        assertThat(options.getVersion()).isEqualTo((short) 3);
        assertThat(options.isUseLongIndices()).isTrue();
        assertThat(options.isIncludeHeader()).isFalse();
    }

    @Test
    @DisplayName("Should detect save options from valid data")
    void shouldDetectSaveOptionsFromData() {
        // Arrange - Create a minimal valid map data with version header
        byte[] dataVersion1 = new byte[] {
                0x01, 0x00, // version = 1 (little endian)
                0x00, 0x00, 0x00, 0x00 // placeholder data
        };

        byte[] dataVersion136 = new byte[] {
                (byte) 0x88, 0x00, // version = 136 (little endian)
                0x00, 0x00, 0x00, 0x00
        };

        // Act
        MapSaveOptions options1 = MapManager.detectSaveOptions(dataVersion1);
        MapSaveOptions options136 = MapManager.detectSaveOptions(dataVersion136);

        // Assert
        assertThat(options1).isNotNull();
        assertThat(options1.getVersion()).isEqualTo((short) 1);
        assertThat(options1.isUseLongIndices()).isFalse();

        assertThat(options136).isNotNull();
        assertThat(options136.getVersion()).isEqualTo((short) 136);
        assertThat(options136.isUseLongIndices()).isTrue(); // AO Libre uses long indices
    }

    @Test
    @DisplayName("Should handle empty data in detectSaveOptions")
    void shouldHandleEmptyDataInDetect() {
        // Act - Empty array defaults
        MapSaveOptions emptyOptions = MapManager.detectSaveOptions(new byte[0]);

        // Assert - should return default options
        assertThat(emptyOptions).isNotNull();
        assertThat(emptyOptions.getVersion()).isEqualTo((short) 1); // Default version
    }

    @Test
    @DisplayName("Should throw NullPointerException for null data")
    void shouldThrowForNullData() {
        // Assert - detectSaveOptions doesn't handle null
        assertThatThrownBy(() -> MapManager.detectSaveOptions(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Save options should be independent instances")
    void saveOptionsShouldBeIndependent() {
        // Arrange
        MapSaveOptions options1 = MapSaveOptions.standard();
        MapSaveOptions options2 = MapSaveOptions.standard();

        // Act
        options1.setVersion((short) 99);

        // Assert - should not affect options2
        assertThat(options1.getVersion()).isEqualTo((short) 99);
        assertThat(options2.getVersion()).isEqualTo((short) 1);
    }

    @Test
    @DisplayName("Different save option types should have different configurations")
    void differentSaveOptionsShouldDiffer() {
        // Arrange
        MapSaveOptions standard = MapSaveOptions.standard();
        MapSaveOptions aoLibre = MapSaveOptions.aoLibre();
        MapSaveOptions extended = MapSaveOptions.extended();

        // Assert - standard vs aoLibre (different versions and indices)
        assertThat(standard.getVersion()).isNotEqualTo(aoLibre.getVersion());
        assertThat(standard.isUseLongIndices()).isNotEqualTo(aoLibre.isUseLongIndices());

        // Assert - standard vs extended (same version, different indices)
        assertThat(standard.getVersion()).isEqualTo(extended.getVersion());
        assertThat(standard.isUseLongIndices()).isNotEqualTo(extended.isUseLongIndices());
    }

    @Test
    @DisplayName("checkUnsavedChanges should be deprecated and return true")
    void checkUnsavedChangesShouldBeDeprecated() {
        // Act
        @SuppressWarnings("deprecation")
        boolean result = MapManager.checkUnsavedChanges();

        // Assert - deprecated method returns true (despite javadoc saying false)
        // This forces "discard" behavior to avoid blocking
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should handle modification tracking with no active context")
    void shouldHandleNoActiveContext() {
        // Note: These methods depend on GameData.getActiveContext()
        // which is likely null during unit tests
        // They should not throw exceptions

        // Act & Assert - should not throw
        MapManager.markAsModified(); // No-op if no context
        MapManager.markAsSaved(); // No-op if no context
        boolean hasChanges = MapManager.hasUnsavedChanges(); // Returns false if no context

        assertThat(hasChanges).isFalse();
    }
}
