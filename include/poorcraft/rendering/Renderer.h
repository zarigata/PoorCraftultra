#ifndef POORCRAFT_RENDERING_RENDERER_H
#define POORCRAFT_RENDERING_RENDERER_H

#include <cstddef>
#include <cstdint>
#include <string>

namespace poorcraft::rendering
{
struct PerformanceMetrics;
}

#include <glm/glm.hpp>

namespace poorcraft::rendering
{
enum class RendererBackend
{
    Vulkan,
    OpenGL
};

struct RendererCapabilities
{
    RendererBackend backend{RendererBackend::Vulkan};
    bool supportsRayTracing{false};
    unsigned int maxTextureSize{0};
    std::string backendVersion{};
};

class Renderer
{
public:
    using BufferHandle = std::uint32_t;
    using TextureHandle = std::uint32_t;

    virtual ~Renderer() = default;

    virtual bool initialize() = 0;
    virtual void shutdown() = 0;

    virtual void beginFrame() = 0;
    virtual void clear(float r, float g, float b, float a) = 0;
    virtual void endFrame() = 0;

    virtual RendererCapabilities getCapabilities() const = 0;
    virtual bool isVSyncEnabled() const = 0;
    virtual void setVSync(bool enabled) = 0;
    virtual bool isVSyncSupported() const { return true; }

    virtual void setViewProjection(const glm::mat4& view, const glm::mat4& projection) = 0;

    virtual BufferHandle createVertexBuffer(const void* data, std::size_t size) = 0;
    virtual BufferHandle createIndexBuffer(const void* data, std::size_t size) = 0;
    virtual void destroyBuffer(BufferHandle handle) = 0;

    // Textures use RGBA8 data; slot 0 reserved for block atlas.
    virtual TextureHandle createTexture(const void* data, std::uint32_t width, std::uint32_t height, std::uint32_t channels) = 0;
    virtual void destroyTexture(TextureHandle handle) = 0;
    virtual void bindTexture(TextureHandle handle, std::uint32_t slot) = 0;

    // Lighting parameters packed as vec4s for std140 alignment in shaders
    struct LightingParams {
        glm::vec4 sunDirAndIntensity{0.0f, -1.0f, 0.0f, 1.0f};  // xyz = direction, w = intensity
        glm::vec4 sunColor{1.0f, 1.0f, 1.0f, 0.0f};              // rgb = color, w unused
        glm::vec4 ambientColorAndIntensity{0.2f, 0.3f, 0.4f, 0.2f}; // rgb = color, w = intensity
    };

    virtual void setLightingParams(const LightingParams& params) = 0;

    virtual void drawIndexed(BufferHandle vertexBuffer, BufferHandle indexBuffer, std::uint32_t indexCount, const glm::mat4& modelMatrix) = 0;

    /**
     * UI rendering happens after the 3D world render pass but before presentation.
     * The application issues ImGui::NewFrame/Render; the renderer binds backend state.
     */
    virtual bool initializeUI() = 0;
    virtual void shutdownUI() = 0;
    virtual void beginUIPass() = 0;
    virtual void renderUI() = 0;

    virtual void beginPerformanceCapture() = 0;
    virtual void endPerformanceCapture() = 0;
    virtual PerformanceMetrics getPerformanceMetrics() const = 0;

protected:
    Renderer() = default;
};
} // namespace poorcraft::rendering

#endif // POORCRAFT_RENDERING_RENDERER_H
