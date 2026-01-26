package org.argentumforge.engine.gui.forms;

import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImString;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import org.argentumforge.engine.game.Options;
import org.argentumforge.engine.gui.widgets.ImGuiFolderPicker;
import org.argentumforge.engine.i18n.I18n;
import org.argentumforge.engine.utils.FileChooserUtil;

import java.io.File;

/**
 * Asistente de configuración inicial para primera ejecución.
 * Guía al usuario a través de la configuración de rutas y preferencias básicas.
 */
public final class FSetupWizard extends Form {

    private ImGuiFolderPicker folderPicker;
    private ImString pendingPathTarget;

    private static final int STEP_WELCOME = 0;
    private static final int STEP_ROUTES = 1;
    private static final int STEP_PREFERENCES = 2;
    private static final int STEP_MINIMAP = 3;
    private static final int STEP_CONFIRMATION = 4;

    private int currentStep = STEP_WELCOME;
    private final Runnable onComplete;
    private final boolean isFirstRun;

    // Campos de configuración
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

    private final ImBoolean generateMinimap = new ImBoolean(true);
    private final ImBoolean graphicCursor = new ImBoolean(false);
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

        // Detectar idiomas disponibles en la carpeta lang/
        java.util.List<String> langList = new java.util.ArrayList<>();
        java.util.List<String> langNameList = new java.util.ArrayList<>();

        java.io.File langDir = new java.io.File("lang");
        if (langDir.exists() && langDir.isDirectory()) {
            java.io.File[] files = langDir.listFiles((dir, name) -> name.endsWith(".properties"));
            if (files != null) {
                for (java.io.File file : files) {
                    String fileName = file.getName();
                    String langCode = fileName.replace(".properties", "");
                    langList.add(langCode);

                    // Mapear códigos a nombres legibles
                    String displayName = switch (langCode) {
                        case "es_ES" -> "Español";
                        case "en_US" -> "English";
                        case "pt_BR" -> "Português (Brasil)";
                        default -> langCode; // Fallback al código si no está mapeado
                    };
                    langNameList.add(displayName);
                }
            }
        }

        // Si no se encontraron idiomas, usar fallback
        if (langList.isEmpty()) {
            langList.add("es_ES");
            langList.add("en_US");
            langNameList.add("Español");
            langNameList.add("English");
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
        graphicCursor.set(opts.isCursorGraphic());
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
    }

    @Override
    public void render() {
        // Tamaño adaptativo según el paso actual
        int windowHeight = switch (currentStep) {
            case STEP_WELCOME -> 250;
            case STEP_ROUTES -> 350;
            case STEP_PREFERENCES -> 420;
            case STEP_MINIMAP -> 320;
            case STEP_CONFIRMATION -> 380;
            default -> 400;
        };

        ImGui.setNextWindowSize(650, windowHeight, ImGuiCond.Always);
        ImGui.setNextWindowPos(
                (org.argentumforge.engine.Window.INSTANCE.getWidth() - 650) / 2f,
                (org.argentumforge.engine.Window.INSTANCE.getHeight() - windowHeight) / 2f,
                ImGuiCond.Always);

        int flags = ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoResize;
        // No se puede evitar que cierren con X en ImGui, pero se puede ignorar

        if (ImGui.begin(I18n.INSTANCE.get("wizard.title"), windowOpen, flags)) {
            switch (currentStep) {
                case STEP_WELCOME -> renderWelcome();
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
                // Durante primera ejecución, mostrar confirmación
                windowOpen.set(true); // Reabrir la ventana
                showExitConfirmation = true;
            } else {
                // Durante ejecución manual, simplemente cerrar
                this.close();
            }
        }

        if (folderPicker != null && folderPicker.isOpen()) {
            folderPicker.render();

            File selected = folderPicker.getSelectedDir();
            if (selected != null && pendingPathTarget != null) {
                pendingPathTarget.set(selected.getAbsolutePath());
                pendingPathTarget = null;
                folderPicker = null;
            }
        }
    }

    private void renderWelcome() {
        ImGui.textWrapped(I18n.INSTANCE.get("wizard.welcome.message"));
        ImGui.spacing();
        ImGui.spacing();
        ImGui.textWrapped(I18n.INSTANCE.get("wizard.welcome.description"));
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


        if (ImGui.button(I18n.INSTANCE.get("wizard.routes.browse") + "##" + labelKey)) {
            folderPicker = new ImGuiFolderPicker(
                    pathField.get().isEmpty()
                            ? new File(System.getProperty("user.home"))
                            : new File(pathField.get())
            );
            folderPicker.open();
            pendingPathTarget = pathField;
        }

        ImGui.spacing();
    }

