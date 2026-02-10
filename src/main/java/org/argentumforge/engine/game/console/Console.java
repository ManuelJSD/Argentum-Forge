package org.argentumforge.engine.game.console;

import imgui.ImFont;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.flag.ImGuiInputTextFlags;
import imgui.type.ImString;
import imgui.ImGuiViewport;
import org.argentumforge.engine.renderer.RGBColor;
import org.argentumforge.engine.game.console.ImGuiFonts;
import org.argentumforge.engine.game.Options;
import java.time.format.DateTimeFormatter;
import java.time.LocalTime;

import java.util.ArrayList;
import java.util.List;

import static org.argentumforge.engine.game.console.FontStyle.*;

/**
 * Clase que implementa una consola de texto para mostrar mensajes al usuario.
 * <p>
 * La consola mantiene un historial de mensajes con un límite máximo, eliminando
 * automáticamente los más antiguos cuando se
 * alcanza dicho límite. Permite personalizar los mensajes con diferentes
 * colores y estilos para categorizar o resaltar la
 * información mostrada al usuario.
 * <p>
 * Esta consola es fundamental para la comunicación unidireccional del sistema
 * hacia el usuario, mostrando eventos importantes,
 * resultados de acciones y otros datos relevantes durante la edición del mapa.
 */

public enum Console {
    INSTANCE;

    public enum MessageType {
        INFO, WARNING, ERROR, COMMAND, CUSTOM
    }

    private static final int MAX_SIZE_DATA = 500;
    private static final int MAX_CHARACTERS_LENGTH = 100; // Increased length for wider consoles
    private final boolean autoScroll;
    private final List<ConsoleData> data;
    private boolean scrollToBottom;
    private final ImString inputBuffer = new ImString(256);
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    private boolean isInputActive = false;
    private boolean reclaimFocus = false;

    public boolean isInputActive() {
        return isInputActive;
    }

    Console() {
        autoScroll = true;
        scrollToBottom = false;
        data = new ArrayList<>();
    }

    /**
     * Helper to add message with specific type.
     */
    public void addMsgToConsole(String text, MessageType type) {
        addMsgToConsole(text, REGULAR, null, type);
    }

    public void addMsgToConsole(String text, FontStyle style, MessageType type) {
        addMsgToConsole(text, style, null, type);
    }

    /**
     * Legacy support for direct color, mapped to CUSTOM type.
     */
    public void addMsgToConsole(String text, FontStyle style, RGBColor color) {
        addMsgToConsole(text, style, color, MessageType.CUSTOM);
    }

    public void addMsgToConsole(String format, FontStyle style, RGBColor color, Object... args) {
        addMsgToConsole(String.format(format, args), style, color, MessageType.CUSTOM);
    }

    private void addMsgToConsole(String text, FontStyle style, RGBColor color, MessageType type) {
        String timestamp = LocalTime.now().format(timeFormatter);

        StringBuilder resultado = new StringBuilder();

        for (String linea : text.split("\n")) {
            String[] palabras = linea.split(" ");
            StringBuilder lineaActual = new StringBuilder();

            for (String palabra : palabras) {
                if (lineaActual.length() + palabra.length() + 1 > MAX_CHARACTERS_LENGTH) {
                    if (palabra.length() > MAX_CHARACTERS_LENGTH) {
                        if (lineaActual.length() > 0) {
                            resultado.append(lineaActual.toString().stripTrailing()).append("\n");
                            lineaActual.setLength(0);
                        }
                        int inicio = 0;
                        while (inicio < palabra.length()) {
                            int fin = Math.min(inicio + MAX_CHARACTERS_LENGTH, palabra.length());
                            resultado.append(palabra, inicio, fin).append("\n");
                            inicio = fin;
                        }
                    } else {
                        resultado.append(lineaActual.toString().stripTrailing()).append("\n");
                        lineaActual = new StringBuilder(palabra + " ");
                    }
                } else {
                    lineaActual.append(palabra).append(" ");
                }
            }

            if (lineaActual.length() > 0) {
                resultado.append(lineaActual.toString().stripTrailing()).append("\n");
            }
        }

        data.add(new ConsoleData(resultado.toString(), color, style, type, timestamp));
        scrollToBottom = true;
    }

    public void clearConsole() {
        data.clear();
        scrollToBottom = true;
    }

