package org.argentumforge.engine.managers;

import org.argentumforge.engine.Engine;
import org.argentumforge.engine.scenes.GameScene;
import org.argentumforge.engine.scenes.Scene;
import org.argentumforge.engine.scenes.SceneType;

public enum SceneManager {
    INSTANCE;

    private Scene currentScene;

    public Scene getCurrentScene() {
        return currentScene;
    }

    public void changeScene(SceneType scene) {
        switch (scene) {
            case GAME_SCENE -> currentScene = new GameScene();
            // Add other cases as needed
        }

        if (currentScene != null) {
            currentScene.init();

            // Ajustar la resolución de la ventana según las preferencias de la escena
            int preferredWidth = currentScene.getPreferredWidth();
            int preferredHeight = currentScene.getPreferredHeight();

            // Accedemos a Window a través del Engine singleton refactorizado
            org.argentumforge.engine.Window window = Engine.INSTANCE.getWindow();

            // Solo actualizar si la resolución es diferente a la actual
            if (preferredWidth != window.getWidth() || preferredHeight != window.getHeight()) {
                window.updateResolution(preferredWidth, preferredHeight);
            }

            // Aplicar política de redimensionamiento de la escena
            window.setResizable(currentScene.isResizable());
        }
    }

    // For resetting during profile change or similar
    public void reset() {
        currentScene = null;
    }
}
