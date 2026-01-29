package org.argentumforge.engine.renderer;

import static org.lwjgl.opengl.GL11.*;

public class PostProcessor {
    private ShaderProgram shader;
    private int screenTexture;
    private int width, height;

    private static final String VERTEX_SHADER = "#version 120\n" +
            "varying vec2 vTexCoord;\n" +
            "void main() {\n" +
            "    vTexCoord = gl_MultiTexCoord0.xy;\n" +
            "    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;\n" +
            "}\n";

    private static final String FRAGMENT_SHADER = "#version 120\n" +
            "uniform sampler2D uTexture;\n" +
            "uniform int uFilterType;\n" +
            "uniform float uExposure;\n" +
            "uniform float uContrast;\n" +
            "uniform float uSaturation;\n" +
            "uniform int uBloomActive;\n" +
            "uniform float uBloomIntensity;\n" +
            "uniform float uBloomThreshold;\n" +
            "uniform int uDoFActive;\n" +
            "uniform float uDoFFocus;\n" +
            "uniform float uDoFRange;\n" +
            "uniform int uGrainActive;\n" +
            "uniform float uGrainIntensity;\n" +
            "uniform float uTime;\n" +
            "uniform float uZoom;\n" +
            "varying vec2 vTexCoord;\n" +

            "float rand(vec2 co) {\n" +
            "    return fract(sin(dot(co.xy ,vec2(12.9898,78.233))) * 43758.5453);\n" +
            "}\n" +

            "void main() {\n" +
            "    // 0. Apply Zoom\n" +
            "    vec2 uv = (vTexCoord - 0.5) / max(0.001, uZoom) + 0.5;\n" +
            "    vec4 texColor = texture2D(uTexture, uv);\n" +
            "    vec3 color = texColor.rgb;\n" +

            "    // 1. Color Grading (Exposure, Contrast, Saturation)\n" +
            "    color *= uExposure;\n" +
            "    color = ((color - 0.5) * uContrast) + 0.5;\n" +
            "    float luminance = dot(color, vec3(0.299, 0.587, 0.114));\n" +
            "    color = mix(vec3(luminance), color, uSaturation);\n" +

            "    // 2. Filters (Grayscale, Sepia, etc.)\n" +
            "    if (uFilterType == 1) {\n" + // Grayscale
            "        color = vec3(luminance);\n" +
            "    } else if (uFilterType == 2) {\n" + // Sepia
            "        float r = (color.r * 0.393) + (color.g * 0.769) + (color.b * 0.189);\n" +
            "        float g = (color.r * 0.349) + (color.g * 0.686) + (color.b * 0.168);\n" +
            "        float b = (color.r * 0.272) + (color.g * 0.534) + (color.b * 0.131);\n" +
            "        color = vec3(r, g, b);\n" +
            "    } else if (uFilterType == 3) {\n" + // Vintage
            "        color = vec3(luminance * 0.8, luminance * 0.7, luminance * 0.9);\n" +
            "    } else if (uFilterType == 4) {\n" + // Warm
            "        color *= vec3(1.1, 1.05, 0.9);\n" +
            "    } else if (uFilterType == 5) {\n" + // Cold
            "        color *= vec3(0.9, 1.05, 1.2);\n" +
            "    }\n" +

            "    // 3. Bloom (Improved 7x7)\n" +
            "    if (uBloomActive != 0) {\n" +
            "        vec3 glow = vec3(0.0);\n" +
            "        float step = 0.003;\n" +
            "        for(int x = -3; x <= 3; x++) {\n" +
            "            for(int y = -3; y <= 3; y++) {\n" +
            "                vec3 smp = texture2D(uTexture, uv + vec2(float(x)*step, float(y)*step)).rgb;\n" +
            "                float br = dot(smp, vec3(0.299, 0.587, 0.114));\n" +
            "                if (br > uBloomThreshold) {\n" +
            "                    glow += (smp - uBloomThreshold) / max(0.01, 1.0 - uBloomThreshold);\n" +
            "                }\n" +
            "            }\n" +
            "        }\n" +
            "        color += (glow / 49.0) * uBloomIntensity;\n" +
            "    }\n" +

