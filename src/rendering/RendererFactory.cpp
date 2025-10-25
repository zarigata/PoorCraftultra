#include "poorcraft/rendering/RendererFactory.h"

#include "poorcraft/core/Window.h"
#include "poorcraft/rendering/OpenGLRenderer.h"
#include "poorcraft/rendering/VulkanRenderer.h"

#include <iostream>
#include <memory>

namespace poorcraft::rendering
{
namespace
{
void logRendererFailure(const std::string& backend)
{
    std::cerr << "Failed to initialize " << backend << " renderer backend" << '\n';
}

core::GraphicsAPI backendToGraphicsAPI(RendererBackend backend)
{
    return backend == RendererBackend::Vulkan ? core::GraphicsAPI::Vulkan : core::GraphicsAPI::OpenGL;
}
}

RendererSelectionResult createRenderer(
    const std::string& title,
    int width,
    int height,
    bool resizable,
    bool fullscreen,
    RendererBackend preferred)
{
    const auto tryCreate = [&](RendererBackend backend) -> RendererSelectionResult {
        auto window = std::make_unique<core::Window>(
            title,
            width,
            height,
            resizable,
            fullscreen,
            backendToGraphicsAPI(backend));

        if(!window->isOpen())
        {
            logRendererFailure("Window for " + std::string(backend == RendererBackend::Vulkan ? "Vulkan" : "OpenGL"));
            return {};
        }

        switch(backend)
        {
        case RendererBackend::Vulkan:
#ifdef POORCRAFT_VULKAN_ENABLED
            {
                auto renderer = std::make_unique<VulkanRenderer>(*window);
                if(renderer->initialize())
                {
                    std::cout << "Vulkan renderer initialized" << '\n';
                    return {std::move(window), std::move(renderer)};
                }
                logRendererFailure("Vulkan");
            }
#endif
            break;
        case RendererBackend::OpenGL:
#ifdef POORCRAFT_OPENGL_ENABLED
            {
                auto renderer = std::make_unique<OpenGLRenderer>(*window);
                if(renderer->initialize())
                {
                    std::cout << "OpenGL renderer initialized" << '\n';
                    return {std::move(window), std::move(renderer)};
                }
                logRendererFailure("OpenGL");
            }
#endif
            break;
        }
        return {};
    };

    if(auto selection = tryCreate(preferred); selection.renderer != nullptr)
    {
        return selection;
    }

    if(preferred == RendererBackend::Vulkan)
    {
        return tryCreate(RendererBackend::OpenGL);
    }
    return tryCreate(RendererBackend::Vulkan);
}
} // namespace poorcraft::rendering
