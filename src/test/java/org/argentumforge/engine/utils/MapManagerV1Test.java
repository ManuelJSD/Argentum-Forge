package org.argentumforge.engine.utils;

import org.argentumforge.engine.utils.MapManager.MapFormatType;
import org.argentumforge.engine.utils.MapManager.MapSaveOptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MapManagerV1Test {

    @Test
    @DisplayName("Should create V1 Legacy save options")
    void shouldCreateV1LegacySaveOptions() {
        // Act
        MapSaveOptions options = MapSaveOptions.v1Legacy();

        // Assert
        assertThat(options).isNotNull();
        assertThat(options.getFormatType()).isEqualTo(MapFormatType.V1_LEGACY);
        assertThat(options.getVersion()).isEqualTo((short) 1);
        assertThat(options.isUseLongIndices()).isFalse();
        assertThat(options.isIncludeHeader()).isTrue();
    }

    @Test
    @DisplayName("Should detect V1 Legacy format based on file size")
    void shouldDetectV1LegacyFromSize() {
        // Arrange - Create a byte array of exactly 130,273 bytes
        // 273 (Header) + 10000 * 13 (Tile Data) = 130,273
        byte[] v1Data = new byte[130273];

        // Act
        MapSaveOptions options = MapManager.detectSaveOptions(v1Data);

        // Assert
        assertThat(options).isNotNull();
        assertThat(options.getFormatType()).isEqualTo(MapFormatType.V1_LEGACY);
        assertThat(options.getVersion()).isEqualTo((short) 1);
    }

    @Test
    @DisplayName("Should default to V2 Standard for other sizes")
    void shouldDefaultToV2Standard() {
        // Arrange - Random size
        byte[] otherData = new byte[100];

        // Act
        MapSaveOptions options = MapManager.detectSaveOptions(otherData);

        // Assert
        assertThat(options).isNotNull();
        assertThat(options.getFormatType()).isEqualTo(MapFormatType.V2_STANDARD);
    }
}
