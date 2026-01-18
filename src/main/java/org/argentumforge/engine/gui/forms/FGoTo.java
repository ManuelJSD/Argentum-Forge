package org.argentumforge.engine.gui.forms;

import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImInt;
import org.argentumforge.engine.game.User;
import org.argentumforge.engine.gui.widgets.UIComponents;
import org.argentumforge.engine.i18n.I18n;

public class FGoTo extends Form {

    private final ImInt xVal = new ImInt();
    private final ImInt yVal = new ImInt();

    public FGoTo() {
        // Pre-llenar con la posición actual del usuario
        xVal.set(User.INSTANCE.getUserPos().getX());
        yVal.set(User.INSTANCE.getUserPos().getY());
    }

    @Override
    public void render() {
        // Centrar ventana
        ImGui.setNextWindowPos(
                org.argentumforge.engine.Window.SCREEN_WIDTH / 2.0f - 125,
                org.argentumforge.engine.Window.SCREEN_HEIGHT / 2.0f - 80);
        ImGui.setNextWindowSize(250, 160);

        if (ImGui.begin(I18n.INSTANCE.get("goto.title"), ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoCollapse)) {

            ImGui.dummy(0, 5);

            // Input X
            ImGui.text("X:");
            ImGui.sameLine();
            ImGui.inputInt("##x", xVal);

            // Input Y
            ImGui.text("Y:");
            ImGui.sameLine();
            ImGui.inputInt("##y", yVal);

            ImGui.dummy(0, 15);
            ImGui.separator();
            ImGui.dummy(0, 5);

            // Botones
            if (ImGui.button(I18n.INSTANCE.get("common.go"), 100, 25)) {
                go();
            }

            ImGui.sameLine();

            if (ImGui.button(I18n.INSTANCE.get("common.cancel"), 100, 25)) {
                close();
            }
        }
        ImGui.end();
    }

    private void go() {
        int x = xVal.get();
        int y = yVal.get();

        // El metodo teleport ya valida limites
        User.INSTANCE.teleport(x, y);
        close();
    }
}
