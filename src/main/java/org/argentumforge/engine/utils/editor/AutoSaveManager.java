package org.argentumforge.engine.utils.editor;

import org.argentumforge.engine.game.Options;
import org.argentumforge.engine.utils.GameData;
import org.argentumforge.engine.utils.MapContext;
import org.argentumforge.engine.utils.MapFileUtils;
import org.argentumforge.engine.utils.Time;
import org.tinylog.Logger;

/**
 * Gestiona el autoguardado de mapas en intervalos configurables.
 */
public class AutoSaveManager {

    private static AutoSaveManager instance;
    private float elapsedTime = 0;

    private AutoSaveManager() {
    }

    public static AutoSaveManager getInstance() {
        if (instance == null) {
            instance = new AutoSaveManager();
        }
        return instance;
    }

    public void update() {
        if (!Options.INSTANCE.isAutoSaveEnabled()) {
            elapsedTime = 0;
            return;
        }

        elapsedTime += Time.deltaTime;

        float intervalSeconds = Options.INSTANCE.getAutoSaveIntervalMinutes() * 60;

        if (elapsedTime >= intervalSeconds) {
            elapsedTime = 0;
            triggerAutoSave();
        }
    }

    private void triggerAutoSave() {
        MapContext context = GameData.getActiveContext();
        if (context == null)
            return;

        if (context.isModified()) {
            Logger.info("Autoguardado disparado para mapa: {}", context.getMapName());
            MapFileUtils.quickSaveMap();
        }
    }

    public void resetTimer() {
        this.elapsedTime = 0;
    }
}
