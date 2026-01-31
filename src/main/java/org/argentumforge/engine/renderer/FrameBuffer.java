package org.argentumforge.engine.renderer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

import org.tinylog.Logger;

/**
 * Encapsula un Frame Buffer Object (FBO) de OpenGL.
 * Permite renderizar la escena a una textura en lugar de directamente a la
 * pantalla.
 */
public class FrameBuffer {
    private int fboId = 0;
    private int textureId = 0;
    private int depthId = 0;
    private int width;
    private int height;

    public FrameBuffer(int width, int height) {
        this.width = width;
        this.height = height;
        init();
    }

    private void init() {
        // 1. Generar e inicializar el Framebuffer
        fboId = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fboId);

        // 2. Crear la textura donde renderizaremos (Color Attachment)
        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE,
                (java.nio.ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, textureId, 0);

        // 3. Crear Renderbuffer para profundidad (Depth Attachment), opcional pero
        // recomendable para 3D/Depth
        // Aunque el juego es 2D, es buena práctica tener depth buffer si se usa depth
        // test.
        depthId = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, depthId);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT, width, height);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthId);

        // 4. Verificar integridad
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            Logger.error("Error: Framebuffer no está completo!");
        }

        // 5. Desvincular
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public void bind() {
        glBindFramebuffer(GL_FRAMEBUFFER, fboId);
        glViewport(0, 0, width, height);
    }

    public void unbind() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public void resize(int newWidth, int newHeight) {
        if (this.width == newWidth && this.height == newHeight)
            return;

        this.width = newWidth;
        this.height = newHeight;
        cleanup();
        init();
    }

    public void cleanup() {
        glDeleteFramebuffers(fboId);
        glDeleteTextures(textureId);
        glDeleteRenderbuffers(depthId);
    }

    public int getTextureId() {
        return textureId;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
