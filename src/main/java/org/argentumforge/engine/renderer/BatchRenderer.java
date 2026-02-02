package org.argentumforge.engine.renderer;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.BufferUtils;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Clase Batch Renderer <br>
 * <br>
 * Utiliza OpenGL 3.3 Core Profile (VAO/VBO/EBO + Shaders).
 */
public class BatchRenderer {

    // Shader Source Code
    private static final String VERTEX_SHADER = "#version 330 core\n" +
            "layout (location = 0) in vec2 aPos;\n" +
            "layout (location = 1) in vec2 aTexCoords;\n" +
            "layout (location = 2) in vec4 aColor;\n" +
            "\n" +
            "out vec2 fTexCoords;\n" +
            "out vec4 fColor;\n" +
            "\n" +
            "uniform mat4 uProjection;\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "    fTexCoords = aTexCoords;\n" +
            "    fColor = aColor;\n" +
            "    gl_Position = uProjection * vec4(aPos, 0.0, 1.0);\n" +
            "}\n";

    private static final String FRAGMENT_SHADER = "#version 330 core\n" +
            "in vec2 fTexCoords;\n" +
            "in vec4 fColor;\n" +
            "\n" +
            "out vec4 color;\n" +
            "\n" +
            "uniform sampler2D uTexture;\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "    vec4 texColor = fColor * texture(uTexture, fTexCoords);\n" +
            "    if (texColor.a < 0.05) discard;\n" +
            "    color = texColor;\n" +
            "}\n";

    private static class Quad {
        float x, y, srcWidth, srcHeight, destWidth, destHeight;
        float srcX, srcY;
        float texWidth, texHeight;
        float r1, g1, b1, a1; // Vertex 0
        float r2, g2, b2, a2; // Vertex 1
        float r3, g3, b3, a3; // Vertex 2
        float r4, g4, b4, a4; // Vertex 3
        float skewX;
        boolean blend;
        Texture texture;
    }

    private final List<Quad> quads = new ArrayList<>();
    private int activeQuads = 0;

    // Vertex Data: Pos(2) + Tex(2) + Color(4) = 8 floats
    private static final int POS_SIZE = 2;
    private static final int TEX_SIZE = 2;
    private static final int COL_SIZE = 4;
    private static final int VERTEX_SIZE = POS_SIZE + TEX_SIZE + COL_SIZE; // 8 floats
    private static final int VERTEX_SIZE_BYTES = VERTEX_SIZE * Float.BYTES; // 32 bytes

    private FloatBuffer vertexBuffer;
    private int maxQuads = 1000;

    // OpenGL Objects
    private int vaoId, vboId, eboId;
    private ShaderProgram shader;
    private int projMatrixLoc;
    private final FloatBuffer orthoMatrixBuffer = BufferUtils.createFloatBuffer(16);

    public BatchRenderer() {
        init();
    }

    private void init() {
        // 1. Compile Shader
        shader = new ShaderProgram();
        shader.createVertexShader(VERTEX_SHADER);
        shader.createFragmentShader(FRAGMENT_SHADER);
        shader.link();

        // Cache Uniform Location
        projMatrixLoc = glGetUniformLocation(shader.programId, "uProjection");

        // 2. Buffers
        vertexBuffer = BufferUtils.createFloatBuffer(maxQuads * 4 * VERTEX_SIZE);

        // 3. VAO/VBO/EBO
        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);

