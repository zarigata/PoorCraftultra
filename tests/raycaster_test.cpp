#include "poorcraft/world/Raycaster.h"

#include "poorcraft/rendering/Renderer.h"
#include "poorcraft/world/Block.h"
#include "poorcraft/world/Chunk.h"
#include "poorcraft/world/ChunkManager.h"

#include <gtest/gtest.h>

#include <glm/glm.hpp>
#include <memory>

namespace poorcraft::world {

namespace {
class StubRenderer : public rendering::Renderer {
public:
    bool initialize() override { return true; }
    void shutdown() override {}
    void beginFrame() override {}
    void clear(float, float, float, float) override {}
    void endFrame() override {}
    rendering::RendererCapabilities getCapabilities() const override { return {}; }
    void setVSync(bool) override {}
    void setViewProjection(const glm::mat4&, const glm::mat4&) override {}
    rendering::Renderer::BufferHandle createVertexBuffer(const void*, std::size_t) override { return 0; }
    rendering::Renderer::BufferHandle createIndexBuffer(const void*, std::size_t) override { return 0; }
    void destroyBuffer(rendering::Renderer::BufferHandle) override {}
    void drawIndexed(rendering::Renderer::BufferHandle,
                     rendering::Renderer::BufferHandle,
                     std::uint32_t,
                     const glm::mat4&) override {}
};
} // namespace

class RaycasterTest : public ::testing::Test {
protected:
    void SetUp() override {
        renderer = std::make_unique<StubRenderer>();
        chunkManager = std::make_unique<ChunkManager>(*renderer, 42u);
        chunkManager->setRenderDistance(0);
        chunkManager->update(glm::vec3(0.0f));
    }

