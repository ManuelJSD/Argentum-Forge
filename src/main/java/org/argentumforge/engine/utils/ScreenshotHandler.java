package org.argentumforge.engine.utils;

import java.io.File;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.stb.STBImageWrite;
import org.tinylog.Logger;

public class ScreenshotHandler {

    public static void takeScreenshot() {
        int width = org.argentumforge.engine.Engine.INSTANCE.getWindow().getWidth();
        int height = org.argentumforge.engine.Engine.INSTANCE.getWindow().getHeight();

        // Create the buffer to hold the pixel data
        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4); // 4 bytes per pixel (RGBA)

        // Read the pixels from the frame buffer
        GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

        // Creates a file name based on the current date and time
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "screenshot_" + timestamp + ".png";
        File dir = new File("screenshots");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String filePath = dir.getAbsolutePath() + File.separator + fileName;

        new Thread(() -> {
            // STBImageWrite expects the image data to be flipped vertically because
            // OpenGL's origin is at the bottom-left
            // but standard images have the origin at the top-left.
            // We can do manual flip or use stbi_flip_vertically_on_write (global setting,
            // might affect others).
            // Safer to just write it. If it's upside down, we can flip the buffer.
            // Let's manually flip the buffer for correctness.

            ByteBuffer flippedBuffer = BufferUtils.createByteBuffer(width * height * 4);
            int rowSize = width * 4;
            for (int y = 0; y < height; y++) {
                int sourceIndex = (height - 1 - y) * rowSize;
                int destIndex = y * rowSize;
                // Copy row
                buffer.position(sourceIndex);
                buffer.limit(sourceIndex + rowSize);
                flippedBuffer.position(destIndex);
                flippedBuffer.put(buffer);
            }
            flippedBuffer.flip();
            buffer.clear(); // Reset original buffer just in case

            if (STBImageWrite.stbi_write_png(filePath, width, height, 4, flippedBuffer, width * 4)) {
                Logger.info("Screenshot saved to: " + filePath);
                // We could notify via Console if we had access to instance, but this is static
                // util.
            } else {
                Logger.error("Failed to save screenshot.");
            }
        }).start();
    }
}
