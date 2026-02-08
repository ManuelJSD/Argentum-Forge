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

    /**
     * Dibuja una previsualización de un Prefab con escalado inteligente basado en
     * el contenido real.
     */
    public static void drawPrefab(org.argentumforge.engine.utils.editor.models.Prefab prefab, float maxWidth,
            float maxHeight) {
        if (prefab == null || prefab.getData().isEmpty())
            return;

        // 1. Calcular Bounding Box Real del contenido visual
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        boolean hasContent = false;

        for (org.argentumforge.engine.utils.editor.models.Prefab.PrefabCell cell : prefab.getData()) {
            float cellBaseX = cell.x * 32.0f; // Posición base del tile en pixeles del mundo
            float cellBaseY = cell.y * 32.0f;

            for (int l = 1; l <= 4; l++) {
                int grhIndex = cell.layerGrhs[l];
                if (grhIndex <= 0)
                    continue;

                GrhData data = grhData[grhIndex];
                if (data == null)
                    continue;
                if (data.getNumFrames() > 1) {
                    int f1 = data.getFrame(1);
                    if (f1 > 0 && f1 < grhData.length && grhData[f1] != null)
                        data = grhData[f1];
                    else
                        continue; // If frame 1 is invalid, skip this grh
                }
                if (data == null || data.getFileNum() <= 0)
                    continue;

                // Calcular bounds de este gráfico
                // En AO, los gráficos se dibujan:
                // X: Centrado en el tile -> cellBaseX - (pixelWidth - 32) / 2
                // Y: Bottom aligned -> cellBaseY - (pixelHeight - 32)

                float grhW = data.getPixelWidth();
                float grhH = data.getPixelHeight();

                float gX = cellBaseX - (grhW - 32) / 2.0f;
                float gY = cellBaseY - (grhH - 32); // Y crece hacia abajo en screen coords?
                // Ojo: En este motor (libGDX/LWJGL setup tradicional), Y suele ser invertido o
                // no.
                // Pero aquí estamos en ImGui.
                // cell.y aumenta hacia abajo (filas 0, 1, 2...).
                // Un gráfico offsetado "arriba" debería tener Y MENOR.
                // (pixelHeight - 32) es positivo si el gráfico es alto (ej arbol).
                // cellBaseY - positivo = Y menor (más arriba). CORRECTO.

                if (gX < minX)
                    minX = gX;
                if (gY < minY)
                    minY = gY;
                if (gX + grhW > maxX)
                    maxX = gX + grhW;
                if (gY + grhH > maxY)
                    maxY = gY + grhH;

                hasContent = true;
            }
        }

        if (!hasContent)
            return;

        // Añadir un pequeño margen
        float margin = 0.0f; // No margin for now, let's see how it looks
        minX -= margin;
        minY -= margin;
        maxX += margin;
        maxY += margin;

        float contentW = maxX - minX;
        float contentH = maxY - minY;

        if (contentW <= 0 || contentH <= 0)
            return;

        // 2. Calcular Escala para encajar en maxWidth/maxHeight
        float scale = 1.0f;
        if (contentW > maxWidth || contentH > maxHeight) {
            scale = Math.min(maxWidth / contentW, maxHeight / contentH);
        } else {
            // Si es más pequeño, escalar UP para llenar?
            // O mantener 1.0?
            // Mejor escalar UP hasta cierto límite (ej 2.0) o llenar el espacio para
            // uniformidad.
            // For now, if it's smaller, we'll just scale it up to fit the smaller
            // dimension,
            // but not exceed 1.0 if it's already small enough.
            // Let's just use the same logic as above, it will result in scale <= 1.0 if
            // content fits.
            scale = Math.min(maxWidth / contentW, maxHeight / contentH);
            if (scale > 1.0f)
                scale = 1.0f; // Don't upscale if it already fits
        }

        float finalW = contentW * scale;
        float finalH = contentH * scale;

        float startX = ImGui.getCursorPosX();
        float startY = ImGui.getCursorPosY();

        // 3. Centrar en el área destino
        float offsetX = (maxWidth - finalW) / 2;
        float offsetY = (maxHeight - finalH) / 2;

        // Fondo y Borde de debug (opcional)
        ImGui.getWindowDrawList().addRectFilled(
                startX + offsetX, startY + offsetY,
                startX + offsetX + finalW, startY + offsetY + finalH,
                ImGui.getColorU32(0, 0, 0, 100));

        // 4. Dibujar
        for (org.argentumforge.engine.utils.editor.models.Prefab.PrefabCell cell : prefab.getData()) {
            float cellBaseX = cell.x * 32.0f;
            float cellBaseY = cell.y * 32.0f;

            for (int l = 1; l <= 4; l++) {
                int grhIndex = cell.layerGrhs[l];
                if (grhIndex > 0) {
                    drawGrhWithBounds(grhIndex, cellBaseX, cellBaseY, startX + offsetX, startY + offsetY, minX, minY,
                            scale);
                }
            }
        }

        ImGui.setCursorPos(startX, startY);
        ImGui.dummy(maxWidth, maxHeight);
    }

    private static void drawGrhWithBounds(int grhIndex, float cellBaseX, float cellBaseY, float drawOriginX,
            float drawOriginY, float contentMinX, float contentMinY, float scale) {
        if (grhIndex <= 0 || grhIndex >= grhData.length || grhData[grhIndex] == null)
            return;

        GrhData data = grhData[grhIndex];
        if (data.getNumFrames() > 1) {
            int f1 = data.getFrame(1);
            if (f1 > 0 && f1 < grhData.length && grhData[f1] != null)
                data = grhData[f1];
            else
                return; // If frame 1 is invalid, skip this grh
        }
        if (data.getFileNum() <= 0)
            return;

        Texture tex = Surface.INSTANCE.getTexture(data.getFileNum());
        if (tex == null)
            return;

        float grhW = data.getPixelWidth();
        float grhH = data.getPixelHeight();

        // Calculate graphic's position in the prefab's original coordinate system
        float gX = cellBaseX - (grhW - 32) / 2.0f;
        float gY = cellBaseY - (grhH - 32);

        // Adjust position relative to the content's bounding box origin (contentMinX,
        // contentMinY)
        float relativeX = gX - contentMinX;
        float relativeY = gY - contentMinY;

        // Scale and offset to the ImGui drawing area
        float finalX = drawOriginX + (relativeX * scale);
        float finalY = drawOriginY + (relativeY * scale);

        float drawW = grhW * scale;
        float drawH = grhH * scale;

        float u0 = data.getsX() / (float) tex.getTex_width();
        float v0 = (data.getsY() + data.getPixelHeight()) / (float) tex.getTex_height();
        float u1 = (data.getsX() + data.getPixelWidth()) / (float) tex.getTex_width();
        float v1 = data.getsY() / (float) tex.getTex_height();

        ImGui.setCursorPos(finalX, finalY);
        ImGui.image(tex.getId(), drawW, drawH, u0, v1, u1, v0);
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
