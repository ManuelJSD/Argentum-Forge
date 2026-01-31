package org.argentumforge.engine.gui.forms.main;

import imgui.ImGui;
import imgui.ImGuiViewport;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiWindowFlags;
import org.argentumforge.engine.game.User;
import org.argentumforge.engine.i18n.I18n;
import org.argentumforge.engine.listeners.EditorInputManager;
import org.argentumforge.engine.listeners.MouseListener;
import org.argentumforge.engine.scenes.Camera;
import org.argentumforge.engine.utils.GithubReleaseChecker;
import org.argentumforge.engine.utils.Time;

/**
 * Componente para renderizar la barra de estado inferior.
 */
public class MainStatusBar {

    public void render() {
        ImGuiViewport viewport = ImGui.getMainViewport();
        float statusHeight = 25.0f;

        // Anclar al fondo del área de trabajo (abajo)
        ImGui.setNextWindowPos(viewport.getWorkPosX(), viewport.getWorkPosY() + viewport.getWorkSizeY() - statusHeight);
        ImGui.setNextWindowSize(viewport.getWorkSizeX(), statusHeight);
        ImGui.setNextWindowViewport(viewport.getID());

        // Estilo: Fondo oscuro, sin bordes ni decoraciones
        ImGui.pushStyleColor(ImGuiCol.WindowBg, 0.1f, 0.1f, 0.1f, 0.9f);

        if (ImGui.begin("StatusBar",
                ImGuiWindowFlags.NoDecoration | ImGuiWindowFlags.NoInputs
                        | ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoSavedSettings
                        | ImGuiWindowFlags.NoBringToFrontOnFocus | ImGuiWindowFlags.NoFocusOnAppearing)) {

            // Datos a mostrar
            String fpsText = Time.FPS + " FPS";
            String zoomText = "Zoom: " + (int) (Camera.getZoomScale() * 100) + "%";

            // Coordenadas del Usuario
            int userX = User.INSTANCE.getUserPos().getX();
            int userY = User.INSTANCE.getUserPos().getY();
            String userPosText = "User: " + userX + ", " + userY;

            // Coordenadas del Mouse
            int mx = (int) MouseListener.getX() - Camera.POS_SCREEN_X;
            int my = (int) MouseListener.getY() - Camera.POS_SCREEN_Y;
            int tileMouseX = EditorInputManager.getTileMouseX(mx);
            int tileMouseY = EditorInputManager.getTileMouseY(my);
            String mousePosText = "Mouse: " + tileMouseX + ", " + tileMouseY;

            // Renderizado con espaciado
            ImGui.text(fpsText);
            ImGui.sameLine(0, 20); // Spacing
            ImGui.text("|");
            ImGui.sameLine(0, 20);
            ImGui.text(zoomText);
            ImGui.sameLine(0, 20);
            ImGui.text("|");

            // Highlight User Pos
            ImGui.sameLine(0, 20);
            ImGui.textColored(0.2f, 0.8f, 1.0f, 1.0f, userPosText);

            // Highlight Mouse Pos if valid
            ImGui.sameLine(0, 20);
            ImGui.text("|");
            ImGui.sameLine(0, 20);

            boolean validDetails = tileMouseX >= Camera.XMinMapSize && tileMouseX <= Camera.XMaxMapSize &&
                    tileMouseY >= Camera.YMinMapSize && tileMouseY <= Camera.YMaxMapSize;

            if (validDetails) {
                ImGui.textColored(1.0f, 0.8f, 0.2f, 1.0f, mousePosText);
            } else {
                ImGui.textDisabled(mousePosText);
            }

            // Version info at the right
            String versionText = "v" + org.argentumforge.engine.Engine.VERSION;
            float versionWidth = ImGui.calcTextSize(versionText).x;
            float rightMargin = 20.0f;
            float cursorX = viewport.getSizeX() - versionWidth - rightMargin;

            if (GithubReleaseChecker.isUpdateAvailable()) {
                String icon = "(!)";
                float iconWidth = ImGui.calcTextSize(icon).x;
                float spacing = 5.0f;

                float iconPos = cursorX - iconWidth - spacing;

                ImGui.sameLine(iconPos);
                ImGui.textColored(1.0f, 0.8f, 0.0f, 1.0f, icon);

                if (ImGui.isItemHovered()) {
                    GithubReleaseChecker.ReleaseInfo release = GithubReleaseChecker.getLatestRelease();
                    String tooltip = I18n.INSTANCE.get("update.available_short");
                    if (release != null)
                        tooltip += ": " + release.tagName;
                    tooltip += "\n" + I18n.INSTANCE.get("update.click_to_download");

                    ImGui.setTooltip(tooltip);

                    if (ImGui.isMouseClicked(0) && release != null) {
                        org.argentumforge.engine.gui.forms.Form.openURL(release.htmlUrl);
                    }
                }
            } else {
                // Nothing specific if no update
            }

            // Draw version text at its absolute calculated position
            ImGui.sameLine(cursorX);
            ImGui.textDisabled(versionText);

        }
        ImGui.end();
        ImGui.popStyleColor();
    }
}
