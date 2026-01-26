package org.argentumforge.engine.utils.editor;

import org.argentumforge.engine.game.Options;
import org.argentumforge.engine.utils.AssetRegistry;
import org.argentumforge.engine.utils.ProfileManager;
import org.argentumforge.engine.utils.inits.GrhData;
import org.tinylog.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;

/**
 * Herramienta para generar el archivo de colores del minimapa (minimap.bin).
 * Escanea todos los gráficos cargados, lee sus imágenes y calcula el color
 * promedio.
 */
public class MinimapColorGenerator {

    public static boolean generating = false;
    public static float progress = 0.0f;

    public static void generateBinary() {
        if (generating)
            return;

        generating = true;
        progress = 0.0f;

        new Thread(() -> {
            String fileName = ProfileManager.PROFILES_DIR + "/minimap.bin";
            if (org.argentumforge.engine.utils.ProfileManager.INSTANCE.getCurrentProfile() != null) {
                fileName = ProfileManager.PROFILES_DIR + "/minimap_"
                        + org.argentumforge.engine.utils.ProfileManager.INSTANCE.getCurrentProfile().getName() + ".bin";
            }
            Path outputPath = Path.of(fileName);
            Logger.info("Iniciando generacion de colores de minimapa en: {}", outputPath.toAbsolutePath());

            GrhData[] grhData = AssetRegistry.grhData;
            if (grhData == null || grhData.length == 0) {
                Logger.warn("No hay datos de graficos cargados.");
                generating = false;
                return;
            }

            try (FileOutputStream fos = new FileOutputStream(outputPath.toFile());
                    BufferedOutputStream bos = new BufferedOutputStream(fos)) {

                // Buffer para escribir enteros (4 bytes) en Little Endian (estándar
                // VB6/Windows)
                ByteBuffer buffer = ByteBuffer.allocate(4);
                buffer.order(ByteOrder.LITTLE_ENDIAN);

                int total = grhData.length;
                int processed = 0;

                // En VB6 Seek #1, 1 empieza en el byte 1.
                // Iteramos desde 1 hasta GrhCount como en el ejemplo.
                for (int i = 1; i < total; i++) {
                    int color = 0; // Negro por defecto

                    if (grhData[i] != null) {
                        color = calculateAverageColor(grhData[i], i);
                    }

                    // Actualizar mapa en memoria para verlo al instante
                    if (color != 0) {
                        // ImGui usa ABGR o RGBA dependiendo del backend, pero aquí guardamos formato
                        // nativo de AO
                        // para compatibilidad binaria si se desea, o formato ImGui para uso directo.
                        // El loader después convierte.
                        // Pero ojo: VB6 escribe un Long. RGB(r,g,b).
                        // RGB en VB6 es: 0x00BBGGRR (Little Endian en memoria 0xRR, 0xGG, 0xBB, 0x00?)
                        // RGB function returns a Long integer: Red + (Green * 256) + (Blue * 65536).
                        // So Memory layout (Little Endian): R, G, B, 0.

                        // En Java/ImGui queremos 0xAABBGGRR o similar.
                        // Para mantener el formato "minimap.bin" compatible, escribiremos RGB estilo
                        // VB6.
                        // Y actualizaremos el AssetRegistry convirtiendo al vuelo.

                        // Guardamos en memoria como ImGui color (A B G R packed int)
                        int r = color & 0xFF;
                        int g = (color >> 8) & 0xFF;
                        int b = (color >> 16) & 0xFF;

                        // Manual pack to match GameData change and ensure correct ImGui Format (ABGR)
                        // Pack: (A << 24) | (B << 16) | (G << 8) | R
                        int packed = (0xFF << 24) | (b << 16) | (g << 8) | r;
                        AssetRegistry.minimapColors.put(i, packed);
                    }

                    buffer.clear();
                    buffer.putInt(color);
                    bos.write(buffer.array());

                    processed++;
                    progress = (processed / (float) total) * 100;

                    if (processed % 100 == 0) {
                        Logger.info("Generando colores minimapa: {}/{} procesados ({:.0f}%)",
                                processed, total, (processed / (float) total) * 100);
                    }
                }

                Logger.info("Generacion completada. {} colores guardados.", processed);
                javax.swing.JOptionPane.showMessageDialog(null,
                        "Se generaron los colores del minimapa exitosamente.\nArchivo guardado en: "
                                + outputPath.toAbsolutePath(),
                        "Generador de Minimapa", javax.swing.JOptionPane.INFORMATION_MESSAGE);

            } catch (IOException e) {
                Logger.error(e, "Error al generar minimap.bin");
            } finally {
                generating = false;
            }
        }).start();
    }

