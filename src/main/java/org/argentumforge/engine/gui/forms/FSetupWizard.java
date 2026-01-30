package org.argentumforge.engine.gui.forms;

import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImString;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import org.argentumforge.engine.game.Options;
import org.argentumforge.engine.i18n.I18n;

import java.io.File;

/**
 * Asistente de configuración inicial para primera ejecución.
 * Guía al usuario a través de la configuración de rutas y preferencias básicas.
 */
public final class FSetupWizard extends Form {

    private static final int STEP_WELCOME = 0;
    private static final int STEP_PROFILE = 1; // Nuevo paso
    private static final int STEP_ROUTES = 2;
    private static final int STEP_PREFERENCES = 3;
    private static final int STEP_MINIMAP = 4;
    private static final int STEP_CONFIRMATION = 5;

    private int currentStep = STEP_WELCOME;
    private final Runnable onComplete;
    private final boolean isFirstRun;

    // Campos de configuración
    private final ImString profileName = new ImString(64); // Nuevo campo
    private final ImString graphicsPath = new ImString(256);
    private final ImString datsPath = new ImString(256);
    private final ImString initsPath = new ImString(256);
    private final ImString musicPath = new ImString(256);

    private final ImBoolean musicEnabled = new ImBoolean(true);
    private final ImBoolean soundEnabled = new ImBoolean(true);
    private final ImBoolean fullscreen = new ImBoolean(false);

    private final ImInt resolutionIndex = new ImInt(0);
    private final String[] resolutions = { "1366x768", "1920x1080", "2560x1440", "1280x720" };

    private final ImInt languageIndex = new ImInt(0);
    private final String[] languages;
    private final String[] languageNames;

    private final ImInt clientWidth = new ImInt(13);
    private final ImInt clientHeight = new ImInt(11);

    private final ImInt styleIndex = new ImInt(0);
    private final String[] styles = { "MODERN", "DARK", "CLASSIC", "LIGHT" };

    private final ImBoolean generateMinimap = new ImBoolean(true);

    private final ImBoolean windowOpen = new ImBoolean(true);
    private boolean showExitConfirmation = false;

    private String errorMessage = "";

    /**
     * Constructor del wizard.
     * 
     * @param onComplete Callback a ejecutar cuando se complete la configuración
     * @param isFirstRun Si es true, no permite cerrar el wizard con la X
     */
    public FSetupWizard(Runnable onComplete, boolean isFirstRun) {
        this.onComplete = onComplete;
        this.isFirstRun = isFirstRun;

        // Obtener idiomas disponibles desde I18n (combina locales y defaults)
        java.util.List<String> langList = I18n.INSTANCE.getAvailableLanguages();
        java.util.List<String> langNameList = new java.util.ArrayList<>();

        for (String langCode : langList) {
            langNameList.add(I18n.INSTANCE.getLanguageName(langCode));
        }

        this.languages = langList.toArray(new String[0]);
        this.languageNames = langNameList.toArray(new String[0]);

        // Cargar valores actuales de Options si existen
        Options opts = Options.INSTANCE;
        graphicsPath.set(opts.getGraphicsPath());
        datsPath.set(opts.getDatsPath());
        initsPath.set(opts.getInitPath());
        musicPath.set(opts.getMusicPath());
        musicEnabled.set(opts.isMusic());
        soundEnabled.set(opts.isSound());
        fullscreen.set(opts.isFullscreen());

        clientWidth.set(opts.getClientWidth());
        clientHeight.set(opts.getClientHeight());

        // Detectar idioma actual
        String currentLang = opts.getLanguage();
        for (int i = 0; i < languages.length; i++) {
            if (languages[i].equals(currentLang)) {
                languageIndex.set(i);
                break;
            }
        }

        // Detectar resolución actual
        String currentRes = opts.getScreenWidth() + "x" + opts.getScreenHeight();
        for (int i = 0; i < resolutions.length; i++) {
            if (resolutions[i].equals(currentRes)) {
                resolutionIndex.set(i);
                break;
            }
        }

        // Detectar estilo actual
        String currentStyle = opts.getVisualTheme();
        for (int i = 0; i < styles.length; i++) {
            if (styles[i].equals(currentStyle)) {
                styleIndex.set(i);
                break;
            }
        }
    }

