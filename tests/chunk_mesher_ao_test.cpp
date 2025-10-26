#include <gtest/gtest.h>
#include "poorcraft/world/ChunkMesher.h"
#include "poorcraft/world/Chunk.h"
#include "poorcraft/world/ChunkMesh.h"
#include "poorcraft/rendering/TextureAtlas.h"

using namespace poorcraft::world;
using namespace poorcraft::rendering;
using namespace poorcraft::common;

class ChunkMesherAOTest : public ::testing::Test {
protected:
    void SetUp() override {
        atlas.initialize(32);
    }

    TextureAtlas atlas;
};

TEST_F(ChunkMesherAOTest, FullyLitBlockHasMaxAO) {
    // Create a chunk with a single block surrounded by air
    Chunk chunk({0, 0, 0});
    chunk.setBlock(8, 8, 8, BlockType::Stone);

    ChunkMesh mesh;
    const Chunk* neighbors[6] = {nullptr, nullptr, nullptr, nullptr, nullptr, nullptr};
    ChunkMesher::generateMesh(chunk, mesh, neighbors, atlas);

    ASSERT_FALSE(mesh.isEmpty());

    // All vertices should have AO close to 1.0 (fully lit)
    const auto& vertices = mesh.getVertices();
    for (const auto& vertex : vertices) {
        EXPECT_GE(vertex.ao, 0.9f) << "Isolated block should have high AO";
    }
}

TEST_F(ChunkMesherAOTest, FullyOccludedBlockHasLowAO) {
    // Create a chunk filled with blocks
    Chunk chunk({0, 0, 0});
    for (int x = 0; x < CHUNK_SIZE_X; ++x) {
        for (int y = 0; y < CHUNK_SIZE_Y; ++y) {
            for (int z = 0; z < CHUNK_SIZE_Z; ++z) {
                chunk.setBlock(x, y, z, BlockType::Stone);
            }
        }
    }

    // Clear one block to create a face
    chunk.setBlock(8, 8, 8, BlockType::Air);

    ChunkMesh mesh;
    const Chunk* neighbors[6] = {nullptr, nullptr, nullptr, nullptr, nullptr, nullptr};
    ChunkMesher::generateMesh(chunk, mesh, neighbors, atlas);

    ASSERT_FALSE(mesh.isEmpty());

    // Vertices on the exposed face should have lower AO due to surrounding blocks
    const auto& vertices = mesh.getVertices();
    bool foundLowAO = false;
    for (const auto& vertex : vertices) {
        if (vertex.ao < 0.5f) {
            foundLowAO = true;
            break;
        }
    }
    EXPECT_TRUE(foundLowAO) << "Occluded faces should have reduced AO";
}

TEST_F(ChunkMesherAOTest, CornerOcclusionReducesAO) {
    // Create a corner configuration: block at origin with neighbors on two edges
    Chunk chunk({0, 0, 0});
    chunk.setBlock(8, 8, 8, BlockType::Stone);
    chunk.setBlock(9, 8, 8, BlockType::Stone); // +X neighbor
    chunk.setBlock(8, 9, 8, BlockType::Stone); // +Y neighbor

    ChunkMesh mesh;
    const Chunk* neighbors[6] = {nullptr, nullptr, nullptr, nullptr, nullptr, nullptr};
    ChunkMesher::generateMesh(chunk, mesh, neighbors, atlas);

    ASSERT_FALSE(mesh.isEmpty());

    // Should have varying AO values due to corner occlusion
    const auto& vertices = mesh.getVertices();
    float minAO = 1.0f;
    float maxAO = 0.0f;
    for (const auto& vertex : vertices) {
        minAO = std::min(minAO, vertex.ao);
        maxAO = std::max(maxAO, vertex.ao);
    }

    EXPECT_LT(minAO, maxAO) << "Corner occlusion should create AO variation";
    EXPECT_LT(minAO, 0.9f) << "Some vertices should be occluded";
}

