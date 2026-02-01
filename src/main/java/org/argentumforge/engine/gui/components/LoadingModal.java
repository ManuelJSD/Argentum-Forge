package org.argentumforge.engine.gui.components;

import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImFloat;

/**
 * A modal dialog that displays a loading spinner and a status message.
 * Blocks interaction with the rest of the application while active.
 */
public class LoadingModal {

    private static LoadingModal instance;
    private boolean visible = false;
    private String message = "Loading...";
    private float spinnerAngle = 0.0f;

    private LoadingModal() {
    }

    public static LoadingModal getInstance() {
        if (instance == null) {
            instance = new LoadingModal();
        }
        return instance;
    }

    public void show(String message) {
        this.message = message;
        this.visible = true;
        ImGui.openPopup("##LoadingModal");
    }

    public void hide() {
        this.visible = false;
    }

    public void render() {
        if (!visible)
            return;

        // Ensure popup is open
        if (!ImGui.isPopupOpen("##LoadingModal")) {
            ImGui.openPopup("##LoadingModal");
        }

        // Center the modal
        float windowWidth = ImGui.getIO().getDisplaySizeX();
        float windowHeight = ImGui.getIO().getDisplaySizeY();
        ImGui.setNextWindowPos(windowWidth / 2.0f, windowHeight / 2.0f, 0, 0.5f, 0.5f);

        int flags = ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoMove
                | ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.AlwaysAutoResize;

        if (ImGui.beginPopupModal("##LoadingModal", flags)) {
            ImGui.dummy(0, 10);

            // Draw Spinner
            renderSpinner(20.0f, 3.0f, 12, ImGui.getColorU32(1.0f, 1.0f, 1.0f, 1.0f));

            ImGui.dummy(0, 10);
            ImGui.text(message);
            ImGui.dummy(0, 10);

            ImGui.endPopup();
        }
    }

    // Custom spinner implementation based on ImGui loading indicators
    private void renderSpinner(float radius, float thickness, int numSegments, int color) {
        float time = (float) ImGui.getTime();
        float windowPosX = ImGui.getCursorScreenPosX();
        float windowPosY = ImGui.getCursorScreenPosY();

        // Advance cursor to make space for spinner
        ImGui.dummy(radius * 2, radius * 2);

        // Center of the spinner
        float centerX = windowPosX + radius;
        float centerY = windowPosY + radius;

        for (int i = 0; i < numSegments; i++) {
            float angle = (float) (i * 2 * Math.PI / numSegments) - time * 5.0f;
            float x = (float) (centerX + Math.cos(angle) * (radius - thickness));
            float y = (float) (centerY + Math.sin(angle) * (radius - thickness));

            // Fade transparency
            float alpha = (float) i / (float) numSegments;
            int segmentColor = (color & 0x00FFFFFF) | ((int) (alpha * 255) << 24);

            ImGui.getWindowDrawList().addCircleFilled(x, y, thickness, segmentColor);
        }
    }

    public boolean isVisible() {
        return visible;
    }
}