    private static int calculateAverageColor(GrhData grh, int index) {
        // Validación básica
        if (grh.getFileNum() <= 0)
            return 0;

        // Si es animación, tomamos el primer frame (recursivo seguro porque
        // GrhData.fileNum > 0 indica frame base)
        if (grh.getNumFrames() > 1) {
            // El array de frames tiene índices a otros GRH.
            // Tomamos el frame 1 (índice 1 en array 1-based de AO)
            int firstFrameIndex = grh.getFrame(1);
            if (firstFrameIndex > 0 && firstFrameIndex < AssetRegistry.grhData.length
                    && AssetRegistry.grhData[firstFrameIndex] != null) {
                return calculateAverageColor(AssetRegistry.grhData[firstFrameIndex], firstFrameIndex);
            }
            return 0;
        }

        File file = new File(Options.INSTANCE.getGraphicsPath(), grh.getFileNum() + ".png");
        // Fallback a bmp si no existe png? AO usa png modernamente
        if (!file.exists()) {
            file = new File(Options.INSTANCE.getGraphicsPath(), grh.getFileNum() + ".bmp");
            if (!file.exists())
                return 0;
        }

        try {
            BufferedImage image = ImageIO.read(file);
            if (image == null)
                return 0;

            long rSum = 0, gSum = 0, bSum = 0;
            long count = 0;

            // Coordenadas de recorte
            int sx = (int) grh.getsX();
            int sy = (int) grh.getsY();
            int w = (int) grh.getPixelWidth();
            int h = (int) grh.getPixelHeight();

            if (sx < 0 || sy < 0 || w <= 0 || h <= 0 || sx + w > image.getWidth() || sy + h > image.getHeight()) {
                Logger.warn("GRH {} fuera de limites de imagen: sx={}, sy={}, w={}, h={}, imgW={}, imgH={}",
                        index, sx, sy, w, h, image.getWidth(), image.getHeight());
                return 0;
            }

            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int pixel = image.getRGB(sx + x, sy + y);
                    // Check transparency/alpha/black
                    // VB6 code checks for vbBlack (0). Usually we check alpha or pure black if no
                    // alpha.
                    // Assuming PNG with alpha or RGB with magentamask?
                    // VB6 code snippet: If tempGetPixel = vbBlack Then InvalidPixels
                    // Let's assume strict black keying or alpha if present.

                    int alpha = (pixel >> 24) & 0xFF;
                    int r = (pixel >> 16) & 0xFF;
                    int g = (pixel >> 8) & 0xFF;
                    int b = pixel & 0xFF;

                    // Fuzzy check for Magenta (R>245, B>245, G<10) to catch compression artifacts
                    boolean isTransparent = (alpha == 0) || (r == 0 && g == 0 && b == 0)
                            || (r > 245 && g < 10 && b > 245);

                    if (!isTransparent) {
                        rSum += r;
                        gSum += g;
                        bSum += b;
                        count++;
                    }
                }
            }

            if (count > 0) {
                int rAvg = (int) (rSum / count);
                int gAvg = (int) (gSum / count);
                int bAvg = (int) (bSum / count);

                // Retornar formato VB6 Long RGB: R + G*256 + B*65536
                // Memoria: R, G, B, 0
                return (rAvg) | (gAvg << 8) | (bAvg << 16);
            }

        } catch (IOException e) {
            // Ignorar imagen rota
        }
        return 0;
    }
}
