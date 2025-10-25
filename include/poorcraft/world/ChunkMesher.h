#pragma once

#include "poorcraft/world/Chunk.h"
#include "poorcraft/world/ChunkMesh.h"

namespace poorcraft::world {

class ChunkMesher {
public:
    enum class FaceDirection {
        PosX = 0,
        NegX,
        PosY,
        NegY,
        PosZ,
        NegZ,
        Count
    };

    static void generateMesh(const Chunk& chunk, ChunkMesh& outMesh, const Chunk* neighbors[6]);

private:
    static bool shouldCreateFace(
        const Chunk& chunk,
        int x,
        int y,
        int z,
        int dx,
        int dy,
        int dz,
        const Chunk* neighbors[6]
    );

    static void generateFaceQuads(
        const Chunk& chunk,
        FaceDirection direction,
        ChunkMesh& outMesh,
        const Chunk* neighbors[6]
    );
};

} // namespace poorcraft::world