        // VBO
        vboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer.capacity() * Float.BYTES, GL_DYNAMIC_DRAW);

        // EBO (Indices)
        generateEbo(maxQuads);

        // Pointers
        // Pos attribute
        glVertexAttribPointer(0, POS_SIZE, GL_FLOAT, false, VERTEX_SIZE_BYTES, 0);
        glEnableVertexAttribArray(0);

        // TexCoord attribute
        glVertexAttribPointer(1, TEX_SIZE, GL_FLOAT, false, VERTEX_SIZE_BYTES, POS_SIZE * Float.BYTES);
        glEnableVertexAttribArray(1);

        // Color attribute
        glVertexAttribPointer(2, COL_SIZE, GL_FLOAT, false, VERTEX_SIZE_BYTES, (POS_SIZE + TEX_SIZE) * Float.BYTES);
        glEnableVertexAttribArray(2);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        // Initialize Matrix
        updateProjectionMatrix();
    }

    private void generateEbo(int capacityQuads) {
        // 1 Quad = 6 Indices (2 Triangles)
        IntBuffer elementBuffer = BufferUtils.createIntBuffer(capacityQuads * 6);
        for (int i = 0; i < capacityQuads; i++) {
            int offset = i * 4;
            // Triangle 1: 3, 2, 0 (BottomRight, TopRight, BottomLeft) - CCW
            // Triangle 2: 0, 2, 1 (BottomLeft, TopRight, TopLeft) - CCW
            // Orden standard OpenGL CCW

            // Quad Vertices Order in fillQuadData:
            // 0: Bottom-Left
            // 1: Top-Left
            // 2: Top-Right
            // 3: Bottom-Right

            // Draw order:
            // 3, 2, 0 -> BottomRight, TopRight, BottomLeft
            // 0, 2, 1 -> BottomLeft, TopRight, TopLeft

            elementBuffer.put(offset + 3);
            elementBuffer.put(offset + 2);
            elementBuffer.put(offset + 0);

            elementBuffer.put(offset + 0);
            elementBuffer.put(offset + 2);
            elementBuffer.put(offset + 1);
        }
        elementBuffer.flip();

        if (eboId == 0)
            eboId = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, elementBuffer, GL_STATIC_DRAW);
    }

    private void ensureCapacity(int quadsNeeded) {
        if (quadsNeeded > maxQuads) {
            maxQuads = quadsNeeded + 500;
            vertexBuffer = BufferUtils.createFloatBuffer(maxQuads * 4 * VERTEX_SIZE);

            // Recrear VBO con nuevo tamaño
            glBindBuffer(GL_ARRAY_BUFFER, vboId);
            glBufferData(GL_ARRAY_BUFFER, vertexBuffer.capacity() * Float.BYTES, GL_DYNAMIC_DRAW);
            glBindBuffer(GL_ARRAY_BUFFER, 0);

            // Recrear EBO
            glBindVertexArray(vaoId); // EBO state is stored in VAO
            generateEbo(maxQuads);
            glBindVertexArray(0);
        }
    }

    public void begin() {
        activeQuads = 0;
    }

    public void draw(Texture texture, float x, float y, float srcX, float srcY, float srcWidth, float srcHeight,
            float destWidth, float destHeight, boolean blend, float alpha, RGBColor color) {
        draw(texture, x, y, srcX, srcY, srcWidth, srcHeight, destWidth, destHeight, 0.0f, blend, alpha, color);
    }

    public void draw(Texture texture, float x, float y, float srcX, float srcY, float srcWidth, float srcHeight,
            float destWidth, float destHeight, float skewX, boolean blend, float alpha, RGBColor color) {

        Quad quad;
        if (activeQuads < quads.size()) {
            quad = quads.get(activeQuads);
        } else {
            quad = new Quad();
            quads.add(quad);
        }
        activeQuads++;

        quad.x = x;
        quad.y = y;
        quad.srcWidth = srcWidth;
        quad.srcHeight = srcHeight;
        quad.destWidth = destWidth;
        quad.destHeight = destHeight;
        quad.srcX = srcX;
        quad.srcY = srcY;
        quad.skewX = skewX;
        quad.texWidth = texture.getTex_width();
        quad.texHeight = texture.getTex_height();
        quad.r1 = quad.r2 = quad.r3 = quad.r4 = color.getRed();
        quad.g1 = quad.g2 = quad.g3 = quad.g4 = color.getGreen();
        quad.b1 = quad.b2 = quad.b3 = quad.b4 = color.getBlue();
        quad.a1 = quad.a2 = quad.a3 = quad.a4 = alpha;
        quad.texture = texture;
        quad.blend = blend;
    }

    public void draw(Texture texture, float x, float y, float srcX, float srcY, float srcWidth, float srcHeight,
            float destWidth, float destHeight, boolean blend,
            float r1, float g1, float b1, float a1,
            float r2, float g2, float b2, float a2,
            float r3, float g3, float b3, float a3,
            float r4, float g4, float b4, float a4) {

        Quad quad;
        if (activeQuads < quads.size()) {
            quad = quads.get(activeQuads);
        } else {
            quad = new Quad();
            quads.add(quad);
        }
        activeQuads++;

        quad.x = x;
        quad.y = y;
        quad.srcWidth = srcWidth;
        quad.srcHeight = srcHeight;
        quad.destWidth = destWidth;
        quad.destHeight = destHeight;
        quad.srcX = srcX;
        quad.srcY = srcY;
        quad.texWidth = texture.getTex_width();
        quad.texHeight = texture.getTex_height();
        quad.r1 = r1;
        quad.g1 = g1;
        quad.b1 = b1;
        quad.a1 = a1;
        quad.r2 = r2;
        quad.g2 = g2;
        quad.b2 = b2;
        quad.a2 = a2;
        quad.r3 = r3;
        quad.g3 = g3;
        quad.b3 = b3;
        quad.a3 = a3;
        quad.r4 = r4;
        quad.g4 = g4;
        quad.b4 = b4;
        quad.a4 = a4;
        quad.texture = texture;
        quad.blend = blend;
    }

    public void end() {
        if (activeQuads == 0)
            return;

        ensureCapacity(activeQuads);

        // 1. Prepare Shader
        shader.bind();

        updateProjectionMatrix();
        glUniformMatrix4fv(projMatrixLoc, false, orthoMatrixBuffer);

        // 2. Bind VAO
        glBindVertexArray(vaoId);

        // 3. Fill Buffer (Pass 1)
        vertexBuffer.clear();
        for (int i = 0; i < activeQuads; i++) {
            fillQuadData(quads.get(i));
        }
        vertexBuffer.flip();

        // 4. Upload ALL Data Once
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferSubData(GL_ARRAY_BUFFER, 0, vertexBuffer);

        // 5. Draw Batches (Pass 2)
        Texture lastTexture = null;
        int batchStart = 0;
        int quadsInBatch = 0;
        boolean lastBlend = false;

        for (int i = 0; i <= activeQuads; i++) {
            Quad quad = (i < activeQuads) ? quads.get(i) : null;

            // Detect batch change or end
            boolean stateChanged = (lastTexture != null
                    && (quad == null || lastTexture != quad.texture || lastBlend != quad.blend));

            if (i > 0 && stateChanged) {
                renderBatch(batchStart, quadsInBatch, lastTexture, lastBlend);
                batchStart = i;
                quadsInBatch = 0;
            }

            if (quad != null) {
                lastTexture = quad.texture;
                lastBlend = quad.blend;
                quadsInBatch++;
            }
        }

        glBindVertexArray(0);
        shader.unbind();
    }

    private void updateProjectionMatrix() {
        int width = 0, height = 0;
        if (org.argentumforge.engine.Engine.INSTANCE.getWindow() != null) {
            width = org.argentumforge.engine.Engine.INSTANCE.getWindow().getWidth();
            height = org.argentumforge.engine.Engine.INSTANCE.getWindow().getHeight();
        }

        orthoMatrixBuffer.clear();

        // col 1
        orthoMatrixBuffer.put(2.0f / width);
        orthoMatrixBuffer.put(0.0f);
        orthoMatrixBuffer.put(0.0f);
        orthoMatrixBuffer.put(0.0f);

        // col 2
        orthoMatrixBuffer.put(0.0f);
        orthoMatrixBuffer.put(-2.0f / height); // Flipped Y
        orthoMatrixBuffer.put(0.0f);
        orthoMatrixBuffer.put(0.0f);

        // col 3
        orthoMatrixBuffer.put(0.0f);
        orthoMatrixBuffer.put(0.0f);
        orthoMatrixBuffer.put(-1.0f); // z -1
        orthoMatrixBuffer.put(0.0f);

        // col 4
        orthoMatrixBuffer.put(-1.0f);
        orthoMatrixBuffer.put(1.0f); // Flipped Y translation
        orthoMatrixBuffer.put(0.0f);
        orthoMatrixBuffer.put(1.0f);

        orthoMatrixBuffer.flip();
    }

    private void fillQuadData(Quad quad) {
        float u0 = quad.srcX / quad.texWidth;
        float v0 = (quad.srcY + quad.srcHeight) / quad.texHeight;
        float u1 = (quad.srcX + quad.srcWidth) / quad.texWidth;
        float v1 = quad.srcY / quad.texHeight;

        // Vértice 0 (Bottom-Left en logica, pero Top-Down en UVs)
        vertexBuffer.put(quad.x).put(quad.y + quad.destHeight);
        vertexBuffer.put(u0).put(v0);
        vertexBuffer.put(quad.r1).put(quad.g1).put(quad.b1).put(quad.a1);

        // Vértice 1 (Top-Left)
        vertexBuffer.put(quad.x + quad.skewX).put(quad.y);
        vertexBuffer.put(u0).put(v1);
        vertexBuffer.put(quad.r2).put(quad.g2).put(quad.b2).put(quad.a2);

        // Vértice 2 (Top-Right)
        vertexBuffer.put(quad.x + quad.destWidth + quad.skewX).put(quad.y);
        vertexBuffer.put(u1).put(v1);
        vertexBuffer.put(quad.r3).put(quad.g3).put(quad.b3).put(quad.a3);

        // Vértice 3 (Bottom-Right)
        vertexBuffer.put(quad.x + quad.destWidth).put(quad.y + quad.destHeight);
        vertexBuffer.put(u1).put(v0);
        vertexBuffer.put(quad.r4).put(quad.g4).put(quad.b4).put(quad.a4);
    }

    private void renderBatch(int start, int count, Texture texture, boolean blend) {
        texture.bind();

        if (blend) {
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE);
        } else {
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        }

        // Draw Elements
        // 6 indices por quad. int indices (4 bytes)
        long offset = (long) start * 6 * 4;
        glDrawElements(GL_TRIANGLES, count * 6, GL_UNSIGNED_INT, offset);
    }
}