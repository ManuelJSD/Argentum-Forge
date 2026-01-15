package org.argentumforge.engine.gui.forms;

import imgui.type.ImFloat;

import imgui.ImGui;
import imgui.flag.*;
import org.argentumforge.engine.Window;
import org.argentumforge.engine.game.console.Console;
import org.argentumforge.engine.gui.ImGUISystem;
import org.argentumforge.engine.renderer.RenderSettings;

import static org.argentumforge.engine.utils.GameData.options;
import static org.argentumforge.engine.utils.Time.FPS;
import static org.argentumforge.engine.utils.Time.deltaTime;

/**
 * Formulario principal que proporciona la interfaz de usuario del editor de
 * mapas.
 * <p>
 * La clase {@code FMain} representa la pantalla principal del editor.
 * Esta clase extiende {@link Form} y actúa como el núcleo de la interfaz
 * gráfica de la herramienta.
 * <p>
 * Funcionalidades principales:
 * <ul>
 * <li>Visualización y gestión del viewport del mapa.</li>
 * <li>Acceso a herramientas de edición de superficies, bloqueos, NPCs y
 * objetos.</li>
 * <li>Integración de la consola de mensajes del sistema para logs del
 * editor.</li>
 * <li>Menú principal para carga, guardado y configuración ambiental.</li>
 * <li>Visualización de FPS para monitoreo de performance.</li>
 * </ul>
 * <p>
 * La clase está organizada en métodos privados que separan la lógica de
 * renderizado en secciones específicas para mejorar la legibilidad y el
 * mantenimiento.
 * <p>
 * <b>Nota:</b> Todos los elementos gráficos se dibujan usando ImGui.
 */

public final class FMain extends Form {

    private static final int TRANSPARENT_COLOR = ImGui.getColorU32(0f, 0f, 0f, 0f);

    private FSurfaceEditor surfaceEditor;
    private FBlockEditor blockEditor;
    private FNpcEditor npcEditor;
    private FObjEditor objEditor;
    private FMinimap minimap;
    private FGrhLibrary grhLibrary;
    private float[] ambientColorArr;

    public FMain() {
        ambientColorArr = new float[] {
                org.argentumforge.engine.game.Weather.INSTANCE.getWeatherColor().getRed(),
                org.argentumforge.engine.game.Weather.INSTANCE.getWeatherColor().getGreen(),
                org.argentumforge.engine.game.Weather.INSTANCE.getWeatherColor().getBlue()
        };
        surfaceEditor = new FSurfaceEditor();
        blockEditor = new FBlockEditor();
        npcEditor = new FNpcEditor();
        objEditor = new FObjEditor();
        minimap = new FMinimap();
        grhLibrary = new FGrhLibrary();
    }

    @Override
    public void render() {
        drawMenuBar();
        this.renderFPS();
        this.drawButtons();
        Console.INSTANCE.drawConsole();
    }

    /**
     * Dibuja un botón invisible en la posición y tamaño indicados. Devuelve true si
     * fue presionado.
     */
    private boolean drawButton(int x, int y, int w, int h, String label) {
        ImGui.setCursorPos(x, y);
        return ImGui.invisibleButton(label, w, h);
    }

    // FPS
    private void renderFPS() {
        final String txtFPS = String.valueOf(FPS) + " FPS";
        float widgetWidth = 80;

        ImGui.setNextWindowPos(Window.INSTANCE.getWidth() - widgetWidth - 10, 20);
        ImGui.setNextWindowSize(widgetWidth, 30);
        if (ImGui.begin("FPS", ImGuiWindowFlags.NoDecoration | ImGuiWindowFlags.NoBackground | ImGuiWindowFlags.NoInputs
                | ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoSavedSettings)) {
            ImGui.pushStyleVar(ImGuiStyleVar.SelectableTextAlign, 1.0f, 0.5f);
            ImGui.pushStyleColor(ImGuiCol.HeaderHovered, TRANSPARENT_COLOR);
            ImGui.pushStyleColor(ImGuiCol.HeaderActive, TRANSPARENT_COLOR);
            ImGui.selectable(txtFPS, false, ImGuiSelectableFlags.None, widgetWidth - 10, 20);
            ImGui.popStyleColor();
            ImGui.popStyleColor();
            ImGui.popStyleVar();
        }
        ImGui.end();
    }

    // Botones principales
    private void drawButtons() {
        ImGui.setNextWindowPos(0, 20);
        ImGui.setNextWindowSize(700, 40); // Solo el ancho de los botones
        if (ImGui.begin("ToolBar", ImGuiWindowFlags.NoDecoration | ImGuiWindowFlags.NoBackground
                | ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoSavedSettings)) {
            drawEditorButtons();
        }
        ImGui.end();
    }

