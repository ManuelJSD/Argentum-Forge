package org.argentumforge.engine.gui.forms;

import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImString;
import org.argentumforge.engine.game.Options;
import org.argentumforge.engine.i18n.I18n;

import org.argentumforge.engine.utils.Profile;
import org.argentumforge.engine.utils.ProfileManager;

import java.util.List;
import org.argentumforge.engine.renderer.Texture;
import org.argentumforge.engine.renderer.Surface;

public class FProfileSelector extends Form {
    private final Runnable onProfileSelected;
    private final ImString newProfileName = new ImString(64);
    private boolean showCreateDialog = false;
    private Profile selectedProfile = null;
    private String errorMessage = "";
    private Texture backgroundTexture;

    public FProfileSelector(Runnable onProfileSelected) {
        this.onProfileSelected = onProfileSelected;
        // Cargar textura de fondo
        try {
            this.backgroundTexture = Surface.INSTANCE.createTexture("VentanaInicio.jpg", false);
            if (this.backgroundTexture != null) {
                org.argentumforge.engine.Engine.INSTANCE.getWindow().updateResolution(
                        this.backgroundTexture.getTex_width(),
                        this.backgroundTexture.getTex_height());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Preseleccionar el último usado o el primero si existe
        if (ProfileManager.INSTANCE.hasProfiles()) {
            selectedProfile = ProfileManager.INSTANCE.getProfiles().get(0);
        }
    }

    @Override
    public void render() {
        if (backgroundTexture != null) {
            ImGui.getBackgroundDrawList().addImage(
                    backgroundTexture.getId(),
                    0, 0,
                    org.argentumforge.engine.Engine.INSTANCE.getWindow().getWidth(),
                    org.argentumforge.engine.Engine.INSTANCE.getWindow().getHeight());
        }

        int windowWidth = 350;
        int windowHeight = 270;

        ImGui.setNextWindowSize(windowWidth, windowHeight, ImGuiCond.Always);
        ImGui.setNextWindowPos(
                (org.argentumforge.engine.Engine.INSTANCE.getWindow().getWidth() - windowWidth) / 2f,
                (org.argentumforge.engine.Engine.INSTANCE.getWindow().getHeight() - windowHeight) * 0.70f,
                ImGuiCond.Always);

        int flags = ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoMove
                | ImGuiWindowFlags.NoTitleBar;

        if (ImGui.begin("Seleccionar Perfil", flags)) {

            // Header
            ImGui.spacing();
            String title = I18n.INSTANCE.get("profile.select.title");
            float titleWidth = ImGui.calcTextSize(title).x;
            ImGui.setCursorPosX((windowWidth - titleWidth) / 2f);
            ImGui.textColored(org.argentumforge.engine.gui.Theme.COLOR_PRIMARY, title);
            ImGui.spacing();
            ImGui.separator();
            ImGui.spacing();

            // Move profiles list scope here
            List<Profile> profiles = ProfileManager.INSTANCE.getProfiles();

            ImGui.pushStyleColor(imgui.flag.ImGuiCol.FrameBg, org.argentumforge.engine.gui.Theme.BG_PANEL);
            if (ImGui.beginChild("ProfileList", 0, 150, true)) {
                for (Profile p : profiles) {
                    boolean isSelected = (selectedProfile == p);
                    if (isSelected) {
                        ImGui.pushStyleColor(imgui.flag.ImGuiCol.Header,
                                org.argentumforge.engine.gui.Theme.COLOR_PRIMARY);
                    }

                    if (ImGui.selectable(p.getName(), isSelected)) {
                        selectedProfile = p;
                    }

                    if (isSelected) {
                        ImGui.popStyleColor();
                        ImGui.setItemDefaultFocus();
                    }
                }
            }
            ImGui.endChild();
            ImGui.popStyleColor();

            ImGui.dummy(0, 10); // Spacing to center buttons logically

            // Botones de acción centrados
            float buttonWidth = 100;
            float totalButtonWidth = (buttonWidth * 3) + (ImGui.getStyle().getItemSpacingX() * 2);
            ImGui.setCursorPosX((windowWidth - totalButtonWidth) / 2f);

            // Select - Primary
            ImGui.pushStyleColor(imgui.flag.ImGuiCol.Button, org.argentumforge.engine.gui.Theme.COLOR_PRIMARY);
            if (ImGui.button(I18n.INSTANCE.get("common.select"), buttonWidth, 30)) {
                selectProfile();
            }
            ImGui.popStyleColor();

            ImGui.sameLine();

            // New - Default/Accent
            if (ImGui.button(I18n.INSTANCE.get("profile.create"), buttonWidth, 30)) {
                showCreateDialog = true;
                newProfileName.set("");
                errorMessage = "";
            }

            ImGui.sameLine();

            // Delete - Danger
            ImGui.pushStyleColor(imgui.flag.ImGuiCol.Button, org.argentumforge.engine.gui.Theme.COLOR_DANGER);
            if (ImGui.button(I18n.INSTANCE.get("profile.delete"), buttonWidth, 30)) {
                if (selectedProfile != null) {
                    org.argentumforge.engine.gui.DialogManager.getInstance().showConfirm(
                            "Confirmar Eliminación",
                            "¿Está seguro que desea eliminar el perfil '" + selectedProfile.getName()
                                    + "'?\nEsta acción no se puede deshacer.",
                            () -> {
                                ProfileManager.INSTANCE.deleteProfile(selectedProfile);
                                if (profiles.isEmpty()) {
                                    selectedProfile = null;
                                } else {
                                    selectedProfile = profiles.get(0);
                                }
                            },
                            null);
                }
            }
            ImGui.popStyleColor();

            // Mensaje de error principal
            if (!errorMessage.isEmpty() && !showCreateDialog) {
                ImGui.spacing();
                ImGui.textColored(org.argentumforge.engine.gui.Theme.COLOR_DANGER, errorMessage);
            }

            renderCreateDialog();
        }
        ImGui.end();
    }

    private void renderCreateDialog() {
        if (showCreateDialog) {
            ImGui.openPopup("Crear Nuevo Perfil");
        }

        if (ImGui.beginPopupModal("Crear Nuevo Perfil", ImGuiWindowFlags.AlwaysAutoResize)) {
            ImGui.text("Nombre del Perfil:");
            ImGui.inputText("##proname", newProfileName);

            if (!errorMessage.isEmpty()) {
                ImGui.textColored(1.0f, 0.0f, 0.0f, 1.0f, errorMessage);
            }

            ImGui.spacing();

            if (ImGui.button(I18n.INSTANCE.get("common.create"), 120, 0)) {
                String name = newProfileName.get().trim();
                if (name.isEmpty()) {
                    errorMessage = I18n.INSTANCE.get("wizard.error.emptyPath");
                } else {
                    Profile p = ProfileManager.INSTANCE.createProfile(name);
                    selectedProfile = p; // Autoseleccionar

                    // Configurar entorno para el nuevo perfil (carga defaults)
                    ProfileManager.INSTANCE.setCurrentProfile(p);
                    Options.INSTANCE.setConfigPath(p.getConfigPath());
                    Options.INSTANCE.load();

                    showCreateDialog = false;
                    ImGui.closeCurrentPopup();

                    // Lanzar FRoutes obligatoriamente para este nuevo perfil
                    // Pasamos el callback para continuar la carga al guardar
                    FRoutes routesForm = new FRoutes(onProfileSelected);
                    org.argentumforge.engine.gui.ImGUISystem.INSTANCE.show(routesForm);

                    // Cerrar el selector de perfiles ya que el control pasa al wizard -> juego
                    this.close();
                }
            }
            ImGui.sameLine();
            if (ImGui.button(I18n.INSTANCE.get("common.cancel"), 120, 0)) {
                showCreateDialog = false;
                ImGui.closeCurrentPopup();
            }

            ImGui.endPopup();
        }
    }

    private void selectProfile() {
        if (selectedProfile == null) {
            errorMessage = "Debe seleccionar un perfil.";
            return;
        }

        // Cargar configuración del perfil
        ProfileManager.INSTANCE.setCurrentProfile(selectedProfile);
        Options.INSTANCE.setConfigPath(selectedProfile.getConfigPath());
        Options.INSTANCE.load();

        // Si es la primera vez que se carga este perfil (no existe config),
        // deberíamos lanzar el SetupWizard.
        // Pero `load()` ya crea un default si no existe.
        // Quizá queramos detectar esto para lanzar el Wizard.

        // Ejecutar callback
        if (onProfileSelected != null) {
            onProfileSelected.run();
        }

        // Cerrar este form
        this.close();
    }
}
