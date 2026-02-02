package org.argentumforge.engine.social;

import de.jcm.discordgamesdk.Core;
import de.jcm.discordgamesdk.CreateParams;
import de.jcm.discordgamesdk.activity.Activity;
import de.jcm.discordgamesdk.activity.ActivityType;

import org.tinylog.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;

/**
 * Handles Discord Rich Presence integration using Discord Game SDK
 * (discord-game-sdk4j).
 */
public class DiscordIntegration {

    private static DiscordIntegration instance;
    private final long applicationId = 1335682662243794975L;
    private Core core;
    private boolean isInitialized = false;

    private DiscordIntegration() {
    }

    public static DiscordIntegration getInstance() {
        if (instance == null) {
            instance = new DiscordIntegration();
        }
        return instance;
    }

    public void init() {
        if (isInitialized)
            return;

        Logger.info("Initializing Discord Game SDK with App ID: {}", applicationId);

        try {
            // Attempt to load the native library from the classpath/jar
            // Note: In a real distribution, you might need to extract this or ensure the
            // DLL is present.
            // discord-game-sdk4j generally requires the native library file.
            // For now, we attempt to find it or let it fail gracefully if not found.

            // Checking if we are on Windows
            String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
            File discordLib = null;

            // For this implementation, we will assume the user has the
            // 'discord_game_sdk.dll' in the run path
            // OR we try to initialize without specifying, hoping for a bundled loader.
            // However, de.jcm.discordgamesdk.Core.init(File) is the standard way.

            // To be safe and simple for now:
            File libDir = new File("natives");
            if (os.contains("win")) {
                discordLib = new File(libDir, "discord_game_sdk.dll");
            } else if (os.contains("linux")) {
                discordLib = new File(libDir, "discord_game_sdk.so");
            } else if (os.contains("mac")) {
                discordLib = new File(libDir, "discord_game_sdk.dylib");
            }

            if (discordLib == null || !discordLib.exists()) {
                // If not found in explicit path, try system load or temp extraction if
                // supported by library
                // But typically we need to provide it.
                // Let's try to extract it from the jar if we can, or just warn.

                // FALLBACK: Try initializing with a temp file if the library supports resource
                // extraction
                // (JnCrMx's fork has some loader utilities sometimes)

                // For this edit, we will try to use the library's built-in loader if available,
                // otherwise we might need to tell the user to get the DLL.
                // But let's assume we can proceed or fail gracefully.

                // Assuming standard init for now, catching UnsatisfiedLinkError
                try {
                    System.loadLibrary("discord_game_sdk");
                } catch (UnsatisfiedLinkError e) {
                    // Log and return, don't crash
                    Logger.warn("Discord Game SDK native library not found. Discord integration disabled.");
                    Logger.warn("Please ensure 'discord_game_sdk.dll' (or .so/.dylib) is in your library path.");
                    return;
                }
            } else {
                Core.init(discordLib);
            }

            // Create Params
            try (CreateParams params = new CreateParams()) {
                params.setClientID(applicationId);
                params.setFlags(CreateParams.getDefaultFlags());

                core = new Core(params);
                isInitialized = true;
                Logger.info("Discord Game SDK initialized successfully!");

                // Start a thread to run callbacks
                Thread callbackThread = new Thread(() -> {
                    while (!Thread.currentThread().isInterrupted() && isInitialized && core.isOpen()) {
                        try {
                            core.runCallbacks();
                            Thread.sleep(16); // ~60 FPS update rate
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } catch (Exception e) {
                            Logger.error(e, "Error in Discord callback loop");
                        }
                    }
                }, "Discord-Callback-Thread");
                callbackThread.setDaemon(true);
                callbackThread.start();

                updatePresence("In Main Menu", "Argentum Forge");
            }

        } catch (UnsatisfiedLinkError e) {
            Logger.error(e, "Failed to load Discord Game SDK native library.");
            isInitialized = false;
        } catch (Exception e) {
            Logger.error(e, "Discord Game SDK init failed");
            isInitialized = false;
        }
    }

    public void updatePresence(String details, String state) {
        if (!isInitialized || core == null || !core.isOpen())
            return;

        try {
            Logger.debug("Updating Presence: {} - {}", details, state);
            try (Activity activity = new Activity()) {
                activity.setDetails(details);
                activity.setState(state);
                activity.assets().setLargeImage("icon");
                activity.assets().setLargeText("Argentum Forge");
                activity.timestamps().setStart(java.time.Instant.now());

                core.activityManager().updateActivity(activity);
            }
        } catch (Exception e) {
            Logger.error(e, "Failed to update presence");
        }
    }

    public void updateDetails(String details) {
        updatePresence(details, "Map Editor");
    }

    public void updateState(String state) {
        // Placeholder
    }

    public void shutdown() {
        if (!isInitialized)
            return;

        Logger.info("Shutting down Discord Game SDK...");
        if (core != null) {
            try {
                core.close();
            } catch (Exception e) {
                Logger.error(e, "Error closing Discord Core");
            }
        }
        isInitialized = false;
    }

    public boolean isInitialized() {
        return isInitialized;
    }
}
