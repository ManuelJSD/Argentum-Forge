package org.argentumforge.engine.gui.forms;

import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImInt;

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
        ImGui.setNextWindowSize(500, 0, ImGuiCond.Once);
        ImGui.begin(org.argentumforge.engine.i18n.I18n.INSTANCE.get("options.title"),
                ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.AlwaysAutoResize
                        | ImGuiWindowFlags.NoDocking);

        if (ImGui.beginTabBar("OptionsTabs")) {

            // --- TAB: GENERAL ---
            if (ImGui.beginTabItem(org.argentumforge.engine.i18n.I18n.INSTANCE.get("options.tab.general", "General"))) {
                ImGui.dummy(0, 10);

                // Language
                java.util.List<String> availableLanguages = org.argentumforge.engine.i18n.I18n.INSTANCE
                        .getAvailableLanguages();
                String currentLanguage = options.getLanguage();
                int currentLangIndex = availableLanguages.indexOf(currentLanguage);
                if (currentLangIndex == -1)
                    currentLangIndex = 0;

                ImGui.text(org.argentumforge.engine.i18n.I18n.INSTANCE.get("options.language") + ":");
                ImGui.sameLine();
                ImGui.setNextItemWidth(200);
                if (ImGui.beginCombo("##language",
                        org.argentumforge.engine.i18n.I18n.INSTANCE.getLanguageName(currentLanguage))) {
                    for (int i = 0; i < availableLanguages.size(); i++) {
                        String lang = availableLanguages.get(i);
                        boolean isSelected = (i == currentLangIndex);
                        if (ImGui.selectable(org.argentumforge.engine.i18n.I18n.INSTANCE.getLanguageName(lang),
                                isSelected)) {
                            options.setLanguage(lang);
                            org.argentumforge.engine.i18n.I18n.INSTANCE.loadLanguage(lang);
                            options.save();
                        }
                        if (isSelected)
                            ImGui.setItemDefaultFocus();
                    }
                    ImGui.endCombo();
                }
                ImGui.textColored(ImGui.getColorU32(1.0f, 1.0f, 0.0f, 1.0f),
                        "* " + org.argentumforge.engine.i18n.I18n.INSTANCE.get("options.restart"));

                ImGui.separator();

                // Docking Option
                if (ImGui.checkbox(org.argentumforge.engine.i18n.I18n.INSTANCE.get("options.docking"),
                        options.isDockingEnabled())) {
                    options.setDockingEnabled(!options.isDockingEnabled());
                    options.save();
                    org.argentumforge.engine.gui.DialogManager.getInstance().showInfo("Docking",
                            org.argentumforge.engine.i18n.I18n.INSTANCE.get("options.docking.message"));
                }
                if (ImGui.isItemHovered()) {
                    ImGui.setTooltip(org.argentumforge.engine.i18n.I18n.INSTANCE.get("options.docking.tooltip"));
                }

                ImGui.separator();

                // Pre-releases Option
                if (ImGui.checkbox(org.argentumforge.engine.i18n.I18n.INSTANCE.get("options.general.check_prereleases"),
                        options.isCheckPreReleases())) {
                    options.setCheckPreReleases(!options.isCheckPreReleases());
                    options.save();
                }

                ImGui.separator();

                // Auto-save Settings
                ImGui.textColored(ImGui.getColorU32(0.2f, 0.7f, 1.0f, 1.0f),
                        org.argentumforge.engine.i18n.I18n.INSTANCE.get("options.autosave") + ":");
                ImGui.dummy(0, 5);

                if (ImGui.checkbox(org.argentumforge.engine.i18n.I18n.INSTANCE.get("options.autosave.enabled"),
                        options.isAutoSaveEnabled())) {
                    options.setAutoSaveEnabled(!options.isAutoSaveEnabled());
                    options.save();
                }

                ImInt interval = new ImInt(options.getAutoSaveIntervalMinutes());
                ImGui.setNextItemWidth(100);
                if (ImGui.inputInt("##autosaveInterval", interval)) {
                    if (interval.get() < 1)
                        interval.set(1);
                    if (interval.get() > 60)
                        interval.set(60);
                    options.setAutoSaveIntervalMinutes(interval.get());
                    options.save();
                    // Reset timer to apply new interval from now
                    org.argentumforge.engine.utils.editor.AutoSaveManager.getInstance().resetTimer();
                }
                ImGui.sameLine();
                ImGui.text(org.argentumforge.engine.i18n.I18n.INSTANCE.get("options.autosave.interval"));

                ImGui.endTabItem();
            }

            // --- TAB: APARIENCIA ---
            if (ImGui.beginTabItem("Apariencia")) {
                ImGui.dummy(0, 10);

                // User Appearance
                ImGui.text("Apariencia en Tierra:");
                ImGui.separator();

                ImInt body = new ImInt(org.argentumforge.engine.game.User.INSTANCE.getUserBody());
                if (ImGui.inputInt("Cuerpo", body)) {
                    if (body.get() < 1)
                        body.set(1);
                    org.argentumforge.engine.game.User.INSTANCE.setUserBody(body.get());
                    org.argentumforge.engine.game.User.INSTANCE.refreshUserCharacter();
                }

                ImInt head = new ImInt(org.argentumforge.engine.game.User.INSTANCE.getUserHead());
                if (ImGui.inputInt("Cabeza", head)) {
                    if (head.get() < 1)
                        head.set(1);
                    org.argentumforge.engine.game.User.INSTANCE.setUserHead(head.get());
                    org.argentumforge.engine.game.User.INSTANCE.refreshUserCharacter();
                }

                ImGui.dummy(0, 10);
                ImGui.text("Apariencia en Agua:");
                ImGui.separator();

                ImInt waterBody = new ImInt(org.argentumforge.engine.game.User.INSTANCE.getUserWaterBody());
                if (ImGui.inputInt("Cuerpo (Agua)", waterBody)) {
                    if (waterBody.get() < 1)
                        waterBody.set(1);
                    org.argentumforge.engine.game.User.INSTANCE.setUserWaterBody(waterBody.get());
                    // Update appearance immediately if on water
                    if (org.argentumforge.engine.game.User.INSTANCE.isWalkingmode()) {
                        org.argentumforge.engine.game.User.INSTANCE.checkAppearance();
                    }
                }

                ImGui.dummy(0, 10);
                ImGui.textDisabled("(Los cambios se ven en tiempo real)");

                ImGui.dummy(0, 10);
                ImGui.text("Interfaz de Usuario:");
                ImGui.separator();

                // Theme Selector using ThemeManager
                org.argentumforge.engine.gui.Theme.StyleType currentTheme = org.argentumforge.engine.gui.ThemeManager
                        .getInstance().getCurrentTheme();
                if (ImGui.beginCombo("Tema Visual", currentTheme.name())) {
                    for (org.argentumforge.engine.gui.Theme.StyleType type : org.argentumforge.engine.gui.Theme.StyleType
                            .values()) {
                        boolean isSelected = (type == currentTheme);
                        if (ImGui.selectable(type.name(), isSelected)) {
                            org.argentumforge.engine.gui.ThemeManager.getInstance().setTheme(type);
                        }
                        if (isSelected) {
                            ImGui.setItemDefaultFocus();
                        }
                    }
                    ImGui.endCombo();
                }

                ImGui.dummy(0, 10);
                ImGui.endTabItem();
            }

            // --- TAB: GRAFICOS ---
            if (ImGui.beginTabItem(org.argentumforge.engine.i18n.I18n.INSTANCE.get("options.tab.graphics"))) {
                ImGui.dummy(0, 10);

                // Resolution
                String[] resolutions = { "1024x768", "1024x1024", "1280x720", "1366x768", "1920x1080", "2560x1440",
                        "3840x2160" };
                String currentResString = options.getScreenWidth() + "x" + options.getScreenHeight();

                ImGui.text(org.argentumforge.engine.i18n.I18n.INSTANCE.get("options.resolution") + ":");
                ImGui.sameLine();
                ImGui.setNextItemWidth(200);
                if (ImGui.beginCombo("##resolution", currentResString)) {
                    for (String res : resolutions) {
                        boolean isSelected = res.equals(currentResString);
                        if (ImGui.selectable(res, isSelected)) {
                            String[] parts = res.split("x");
                            options.setScreenWidth(Integer.parseInt(parts[0]));
                            options.setScreenHeight(Integer.parseInt(parts[1]));
                            org.argentumforge.engine.Engine.INSTANCE.getWindow().updateResolution(
                                    options.getScreenWidth(),
                                    options.getScreenHeight());
                            options.save();
                        }
                        if (isSelected)
                            ImGui.setItemDefaultFocus();
                    }
                    ImGui.endCombo();
                }

                ImGui.spacing();
                ImGui.spacing();

                // Client Area (View)
                ImGui.text(org.argentumforge.engine.i18n.I18n.INSTANCE.get("options.clientArea") + ":");
                ImGui.dummy(0, 5);

                ImInt cW = new ImInt(options.getClientWidth());
                ImInt cH = new ImInt(options.getClientHeight());

                ImGui.setNextItemWidth(100);
                if (ImGui.inputInt(org.argentumforge.engine.i18n.I18n.INSTANCE.get("options.clientArea.width"), cW)) {
                    if (cW.get() < 9)
                        cW.set(9); // Min limit
                    options.setClientWidth(cW.get());
                    // Unify: Viewport size always follows Client size
                    options.getRenderSettings().setViewportWidth(cW.get());
                    options.save();
                }

                // Tooltip for Width/Height
                if (ImGui.isItemHovered()) {
                    ImGui.setTooltip(org.argentumforge.engine.i18n.I18n.INSTANCE.get("options.clientArea.tooltip"));
                }

                ImGui.setNextItemWidth(100);
                if (ImGui.inputInt(org.argentumforge.engine.i18n.I18n.INSTANCE.get("options.clientArea.height"), cH)) {
                    if (cH.get() < 9)
                        cH.set(9); // Min limit
                    options.setClientHeight(cH.get());
                    // Unify: Viewport size always follows Client size
                    options.getRenderSettings().setViewportHeight(cH.get());
                    options.save();
                }

                // Tooltip for Width/Height (Same tooltip for consistency)
                if (ImGui.isItemHovered()) {
                    ImGui.setTooltip(org.argentumforge.engine.i18n.I18n.INSTANCE.get("options.clientArea.tooltip"));
                }

                // Move Viewport Toggle Here
                ImGui.dummy(0, 5);
                boolean showViewport = options.getRenderSettings().isShowViewportOverlay();
                if (ImGui.checkbox(org.argentumforge.engine.i18n.I18n.INSTANCE.get("options.viewport.enable"),
                        showViewport)) {
                    options.getRenderSettings().setShowViewportOverlay(!showViewport);
                    options.save();
                }
                // Optional: Allow changing color if desired, but maybe keep it simple as
                // requested "unify".
                // Let's keep the color picker here too for completeness, or just the toggle if
                // the user just wanted the "option".
                // The user said "trasladar esa opcion" (singular), but usually color goes with
                // it.
                // I will add the color picker below the checkbox for completeness.
                ImGui.sameLine();
                float[] vColor = options.getRenderSettings().getViewportColor();
                if (ImGui.colorEdit4(org.argentumforge.engine.i18n.I18n.INSTANCE.get("options.viewport.color"),
                        vColor,
                        imgui.flag.ImGuiColorEditFlags.NoInputs | imgui.flag.ImGuiColorEditFlags.AlphaPreview)) {
                    options.getRenderSettings().setViewportColor(vColor);
                    options.save();
                }

                ImGui.spacing();
                ImGui.spacing();
                ImGui.separator();

                // --- SECCIÓN: MOTOR Y RENDIMIENTO ---
                ImGui.textColored(ImGui.getColorU32(0.2f, 0.7f, 1.0f, 1.0f),
                        org.argentumforge.engine.i18n.I18n.INSTANCE.get("options.tab.simulation") + ":");
                ImGui.dummy(0, 5);

                // Fullscreen
                if (ImGui.checkbox(
                        org.argentumforge.engine.i18n.I18n.INSTANCE.get("options.graphics") + " (Fullscreen)",
                        options.isFullscreen())) {
                    options.setFullscreen(!options.isFullscreen());
                    org.argentumforge.engine.Engine.INSTANCE.getWindow().toggleWindow();
                }

                ImGui.sameLine(250);

                // VSync
                if (ImGui.checkbox("VSync", options.isVsync())) {
                    options.setVsync(!options.isVsync());
                    org.argentumforge.engine.Engine.INSTANCE.getWindow().toggleWindow();
                }

                // Disable Animations
                if (ImGui.checkbox(
                        org.argentumforge.engine.i18n.I18n.INSTANCE.get("options.graphics.disableAnimations"),
                        options.getRenderSettings().isDisableAnimations())) {
                    options.getRenderSettings()
                            .setDisableAnimations(!options.getRenderSettings().isDisableAnimations());
                    options.save();
                }

                ImGui.endTabItem();
            }

            // --- TAB: GUÍAS VISUALES ---
            if (ImGui.beginTabItem(org.argentumforge.engine.i18n.I18n.INSTANCE.get("options.grid.guides", "Guías"))) {
                org.argentumforge.engine.renderer.RenderSettings settings = options.getRenderSettings();

                ImGui.dummy(0, 10);

                // --- REJILLA ---
                ImGui.textColored(ImGui.getColorU32(0.2f, 0.7f, 1.0f, 1.0f),
                        org.argentumforge.engine.i18n.I18n.INSTANCE.get("options.grid.title") + ":");
                ImGui.separator();

                if (ImGui.checkbox(org.argentumforge.engine.i18n.I18n.INSTANCE.get("menu.view.grid"),
                        settings.isShowGrid())) {
                    settings.setShowGrid(!settings.isShowGrid());
                    options.save();
                }

                ImGui.sameLine();
                if (ImGui.checkbox(org.argentumforge.engine.i18n.I18n.INSTANCE.get("options.grid.adaptive"),
                        settings.isAdaptiveGrid())) {
                    settings.setAdaptiveGrid(!settings.isAdaptiveGrid());
                    options.save();
                }

                float[] gColor = settings.getGridColor();
                if (ImGui.colorEdit4(org.argentumforge.engine.i18n.I18n.INSTANCE.get("options.grid.color"), gColor)) {
                    settings.setGridColor(gColor);
                    options.save();
                }

                ImGui.spacing();

                // --- REJILLA MAYOR ---
                if (ImGui.checkbox(org.argentumforge.engine.i18n.I18n.INSTANCE.get("options.grid.showMajor"),
                        settings.isShowMajorGrid())) {
                    settings.setShowMajorGrid(!settings.isShowMajorGrid());
                    options.save();
                }

                ImGui.setNextItemWidth(100);
                ImInt mInterval = new ImInt(settings.getGridMajorInterval());
                if (ImGui.inputInt(org.argentumforge.engine.i18n.I18n.INSTANCE.get("options.grid.majorInterval"),
                        mInterval)) {
                    if (mInterval.get() < 2)
                        mInterval.set(2);
                    settings.setGridMajorInterval(mInterval.get());
                    options.save();
                }

                float[] gmColor = settings.getGridMajorColor();
                if (ImGui.colorEdit4(org.argentumforge.engine.i18n.I18n.INSTANCE.get("options.grid.majorColor"),
                        gmColor)) {
                    settings.setGridMajorColor(gmColor);
                    options.save();
                }

                ImGui.dummy(0, 10);

                // --- VIEWPORT (Moved to Graphics) ---
                // Kept empty placeholder or just remove.
                // Removing entire section.

                ImGui.dummy(0, 10);

                // --- INDICADORES Y ESTILOS ---
                ImGui.textColored(ImGui.getColorU32(0.2f, 0.7f, 1.0f, 1.0f), "Estilos de Indicadores:");
                ImGui.separator();

                // 1. BLOQUEOS
                ImGui.text("Bloqueos:");
                float[] blockAlpha = { settings.getBlockOpacity() };
                ImGui.setNextItemWidth(150);
                if (ImGui.sliderFloat(org.argentumforge.engine.i18n.I18n.INSTANCE.get("options.visual.opacity.blocks"),
                        blockAlpha, 0.0f, 1.0f)) {
                    settings.setBlockOpacity(blockAlpha[0]);
                    options.save();
                }

                ImGui.sameLine();
                ImGui.setNextItemWidth(200);
                org.argentumforge.engine.renderer.RenderSettings.IndicatorStyle currentBlockStyle = settings
                        .getBlockIndicatorStyle();
                String currentBlockStyleName = org.argentumforge.engine.i18n.I18n.INSTANCE
                        .get("options.visual.indicator." + currentBlockStyle.name());
                if (ImGui.beginCombo("##blockStyle", currentBlockStyleName)) {
                    for (org.argentumforge.engine.renderer.RenderSettings.IndicatorStyle style : org.argentumforge.engine.renderer.RenderSettings.IndicatorStyle
                            .values()) {
                        boolean isSelected = (style == currentBlockStyle);
                        String name = org.argentumforge.engine.i18n.I18n.INSTANCE
                                .get("options.visual.indicator." + style.name());
                        if (ImGui.selectable(name, isSelected)) {
                            settings.setBlockIndicatorStyle(style);
                            options.save();
                        }
                        if (isSelected)
                            ImGui.setItemDefaultFocus();
                    }
                    ImGui.endCombo();
                }

                // 2. TRASLADOS
                ImGui.dummy(0, 5);
                ImGui.text("Traslados:");
                float[] transferAlpha = { settings.getTransferOpacity() };
                ImGui.setNextItemWidth(150);
                if (ImGui.sliderFloat(
                        org.argentumforge.engine.i18n.I18n.INSTANCE.get("options.visual.opacity.transfers"),
                        transferAlpha, 0.0f, 1.0f)) {
                    settings.setTransferOpacity(transferAlpha[0]);
                    options.save();
                }

                ImGui.sameLine();
                ImGui.setNextItemWidth(200);
                org.argentumforge.engine.renderer.RenderSettings.IndicatorStyle currentTransferStyle = settings
                        .getTransferIndicatorStyle();
                String currentTransferStyleName = org.argentumforge.engine.i18n.I18n.INSTANCE
                        .get("options.visual.indicator." + currentTransferStyle.name());
                if (ImGui.beginCombo("##transferStyle", currentTransferStyleName)) {
                    for (org.argentumforge.engine.renderer.RenderSettings.IndicatorStyle style : org.argentumforge.engine.renderer.RenderSettings.IndicatorStyle
                            .values()) {
                        boolean isSelected = (style == currentTransferStyle);
                        String name = org.argentumforge.engine.i18n.I18n.INSTANCE
                                .get("options.visual.indicator." + style.name());
                        if (ImGui.selectable(name, isSelected)) {
                            settings.setTransferIndicatorStyle(style);
                            options.save();
                        }
                        if (isSelected)
                            ImGui.setItemDefaultFocus();
                    }
                    ImGui.endCombo();
                }

                ImGui.dummy(0, 10);

                // --- OTROS ---
                ImGui.textColored(ImGui.getColorU32(0.2f, 0.7f, 1.0f, 1.0f), "Otros:");
                ImGui.separator();
                // Effecto Respiración (Moved here from Graphics)
                if (ImGui.checkbox(org.argentumforge.engine.i18n.I18n.INSTANCE.get("options.visual.breathing"),
                        settings.isShowNpcBreathing())) {
                    settings.setShowNpcBreathing(!settings.isShowNpcBreathing());
                    options.save();
                }

                // Ghost Alpha
                float[] ghostAlpha = { settings.getGhostOpacity() };
                ImGui.setNextItemWidth(150);
                if (ImGui.sliderFloat(org.argentumforge.engine.i18n.I18n.INSTANCE.get("options.paste.ghostAlpha"),
                        ghostAlpha, 0.0f, 1.0f)) {
                    settings.setGhostOpacity(ghostAlpha[0]);
                    options.save();
                }

                ImGui.endTabItem();
            }

            ImGui.endTabBar();
        }

        // Bottom Actions
        ImGui.dummy(0, 20);
        ImGui.separator();
        ImGui.dummy(0, 10);

        this.drawButtons();

        ImGui.end();
    }

    private void drawButtons() {
        // Calcular ancho total para centrar
        float buttonWidth = 120;
        float spacing = 10;
        float totalWidth = (buttonWidth * 3) + (spacing * 2);

        float startX = (ImGui.getWindowWidth() - totalWidth) / 2;

        // Botón Teclas
        ImGui.setCursorPosX(startX);
        if (ImGui.button(org.argentumforge.engine.i18n.I18n.INSTANCE.get("options.keys"), buttonWidth, 25)) {
            org.argentumforge.engine.gui.ImGUISystem.INSTANCE.show(new FBindKeys());
        }

        ImGui.sameLine();

        // Botón Rutas
        if (ImGui.button(org.argentumforge.engine.i18n.I18n.INSTANCE.get("options.paths"), buttonWidth, 25)) {
            org.argentumforge.engine.gui.ImGUISystem.INSTANCE.show(new FRoutes());
        }

        ImGui.sameLine();

        // Botón Cerrar
        if (ImGui.button(org.argentumforge.engine.i18n.I18n.INSTANCE.get("common.close"), buttonWidth, 25)) {
            options.save();
            this.close();
        }
    }

}
