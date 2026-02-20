package org.argentumforge.engine.utils;

import org.tinylog.Logger;
import org.argentumforge.engine.Engine;
import org.argentumforge.engine.game.Options;
import org.argentumforge.engine.game.Weather;
import org.argentumforge.engine.renderer.RenderSettings;
import org.argentumforge.engine.renderer.Surface;
import org.argentumforge.engine.utils.inits.MapData;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashSet;
import java.util.Set;

import static org.argentumforge.engine.game.models.Character.drawCharacter;
import static org.argentumforge.engine.renderer.Drawn.drawTexture;
import static org.argentumforge.engine.utils.AssetRegistry.grhData;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.stb.STBImageWrite.*;

/**
 * Exporta el mapa actual a una imagen PNG.
 * <p>
 * Usa un FBO off-screen con proyección ortográfica 1:1 (sin zoom de cámara),
 * pre-carga síncronamente todas las texturas requeridas y muestra un overlay
 * de "Exportando..." al usuario mientras el proceso está en curso.
 */
public class MapExporter {

    private static final int TILE_SIZE = 32;

    /**
     * Flag estático consultable desde el loop de render para mostrar el
     * overlay "Exportando mapa...".
     */
    public static volatile boolean isExporting = false;

    public static void exportMap(String filePath) {
        var context = GameData.getActiveContext();
        if (context == null || context.getMapData() == null)
            return;

        var mapData = context.getMapData();

        int mapWidth = mapData.length;
        int mapHeight = mapData[0].length;
        int pixelWidth = mapWidth * TILE_SIZE;
        int pixelHeight = mapHeight * TILE_SIZE;

        // Activar overlay de exportación
        isExporting = true;
        Logger.info("MapExporter: Iniciando exportación {}x{} px ({} tiles)", pixelWidth, pixelHeight,
                mapWidth * mapHeight);

        try {
            // --- PASO 1: Pre-cargar texturas síncronamente ---
            Set<Integer> fileNums = collectFileNums(mapData, mapWidth, mapHeight);
            Surface.INSTANCE.preloadSync(fileNums);

            // --- PASO 2: Guardar estado OpenGL previo ---
            int[] prevViewport = new int[4];
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer viewportBuff = stack.mallocInt(4);
                glGetIntegerv(GL_VIEWPORT, viewportBuff);
                prevViewport[0] = viewportBuff.get(0);
                prevViewport[1] = viewportBuff.get(1);
                prevViewport[2] = viewportBuff.get(2);
                prevViewport[3] = viewportBuff.get(3);
            }

            // --- PASO 3: Crear FBO ---
            int fbo = glGenFramebuffers();
            glBindFramebuffer(GL_FRAMEBUFFER, fbo);

            int texture = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, texture);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, pixelWidth, pixelHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE,
                    (ByteBuffer) null);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture, 0);

            if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
                Logger.error("MapExporter: Framebuffer incompleto para exportación ({} x {}).", pixelWidth,
                        pixelHeight);
                glBindFramebuffer(GL_FRAMEBUFFER, 0);
                glDeleteTextures(texture);
                glDeleteFramebuffers(fbo);
                return;
            }

            // --- PASO 4: Configurar viewport y proyección del BatchRenderer ---
            glViewport(0, 0, pixelWidth, pixelHeight);

            // Indicar al BatchRenderer que use la proyección del FBO (1:1, sin zoom)
            Engine.batch.setExportProjection(pixelWidth, pixelHeight);

            // --- PASO 5: Renderizar al FBO ---
            glClearColor(0f, 0f, 0f, 1f);
            glClear(GL_COLOR_BUFFER_BIT);

            Engine.batch.begin();
            renderMapContent(mapData, mapWidth, mapHeight);
            Engine.batch.end();

            // --- PASO 6: Leer píxeles y guardar PNG ---
            ByteBuffer buffer = BufferUtils.createByteBuffer(pixelWidth * pixelHeight * 4);
            glReadPixels(0, 0, pixelWidth, pixelHeight, GL_RGBA, GL_UNSIGNED_BYTE, buffer);

            // glReadPixels lee desde abajo-izquierda; nuestra proyección es Y-down,
            // por lo que la imagen saldrá invertida verticalmente → flip manual.
            flipBuffer(buffer, pixelWidth, pixelHeight);

            stbi_write_png(filePath, pixelWidth, pixelHeight, 4, buffer, pixelWidth * 4);
            Logger.info("MapExporter: Mapa exportado correctamente → {}", filePath);

            // --- PASO 7: Restaurar estado OpenGL ---
            Engine.batch.clearExportProjection();
            glViewport(prevViewport[0], prevViewport[1], prevViewport[2], prevViewport[3]);
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
            glDeleteTextures(texture);
            glDeleteFramebuffers(fbo);

        } finally {
            // Siempre desactivar el overlay, incluso si hubo error
            isExporting = false;
        }
    }

    /**
     * Recopila todos los {@code fileNum} únicos de las texturas usadas en el mapa
     * para poder pre-cargarlas síncronamente antes de renderizar.
     */
    private static Set<Integer> collectFileNums(MapData[][] mapData, int mapWidth, int mapHeight) {
        Set<Integer> fileNums = new HashSet<>();
        for (int x = 0; x < mapWidth; x++) {
            for (int y = 0; y < mapHeight; y++) {
                if (mapData[x][y] == null)
                    continue;
                addGrhFileNum(fileNums, mapData[x][y].getLayer(1).getGrhIndex());
                addGrhFileNum(fileNums, mapData[x][y].getLayer(2).getGrhIndex());
                addGrhFileNum(fileNums, mapData[x][y].getLayer(3).getGrhIndex());
                addGrhFileNum(fileNums, mapData[x][y].getLayer(4).getGrhIndex());
                addGrhFileNum(fileNums, mapData[x][y].getObjGrh().getGrhIndex());
            }
        }
        fileNums.remove(0); // Eliminar índice vacío si quedó
        Logger.info("MapExporter: Se necesitan {} archivos de textura únicos.", fileNums.size());
        return fileNums;
    }

    /**
     * Agrega el {@code fileNum} del GRH al conjunto, validando índices.
     */
    private static void addGrhFileNum(Set<Integer> fileNums, int grhIndex) {
        if (grhIndex <= 0 || grhIndex >= grhData.length || grhData[grhIndex] == null)
            return;
        // GRHs animados: agregar el fileNum de todos sus frames
        int numFrames = grhData[grhIndex].getNumFrames();
        for (int f = 0; f < numFrames; f++) {
            int frameIndex = grhData[grhIndex].getFrame(f);
            if (frameIndex > 0 && frameIndex < grhData.length && grhData[frameIndex] != null) {
                fileNums.add(grhData[frameIndex].getFileNum());
            }
        }
    }

    private static void renderMapContent(MapData[][] mapData, int mapWidth, int mapHeight) {
        RenderSettings renderSettings = Options.INSTANCE.getRenderSettings();
        Weather weather = Weather.INSTANCE;

        // Capa 1
        if (renderSettings.getShowLayer()[0]) {
            for (int x = 0; x < mapWidth; x++) {
                for (int y = 0; y < mapHeight; y++) {
                    if (mapData[x][y] == null)
                        continue;
                    if (mapData[x][y].getLayer(1).getGrhIndex() != 0) {
                        drawTexture(mapData[x][y].getLayer(1), x * TILE_SIZE, y * TILE_SIZE,
                                true, true, false, 1.0f, weather.getWeatherColor());
                    }
                }
            }
        }

        // Capa 2 y Objetos pequeños (32x32)
        if (renderSettings.getShowLayer()[1] || renderSettings.getShowOJBs()) {
            for (int x = 0; x < mapWidth; x++) {
                for (int y = 0; y < mapHeight; y++) {
                    if (mapData[x][y] == null)
                        continue;

                    if (renderSettings.getShowLayer()[1]) {
                        if (mapData[x][y].getLayer(2).getGrhIndex() != 0) {
                            drawTexture(mapData[x][y].getLayer(2), x * TILE_SIZE, y * TILE_SIZE,
                                    true, true, false, 1.0f, weather.getWeatherColor());
                        }
                    }
                    if (renderSettings.getShowOJBs()) {
                        if (mapData[x][y].getObjGrh().getGrhIndex() != 0) {
                            if (grhData[mapData[x][y].getObjGrh().getGrhIndex()].getPixelWidth() == TILE_SIZE &&
                                    grhData[mapData[x][y].getObjGrh().getGrhIndex()].getPixelHeight() == TILE_SIZE) {
                                drawTexture(mapData[x][y].getObjGrh(), x * TILE_SIZE, y * TILE_SIZE,
                                        true, true, false, 1.0f, weather.getWeatherColor());
                            }
                        }
                    }
                }
            }
        }

        // Capa 3, NPCs y Objetos grandes
        for (int x = 0; x < mapWidth; x++) {
            for (int y = 0; y < mapHeight; y++) {
                if (mapData[x][y] == null)
                    continue;

                if (renderSettings.getShowOJBs()) {
                    if (mapData[x][y].getObjGrh().getGrhIndex() != 0) {
                        if (grhData[mapData[x][y].getObjGrh().getGrhIndex()].getPixelWidth() != TILE_SIZE &&
                                grhData[mapData[x][y].getObjGrh().getGrhIndex()].getPixelHeight() != TILE_SIZE) {
                            drawTexture(mapData[x][y].getObjGrh(), x * TILE_SIZE, y * TILE_SIZE,
                                    true, true, false, 1.0f, weather.getWeatherColor());
                        }
                    }
                }

                if (mapData[x][y].getCharIndex() != 0 && renderSettings.getShowNPCs()) {
                    drawCharacter(mapData[x][y].getCharIndex(), x * TILE_SIZE, y * TILE_SIZE, 1.0f,
                            weather.getWeatherColor());
                }

                if (renderSettings.getShowLayer()[2]) {
                    if (mapData[x][y].getLayer(3).getGrhIndex() != 0) {
                        drawTexture(mapData[x][y].getLayer(3), x * TILE_SIZE, y * TILE_SIZE,
                                true, true, false, 1.0f, weather.getWeatherColor());
                    }
                }
            }
        }

        // Capa 4
        if (renderSettings.getShowLayer()[3]) {
            for (int x = 0; x < mapWidth; x++) {
                for (int y = 0; y < mapHeight; y++) {
                    if (mapData[x][y] == null)
                        continue;
                    if (mapData[x][y].getLayer(4).getGrhIndex() > 0) {
                        drawTexture(mapData[x][y].getLayer(4), x * TILE_SIZE, y * TILE_SIZE,
                                true, true, false, 1.0f, weather.getWeatherColor());
                    }
                }
            }
        }
    }

    private static void flipBuffer(ByteBuffer buffer, int width, int height) {
        int stride = width * 4;
        byte[] row = new byte[stride];
        for (int y = 0; y < height / 2; y++) {
            int topOffset = y * stride;
            int bottomOffset = (height - y - 1) * stride;

            buffer.position(topOffset);
            buffer.get(row);

            byte[] bottomRow = new byte[stride];
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
