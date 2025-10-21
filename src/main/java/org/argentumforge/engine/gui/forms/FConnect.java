package org.argentumforge.engine.gui.forms;

import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImString;
import org.argentumforge.engine.Engine;
import org.argentumforge.engine.Window;
import org.argentumforge.engine.game.Messages;
import org.argentumforge.engine.game.User;
import org.argentumforge.engine.game.models.Direction;
import org.argentumforge.engine.gui.ImGUISystem;
import org.argentumforge.engine.gui.widgets.ImageButton3State;
import org.argentumforge.engine.utils.GameData;
import org.argentumforge.network.Connection;

import java.io.IOException;

import static org.argentumforge.engine.audio.Sound.playMusic;
import static org.argentumforge.engine.game.Messages.MessageKey.ENTER_USER_PASS;
import static org.argentumforge.engine.utils.GameData.charList;
import static org.argentumforge.engine.utils.GameData.options;
import static org.argentumforge.network.protocol.Protocol.loginExistingChar;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER;

/**
 * Formulario de conexion al servidor.
 * <p>
 * La clase {@code FConnect} implementa la interfaz grafica que se muestra al usuario cuando inicia el cliente y necesita
 * conectarse al servidor. Este formulario es la puerta de entrada principal a la experiencia de juego, permitiendo a los usuarios
 * autenticarse con sus cuentas existentes.
 * <p>
 * El formulario proporciona las siguientes funcionalidades:
 * <ul>
 * <li>Campos para ingresar la direccion IP y puerto del servidor
 * <li>Campos para ingresar nombre de usuario y contraseña
 * <li>Boton para iniciar la conexion al servidor con las credenciales proporcionadas
 * <li>Boton para crear un nuevo personaje, dirigiendo al usuario a {@link FCreateCharacter}
 * <li>Botones adicionales para acceder a opciones como recuperacion de cuenta, manual del juego, reglas, codigo fuente y salida
 * del programa
 * </ul>
 * <p>
 * Esta clase maneja la validacion basica de campos y establece la comunicacion inicial con el servidor, enviando las credenciales
 * del usuario y procesando la respuesta para determinar si se permite el acceso. En caso de conexion exitosa, la aplicacion
 * avanzara a la pantalla principal.
 */

public final class FConnect extends Form {

    // Botones gráficos de 3 estados
    private ImageButton3State btnConnect;
    private ImageButton3State btnCreateCharacter;
    private ImageButton3State btnExit;

    public FConnect() {
        try {
            this.backgroundImage = loadTexture("VentanaConectar");
            // Instanciación de botones con 3 estados (usa los tamaños y posiciones existentes)
            btnConnect = new ImageButton3State(
                loadTexture("BotonConectarse"),
                loadTexture("BotonConectarseRollover"),
                loadTexture("BotonConectarseClick"),
                    325, 264, 89, 25
            );
            btnCreateCharacter = new ImageButton3State(
                loadTexture("BotonCrearPersonajeConectar"),
                loadTexture("BotonCrearPersonajeRolloverConectar"),
                loadTexture("BotonCrearPersonajeClickConectar"),
                45, 561, 89, 25
            );
            btnExit = new ImageButton3State(
                loadTexture("BotonSalirConnect"),
                loadTexture("BotonBotonSalirRolloverConnect"),
                loadTexture("BotonSalirClickConnect"),
                669, 561, 89, 25
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void render() {
        ImGui.setNextWindowSize(Window.INSTANCE.getWidth() + 10, Window.INSTANCE.getHeight() + 5, ImGuiCond.Once);
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

        ImGui.getWindowDrawList().addImage(backgroundImage, 0, 0, Window.INSTANCE.getWidth(), Window.INSTANCE.getHeight());

        // Botones gráficos de 3 estados
        if (btnConnect.render() || ImGui.isKeyPressed(GLFW_KEY_ENTER)) this.buttonConnect();
        if (btnCreateCharacter.render()) this.buttonCreateCharacter();
        if (btnExit.render()) this.buttonExitGame();

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
        
        // 2. Cargar mapa inicial (DEBE hacerse ANTES de configurar el personaje en mapData)
        GameData.loadMap(1);
        
        // 3. Configurar el personaje en charList
        charList[charIndex].getPos().setX(startX);
        charList[charIndex].getPos().setY(startY);
        charList[charIndex].setHeading(Direction.DOWN);
        charList[charIndex].setiBody(1);      // ID del cuerpo gráfico
        charList[charIndex].setiHead(1);      // ID de la cabeza gráfica
        charList[charIndex].setDead(false);
        charList[charIndex].setPriv(25);      // Privilegios de administrador
        charList[charIndex].setActive(true);  // Marcar como activo
        
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

    private void buttonCreateCharacter() {
        ImGUISystem.INSTANCE.show(new FCreateCharacter());
        //playMusic("7.ogg");

        btnConnect.delete();
        btnCreateCharacter.delete();
        btnExit.delete();
        this.close();
    }

    private void buttonExitGame() {
        Engine.closeClient();
    }

}