    private org.argentumforge.engine.renderer.Texture backgroundTexture;

    @Override
    public void render() {
        if (backgroundTexture == null) {
            try {
                this.backgroundTexture = org.argentumforge.engine.renderer.Surface.INSTANCE
                        .createTexture("VentanaInicio.jpg", false);
                if (this.backgroundTexture != null) {
                    org.argentumforge.engine.Window.INSTANCE.updateResolution(
                            this.backgroundTexture.getTex_width(),
                            this.backgroundTexture.getTex_height());
                }
            } catch (Exception e) {
                // Ignore
            }
        }

        if (backgroundTexture != null) {
            ImGui.getBackgroundDrawList().addImage(
                    backgroundTexture.getId(),
                    0, 0,
                    org.argentumforge.engine.Window.INSTANCE.getWidth(),
                    org.argentumforge.engine.Window.INSTANCE.getHeight());
        }

        // Tamaño adaptativo según el paso actual
        int windowHeight = switch (currentStep) {
            case STEP_WELCOME -> 250;
            case STEP_PROFILE -> 200;
            case STEP_ROUTES -> 350;
            case STEP_PREFERENCES -> 450;
            case STEP_MINIMAP -> 320;
            case STEP_CONFIRMATION -> 400;
            default -> 400;
        };

        ImGui.setNextWindowSize(650, windowHeight, ImGuiCond.Always);
        ImGui.setNextWindowPos(
                (org.argentumforge.engine.Window.INSTANCE.getWidth() - 650) / 2f,
                (org.argentumforge.engine.Window.INSTANCE.getHeight() - windowHeight) * 0.70f,
                ImGuiCond.Always);

        int flags = ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoScrollbar
                | ImGuiWindowFlags.NoScrollWithMouse;

        if (ImGui.begin(I18n.INSTANCE.get("wizard.title"), windowOpen, flags)) {
            switch (currentStep) {
                case STEP_WELCOME -> renderWelcome();
                case STEP_PROFILE -> renderProfile();
                case STEP_ROUTES -> renderRoutes();
                case STEP_PREFERENCES -> renderPreferences();
                case STEP_MINIMAP -> renderMinimap();
                case STEP_CONFIRMATION -> renderConfirmation();
            }

            // Mostrar error si hay
            if (!errorMessage.isEmpty()) {
                ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, 1.0f, 0.0f, 0.0f, 1.0f);
                ImGui.text(errorMessage);
                ImGui.popStyleColor();
            }

            ImGui.separator();
            renderNavigation();
        }
        ImGui.end();