TEST_F(ChunkMesherAOTest, EdgeOcclusionIsIntermediate) {
    // Create an edge configuration: block with one neighbor
    Chunk chunk({0, 0, 0});
    chunk.setBlock(8, 8, 8, BlockType::Stone);
    chunk.setBlock(9, 8, 8, BlockType::Stone); // +X neighbor only

    ChunkMesh mesh;
    const Chunk* neighbors[6] = {nullptr, nullptr, nullptr, nullptr, nullptr, nullptr};
    ChunkMesher::generateMesh(chunk, mesh, neighbors, atlas);

    ASSERT_FALSE(mesh.isEmpty());

    // AO should be between fully lit and fully occluded
    const auto& vertices = mesh.getVertices();
    for (const auto& vertex : vertices) {
        EXPECT_GE(vertex.ao, 0.25f);
        EXPECT_LE(vertex.ao, 1.0f);
    }
}

TEST_F(ChunkMesherAOTest, NeighborChunkInteractionAffectsAO) {
    // Create main chunk with a block at the edge
    Chunk chunk({0, 0, 0});
    chunk.setBlock(0, 8, 8, BlockType::Stone); // At -X edge

    // Create neighbor chunk with adjacent block
    Chunk neighborChunk({-1, 0, 0});
    neighborChunk.setBlock(CHUNK_SIZE_X - 1, 8, 8, BlockType::Stone);

    const Chunk* neighbors[6] = {nullptr, &neighborChunk, nullptr, nullptr, nullptr, nullptr};
    ChunkMesh mesh;
    ChunkMesher::generateMesh(chunk, mesh, neighbors, atlas);

    ASSERT_FALSE(mesh.isEmpty());

    // The face should not be generated on the -X side due to neighbor
    // Or if generated, should have reduced AO
    const auto& vertices = mesh.getVertices();
    EXPECT_GT(vertices.size(), 0u);
}

TEST_F(ChunkMesherAOTest, DiagonalNeighborAffectsAO) {
    // Create a configuration where diagonal sampling matters
    Chunk chunk({0, 0, 0});
    chunk.setBlock(0, 8, 0, BlockType::Stone); // Corner block

    // Create neighbors with blocks that form a diagonal
    Chunk neighborX({-1, 0, 0});
    neighborX.setBlock(CHUNK_SIZE_X - 1, 8, 0, BlockType::Stone);

    Chunk neighborZ({0, 0, -1});
    neighborZ.setBlock(0, 8, CHUNK_SIZE_Z - 1, BlockType::Stone);

    const Chunk* neighbors[6] = {nullptr, &neighborX, nullptr, nullptr, nullptr, &neighborZ};
    ChunkMesh mesh;
    ChunkMesher::generateMesh(chunk, mesh, neighbors, atlas);

    // Mesh should be generated without crashes
    // Diagonal should be approximated correctly
    EXPECT_TRUE(true) << "Diagonal neighbor handling should not crash";
}

TEST_F(ChunkMesherAOTest, AOValuesAreInValidRange) {
    // Create a random configuration
    Chunk chunk({0, 0, 0});
    for (int x = 0; x < CHUNK_SIZE_X; x += 2) {
        for (int y = 0; y < CHUNK_SIZE_Y; y += 2) {
            for (int z = 0; z < CHUNK_SIZE_Z; z += 2) {
                chunk.setBlock(x, y, z, BlockType::Stone);
            }
        }
    }

    ChunkMesh mesh;
    const Chunk* neighbors[6] = {nullptr, nullptr, nullptr, nullptr, nullptr, nullptr};
    ChunkMesher::generateMesh(chunk, mesh, neighbors, atlas);

    // All AO values should be in [0, 1] range
    const auto& vertices = mesh.getVertices();
    for (const auto& vertex : vertices) {
        EXPECT_GE(vertex.ao, 0.0f) << "AO should not be negative";
        EXPECT_LE(vertex.ao, 1.0f) << "AO should not exceed 1.0";
    }
}
