#ifndef POORCRAFT_RENDERING_RENDERER_H
#define POORCRAFT_RENDERING_RENDERER_H

#include <cstddef>
#include <cstdint>
#include <string>

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

    virtual ~Renderer() = default;

    virtual bool initialize() = 0;
    virtual void shutdown() = 0;

    virtual void beginFrame() = 0;
    virtual void clear(float r, float g, float b, float a) = 0;
    virtual void endFrame() = 0;

    virtual RendererCapabilities getCapabilities() const = 0;
    virtual void setVSync(bool enabled) = 0;

    virtual void setViewProjection(const glm::mat4& view, const glm::mat4& projection) = 0;

    virtual BufferHandle createVertexBuffer(const void* data, std::size_t size) = 0;
    virtual BufferHandle createIndexBuffer(const void* data, std::size_t size) = 0;
    virtual void destroyBuffer(BufferHandle handle) = 0;

    virtual void drawIndexed(BufferHandle vertexBuffer, BufferHandle indexBuffer, std::uint32_t indexCount, const glm::mat4& modelMatrix) = 0;

protected:
    Renderer() = default;
};
} // namespace poorcraft::rendering

#endif // POORCRAFT_RENDERING_RENDERER_H
