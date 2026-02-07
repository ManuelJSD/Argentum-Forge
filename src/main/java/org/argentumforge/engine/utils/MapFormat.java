package org.argentumforge.engine.utils;

public enum MapFormat {
    LEGACY(".map", "Argentum Online Map (Legacy)", true),
    CSM(".csm", "Client Side Map (Optimized)", false); // TODO: Implementar

    private final String extension;
    private final String description;
    private final boolean writable;

    MapFormat(String extension, String description, boolean writable) {
        this.extension = extension;
        this.description = description;
        this.writable = writable;
    }

    public String getExtension() {
        return extension;
    }

    public String getDescription() {
        return description;
    }

    public boolean isWritable() {
        return writable;
    }

    public boolean match(String filename) {
        return filename != null && filename.toLowerCase().endsWith(extension);
    }
}
