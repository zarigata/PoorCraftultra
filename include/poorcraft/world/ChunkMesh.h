#pragma once

#include <cstdint>
#include <vector>

#include <glm/glm.hpp>

namespace poorcraft::world {

struct ChunkVertex {
    glm::vec3 position{};
    glm::vec3 normal{};
    glm::vec2 texCoord{};
    float ao{1.0f};
};

class ChunkMesh {
public:
    ChunkMesh() = default;

    void clear();
    // aoValues contains per-vertex ambient occlusion factors in [0, 1] calculated by the ChunkMesher.
    void addQuad(
        const glm::vec3 corners[4],
        const glm::vec3& normal,
        const glm::vec2 uvs[4],
        const float aoValues[4]
    );

    [[nodiscard]] const std::vector<ChunkVertex>& getVertices() const noexcept { return m_vertices; }
    [[nodiscard]] const std::vector<std::uint32_t>& getIndices() const noexcept { return m_indices; }
    [[nodiscard]] std::size_t getVertexCount() const noexcept { return m_vertices.size(); }
    [[nodiscard]] std::size_t getIndexCount() const noexcept { return m_indices.size(); }

    [[nodiscard]] bool isEmpty() const noexcept { return m_vertices.empty(); }

    [[nodiscard]] bool isUploaded() const noexcept { return m_uploaded; }
    void setUploaded(bool uploaded) noexcept { m_uploaded = uploaded; }

private:
    std::vector<ChunkVertex> m_vertices;
    std::vector<std::uint32_t> m_indices;
    bool m_uploaded{false};
};

} // namespace poorcraft::world