    /**
     * Dibuja los botones para mostrar/ocultar los editores de superficies y
     * bloqueos.
     */
    private void drawEditorButtons() {
        ImGui.setCursorPos(10, 5); // Alineado arriba dentro de la mini-ventana

        // Botón Superficies
        if (ImGui.button("Superficies", 100, 25)) {
            if (ImGUISystem.INSTANCE.isFormVisible("FSurfaceEditor")) {
                ImGUISystem.INSTANCE.deleteFrmArray(surfaceEditor);
            } else {
                ImGUISystem.INSTANCE.show(surfaceEditor);
            }
        }

        ImGui.sameLine();

        // Botón Bloqueos
        if (ImGui.button("Bloqueos", 100, 25)) {
            if (ImGUISystem.INSTANCE.isFormVisible("FBlockEditor")) {
                ImGUISystem.INSTANCE.deleteFrmArray(blockEditor);
            } else {
                ImGUISystem.INSTANCE.show(blockEditor);
            }
        }

        ImGui.sameLine();

        // Botón NPCs
        if (ImGui.button("NPCs", 100, 25)) {
            if (ImGUISystem.INSTANCE.isFormVisible("FNpcEditor")) {
                ImGUISystem.INSTANCE.deleteFrmArray(npcEditor);
            } else {
                ImGUISystem.INSTANCE.show(npcEditor);
            }
        }

        ImGui.sameLine();

        // Botón Objetos
        if (ImGui.button("Objetos", 100, 25)) {
            if (ImGUISystem.INSTANCE.isFormVisible("FObjEditor")) {
                ImGUISystem.INSTANCE.deleteFrmArray(objEditor);
            } else {
                ImGUISystem.INSTANCE.show(objEditor);
            }
        }

        ImGui.sameLine();

        // Botón Minimapa
        if (ImGui.button("Minimapa", 100, 25)) {
            if (ImGUISystem.INSTANCE.isFormVisible("FMinimap")) {
                ImGUISystem.INSTANCE.deleteFrmArray(minimap);
            } else {
                ImGUISystem.INSTANCE.show(minimap);
            }
        }

        ImGui.sameLine();

        ImGui.sameLine();

        ImGui.sameLine();

        // Botón Opciones
        /*
         * if (ImGui.button("Opciones", 100, 25)) {
         * ImGUISystem.INSTANCE.show(new FOptions());
         * }
         */
    }

