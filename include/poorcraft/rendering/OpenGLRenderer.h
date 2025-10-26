#ifndef POORCRAFT_RENDERING_OPENGLRENDERER_H
#define POORCRAFT_RENDERING_OPENGLRENDERER_H

#include "poorcraft/rendering/Renderer.h"

#include <SDL2/SDL.h>

#include <glm/mat4x4.hpp>

#include <array>
#include <chrono>
#include <unordered_map>
#include <vector>

struct ImDrawData;

namespace poorcraft::core
{
class Window;
}

namespace poorcraft::rendering
{
class OpenGLRenderer : public Renderer
{
public:
    explicit OpenGLRenderer(core::Window& window);
    ~OpenGLRenderer() override = default;

    bool initialize() override;
    void shutdown() override;

    void beginFrame() override;
    void clear(float r, float g, float b, float a) override;
    void endFrame() override;

    RendererCapabilities getCapabilities() const override;
    bool isVSyncEnabled() const override;
    void setVSync(bool enabled) override;

    void setViewProjection(const glm::mat4& view, const glm::mat4& projection) override;

    BufferHandle createVertexBuffer(const void* data, std::size_t size) override;
    BufferHandle createIndexBuffer(const void* data, std::size_t size) override;
    void destroyBuffer(BufferHandle handle) override;

    void drawIndexed(BufferHandle vertexBuffer, BufferHandle indexBuffer, std::uint32_t indexCount, const glm::mat4& modelMatrix) override;

    TextureHandle createTexture(const void* data, std::uint32_t width, std::uint32_t height, std::uint32_t channels) override;
    void destroyTexture(TextureHandle handle) override;
    void bindTexture(TextureHandle handle, std::uint32_t slot) override;
    void setLightingParams(const LightingParams& params) override;

    bool initializeUI() override;
    void shutdownUI() override;
    void beginUIPass() override;
    void renderUI() override;

    void beginPerformanceCapture() override;
    void endPerformanceCapture() override;
    PerformanceMetrics getPerformanceMetrics() const override;

private:
    bool createGLContext();
    bool loadGLFunctions();
    void applyVSync();
    bool createShaderProgram();
    void destroyShaderProgram();
    void updateProjection();
    void applyLightingUniforms();
    TextureHandle createDefaultTexture();

    struct BufferResource
    {
        unsigned int buffer{0};
        std::size_t size{0};
        unsigned int target{0};
        unsigned int vao{0};
    };

    struct DrawCommand
    {
        BufferHandle vertexBuffer{};
        BufferHandle indexBuffer{};
        std::uint32_t indexCount{};
        glm::mat4 modelMatrix{1.0f};
    };

    struct TextureResource
    {
        unsigned int id{0};
        std::uint32_t width{0};
        std::uint32_t height{0};
    };

    core::Window& m_window;
    SDL_GLContext m_glContext{nullptr};
    bool m_vsyncEnabled{true};
    bool m_imguiInitialized{false};

    unsigned int m_shaderProgram{0};
    int m_viewProjLocation{-1};
    int m_modelLocation{-1};
    int m_textureLocation{-1};
    int m_sunDirLocation{-1};
    int m_sunColorLocation{-1};
    int m_sunIntensityLocation{-1};
    int m_ambientColorLocation{-1};
    int m_ambientIntensityLocation{-1};

    glm::mat4 m_viewMatrix{1.0f};
    glm::mat4 m_projectionMatrix{1.0f};
    glm::mat4 m_viewProjection{1.0f};

    BufferHandle m_nextBufferHandle{1};
    std::unordered_map<BufferHandle, BufferResource> m_vertexBuffers;
    std::unordered_map<BufferHandle, BufferResource> m_indexBuffers;
    std::vector<DrawCommand> m_drawCommands;

    TextureHandle m_nextTextureHandle{1};
    std::unordered_map<TextureHandle, TextureResource> m_textures;
    std::array<TextureHandle, 8> m_activeTextures{};
    Renderer::LightingParams m_lightingParams{};
    bool m_lightingDirty{true};
    TextureHandle m_defaultTexture{0};

    unsigned int m_timerQueries[4]{0, 0, 0, 0};
    bool m_timerQueriesSupported{false};
    std::chrono::high_resolution_clock::time_point m_frameCaptureStart{};
    bool m_performanceCaptureActive{false};
    PerformanceMetrics m_currentMetrics{};
    PerformanceMetrics m_smoothedMetrics{};
    std::array<PerformanceMetrics, 60> m_metricsHistory{};
    std::size_t m_metricsHistoryIndex{0};
    std::size_t m_metricsHistoryCount{0};
    std::uint32_t m_lastDrawCallCount{0};
    std::uint64_t m_lastVertexCount{0};
    std::uint64_t m_lastTriangleCount{0};

    bool createTimerQueries();
    void destroyTimerQueries();
    double getQueryResultMs(unsigned int query) const;
};
} // namespace poorcraft::rendering

#endif // POORCRAFT_RENDERING_OPENGLRENDERER_H