    /**
     * Dibuja la consola (esta es una porción de GUI del FMain).
     */
    public void drawConsole() {
        Options options = Options.INSTANCE;

        if (data.size() > MAX_SIZE_DATA)
            clearConsole();

        ImGuiViewport viewport = ImGui.getMainViewport();
        float xPadding = 10.0f;
        float yPadding = 45.0f;

        int width = options.getConsoleWidth();
        int height = options.getConsoleHeight();
        float opacity = options.getConsoleOpacity();
        float fontScale = options.getConsoleFontSize();
        boolean showTimestamps = options.isConsoleShowTimestamps();

        ImGui.setNextWindowPos(viewport.getWorkPosX() + xPadding,
                viewport.getWorkPosY() + viewport.getWorkSizeY() - height - yPadding);
        ImGui.setNextWindowSize(width, height, ImGuiCond.Always);
        ImGui.setNextWindowBgAlpha(opacity);

        // Allow inputs for scrolling and text input
        int flags = ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoResize
                | ImGuiWindowFlags.NoSavedSettings;
        // Removed NoInputs, NoScrollbar, NoScrollWithMouse to allow interaction

        ImGui.begin("console", flags);
        ImGui.setWindowFontScale(fontScale);

        // Calculate height for input bar
        float inputHeight = isInputActive ? ImGui.getTextLineHeightWithSpacing() + 12 : 0;
        float scrollRegionHeight = height - inputHeight - (isInputActive ? 15 : 10);

        ImGui.beginChild("ScrollingRegion", 0, scrollRegionHeight, false, ImGuiWindowFlags.HorizontalScrollbar);

        for (ConsoleData item : data) {
            final ImFont font = switch (item.style) {
                case REGULAR -> ImGuiFonts.fontRegular;
                case BOLD -> ImGuiFonts.fontBold;
                case ITALIC -> ImGuiFonts.fontItalic;
                case BOLD_ITALIC -> ImGuiFonts.fontBoldItalic;
            };

            if (font != null)
                ImGui.pushFont(font);

            // Determine Color
            int colorU32;
            if (item.type == MessageType.CUSTOM && item.customColor != null) {
                colorU32 = ImGui.getColorU32(item.customColor.getRed(), item.customColor.getGreen(),
                        item.customColor.getBlue(), 1f);
            } else {
                float[] c = switch (item.type) {
                    case INFO, CUSTOM -> options.getConsoleColorInfo();
                    case WARNING -> options.getConsoleColorWarning();
                    case ERROR -> options.getConsoleColorError();
                    case COMMAND -> options.getConsoleColorCommand();
                };
                colorU32 = ImGui.getColorU32(c[0], c[1], c[2], c[3]);
            }

            ImGui.pushStyleColor(ImGuiCol.Text, colorU32);

            if (showTimestamps) {
                ImGui.textUnformatted("[" + item.timestamp + "] " + item.consoleText);
            } else {
                ImGui.textUnformatted(item.consoleText);
            }

            ImGui.popStyleColor();
            if (font != null)
                ImGui.popFont();
        }

        if (scrollToBottom || (autoScroll && ImGui.getScrollY() >= ImGui.getScrollMaxY()))
            ImGui.setScrollHereY(1.0f);

        scrollToBottom = false;

        ImGui.endChild();

        // Toggle input with Enter
        if (!isInputActive && ImGui.isKeyPressed(imgui.flag.ImGuiKey.Enter)) {
            isInputActive = true;
            reclaimFocus = true;
        }

        if (isInputActive) {
            ImGui.separator();

            // Force focus on start
            if (reclaimFocus) {
                ImGui.setKeyboardFocusHere(0);
                reclaimFocus = false;
            }

            // Command Input
            ImGui.pushItemWidth(-1); // Full width
            if (ImGui.inputText("##ConsoleInput", inputBuffer,
                    ImGuiInputTextFlags.EnterReturnsTrue | ImGuiInputTextFlags.AutoSelectAll)) {
                String command = inputBuffer.get().trim();
                if (!command.isEmpty()) {
                    addMsgToConsole("> " + command, FontStyle.BOLD, MessageType.COMMAND);
                    ConsoleCommandProcessor.process(command);
                    inputBuffer.set("");
                    scrollToBottom = true;
                }
                isInputActive = false; // Hide after submit
            }

            // Keep focus if window is lost but active
            // Removing stricter check to avoid fighting with user if they click away
            if (ImGui.isItemHovered() || (ImGui.isWindowFocused() && !ImGui.isAnyItemActive())) {
                // Optional: Keep focus?
            }

            // Close on Escape
            if (ImGui.isKeyPressed(imgui.flag.ImGuiKey.Escape)) {
                isInputActive = false;
            }

            ImGui.popItemWidth();
        }

        ImGui.end();
    }

    private record ConsoleData(String consoleText, RGBColor customColor, FontStyle style, MessageType type,
            String timestamp) {
        public ConsoleData {
            if (consoleText == null)
                consoleText = "";
            if (style == null)
                style = REGULAR;
            if (type == null)
                type = MessageType.INFO;
            if (timestamp == null)
                timestamp = "00:00:00";
        }
    }

}
