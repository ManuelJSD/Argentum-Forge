package org.argentumforge.engine.managers;

import org.argentumforge.engine.Window;
import org.argentumforge.engine.audio.Sound;
import org.argentumforge.engine.listeners.KeyHandler;
import org.argentumforge.engine.renderer.BatchRenderer;
import org.argentumforge.engine.renderer.PostProcessor;
import org.argentumforge.engine.renderer.RenderSettings;
import org.argentumforge.engine.scenes.Scene;
import org.argentumforge.engine.utils.GameData;
import org.argentumforge.engine.utils.Time;

public enum RenderManager {
    INSTANCE;

    private BatchRenderer batch;
    private PostProcessor postProcessor;

    public void init() {
        this.batch = new BatchRenderer();
    }

    public BatchRenderer getBatch() {
        return batch;
    }

    public void render(Window window) {
        // Access SceneManager via Singleton
        Scene currentScene = SceneManager.INSTANCE.getCurrentScene();

        // Si no hay escena (ej: esperando el wizard), solo renderizar GUI desde Engine
        // loop principal
        // Pero idealmente el render loop principal debería delegar aquí.
        // Si RenderManager es responsable de TODO el render, también debería disparar
        // la GUI si no hay escena.
        // Sin embargo, Engine.java tenía lógica específica para wizard.
        // Vamos a asumir que render() se llama cuando se QUIERE renderizar juego.

        if (currentScene == null)
            return;

        window.setupGameProjection();

        if (!currentScene.isVisible()) {
            SceneManager.INSTANCE.changeScene(currentScene.getChangeScene());
            // Update reference after change
            currentScene = SceneManager.INSTANCE.getCurrentScene();
        }

        if (currentScene == null)
            return; // Safety check

        // Configurar PostProcessor si es necesario (Photo Mode)
        RenderSettings settings = GameData.options.getRenderSettings();
        boolean usePostProcessing = settings.isPhotoModeActive();

        if (usePostProcessing) {
            int winWidth = window.getWidth();
            int winHeight = window.getHeight();

            if (postProcessor == null) {
                postProcessor = new PostProcessor(winWidth, winHeight);
            } else if (postProcessor.getWidth() != winWidth || postProcessor.getHeight() != winHeight) {
                postProcessor.resize(winWidth, winHeight);
            }

            // Iniciar captura al FBO
            postProcessor.beginCapture();
        }

        batch.begin();
        currentScene.mouseEvents();
        currentScene.keyEvents();
        currentScene.render();
        batch.end();

        if (usePostProcessing) {
            // Finalizar captura y renderizar efecto
            postProcessor.endCapture();
            postProcessor.apply(settings, Time.getRunningTime());

            // Renderizar Vignette (Overlay)
            if (settings.isPhotoVignette()) {
                // Reiniciar batch para dibujar UI/Vignette sobre el post-procesado
                batch.begin();
                float intensity = settings.getVignetteIntensity();
                if (org.argentumforge.engine.game.User.INSTANCE.isUnderCeiling())
                    intensity = Math.min(0.95f, intensity + 0.15f);

                org.argentumforge.engine.renderer.Drawn.drawVignette(
                        window.getWidth(),
                        window.getHeight(),
                        intensity);
                batch.end();
            }
        }

        // ImGui frame management has been moved to Engine.java loop
        Sound.renderMusic();
        KeyHandler.update();
    }
}
