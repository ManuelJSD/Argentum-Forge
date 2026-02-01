package org.argentumforge.engine.gui;

import imgui.ImGui;
import org.argentumforge.engine.renderer.Surface;
import org.argentumforge.engine.renderer.Texture;
import org.argentumforge.engine.utils.AssetRegistry;
import org.argentumforge.engine.utils.inits.BodyData;
import org.argentumforge.engine.utils.inits.GrhData;
import org.argentumforge.engine.utils.inits.HeadData;

import static org.argentumforge.engine.utils.AssetRegistry.grhData;

/**
 * Utilidades para previsualizar elementos gráficos en la interfaz de ImGui.
 */
public final class PreviewUtils {

    private PreviewUtils() {
    }

    /**
     * Dibuja un GrhIndex dentro de la interfaz de ImGui.
     */
    public static void drawGrh(int grhIndex, float scale) {
        if (grhIndex <= 0 || grhIndex >= grhData.length || grhData[grhIndex] == null) {
            return;
        }

        int currentGrh = grhIndex;
        if (grhData[grhIndex].getNumFrames() > 1) {
            currentGrh = grhData[grhIndex].getFrame(1); // Frames en AO son 1-indexed
        }

        GrhData data = grhData[currentGrh];
        if (data.getFileNum() <= 0)
            return;

        Texture tex = Surface.INSTANCE.getTexture(data.getFileNum());
        if (tex == null)
            return;

        float u0 = data.getsX() / (float) tex.getTex_width();
        float v0 = (data.getsY() + data.getPixelHeight()) / (float) tex.getTex_height();
        float u1 = (data.getsX() + data.getPixelWidth()) / (float) tex.getTex_width();
        float v1 = data.getsY() / (float) tex.getTex_height();

        ImGui.image(tex.getId(), data.getPixelWidth() * scale, data.getPixelHeight() * scale, u0, v1, u1, v0);
    }

    /**
     * Dibuja un GrhIndex ajustado (aspect ratio) y centrado dentro de un área
     * específica.
     */
    public static void drawGrhFit(int grhIndex, float maxWidth, float maxHeight) {
        if (grhIndex <= 0 || grhIndex >= grhData.length || grhData[grhIndex] == null)
            return;

        int currentGrh = grhIndex;
        if (grhData[grhIndex].getNumFrames() > 1) {
            currentGrh = grhData[grhIndex].getFrame(1);
        }

        GrhData data = grhData[currentGrh];
        if (data.getFileNum() <= 0)
            return;

        Texture tex = Surface.INSTANCE.getTexture(data.getFileNum());
        if (tex == null)
            return;

        float w = data.getPixelWidth();
        float h = data.getPixelHeight();

        // Calcular escala para que quepa en el área manteniendo proporción
        float scale = 1.0f;
        if (w > maxWidth || h > maxHeight) {
            scale = Math.min(maxWidth / w, maxHeight / h);
        }

        float finalW = w * scale;
        float finalH = h * scale;

        // Guardar posición inicial para centrar
        float startX = ImGui.getCursorPosX();
        float startY = ImGui.getCursorPosY();

        // Centrar dentro del área maxWidth x maxHeight
        ImGui.setCursorPos(startX + (maxWidth - finalW) / 2, startY + (maxHeight - finalH) / 2);

        float u0 = data.getsX() / (float) tex.getTex_width();
        float v0 = (data.getsY() + data.getPixelHeight()) / (float) tex.getTex_height();
        float u1 = (data.getsX() + data.getPixelWidth()) / (float) tex.getTex_width();
        float v1 = data.getsY() / (float) tex.getTex_height();

        ImGui.image(tex.getId(), finalW, finalH, u0, v1, u1, v0);

        // Restaurar cursor al inicio del área lógica para que ImGui sepa que ocupamos
        // ese
        // espacio
        ImGui.setCursorPos(startX, startY);
        ImGui.dummy(maxWidth, maxHeight);
    }

