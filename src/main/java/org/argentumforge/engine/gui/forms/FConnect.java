package org.argentumforge.engine.gui.forms;

import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import org.argentumforge.engine.Engine;
import org.argentumforge.engine.Window;
import org.argentumforge.engine.game.User;
import org.argentumforge.engine.game.models.Direction;
import org.argentumforge.engine.gui.widgets.ImageButton3State;
import org.argentumforge.engine.utils.GameData;

import java.io.IOException;

import static org.argentumforge.engine.utils.GameData.charList;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER;

public final class FConnect extends Form {

    // Botones gráficos de 3 estados
    private ImageButton3State btnNuevoMapa;
    private ImageButton3State btnCargarMapa;
    private ImageButton3State btnExit;

    public FConnect() {
        try {
            this.backgroundImage = loadTexture("VentanaInicio");
            // Instanciación de botones con 3 estados (usa los tamaños y posiciones
            // existentes)
            btnNuevoMapa = new ImageButton3State(
                    loadTexture("BotonNuevoMapa"),
                    loadTexture("BotonNuevoMapaRollover"),
                    loadTexture("BotonNuevoMapaClick"),
                    137, 462, 766, 144);
            btnCargarMapa = new ImageButton3State(
                    loadTexture("BotonCargarMapa"),
                    loadTexture("BotonCargarMapaRollover"),
                    loadTexture("BotonCargarMapaClick"),
                    137, 625, 766, 144);
            btnExit = new ImageButton3State(
                    loadTexture("BotonSalir"),
                    loadTexture("BotonSalirRollover"),
                    loadTexture("BotonSalirClick"),
                    137, 800, 766, 144);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void render() {
        ImGui.setNextWindowSize(Window.INSTANCE.getWidth() + 10, Window.INSTANCE.getHeight() + 5, ImGuiCond.Always);
        ImGui.setNextWindowPos(-5, -1, ImGuiCond.Once);

        // Start Custom window
        ImGui.begin(this.getClass().getSimpleName(), ImGuiWindowFlags.NoTitleBar |
                ImGuiWindowFlags.NoMove |
                ImGuiWindowFlags.NoFocusOnAppearing |
                ImGuiWindowFlags.NoDecoration |
                ImGuiWindowFlags.NoBackground |
                ImGuiWindowFlags.NoResize |
                ImGuiWindowFlags.NoSavedSettings |
                ImGuiWindowFlags.NoBringToFrontOnFocus);

        // Obtener dimensiones de la textura (tamaño original)
        int bgWidth = backgroundImage; // ImGui usa el ID de textura, necesitamos obtener el tamaño real
        int bgHeight = backgroundImage;

        // Por ahora, asumimos que la imagen es 1024x1024 (tamaño original)
        // Centrar la imagen en la ventana
        int imageWidth = 1024;
        int imageHeight = 1024;
        int x = (Window.INSTANCE.getWidth() - imageWidth) / 2;
        int y = (Window.INSTANCE.getHeight() - imageHeight) / 2;

        ImGui.getWindowDrawList().addImage(backgroundImage, x, y, x + imageWidth, y + imageHeight);

        // Calcular posiciones dinámicas para los botones (relativas a la imagen
        // centrada)
        int centerX = x + (imageWidth - 766) / 2;
        int yNuevo = y + (int) (imageHeight * 0.45f);
        int yCargar = y + (int) (imageHeight * 0.61f);
        int ySalir = y + (int) (imageHeight * 0.78f);

        // Botones gráficos de 3 estados
        if (btnNuevoMapa.render(centerX, yNuevo) || ImGui.isKeyPressed(GLFW_KEY_ENTER))
            this.buttonConnect();
        if (btnCargarMapa.render(centerX, yCargar))
            ; // TODO: ACCCION!
        if (btnExit.render(centerX, ySalir))
            this.buttonExitGame();

        ImGui.end();
    }

    private void buttonConnect() {

        User.INSTANCE.setUserName("Editor");
        // Simular conexión exitosa
        simulateEditorConnection();

    }

    /**
     * Simula una conexión exitosa para modo editor (sin servidor).
     * Inicializa todos los datos necesarios para que GameScene funcione localmente.
     */
    private void simulateEditorConnection() {
        User user = User.INSTANCE;

        // 1. Configurar posición inicial del usuario
        int startX = 50;
        int startY = 50;
        short charIndex = 1;

        user.getUserPos().setX(startX);
        user.getUserPos().setY(startY);
        user.setUserMap((short) 1);
        user.setUserCharIndex(charIndex);

        // 2. Cargar mapa inicial (DEBE hacerse ANTES de configurar el personaje en
        // mapData)
        GameData.loadMap(1);

        // 3. Configurar el personaje en charList
        charList[charIndex].getPos().setX(startX);
        charList[charIndex].getPos().setY(startY);
        charList[charIndex].setHeading(Direction.DOWN);
        charList[charIndex].setiBody(1); // ID del cuerpo gráfico
        charList[charIndex].setiHead(1); // ID de la cabeza gráfica
        charList[charIndex].setDead(false);
        charList[charIndex].setPriv(25); // Privilegios de administrador
        charList[charIndex].setActive(true); // Marcar como activo

        // 4. Registrar el personaje en el mapa (CRÍTICO)
        GameData.mapData[startX][startY].setCharIndex(charIndex);

        // 5. Actualizar áreas de visión
        user.areaChange(startX, startY);

        // 6. Inicializar estados
        user.setUserMoving(false);
        user.setUserNavegando(false);
        user.setUserComerciando(false);

        // 7. Marcar como conectado (esto activa la transición a GameScene)
        user.setUserConected(true);
    }

    private void buttonExitGame() {
        Engine.closeClient();
    }

}
