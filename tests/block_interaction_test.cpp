#include "poorcraft/world/ChunkManager.h"
#include "poorcraft/world/Raycaster.h"
#include "poorcraft/world/Chunk.h"
#include "poorcraft/world/Block.h"
#include "poorcraft/rendering/Renderer.h"
#include "poorcraft/core/Inventory.h"
#include "poorcraft/core/Player.h"

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

class BlockInteractionTest : public ::testing::Test {
protected:
    void SetUp() override {
        renderer = std::make_unique<StubRenderer>();
        chunkManager = std::make_unique<ChunkManager>(*renderer, 1337u);
        chunkManager->setRenderDistance(1);
        chunkManager->update(glm::vec3(0.0f));
        player = std::make_unique<poorcraft::core::Player>(glm::vec3(0.0f, 70.0f, 0.0f));
    }

    std::unique_ptr<StubRenderer> renderer;
    std::unique_ptr<ChunkManager> chunkManager;
    std::unique_ptr<poorcraft::core::Player> player;
};

TEST_F(BlockInteractionTest, BreakBlockRemovesIt)
{
    const glm::ivec3 blockPos(0, 65, 0);
    chunkManager->setBlockAt(blockPos.x, blockPos.y, blockPos.z, BlockType::Stone);

    const auto hit = Raycaster::raycast(glm::vec3(blockPos) + glm::vec3(0.5f), glm::vec3(0.0f, -1.0f, 0.0f), 5.0f, *chunkManager);
    ASSERT_TRUE(hit.hit);
    EXPECT_TRUE(chunkManager->setBlockAt(hit.blockPosition.x, hit.blockPosition.y, hit.blockPosition.z, BlockType::Air));
    EXPECT_EQ(chunkManager->getBlockAt(blockPos.x, blockPos.y, blockPos.z), BlockType::Air);
}

TEST_F(BlockInteractionTest, PlaceBlockAddsIt)
{
    const glm::vec3 origin(0.5f, 70.5f, 0.5f);
    const glm::vec3 direction(0.0f, -1.0f, 0.0f);
    const auto hit = Raycaster::raycast(origin, direction, 10.0f, *chunkManager);
    ASSERT_TRUE(hit.hit);

    const glm::ivec3 placement = hit.previousBlockPosition;
    EXPECT_TRUE(chunkManager->setBlockAt(placement.x, placement.y, placement.z, BlockType::Stone));
    EXPECT_EQ(chunkManager->getBlockAt(placement.x, placement.y, placement.z), BlockType::Stone);
}

TEST_F(BlockInteractionTest, PlacementAvoidsPlayerCollision)
{
    poorcraft::core::Inventory inventory;
    inventory.setSelectedSlot(2); // Stone

    const auto playerAABB = player->getAABB();
    const glm::ivec3 blockPos = glm::floor(playerAABB.min / BLOCK_SIZE);

    bool intersectsPlayer = true;
    const glm::vec3 blockMin = glm::vec3(blockPos) * BLOCK_SIZE;
    const glm::vec3 blockMax = blockMin + glm::vec3(BLOCK_SIZE);

    intersectsPlayer = (blockMax.x > playerAABB.min.x && blockMin.x < playerAABB.max.x) &&
                       (blockMax.y > playerAABB.min.y && blockMin.y < playerAABB.max.y) &&
                       (blockMax.z > playerAABB.min.z && blockMin.z < playerAABB.max.z);

    EXPECT_TRUE(intersectsPlayer);
}

TEST_F(BlockInteractionTest, NeighborChunksMarkedDirty)
{
    const glm::ivec3 boundaryBlock(CHUNK_SIZE_X - 1, 64, 0);
    const glm::ivec3 neighborBlock(CHUNK_SIZE_X, 64, 0);

    EXPECT_TRUE(chunkManager->setBlockAt(boundaryBlock.x, boundaryBlock.y, boundaryBlock.z, BlockType::Dirt));
    EXPECT_TRUE(chunkManager->setBlockAt(neighborBlock.x, neighborBlock.y, neighborBlock.z, BlockType::Dirt));

    EXPECT_TRUE(chunkManager->setBlockAt(boundaryBlock.x, boundaryBlock.y, boundaryBlock.z, BlockType::Air));
    EXPECT_TRUE(chunkManager->setBlockAt(neighborBlock.x, neighborBlock.y, neighborBlock.z, BlockType::Air));
}

TEST_F(BlockInteractionTest, BreakPlace_100Blocks)
{
    chunkManager->setRenderDistance(1);
    chunkManager->update(glm::vec3(0.0f));

    const glm::ivec3 basePosition(0, 65, 0);

    for (int i = 0; i < 100; ++i) {
        const glm::ivec3 blockPosition{
            basePosition.x + (i % CHUNK_SIZE_X),
            basePosition.y,
            basePosition.z + (i / CHUNK_SIZE_X)
        };

        EXPECT_TRUE(chunkManager->setBlockAt(blockPosition.x, blockPosition.y, blockPosition.z, BlockType::Stone))
            << "Placement failed at iteration " << i;
        EXPECT_EQ(chunkManager->getBlockAt(blockPosition.x, blockPosition.y, blockPosition.z), BlockType::Stone);

        chunkManager->update(glm::vec3(0.0f));

        EXPECT_TRUE(chunkManager->setBlockAt(blockPosition.x, blockPosition.y, blockPosition.z, BlockType::Air))
            << "Break failed at iteration " << i;
        EXPECT_EQ(chunkManager->getBlockAt(blockPosition.x, blockPosition.y, blockPosition.z), BlockType::Air);

        chunkManager->update(glm::vec3(0.0f));
    }
}

} // namespace poorcraft::world
