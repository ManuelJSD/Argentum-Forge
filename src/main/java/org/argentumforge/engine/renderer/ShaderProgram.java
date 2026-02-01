package org.argentumforge.engine.renderer;

import org.tinylog.Logger;

import static org.lwjgl.opengl.GL20.*;

public class ShaderProgram {
    final int programId;

    private int vertexShaderId;
    private int fragmentShaderId;

    public ShaderProgram() {
        programId = glCreateProgram();
        if (programId == 0) {
            Logger.error("Could not create Shader Program");
            throw new RuntimeException("Could not create Shader Program");
        }
    }

    public void createVertexShader(String shaderCode) {
        vertexShaderId = createShader(shaderCode, GL_VERTEX_SHADER);
    }

    public void createFragmentShader(String shaderCode) {
        fragmentShaderId = createShader(shaderCode, GL_FRAGMENT_SHADER);
    }

    protected int createShader(String shaderCode, int shaderType) {
        int shaderId = glCreateShader(shaderType);
        if (shaderId == 0) {
            Logger.error("Error creating shader. Type: {}", shaderType);
            throw new RuntimeException("Error creating shader. Type: " + shaderType);
        }

        glShaderSource(shaderId, shaderCode);
        glCompileShader(shaderId);

        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == 0) {
            String errorLog = glGetShaderInfoLog(shaderId, 1024);
            Logger.error("Shader Error (Type {}): {}", shaderType, errorLog);
            throw new RuntimeException("Error compiling Shader code: " + errorLog);
        }
        Logger.debug("Shader compiled successfully. Type: {}", shaderType);

        glAttachShader(programId, shaderId);

        return shaderId;
    }

    public void link() {
        glLinkProgram(programId);
        if (glGetProgrami(programId, GL_LINK_STATUS) == 0) {
            String log = glGetProgramInfoLog(programId, 1024);
            Logger.error("Program Link Error: {}", log);
            throw new RuntimeException("Error linking Shader Program: " + log);
        }
        Logger.debug("Shader program linked successfully.");

        if (vertexShaderId != 0) {
            glDetachShader(programId, vertexShaderId);
        }
        if (fragmentShaderId != 0) {
            glDetachShader(programId, fragmentShaderId);
        }

        glValidateProgram(programId);
        if (glGetProgrami(programId, GL_VALIDATE_STATUS) == 0) {
            Logger.warn("Warning validating Shader Program: {}", glGetProgramInfoLog(programId, 1024));
        }
    }

    public void bind() {
        glUseProgram(programId);
    }

    public void unbind() {
        glUseProgram(0);
    }

    public void setUniform(String name, int value) {
        int location = glGetUniformLocation(programId, name);
        glUniform1i(location, value);
    }

    public void setUniform(String name, float value) {
        int location = glGetUniformLocation(programId, name);
        glUniform1f(location, value);
    }

    public void cleanup() {
        unbind();
        if (programId != 0) {
            glDeleteProgram(programId);
        }
    }
}
