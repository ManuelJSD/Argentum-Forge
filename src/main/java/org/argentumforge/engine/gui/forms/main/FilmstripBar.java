package org.argentumforge.engine.gui.forms.main;

import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import org.argentumforge.engine.gui.Theme;
import org.argentumforge.engine.utils.GameData;
import org.argentumforge.engine.utils.MapManager;
import org.argentumforge.engine.game.User;

import java.io.BufferedReader;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/**
 * Barra de navegación tipo "Cinta de Película" (Filmstrip).
 * Muestra accesos directos a mapas adyacentes (anterior/siguiente).
 */
public class FilmstripBar {

    private static final int BUTTON_SIZE = 40;
    private static final int BUTTON_PADDING = 4;
    private static final int VISIBLE_BUTTONS = 13; // Cantidad visible por defecto

    // Cache simple para metadatos de mapas (Nombre, Tipo)
    private final Map<Integer, MapInfo> mapInfoCache = new HashMap<>();
    private int scrollOffset = 0;
    private int lastCenterMapId = -1;
    private boolean isVisible = true;

    private static class MapInfo {
        String name;
        String type; // "CIUDAD", "DUNGEON", etc.
        boolean exists;

        MapInfo(String name, String type, boolean exists) {
            this.name = name;
            this.type = type;
            this.exists = exists;
        }
    }

    public void render() {
        if (GameData.getActiveContext() == null)
            return;

        int currentMapId = getCurrentMapId();
        if (currentMapId <= 0)
            return;

        imgui.ImGuiViewport viewport = ImGui.getMainViewport();
        float windowWidth = viewport.getWorkSizeX();
        float windowHeight = viewport.getWorkSizeY();

        // --- 1. Botón Flotante (Centrado Abajo) ---
        // User request: "mismo lugar que la del ojo cerrado" -> Bottom Center.
        float btnSize = 30.0f;

        ImGui.setNextWindowPos(
                viewport.getWorkPosX() + (windowWidth - btnSize) / 2,
                viewport.getWorkPosY() + windowHeight - btnSize - 5);
        ImGui.setNextWindowSize(btnSize, btnSize);

        int flagsBtn = ImGuiWindowFlags.NoDecoration | ImGuiWindowFlags.NoBackground | ImGuiWindowFlags.NoSavedSettings
                | ImGuiWindowFlags.NoFocusOnAppearing | ImGuiWindowFlags.NoInputs;
        flagsBtn &= ~ImGuiWindowFlags.NoInputs;

        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0, 0);
        if (ImGui.begin("FilmstripToggle", flagsBtn)) {
            // Fondo circular para el botón (opcional, igual que antes)
            // Si está oculto, no tenía fondo antes, solo el icono. Si está visible,
            // tampoco.
            // Mantendremos consistencia: Solo el icono flotante.

            if (ImGui.invisibleButton("##toggleFilmstrip", btnSize, btnSize)) {
                isVisible = !isVisible;
            }

            boolean hovered = ImGui.isItemHovered();
            // Icono: Ojo Abierto (Visible, click para cerrar) / Ojo Cerrado (Oculto, click
            // para abrir)
            drawEyeIcon(ImGui.getItemRectMinX(), ImGui.getItemRectMinY(), btnSize, isVisible, hovered);

            if (hovered)
                ImGui.setTooltip(isVisible ? "Ocultar Cinta" : "Mostrar Cinta");
        }
        ImGui.end();
        ImGui.popStyleVar();

        // --- 2. Cinta de Mapas (Solo si es visible) ---
        if (!isVisible)
            return;

        if (currentMapId != lastCenterMapId) {
            scrollOffset = 0;
            lastCenterMapId = currentMapId;
        }

        float barHeight = 60.0f;

        // Cinta encima del botón (Botón está en -5 aprox, Cinta en -40 - 60 = -100?)
        // Ajustamos: Botón en Bottom-5. Cinta en Bottom-40.
        ImGui.setNextWindowPos(viewport.getWorkPosX(), viewport.getWorkPosY() + windowHeight - barHeight - 40);
        ImGui.setNextWindowSize(windowWidth, barHeight);

