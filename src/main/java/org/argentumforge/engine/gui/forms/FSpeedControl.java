package org.argentumforge.engine.gui.forms;

import imgui.ImGui;
import imgui.type.ImBoolean;
import imgui.flag.ImGuiWindowFlags;
import imgui.flag.ImGuiCond;
import org.argentumforge.engine.i18n.I18n;
import org.argentumforge.engine.game.User;
import static org.argentumforge.engine.utils.GameData.options;

public class FSpeedControl extends Form {

    public FSpeedControl() {
        super();
    }

    @Override
    public void render() {
        // Slightly wider and taller for better spacing
        ImGui.setNextWindowSize(320, 160, ImGuiCond.Always);

        int windowFlags = ImGuiWindowFlags.NoDocking | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoCollapse;

        ImBoolean p_open = new ImBoolean(true);
        if (ImGui.begin(I18n.INSTANCE.get("options.moveSpeed"), p_open, windowFlags)) {

            boolean walking = User.INSTANCE.isWalkingmode();
            int currentSpeed = walking ? options.getMoveSpeedWalk() : options.getMoveSpeedNormal();
            int[] speedPtr = { currentSpeed };
            String label = walking ? I18n.INSTANCE.get("options.moveSpeed.walk")
                    : I18n.INSTANCE.get("options.moveSpeed.normal");
            int minRequest = walking ? 1 : 4;
            int maxRequest = walking ? 16 : 128;
            int defaultSpeed = walking ? 8 : 32;

            // Centered Label
            float windowWidth = ImGui.getWindowWidth();
            float textWidth = ImGui.calcTextSize(label).x;
            ImGui.setCursorPosX((windowWidth - textWidth) / 2);
            ImGui.textColored(0.8f, 0.8f, 1.0f, 1.0f, label);

            ImGui.spacing();

            // Slider and Reset in one row with better alignment
            ImGui.setCursorPosX(20);
            ImGui.pushItemWidth(220);
            if (ImGui.sliderInt("##activeSpeed", speedPtr, minRequest, maxRequest)) {
                if (walking)
                    options.setMoveSpeedWalk(speedPtr[0]);
                else
                    options.setMoveSpeedNormal(speedPtr[0]);
            }
            ImGui.popItemWidth();

            ImGui.sameLine();

            // Nice looking Reset button
            if (ImGui.button(I18n.INSTANCE.get("common.abbr.reset"), 30, 24)) {
                if (walking)
                    options.setMoveSpeedWalk(defaultSpeed);
                else
                    options.setMoveSpeedNormal(defaultSpeed);
            }
            if (ImGui.isItemHovered()) {
                ImGui.setTooltip(I18n.INSTANCE.get("common.reset"));
            }

            ImGui.dummy(0, 15); // More spacing before control buttons

            // Centered Control Buttons with better styling approach (just size/position for
            // now)
            float buttonWidth = 80;
            float buttonHeight = 30;
            float spacing = 20;
            float startX = (windowWidth - (buttonWidth * 2 + spacing)) / 2;

            ImGui.setCursorPosX(startX);
            if (ImGui.button("-", buttonWidth, buttonHeight)) {
                options.decreaseSpeed();
            }

            ImGui.sameLine(startX + buttonWidth + spacing);

            if (ImGui.button("+", buttonWidth, buttonHeight)) {
                options.increaseSpeed();
            }
        }
        ImGui.end();

        if (!p_open.get()) {
            this.close();
        }
    }
}
