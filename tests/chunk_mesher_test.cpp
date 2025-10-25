#include "poorcraft/world/ChunkMesher.h"
#include "poorcraft/world/ChunkMesh.h"

#include <gtest/gtest.h>

using poorcraft::world::BlockType;
using poorcraft::world::CHUNK_SIZE_X;
using poorcraft::world::Chunk;
using poorcraft::world::ChunkMesh;
using poorcraft::world::ChunkMesher;
using poorcraft::world::ChunkPosition;

namespace
{
constexpr ChunkPosition kOriginPos{0, 0};
}

TEST(ChunkMesherTest, GeneratesQuadForSingleBlock)
{
    Chunk chunk(kOriginPos);
    chunk.setBlock(1, 1, 1, BlockType::Stone);

    ChunkMesh mesh;
    const Chunk* neighbors[6] = {};

    ChunkMesher::generateMesh(chunk, mesh, neighbors);
    EXPECT_FALSE(mesh.isEmpty());
    EXPECT_EQ(mesh.getIndexCount(), 36u) << "Single cube should generate 6 quads";
}

TEST(ChunkMesherTest, HandlesNeighborCulling)
{
    Chunk chunk(kOriginPos);
    chunk.setBlock(0, 0, 0, BlockType::Stone);

    Chunk neighbor({-1, 0});
    neighbor.setBlock(CHUNK_SIZE_X - 1, 0, 0, BlockType::Stone);

    ChunkMesh mesh;
    const Chunk* neighbors[6] = {};
    neighbors[static_cast<int>(ChunkMesher::FaceDirection::NegX)] = &neighbor;

    ChunkMesher::generateMesh(chunk, mesh, neighbors);
    EXPECT_EQ(mesh.getIndexCount(), 30u) << "Face adjacent to neighbor should be culled";
}
