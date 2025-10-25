#include <SDL2/SDL.h>

#include <gtest/gtest.h>

#include "poorcraft/core/Input.h"
#include "poorcraft/core/Player.h"
#include "poorcraft/rendering/Renderer.h"
#include "poorcraft/world/Block.h"
#include "poorcraft/world/ChunkManager.h"

#include <cmath>
#include <glm/glm.hpp>
#include <unordered_set>

namespace {

class DummyRenderer final : public poorcraft::rendering::Renderer {
public:
    bool initialize() override { return true; }
    void shutdown() override {}

    void beginFrame() override {}
    void clear(float, float, float, float) override {}
    void endFrame() override {}

    poorcraft::rendering::RendererCapabilities getCapabilities() const override {
        return {};
    }

    void setVSync(bool) override {}
    void setViewProjection(const glm::mat4&, const glm::mat4&) override {}

    BufferHandle createVertexBuffer(const void*, std::size_t) override { return 0; }
    BufferHandle createIndexBuffer(const void*, std::size_t) override { return 0; }
    void destroyBuffer(BufferHandle) override {}

    void drawIndexed(BufferHandle, BufferHandle, std::uint32_t, const glm::mat4&) override {}
};

DummyRenderer g_dummyRenderer;

struct IVec3Hash {
    std::size_t operator()(const glm::ivec3& v) const noexcept {
        std::size_t seed = static_cast<std::size_t>(v.x);
        seed ^= static_cast<std::size_t>(v.y) + 0x9e3779b9 + (seed << 6) + (seed >> 2);
        seed ^= static_cast<std::size_t>(v.z) + 0x9e3779b9 + (seed << 6) + (seed >> 2);
        return seed;
    }
};

class StubChunkManager : public poorcraft::world::ChunkManager {
public:
    StubChunkManager()
        : poorcraft::world::ChunkManager(g_dummyRenderer, 0u) {}

    void setSolidBlock(const glm::ivec3& block, bool solid = true) {
        if (solid) {
            m_solidBlocks.insert(block);
        } else {
            m_solidBlocks.erase(block);
        }
    }

    void populateFloor(int radius) {
        for (int x = -radius; x <= radius; ++x) {
            for (int z = -radius; z <= radius; ++z) {
                setSolidBlock({x, 0, z});
            }
        }
    }

    poorcraft::world::BlockType getBlockAt(const glm::vec3& worldPosition) const override {
        const glm::vec3 scaled = worldPosition / poorcraft::world::BLOCK_SIZE;
        const glm::vec3 floored = glm::floor(scaled);
        const glm::ivec3 block{
            static_cast<int>(floored.x),
            static_cast<int>(floored.y),
            static_cast<int>(floored.z)
        };
        if (block.y < 0) {
            return poorcraft::world::BlockType::Stone;
        }
        return m_solidBlocks.find(block) != m_solidBlocks.end() ? poorcraft::world::BlockType::Stone : poorcraft::world::BlockType::Air;
    }

    bool isBlockSolid(const glm::vec3& worldPosition) const override {
        const glm::vec3 scaled = worldPosition / poorcraft::world::BLOCK_SIZE;
        const glm::vec3 floored = glm::floor(scaled);
        const glm::ivec3 block{
            static_cast<int>(floored.x),
            static_cast<int>(floored.y),
            static_cast<int>(floored.z)
        };
        if (block.y < 0) {
            return true;
        }
        return m_solidBlocks.find(block) != m_solidBlocks.end();
    }

private:
    std::unordered_set<glm::ivec3, IVec3Hash> m_solidBlocks;
};

void sendKeyEvent(poorcraft::core::Input& input, poorcraft::core::Input::KeyCode key, bool pressed) {
    SDL_Event event{};
    event.type = pressed ? SDL_KEYDOWN : SDL_KEYUP;
    event.key.repeat = 0;
    event.key.state = pressed ? SDL_PRESSED : SDL_RELEASED;
    event.key.keysym.scancode = static_cast<SDL_Scancode>(key);
    input.processEvent(event);
}

} // namespace

TEST(PlayerTest, InitialState) {
    poorcraft::core::Player player(glm::vec3(0.0f));
    EXPECT_EQ(player.getPosition(), glm::vec3(0.0f));
    EXPECT_EQ(player.getVelocity(), glm::vec3(0.0f));
    EXPECT_EQ(player.getMovementMode(), poorcraft::core::Player::MovementMode::Walk);
    EXPECT_FALSE(player.isOnGround());
}

TEST(PlayerTest, Gravity) {
    poorcraft::core::Player player(glm::vec3(0.0f, 10.0f, 0.0f));
    poorcraft::core::Input input;
    StubChunkManager chunkManager;

    const float deltaTime = 0.016f;
    for (int i = 0; i < 10; ++i) {
        input.reset();
        player.update(input, deltaTime, chunkManager);
    }

    EXPECT_LT(player.getVelocity().y, 0.0f);
    EXPECT_LT(player.getPosition().y, 10.0f);
}

