package org.argentumforge.engine.gui.forms.main;

import imgui.ImGui;
import imgui.ImGuiViewport;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiWindowFlags;
import org.argentumforge.engine.game.User;
import org.argentumforge.engine.listeners.EditorInputManager;
import org.argentumforge.engine.listeners.MouseListener;
import org.argentumforge.engine.scenes.Camera;
import org.argentumforge.engine.utils.Time;

/**
 * Componente para renderizar la barra de estado inferior.
 */
public class MainStatusBar {

    private static final float STATUS_BAR_HEIGHT = 30.0f;

    public void render() {
        // Altura de la barra
        float statusHeight = STATUS_BAR_HEIGHT;
        ImGuiViewport viewport = ImGui.getMainViewport();
        // Posición en la parte inferior de la ventana
        ImGui.setNextWindowPos(0, viewport.getSizeY() - statusHeight);
        // Ancho completo
        ImGui.setNextWindowSize(viewport.getSizeX(), statusHeight);

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
            ImGui.sameLine(viewport.getSizeX() - versionWidth - 20);
            ImGui.textDisabled(versionText);

        }
        ImGui.end();
        ImGui.popStyleColor();
    }
}
