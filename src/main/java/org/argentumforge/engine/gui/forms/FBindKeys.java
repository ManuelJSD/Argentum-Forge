package org.argentumforge.engine.gui.forms;

import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import org.argentumforge.engine.game.models.Key;
import org.argentumforge.engine.listeners.KeyHandler;

import static org.argentumforge.engine.audio.Sound.SND_CLICK;
import static org.argentumforge.engine.audio.Sound.playSound;
import static org.lwjgl.glfw.GLFW.*;

public class FBindKeys extends Form {

    public FBindKeys() {
        // No background image needed anymore
    }

    private String getKeyName(int key) {
        int scancode = glfwGetKeyScancode(key);
        String keyName = glfwGetKeyName(key, scancode);

        if (keyName == null) {
            return switch (key) {
                // Teclas especiales.
                case GLFW_KEY_SPACE -> "ESPACIO";
                case GLFW_KEY_ENTER -> "ENTER";
                case GLFW_KEY_LEFT_SHIFT -> "SHIFT IZQ";
                case GLFW_KEY_RIGHT_SHIFT -> "SHIFT DER";
                case GLFW_KEY_ESCAPE -> "ESC";
                case GLFW_KEY_END -> "FIN";
                case GLFW_KEY_TAB -> "TAB";
                case GLFW_KEY_LEFT_CONTROL -> "CTRL IZQ";
                case GLFW_KEY_RIGHT_CONTROL -> "CTRL DER";
                case GLFW_KEY_LEFT_ALT -> "ALT IZQ";
                case GLFW_KEY_RIGHT_ALT -> "ALT DER";
                case GLFW_KEY_DELETE -> "SUPRIMIR";

                // F1-F12
                case GLFW_KEY_F1 -> "F1";
                case GLFW_KEY_F2 -> "F2";
                case GLFW_KEY_F3 -> "F3";
                case GLFW_KEY_F4 -> "F4";
                case GLFW_KEY_F5 -> "F5";
                case GLFW_KEY_F6 -> "F6";
                case GLFW_KEY_F7 -> "F7";
                case GLFW_KEY_F8 -> "F8";
                case GLFW_KEY_F9 -> "F9";
                case GLFW_KEY_F10 -> "F10";
                case GLFW_KEY_F11 -> "F11";
                case GLFW_KEY_F12 -> "F12";

                // tecla inreconocible
                default -> "???";
            };
        }

        return keyName;
    }

    @Override
    public void render() {
        ImGui.setNextWindowFocus();
        ImGui.setNextWindowSize(400, 400, ImGuiCond.Always);

        if (ImGui.begin("Configuración de Teclas", ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoResize)) {

            // Use columns to separate Labels (Left) and Buttons (Right)
            ImGui.columns(2, "BindKeysCols", false); // false = no border resize

            // Set fixed width for the second column (Buttons) roughly 140px
            // The first column takes the remaining space
            float windowWidth = ImGui.getWindowWidth();
            ImGui.setColumnWidth(0, windowWidth - 140);

            renderGroupHeader("Movimiento");
            renderKeyBindRow("Arriba", Key.UP);
            renderKeyBindRow("Abajo", Key.DOWN);
            renderKeyBindRow("Izquierda", Key.LEFT);
            renderKeyBindRow("Derecha", Key.RIGHT);
            ImGui.dummy(0, 10);

            renderGroupHeader("Opciones Personales");
            renderKeyBindRow("Musica", Key.TOGGLE_MUSIC);
            renderKeyBindRow("Sonido", Key.TOGGLE_SOUND);
            ImGui.dummy(0, 10);

            renderGroupHeader("Otras Teclas");
            renderKeyBindRow("Capturar Pantalla", Key.TAKE_SCREENSHOT);
            renderKeyBindRow("Mostrar Opciones", Key.SHOW_OPTIONS);
            renderKeyBindRow("Mostrar Debug", Key.DEBUG_SHOW);
            renderKeyBindRow("Modo Caminata", Key.TOGGLE_WALKING_MODE);
            renderKeyBindRow("Salir", Key.EXIT_GAME);

            ImGui.columns(1); // End columns

            ImGui.separator();
            ImGui.dummy(0, 10);

            // Bottom buttons centered
            float buttonWidth = 140;
            float spacing = 20;
            float totalWidth = (buttonWidth * 2) + spacing;

            ImGui.setCursorPosX((windowWidth - totalWidth) / 2);

            if (ImGui.button("Cargar Default", buttonWidth, 30)) {
                buttonDefault();
            }

            ImGui.sameLine();

            if (ImGui.button("Guardar y Salir", buttonWidth, 30)) {
                buttonSave();
            }

            ImGui.end();
        }
    }

    private void renderGroupHeader(String title) {
        ImGui.textColored(1.0f, 0.84f, 0.0f, 1.0f, title); // Gold color
        ImGui.separator();
    }

    private void renderKeyBindRow(String label, Key key) {
        ImGui.pushID(label);

        // Column 0: Label
        ImGui.alignTextToFramePadding();
        ImGui.text(label);

        ImGui.nextColumn();

        // Column 1: Button
        float buttonWidth = ImGui.getColumnWidth() - 10; // Fill column with small padding

        String actual = getKeyName(key.getKeyCode()).toUpperCase();

        if (key.getPreparedToBind()) {
            actual = "PRES. TECLA";
        }

        if (ImGui.button(actual, buttonWidth, 0)) {
            if (key.getPreparedToBind()) {
                key.setPreparedToBind(false);
            } else {
                if (!Key.checkIsBinding()) {
                    key.setPreparedToBind(true);
                }
            }
        }

        ImGui.nextColumn(); // Go back to column 0 for next row

        ImGui.popID();
    }

    private void buttonDefault() {
        playSound(SND_CLICK);

        Key.loadDefaultKeys();
        KeyHandler.updateMovementKeys();
        close();
    }

    private void buttonSave() {
        playSound(SND_CLICK);

        Key.saveKeys();
        close();
    }
}