    std::unique_ptr<StubRenderer> renderer;
    std::unique_ptr<ChunkManager> chunkManager;
};

TEST_F(RaycasterTest, RaycastMiss) {
    const glm::vec3 origin(0.5f, 50.0f, 0.5f);
    const glm::vec3 direction(0.0f, 1.0f, 0.0f);
    const auto hit = Raycaster::raycast(origin, direction, 5.0f, *chunkManager);
    EXPECT_FALSE(hit.hit);
}

TEST_F(RaycasterTest, RaycastHitsSolidBlock) {
    const glm::ivec3 blockPos(0, 60, 0);
    chunkManager->setBlockAt(blockPos.x, blockPos.y, blockPos.z, BlockType::Stone);

    const glm::vec3 origin(0.5f, 55.0f, 0.5f);
    const glm::vec3 direction(0.0f, 1.0f, 0.0f);
    const auto hit = Raycaster::raycast(origin, direction, 10.0f, *chunkManager);

    ASSERT_TRUE(hit.hit);
    EXPECT_EQ(hit.blockPosition, blockPos);
    EXPECT_EQ(hit.blockType, BlockType::Stone);
}

TEST_F(RaycasterTest, RaycastHitDistance) {
    const glm::ivec3 blockPos(0, 61, 0);
    chunkManager->setBlockAt(blockPos.x, blockPos.y, blockPos.z, BlockType::Stone);

    const glm::vec3 origin(0.5f, 55.0f, 0.5f);
    const glm::vec3 direction(0.0f, 1.0f, 0.0f);

    for(float reach : {2.0f, 5.0f, 10.0f})
    {
        const auto hit = Raycaster::raycast(origin, direction, reach, *chunkManager);
        if(reach >= 6.0f)
        {
            ASSERT_TRUE(hit.hit);
            EXPECT_EQ(hit.blockPosition, blockPos);
        }
        else
        {
            EXPECT_FALSE(hit.hit);
        }
    }
}

TEST_F(RaycasterTest, RaycastPreviousBlockPosition) {
    const glm::ivec3 blockPos(0, 60, 0);
    chunkManager->setBlockAt(blockPos.x, blockPos.y, blockPos.z, BlockType::Dirt);

    const glm::vec3 origin(0.5f, 55.0f, 0.5f);
    const glm::vec3 direction(0.0f, 1.0f, 0.0f);
    const auto hit = Raycaster::raycast(origin, direction, 10.0f, *chunkManager);

    ASSERT_TRUE(hit.hit);
    EXPECT_EQ(hit.blockPosition, blockPos);
    EXPECT_EQ(hit.previousBlockPosition, blockPos - glm::ivec3(0, 1, 0));
}

TEST_F(RaycasterTest, RaycastNormalDirections) {
    const glm::ivec3 blockPosX(1, 60, 0);
    const glm::ivec3 blockPosY(0, 61, 0);
    const glm::ivec3 blockPosZ(0, 60, 1);
    chunkManager->setBlockAt(blockPosX.x, blockPosX.y, blockPosX.z, BlockType::Grass);
    chunkManager->setBlockAt(blockPosY.x, blockPosY.y, blockPosY.z, BlockType::Grass);
    chunkManager->setBlockAt(blockPosZ.x, blockPosZ.y, blockPosZ.z, BlockType::Grass);

    const auto hitX = Raycaster::raycast(glm::vec3(-0.5f, 60.5f, 0.5f), glm::vec3(1.0f, 0.0f, 0.0f), 10.0f, *chunkManager);
    ASSERT_TRUE(hitX.hit);
    EXPECT_EQ(hitX.blockPosition, blockPosX);
    EXPECT_EQ(hitX.normal, glm::vec3(-1.0f, 0.0f, 0.0f));

    const auto hitY = Raycaster::raycast(glm::vec3(0.5f, 60.0f, 0.5f), glm::vec3(0.0f, 1.0f, 0.0f), 10.0f, *chunkManager);
    ASSERT_TRUE(hitY.hit);
    EXPECT_EQ(hitY.blockPosition, blockPosY);
    EXPECT_EQ(hitY.normal, glm::vec3(0.0f, -1.0f, 0.0f));

    const auto hitZ = Raycaster::raycast(glm::vec3(0.5f, 60.5f, -0.5f), glm::vec3(0.0f, 0.0f, 1.0f), 10.0f, *chunkManager);
    ASSERT_TRUE(hitZ.hit);
    EXPECT_EQ(hitZ.blockPosition, blockPosZ);
    EXPECT_EQ(hitZ.normal, glm::vec3(0.0f, 0.0f, -1.0f));
}

TEST_F(RaycasterTest, RaycastMaxDistance) {
    const glm::ivec3 blockPos(0, 60, 5);
    chunkManager->setBlockAt(blockPos.x, blockPos.y, blockPos.z, BlockType::Stone);

    const glm::vec3 origin(0.5f, 60.5f, 0.5f);
    const glm::vec3 direction(0.0f, 0.0f, 1.0f);
    const auto hitWithin = Raycaster::raycast(origin, direction, 6.0f, *chunkManager);
    ASSERT_TRUE(hitWithin.hit);
    EXPECT_EQ(hitWithin.blockPosition, blockPos);

    const auto hitBeyond = Raycaster::raycast(origin, direction, 4.0f, *chunkManager);
    EXPECT_FALSE(hitBeyond.hit);
}

TEST_F(RaycasterTest, RaycastThroughAirGaps) {
    const glm::ivec3 firstBlock(0, 60, 2);
    const glm::ivec3 secondBlock(0, 60, 5);
    chunkManager->setBlockAt(firstBlock.x, firstBlock.y, firstBlock.z, BlockType::Grass);
    chunkManager->setBlockAt(secondBlock.x, secondBlock.y, secondBlock.z, BlockType::Stone);
    chunkManager->setBlockAt(firstBlock.x, firstBlock.y, firstBlock.z, BlockType::Air);

    const glm::vec3 origin(0.5f, 60.5f, 0.5f);
    const glm::vec3 direction(0.0f, 0.0f, 1.0f);
    const auto hit = Raycaster::raycast(origin, direction, 10.0f, *chunkManager);

    ASSERT_TRUE(hit.hit);
    EXPECT_EQ(hit.blockPosition, secondBlock);
    EXPECT_EQ(hit.blockType, BlockType::Stone);
}

} // namespace poorcraft::world