        int flags = ImGuiWindowFlags.NoDecoration | ImGuiWindowFlags.NoInputs | ImGuiWindowFlags.NoBackground
                | ImGuiWindowFlags.NoSavedSettings | ImGuiWindowFlags.NoFocusOnAppearing;
        flags &= ~ImGuiWindowFlags.NoInputs;

        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0, 0);

        if (ImGui.begin("FilmstripOverlay", flags)) {

            // Layout: [<] [Btns...] [>]
            float navBtnWidth = 20;
            float stripWidth = (BUTTON_SIZE + BUTTON_PADDING) * VISIBLE_BUTTONS + BUTTON_PADDING;
            float totalWidth = stripWidth + (navBtnWidth * 2) + 10;

            float startX = (windowWidth - totalWidth) / 2.0f;
            if (startX < 0)
                startX = 0;

            ImGui.setCursorPosX(startX);

            // Fondo
            ImGui.getWindowDrawList().addRectFilled(
                    ImGui.getCursorScreenPosX(),
                    ImGui.getCursorScreenPosY(),
                    ImGui.getCursorScreenPosX() + totalWidth,
                    ImGui.getCursorScreenPosY() + BUTTON_SIZE + 10,
                    Theme.rgba(20, 20, 20, 220),
                    10.0f);

            ImGui.setCursorPosX(startX + 5);
            ImGui.setCursorPosY(ImGui.getCursorPosY() + 5);

            // Botón Scroll Izquierda
            if (ImGui.button("<", navBtnWidth, BUTTON_SIZE)) {
                scrollOffset--;
            }
            ImGui.sameLine();

            // Renderizar tira de botones
            int range = VISIBLE_BUTTONS / 2;
            int startMapId = currentMapId - range + scrollOffset;
            int endMapId = currentMapId + range + scrollOffset;

            for (int mapId = startMapId; mapId <= endMapId; mapId++) {
                boolean isCurrent = (mapId == currentMapId);
                renderMapButton(mapId, isCurrent);
                ImGui.sameLine(0, BUTTON_PADDING);
            }

            // Botón Scroll Derecha
            if (ImGui.button(">", navBtnWidth, BUTTON_SIZE)) {
                scrollOffset++;
            }
        }
        ImGui.end();
        ImGui.popStyleVar();
    }

    private void drawEyeIcon(float x, float y, float size, boolean isOpen, boolean hovered) {
        imgui.ImDrawList dl = ImGui.getWindowDrawList();
        int color = hovered ? Theme.rgba(255, 255, 255, 255) : Theme.rgba(180, 180, 180, 255);
        float cx = x + size / 2;
        float cy = y + size / 2;
        float halfW = size * 0.35f;
        float h = size * 0.2f;

        if (isOpen) {
            // Ojo Abierto
            // Párpado superior
            dl.addBezierQuadratic(cx - halfW, cy, cx, cy - h * 1.5f, cx + halfW, cy, color, 2.0f);
            // Párpado inferior
            dl.addBezierQuadratic(cx - halfW, cy, cx, cy + h * 1.5f, cx + halfW, cy, color, 2.0f);
            // Pupila
            dl.addCircleFilled(cx, cy, h * 0.6f, color);
        } else {
            // Ojo Cerrado
            // Párpado superior (curva hacia abajo)
            dl.addBezierQuadratic(cx - halfW, cy + h * 0.5f, cx, cy + h * 1.5f, cx + halfW, cy + h * 0.5f, color, 2.0f);

            // Pestañas (3 lineas)
            // Izq
            dl.addLine(cx - halfW * 0.5f, cy + h, cx - halfW * 0.7f, cy + h * 2.0f, color, 1.5f);
            // Centro
            dl.addLine(cx, cy + h * 1.5f, cx, cy + h * 2.5f, color, 1.5f);
            // Der
            dl.addLine(cx + halfW * 0.5f, cy + h, cx + halfW * 0.7f, cy + h * 2.0f, color, 1.5f);
        }
    }

    private void renderMapButton(int mapId, boolean isCurrent) {
        if (mapId <= 0) {
            ImGui.invisibleButton("##empty" + mapId, BUTTON_SIZE, BUTTON_SIZE);
            return;
        }

        MapInfo info = getMapInfo(mapId);

        ImGui.pushID(mapId);

        // Colores base
        if (isCurrent) {
            ImGui.pushStyleColor(ImGuiCol.Button, Theme.COLOR_ACCENT);
            ImGui.pushStyleColor(ImGuiCol.Text, Theme.rgba(255, 255, 255, 255));
        } else if (info.exists) {
            ImGui.pushStyleColor(ImGuiCol.Button, Theme.rgba(60, 60, 60, 255));
            ImGui.pushStyleColor(ImGuiCol.Text, Theme.rgba(200, 200, 200, 255));
        } else {
            ImGui.pushStyleColor(ImGuiCol.Button, Theme.rgba(30, 30, 30, 100));
            ImGui.pushStyleColor(ImGuiCol.Text, Theme.rgba(100, 100, 100, 100));
        }

        if (ImGui.button(String.valueOf(mapId), BUTTON_SIZE, BUTTON_SIZE)) {
            if (info.exists && !isCurrent) {
                MapManager.loadMapAsync(MapManager.resolveMapPath(mapId), null);
            }
        }

        // Efectos Visuales
        if (isCurrent) {
            // Neon Glow / Borde Brillante
            ImGui.getWindowDrawList().addRect(
                    ImGui.getItemRectMinX() - 1, ImGui.getItemRectMinY() - 1,
                    ImGui.getItemRectMaxX() + 1, ImGui.getItemRectMaxY() + 1,
                    Theme.rgba(100, 255, 100, 255), // Verde Neon
                    4.0f, 0, 3.0f // Grosor 3
            );
        }

        // Indicador de Tipo (Punto de color)
        if (info.exists) {
            int typeColor = getTypeColor(info.type);
            ImGui.getWindowDrawList().addCircleFilled(
                    ImGui.getItemRectMaxX() - 6,
                    ImGui.getItemRectMinY() + 6,
                    3.0f,
                    typeColor);
        }

        ImGui.popStyleColor(2);

        // Tooltip con Nombre
        if (ImGui.isItemHovered() && info.exists) {
            String tooltip = "Mapa " + mapId;
            if (info.name != null && !info.name.isEmpty()) {
                tooltip += "\n" + info.name;
            }
            if (info.type != null && !info.type.isEmpty()) {
                tooltip += "\n(" + info.type + ")";
            }
            ImGui.setTooltip(tooltip);
        }

        ImGui.popID();
    }

    private int getTypeColor(String type) {
        if (type == null)
            return Theme.rgba(150, 150, 150, 255); // Gris
        String t = type.toUpperCase();
        if (t.contains("CIUDAD") || t.contains("CITY"))
            return Theme.rgba(100, 150, 255, 255); // Azul
        if (t.contains("BOSQUE") || t.contains("FOREST"))
            return Theme.rgba(50, 200, 50, 255); // Verde
        if (t.contains("DUNGEON") || t.contains("CALABOZO") || t.contains("CUEVA"))
            return Theme.rgba(200, 50, 50, 255); // Rojo
        if (t.contains("NIEVE") || t.contains("POLAR"))
            return Theme.rgba(200, 200, 255, 255); // Blanco
        if (t.contains("DESIERTO"))
            return Theme.rgba(220, 200, 100, 255); // Amarillo
        return Theme.rgba(150, 150, 150, 255);
    }

    private MapInfo getMapInfo(int mapId) {
        if (mapInfoCache.containsKey(mapId)) {
            return mapInfoCache.get(mapId);
        }

        String path = MapManager.resolveMapPath(mapId);
        if (path == null) {
            MapInfo info = new MapInfo(null, null, false);
            mapInfoCache.put(mapId, info);
            return info;
        }

        // Leer .dat ligero
        String name = "";
        String type = ""; // Zona o Terreno

        // Asumimos estructura standard: mismo nombre base pero .dat
        String datPath = path.substring(0, path.lastIndexOf('.')) + ".dat";
        File datFile = new File(datPath);

        if (datFile.exists()) {
            try (BufferedReader br = Files.newBufferedReader(datFile.toPath(), StandardCharsets.ISO_8859_1)) {
                String line;
                while ((line = br.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("Name=")) {
                        name = trimmed.substring(5).trim();
                    } else if (trimmed.startsWith("Terreno=")) {
                        // Preferimos terreno como tipo visual
                        type = trimmed.substring(8).trim();
                    } else if (trimmed.startsWith("Zona=") && type.isEmpty()) {
                        // Fallback a zona
                        type = trimmed.substring(5).trim();
                    }
                }
            } catch (Exception e) {
                // Ignore error, just incomplete info
            }
        }

        MapInfo info = new MapInfo(name, type, true);
        mapInfoCache.put(mapId, info);
        return info;
    }

    private int getCurrentMapId() {
        org.argentumforge.engine.utils.MapContext ctx = GameData.getActiveContext();
        if (ctx != null) {
            String path = ctx.getFilePath();
            if (path != null) {
                try {
                    File f = new File(path);
                    String name = f.getName();
                    String numberStr = name.replaceAll("\\D+", "");
                    if (!numberStr.isEmpty()) {
                        return Integer.parseInt(numberStr);
                    }
                } catch (Exception e) {
                }
            }
        }
        return User.INSTANCE.getUserMap();
    }
}
