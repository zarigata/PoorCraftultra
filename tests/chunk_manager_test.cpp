#include "poorcraft/world/ChunkManager.h"

#include <gtest/gtest.h>

#include <glm/glm.hpp>

namespace
{
class StubRenderer : public poorcraft::rendering::Renderer
{
public:
    bool initialize() override { return true; }
    void shutdown() override {}

    void beginFrame() override {}
    void clear(float, float, float, float) override {}
    void endFrame() override {}

    poorcraft::rendering::RendererCapabilities getCapabilities() const override
    {
        return {};
    }

    void setVSync(bool) override {}

    void setViewProjection(const glm::mat4& view, const glm::mat4& projection) override
    {
        m_lastView = view;
        m_lastProjection = projection;
    }

    BufferHandle createVertexBuffer(const void*, std::size_t) override
    {
        return ++m_nextHandle;
    }

    BufferHandle createIndexBuffer(const void*, std::size_t) override
    {
        return ++m_nextHandle;
    }

    void destroyBuffer(BufferHandle) override {}

    void drawIndexed(BufferHandle, BufferHandle, std::uint32_t, const glm::mat4&) override
    {
        ++m_drawCalls;
    }

    glm::mat4 m_lastView{1.0f};
    glm::mat4 m_lastProjection{1.0f};
    std::uint32_t m_nextHandle{0};
    std::uint32_t m_drawCalls{0};
};
} // namespace

TEST(ChunkManagerTest, UpdateLoadsChunksWithinDistance)
{
    StubRenderer renderer;
    poorcraft::world::ChunkManager manager(renderer, 42u);
    manager.setRenderDistance(0);

    manager.update(glm::vec3(0.0f));

    EXPECT_EQ(manager.getLoadedChunkCount(), 1u);

    renderer.m_drawCalls = 0;
    renderer.setViewProjection(glm::mat4(1.0f), glm::mat4(1.0f));
    manager.render();
    EXPECT_EQ(renderer.m_drawCalls, manager.getLoadedChunkCount());
}

TEST(ChunkManagerTest, UpdateUnloadsChunksOutsideRadius)
{
    StubRenderer renderer;
    poorcraft::world::ChunkManager manager(renderer, 1337u);
    manager.setRenderDistance(0);

    manager.update(glm::vec3(0.0f));
    EXPECT_EQ(manager.getLoadedChunkCount(), 1u);

    const float shift = static_cast<float>(poorcraft::world::CHUNK_SIZE_X);
    manager.update(glm::vec3(shift, 0.0f, 0.0f));

    EXPECT_EQ(manager.getLoadedChunkCount(), 1u);
}
