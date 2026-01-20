package org.argentumforge.engine.renderer;

import org.argentumforge.engine.Window;
import org.argentumforge.engine.scenes.Scene;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;

public class Renderer {
    private final Camera2D camera;
    private final Shader shader;
    private Matrix4f projection;

    public static final BatchRenderer batch = new BatchRenderer();

    public Renderer() {
        camera = new Camera2D(0, 0);

        shader = new Shader(
                "resources/shaders/default.vert",
                "resources/shaders/default.frag"
        );

        projection = new Matrix4f()
                .ortho2D(0, Window.SCREEN_WIDTH, Window.SCREEN_HEIGHT, 0);
    }

    public void updateProjection(float left, float right, float bottom, float top) {
        this.projection = new Matrix4f()
                .ortho2D(left, right, bottom, top);
    }


    public void draw(
            Texture tex,
            float x, float y,
            float srcX, float srcY,
            float srcW, float srcH,
            float destWidth, float destHeight,
            boolean blend,
            float alpha,
            RGBColor color
    ) {
        if (tex == null || tex.getId() == 0) return;

        float u0 = srcX / tex.getTex_width();
        float u1 = (srcX + srcW) / tex.getTex_width();
        float v1 = srcY / tex.getTex_height();
        float v0 = (srcY + srcH) / tex.getTex_height();

        batch.submitQuad(
                tex,
                x, y,
                destWidth, destHeight,
                u0, v0, u1, v1,
                blend,
                color.getRed(),
                color.getGreen(),
                color.getBlue(),
                alpha
        );
    }

    public void render(Scene scene) {
        shader.bind();

        int texLoc = glGetUniformLocation(shader.getId(), "uTexture");
        glUniform1i(texLoc, 0);

        int projLoc = glGetUniformLocation(shader.getId(), "uProjection");
        FloatBuffer projBuf = MemoryUtil.memAllocFloat(16);
        projection.get(projBuf);
        glUniformMatrix4fv(projLoc, false, projBuf);

        int viewLoc = glGetUniformLocation(shader.getId(), "uView");
        FloatBuffer viewBuf = MemoryUtil.memAllocFloat(16);
        camera.getViewMatrix().get(viewBuf);
        glUniformMatrix4fv(viewLoc, false, viewBuf);

        batch.begin();
        scene.render();
        batch.end();

        MemoryUtil.memFree(projBuf);
        MemoryUtil.memFree(viewBuf);

        shader.unbind();
    }

    public void beginOffscreen(int width, int height) {
        shader.bind();

        projection = new Matrix4f()
                .ortho2D(0, width, height, 0);

        int projLoc = glGetUniformLocation(shader.getId(), "uProjection");
        FloatBuffer projBuf = MemoryUtil.memAllocFloat(16);
        projection.get(projBuf);
        glUniformMatrix4fv(projLoc, false, projBuf);

        int viewLoc = glGetUniformLocation(shader.getId(), "uView");
        FloatBuffer viewBuf = MemoryUtil.memAllocFloat(16);
        new Matrix4f().identity().get(viewBuf);
        glUniformMatrix4fv(viewLoc, false, viewBuf);

        MemoryUtil.memFree(projBuf);
        MemoryUtil.memFree(viewBuf);

        batch.begin();
    }

    public void endOffscreen() {
        batch.end();
        shader.unbind();
    }

}
