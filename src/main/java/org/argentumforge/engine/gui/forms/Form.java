package org.argentumforge.engine.gui.forms;

import imgui.ImGui;
import imgui.ImVec2;
import org.argentumforge.engine.game.User;
import org.argentumforge.engine.gui.ImGUISystem;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import org.tinylog.Logger;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.argentumforge.engine.Window.SCREEN_HEIGHT;
import static org.argentumforge.engine.Window.SCREEN_WIDTH;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;

/**
 * <p>
 * Define la estructura comun y comportamiento basico que deben implementar
 * todos los formularios. Proporciona un marco unificado
 * para la creacion de interfaces de usuario interactivas, obligando a todas las
 * subclases a implementar su propia logica de
 * renderizado mediante el metodo abstracto {@code render()}.
 * <p>
 * Incluye funcionalidad para gestion de recursos graficos sin dependencias de
 * AWT
 * (usando LWJGL STBImage).
 */

public abstract class Form {

    protected int backgroundImage;
    protected static final User USER = User.INSTANCE;
    protected static final ImGUISystem IM_GUI_SYSTEM = ImGUISystem.INSTANCE;

    public abstract void render();

    public void close() {
        glDeleteTextures(backgroundImage);
        IM_GUI_SYSTEM.deleteFrmArray(this);
    }

    protected void checkMoveFrm() {
        float barHeight = 10.0f;
        ImVec2 windowPos = ImGui.getWindowPos();

        ImGui.invisibleButton("title_bar", ImGui.getWindowSizeX() - 32, barHeight);
        boolean isTitleBarActive = ImGui.isItemActive();

        if (isTitleBarActive) {
            ImVec2 delta = ImGui.getIO().getMouseDelta();
            final float newPosX = windowPos.x + delta.x;
            final float newPosY = windowPos.y + delta.y;

            if ((newPosX > 0 && newPosX < SCREEN_WIDTH) && (newPosY > 0 && newPosY < SCREEN_HEIGHT)) {
                ImGui.setWindowPos(newPosX, newPosY);
            }
        }
    }

    protected int loadTexture(final String file) throws IOException {
        Path guiPath = findLocalFile("resources", "gui", file);
        if (guiPath == null || !Files.exists(guiPath)) {
            Logger.error("No se pudo encontrar el recurso GUI: " + file);
            return -1;
        }

        // Leer archivo a ByteBuffer para STB
        byte[] bytes = Files.readAllBytes(guiPath);
        ByteBuffer fileData = BufferUtils.createByteBuffer(bytes.length);
        fileData.put(bytes);
        fileData.flip();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer comp = stack.mallocInt(1);

            // Cargar imagen desde memoria
            ByteBuffer image = STBImage.stbi_load_from_memory(fileData, w, h, comp, 4); // 4 = RGBA
            if (image == null) {
                Logger.error("Fallo al cargar textura " + file + ": " + STBImage.stbi_failure_reason());
                return -1;
            }

            int width = w.get(0);
            int height = h.get(0);

            int textureID = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, textureID);

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, image);

            STBImage.stbi_image_free(image);
            return textureID;
        }
    }

    private Path findLocalFile(String base, String folder, String filename) {
        Path directPath = Path.of(base, folder, filename);
        if (Files.exists(directPath))
            return directPath;

        String[] extensions = { ".jpg", ".png", ".bmp" };
        for (String ext : extensions) {
            Path p = Path.of(base, folder, filename + ext);
            if (Files.exists(p))
                return p;
        }
        return null;
    }

    public static void openURL(String url) {
        String os = System.getProperty("os.name").toLowerCase();
        Runtime rt = Runtime.getRuntime();

        try {
            if (os.contains("win")) {
                rt.exec("rundll32 url.dll,FileProtocolHandler " + url);
            } else if (os.contains("mac")) {
                rt.exec("open " + url);
            } else if (os.contains("nix") || os.contains("nux")) {
                String[] browsers = { "xdg-open", "google-chrome", "firefox", "opera",
                        "epiphany", "konqueror", "conkeror", "midori", "kazehakase", "mozilla" };
                boolean found = false;
                for (String browser : browsers) {
                    try {
                        rt.exec(new String[] { browser, url });
                        found = true;
                        break;
                    } catch (IOException e) {
                        // continue
                    }
                }
                if (!found) {
                    Logger.warn("No se encontró navegador para abrir URL en Linux.");
                }
            } else {
                Logger.warn("Apertura de URL no soportada automÃ¡ticamente en este OS: " + os);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
