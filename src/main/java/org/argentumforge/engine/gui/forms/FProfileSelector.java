package org.argentumforge.engine.gui.forms;

import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiTableColumnFlags;
import imgui.flag.ImGuiTableFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImString;
import org.argentumforge.engine.game.Options;
import org.argentumforge.engine.i18n.I18n;

import org.argentumforge.engine.utils.Profile;
import org.argentumforge.engine.utils.ProfileManager;

import java.util.List;
import org.argentumforge.engine.renderer.Texture;
import org.argentumforge.engine.renderer.Texture;
import org.argentumforge.engine.renderer.Surface;
import org.argentumforge.engine.Engine;
import org.argentumforge.engine.gui.Theme;
import org.argentumforge.engine.gui.DialogManager;
import org.argentumforge.engine.gui.ImGUISystem;

public class FProfileSelector extends Form {
    private final Runnable onProfileSelected;
    private final ImString newProfileName = new ImString(64);
    private final ImString renameProfileName = new ImString(64);
    private boolean showCreateDialog = false;
    private boolean showRenameDialog = false;
    private boolean editMode = false;
    private Profile profileToRename;
    private Profile selectedProfile = null;
    private String errorMessage = "";
    private Texture backgroundTexture;

    public FProfileSelector(Runnable onProfileSelected) {
        this.onProfileSelected = onProfileSelected;
        // Cargar textura de fondo
        try {
            this.backgroundTexture = Surface.INSTANCE.createTexture("VentanaInicio.jpg", false);
            if (this.backgroundTexture != null) {
                Engine.INSTANCE.getWindow().updateResolution(
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
                    Engine.INSTANCE.getWindow().getWidth(),
                    Engine.INSTANCE.getWindow().getHeight());
        }

        int windowWidth = 450;
        int windowHeight = 350;

        ImGui.setNextWindowSize(windowWidth, windowHeight, ImGuiCond.Always);
        ImGui.setNextWindowPos(
                (Engine.INSTANCE.getWindow().getWidth() - windowWidth) / 2f,
                (Engine.INSTANCE.getWindow().getHeight() - windowHeight) * 0.70f,
                ImGuiCond.Always);

        int flags = ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoMove
                | ImGuiWindowFlags.NoTitleBar;

        if (ImGui.begin(I18n.INSTANCE.get("profile.select.title") + "###ProfileSelector", flags)) {

            // Header
            ImGui.spacing();
            String title = I18n.INSTANCE.get("profile.select.title");
            float titleWidth = ImGui.calcTextSize(title).x;
            ImGui.setCursorPosX((windowWidth - titleWidth) / 2f);
            ImGui.textColored(Theme.COLOR_PRIMARY, title);
            ImGui.spacing();
            ImGui.separator();
            ImGui.spacing();

            List<Profile> profiles = ProfileManager.INSTANCE.getProfiles();

            ImGui.pushStyleColor(imgui.flag.ImGuiCol.FrameBg, Theme.BG_PANEL);
            if (ImGui.beginChild("ProfileList", 0, 180, true)) {
                if (ImGui.beginTable("ProfilesTable", editMode ? 2 : 1, ImGuiTableFlags.SizingStretchProp)) {
                    ImGui.tableSetupColumn("Name", ImGuiTableColumnFlags.WidthStretch);
                    if (editMode) {
                        ImGui.tableSetupColumn("Actions", ImGuiTableColumnFlags.WidthFixed, 150);
                    }

                    for (int i = 0; i < profiles.size(); i++) {
                        Profile p = profiles.get(i);
                        boolean isSelected = (selectedProfile == p);

                        ImGui.tableNextRow();
                        ImGui.tableNextColumn();

                        // Use pushID to ensure unique IDs even if names are identical
                        ImGui.pushID(i);

                        if (isSelected) {
                            ImGui.pushStyleColor(imgui.flag.ImGuiCol.Header,
                                    Theme.COLOR_PRIMARY);
                        }

                        if (ImGui.selectable(p.getName(), isSelected)) {
                            selectedProfile = p;
                        }

                        // Menú Contextual (Click derecho)
                        if (ImGui.beginPopupContextItem("ProfileContextMenu")) {
                            selectedProfile = p; // Asegurar que el click derecho selecciona el perfil

                            if (ImGui.menuItem(I18n.INSTANCE.get("profile.edit"))) {
                                profileToRename = p;
                                renameProfileName.set(p.getName());
                                showRenameDialog = true;
                            }

                            ImGui.separator();

                            if (ImGui.menuItem(I18n.INSTANCE.get("profile.moveUp"), null, false, i > 0)) {
                                ProfileManager.INSTANCE.reorderProfile(i, true);
                            }
                            if (ImGui.menuItem(I18n.INSTANCE.get("profile.moveDown"), null, false,
                                    i < profiles.size() - 1)) {
                                ProfileManager.INSTANCE.reorderProfile(i, false);
                            }

                            ImGui.separator();

                            ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text,
                                    Theme.COLOR_DANGER);
                            if (ImGui.menuItem(I18n.INSTANCE.get("profile.delete"))) {
                                final Profile toDelete = p;
                                DialogManager.getInstance().showConfirm(
                                        I18n.INSTANCE.get("profile.delete.confirm.title"),
                                        String.format(I18n.INSTANCE.get("profile.delete.confirm.msg"),
                                                toDelete.getName()),
                                        () -> {
                                            ProfileManager.INSTANCE.deleteProfile(toDelete);
                                            if (selectedProfile == toDelete) {
                                                selectedProfile = ProfileManager.INSTANCE.hasProfiles()
                                                        ? ProfileManager.INSTANCE.getProfiles().get(0)
                                                        : null;
                                            }
                                        },
                                        null);
                            }
                            ImGui.popStyleColor();

                            ImGui.endPopup();
                        }

                        if (isSelected) {
                            ImGui.popStyleColor();
                            ImGui.setItemDefaultFocus();
                        }

                        if (editMode) {
                            ImGui.tableNextColumn();

                            // Botones de acción compactos
                            if (ImGui.button("^", 25, 20)) {
                                ProfileManager.INSTANCE.reorderProfile(i, true);
                            }
                            if (ImGui.isItemHovered())
                                ImGui.setTooltip(I18n.INSTANCE.get("profile.moveUp"));

                            ImGui.sameLine();
                            if (ImGui.button("v", 25, 20)) {
                                ProfileManager.INSTANCE.reorderProfile(i, false);
                            }
                            if (ImGui.isItemHovered())
                                ImGui.setTooltip(I18n.INSTANCE.get("profile.moveDown"));

                            ImGui.sameLine();
                            if (ImGui.button(I18n.INSTANCE.get("profile.edit"), 45, 20)) {
                                profileToRename = p;
                                renameProfileName.set(p.getName());
                                showRenameDialog = true;
                            }

                            ImGui.sameLine();
                            ImGui.pushStyleColor(imgui.flag.ImGuiCol.Button,
                                    Theme.COLOR_DANGER);
                            if (ImGui.button("X", 25, 20)) {
                                final Profile toDelete = p;
                                DialogManager.getInstance().showConfirm(
                                        I18n.INSTANCE.get("profile.delete.confirm.title"),
                                        String.format(I18n.INSTANCE.get("profile.delete.confirm.msg"),
                                                toDelete.getName()),
                                        () -> {
                                            ProfileManager.INSTANCE.deleteProfile(toDelete);
                                            if (selectedProfile == toDelete) {
                                                selectedProfile = ProfileManager.INSTANCE.hasProfiles()
                                                        ? ProfileManager.INSTANCE.getProfiles().get(0)
                                                        : null;
                                            }
                                        },
                                        null);
                            }
                            ImGui.popStyleColor();
                        }

                        ImGui.popID();
                    }
                    ImGui.endTable();
                }
            }
            ImGui.endChild();
            ImGui.popStyleColor();

            ImGui.dummy(0, 5); // Spacing to center buttons logically

            // Bloque de flujo principal (Seleccionar + Nuevo)
            float buttonWidth = 140;
            float groupWidth = (buttonWidth * 2) + ImGui.getStyle().getItemSpacingX();
            ImGui.setCursorPosX((windowWidth - groupWidth) / 2f);

            ImGui.beginGroup();
            // Select - Primary
            ImGui.pushStyleColor(imgui.flag.ImGuiCol.Button, Theme.COLOR_PRIMARY);
            if (ImGui.button(I18n.INSTANCE.get("common.select"), buttonWidth, 35)) {
                selectProfile();
            }
            ImGui.popStyleColor();

            ImGui.sameLine();

            // New - Grouped with Select
            if (ImGui.button(I18n.INSTANCE.get("profile.create"), buttonWidth, 35)) {
                showCreateDialog = true;
                newProfileName.set("");
                errorMessage = "";
            }
            ImGui.endGroup();

            ImGui.dummy(0, 5);
            ImGui.setCursorPosX((windowWidth - buttonWidth) / 2f);
            boolean pushedManageColor = false;
            if (editMode) {
                ImGui.pushStyleColor(imgui.flag.ImGuiCol.Button, Theme.COLOR_ACCENT);
                pushedManageColor = true;
            }
            if (ImGui.button(I18n.INSTANCE.get("profile.manage"), buttonWidth, 30)) {
                editMode = !editMode;
            }
            if (pushedManageColor) {
                ImGui.popStyleColor();
            }

            // Mensaje de error principal
            if (!errorMessage.isEmpty() && !showCreateDialog && !showRenameDialog) {
                ImGui.spacing();
                ImGui.textColored(Theme.COLOR_DANGER, errorMessage);
            }

            renderCreateDialog();
            renderRenameDialog();
        }
        ImGui.end();
    }

