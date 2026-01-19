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
        final BufferedImage bi;

        try {
            // Generar textura en la GPU
            this.id = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, id);

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

            bi = new BufferedImage(refTexture.tex_width, refTexture.tex_height, BufferedImage.TYPE_4BYTE_ABGR);

            Graphics2D g = bi.createGraphics();
            g.scale(1, -1);
            g.drawImage(image, 0, 0, refTexture.tex_width, -refTexture.tex_height, null);

            final byte[] data = new byte[4 * refTexture.tex_width * refTexture.tex_height];
            bi.getRaster().getDataElements(0, 0, refTexture.tex_width, refTexture.tex_height, data);

            if (!isGUI) {
                for (int j = 0; j < refTexture.tex_width * refTexture.tex_height; j++) {
                    if (data[j * 4] == 0 && data[j * 4 + 1] == 0 && data[j * 4 + 2] == 0) {
                        data[j * 4] = -1;
                        data[j * 4 + 1] = -1;
                        data[j * 4 + 2] = -1;
                        data[j * 4 + 3] = 0;
                    } else
                        data[j * 4 + 3] = -1;
                }
            }

            pixels = BufferUtils.createByteBuffer(data.length);
            pixels.put(data);
            pixels.rewind();

            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA,
                    refTexture.tex_width, refTexture.tex_height, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);

            // Establecer parámetros de la textura
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

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