package org.argentumforge.engine.gui;

import imgui.ImGui;
import imgui.ImGuiViewport;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImString;
import org.argentumforge.engine.i18n.I18n;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/**
 * Sistema centralizado para mostrar diálogos modales en ImGui.
 * Reemplaza los dialogos bloqueantes para evitar bloqueos del hilo principal y
 * mantener
 * la estética de la aplicación.
 */
public class DialogManager {

    private static final DialogManager INSTANCE = new DialogManager();
    private final ConcurrentLinkedQueue<DialogRequest> dialogQueue = new ConcurrentLinkedQueue<>();
    private DialogRequest currentDialog = null;

    private DialogManager() {
    }

    public static DialogManager getInstance() {
        return INSTANCE;
    }

    public void showError(String title, String message) {
        dialogQueue.add(new DialogRequest(Type.ERROR, title, message, null, null));
    }

    public void showInfo(String title, String message) {
        dialogQueue.add(new DialogRequest(Type.INFO, title, message, null, null));
    }

    public void showConfirm(String title, String message, Runnable onYes, Runnable onNo) {
        dialogQueue.add(new DialogRequest(Type.CONFIRM, title, message, onYes, onNo));
    }

    public void showInput(String title, String message, Consumer<String> onInput, Runnable onCancel) {
        // Not implemented yet, needed for some advanced flows, but simple confirm is
        // priority
    }

    public void showYesNoCancel(String title, String message, Runnable onYes, Runnable onNo, Runnable onCancel) {
        dialogQueue.add(new DialogRequest(Type.YES_NO_CANCEL, title, message, onYes, onNo, onCancel));
    }

    public void render() {
        if (currentDialog == null && !dialogQueue.isEmpty()) {
            currentDialog = dialogQueue.poll();
            ImGui.openPopup(currentDialog.title);
        }

        if (currentDialog != null) {
            int flags = ImGuiWindowFlags.AlwaysAutoResize | ImGuiWindowFlags.NoSavedSettings;

            ImGui.setNextWindowSizeConstraints(400, 0, Float.MAX_VALUE, Float.MAX_VALUE);

            if (ImGui.beginPopupModal(currentDialog.title, flags)) {

                // Center the window using actual size AFTER rendering
                // Use ImGui's main viewport work area (correct coordinate system)
                imgui.ImGuiViewport viewport = ImGui.getMainViewport();
                float windowWidth = ImGui.getWindowWidth();
                float windowHeight = ImGui.getWindowHeight();

                // Calculate center position including work area offset
                float centeredX = viewport.getWorkPosX() + (viewport.getWorkSizeX() - windowWidth) / 2.0f;
                float centeredY = viewport.getWorkPosY() + (viewport.getWorkSizeY() - windowHeight) / 2.0f;

                ImGui.setWindowPos(centeredX, centeredY);

                ImGui.textWrapped(currentDialog.message);
                ImGui.spacing();
                ImGui.separator();
                ImGui.spacing();

                if (currentDialog.type == Type.ERROR || currentDialog.type == Type.INFO) {
                    // Botón Centrado
                    float width = ImGui.getWindowWidth();
                    ImGui.setCursorPosX((width - 120) / 2);
                    if (ImGui.button(I18n.INSTANCE.get("common.close"), 120, 0)) {
                        closeCurrent();
                    }
                } else if (currentDialog.type == Type.CONFIRM) {
                    // Botones Sí / No
                    float width = ImGui.getWindowWidth();
                    ImGui.setCursorPosX((width - 250) / 2);

                    if (ImGui.button(I18n.INSTANCE.get("common.yes"), 120, 0)) {
                        if (currentDialog.onYes != null)
                            currentDialog.onYes.run();
                        closeCurrent();
                    }
                    ImGui.sameLine();
                    if (ImGui.button(I18n.INSTANCE.get("common.no"), 120, 0)) {
                        if (currentDialog.onNo != null)
                            currentDialog.onNo.run();
                        closeCurrent();
                    }
                } else if (currentDialog.type == Type.YES_NO_CANCEL) {
                    // Botones Sí / No / Cancelar
                    float width = ImGui.getWindowWidth();
                    float btnWidth = 100;
                    float spacing = 10;
                    float totalBtnWidth = (btnWidth * 3) + (spacing * 2);

                    ImGui.setCursorPosX((width - totalBtnWidth) / 2);

                    if (ImGui.button(I18n.INSTANCE.get("common.yes"), btnWidth, 0)) {
                        if (currentDialog.onYes != null)
                            currentDialog.onYes.run();
                        closeCurrent();
                    }
                    ImGui.sameLine(0, spacing);
                    if (ImGui.button(I18n.INSTANCE.get("common.no"), btnWidth, 0)) {
                        if (currentDialog.onNo != null)
                            currentDialog.onNo.run();
                        closeCurrent();
                    }
                    ImGui.sameLine(0, spacing);
                    if (ImGui.button(I18n.INSTANCE.get("common.cancel"), btnWidth, 0)) {
                        if (currentDialog.onCancel != null)
                            currentDialog.onCancel.run();
                        closeCurrent();
                    }
                }

                ImGui.endPopup();
            }
        }
    }

    private void closeCurrent() {
        ImGui.closeCurrentPopup();
        currentDialog = null;
    }

    private static class DialogRequest {
        Type type;
        String title;
        String message;
        Runnable onYes; // Used for Confirm (Yes) or simple callback
        Runnable onNo; // Used for Confirm (No)
        Runnable onCancel;

        public DialogRequest(Type type, String title, String message, Runnable onYes, Runnable onNo) {
            this(type, title, message, onYes, onNo, null);
        }

        public DialogRequest(Type type, String title, String message, Runnable onYes, Runnable onNo,
                Runnable onCancel) {
            this.type = type;
            this.title = title;
            this.message = message;
            this.onYes = onYes;
            this.onNo = onNo;
            this.onCancel = onCancel;
        }
    }

    private enum Type {
        INFO, ERROR, CONFIRM, YES_NO_CANCEL, INPUT
    }
}