    private void renderRenameDialog() {
        if (showRenameDialog) {
            ImGui.openPopup(I18n.INSTANCE.get("profile.rename.title"));
        }

        // Center the popup
        ImGui.setNextWindowPos(
                (Engine.INSTANCE.getWindow().getWidth() - 300) / 2f,
                (Engine.INSTANCE.getWindow().getHeight() - 150) / 2f,
                ImGuiCond.Appearing);

        if (ImGui.beginPopupModal(I18n.INSTANCE.get("profile.rename.title"), ImGuiWindowFlags.AlwaysAutoResize)) {
            ImGui.text(I18n.INSTANCE.get("profile.create.name"));
            ImGui.inputText("##renamepro", renameProfileName);

            ImGui.spacing();

            if (ImGui.button(I18n.INSTANCE.get("common.apply"), 120, 0)) {
                String name = renameProfileName.get().trim();
                if (name.isEmpty()) {
                    errorMessage = I18n.INSTANCE.get("wizard.error.emptyPath");
                } else {
                    ProfileManager.INSTANCE.renameProfile(profileToRename, name);
                    showRenameDialog = false;
                    ImGui.closeCurrentPopup();
                }
            }
            ImGui.sameLine();
            if (ImGui.button(I18n.INSTANCE.get("common.cancel"), 120, 0)) {
                showRenameDialog = false;
                ImGui.closeCurrentPopup();
            }

            ImGui.endPopup();
        }
    }

