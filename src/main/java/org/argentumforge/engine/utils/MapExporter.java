package org.argentumforge.engine.utils;

import org.argentumforge.engine.Engine;
import org.argentumforge.engine.game.Options;
import org.argentumforge.engine.game.Weather;
import org.argentumforge.engine.renderer.RenderSettings;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.argentumforge.engine.game.models.Character.drawCharacter;
import static org.argentumforge.engine.renderer.Drawn.drawTexture;
// import static org.argentumforge.engine.utils.GameData.mapData; // Removed static import
import static org.argentumforge.engine.utils.AssetRegistry.grhData;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.stb.STBImageWrite.*;

public class MapExporter {

    private static final int TILE_SIZE = 32;

    public static void exportMap(String filePath) {
        var context = GameData.getActiveContext();
        if (context == null || context.getMapData() == null)
            return;

        var mapData = context.getMapData();

        int mapWidth = mapData.length;
        int mapHeight = mapData[0].length;
        int pixelWidth = mapWidth * TILE_SIZE;
        int pixelHeight = mapHeight * TILE_SIZE;

        // Guardar estado previo
        int[] prevViewport = new int[4];
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer viewportBuff = stack.mallocInt(4);
            glGetIntegerv(GL_VIEWPORT, viewportBuff);
            prevViewport[0] = viewportBuff.get(0);
            prevViewport[1] = viewportBuff.get(1);
            prevViewport[2] = viewportBuff.get(2);
            prevViewport[3] = viewportBuff.get(3);
        }

        // Crear Framebuffer
        int fbo = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);

        // Crear Textura para el FBO
        int texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, pixelWidth, pixelHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE,
                (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture, 0);

        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            System.err.println("Error: Framebuffer incompleto para exportación.");
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
            glDeleteTextures(texture);
            glDeleteFramebuffers(fbo);
            return;
        }

        // Configurar Viewport y Proyección
        glViewport(0, 0, pixelWidth, pixelHeight);
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0, pixelWidth, pixelHeight, 0, 1, -1); // Y-down
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();

        // Renderizar
        glClearColor(0, 0, 0, 1);
        glClear(GL_COLOR_BUFFER_BIT);

        Engine.batch.begin();
        renderMapContent(mapData, mapWidth, mapHeight);
        Engine.batch.end();

        // Leer Píxeles
        ByteBuffer buffer = BufferUtils.createByteBuffer(pixelWidth * pixelHeight * 4);
        glReadPixels(0, 0, pixelWidth, pixelHeight, GL_RGBA, GL_UNSIGNED_BYTE, buffer);

        // STB Image Write guarda de abajo hacia arriba por defecto en OpenGL si no se
        // flipea,
        // pero como usamos glOrtho Y-down, el origen (0,0) es Arriba-Izquierda.
        // glReadPixels lee desde Abajo-Izquierda.
        // Por ende, la imagen saldrá invertida verticalmente.
        // Usamos stbi_flip_vertically_on_write(1) ??
        // O mejor invertimos manualmente el buffer o usamos flag de STB.
        // Probamos stbi_flip... no existe en WriteJava binding directo a veces, pero
        // stbi_write_png tiene stride. Si stride es 0, es packed.
        // Vamos a guardar y ver. Si sale invertida, habilitamos flip.
        // UPDATE: LWJGL stbi_write_png no tiene flag global de flip.
        // Pero GL lee desde abajo. Nuestra proyección es Y-down.
        // Textura (0,0) está "arriba" lógicamente.
        // ReadPixels (0,0) es "abajo" físicamente en la textura.
        // Resultado: Imagen invertida.
        // Solución: Flip buffer manualmente o renderizar invertido?
        // Mejor flip buffer.

        flipBuffer(buffer, pixelWidth, pixelHeight);

        stbi_write_png(filePath, pixelWidth, pixelHeight, 4, buffer, pixelWidth * 4);

        // Restaurar
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();
        glViewport(prevViewport[0], prevViewport[1], prevViewport[2], prevViewport[3]);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glDeleteTextures(texture);
        glDeleteFramebuffers(fbo);

        System.out.println("Mapa exportado a: " + filePath);
    }

    private static void renderMapContent(org.argentumforge.engine.utils.inits.MapData[][] mapData, int mapWidth,
            int mapHeight) {
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

        // Capa 2 y Objetos
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

        // Capa 3, Personajes y Objetos Grandes
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
