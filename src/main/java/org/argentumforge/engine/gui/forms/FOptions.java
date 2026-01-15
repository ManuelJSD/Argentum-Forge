package org.argentumforge.engine.gui.forms;

import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import org.argentumforge.engine.Window;
import static org.argentumforge.engine.audio.Sound.*;
import static org.argentumforge.engine.utils.GameData.options;

/**
 * <p>
 * Proporciona una interfaz grafica completa para que el usuario pueda ver y
 * modificar las diferentes opciones de configuracion.
 * Permite gestionar ajustes como la pantalla completa, sincronizacion vertical,
 * activacion/desactivacion de musica y sonidos.
 * <p>
 * Incluye tambien una serie de botones que dan acceso a otras funcionalidades
 * relacionadas con la configuracion, como la
 * configuracion de teclas, visualizacion del mapa, acceso al manual, soporte,
 * mensajes personalizados, cambio de contrasena,
 * radio y tutorial.
 * <p>
 * El formulario se encarga de aplicar los cambios de configuracion
 * inmediatamente cuando el usuario modifica las opciones, y
 * guarda los ajustes en un archivo de configuracion cuando se cierra. Mantiene
 * una interfaz cohesiva y uniforme con el resto de
 * elementos.
 */

public final class FOptions extends Form {

    public FOptions() {

    }

    @Override
    public void render() {
        ImGui.setNextWindowSize(400, 360, ImGuiCond.Always);
        ImGui.begin("Configuración", ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoResize);

        if (ImGui.checkbox("Pantalla Completa", options.isFullscreen())) {
            options.setFullscreen(!options.isFullscreen());
            Window.INSTANCE.toggleWindow();
        }

        // Selector de Resolucion
        String[] resolutions = { "800x600", "1024x768", "1024x1024", "1280x720", "1366x768", "1920x1080", "2560x1440",
                "3840x2160" };
        int currentResIndex = 0;
        String currentResString = options.getScreenWidth() + "x" + options.getScreenHeight();

        for (int i = 0; i < resolutions.length; i++) {
            if (resolutions[i].equals(currentResString)) {
                currentResIndex = i;
                break;
            }
        }

        ImGui.setNextItemWidth(200);
        if (ImGui.beginCombo("Resolucion", resolutions[currentResIndex])) {
            for (int i = 0; i < resolutions.length; i++) {
                boolean isSelected = (currentResIndex == i);
                if (ImGui.selectable(resolutions[i], isSelected)) {
                    String[] parts = resolutions[i].split("x");
                    int newWidth = Integer.parseInt(parts[0]);
                    int newHeight = Integer.parseInt(parts[1]);

                    options.setScreenWidth(newWidth);
                    options.setScreenHeight(newHeight);
                    Window.INSTANCE.updateResolution(newWidth, newHeight);
                    options.save();
                }
                if (isSelected) {
                    ImGui.setItemDefaultFocus();
                }
            }
            ImGui.endCombo();
        }

        if (ImGui.checkbox("Sincronizacion Vertical", options.isVsync())) {
            options.setVsync(!options.isVsync());
            Window.INSTANCE.toggleWindow();
        }

        ImGui.separator();

        if (ImGui.checkbox("Musica", options.isMusic())) {
            options.setMusic(!options.isMusic());
            stopMusic();
        }

        if (ImGui.checkbox("Audio", options.isSound())) {
            options.setSound(!options.isSound());
        }

        ImGui.separator();

        if (ImGui.checkbox("Cursores graficos", options.isCursorGraphic())) {
            options.setCursorGraphic(!options.isCursorGraphic());
        }

        ImGui.separator();
        ImGui.text("Transparencia Previsualizacion (Ghost):");
        float[] ghostAlpha = { options.getRenderSettings().getGhostOpacity() };
        if (ImGui.sliderFloat("##ghostAlpha", ghostAlpha, 0.0f, 1.0f)) {
            options.getRenderSettings().setGhostOpacity(ghostAlpha[0]);
        }

        // Add some spacing before buttons
        ImGui.dummy(0, 20);

        this.drawButtons();

        ImGui.end();
    }

    private void drawButtons() {
        float buttonWidth = 200;
        float centerX = (ImGui.getWindowWidth() - buttonWidth) / 2;

        ImGui.setCursorPosX(centerX);
        if (ImGui.button("Configurar Teclas", buttonWidth, 25)) {
            playSound(SND_CLICK);
            IM_GUI_SYSTEM.show(new FBindKeys());
        }

        ImGui.setCursorPosX(centerX);
        if (ImGui.button("Editar Rutas", buttonWidth, 25)) {
            playSound(SND_CLICK);
            IM_GUI_SYSTEM.show(new FRoutes());
        }

        ImGui.separator();

        ImGui.setCursorPosX(centerX);
        if (ImGui.button("Cerrar", buttonWidth, 25)) {
            options.save();
            this.close();
        }
    }

}
