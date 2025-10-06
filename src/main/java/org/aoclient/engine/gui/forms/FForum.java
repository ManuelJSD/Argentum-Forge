package org.aoclient.engine.gui.forms;

import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiStyleVar;
import org.aoclient.engine.game.managers.ForumManager;
import org.aoclient.engine.game.models.Forum;
import org.aoclient.engine.gui.widgets.ImageButton3State;

import java.io.IOException;
import java.util.List;

/**
 * Tal vez se pregunten porque los nombres de las variables estan en ingles y los comentarios en espaniol.
 * Bien pues no hay una explicacion logica, simplemente es un proyecto donde no hay un concenso sobre si programar full ingles o espaniol,
 * asi que yo opto por el spaninglish xD
 * Atte. LwK
 */

public class FForum extends Form {

    private int Privileges;
    private int CanPostSticky;
    private Forum selectedMessage = null;
    
    // Botones gráficos de 3 estados
    private ImageButton3State btnLeaveMessage;
    private ImageButton3State btnLeaveAnnouncement;
    private ImageButton3State btnClose;
    private ImageButton3State btnBackToList; // Botón para volver a la lista

    public FForum(int privileges, int canPostSticky) {
        super();
        this.Privileges = privileges;
        this.CanPostSticky = canPostSticky;

        try {
            this.backgroundImage = loadTexture("ForoGeneral");

            // Botones
            btnLeaveMessage = new ImageButton3State(
                    loadTexture("BotonDejarMsgForo"),
                    loadTexture("BotonDejarMsgRolloverForo"),
                    loadTexture("BotonDejarMsgClickForo"),
                    48, 404, 97, 24
            );

            btnLeaveAnnouncement = new ImageButton3State(
                    loadTexture("BotonDejarAnuncioForo"),
                    loadTexture("BotonDejarAnuncioRolloverForo"),
                    loadTexture("BotonDejarAnuncioClickForo"),
                    160, 404, 97, 24
            );

            btnClose = new ImageButton3State(
                    loadTexture("BotonCerrarForo"),
                    loadTexture("BotonCerrarRolloverForo"),
                    loadTexture("BotonCerrarClickForo"),
                    272, 404, 97, 24
            );
            
            // Botón para volver a la lista (inicialmente oculto)
            /*btnBackToList = new ImageButton3State(
                    loadTexture("BotonVolver"),
                    loadTexture("BotonVolverRollover"),
                    loadTexture("BotonVolverClick"),
                    30, 30, 80, 24
            );*/

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void render() {
        ImGui.setNextWindowFocus(); // dale foco solo a este FRM

        float windowWidth = 414;
        float windowHeight = 459;
        
        // Obtener el tamaño de la ventana principal
        imgui.ImVec2 mainWindowSize = new imgui.ImVec2();
        ImGui.getMainViewport().getSize(mainWindowSize);
        
        // Calcular la posición para centrar la ventana
        float centerX = (mainWindowSize.x - windowWidth) * 0.5f;
        float centerY = (mainWindowSize.y - windowHeight) * 0.5f;
        
        ImGui.setNextWindowPos(centerX, centerY, ImGuiCond.Appearing);
        ImGui.setNextWindowSize(windowWidth, windowHeight, ImGuiCond.Always);
        ImGui.begin(this.getClass().getSimpleName(), ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoDecoration |
                ImGuiWindowFlags.NoBackground | ImGuiWindowFlags.NoMove);

        // para poder mover el frm.
        this.checkMoveFrm();

        ImGui.setCursorPos(5, 0);
        ImGui.image(backgroundImage, 414, 459);

        drawButtons();

        if (selectedMessage == null) {
            showMessageList();
        } else {
            showMessageContent();
        }

        ImGui.end();
    }

    private void drawButtons() {

        // Dejar mensaje
        if (btnLeaveMessage.render()){

        }

        // Dejar anuncio
        if (btnLeaveAnnouncement.render()){

        }

        // Cerrar
        if (btnClose.render()){
            close();
        }

    }

    /**
     * Muestra la lista de mensajes del foro.
     */
    private void showMessageList() {
        // Estilo para la lista de mensajes
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 30, 20);
        ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 5, 10);
        
        // Posición y tamaño del área de la lista
        ImGui.setCursorPos(55, 51);
        ImGui.beginChild("messageList", 319, 340, true);
        
        // Obtener la lista de mensajes
        List<Forum> messages = ForumManager.getInstance().getForumMessages();
        
        if (messages.isEmpty()) {
            ImGui.text("No hay mensajes en el foro.");
        } else {
            // Mostrar cada mensaje como un botón con su título
            for (Forum message : messages) {
                System.out.println("Mensaje: " + message.getTitle() + 
                               ", Sticky: " + message.isSticky() + 
                               ", Tipo: " + message.getAlignment());
                
                // Estilo diferente para mensajes sticky
                float buttonWidth = ImGui.getWindowWidth() * 0.9f; // 90% del ancho disponible
                
                if (message.isSticky()) {
                    // Fondo amarillo para mensajes sticky
                    ImGui.pushStyleColor(ImGuiCol.Button, 0.9f, 0.9f, 0.1f, 0.4f); // Amarillo más opaco
                    
                    // Mostrar la etiqueta [ANUNCIO] en un color llamativo
                    ImGui.sameLine(0, 0);
                    ImGui.pushStyleColor(ImGuiCol.Text, 1.0f, 0.8f, 0.0f, 1.0f); // Amarillo brillante
                    ImGui.text("[ANUNCIO] ");
                    ImGui.popStyleColor();
                    
                    // Mostrar el título normal después de la etiqueta
                    ImGui.sameLine(0, 0);
                    if (ImGui.button(message.getTitle() + "##" + message.getTitle(), 
                                  buttonWidth - ImGui.calcTextSize("[ANUNCIO] ").x, 0)) {
                        selectedMessage = message;
                    }
                    
                    ImGui.popStyleColor(); // Quitar el estilo del botón*/
                } else {
                    // Para mensajes normales, fondo gris
                    ImGui.pushStyleColor(ImGuiCol.Button, 0.1f, 0.1f, 0.1f, 0.3f);
                    if (ImGui.button(message.getTitle() + "##" + message.getTitle(), buttonWidth, 0)) {
                        selectedMessage = message;
                    }
                    ImGui.popStyleColor();
                }
            }
        }
        
        ImGui.endChild();
        ImGui.popStyleVar(2);
    }
    
    /**
     * Muestra el contenido de un mensaje seleccionado.
     */
    private void showMessageContent() {
        // Botón para volver a la lista
        /*if (btnBackToList.render()) {
            selectedMessage = null;
            return;
        }*/
        
        // Estilo para el contenido del mensaje
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 30, 20);
        ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 5, 10);
        
        // Posición y tamaño del área del mensaje
        ImGui.setCursorPos(30, 60);
        ImGui.beginChild("messageContent", 354, 320, true);
        
        // Mostrar título
        ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 5, 10);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 10, 10);
        
        // Mostrar el título
        ImGui.textWrapped(selectedMessage.getTitle());
        
        // Restaurar estilos
        ImGui.popStyleVar(2);
        ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 5, 10);
        
        ImGui.separator();
        
        // Mostrar autor
        ImGui.text("Autor: " + selectedMessage.getAuthor());
        
        // Mostrar contenido del mensaje
        ImGui.newLine();
        ImGui.textWrapped(selectedMessage.getMessage());
        
        ImGui.endChild();
        ImGui.popStyleVar(2);
    }


}
