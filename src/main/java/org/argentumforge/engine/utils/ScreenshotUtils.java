package org.argentumforge.engine.utils;

import org.argentumforge.engine.game.console.Console;
import org.argentumforge.engine.renderer.RGBColor;
import org.lwjgl.BufferUtils;
import java.io.File;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.argentumforge.engine.game.console.FontStyle.REGULAR;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.stb.STBImageWrite.stbi_write_png;

/**
 * Utilidad profesional para capturar la pantalla y guardarla como imagen PNG.
 */
public class ScreenshotUtils {

    /**
     * Captura el estado actual del framebuffer y lo guarda en la carpeta
     * 'screenshots'.
     * Muestra una notificación en la consola del motor al finalizar.
     */
    public static void takeScreenshot() {
        // Obtenemos dimensiones de la ventana actual
        int width = org.argentumforge.engine.Engine.INSTANCE.getWindow().getWidth();
        int height = org.argentumforge.engine.Engine.INSTANCE.getWindow().getHeight();

        // Creamos un buffer para almacenar los píxeles (RGBA = 4 bytes por píxel)
        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);

        // Leemos los píxeles directamente del framebuffer
        // IMPORTANTE: Se lee lo que el usuario está viendo actualmente (incluyendo
        // shaders)
        glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, buffer);

        // Aseguramos que el directorio de capturas exista
        File dir = new File("screenshots");
        if (!dir.exists()) {
            dir.mkdir();
        }

        // Generamos un nombre de archivo único basado en la fecha y hora
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String filename = "screenshots/ArgentumForge_" + timestamp + ".png";

        // OpenGL lee de abajo hacia arriba, rotamos el buffer para que la imagen sea
        // correcta
        flipBuffer(buffer, width, height);

        // Guardamos usando la librería STB (Súper rápida y ya integrada en el motor)
        if (stbi_write_png(filename, width, height, 4, buffer, width * 4)) {
            Console.INSTANCE.addMsgToConsole("¡Foto guardada con éxito!: " + filename, REGULAR,
                    new RGBColor(0f, 1f, 1f));
        } else {
            Console.INSTANCE.addMsgToConsole("Error crítico: No se pudo guardar la foto.", REGULAR,
                    new RGBColor(1f, 0f, 0f));
        }
    }

    /**
     * Invierte el buffer de píxeles verticalmente.
     */
    private static void flipBuffer(ByteBuffer buffer, int width, int height) {
        int stride = width * 4;
        byte[] row = new byte[stride];
        byte[] bottomRow = new byte[stride];

        for (int y = 0; y < height / 2; y++) {
            int topOffset = y * stride;
            int bottomOffset = (height - y - 1) * stride;

            buffer.position(topOffset);
            buffer.get(row);

            buffer.position(bottomOffset);
            buffer.get(bottomRow);

            buffer.position(topOffset);
            buffer.put(bottomRow);

            buffer.position(bottomOffset);
            buffer.put(row);
        }
        buffer.clear();
    }
}
