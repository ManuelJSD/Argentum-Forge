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

    /**
     * Contenedor para los datos de una textura cargada en memoria RAM
     * pero aún no subida a la GPU.
     */
    public static class TextureData {
        public ByteBuffer pixels;
        public int width;
        public int height;
        public boolean isGUI;
        public String fileName;

        public void cleanup() {
            if (pixels != null) {
                STBImage.stbi_image_free(pixels);
                pixels = null;
            }
        }
    }

    /**
     * Realiza la carga pesada (Disco + Decodificación) de forma síncrona.
     * Diseñado para ser llamado desde hilos secundarios.
     */
    public static TextureData prepareData(String ignoredSource, String file, boolean isGUI) {
        ByteBuffer rawDataBuffer = null;
        try {
            byte[] resourceData = loadLocalGraphicSync(file);
            if (resourceData == null)
                return null;

            rawDataBuffer = BufferUtils.createByteBuffer(resourceData.length);
            rawDataBuffer.put(resourceData);
            rawDataBuffer.flip();

            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer w = stack.mallocInt(1);
                IntBuffer h = stack.mallocInt(1);
                IntBuffer comp = stack.mallocInt(1);

                STBImage.stbi_set_flip_vertically_on_load(false);
                ByteBuffer imageBuffer = STBImage.stbi_load_from_memory(rawDataBuffer, w, h, comp, 4);

                if (imageBuffer == null) {
                    Logger.error("STBI failed to load {}: {}", file, STBImage.stbi_failure_reason());
                    return null;
                }

                TextureData data = new TextureData();
                data.pixels = imageBuffer;
                data.width = w.get(0);
                data.height = h.get(0);
                data.isGUI = isGUI;
                data.fileName = file;

                // Procesar Transparencia (Magic Black Key)
                // Se aplica siempre si es un gráfico del juego (números) para asegurar el fondo
                // transparente
                boolean isLegacyGraphic = file.matches("\\d+.*");
                if (!isGUI || isLegacyGraphic) {
                    applyColorKey(data);
                }

                return data;
            }
        } catch (Exception ex) {
            Logger.error(ex, "Error preparando datos para textura: " + file);
            return null;
        }
    }

    private static void applyColorKey(TextureData data) {
        int pixelCount = data.width * data.height;
        for (int i = 0; i < pixelCount; i++) {
            int offset = i * 4;
            // Robust check: near black pixels (0-3 range) handled as transparent
            // Use & 0xFF to treat bytes as unsigned
            int r = data.pixels.get(offset) & 0xFF;
            int g = data.pixels.get(offset + 1) & 0xFF;
            int b = data.pixels.get(offset + 2) & 0xFF;

            if (r < 12 && g < 12 && b < 12) {
                data.pixels.put(offset, (byte) 0);
                data.pixels.put(offset + 1, (byte) 0);
                data.pixels.put(offset + 2, (byte) 0);
                data.pixels.put(offset + 3, (byte) 0);
            }
        }
    }

    /**
     * Sube los datos preparados a la GPU. DEBE llamarse desde el hilo principal
     * (OpenGL).
     */
    public void upload(TextureData data) {
        if (data == null || data.pixels == null)
            return;
        this.tex_width = data.width;
        this.tex_height = data.height;

        this.id = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, id);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA,
                tex_width, tex_height, 0, GL_RGBA, GL_UNSIGNED_BYTE, data.pixels);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    }

    /**
     * Método original (Síncrono) para compatibilidad o carga inmediata.
     */
    public void loadTexture(Texture refTexture, String compressedFile, String file, boolean isGUI) {
        TextureData data = prepareData(compressedFile, file, isGUI);
        if (data != null) {
            upload(data);
            data.cleanup();
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

    private static byte[] loadLocalGraphicSync(String fileName) {
        String graphicsPath = Options.INSTANCE.getGraphicsPath();
        String[] extensions = { ".png", ".bmp", ".jpg", ".PNG", ".BMP", ".JPG" };

        // 1. Si ya tiene extensión, intentar carga directa o en carpetas de recursos
        // comunes
        if (fileName.contains(".")) {
            // A. Disco (Path configurado)
            if (!graphicsPath.isEmpty()) {
                Path directPath = Path.of(graphicsPath, fileName);
                byte[] data = tryReadFile(directPath);
                if (data != null)
                    return data;
            }

            // B. Carpetas de recursos estandar
            String[] resourceDirs = { "resources/gui/", "resources/graphics/", "resources/" };
            for (String dir : resourceDirs) {
                Path p = Path.of(dir, fileName);
                byte[] data = tryReadFile(p);
                if (data != null)
                    return data;
            }

            // C. Fallback JAR
            String[] jarPaths = { "/graphics/", "/gui/", "/" };
            for (String jarPath : jarPaths) {
                // Try original
                try (java.io.InputStream is = Texture.class.getResourceAsStream(jarPath + fileName)) {
                    if (is != null)
                        return is.readAllBytes();
                } catch (IOException ignored) {
                }

                // Try case variations of common extensions if they match
                String base = fileName.substring(0, fileName.lastIndexOf('.'));
                String ext = fileName.substring(fileName.lastIndexOf('.')).toLowerCase();
                String reversedExt = ext.equals(".png") ? ".PNG"
                        : (ext.equals(".bmp") ? ".BMP" : (ext.equals(".jpg") ? ".JPG" : null));

                if (reversedExt != null) {
                    try (java.io.InputStream is = Texture.class.getResourceAsStream(jarPath + base + reversedExt)) {
                        if (is != null)
                            return is.readAllBytes();
                    } catch (IOException ignored) {
                    }
                }
            }
        }

        // 2. Intentar buscar por nombre base + extensiones
        // A. Disco (Path configurado)
        if (!graphicsPath.isEmpty()) {
            for (String ext : extensions) {
                Path p = Path.of(graphicsPath, fileName + ext);
                byte[] data = tryReadFile(p);
                if (data != null)
                    return data;
            }
        }

        // B. Carpetas de recursos estandar
        String[] resourceDirs = { "resources/graphics/", "resources/gui/", "resources/" };
        for (String dir : resourceDirs) {
            for (String ext : extensions) {
                Path p = Path.of(dir, fileName + ext);
                byte[] data = tryReadFile(p);
                if (data != null)
                    return data;
            }
        }

        // C. Fallback JAR
        String[] jarPaths = { "/graphics/", "/gui/", "/" };
        for (String jarPath : jarPaths) {
            for (String ext : extensions) {
                try (java.io.InputStream is = Texture.class.getResourceAsStream(jarPath + fileName + ext)) {
                    if (is != null)
                        return is.readAllBytes();
                } catch (IOException ignored) {
                }
            }
        }

        Logger.warn("Grafico no encontrado: {} (ID:{}) en {}. Buscado en carpetas de recursos y JAR.", fileName,
                fileName,
                graphicsPath);
        return null;
    }

    private static byte[] tryReadFile(Path path) {
        if (Files.exists(path)) {
            try {
                return Files.readAllBytes(path);
            } catch (IOException ignored) {
            }
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