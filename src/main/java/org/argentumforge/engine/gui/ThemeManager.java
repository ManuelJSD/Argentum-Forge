package org.argentumforge.engine.gui;

import org.argentumforge.engine.game.Options;
import org.tinylog.Logger;

/**
 * Manages the application's visual theme.
 * Handles loading, saving, and applying the selected theme using {@link Theme}.
 */
public class ThemeManager {

    private static ThemeManager instance;
    private Theme.StyleType currentTheme;

    private ThemeManager() {
        // Private constructor for singleton
    }

    public static ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    /**
     * Initializes the theme manager, loading the preference from Options.
     * Should be called on application startup after ImGui is initialized.
     */
    public void init() {
        String savedTheme = Options.INSTANCE.getVisualTheme();
        try {
            currentTheme = Theme.StyleType.valueOf(savedTheme.toUpperCase());
        } catch (IllegalArgumentException e) {
            Logger.warn("Invalid theme '{}' found in options. Reverting to MODERN.", savedTheme);
            currentTheme = Theme.StyleType.MODERN;
        }
        applyTheme(currentTheme);
    }

    /**
     * Sets and applies a new theme.
     * Updates the persistent configuration.
     *
     * @param theme The new theme to apply.
     */
    public void setTheme(Theme.StyleType theme) {
        if (theme == null)
            return;

        this.currentTheme = theme;
        applyTheme(theme);

        // Persist change
        Options.INSTANCE.setVisualTheme(theme.name());
        Options.INSTANCE.save();
        Logger.info("Theme changed to: {}", theme.name());
    }

    /**
     * Returns the currently active theme.
     */
    public Theme.StyleType getCurrentTheme() {
        return currentTheme;
    }

    private void applyTheme(Theme.StyleType theme) {
        Theme.applyStyle(theme);
    }
}