    private void renderPreferences() {
        ImGui.text(I18n.INSTANCE.get("wizard.prefs.title"));
        ImGui.separator();
        ImGui.spacing();

        // Idioma
        ImGui.text(I18n.INSTANCE.get("wizard.prefs.language"));
        ImGui.combo("##language", languageIndex, languageNames);
        ImGui.spacing();

        // Resolución
        ImGui.text(I18n.INSTANCE.get("wizard.prefs.resolution"));
        ImGui.combo("##resolution", resolutionIndex, resolutions);
        ImGui.spacing();

        // Dimensiones del cliente
        ImGui.text(I18n.INSTANCE.get("wizard.prefs.clientSize"));
        ImGui.pushItemWidth(100);
        ImGui.inputInt("Width (tiles)", clientWidth);
        ImGui.inputInt("Height (tiles)", clientHeight);
        ImGui.popItemWidth();
        ImGui.spacing();

        // Opciones de audio
        ImGui.checkbox(I18n.INSTANCE.get("wizard.prefs.music"), musicEnabled);
        ImGui.checkbox(I18n.INSTANCE.get("wizard.prefs.sound"), soundEnabled);
        ImGui.spacing();

        // Pantalla completa
        ImGui.checkbox(I18n.INSTANCE.get("wizard.prefs.fullscreen"), fullscreen);

        // Cursor gráfico
        ImGui.checkbox(I18n.INSTANCE.get("wizard.prefs.graphicCursor"), graphicCursor);
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

        ImGui.bulletText(I18n.INSTANCE.get("wizard.routes.graphics") + " " + graphicsPath.get());
        ImGui.bulletText(I18n.INSTANCE.get("wizard.routes.dats") + " " + datsPath.get());
        ImGui.bulletText(I18n.INSTANCE.get("wizard.routes.inits") + " " + initsPath.get());
        ImGui.bulletText(I18n.INSTANCE.get("wizard.routes.music") + " " + musicPath.get());
        ImGui.spacing();
        ImGui.bulletText(I18n.INSTANCE.get("wizard.prefs.language") + " " + languageNames[languageIndex.get()]);
        ImGui.bulletText(I18n.INSTANCE.get("wizard.prefs.resolution") + " " + resolutions[resolutionIndex.get()]);
        ImGui.bulletText(
                I18n.INSTANCE.get("wizard.prefs.clientSize") + " " + clientWidth.get() + "x" + clientHeight.get());
        ImGui.spacing();
        ImGui.bulletText(I18n.INSTANCE.get("wizard.minimap.generate") + ": " + (generateMinimap.get() ? "Sí" : "No"));
    }

    private void renderNavigation() {
        float buttonWidth = 120;

        // Botón "Atrás"
        if (currentStep > STEP_WELCOME) {
            if (ImGui.button(I18n.INSTANCE.get("common.back"), buttonWidth, 0)) {
                currentStep--;
                errorMessage = "";
            }
        } else {
            ImGui.dummy(buttonWidth, 0);
        }

        ImGui.sameLine();

        // Espacio flexible
        float availWidth = ImGui.getContentRegionAvailX() - buttonWidth * 2 - ImGui.getStyle().getItemSpacingX();
        if (availWidth > 0) {
            ImGui.dummy(availWidth, 0);
            ImGui.sameLine();
        }

        // Botón "Siguiente" o "Finalizar"
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

        // Botón "Cancelar" (solo si no es primera ejecución)
        if (!isFirstRun) {
            if (ImGui.button(I18n.INSTANCE.get("common.cancel"), buttonWidth, 0)) {
                this.close();
            }
        }

        // Modal de confirmación de salida
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
        if (currentStep == STEP_ROUTES) {
            // Validar que las rutas no estén vacías y existan
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

            // Validar que las rutas existan
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
        // Guardar configuración en Options
        Options opts = Options.INSTANCE;
        opts.setGraphicsPath(graphicsPath.get());
        opts.setDatsPath(datsPath.get());
        opts.setInitPath(initsPath.get());
        opts.setMusicPath(musicPath.get());
        opts.setMusic(musicEnabled.get());
        opts.setSound(soundEnabled.get());
        opts.setFullscreen(fullscreen.get());
        opts.setCursorGraphic(graphicCursor.get());
        opts.setClientWidth(clientWidth.get());
        opts.setClientHeight(clientHeight.get());
        opts.setLanguage(languages[languageIndex.get()]);

        // Parsear resolución
        String[] res = resolutions[resolutionIndex.get()].split("x");
        opts.setScreenWidth(Integer.parseInt(res[0]));
        opts.setScreenHeight(Integer.parseInt(res[1]));

        // Guardar en archivo
        opts.save();

        // Recargar recursos con las nuevas rutas para que estén disponibles
        org.argentumforge.engine.utils.GameData.init();

        // Generar colores del minimapa si está marcado
        if (generateMinimap.get()) {
            org.argentumforge.engine.utils.editor.MinimapColorGenerator.generateBinary();
        }

        // Cerrar wizard
        this.close();

        // Ejecutar callback
        if (onComplete != null) {
            onComplete.run();
        }
    }
}
