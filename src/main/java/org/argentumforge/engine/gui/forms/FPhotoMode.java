package org.argentumforge.engine.gui.forms;

import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiCol;
import org.argentumforge.engine.i18n.I18n;
import org.argentumforge.engine.utils.ScreenshotUtils;
import org.argentumforge.engine.renderer.RenderSettings;
import static org.argentumforge.engine.utils.GameData.options;

/**
 * Panel de control minimalista para el Modo Foto.
 * Solo se muestra cuando el modo foto está activo.
 */
public class FPhotoMode {

    public void render() {
        RenderSettings settings = options.getRenderSettings();

        // Ventana flotante en la esquina superior derecha
        ImGui.setNextWindowPos(ImGui.getMainViewport().getSizeX() - 320, 20, ImGuiCond.Once);
        ImGui.setNextWindowSize(300, 450, ImGuiCond.Once);

        int flags = ImGuiWindowFlags.AlwaysAutoResize | ImGuiWindowFlags.NoSavedSettings;

        if (ImGui.begin(getLabel("title", "--- PANEL DE FOTOGRAFÍA ---"), flags)) {

            if (ImGui.beginTabBar("PhotoTabs")) {

                // TAB 1: LUZ Y COLOR
                if (ImGui.beginTabItem(getLabel("tab.light", "Luz y Color"))) {
                    ImGui.dummy(0, 5);

                    float[] exp = { settings.getPhotoExposure() };
                    if (ImGui.sliderFloat(getLabel("exposure", "Exposición"), exp, 0.5f, 2.0f))
                        settings.setPhotoExposure(exp[0]);

                    float[] con = { settings.getPhotoContrast() };
                    if (ImGui.sliderFloat(getLabel("contrast", "Contraste"), con, 0.5f, 2.0f))
                        settings.setPhotoContrast(con[0]);

                    float[] sat = { settings.getPhotoSaturation() };
                    if (ImGui.sliderFloat(getLabel("saturation", "Saturación"), sat, 0.0f, 2.0f))
                        settings.setPhotoSaturation(sat[0]);

                    ImGui.separator();
                    ImGui.text(getLabel("filter", "Filtro:"));
                    String[] filters = {
                            getLabel("filter.none", "Ninguno"),
                            getLabel("filter.grayscale", "Escala de Grises"),
                            getLabel("filter.sepia", "Sepia"),
                            getLabel("filter.vintage", "Vintage"),
                            getLabel("filter.warm", "Cálido"),
                            getLabel("filter.cool", "Frío")
                    };
                    int currentFilter = settings.getPhotoColorFilter().ordinal();
                    if (ImGui.beginCombo("##colorFilter", filters[currentFilter])) {
                        for (int i = 0; i < filters.length; i++) {
                            if (ImGui.selectable(filters[i], currentFilter == i)) {
                                settings.setPhotoColorFilter(RenderSettings.ColorFilter.values()[i]);
                            }
                        }
                        ImGui.endCombo();
                    }
                    ImGui.endTabItem();
                }

                // TAB 2: LENTE
                if (ImGui.beginTabItem(getLabel("tab.lens", "Lente"))) {
                    ImGui.dummy(0, 5);

                    if (ImGui.checkbox(getLabel("bloom", "Bloom (Resplandor)"), settings.isPhotoBloom()))
                        settings.setPhotoBloom(!settings.isPhotoBloom());
                    if (settings.isPhotoBloom()) {
                        float[] val = { settings.getBloomIntensity() };
                        if (ImGui.sliderFloat(getLabel("bloom.intensity", "Intensidad Bloom"), val, 0.0f, 2.0f))
                            settings.setBloomIntensity(val[0]);
                        float[] thr = { settings.getPhotoBloomThreshold() };
                        if (ImGui.sliderFloat(getLabel("bloom.threshold", "Umbral Bloom"), thr, 0.0f, 1.0f))
                            settings.setPhotoBloomThreshold(thr[0]);
                    }

                    ImGui.separator();
                    if (ImGui.checkbox(getLabel("grain", "Grano de Película"), settings.isPhotoGrain()))
                        settings.setPhotoGrain(!settings.isPhotoGrain());
                    if (settings.isPhotoGrain()) {
                        float[] val = { settings.getGrainIntensity() };
                        if (ImGui.sliderFloat(getLabel("grain.intensity", "Intensidad Grano"), val, 0.0f, 0.5f))
                            settings.setGrainIntensity(val[0]);
                    }

                    ImGui.separator();
                    if (ImGui.checkbox(getLabel("vignette", "Viñeta"), settings.isPhotoVignette()))
                        settings.setPhotoVignette(!settings.isPhotoVignette());
                    if (settings.isPhotoVignette()) {
                        float[] val = { settings.getVignetteIntensity() };
                        if (ImGui.sliderFloat(getLabel("vignette.intensity", "Intensidad Viñeta"), val, 0.0f, 1.0f))
                            settings.setVignetteIntensity(val[0]);
                    }
                    ImGui.endTabItem();
                }

                // TAB 3: ENFOQUE Y CÁMARA
                if (ImGui.beginTabItem(getLabel("tab.cam", "Enfoque y Cámara"))) {
                    ImGui.dummy(0, 5);

                    float[] zoom = { settings.getPhotoZoom() };
                    if (ImGui.sliderFloat(getLabel("zoom", "Zoom Óptico"), zoom, 1.0f, 5.0f))
                        settings.setPhotoZoom(zoom[0]);

                    ImGui.separator();
                    if (ImGui.checkbox(getLabel("dof", "Profundidad de Campo"), settings.isPhotoDoF()))
                        settings.setPhotoDoF(!settings.isPhotoDoF());
                    if (settings.isPhotoDoF()) {
                        float[] foc = { settings.getDofFocus() };
                        if (ImGui.sliderFloat(getLabel("dof.focus", "Punto Enfoque"), foc, 0.0f, 1.0f))
                            settings.setDofFocus(foc[0]);
                        float[] ran = { settings.getDofRange() };
                        if (ImGui.sliderFloat(getLabel("dof.range", "Rango Enfoque"), ran, 0.0f, 1.0f))
                            settings.setDofRange(ran[0]);
                    }

                    ImGui.separator();
                    if (ImGui.checkbox(getLabel("timestop", "Pausar Tiempo"), settings.isPhotoTimeStop()))
                        settings.setPhotoTimeStop(!settings.isPhotoTimeStop());
                    ImGui.textColored(ImGui.getColorU32(0.7f, 0.7f, 0.7f, 1.0f),
                            getLabel("timestop.desc", "Congela animaciones y NPCs"));

                    ImGui.endTabItem();
                }

                // TAB 4: SOMBRAS
                if (ImGui.beginTabItem(getLabel("tab.shadows", "Sombras"))) {
                    ImGui.dummy(0, 5);
                    if (ImGui.checkbox(getLabel("shadows", "Sombras de Objetos"), settings.isPhotoShadows()))
                        settings.setPhotoShadows(!settings.isPhotoShadows());
                    if (settings.isPhotoShadows()) {
                        ImGui.indent(20);
                        if (ImGui.checkbox(getLabel("shadows.soft", "Sombras Suaves (HQ)"),
                                settings.isPhotoSoftShadows()))
                            settings.setPhotoSoftShadows(!settings.isPhotoSoftShadows());
                        ImGui.unindent(20);
                    }
                    ImGui.endTabItem();
                }

                ImGui.endTabBar();
            }

            ImGui.dummy(0, 10);
            ImGui.separator();
            ImGui.dummy(0, 5);

            // Botón Principal: HACER FOTO
            ImGui.pushStyleColor(ImGuiCol.Button, 0.2f, 0.4f, 0.8f, 1.0f);
            ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.3f, 0.5f, 0.9f, 1.0f);
            ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0.1f, 0.3f, 0.7f, 1.0f);
            if (ImGui.button(getLabel("btn.take", "HACER FOTO (F2)"), -1, 40)) {
                ScreenshotUtils.takeScreenshot();
            }
            ImGui.popStyleColor(3);

            ImGui.dummy(0, 5);
            if (ImGui.button(getLabel("btn.reset", "Restablecer Ajustes"), -1, 30)) {
                settings.resetPhotoMode();
            }
            ImGui.dummy(0, 5);
            if (ImGui.button(getLabel("btn.close", "Cerrar Modo Foto"), -1, 30)) {
                settings.setPhotoModeActive(false);
            }

            ImGui.end();
        }
    }

    private String getLabel(String key, String defaultVal) {
        String fullKey = "photomode." + key;
        String val = I18n.INSTANCE.get(fullKey);
        // Si no existe la traducción, devolvemos el valor por defecto
        return (val.equals(fullKey)) ? defaultVal : val;
    }
}
