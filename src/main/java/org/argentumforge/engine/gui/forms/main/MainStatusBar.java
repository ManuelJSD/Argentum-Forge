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
import org.argentumforge.engine.gui.forms.Form;

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
            String fpsText = Time.FPS + " " + I18n.INSTANCE.get("status.fps");
            String zoomText = I18n.INSTANCE.get("status.zoom") + ": " + (int) (Camera.getZoomScale() * 100) + "%";

            // Coordenadas del Usuario
            int userX = User.INSTANCE.getUserPos().getX();
            int userY = User.INSTANCE.getUserPos().getY();
            String userPosText = I18n.INSTANCE.get("status.user") + ": " + userX + ", " + userY;

            // Coordenadas del Mouse
            int mx = (int) MouseListener.getX() - Camera.POS_SCREEN_X;
            int my = (int) MouseListener.getY() - Camera.POS_SCREEN_Y;
            int tileMouseX = EditorInputManager.getTileMouseX(mx);
            int tileMouseY = EditorInputManager.getTileMouseY(my);
            String mousePosText = I18n.INSTANCE.get("status.mouse") + ": " + tileMouseX + ", " + tileMouseY;

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
                GithubReleaseChecker.ReleaseInfo release = GithubReleaseChecker.getLatestRelease();
                String tagName = (release != null) ? release.tagName : "v?";
                String updateText = String.format(I18n.INSTANCE.get("status.update"), tagName);

                float textWidth = ImGui.calcTextSize(updateText).x;
                float spacing = 10.0f;
                float textPos = cursorX - textWidth - spacing;

                // Pulsing effect: Alpha oscillates between 0.6 and 1.0
                double time = ImGui.getTime();
                float alpha = (float) (Math.abs(Math.sin(time * 3.0)) * 0.4f + 0.6f);

                ImGui.sameLine(textPos);
                ImGui.textColored(1.0f, 0.85f, 0.0f, alpha, updateText);

                if (ImGui.isItemClicked()) {
                    if (release != null) {
                        try {
                            Form.openURL(release.htmlUrl);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                if (ImGui.isItemHovered()) {
                    ImGui.setMouseCursor(imgui.flag.ImGuiMouseCursor.Hand);
                    String tooltip = I18n.INSTANCE.get("update.available_short");
                    if (release != null)
                        tooltip += "\n" + release.name;

                    ImGui.setTooltip(tooltip);
                }
            }

            // Draw version text at its absolute calculated position
            ImGui.sameLine(cursorX);
            ImGui.textDisabled(versionText);

        }
        ImGui.end();
        ImGui.popStyleColor();
    }
}
