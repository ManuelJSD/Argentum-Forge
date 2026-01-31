package org.argentumforge.engine.utils;

import java.nio.file.Path;

public class TestUtils {

    /**
     * Isolates the ProfileManager to use a temporary directory.
     * Call this in @BeforeEach or @BeforeAll methods.
     */
    public static void isolateProfileManager(Path tempDir) {
        Path tempProfilesDir = tempDir.resolve("profiles");
        Path tempProfilesFile = tempDir.resolve("profiles.json");

        ProfileManager.INSTANCE.setProfilesDir(tempProfilesDir.toAbsolutePath().toString());
        ProfileManager.INSTANCE.setProfilesFile(tempProfilesFile.toAbsolutePath().toString());
        ProfileManager.INSTANCE.getProfiles().clear();
        ProfileManager.INSTANCE.setCurrentProfile(null);
    }

    /**
     * Resets ProfileManager to defaults.
     * Call this in @AfterEach or @AfterAll methods.
     */
    public static void resetProfileManager() {
        ProfileManager.INSTANCE.setProfilesDir("profiles");
        ProfileManager.INSTANCE.setProfilesFile("profiles.json");
    }
}
