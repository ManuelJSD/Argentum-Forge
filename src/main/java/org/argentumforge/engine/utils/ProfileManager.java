package org.argentumforge.engine.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.tinylog.Logger;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestor singleton para administrar perfiles de usuario.
 * Guarda la lista de perfiles en 'profiles.json'.
 */
public enum ProfileManager {
    INSTANCE;

    private static final String DEFAULT_PROFILES_FILE = "profiles.json";
    private static final String DEFAULT_PROFILES_DIR = "profiles";

    private String profilesFile = DEFAULT_PROFILES_FILE;
    private String profilesDir = DEFAULT_PROFILES_DIR;

    public String getProfilesDir() {
        return profilesDir;
    }

    public void setProfilesFile(String profilesFile) {
        this.profilesFile = profilesFile;
    }

    public void setProfilesDir(String profilesDir) {
        this.profilesDir = profilesDir;
    }

    private List<Profile> profiles = new ArrayList<>();
    private Profile currentProfile;

    public void load() {
        File file = new File(profilesFile);
        if (!file.exists()) {
            ensureProfilesDir();
            return;
        }

        try (Reader reader = new FileReader(file)) {
            Gson gson = new Gson();
            Type listType = new TypeToken<ArrayList<Profile>>() {
            }.getType();
            profiles = gson.fromJson(reader, listType);

            if (profiles == null) {
                profiles = new ArrayList<>();
            }
        } catch (IOException e) {
            Logger.error(e, "Error al cargar " + profilesFile);
            profiles = new ArrayList<>();
        }
    }

    public void save() {
        try (Writer writer = new FileWriter(profilesFile)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(profiles, writer);
        } catch (IOException e) {
            Logger.error(e, "Error al guardar " + profilesFile);
        }
    }

    public Profile createProfile(String name) {
        ensureProfilesDir();
        // Sanitizar nombre para nombre de archivo
        String safeName = name.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
        String configFileName = profilesDir + "/" + safeName + ".ini";

        Profile newProfile = new Profile(name, configFileName);
        profiles.add(newProfile);
        save();
        return newProfile;
    }

    public void deleteProfile(Profile profile) {
        if (profile == null)
            return;

        // Eliminar archivo de configuración asociado
        try {
            Files.deleteIfExists(Path.of(profile.getConfigPath()));
        } catch (IOException e) {
            Logger.warn("No se pudo eliminar el archivo de configuración: {}", profile.getConfigPath());
        }

        profiles.remove(profile);
        save();

        if (currentProfile == profile) {
            currentProfile = null;
        }
    }

    public List<Profile> getProfiles() {
        return profiles;
    }

    public Profile getCurrentProfile() {
        return currentProfile;
    }

    public void setCurrentProfile(Profile currentProfile) {
        this.currentProfile = currentProfile;
    }

    public boolean hasProfiles() {
        return !profiles.isEmpty();
    }

    private void ensureProfilesDir() {
        File dir = new File(profilesDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
}