        // Detectar si el usuario cerró la ventana con la X
        if (!windowOpen.get()) {
            if (isFirstRun) {
                windowOpen.set(true);
                showExitConfirmation = true;
            } else {
                this.close();
            }
        }
    }

    private void renderProfile() {
        ImGui.text(I18n.INSTANCE.get("wizard.profile.title"));
        ImGui.separator();
        ImGui.spacing();

        ImGui.textWrapped(I18n.INSTANCE.get("wizard.profile.description"));
        ImGui.spacing();

        ImGui.text(I18n.INSTANCE.get("wizard.profile.name"));
        ImGui.pushItemWidth(400);
        ImGui.inputText("##proname", profileName);
        ImGui.popItemWidth();
    }

    private void renderWelcome() {
        ImGui.textWrapped(I18n.INSTANCE.get("wizard.welcome.message"));
        ImGui.spacing();
        ImGui.spacing();
        ImGui.textWrapped(I18n.INSTANCE.get("wizard.welcome.description"));
        ImGui.spacing();
        ImGui.spacing();

        ImGui.text(I18n.INSTANCE.get("wizard.prefs.language"));
        if (ImGui.combo("##language", languageIndex, languageNames)) {
            // Recargar idioma inmediatamente al cambiar la selección
            String selectedLang = languages[languageIndex.get()];
            I18n.INSTANCE.loadLanguage(selectedLang);
        }
    }

    private void renderRoutes() {
        ImGui.text(I18n.INSTANCE.get("wizard.routes.title"));
        ImGui.separator();
        ImGui.spacing();
        renderPathField("wizard.routes.graphics", graphicsPath);
        renderPathField("wizard.routes.dats", datsPath);
        renderPathField("wizard.routes.inits", initsPath);
        renderPathField("wizard.routes.music", musicPath);
    }

    private void renderPathField(String labelKey, ImString pathField) {
        ImGui.text(I18n.INSTANCE.get(labelKey));
        ImGui.pushItemWidth(500);
        ImGui.inputText("##" + labelKey, pathField);
        ImGui.popItemWidth();
        ImGui.sameLine();
        if (ImGui.button(I18n.INSTANCE.get("wizard.routes.browse") + "##" + labelKey, 60, 0)) {
            String defaultPath = pathField.get();
            if (defaultPath.isEmpty())
                defaultPath = "";
            String selected = org.argentumforge.engine.gui.FileDialog.selectFolder(I18n.INSTANCE.get(labelKey),
                    defaultPath);
            if (selected != null) {
                pathField.set(selected);
            }
        }
        ImGui.spacing();
    }

    private void renderPreferences() {
        ImGui.text(I18n.INSTANCE.get("wizard.prefs.title"));
        ImGui.separator();
        ImGui.spacing();

        ImGui.text(I18n.INSTANCE.get("wizard.prefs.resolution"));
        ImGui.combo("##resolution", resolutionIndex, resolutions);
        ImGui.spacing();

        ImGui.text(I18n.INSTANCE.get("wizard.prefs.clientSize"));
        ImGui.pushItemWidth(100);
        ImGui.inputInt(I18n.INSTANCE.get("wizard.prefs.width"), clientWidth);
        ImGui.inputInt(I18n.INSTANCE.get("wizard.prefs.height"), clientHeight);
        ImGui.popItemWidth();
        ImGui.spacing();

        ImGui.checkbox(I18n.INSTANCE.get("wizard.prefs.music"), musicEnabled);
        ImGui.checkbox(I18n.INSTANCE.get("wizard.prefs.sound"), soundEnabled);
        ImGui.spacing();

        ImGui.checkbox(I18n.INSTANCE.get("wizard.prefs.fullscreen"), fullscreen);
        ImGui.spacing();

        ImGui.text(I18n.INSTANCE.get("options.visualTheme"));
        if (ImGui.combo("##visualStyle", styleIndex, styles)) {
            // Aplicar estilo inmediatamente para previsualización
            org.argentumforge.engine.gui.Theme.applyStyle(
                    org.argentumforge.engine.gui.Theme.StyleType.valueOf(styles[styleIndex.get()]));
        }
    }

    private void renderMinimap() {
        ImGui.text(I18n.INSTANCE.get("wizard.minimap.title"));
        ImGui.separator();
        ImGui.spacing();

        ImGui.textWrapped(I18n.INSTANCE.get("wizard.minimap.description"));
        ImGui.spacing();
        ImGui.spacing();

        ImGui.checkbox(I18n.INSTANCE.get("wizard.minimap.generate"), generateMinimap);

        if (!generateMinimap.get()) {
            ImGui.spacing();
            ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, 1.0f, 1.0f, 0.0f, 1.0f);
            ImGui.textWrapped(I18n.INSTANCE.get("wizard.minimap.skip"));
            ImGui.popStyleColor();
        }
    }

    private void renderConfirmation() {
        ImGui.text(I18n.INSTANCE.get("wizard.confirm.title"));
        ImGui.separator();
        ImGui.spacing();

        ImGui.bulletText(I18n.INSTANCE.get("wizard.profile.name") + ": " + profileName.get());
        ImGui.spacing();

        ImGui.bulletText(I18n.INSTANCE.get("wizard.routes.graphics") + " " + graphicsPath.get());
        ImGui.bulletText(I18n.INSTANCE.get("wizard.routes.dats") + " " + datsPath.get());
        ImGui.bulletText(I18n.INSTANCE.get("wizard.routes.inits") + " " + initsPath.get());
        ImGui.bulletText(I18n.INSTANCE.get("wizard.routes.music") + " " + musicPath.get());
        ImGui.spacing();
        ImGui.bulletText(I18n.INSTANCE.get("wizard.prefs.language") + " " + languageNames[languageIndex.get()]);
        ImGui.bulletText(I18n.INSTANCE.get("wizard.prefs.resolution") + " " + resolutions[resolutionIndex.get()]);
        ImGui.bulletText(
                I18n.INSTANCE.get("wizard.prefs.clientSize") + " " + clientWidth.get() + "x" + clientHeight.get());
        ImGui.bulletText(I18n.INSTANCE.get("options.visualTheme") + ": " + styles[styleIndex.get()]);
        ImGui.spacing();
        ImGui.bulletText(I18n.INSTANCE.get("wizard.minimap.generate") + ": " + (generateMinimap.get() ? "Sí" : "No"));
    }

    private void renderNavigation() {
        float buttonWidth = 120;

        if (currentStep > STEP_WELCOME) {
            if (ImGui.button(I18n.INSTANCE.get("common.back"), buttonWidth, 0)) {
                currentStep--;
                errorMessage = "";
            }
        } else {
            ImGui.dummy(buttonWidth, 0);
        }

        ImGui.sameLine();

        float availWidth = ImGui.getContentRegionAvailX() - buttonWidth * 2 - ImGui.getStyle().getItemSpacingX();
        if (availWidth > 0) {
            ImGui.dummy(availWidth, 0);
            ImGui.sameLine();
        }

        if (currentStep == STEP_CONFIRMATION) {
            if (ImGui.button(I18n.INSTANCE.get("common.finish"), buttonWidth, 0)) {
                finish();
            }
        } else {
            if (ImGui.button(I18n.INSTANCE.get("common.next"), buttonWidth, 0)) {
                if (validateCurrentStep()) {
                    currentStep++;
                    errorMessage = "";
                }
            }
        }

        ImGui.sameLine();

        if (!isFirstRun) {
            if (ImGui.button(I18n.INSTANCE.get("common.cancel"), buttonWidth, 0)) {
                this.close();
            }
        }

        renderExitConfirmation();
    }

    private void renderExitConfirmation() {
        if (showExitConfirmation) {
            ImGui.openPopup(I18n.INSTANCE.get("wizard.exit.title"));
        }

        if (ImGui.beginPopupModal(I18n.INSTANCE.get("wizard.exit.title"), ImGuiWindowFlags.AlwaysAutoResize)) {
            ImGui.text(I18n.INSTANCE.get("wizard.exit.message"));
            ImGui.spacing();
            ImGui.separator();
            ImGui.spacing();

            if (ImGui.button(I18n.INSTANCE.get("wizard.exit.confirm"), 120, 0)) {
                System.exit(0);
            }

            ImGui.sameLine();

            if (ImGui.button(I18n.INSTANCE.get("common.cancel"), 120, 0)) {
                showExitConfirmation = false;
                ImGui.closeCurrentPopup();
            }

            ImGui.endPopup();
        }
    }

    private boolean validateCurrentStep() {
        if (currentStep == STEP_PROFILE) {
            if (profileName.get().trim().isEmpty()) {
                errorMessage = I18n.INSTANCE.get("wizard.profile.error.empty");
                return false;
            }
        }

        if (currentStep == STEP_ROUTES) {
            if (graphicsPath.get().isEmpty()) {
                errorMessage = I18n.INSTANCE.get("wizard.error.emptyPath") + ": "
                        + I18n.INSTANCE.get("wizard.routes.graphics");
                return false;
            }
            if (datsPath.get().isEmpty()) {
                errorMessage = I18n.INSTANCE.get("wizard.error.emptyPath") + ": "
                        + I18n.INSTANCE.get("wizard.routes.dats");
                return false;
            }
            if (initsPath.get().isEmpty()) {
                errorMessage = I18n.INSTANCE.get("wizard.error.emptyPath") + ": "
                        + I18n.INSTANCE.get("wizard.routes.inits");
                return false;
            }
            if (musicPath.get().isEmpty()) {
                errorMessage = I18n.INSTANCE.get("wizard.error.emptyPath") + ": "
                        + I18n.INSTANCE.get("wizard.routes.music");
                return false;
            }

            if (!new File(graphicsPath.get()).exists()) {
                errorMessage = I18n.INSTANCE.get("wizard.error.invalidPath") + ": "
                        + I18n.INSTANCE.get("wizard.routes.graphics");
                return false;
            }
            if (!new File(datsPath.get()).exists()) {
                errorMessage = I18n.INSTANCE.get("wizard.error.invalidPath") + ": "
                        + I18n.INSTANCE.get("wizard.routes.dats");
                return false;
            }
            if (!new File(initsPath.get()).exists()) {
                errorMessage = I18n.INSTANCE.get("wizard.error.invalidPath") + ": "
                        + I18n.INSTANCE.get("wizard.routes.inits");
                return false;
            }
            if (!new File(musicPath.get()).exists()) {
                errorMessage = I18n.INSTANCE.get("wizard.error.invalidPath") + ": "
                        + I18n.INSTANCE.get("wizard.routes.music");
                return false;
            }
        }

        return true;
    }

    private void finish() {
        // Crear perfil nuevo mediante ProfileManager
        String pName = profileName.get().trim();
        if (pName.isEmpty())
            pName = I18n.INSTANCE.get("wizard.profile.defaultName");

        org.argentumforge.engine.utils.Profile newProfile = org.argentumforge.engine.utils.ProfileManager.INSTANCE
                .createProfile(pName);

        org.argentumforge.engine.utils.ProfileManager.INSTANCE.setCurrentProfile(newProfile);

        // Configurar Options con la ruta del nuevo perfil
        Options opts = Options.INSTANCE;
        opts.setConfigPath(newProfile.getConfigPath());

        // Setear valores
        opts.setGraphicsPath(graphicsPath.get());
        opts.setDatsPath(datsPath.get());
        opts.setInitPath(initsPath.get());
        opts.setMusicPath(musicPath.get());
        opts.setMusic(musicEnabled.get());
        opts.setSound(soundEnabled.get());
        opts.setFullscreen(fullscreen.get());

        opts.setClientWidth(clientWidth.get());
        opts.setClientHeight(clientHeight.get());
        opts.setLanguage(languages[languageIndex.get()]);

        String[] res = resolutions[resolutionIndex.get()].split("x");
        opts.setScreenWidth(Integer.parseInt(res[0]));
        opts.setScreenHeight(Integer.parseInt(res[1]));

        opts.setVisualTheme(styles[styleIndex.get()]);

        // Guardar en el archivo del perfil
        opts.save();

        // Inicializar
        org.argentumforge.engine.utils.GameData.init();

        if (generateMinimap.get()) {
            org.argentumforge.engine.utils.editor.MinimapColorGenerator.generateBinary();
        }

        this.close();

        if (onComplete != null) {
            onComplete.run();
        }
    }
}