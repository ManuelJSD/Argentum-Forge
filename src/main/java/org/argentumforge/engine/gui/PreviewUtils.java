package org.argentumforge.engine.gui;

import imgui.ImGui;
import imgui.ImDrawList;
import org.argentumforge.engine.renderer.Surface;
import org.argentumforge.engine.renderer.Texture;
import org.argentumforge.engine.utils.GameData;
import org.argentumforge.engine.utils.inits.BodyData;
import org.argentumforge.engine.utils.inits.GrhData;
import org.argentumforge.engine.utils.inits.HeadData;

import static org.argentumforge.engine.utils.GameData.grhData;

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

        // Si es animado, mostramos el primer frame
        int currentGrh = grhIndex;
        if (grhData[grhIndex].getNumFrames() > 1) {
            currentGrh = grhData[grhIndex].getFrame(0);
        }

        GrhData data = grhData[currentGrh];
        if (data.getFileNum() <= 0)
            return;

        Texture tex = Surface.INSTANCE.getTexture(data.getFileNum());
        if (tex == null)
            return;

        float texW = tex.getTex_width();
        float texH = tex.getTex_height();

        // Calcular UVs (ImGui usa 0.0 a 1.0)
        float u0 = data.getsX() / texW;
        float v0 = (data.getsY() + data.getPixelHeight()) / texH;
        float u1 = (data.getsX() + data.getPixelWidth()) / texW;
        float v1 = data.getsY() / texH;

        // ImGui.image: texture_id, size_x, size_y, uv0 (top-left), uv1 (bottom-right)
        // Engine renders textures flipped, so we use v1 (top) and v0 (bottom).
        ImGui.image(tex.getId(), data.getPixelWidth() * scale, data.getPixelHeight() * scale, u0, v1, u1, v0);
    }

    /**
     * Dibuja un NPC (Cuerpo + Cabeza) en ImGui usando la DrawList para mejor
     * control de capas.
     */
    public static void drawNpc(int bodyIndex, int headIndex, float scale) {
        if (bodyIndex <= 0 || bodyIndex >= GameData.bodyData.length || GameData.bodyData[bodyIndex] == null)
            return;

        BodyData body = GameData.bodyData[bodyIndex];
        int bodyGrhIndex = body.getWalk(3).getGrhIndex(); // 3 = Sur en AO usualmente, o 1. Probamos 3.
        if (bodyGrhIndex <= 0)
            bodyGrhIndex = body.getWalk(1).getGrhIndex();
        if (bodyGrhIndex <= 0)
            return;

        GrhData bodyData = grhData[bodyGrhIndex];
        if (bodyData.getFileNum() <= 0)
            return;

        Texture bodyTex = Surface.INSTANCE.getTexture(bodyData.getFileNum());
        if (bodyTex == null)
            return;

        // Reservar espacio en el layout de ImGui
        float startX = ImGui.getCursorScreenPos().x;
        float startY = ImGui.getCursorScreenPos().y;
        float areaWidth = 100 * scale;
        float areaHeight = 100 * scale;
        ImGui.dummy(areaWidth, areaHeight);

        ImDrawList drawList = ImGui.getWindowDrawList();

        // Centro de la base (donde estarian los pies)
        float centerX = startX + areaWidth / 2;
        float bottomY = startY + areaHeight - 10 * scale;

        // Renderizar Cuerpo
        renderGrhToDrawList(drawList, bodyGrhIndex, centerX, bottomY, scale, true);

        // Renderizar Cabeza
        if (headIndex > 0 && headIndex < GameData.headData.length && GameData.headData[headIndex] != null) {
            HeadData head = GameData.headData[headIndex];
            int headGrhIndex = head.getHead(3).getGrhIndex();
            if (headGrhIndex <= 0)
                headGrhIndex = head.getHead(1).getGrhIndex();

            if (headGrhIndex > 0) {
                float headX = centerX + body.getHeadOffset().getX() * scale;
                float headY = bottomY + body.getHeadOffset().getY() * scale;
                renderGrhToDrawList(drawList, headGrhIndex, headX, headY, scale, true);
            }
        }
    }

    private static void renderGrhToDrawList(ImDrawList drawList, int grhIndex, float x, float y, float scale,
            boolean center) {
        if (grhIndex <= 0 || grhData[grhIndex] == null)
            return;

        int currentGrh = grhIndex;
        if (grhData[grhIndex].getNumFrames() > 1) {
            currentGrh = grhData[grhIndex].getFrame(0);
        }
        GrhData data = grhData[currentGrh];
        Texture tex = Surface.INSTANCE.getTexture(data.getFileNum());
        if (tex == null)
            return;

        float w = data.getPixelWidth() * scale;
        float h = data.getPixelHeight() * scale;

        float drawX = x;
        float drawY = y;

        if (center) {
            drawX = x - w / 2;
            drawY = y - h;
        }

        float u0 = data.getsX() / (float) tex.getTex_width();
        float v0 = (data.getsY() + data.getPixelHeight()) / (float) tex.getTex_height();
        float u1 = (data.getsX() + data.getPixelWidth()) / (float) tex.getTex_width();
        float v1 = data.getsY() / (float) tex.getTex_height();

        // ImGui addImage: p_min (top-left), p_max (bottom-right), uv_min (top-left),
        // uv_max (bottom-right)
        // Engine renders textures flipped, so we use v1 (top) and v0 (bottom).
        drawList.addImage(tex.getId(), drawX, drawY, drawX + w, drawY + h, u0, v1, u1, v0);
    }
}
