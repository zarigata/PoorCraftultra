package com.poorcraftultra.core;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.util.stream.Collectors;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Manages OpenGL shader programs.
 */
public class Shader {
    private int programId;
    private int vertexShaderId;
    private int fragmentShaderId;
    private final String vertexPath;
    private final String fragmentPath;

    /**
     * Creates a new Shader instance.
     *
     * @param vertexPath   Path to the vertex shader source file (relative to resources)
     * @param fragmentPath Path to the fragment shader source file (relative to resources)
     */
    public Shader(String vertexPath, String fragmentPath) {
        this.vertexPath = vertexPath;
        this.fragmentPath = fragmentPath;
    }

    /**
     * Loads, compiles, and links the shader program.
     *
     * @throws RuntimeException if shader compilation or linking fails
     */
    public void load() {
        try {
            // Read shader sources
            String vertexSource = readShaderFile(vertexPath);
            String fragmentSource = readShaderFile(fragmentPath);

            // Compile vertex shader
            vertexShaderId = compileShader(GL_VERTEX_SHADER, vertexSource);

            // Compile fragment shader
            fragmentShaderId = compileShader(GL_FRAGMENT_SHADER, fragmentSource);

            // Create and link program
            programId = glCreateProgram();
            glAttachShader(programId, vertexShaderId);
            glAttachShader(programId, fragmentShaderId);
            glLinkProgram(programId);

            // Check for linking errors
            if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE) {
                String errorLog = glGetProgramInfoLog(programId);
                throw new RuntimeException("Failed to link shader program: " + errorLog);
            }

            // Detach and delete shaders after linking
            glDetachShader(programId, vertexShaderId);
            glDetachShader(programId, fragmentShaderId);
            glDeleteShader(vertexShaderId);
            glDeleteShader(fragmentShaderId);

            System.out.println("Shader program loaded successfully (ID: " + programId + ")");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load shader files", e);
        }
    }

    /**
     * Reads a shader source file from resources.
     *
     * @param path The resource path
     * @return The shader source code
     * @throws IOException if the file cannot be read
     */
    private String readShaderFile(String path) throws IOException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(path);
        if (inputStream == null) {
            throw new IOException("Shader file not found: " + path);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    /**
     * Compiles a shader.
     *
     * @param type   The shader type (GL_VERTEX_SHADER or GL_FRAGMENT_SHADER)
     * @param source The shader source code
     * @return The shader ID
     * @throws RuntimeException if compilation fails
     */
    private int compileShader(int type, String source) {
        int shaderId = glCreateShader(type);
        glShaderSource(shaderId, source);
        glCompileShader(shaderId);

        // Check for compilation errors
        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == GL_FALSE) {
            String errorLog = glGetShaderInfoLog(shaderId);
            String shaderType = (type == GL_VERTEX_SHADER) ? "vertex" : "fragment";
            throw new RuntimeException("Failed to compile " + shaderType + " shader: " + errorLog);
        }

        return shaderId;
    }

    /**
     * Binds the shader program for use.
     */
    public void bind() {
        glUseProgram(programId);
    }

    /**
     * Unbinds the shader program.
     */
    public void unbind() {
        glUseProgram(0);
    }

    /**
     * Gets the location of a uniform variable.
     *
     * @param name The uniform variable name
     * @return The uniform location, or -1 if not found
     */
    public int getUniformLocation(String name) {
        return glGetUniformLocation(programId, name);
    }

    /**
     * Sets a mat4 uniform value.
     *
     * @param name   The uniform variable name
     * @param matrix The matrix value
     */
    public void setUniformMat4(String name, Matrix4f matrix) {
        int location = getUniformLocation(name);
        if (location != -1) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer buffer = stack.mallocFloat(16);
                matrix.get(buffer);
                glUniformMatrix4fv(location, false, buffer);
            }
        }
    }

    /**
     * Sets a vec3 uniform value.
     *
     * @param name   The uniform variable name
     * @param vector The vector value
     */
    public void setUniformVec3(String name, Vector3f vector) {
        int location = getUniformLocation(name);
        if (location != -1) {
            glUniform3f(location, vector.x, vector.y, vector.z);
        }
    }

    /**
     * Sets a float uniform value.
     *
     * @param name  The uniform variable name
     * @param value The float value
     */
    public void setUniformFloat(String name, float value) {
        int location = getUniformLocation(name);
        if (location != -1) {
            glUniform1f(location, value);
        }
    }

    /**
     * Cleans up shader resources.
     */
    public void cleanup() {
        unbind();
        if (programId != 0) {
            glDeleteProgram(programId);
            System.out.println("Shader program cleaned up");
        }
    }

    /**
     * Gets the shader program ID.
     *
     * @return The program ID
     */
    public int getProgramId() {
        return programId;
    }
}
