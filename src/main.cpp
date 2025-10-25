#include "poorcraft/core/Camera.h"
#include "poorcraft/core/GPUInfo.h"
#include "poorcraft/core/Input.h"
#include "poorcraft/core/Timer.h"
#include "poorcraft/core/Window.h"
#include "poorcraft/rendering/RendererFactory.h"
#include "poorcraft/rendering/VulkanRenderer.h"
#include "poorcraft/world/ChunkManager.h"

#include <glm/glm.hpp>

#include <cstdlib>
#include <iostream>
#include <memory>
#include <SDL2/SDL.h>

using poorcraft::core::Camera;
using poorcraft::core::Input;
using poorcraft::core::Timer;
using poorcraft::core::Window;
using poorcraft::core::gpu::enumerateGPUs;
using poorcraft::core::gpu::getGPUInfoFromOpenGL;
using poorcraft::core::gpu::vendorToString;
using poorcraft::rendering::Renderer;
using poorcraft::rendering::RendererBackend;
using poorcraft::rendering::RendererCapabilities;
using poorcraft::rendering::createRenderer;

namespace
{
constexpr float kClearColorR = 0.39f;
constexpr float kClearColorG = 0.58f;
constexpr float kClearColorB = 0.93f;
constexpr float kClearColorA = 1.0f;
} // namespace

int main()
{
    if(!Window::initSDL())
    {
        std::cerr << "Failed to initialize SDL. Exiting." << std::endl;
        return EXIT_FAILURE;
    }

    constexpr int kWindowWidth = 1280;
    constexpr int kWindowHeight = 720;
    auto selection = createRenderer(
        "PoorCraft Engine v0.1.0",
        kWindowWidth,
        kWindowHeight,
        true,
        false,
        RendererBackend::Vulkan);

    auto window = std::move(selection.window);
    auto renderer = std::move(selection.renderer);

    if(window == nullptr || renderer == nullptr)
    {
        std::cerr << "Unable to initialize any rendering backend. Exiting." << std::endl;
        SDL_Quit();
        return EXIT_FAILURE;
    }

    renderer->setVSync(true);

    poorcraft::world::ChunkManager chunkManager(*renderer, 12345u);

    RendererCapabilities capabilities = renderer->getCapabilities();
    std::cout << "Renderer backend: "
              << (capabilities.backend == RendererBackend::Vulkan ? "Vulkan" : "OpenGL") << std::endl;
    std::cout << "Backend version: " << capabilities.backendVersion << std::endl;
    std::cout << "Max texture size: " << capabilities.maxTextureSize << std::endl;
    std::cout << "Ray tracing support: " << (capabilities.supportsRayTracing ? "Yes" : "No") << std::endl;

#ifdef POORCRAFT_VULKAN_ENABLED
    if(capabilities.backend == RendererBackend::Vulkan)
    {
        if(auto* vulkanRenderer = dynamic_cast<poorcraft::rendering::VulkanRenderer*>(renderer.get()))
        {
            const auto gpus = enumerateGPUs(vulkanRenderer->getInstance());
            for(const auto& gpu : gpus)
            {
                std::cout << "GPU: " << vendorToString(gpu.vendor) << " - " << gpu.deviceName
                          << " (Vendor ID: " << gpu.vendorID << ", Device ID: " << gpu.deviceID
                          << ", Driver: " << gpu.driverVersion << ")" << std::endl;
            }
        }
    }
#endif

#ifdef POORCRAFT_OPENGL_ENABLED
    if(capabilities.backend == RendererBackend::OpenGL)
    {
        const auto gpu = getGPUInfoFromOpenGL();
        std::cout << "GPU: " << vendorToString(gpu.vendor) << " - " << gpu.deviceName << std::endl;
    }
#endif

    Input input;
    Camera camera(glm::vec3(0.0f, 0.0f, 5.0f), 0.0f, 0.0f);
    constexpr float kMovementSpeed = 5.0f;
    constexpr float kMouseSensitivity = 0.002f;
    constexpr float kFieldOfView = glm::radians(60.0f);
    constexpr float kNearPlane = 0.1f;
    constexpr float kFarPlane = 1000.0f;
    input.setRelativeMouseMode(true);

    Timer timer;
    int frameCounter = 0;

    while(window->isOpen() && !window->shouldClose())
    {
        input.reset();
        window->pollEvents();

        SDL_Event event{};
        while(SDL_PollEvent(&event))
        {
            input.processEvent(event);
        }

        timer.tick();
        const float deltaTime = static_cast<float>(timer.getDeltaTime());

        glm::vec3 movement(0.0f);
        const glm::vec3 forward = camera.getForward();
        const glm::vec3 right = camera.getRight();
        const glm::vec3 worldUp(0.0f, 1.0f, 0.0f);

        // Flatten forward vector to XZ plane for W/S movement
        glm::vec3 flatForward(forward.x, 0.0f, forward.z);
        if(glm::length(flatForward) > 0.0f)
        {
            flatForward = glm::normalize(flatForward);
        }

        if(input.isKeyDown(Input::KeyCode::W))
        {
            movement += flatForward;
        }
        if(input.isKeyDown(Input::KeyCode::S))
        {
            movement -= flatForward;
        }
        if(input.isKeyDown(Input::KeyCode::A))
        {
            movement -= right;
        }
        if(input.isKeyDown(Input::KeyCode::D))
        {
            movement += right;
        }
        if(input.isKeyDown(Input::KeyCode::Space))
        {
            movement += worldUp;
        }
        if(input.isKeyDown(Input::KeyCode::LeftShift))
        {
            movement -= worldUp;
        }

        if(glm::length(movement) > 0.0f)
        {
            movement = glm::normalize(movement) * kMovementSpeed * deltaTime;
            camera.translate(movement);
        }

        const auto mouseDelta = input.getMouseDelta();
        if(input.isRelativeMouseMode())
        {
            camera.rotate(mouseDelta.x * kMouseSensitivity, -mouseDelta.y * kMouseSensitivity);
        }

        if(input.isKeyPressed(Input::KeyCode::Escape))
        {
            input.setRelativeMouseMode(false);
        }

        chunkManager.update(camera.getPosition());

        const float aspectRatio = static_cast<float>(window->getWidth()) /
                                  static_cast<float>(window->getHeight());
        const auto viewMatrix = camera.getViewMatrix();
        const auto projectionMatrix = camera.getProjectionMatrix(kFieldOfView, aspectRatio, kNearPlane, kFarPlane);

        renderer->beginFrame();
        renderer->clear(kClearColorR, kClearColorG, kClearColorB, kClearColorA);
        renderer->setViewProjection(viewMatrix, projectionMatrix);
        chunkManager.render();
        renderer->endFrame();

        ++frameCounter;
        if(frameCounter >= 60)
        {
            const auto& cameraPosition = camera.getPosition();
            std::cout << "FPS: " << timer.getFPS() << " | Chunks: " << chunkManager.getLoadedChunkCount()
                      << " | Camera: (" << cameraPosition.x << ", "
                      << cameraPosition.y << ", " << cameraPosition.z << ")" << std::endl;
            frameCounter = 0;
        }
    }

    renderer->shutdown();
    renderer.reset();
    window.reset();

    SDL_Quit();
    return EXIT_SUCCESS;
}
