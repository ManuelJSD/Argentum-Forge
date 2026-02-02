package org.argentumforge.engine.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileManagerTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        TestUtils.isolateProfileManager(tempDir);
    }

    @AfterEach
    void tearDown() throws IOException {
        TestUtils.resetProfileManager();
    }

    @Test
    @DisplayName("Should create a new profile")
    void shouldCreateProfile() {
        // Arrange
        String profileName = "TestProfile";

        // Act
        Profile profile = ProfileManager.INSTANCE.createProfile(profileName);

        // Assert
        assertThat(profile).isNotNull();
        assertThat(profile.getName()).isEqualTo(profileName);
        assertThat(ProfileManager.INSTANCE.getProfiles()).contains(profile);
        assertThat(profile.getConfigPath()).contains("TestProfile");
    }

    @Test
    @DisplayName("Should sanitize profile names for file paths")
    void shouldSanitizeProfileNames() {
        // Arrange
        String unsafeName = "Test Profile (2024)!";

        // Act
        Profile profile = ProfileManager.INSTANCE.createProfile(unsafeName);

        // Assert
        assertThat(profile.getConfigPath()).doesNotContain("(", ")", "!");
        assertThat(profile.getConfigPath()).contains("_");
    }

    @Test
    @DisplayName("Should delete a profile")
    void shouldDeleteProfile() {
        // Arrange
        Profile profile = ProfileManager.INSTANCE.createProfile("ToDelete");
        int initialSize = ProfileManager.INSTANCE.getProfiles().size();

        // Act
        ProfileManager.INSTANCE.deleteProfile(profile);

        // Assert
        assertThat(ProfileManager.INSTANCE.getProfiles()).doesNotContain(profile);
        assertThat(ProfileManager.INSTANCE.getProfiles()).hasSize(initialSize - 1);
    }

    @Test
    @DisplayName("Should clear current profile when deleting it")
    void shouldClearCurrentProfileWhenDeleting() {
        // Arrange
        Profile profile = ProfileManager.INSTANCE.createProfile("Current");
        ProfileManager.INSTANCE.setCurrentProfile(profile);

        // Act
        ProfileManager.INSTANCE.deleteProfile(profile);

        // Assert
        assertThat(ProfileManager.INSTANCE.getCurrentProfile()).isNull();
    }

    @Test
    @DisplayName("Should manage current profile")
    void shouldManageCurrentProfile() {
        // Arrange
        Profile profile1 = ProfileManager.INSTANCE.createProfile("Profile1");
        Profile profile2 = ProfileManager.INSTANCE.createProfile("Profile2");

        // Act & Assert
        ProfileManager.INSTANCE.setCurrentProfile(profile1);
        assertThat(ProfileManager.INSTANCE.getCurrentProfile()).isEqualTo(profile1);

        ProfileManager.INSTANCE.setCurrentProfile(profile2);
        assertThat(ProfileManager.INSTANCE.getCurrentProfile()).isEqualTo(profile2);
    }

    @Test
    @DisplayName("Should report hasProfiles correctly")
    void shouldReportHasProfiles() {
        // Arrange
        ProfileManager.INSTANCE.getProfiles().clear();

        // Assert - initially empty
        assertThat(ProfileManager.INSTANCE.hasProfiles()).isFalse();

        // Act
        ProfileManager.INSTANCE.createProfile("TestProfile");

        // Assert - after adding
        assertThat(ProfileManager.INSTANCE.hasProfiles()).isTrue();
    }

    @Test
    @DisplayName("Should return profiles directory path")
    void shouldReturnProfilesDirectory() {
        String profilesDir = ProfileManager.INSTANCE.getProfilesDir();
        assertThat(profilesDir).contains("profiles");
        assertThat(Path.of(profilesDir)).isAbsolute();
    }

    @Test
    @DisplayName("Should handle null profile deletion gracefully")
    void shouldHandleNullProfileDeletion() {
        // Act & Assert - should not throw
        ProfileManager.INSTANCE.deleteProfile(null);
    }

    @Test
    @DisplayName("Should create profiles directory when creating first profile")
    void shouldCreateProfilesDirectory() {
        // Arrange
        File profilesDir = new File(ProfileManager.INSTANCE.getProfilesDir());
        if (profilesDir.exists()) {
            profilesDir.delete();
        }

        // Act
        ProfileManager.INSTANCE.createProfile("FirstProfile");

        // Assert
        assertThat(profilesDir).exists();
        assertThat(profilesDir).isDirectory();
    }
}
