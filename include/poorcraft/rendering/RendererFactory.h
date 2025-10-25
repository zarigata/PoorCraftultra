#ifndef POORCRAFT_RENDERING_RENDERERFACTORY_H
#define POORCRAFT_RENDERING_RENDERERFACTORY_H

#include "poorcraft/rendering/Renderer.h"
#include <memory>
#include <string>

namespace poorcraft::core
{
class Window;
}

namespace poorcraft::rendering
{
struct RendererSelectionResult
{
    std::unique_ptr<core::Window> window;
    std::unique_ptr<Renderer> renderer;
};

RendererSelectionResult createRenderer(
    const std::string& title,
    int width,
    int height,
    bool resizable = true,
    bool fullscreen = false,
    RendererBackend preferred = RendererBackend::Vulkan);
}

#endif // POORCRAFT_RENDERING_RENDERERFACTORY_H
