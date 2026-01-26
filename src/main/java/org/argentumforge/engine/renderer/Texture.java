package org.argentumforge.engine.renderer;

import org.lwjgl.BufferUtils;
import org.tinylog.Logger;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;

import org.argentumforge.engine.game.Options;

/**
 * La clase {@code Texture} representa una textura en OpenGL para el renderizado
 * grafico.
 * <p>
 * OpenGL guarda las texturas creadas con un identificador numerico (ID), que
 * esta clase almacena junto con informacion sobre el
 * tamaño de la textura. Proporciona funcionalidad para cargar texturas desde
 * archivos comprimidos, y metodos para vincular y
 * desvincular texturas durante el proceso de renderizado.
 * <p>
 * Esta clase es fundamental en el sistema de renderizado, ya que permite que
 * las texturas sean cargadas en memoria grafica y se
 * puedan dibujar en pantalla de manera eficiente a traves de OpenGL.
 */

public class Texture {

    private int id;
    private int tex_width;
    private int tex_height;

    public Texture() {

    }

    public void loadTexture(Texture refTexture, String compressedFile, String file, boolean isGUI) {
        final ByteBuffer pixels;

        try {
            // Generar textura en la GPU
            this.id = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, id);
            glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

            // Lee los datos del recurso
            final byte[] resourceData;
            if (compressedFile.equals("graphics.ao")) {
                resourceData = loadLocalGraphic(file);
            } else {
                // Remove .ao extension if present to look for local file
                String localFolder = compressedFile.replace(".ao", "");
                Path localPath = findLocalFile("resources", localFolder, file);
                if (localPath != null && Files.exists(localPath)) {
                    resourceData = Files.readAllBytes(localPath);
                } else {
                    resourceData = null;
                }
            }

            if (resourceData == null) {
                Logger.error("No se pudieron cargar los datos de: " + file
                        + " (probado con .jpg, .png, .bmp en folder local)");
                return;
            }

            final InputStream is = new ByteArrayInputStream(resourceData);
            final BufferedImage image = ImageIO.read(is);

            refTexture.tex_width = image.getWidth();
            refTexture.tex_height = image.getHeight();

            int width = refTexture.tex_width;
            int height = refTexture.tex_height;

            /// A partir de aca, cambiamos la carga de texturas, para que sea compatible en MacOS.

            // ¿Tiene alpha real?
            boolean hasAlpha = image.getColorModel().hasAlpha();

            int[] srcPixels = new int[width * height];
            image.getRGB(0, 0, width, height, srcPixels, 0, width);

            byte[] data = new byte[width * height * 4];

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {

                    int srcIndex = y * width + x;
                    int dstIndex = srcIndex * 4;

                    int pixel = srcPixels[srcIndex];

                    int a = (pixel >> 24) & 0xFF;
                    int r = (pixel >> 16) & 0xFF;
                    int g = (pixel >> 8) & 0xFF;
                    int b = pixel & 0xFF;

                    // BMP legacy: negro = transparente
                    if (!hasAlpha) {
                        if (r == 0 && g == 0 && b == 0) {
                            a = 0;
                            r = 255;
                            g = 255;
                            b = 255;
                        } else {
                            a = 255;
                        }
                    }

                    data[dstIndex]     = (byte) r;
                    data[dstIndex + 1] = (byte) g;
                    data[dstIndex + 2] = (byte) b;
                    data[dstIndex + 3] = (byte) a;
                }
            }

            pixels = BufferUtils.createByteBuffer(data.length);
            pixels.put(data);
            pixels.flip();

            glTexImage2D(
                    GL_TEXTURE_2D,
                    0,
                    GL_RGBA,
                    width,
                    height,
                    0,
                    GL_RGBA,
                    GL_UNSIGNED_BYTE,
                    pixels
            );

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void bind() {
        glBindTexture(GL_TEXTURE_2D, id);
    }

    public void unbind() {
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public int getId() {
        return this.id;
    }

    public int getTex_width() {
        return tex_width;
    }

    public int getTex_height() {
        return tex_height;
    }

    private byte[] loadLocalGraphic(String fileNum) {
        String graphicsPath = Options.INSTANCE.getGraphicsPath();

        // 1. Intentar con PNG en disco
        Path pngPath = Path.of(graphicsPath, fileNum + ".png");
        if (Files.exists(pngPath)) {
            try {
                return Files.readAllBytes(pngPath);
            } catch (IOException ignored) {
            }
        }

        // 2. Intentar con BMP en disco
        Path bmpPath = Path.of(graphicsPath, fileNum + ".bmp");
        if (Files.exists(bmpPath)) {
            try {
                return Files.readAllBytes(bmpPath);
            } catch (IOException ignored) {
            }
        }

        // 3. Fallback: Intentar cargar como recurso embebido (dentro del JAR)
        // Buscamos en /graphics/ o /fonts/graphics/ según convenga
        try (java.io.InputStream is = Texture.class.getResourceAsStream("/graphics/" + fileNum + ".png")) {
            if (is != null)
                return is.readAllBytes();
        } catch (IOException ignored) {
        }

        try (java.io.InputStream is = Texture.class.getResourceAsStream("/graphics/" + fileNum + ".bmp")) {
            if (is != null)
                return is.readAllBytes();
        } catch (IOException ignored) {
        }

        return null;
    }

    private Path findLocalFile(String base, String folder, String filename) {
        Path directPath = Path.of(base, folder, filename);
        if (Files.exists(directPath))
            return directPath;

        // Probar extensiones comunes
        String[] extensions = { ".jpg", ".png", ".bmp" };
        for (String ext : extensions) {
            Path p = Path.of(base, folder, filename + ext);
            if (Files.exists(p))
                return p;
        }
        return null;
    }

    public void createWhitePixel() {
        this.tex_width = 1;
        this.tex_height = 1;

        // Limpiar cualquier bindeo previo para evitar que se pisen texturas
        glBindTexture(GL_TEXTURE_2D, 0);

        this.id = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, id);

        ByteBuffer pixels = BufferUtils.createByteBuffer(4);
        pixels.put((byte) 255).put((byte) 255).put((byte) 255).put((byte) 255);
        pixels.flip();

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, 1, 1, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        // Desvincular texture
        glBindTexture(GL_TEXTURE_2D, 0);
    }

}