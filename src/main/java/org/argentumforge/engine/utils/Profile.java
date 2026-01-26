package org.argentumforge.engine.utils;

/**
 * Representa un perfil de usuario con su configuración aislada.
 */
public class Profile {
    private String name;
    private String configPath;

    public Profile(String name, String configPath) {
        this.name = name;
        this.configPath = configPath;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getConfigPath() {
        return configPath;
    }

    public void setConfigPath(String configPath) {
        this.configPath = configPath;
    }

    @Override
    public String toString() {
        return name;
    }
}
