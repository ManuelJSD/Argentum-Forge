package org.argentumforge.engine.audio;

import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.ALCapabilities;

import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.system.MemoryUtil.NULL;

import org.tinylog.Logger;

/**
 * Sistema de Audio centralizado.
 * Gestiona el contexto y dispositivo de OpenAL.
 */
public enum AudioSystem {
    INSTANCE;

    private long audioDevice;
    private long audioContext;

    public void init() {
        String defaultDeviceName = alcGetString(0, ALC_DEFAULT_DEVICE_SPECIFIER);
        audioDevice = alcOpenDevice(defaultDeviceName);

        // Verifica si el dispositivo se abrio correctamente
        if (audioDevice == NULL) {
            Logger.warn("Could not open default audio device: {}", defaultDeviceName);

            // Intenta con el primer dispositivo disponible
            String deviceList = alcGetString(0, ALC_DEVICE_SPECIFIER);
            if (deviceList != null && !deviceList.isEmpty()) {
                Logger.info("Trying with the first available device: {}", deviceList);
                audioDevice = alcOpenDevice(deviceList);
            }

            // Si aun falla, intenta sin especificar dispositivo
            if (audioDevice == NULL) {
                Logger.warn("Trying to open unspecified audio device...");
                audioDevice = alcOpenDevice((String) null);
            }
        }

        // Solo crea el contexto si tenemos un dispositivo valido
        if (audioDevice != NULL) {
            int[] attributes = { 0 };
            audioContext = alcCreateContext(audioDevice, attributes);

            if (audioContext == NULL) {
                Logger.error("The audio context could not be created");
                alcCloseDevice(audioDevice);
                audioDevice = NULL;
            } else {
                alcMakeContextCurrent(audioContext);

                ALCCapabilities alcCapabilities = ALC.createCapabilities(audioDevice);
                ALCapabilities alCapabilities = AL.createCapabilities(alcCapabilities);

                if (!alCapabilities.OpenAL10) {
                    Logger.error("OpenAL 1.0 is not supported");
                    alcDestroyContext(audioContext);
                    alcCloseDevice(audioDevice);
                    audioDevice = NULL;
                    audioContext = NULL;
                }
            }
        } else {
            Logger.error("Client running without audio!");
        }
    }

    public void destroy() {
        if (audioContext != NULL)
            alcDestroyContext(audioContext);
        if (audioDevice != NULL)
            alcCloseDevice(audioDevice);
    }

    public boolean isAudioAvailable() {
        return audioDevice != NULL && audioContext != NULL;
    }
}
