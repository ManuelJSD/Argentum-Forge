package org.argentumforge.engine.renderer;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.tinylog.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.lwjgl.opengl.GL11.*;
import org.argentumforge.engine.game.Options;

/**
 * La clase {@code Texture} representa una textura en OpenGL para el renderizado
 * grafico.
 * <p>
 * Implementación moderna usando STBImage para evitar dependencia de AWT
 * (Desktop/ImageIO).
 * Esto permite una carga más rápida y mayor portabilidad.
 */
public class Texture {

    private int id;
    private int tex_width;
    private int tex_height;

    public Texture() {

    }

    public void loadTexture(Texture refTexture, String compressedFile, String file, boolean isGUI) {
        ByteBuffer imageBuffer = null;
        ByteBuffer rawDataBuffer = null;

        try {
            // Lee los datos del recurso a un buffer de bytes
            byte[] resourceData;
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

            // Convertir byte[] a DirectByteBuffer para STB
            rawDataBuffer = BufferUtils.createByteBuffer(resourceData.length);
            rawDataBuffer.put(resourceData);
            rawDataBuffer.flip();

            // Cargar imagen usando STBImage
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer w = stack.mallocInt(1);
                IntBuffer h = stack.mallocInt(1);
                IntBuffer comp = stack.mallocInt(1);

                // Flip verticalmente al cargar (Convención usada en Engines OpenGL)
                // UPDATE: Desactivado porque BatchRenderer asume UV origin Top-Left (V=0 -> Top
                // of Image)
                // Al cargar sin flip (Top-Down), el byte 0 es el Top de la imagen.
                // OpenGL interpreta byte 0 como Bottom. Por tanto V=0 apunta al byte 0 (Top
                // Image).
                STBImage.stbi_set_flip_vertically_on_load(false);

                // Cargar decodificando
                imageBuffer = STBImage.stbi_load_from_memory(rawDataBuffer, w, h, comp, 4); // Forzar 4 canales (RGBA)

                if (imageBuffer == null) {
                    Logger.error("Error al decodificar textura " + file + ": " + STBImage.stbi_failure_reason());
                    return;
                }

                refTexture.tex_width = w.get(0);
                refTexture.tex_height = h.get(0);
            }

            // Procesar Transparencia (Magic Black Key) si NO es GUI
            if (!isGUI) {
                // STB devuelve los datos como RGBA directos
                // Iterar sobre los pixeles para aplicar colorkey (0,0,0 -> transparente)
                int pixelCount = refTexture.tex_width * refTexture.tex_height;
                for (int i = 0; i < pixelCount; i++) {
                    int offset = i * 4;
                    byte r = imageBuffer.get(offset);
                    byte g = imageBuffer.get(offset + 1);
                    byte b = imageBuffer.get(offset + 2);

                    // Chequear si es negro absoluto (0,0,0)
                    if (r == 0 && g == 0 && b == 0) {
                        // Hacer transparente
                        imageBuffer.put(offset, (byte) 0); // R
                        imageBuffer.put(offset + 1, (byte) 0); // G
                        imageBuffer.put(offset + 2, (byte) 0); // B
                        imageBuffer.put(offset + 3, (byte) 0); // A (Alpha 0)
                    } else {
                        // Si no es negro, usar alpha actual (que viene del PNG o es 255 si BMP/JPG)
                    }
                }
            }

            // Generar textura en la GPU
            this.id = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, id);

            // Subir datos a VRAM
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA,
                    refTexture.tex_width, refTexture.tex_height, 0, GL_RGBA, GL_UNSIGNED_BYTE, imageBuffer);

            // Establecer parámetros de la textura
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (imageBuffer != null) {
                STBImage.stbi_image_free(imageBuffer);
            }
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

        // Limpiar cualquier bindeo previo, generar y bindear nueva
        glBindTexture(GL_TEXTURE_2D, 0);
        this.id = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, id);

        ByteBuffer pixels = BufferUtils.createByteBuffer(4);
        pixels.put((byte) 255).put((byte) 255).put((byte) 255).put((byte) 255);
        pixels.flip();

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, 1, 1, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public void cleanup() {
        glDeleteTextures(id);
    }
}