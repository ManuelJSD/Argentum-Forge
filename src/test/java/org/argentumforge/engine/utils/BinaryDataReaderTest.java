package org.argentumforge.engine.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.assertj.core.api.Assertions.assertThat;

class BinaryDataReaderTest {

    private BinaryDataReader reader;

    @BeforeEach
    void setUp() {
        reader = new BinaryDataReader();
    }

    @Test
    @DisplayName("Should read primitives correctly in Little Endian")
    void shouldReadPrimitivesLittleEndian() {
        // Arrange
        ByteBuffer bb = ByteBuffer.allocate(15);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(123456); // 4 bytes
        bb.putShort((short) 567); // 2 bytes
        bb.put((byte) 42); // 1 byte
        bb.putLong(9876543210L); // 8 bytes

        reader.init(bb.array());

        // Act & Assert
        assertThat(reader.readInt()).isEqualTo(123456);
        assertThat(reader.readShort()).isEqualTo((short) 567);
        assertThat(reader.readByte()).isEqualTo((byte) 42);
        assertThat(reader.readLong()).isEqualTo(9876543210L);
    }

    @Test
    @DisplayName("Should read primitives correctly in Big Endian")
    void shouldReadPrimitivesBigEndian() {
        // Arrange
        ByteBuffer bb = ByteBuffer.allocate(10);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.putInt(654321);
        bb.putShort((short) 765);

        reader.init(bb.array(), ByteOrder.BIG_ENDIAN);

        // Act & Assert
        assertThat(reader.readInt()).isEqualTo(654321);
        assertThat(reader.readShort()).isEqualTo((short) 765);
    }

    @Test
    @DisplayName("Should read boolean correctly")
    void shouldReadBoolean() {
        // Arrange
        byte[] data = { 1, 0, 5 };
        reader.init(data);

        // Act & Assert
        assertThat(reader.readBoolean()).isTrue();
        assertThat(reader.readBoolean()).isFalse();
        assertThat(reader.readBoolean()).isTrue(); // non-zero is true
    }

    @Test
    @DisplayName("Should read string of fixed length")
    void shouldReadString() {
        // Arrange
        String text = "Hello World";
        reader.init(text.getBytes());

        // Act & Assert
        assertThat(reader.readString(5)).isEqualTo("Hello");
        assertThat(reader.readString(6)).isEqualTo(" World");
    }

    @Test
    @DisplayName("Should handle skip and hasRemaining")
    void shouldHandleSkipAndRemaining() {
        // Arrange
        byte[] data = new byte[10];
        reader.init(data);

        // Act & Assert
        assertThat(reader.hasRemaining()).isTrue();
        reader.skipBytes(5);
        assertThat(reader.hasRemaining()).isTrue();
        reader.skipBytes(5);
        assertThat(reader.hasRemaining()).isFalse();
    }
}
