package org.argentumforge.engine.gui;

import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import imgui.flag.ImGuiCond;

import java.util.ArrayList;
import java.util.List;

public class ToastManager {
    public static final ToastManager INSTANCE = new ToastManager();

    public enum Type {
        SUCCESS(0.2f, 0.8f, 0.2f, 1.0f),
        INFO(0.2f, 0.6f, 1.0f, 1.0f),
        WARNING(1.0f, 0.8f, 0.2f, 1.0f),
        ERROR(1.0f, 0.2f, 0.2f, 1.0f);

        final float[] color;

        Type(float r, float g, float b, float a) {
            this.color = new float[]{r, g, b, a};
        }
    }

    private static class Toast {
        String message;
        Type type;
        float timer;
        float maxTimer;

        Toast(String message, Type type, float duration) {
            this.message = message;
            this.type = type;
            this.timer = duration;
            this.maxTimer = duration;
        }
    }

    private final List<Toast> toasts = new ArrayList<>();

    private ToastManager() {}

    public void show(String message, Type type) {
        show(message, type, 3.0f);
    }

    public void show(String message, Type type, float duration) {
        toasts.add(new Toast(message, type, duration));
    }

    public void render() {
        if (toasts.isEmpty()) return;

        float deltaTime = imgui.ImGui.getIO().getDeltaTime();
        
        // Coordenadas pantalla (arriba a la derecha)
        float padding = 20.0f;
        float x = ImGui.getMainViewport().getWorkPosX() + ImGui.getMainViewport().getWorkSizeX() - padding;
        float y = ImGui.getMainViewport().getWorkPosY() + padding + 60.0f; // Evitar tapar toolbar

        for (int i = toasts.size() - 1; i >= 0; i--) {
            Toast t = toasts.get(i);
            t.timer -= deltaTime;

            if (t.timer <= 0) {
                toasts.remove(i);
                continue;
            }

            // Calculo fade y posicion
            float alpha = Math.min(1.0f, Math.min(t.timer, t.maxTimer - t.timer) * 4.0f); 
            if (alpha < 0) alpha = 0;

            ImGui.setNextWindowPos(x, y, ImGuiCond.Always, 1.0f, 0.0f);
            
            // Setear opacidad
            ImGui.pushStyleVar(imgui.flag.ImGuiStyleVar.Alpha, alpha);
            
            int flags = ImGuiWindowFlags.NoDecoration 
                      | ImGuiWindowFlags.AlwaysAutoResize 
                      | ImGuiWindowFlags.NoSavedSettings 
                      | ImGuiWindowFlags.NoFocusOnAppearing 
                      | ImGuiWindowFlags.NoNav 
                      | ImGuiWindowFlags.NoMove;

            if (ImGui.begin("Toast_" + i, flags)) {
                String prefix = "[ i ]";
                if (t.type == Type.SUCCESS) prefix = "[OK]";
                else if (t.type == Type.ERROR) prefix = "[ X ]";
                else if (t.type == Type.WARNING) prefix = "[ ! ]";
                
                ImGui.textColored(t.type.color[0], t.type.color[1], t.type.color[2], 1.0f, prefix);
                ImGui.sameLine();
                ImGui.text(t.message);
                
                y += ImGui.getWindowHeight() + 10.0f;
            }
            ImGui.end();
            ImGui.popStyleVar();
        }
    }
}