    private void renderCreateDialog() {
        if (showCreateDialog) {
            ImGui.openPopup(I18n.INSTANCE.get("profile.create.title"));
        }

        // Center the popup
        ImGui.setNextWindowPos(
                (org.argentumforge.engine.Engine.INSTANCE.getWindow().getWidth() - 300) / 2f,
                (org.argentumforge.engine.Engine.INSTANCE.getWindow().getHeight() - 150) / 2f,
                ImGuiCond.Appearing);

        if (ImGui.beginPopupModal(I18n.INSTANCE.get("profile.create.title"), ImGuiWindowFlags.AlwaysAutoResize)) {
            ImGui.text(I18n.INSTANCE.get("profile.create.name"));
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
                    // Pasamos el callback para continuar la carga al guardar, y onCancel para
                    // revertir
                    FRoutes routesForm = new FRoutes(onProfileSelected, () -> {
                        // Revertir creación si cancela
                        ProfileManager.INSTANCE.deleteProfile(p);
                        // Reabrir selector
                        ImGUISystem.INSTANCE.show(new FProfileSelector(onProfileSelected));
                    });
                    ImGUISystem.INSTANCE.show(routesForm);

                    // Cerrar el selector de perfiles ya que el control pasa al wizard
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
            errorMessage = I18n.INSTANCE.get("profile.error.noselection");
            return;
        }

        // Cargar configuración del perfil
        ProfileManager.INSTANCE.setCurrentProfile(selectedProfile);
        Options.INSTANCE.setConfigPath(selectedProfile.getConfigPath());
        Options.INSTANCE.load();

        // Ejecutar callback
        if (onProfileSelected != null) {
            onProfileSelected.run();
        }

        // Cerrar este form
        this.close();
    }
}
