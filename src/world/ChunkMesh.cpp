#include "poorcraft/world/ChunkMesh.h"

namespace poorcraft::world {

void ChunkMesh::clear() {
    m_vertices.clear();
    m_indices.clear();
    m_uploaded = false;
}

void ChunkMesh::addQuad(const glm::vec3 corners[4], const glm::vec3& normal, const glm::vec2 uvs[4]) {
    const std::uint32_t baseIndex = static_cast<std::uint32_t>(m_vertices.size());
    for (int i = 0; i < 4; ++i) {
        ChunkVertex vertex{};
        vertex.position = corners[i];
        vertex.normal = normal;
        vertex.texCoord = uvs[i];
        m_vertices.push_back(vertex);
    }

    m_indices.push_back(baseIndex + 0);
    m_indices.push_back(baseIndex + 1);
    m_indices.push_back(baseIndex + 2);
    m_indices.push_back(baseIndex + 2);
    m_indices.push_back(baseIndex + 3);
    m_indices.push_back(baseIndex + 0);

    m_uploaded = false;
}

} // namespace poorcraft::world