TEST(PlayerTest, GroundCollision) {
    poorcraft::core::Player player(glm::vec3(0.0f, 2.0f, 0.0f));
    poorcraft::core::Input input;
    StubChunkManager chunkManager;
    chunkManager.setSolidBlock({0, 0, 0});

    const float deltaTime = 0.016f;
    for (int i = 0; i < 240; ++i) {
        input.reset();
        player.update(input, deltaTime, chunkManager);
    }

    EXPECT_TRUE(player.isOnGround());
    EXPECT_NEAR(player.getVelocity().y, 0.0f, 1e-3f);
    EXPECT_NEAR(player.getAABB().min.y, 0.0f, 5e-2f);
}

TEST(PlayerTest, Jump) {
    poorcraft::core::Player player(glm::vec3(0.0f, 1.0f, 0.0f));
    poorcraft::core::Input input;
    StubChunkManager chunkManager;
    chunkManager.setSolidBlock({0, 0, 0});

    const float deltaTime = 0.016f;
    for (int i = 0; i < 120; ++i) {
        input.reset();
        player.update(input, deltaTime, chunkManager);
    }
    ASSERT_TRUE(player.isOnGround());

    input.reset();
    sendKeyEvent(input, SDL_SCANCODE_SPACE, true);
    player.update(input, deltaTime, chunkManager);
    EXPECT_GT(player.getVelocity().y, 0.0f);
    EXPECT_FALSE(player.isOnGround());

    input.reset();
    sendKeyEvent(input, SDL_SCANCODE_SPACE, false);
    player.update(input, deltaTime, chunkManager);
}

TEST(PlayerTest, WallCollision) {
    poorcraft::core::Player player(glm::vec3(0.0f, 1.0f, 0.0f));
    poorcraft::core::Input input;
    StubChunkManager chunkManager;
    chunkManager.setSolidBlock({0, 0, 0});
    for (int y = 0; y < 4; ++y) {
        chunkManager.setSolidBlock({1, y, 0});
    }

    const float deltaTime = 0.016f;
    input.reset();
    sendKeyEvent(input, poorcraft::core::Input::KeyCode::D, true);

    for (int i = 0; i < 180; ++i) {
        player.update(input, deltaTime, chunkManager);
        input.reset();
    }

    const float playerRight = player.getPosition().x + player.getAABB().max.x;
    EXPECT_LE(playerRight, 1.0f + 1e-3f);

    input.reset();
    sendKeyEvent(input, poorcraft::core::Input::KeyCode::D, false);
    player.update(input, deltaTime, chunkManager);
}

TEST(PlayerTest, MovementModes) {
    StubChunkManager chunkManager;
    chunkManager.populateFloor(5);
    const float deltaTime = 0.016f;

    auto runForwardDistance = [&](bool sprint) {
        poorcraft::core::Player player(glm::vec3(0.0f, 1.0f, 0.0f));
        poorcraft::core::Input input;

        sendKeyEvent(input, poorcraft::core::Input::KeyCode::W, true);
        if (sprint) {
            sendKeyEvent(input, poorcraft::core::Input::KeyCode::LeftShift, true);
        }

        float startZ = player.getPosition().z;
        for (int i = 0; i < 120; ++i) {
            player.update(input, deltaTime, chunkManager);
            input.reset();
        }

        const float distance = startZ - player.getPosition().z;

        sendKeyEvent(input, poorcraft::core::Input::KeyCode::W, false);
        if (sprint) {
            sendKeyEvent(input, poorcraft::core::Input::KeyCode::LeftShift, false);
        }
        player.update(input, deltaTime, chunkManager);

        return distance;
    };

    const float walkDistance = runForwardDistance(false);
    const float sprintDistance = runForwardDistance(true);

    EXPECT_GT(sprintDistance, walkDistance * 1.1f);

    poorcraft::core::Player flyPlayer(glm::vec3(0.0f, 10.0f, 0.0f));
    poorcraft::core::Input flyInput;
    flyPlayer.setMovementMode(poorcraft::core::Player::MovementMode::Fly);

    for (int i = 0; i < 60; ++i) {
        flyPlayer.update(flyInput, deltaTime, chunkManager);
        flyInput.reset();
    }

    EXPECT_NEAR(flyPlayer.getPosition().y, 10.0f, 1e-3f);
}

TEST(PlayerTest, AABBWorldSpace) {
    poorcraft::core::Player player(glm::vec3(2.0f, 3.0f, 4.0f));
    const auto aabb = player.getAABB();
    EXPECT_NEAR(aabb.min.x, 1.7f, 1e-4f);
    EXPECT_NEAR(aabb.min.y, 3.0f, 1e-4f);
    EXPECT_NEAR(aabb.min.z, 3.7f, 1e-4f);
    EXPECT_NEAR(aabb.max.x, 2.3f, 1e-4f);
    EXPECT_NEAR(aabb.max.y, 4.8f, 1e-4f);
    EXPECT_NEAR(aabb.max.z, 4.3f, 1e-4f);
}

TEST(PlayerTest, EyePosition) {
    poorcraft::core::Player player(glm::vec3(1.0f, 2.0f, 3.0f));
    const glm::vec3 eye = player.getEyePosition();
    EXPECT_NEAR(eye.x, 1.0f, 1e-4f);
    EXPECT_NEAR(eye.y, 2.0f + 1.62f, 1e-4f);
    EXPECT_NEAR(eye.z, 3.0f, 1e-4f);
}
