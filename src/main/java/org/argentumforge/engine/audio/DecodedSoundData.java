package org.argentumforge.engine.audio;

import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import static org.lwjgl.openal.AL10.AL_FORMAT_MONO16;
import static org.lwjgl.openal.AL10.AL_FORMAT_STEREO16;
import static org.lwjgl.stb.STBVorbis.stb_vorbis_decode_filename;
import static org.lwjgl.system.MemoryStack.stackMallocInt;
import static org.lwjgl.system.MemoryStack.stackPush;

/**
 * Esta clase nos permite precargar los datos de algun sonido en {@code .ogg }
 * lo vamos a utilizar para que en un hilo podamos cargar un sonido
 * y evitar lag a la hora de pasar de mapa o de cargar algun sonido con mucho
 * peso.
 */
public final class DecodedSoundData {
    public final ShortBuffer pcm;
    public final String filepath;
    public final int format;
    public final int sampleRate;
    public final boolean needsFree;

    public DecodedSoundData(String filepath, ShortBuffer pcm, int format, int sampleRate, boolean needsFree) {
        this.pcm = pcm;
        this.format = format;
        this.sampleRate = sampleRate;
        this.filepath = filepath;
        this.needsFree = needsFree;
    }

    public static DecodedSoundData decode(String filepath) {
        if (filepath.toLowerCase().endsWith(".ogg")) {
            return decodeOgg(filepath);
        } else if (filepath.toLowerCase().endsWith(".wav") || filepath.toLowerCase().endsWith(".mp3")) {
            return decodeWithAudioSystem(filepath);
        }
        throw new RuntimeException("Unsupported audio format: " + filepath);
    }

    public static DecodedSoundData decodeOgg(String filepath) {
        try (MemoryStack stack = stackPush()) {
            IntBuffer channelsBuffer = stackMallocInt(1);
            IntBuffer sampleRateBuffer = stackMallocInt(1);

            ShortBuffer pcm = stb_vorbis_decode_filename(filepath, channelsBuffer, sampleRateBuffer);
            if (pcm == null)
                throw new RuntimeException("Error decoding: " + filepath);

            int channels = channelsBuffer.get(0);
            int sampleRate = sampleRateBuffer.get(0);

            int format = channels == 1 ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16;
            return new DecodedSoundData(filepath, pcm, format, sampleRate, true);
        }
    }

    private static DecodedSoundData decodeWithAudioSystem(String filepath) {
        try {
            java.io.File file = new java.io.File(filepath);
            javax.sound.sampled.AudioInputStream in = javax.sound.sampled.AudioSystem.getAudioInputStream(file);
            javax.sound.sampled.AudioFormat baseFormat = in.getFormat();
            javax.sound.sampled.AudioFormat targetFormat = new javax.sound.sampled.AudioFormat(
                    javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED,
                    baseFormat.getSampleRate(),
                    16,
                    baseFormat.getChannels(),
                    baseFormat.getChannels() * 2,
                    baseFormat.getSampleRate(),
                    false);
            javax.sound.sampled.AudioInputStream din = javax.sound.sampled.AudioSystem.getAudioInputStream(targetFormat,
                    in);

            byte[] bytes = din.readAllBytes();
            java.nio.ByteBuffer byteBuffer = java.nio.ByteBuffer.allocateDirect(bytes.length);
            byteBuffer.order(java.nio.ByteOrder.nativeOrder());
            byteBuffer.put(bytes);
            byteBuffer.flip();
            ShortBuffer pcm = byteBuffer.asShortBuffer();

            int format = targetFormat.getChannels() == 1 ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16;
            return new DecodedSoundData(filepath, pcm, format, (int) targetFormat.getSampleRate(), false);
        } catch (Exception e) {
            throw new RuntimeException("Error decoding " + filepath + ": " + e.getMessage(), e);
        }
    }

}
