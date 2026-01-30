package org.argentumforge.engine.gui.forms;

import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiTreeNodeFlags;
import org.argentumforge.engine.game.models.Key;
import org.argentumforge.engine.listeners.KeyHandler;

import org.argentumforge.engine.i18n.I18n;

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
                default -> "KEY " + key;
            };
        }

        return keyName;
    }

    @Override
    public void render() {
        ImGui.setNextWindowFocus();
        ImGui.setNextWindowSize(500, 600, ImGuiCond.FirstUseEver);

        if (ImGui.begin(I18n.INSTANCE.get("options.keys.title"),
                ImGuiWindowFlags.NoCollapse)) {

            // Calculate height for the child window (leave space for bottom buttons)
            // Calculate height for the child window (leave space for bottom buttons)
            // If there is an error message, we need more space in the footer
            float footerHeight = (KeyHandler.bindError != null) ? 80 : 50;
            float childHeight = ImGui.getWindowHeight() - footerHeight - ImGui.getCursorPosY() - 10;

            if (ImGui.beginChild("KeyList", 0, childHeight, true)) {

                // MOVEMENT
                if (ImGui.collapsingHeader(I18n.INSTANCE.get("options.keys.movement"),
                        ImGuiTreeNodeFlags.DefaultOpen)) {
                    ImGui.columns(2, "BindKeysCols_Movement", false);
                    ImGui.setColumnWidth(0, ImGui.getWindowWidth() - 160);

                    renderKeyBindRow(I18n.INSTANCE.get("options.keys.up"), Key.UP);
                    renderKeyBindRow(I18n.INSTANCE.get("options.keys.down"), Key.DOWN);
                    renderKeyBindRow(I18n.INSTANCE.get("options.keys.left"), Key.LEFT);
                    renderKeyBindRow(I18n.INSTANCE.get("options.keys.right"), Key.RIGHT);

                    ImGui.columns(1); // End columns
                    ImGui.dummy(0, 5);
                }

                // OTHER
                if (ImGui.collapsingHeader(I18n.INSTANCE.get("options.keys.other"), ImGuiTreeNodeFlags.DefaultOpen)) {
                    ImGui.columns(2, "BindKeysCols_Other", false);
                    ImGui.setColumnWidth(0, ImGui.getWindowWidth() - 160);

                    renderKeyBindRow(I18n.INSTANCE.get("options.keys.screenshot"), Key.TAKE_SCREENSHOT);
                    renderKeyBindRow(I18n.INSTANCE.get("options.keys.showOptions"), Key.SHOW_OPTIONS);
                    renderKeyBindRow(I18n.INSTANCE.get("options.keys.showDebug"), Key.DEBUG_SHOW);
                    renderKeyBindRow(I18n.INSTANCE.get("options.keys.walkMode"), Key.TOGGLE_WALKING_MODE);
                    renderKeyBindRow(I18n.INSTANCE.get("options.keys.toggleGrid"), Key.TOGGLE_GRID);
                    renderKeyBindRow(I18n.INSTANCE.get("options.keys.photoMode"), Key.TOGGLE_PHOTO_MODE);
                    renderKeyBindRow(I18n.INSTANCE.get("options.keys.mapProperties"), Key.MAP_PROPERTIES);

                    ImGui.columns(1);
                    ImGui.dummy(0, 5);
                }

                // SELECTION
                if (ImGui.collapsingHeader(I18n.INSTANCE.get("options.keys.selectionTools"),
                        ImGuiTreeNodeFlags.DefaultOpen)) {
                    ImGui.columns(2, "BindKeysCols_Sel", false);
                    ImGui.setColumnWidth(0, ImGui.getWindowWidth() - 160);

                    renderKeyBindRow(I18n.INSTANCE.get("options.keys.multiSelect"), Key.MULTI_SELECT);

                    ImGui.columns(1);
                    ImGui.dummy(0, 5);
                }

                // TOOLS
                if (ImGui.collapsingHeader(I18n.INSTANCE.get("options.keys.tools"), ImGuiTreeNodeFlags.DefaultOpen)) {
                    ImGui.columns(2, "BindKeysCols_Tools", false);
                    ImGui.setColumnWidth(0, ImGui.getWindowWidth() - 160);

                    renderKeyBindRow(I18n.INSTANCE.get("options.keys.brush"), Key.TOOL_BRUSH);
                    renderKeyBindRow(I18n.INSTANCE.get("options.keys.bucket"), Key.TOOL_BUCKET);
                    renderKeyBindRow(I18n.INSTANCE.get("options.keys.eraser"), Key.TOOL_ERASER);
                    renderKeyBindRow(I18n.INSTANCE.get("options.keys.picker"), Key.TOOL_PICK);
                    renderKeyBindRow(I18n.INSTANCE.get("options.keys.magicWand"), Key.TOOL_MAGIC_WAND);

                    ImGui.columns(1);
                    ImGui.dummy(0, 5);
                }

                // EXIT
                ImGui.separator();
                ImGui.dummy(0, 5);
                ImGui.columns(2, "BindKeysCols_Exit", false);
                ImGui.setColumnWidth(0, ImGui.getWindowWidth() - 160);
                renderKeyBindRow(I18n.INSTANCE.get("options.keys.exit"), Key.EXIT_GAME);
                ImGui.columns(1);

                ImGui.endChild();
            }

            ImGui.separator();
            ImGui.dummy(0, 10);

            // Bottom buttons centered
            float buttonWidth = 110;
            float spacing = 10;
            float totalWidth = (buttonWidth * 3) + (spacing * 2);
            float windowWidth = ImGui.getWindowWidth();

            // Error Display
            if (KeyHandler.bindError != null) {
                // Center text
                float textWidth = ImGui.calcTextSize(KeyHandler.bindError).x;
                ImGui.setCursorPosX((windowWidth - textWidth) / 2);
                ImGui.textColored(1.0f, 0.2f, 0.2f, 1.0f, KeyHandler.bindError);
                ImGui.dummy(0, 5); // spacing
            }

            ImGui.setCursorPosX((windowWidth - totalWidth) / 2);

            if (ImGui.button(I18n.INSTANCE.get("options.keys.loadDefault"), buttonWidth, 30)) {
                buttonDefault();
            }

            ImGui.sameLine();

            if (ImGui.button("Guardar", buttonWidth, 30)) {
                Key.saveKeys();
                org.argentumforge.engine.game.console.Console.INSTANCE.addMsgToConsole(
                        "Teclas guardadas.",
                        org.argentumforge.engine.game.console.FontStyle.REGULAR,
                        new org.argentumforge.engine.renderer.RGBColor(0, 1, 0));
            }

            ImGui.sameLine();

            if (ImGui.button("Salir", buttonWidth, 30)) {
                close();
            }

            ImGui.end();
        }
    }

    private void renderKeyBindRow(String label, Key key) {
        ImGui.pushID(label);

        // Column 0: Label
        ImGui.alignTextToFramePadding();
        ImGui.text(label);

        ImGui.nextColumn();

        // Column 1: Button
        float buttonWidth = ImGui.getColumnWidth() - 10; // Fill column with small padding

        // Ensure we always get the LATEST key code from Key map
        String actual = getKeyName(key.getKeyCode()).toUpperCase();

        // Store initial state to ensure Push/Pop symmetry even if state changes
        // mid-frame
        boolean isBinding = key.getPreparedToBind();

        if (isBinding) {
            actual = I18n.INSTANCE.get("options.keys.pressKey");
            // Push Style Color for "Waiting for input" state (Gold/Yellow)
            ImGui.pushStyleColor(ImGuiCol.Button, 0.8f, 0.6f, 0.1f, 1.0f);
            ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.9f, 0.7f, 0.2f, 1.0f);
            ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0.7f, 0.5f, 0.0f, 1.0f);
        }

        // Use ### to ensure the ID is stable but let the label change?
        String buttonId = actual + "###" + label + "_btn";

        if (ImGui.button(buttonId, buttonWidth, 0)) {
            if (key.getPreparedToBind()) {
                key.setPreparedToBind(false);
            } else {
                if (!Key.checkIsBinding()) {
                    key.setPreparedToBind(true);
                    KeyHandler.bindError = null; // Clear any previous error when starting new bind
                }
            }
        }

        if (isBinding) {
            ImGui.popStyleColor(3); // Pop the 3 pushed colors
        }

        ImGui.nextColumn(); // Go back to column 0 for next row

        ImGui.popID();
    }

    private void buttonDefault() {

        Key.loadDefaultKeys();
        KeyHandler.updateMovementKeys();
        close();
    }
}
