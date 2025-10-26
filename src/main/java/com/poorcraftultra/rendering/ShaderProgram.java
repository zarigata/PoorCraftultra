package com.poorcraftultra.rendering;

import org.lwjgl.opengl.GL20;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * ShaderProgram manages GLSL shaders for rendering.
 * Handles shader compilation, linking, and uniform management.
 */
public class ShaderProgram {

    private final int programId;
    private final Map<String, Integer> uniformCache = new HashMap<>();

    /**
     * Creates a shader program from vertex and fragment shader files.
     * @param vertexPath Path to vertex shader file (relative to resources)
     * @param fragmentPath Path to fragment shader file (relative to resources)
     */
    public ShaderProgram(String vertexPath, String fragmentPath) {
        int vertexShader = compileShader(loadShader(vertexPath), GL20.GL_VERTEX_SHADER);
        int fragmentShader = compileShader(loadShader(fragmentPath), GL20.GL_FRAGMENT_SHADER);

        programId = GL20.glCreateProgram();
        GL20.glAttachShader(programId, vertexShader);
        GL20.glAttachShader(programId, fragmentShader);
        link();

        // Cleanup shaders after linking
        GL20.glDetachShader(programId, vertexShader);
        GL20.glDetachShader(programId, fragmentShader);
        GL20.glDeleteShader(vertexShader);
        GL20.glDeleteShader(fragmentShader);
    }

    /**
     * Loads shader source from resources.
     * @param path Resource path to shader file
     * @return Shader source as string
     */
    private String loadShader(String path) {
        StringBuilder source = new StringBuilder();
        try (InputStream is = getClass().getResourceAsStream(path);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                source.append(line).append("\n");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load shader: " + path, e);
        }
        return source.toString();
    }

    /**
     * Compiles a shader from source.
     * @param source Shader source code
     * @param type Shader type (GL_VERTEX_SHADER or GL_FRAGMENT_SHADER)
     * @return Compiled shader ID
     */
    private int compileShader(String source, int type) {
        int shaderId = GL20.glCreateShader(type);
        GL20.glShaderSource(shaderId, source);
        GL20.glCompileShader(shaderId);

        if (GL20.glGetShaderi(shaderId, GL20.GL_COMPILE_STATUS) == GL20.GL_FALSE) {
            String infoLog = GL20.glGetShaderInfoLog(shaderId);
            GL20.glDeleteShader(shaderId);
            throw new RuntimeException("Shader compilation failed: " + infoLog);
        }

        return shaderId;
    }

    /**
     * Links the shader program.
     */
    private void link() {
        GL20.glLinkProgram(programId);

        if (GL20.glGetProgrami(programId, GL20.GL_LINK_STATUS) == GL20.GL_FALSE) {
            String infoLog = GL20.glGetProgramInfoLog(programId);
            GL20.glDeleteProgram(programId);
            throw new RuntimeException("Shader linking failed: " + infoLog);
        }
    }

    /**
     * Binds this shader program for use.
     */
    public void bind() {
        GL20.glUseProgram(programId);
    }

    /**
     * Unbinds the current shader program.
     */
    public void unbind() {
        GL20.glUseProgram(0);
    }

    /**
     * Gets the location of a uniform variable.
     * @param name Uniform name
     * @return Uniform location
     */
    private int getUniformLocation(String name) {
        if (uniformCache.containsKey(name)) {
            return uniformCache.get(name);
        }

        int location = GL20.glGetUniformLocation(programId, name);
        uniformCache.put(name, location);
        return location;
    }

    /**
     * Sets a matrix uniform.
     * @param name Uniform name
     * @param matrix Matrix value
     */
    public void setUniform(String name, Matrix4f matrix) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(16);
            matrix.get(buffer);
            GL20.glUniformMatrix4fv(getUniformLocation(name), false, buffer);
        }
    }

    /**
     * Sets a vector uniform.
     * @param name Uniform name
     * @param vector Vector value
     */
    public void setUniform(String name, Vector3f vector) {
        GL20.glUniform3f(getUniformLocation(name), vector.x, vector.y, vector.z);
    }

    /**
     * Sets a float uniform.
     * @param name Uniform name
     * @param value Float value
     */
    public void setUniform(String name, float value) {
        GL20.glUniform1f(getUniformLocation(name), value);
    }

    /**
     * Sets an int uniform.
     * @param name Uniform name
     * @param value Int value
     */
    public void setUniform(String name, int value) {
        GL20.glUniform1i(getUniformLocation(name), value);
    }

    /**
     * Gets the number of cached uniforms (for testing).
     * @return Cache size
     */
    int getUniformCacheSize() {
        return uniformCache.size();
    }

    /**
     * Cleans up the shader program.
     */
    public void cleanup() {
        GL20.glDeleteProgram(programId);
    }
}