    /**
     * Dibuja un mosaico completo (GrhIndex + rejilla) en ImGui.
     */
    public static void drawGrhMosaic(int grhIndex, int width, int height, float maxWidth, float maxHeight) {
        if (grhIndex <= 0 || grhIndex >= grhData.length || grhData[grhIndex] == null)
            return;

        float totalW = 0;
        float totalH = 0;

        // Calcular tamaño total del mosaico asumiendo que todos los tiles miden lo
        // mismo que el primero
        GrhData baseData = grhData[grhIndex];
        totalW = baseData.getPixelWidth() * width;
        totalH = baseData.getPixelHeight() * height;

        float scale = 1.0f;
        if (totalW > maxWidth || totalH > maxHeight) {
            scale = Math.min(maxWidth / totalW, maxHeight / totalH);
        }

        float finalW = totalW * scale;
        float finalH = totalH * scale;
        float tileW = baseData.getPixelWidth() * scale;
        float tileH = baseData.getPixelHeight() * scale;

        float startX = ImGui.getCursorPosX();
        float startY = ImGui.getCursorPosY();

        // Centrar
        ImGui.setCursorPos(startX + (maxWidth - finalW) / 2, startY + (maxHeight - finalH) / 2);
        float currentX = ImGui.getCursorPosX();
        float currentY = ImGui.getCursorPosY();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int currentGrh = grhIndex + (y * width) + x;
                if (currentGrh >= grhData.length || grhData[currentGrh] == null)
                    continue;

                GrhData data = grhData[currentGrh];

                // Si el tile es una animación (como el agua), resolvemos el frame
                if (data.getNumFrames() > 1) {
                    int frame1 = data.getFrame(1);
                    if (frame1 > 0 && frame1 < grhData.length && grhData[frame1] != null) {
                        data = grhData[frame1];
                    }
                }

                Texture tex = Surface.INSTANCE.getTexture(data.getFileNum());
                if (tex != null) {
                    float u0 = data.getsX() / (float) tex.getTex_width();
                    float v0 = (data.getsY() + data.getPixelHeight()) / (float) tex.getTex_height();
                    float u1 = (data.getsX() + data.getPixelWidth()) / (float) tex.getTex_width();
                    float v1 = data.getsY() / (float) tex.getTex_height();

                    ImGui.setCursorPos(currentX + (x * tileW), currentY + (y * tileH));
                    ImGui.image(tex.getId(), tileW, tileH, u0, v1, u1, v0);
                }
            }
        }

        ImGui.setCursorPos(startX, startY);
        ImGui.dummy(maxWidth, maxHeight);
    }

    /**
     * Dibuja un NPC (Cuerpo + Cabeza) en ImGui de forma relativa al cursor actual.
     */
    public static void drawNpc(int bodyIndex, int headIndex, float scale) {
        if (bodyIndex <= 0 || bodyIndex >= AssetRegistry.bodyData.length || AssetRegistry.bodyData[bodyIndex] == null)
            return;

        BodyData body = AssetRegistry.bodyData[bodyIndex];

        // Dirección 3 (Sur)
        int bodyGrhIndex = body.getWalk(3).getGrhIndex();
        if (bodyGrhIndex <= 0)
            bodyGrhIndex = body.getWalk(1).getGrhIndex();

        if (bodyGrhIndex <= 0)
            return;

        // Guardamos posición inicial del cursor (posición actual antes de dibujar nada)
        float startPosX = ImGui.getCursorPosX();
        float startPosY = ImGui.getCursorPosY();

        // 1. Dibujar Cuerpo
        drawGrhRelative(bodyGrhIndex, startPosX, startPosY, scale, 0, 0);

        // 2. Dibujar Cabeza
        if (headIndex > 0 && headIndex < AssetRegistry.headData.length && AssetRegistry.headData[headIndex] != null) {
            HeadData head = AssetRegistry.headData[headIndex];
            int headGrhIndex = head.getHead(3).getGrhIndex();
            if (headGrhIndex <= 0)
                headGrhIndex = head.getHead(1).getGrhIndex();

            if (headGrhIndex > 0) {
                float offsetX = body.getHeadOffset().getX() * scale;
                float offsetY = body.getHeadOffset().getY() * scale;
                drawGrhRelative(headGrhIndex, startPosX, startPosY, scale, offsetX, offsetY);
            }
        }

        // Dejar el cursor al final del área (ajustado para que el dummy no desplace
        // demasiado)
        ImGui.setCursorPos(startPosX, startPosY);
        ImGui.dummy(64 * scale, 64 * scale);
    }

    private static void drawGrhRelative(int grhIndex, float basePosX, float basePosY, float scale, float offsetX,
            float offsetY) {
        if (grhIndex <= 0 || grhData[grhIndex] == null)
            return;

        GrhData grh = grhData[grhIndex];
        // En este motor los frames son 1-indexed para animaciones
        int frameIndex = grh.getNumFrames() > 1 ? grh.getFrame(1) : grhIndex;

        if (frameIndex <= 0 || frameIndex >= grhData.length || grhData[frameIndex] == null)
            return;

        GrhData data = grhData[frameIndex];
        Texture tex = Surface.INSTANCE.getTexture(data.getFileNum());
        if (tex == null)
            return;

        float w = data.getPixelWidth() * scale;
        float h = data.getPixelHeight() * scale;

        float u0 = data.getsX() / (float) tex.getTex_width();
        float v0 = (data.getsY() + data.getPixelHeight()) / (float) tex.getTex_height();
        float u1 = (data.getsX() + data.getPixelWidth()) / (float) tex.getTex_width();
        float v1 = data.getsY() / (float) tex.getTex_height();

        // Centrado: basePosX es el inicio del área.
        // AO dibuja desde los pies. Queremos que los pies estén cerca de la base del
        // área (basePosY + 64*scale)
        float targetX = basePosX + offsetX + (32 * scale) - (w / 2);
        float targetY = basePosY + offsetY + (62 * scale) - h;

        ImGui.setCursorPos(targetX, targetY);
        ImGui.image(tex.getId(), w, h, u0, v1, u1, v0);
    }
}
