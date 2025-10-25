#include "poorcraft/core/Window.h"

#include <SDL2/SDL.h>
#include <SDL2/SDL_vulkan.h>
#include <iostream>
#include <vector>

namespace poorcraft::core
{
namespace
{
Uint32 buildWindowFlags(bool resizable, bool fullscreen, GraphicsAPI graphicsAPI)
{
    Uint32 flags = SDL_WINDOW_SHOWN;
    if(graphicsAPI == GraphicsAPI::Vulkan)
    {
        flags |= SDL_WINDOW_VULKAN;
    }
    else
    {
        flags |= SDL_WINDOW_OPENGL;
    }
    if(resizable)
    {
        flags |= SDL_WINDOW_RESIZABLE;
    }
    if(fullscreen)
    {
        flags |= SDL_WINDOW_FULLSCREEN_DESKTOP;
    }
    return flags;
}
} // namespace

bool Window::initSDL()
{
    if(SDL_Init(SDL_INIT_VIDEO | SDL_INIT_EVENTS) != 0)
    {
        std::cerr << "Failed to initialize SDL: " << SDL_GetError() << "\n";
        return false;
    }
    return true;
}

Window::Window(
    const std::string& title,
    int width,
    int height,
    bool resizable,
    bool fullscreen,
    GraphicsAPI graphicsAPI)
    : m_window(SDL_CreateWindow(
          title.c_str(),
          SDL_WINDOWPOS_CENTERED,
          SDL_WINDOWPOS_CENTERED,
          width,
          height,
          buildWindowFlags(resizable, fullscreen, graphicsAPI)))
    , m_width(width)
    , m_height(height)
    , m_graphicsAPI(graphicsAPI)
{
    if(m_window == nullptr)
    {
        std::cerr << "Failed to create SDL window: " << SDL_GetError() << "\n";
        m_shouldClose = true;
    }
}

Window::~Window()
{
    if(m_window != nullptr)
    {
        SDL_DestroyWindow(m_window);
        m_window = nullptr;
    }
}

bool Window::isOpen() const
{
    return m_window != nullptr && !m_shouldClose;
}

bool Window::shouldClose() const
{
    return m_shouldClose;
}

void Window::pollEvents()
{
    SDL_Event event{};
    std::vector<SDL_Event> forwardedEvents;
    const Uint32 windowId = m_window != nullptr ? SDL_GetWindowID(m_window) : 0;

    while(SDL_PollEvent(&event))
    {
        bool handled = false;

        if(event.type == SDL_QUIT)
        {
            m_shouldClose = true;
            handled = true;
        }
        else if(event.type == SDL_WINDOWEVENT && windowId != 0 && event.window.windowID == windowId)
        {
            handled = true;

            switch(event.window.event)
            {
            case SDL_WINDOWEVENT_CLOSE:
                m_shouldClose = true;
                break;
            case SDL_WINDOWEVENT_SIZE_CHANGED:
                m_width = event.window.data1;
                m_height = event.window.data2;
                break;
            default:
                break;
            }
        }

        if(!handled)
        {
            forwardedEvents.push_back(event);
        }
    }

    for(auto& forwarded : forwardedEvents)
    {
        SDL_PushEvent(&forwarded);
    }
}

SDL_Window* Window::getSDLWindow() const
{
    return m_window;
}

int Window::getWidth() const
{
    return m_width;
}

int Window::getHeight() const
{
    return m_height;
}

GraphicsAPI Window::getGraphicsAPI() const
{
    return m_graphicsAPI;
}

void Window::setVSync(bool enabled)
{
    if(m_graphicsAPI != GraphicsAPI::OpenGL)
    {
        return;
    }
    if(SDL_GL_SetSwapInterval(enabled ? 1 : 0) != 0)
    {
        std::cerr << "Failed to set VSync: " << SDL_GetError() << "\n";
    }
}

bool Window::createVulkanSurface(VkInstance instance, VkSurfaceKHR* surface) const
{
    if(m_window == nullptr)
    {
        return false;
    }

    if(SDL_Vulkan_CreateSurface(m_window, instance, surface) != SDL_TRUE)
    {
        std::cerr << "Failed to create Vulkan surface: " << SDL_GetError() << "\n";
        return false;
    }
    return true;
}

std::vector<const char*> Window::getRequiredVulkanExtensions() const
{
    if(m_window == nullptr)
    {
        return {};
    }

    unsigned int count = 0;
    if(SDL_Vulkan_GetInstanceExtensions(m_window, &count, nullptr) != SDL_TRUE || count == 0)
    {
        return {};
    }

    std::vector<const char*> extensions(count);
    if(SDL_Vulkan_GetInstanceExtensions(m_window, &count, extensions.data()) != SDL_TRUE)
    {
        return {};
    }

    return extensions;
}
} // namespace poorcraft::core
