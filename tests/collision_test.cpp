#include <SDL2/SDL.h>

#include <gtest/gtest.h>

#include "poorcraft/core/Input.h"
#include "poorcraft/core/Player.h"
#include "poorcraft/core/Window.h"
#include "poorcraft/rendering/Renderer.h"
#include "poorcraft/rendering/RendererFactory.h"
#include "poorcraft/world/Block.h"
#include "poorcraft/world/ChunkManager.h"

#include <glm/glm.hpp>

#include <memory>

namespace {

constexpr float kDeltaTime = 0.016f;

class CollisionTest : public ::testing::Test {
protected:
    void SetUp() override {
        ASSERT_TRUE(poorcraft::core::Window::initSDL());

        auto selection = poorcraft::rendering::createRenderer(
            "Collision Test Harness",
            640,
            480,
            false,
            false,
            poorcraft::rendering::RendererBackend::Vulkan);

        if (!selection.window || !selection.renderer) {
            selection = poorcraft::rendering::createRenderer(
                "Collision Test Harness (Fallback)",
                640,
                480,
                false,
                false,
                poorcraft::rendering::RendererBackend::OpenGL);
        }

        if (!selection.window || !selection.renderer) {
            GTEST_SKIP() << "Unable to initialize renderer for collision tests.";
        }

        m_window = std::move(selection.window);
        m_renderer = std::move(selection.renderer);
        m_chunkManager = std::make_unique<poorcraft::world::ChunkManager>(*m_renderer, 42u);
    }

    void TearDown() override {
        if (m_renderer) {
            m_renderer->shutdown();
            m_renderer.reset();
        }
        m_window.reset();
        SDL_Quit();
    }

    [[nodiscard]] poorcraft::world::ChunkManager& chunkManager() {
        return *m_chunkManager;
    }

    void preloadChunks(const glm::vec3& position) {
        chunkManager().update(position);
        chunkManager().update(position);
    }

private:
    std::unique_ptr<poorcraft::core::Window> m_window;
    std::unique_ptr<poorcraft::rendering::Renderer> m_renderer;
    std::unique_ptr<poorcraft::world::ChunkManager> m_chunkManager;
};

glm::vec3 findGroundedBlockCenter(poorcraft::world::ChunkManager& chunkManager) {
    for (int y = 40; y >= 0; --y) {
        for (int z = -4; z <= 4; ++z) {
            for (int x = -4; x <= 4; ++x) {
                const glm::vec3 center(static_cast<float>(x) + 0.5f, static_cast<float>(y) + 0.5f, static_cast<float>(z) + 0.5f);
                if (chunkManager.isBlockSolid(center) && !chunkManager.isBlockSolid(center + glm::vec3(0.0f, 1.0f, 0.0f))) {
                    return center;
                }
            }
        }
    }
    return glm::vec3(0.5f, 0.5f, 0.5f);
}

void sendKeyEvent(poorcraft::core::Input& input, poorcraft::core::Input::KeyCode key, bool pressed) {
    SDL_Event event{};
    event.type = pressed ? SDL_KEYDOWN : SDL_KEYUP;
    event.key.repeat = 0;
    event.key.state = pressed ? SDL_PRESSED : SDL_RELEASED;
    event.key.keysym.scancode = static_cast<SDL_Scancode>(key);
    input.processEvent(event);
}

} // namespace

TEST_F(CollisionTest, PlayerFallsOntoGround) {
    poorcraft::core::Player player(glm::vec3(0.0f, 120.0f, 0.0f));
    poorcraft::core::Input input;

    preloadChunks(player.getEyePosition());

    for (int i = 0; i < 600; ++i) {
        player.update(input, kDeltaTime, chunkManager());
        chunkManager().update(player.getEyePosition());
        if (player.isOnGround()) {
            break;
        }
    }

    EXPECT_TRUE(player.isOnGround());
    EXPECT_NEAR(player.getVelocity().y, 0.0f, 1e-2f);
}

TEST_F(CollisionTest, PlayerDoesNotSinkBelowTerrain) {
    const glm::vec3 groundCenter = findGroundedBlockCenter(chunkManager());
    poorcraft::core::Player player(glm::vec3(groundCenter.x, groundCenter.y + 1.0f, groundCenter.z));
    poorcraft::core::Input input;

    preloadChunks(player.getEyePosition());

    for (int frame = 0; frame < 240; ++frame) {
        player.update(input, kDeltaTime, chunkManager());
        chunkManager().update(player.getEyePosition());
        ASSERT_LE(player.getAABB().min.y, groundCenter.y + 1.0f + 1e-2f);
        ASSERT_GE(player.getAABB().min.y, groundCenter.y - 0.1f);
    }

    EXPECT_TRUE(player.isOnGround());
}

TEST_F(CollisionTest, PlayerCannotWalkThroughSolidBlock) {
    const glm::vec3 groundCenter = findGroundedBlockCenter(chunkManager());
    poorcraft::core::Player player(glm::vec3(groundCenter.x - 0.8f, groundCenter.y + 1.0f, groundCenter.z));
    poorcraft::core::Input input;

    const glm::vec3 forward = glm::normalize(glm::vec3(1.0f, 0.0f, 0.0f));
    const glm::vec3 right = glm::normalize(glm::cross(glm::vec3(0.0f, 1.0f, 0.0f), forward));

    player.setViewOrientation(forward, right);

    preloadChunks(player.getEyePosition());

    sendKeyEvent(input, poorcraft::core::Input::KeyCode::W, true);

    for (int frame = 0; frame < 180; ++frame) {
        player.update(input, kDeltaTime, chunkManager());
        chunkManager().update(player.getEyePosition());
        input.reset();
    }

    const float playerMaxX = player.getPosition().x + player.getAABB().max.x;
    EXPECT_LE(playerMaxX, groundCenter.x + 0.5f + 1e-2f);
}