            "    // 4. Depth of Field (Radial Blur)\n" +
            "    if (uDoFActive != 0) {\n" +
            "        float dist = distance(uv, vec2(0.5, 0.5));\n" +
            "        float blurAmount = smoothstep(uDoFFocus - uDoFRange, uDoFFocus + uDoFRange, dist);\n" +
            "        if (blurAmount > 0.02) {\n" +
            "            vec3 blurColor = vec3(0.0);\n" +
            "            for(int i = 0; i < 8; i++) {\n" +
            "                float angle = float(i) * 0.785;\n" +
            "                vec2 off = vec2(cos(angle), sin(angle)) * blurAmount * 0.015;\n" +
            "                blurColor += texture2D(uTexture, uv + off).rgb;\n" +
            "            }\n" +
            "            color = mix(color, blurColor / 8.0, blurAmount);\n" +
            "        }\n" +
            "    }\n" +

            "    // 5. Film Grain\n" +
            "    if (uGrainActive != 0) {\n" +
            "        float noise = rand(uv + vec2(uTime * 0.01, uTime * 0.02));\n" +
            "        color += (noise - 0.5) * uGrainIntensity;\n" +
            "    }\n" +

            "    gl_FragColor = vec4(color, texColor.a);\n" +
            "}\n";

    public PostProcessor(int width, int height) {
        this.width = width;
        this.height = height;
        init();
    }

    private void init() {
        shader = new ShaderProgram();
        shader.createVertexShader(VERTEX_SHADER);
        shader.createFragmentShader(FRAGMENT_SHADER);
        shader.link();

        screenTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, screenTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE,
                (java.nio.ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    }

    public void resize(int newWidth, int newHeight) {
        this.width = newWidth;
        this.height = newHeight;
        glDeleteTextures(screenTexture);
        screenTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, screenTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE,
                (java.nio.ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void apply(RenderSettings settings, float time) {
        // 1. Capture the screen backbuffer to our texture
        glBindTexture(GL_TEXTURE_2D, screenTexture);
        glCopyTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, 0, 0, width, height, 0);

        // 2. Prepare to draw full-screen quad with shader
        shader.bind();
        shader.setUniform("uFilterType", settings.getPhotoColorFilter().ordinal());
        shader.setUniform("uTexture", 0);

        // Advanced Uniforms
        shader.setUniform("uExposure", settings.getPhotoExposure());
        shader.setUniform("uContrast", settings.getPhotoContrast());
        shader.setUniform("uSaturation", settings.getPhotoSaturation());

        shader.setUniform("uBloomActive", settings.isPhotoBloom() ? 1 : 0);
        shader.setUniform("uBloomIntensity", settings.getBloomIntensity());
        shader.setUniform("uBloomThreshold", settings.getPhotoBloomThreshold());

        shader.setUniform("uDoFActive", settings.isPhotoDoF() ? 1 : 0);
        shader.setUniform("uDoFFocus", settings.getDofFocus());
        shader.setUniform("uDoFRange", settings.getDofRange());

        shader.setUniform("uGrainActive", settings.isPhotoGrain() ? 1 : 0);
        shader.setUniform("uGrainIntensity", settings.getGrainIntensity());
        shader.setUniform("uTime", time);
        shader.setUniform("uZoom", settings.getPhotoZoom());

        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, screenTexture);

        // Reset state for post-process quad
        glDisable(GL_BLEND);
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

        glBegin(GL_QUADS);
        glTexCoord2f(0, 1);
        glVertex2f(0, 0);
        glTexCoord2f(1, 1);
        glVertex2f(width, 0);
        glTexCoord2f(1, 0);
        glVertex2f(width, height);
        glTexCoord2f(0, 0);
        glVertex2f(0, height);
        glEnd();

        glEnable(GL_BLEND); // Restore blend for things like vignette or UI
        shader.unbind();
    }

    public void cleanup() {
        if (shader != null)
            shader.cleanup();
        glDeleteTextures(screenTexture);
    }
}
