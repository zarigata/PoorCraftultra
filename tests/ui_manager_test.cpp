#include <gtest/gtest.h>

#include "poorcraft/core/GameState.h"
#include "poorcraft/core/Input.h"
#include "poorcraft/core/Inventory.h"
#include "poorcraft/core/Timer.h"
#include "poorcraft/rendering/Renderer.h"
#include "poorcraft/ui/UIManager.h"
#include "poorcraft/rendering/TextureAtlas.h"
#include "poorcraft/world/ChunkManager.h"

namespace poorcraft::ui
{
namespace
{
class StubRenderer : public rendering::Renderer
{
public:
    bool initialize() override { return true; }
    void shutdown() override {}
    void beginFrame() override {}
    void clear(float, float, float, float) override {}
    void endFrame() override {}
    rendering::RendererCapabilities getCapabilities() const override { return {}; }
    bool isVSyncEnabled() const override { return m_vsync; }
    void setVSync(bool enabled) override { m_vsync = enabled; }
    void setViewProjection(const glm::mat4&, const glm::mat4&) override {}
    BufferHandle createVertexBuffer(const void*, std::size_t) override { return 1; }
    BufferHandle createIndexBuffer(const void*, std::size_t) override { return 1; }
    void destroyBuffer(BufferHandle) override {}
    void drawIndexed(BufferHandle, BufferHandle, std::uint32_t, const glm::mat4&) override {}
    bool initializeUI() override { m_uiInitialized = true; return true; }
    void shutdownUI() override { m_uiInitialized = false; }
    void beginUIPass() override { if(m_uiInitialized) { ++beginCalls; } }
    void renderUI() override { if(m_uiInitialized) { ++renderCalls; } }

    int beginCalls{0};
    int renderCalls{0};

private:
    bool m_vsync{true};
    rendering::TextureAtlas m_atlas;
    bool m_uiInitialized{false};
};

class StubChunkManager : public world::ChunkManager
{
public:
    explicit StubChunkManager(rendering::Renderer& renderer)
        : world::ChunkManager(renderer, m_atlas, 0)
    {
        m_atlas.initialize(16);
    }

    void update(const glm::vec3&) override {}
    void render() override {}

private:
    rendering::TextureAtlas m_atlas;
};

class UIManagerTest : public ::testing::Test
{
protected:
    UIManagerTest()
        : chunkManager(renderer)
        , uiManager(gameStateManager, renderer, input, timer, inventory, chunkManager)
    {}

    rendering::RendererCapabilities capabilities{};
    GameStateManager gameStateManager;
    core::Input input;
    core::Timer timer;
    core::Inventory inventory;
    StubRenderer renderer;
    StubChunkManager chunkManager;
    UIManager uiManager;
};
} // namespace

TEST_F(UIManagerTest, InitializeCreatesUiComponents)
{
    renderer.initializeUI();
    uiManager.initialize();

    SDL_Event event{};
    event.type = SDL_MOUSEMOTION;
    uiManager.processEvent(event); // ensure forwarding does not crash

    uiManager.render();
    EXPECT_GE(renderer.beginCalls, 1);
    EXPECT_GE(renderer.renderCalls, 1);
}

TEST_F(UIManagerTest, StateTransitionsUpdateMouseCapture)
{
    renderer.initializeUI();
    uiManager.initialize();

    EXPECT_FALSE(uiManager.wantsCaptureMouse());
    EXPECT_FALSE(uiManager.wantsCaptureKeyboard());

    gameStateManager.setState(core::GameState::Playing);
    uiManager.render();
    EXPECT_GE(renderer.beginCalls, 1);

    gameStateManager.pushState(core::GameState::Paused);
    EXPECT_FALSE(input.isRelativeMouseMode());
}

TEST_F(UIManagerTest, SettingsStateLoadsCurrentSettings)
{
    renderer.initializeUI();
    uiManager.initialize();

    gameStateManager.setState(core::GameState::Settings);
    uiManager.render();

    EXPECT_GE(renderer.beginCalls, 1);
}

TEST_F(UIManagerTest, ShutdownResetsState)
{
    renderer.initializeUI();
    uiManager.initialize();
    uiManager.shutdown();

    UIManager newManager(gameStateManager, renderer, input, timer, inventory, chunkManager);
    newManager.initialize();
    newManager.render();
    EXPECT_GE(renderer.beginCalls, 1);
}
} // namespace poorcraft::ui
