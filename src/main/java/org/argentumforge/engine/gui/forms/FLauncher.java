package org.argentumforge.engine.gui.forms;

import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import org.argentumforge.engine.Engine;
import org.argentumforge.engine.Window;
import org.argentumforge.engine.game.User;
import org.argentumforge.engine.gui.widgets.ImageButton3State;
import org.argentumforge.engine.i18n.I18n;
import org.argentumforge.engine.utils.GithubReleaseChecker;

import java.io.IOException;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER;

/**
 * Launcher del editor.
 * Maneja la creación de nuevos mapas, carga de existentes y salida.
 */
public final class FLauncher extends Form {

    // Botones de menú principal
    private ImageButton3State btnNuevoMapa;
    private ImageButton3State btnCargarMapa;
    private ImageButton3State btnExit;
    private boolean connectPressed = false;
    private boolean updatePopupShown = false;

    public FLauncher() {
        try {
            this.backgroundImage = loadTexture("VentanaInicio");

            // Configuración de botones (766x144 px)
            int bWidth = 766;
            int bHeight = 144;

            btnNuevoMapa = new ImageButton3State(
                    loadTexture("BotonNuevoMapa"),
                    loadTexture("BotonNuevoMapaRollover"),
                    loadTexture("BotonNuevoMapaClick"),
                    0, 0, bWidth, bHeight);

            btnCargarMapa = new ImageButton3State(
                    loadTexture("BotonCargarMapa"),
                    loadTexture("BotonCargarMapaRollover"),
                    loadTexture("BotonCargarMapaClick"),
                    0, 0, bWidth, bHeight);

            btnExit = new ImageButton3State(
                    loadTexture("BotonSalir"),
                    loadTexture("BotonSalirRollover"),
                    loadTexture("BotonSalirClick"),
                    0, 0, bWidth, bHeight);

        } catch (IOException e) {
            // Si fallan las texturas específicas (como BotonCargarMapa que falta),
            // el constructor fallará.
            System.err.println("Error loading launcher textures: " + e.getMessage());
        }

        // Trigger update check
        GithubReleaseChecker.checkForUpdates();
    }

    @Override
    public void render() {
        ImGui.setNextWindowSize(Window.INSTANCE.getWidth(), Window.INSTANCE.getHeight(), ImGuiCond.Always);
        ImGui.setNextWindowPos(0, 0, ImGuiCond.Always);

        ImGui.begin(this.getClass().getSimpleName(), ImGuiWindowFlags.NoTitleBar |
                ImGuiWindowFlags.NoMove |
                ImGuiWindowFlags.NoFocusOnAppearing |
                ImGuiWindowFlags.NoDecoration |
                ImGuiWindowFlags.NoBackground |
                ImGuiWindowFlags.NoResize |
                ImGuiWindowFlags.NoSavedSettings |
                ImGuiWindowFlags.NoBringToFrontOnFocus);

        // Dimensiones de la imagen de fondo (diseñado para 1024x1024 o similar)
        int imageWidth = 1024;
        int imageHeight = 1024;
        int x = (Window.INSTANCE.getWidth() - imageWidth) / 2;
        int y = (Window.INSTANCE.getHeight() - imageHeight) / 2;

        if (backgroundImage != 0) {
            ImGui.getWindowDrawList().addImage(backgroundImage, x, y, x + imageWidth, y + imageHeight);
        }

        // --- Layout de Botones ---
        int buttonWidth = 766;
        int centerX = x + (imageWidth - buttonWidth) / 2;

        // Alineamos con los slots del fondo (aprox 44% de altura)
        int yStart = y + 448;
        int spacing = 168;

        // Renderizado de botones si no son nulos
        if (btnNuevoMapa != null) {
            if (btnNuevoMapa.render(centerX, yStart) || ImGui.isKeyPressed(GLFW_KEY_ENTER))
                this.buttonConnect();
        }

        if (btnCargarMapa != null) {
            if (btnCargarMapa.render(centerX, yStart + spacing))
                this.buttonLoadMapAction();
        }

        if (btnExit != null) {
            if (btnExit.render(centerX, yStart + spacing * 2))
                this.buttonExitGame();
        }

        // --- INFORMACIÓN DE VERSIÓN ---
        // Intentamos detectar la versión automáticamente del paquete
        String verStr = Engine.class.getPackage().getImplementationVersion();
        if (verStr == null || verStr.isEmpty())
            verStr = Engine.VERSION;
        String version = "v" + verStr;

        // Margen ajustado para situar la versión dentro del recuadro inferior izquierdo
        int marginX = 90;
        int marginBottom = 22;

        // Versión (Izquierda)
        ImGui.setCursorPos(x + marginX, y + imageHeight - marginBottom);
        ImGui.textColored(0.7f, 0.7f, 0.7f, 1.0f, version);

        // Update Notification Logic
        if (GithubReleaseChecker.isUpdateAvailable() && !updatePopupShown) {
            ImGui.openPopup(I18n.INSTANCE.get("update.available"));
            updatePopupShown = true;
        }

        if (ImGui.beginPopupModal(I18n.INSTANCE.get("update.available"), ImGuiWindowFlags.AlwaysAutoResize)) {
            ImGui.text(I18n.INSTANCE.get("update.available"));
            ImGui.spacing();

            GithubReleaseChecker.ReleaseInfo release = GithubReleaseChecker.getLatestRelease();
            if (release != null) {
                ImGui.textColored(0.2f, 0.8f, 0.2f, 1.0f, release.tagName);
                ImGui.textWrapped(release.name);
            }

            ImGui.spacing();
            ImGui.separator();
            ImGui.spacing();

            if (ImGui.button(I18n.INSTANCE.get("update.download"), 200, 30)) {
                if (release != null) {
                    Form.openURL(release.htmlUrl);
                }
                ImGui.closeCurrentPopup();
            }

            ImGui.sameLine();

            if (ImGui.button(I18n.INSTANCE.get("update.close"), 100, 30)) {
                ImGui.closeCurrentPopup();
            }

            ImGui.endPopup();
        }

        ImGui.end();
    }

    private void buttonConnect() {
        simulateEditorConnection(true);
    }

    private void buttonLoadMapAction() {
        if (org.argentumforge.engine.utils.MapFileUtils.openAndLoadMap()) {
            simulateEditorConnection(false);
        }
    }

    private void simulateEditorConnection(boolean newMap) {
        User user = User.INSTANCE;
        int startX = 50;
        int startY = 50;
        short charIndex = 1;

        user.getUserPos().setX(startX);
        user.getUserPos().setY(startY);
        user.setUserMap((short) 1);
        user.setUserCharIndex(charIndex);

        if (newMap) {
            org.argentumforge.engine.utils.MapManager.createEmptyMap(100, 100);
        }

        // Configurar apariencia en el singleton User para persistencia
        user.setUserBody((short) 1);
        user.setUserHead((short) 4);

        // Aplicar al personaje actual
        user.refreshUserCharacter();

        this.connectPressed = true;
    }

    public boolean isConnectPressed() {
        return connectPressed;
    }

    private void buttonExitGame() {
        Engine.closeClient();
    }
}