    private void drawMenuBar() {

        if (ImGui.beginMainMenuBar()) {

            RenderSettings renderSettings = options.getRenderSettings();

            if (ImGui.beginMenu("Archivo")) {
                if (ImGui.menuItem("Cargar Mapa")) {
                    this.loadMapAction();
                }

                if (ImGui.menuItem("Guardar Mapa")) {
                    org.argentumforge.engine.utils.MapFileUtils.saveMap();
                }

                ImGui.separator();

                if (ImGui.menuItem("Opciones")) {
                    ImGUISystem.INSTANCE.show(new FOptions());
                }

                ImGui.separator();

                if (ImGui.menuItem("Salir")) {
                    org.argentumforge.engine.Engine.closeClient();
                }
                ImGui.endMenu();
            }

            if (ImGui.beginMenu("Editar")) {
                org.argentumforge.engine.utils.editor.commands.CommandManager manager = org.argentumforge.engine.utils.editor.commands.CommandManager
                        .getInstance();

                if (ImGui.menuItem("Deshacer", "Ctrl+Z", false, manager.canUndo())) {
                    manager.undo();
                }

                if (ImGui.menuItem("Rehacer", "Ctrl+Y", false, manager.canRedo())) {
                    manager.redo();
                }

                ImGui.endMenu();
            }

            /*
             * if (ImGui.beginMenu("Editores")) {
             * if (ImGui.menuItem("Superficies", "",
             * ImGUISystem.INSTANCE.isFormVisible("FSurfaceEditor"))) {
             * if (ImGUISystem.INSTANCE.isFormVisible("FSurfaceEditor")) {
             * ImGUISystem.INSTANCE.deleteFrmArray(surfaceEditor);
             * } else {
             * ImGUISystem.INSTANCE.show(surfaceEditor);
             * }
             * }
             * if (ImGui.menuItem("Bloqueos", "",
             * ImGUISystem.INSTANCE.isFormVisible("FBlockEditor"))) {
             * if (ImGUISystem.INSTANCE.isFormVisible("FBlockEditor")) {
             * ImGUISystem.INSTANCE.deleteFrmArray(blockEditor);
             * } else {
             * ImGUISystem.INSTANCE.show(blockEditor);
             * }
             * }
             * ImGui.endMenu();
             * }
             */

            if (ImGui.beginMenu("Mapa")) {
                if (ImGui.menuItem("Información del Mapa")) {
                    ImGUISystem.INSTANCE.show(new FInfoMap());
                }
                ImGui.endMenu();
            }

            if (ImGui.beginMenu("Ver")) {
                if (ImGui.beginMenu("Capas")) {
                    if (ImGui.menuItem("Capa 1 (Superficies)", "", renderSettings.getShowLayer()[0])) {
                        renderSettings.getShowLayer()[0] = !renderSettings.getShowLayer()[0];
                        options.save();
                    }
                    if (ImGui.menuItem("Capa 2 (Costas, etc)", "", renderSettings.getShowLayer()[1])) {
                        renderSettings.getShowLayer()[1] = !renderSettings.getShowLayer()[1];
                        options.save();
                    }
                    if (ImGui.menuItem("Capa 3 (Arboles, etc)", "", renderSettings.getShowLayer()[2])) {
                        renderSettings.getShowLayer()[2] = !renderSettings.getShowLayer()[2];
                        options.save();
                    }
                    if (ImGui.menuItem("Capa 4 (Techos, etc)", "", renderSettings.getShowLayer()[3])) {
                        renderSettings.getShowLayer()[3] = !renderSettings.getShowLayer()[3];
                        options.save();
                    }
                    ImGui.endMenu();
                }

                if (ImGui.menuItem("Bloqueos", "", renderSettings.getShowBlock())) {
                    renderSettings.setShowBlock(!renderSettings.getShowBlock());
                    options.save();
                }

                if (ImGui.menuItem("Objetos", "", renderSettings.getShowOJBs())) {
                    renderSettings.setShowOJBs(!renderSettings.getShowOJBs());
                    options.save();
                }

                if (ImGui.menuItem("NPC's", "", renderSettings.getShowNPCs())) {
                    renderSettings.setShowNPCs(!renderSettings.getShowNPCs());
                    options.save();
                }

                if (ImGui.menuItem("Traslados", "", renderSettings.getShowMapTransfer())) {
                    renderSettings.setShowMapTransfer(!renderSettings.getShowMapTransfer());
                    options.save();
                }

                if (ImGui.menuItem("Triggers", "", renderSettings.getShowTriggers())) {
                    renderSettings.setShowTriggers(!renderSettings.getShowTriggers());
                    options.save();
                }

                ImGui.endMenu();
            }

            if (ImGui.beginMenu("Miscelánea")) {
                if (ImGui.menuItem("Modo Caminata", "", org.argentumforge.engine.game.User.INSTANCE.isWalkingmode())) {
                    org.argentumforge.engine.game.User.INSTANCE
                            .setWalkingmode(!org.argentumforge.engine.game.User.INSTANCE.isWalkingmode());
                }

                if (ImGui.beginMenu("Ambiente")) {
                    if (ImGui.colorEdit3("Luz Ambiente", ambientColorArr)) {
                        org.argentumforge.engine.game.Weather.INSTANCE.setAmbientColor(ambientColorArr[0],
                                ambientColorArr[1],
                                ambientColorArr[2]);
                    }

                    ImGui.separator();

                    if (ImGui.menuItem("Día")) {
                        ambientColorArr[0] = 1.0f;
                        ambientColorArr[1] = 1.0f;
                        ambientColorArr[2] = 1.0f;
                        org.argentumforge.engine.game.Weather.INSTANCE.setAmbientColor(1.0f, 1.0f, 1.0f);
                    }
                    if (ImGui.menuItem("Tarde")) {
                        ambientColorArr[0] = 0.8f;
                        ambientColorArr[1] = 0.5f;
                        ambientColorArr[2] = 0.3f;
                        org.argentumforge.engine.game.Weather.INSTANCE.setAmbientColor(0.8f, 0.5f, 0.3f);
                    }
                    if (ImGui.menuItem("Noche")) {
                        ambientColorArr[0] = 0.2f;
                        ambientColorArr[1] = 0.2f;
                        ambientColorArr[2] = 0.4f;
                        org.argentumforge.engine.game.Weather.INSTANCE.setAmbientColor(0.2f, 0.2f, 0.4f);
                    }

                    ImGui.endMenu();
                }

                ImGui.separator();

                if (ImGui.menuItem("Biblioteca GRH", "")) {
                    if (ImGUISystem.INSTANCE.isFormVisible("FGrhLibrary")) {
                        ImGUISystem.INSTANCE.deleteFrmArray(grhLibrary);
                    } else {
                        grhLibrary = new FGrhLibrary();
                        ImGUISystem.INSTANCE.show(grhLibrary);
                    }
                }

                ImGui.endMenu();
            }

            ImGui.endMainMenuBar();
        }
    }

    private void loadMapAction() {
        org.argentumforge.engine.utils.MapFileUtils.openAndLoadMap();
    }

}
