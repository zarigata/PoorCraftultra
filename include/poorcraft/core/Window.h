#ifndef POORCRAFT_CORE_WINDOW_H
#define POORCRAFT_CORE_WINDOW_H

#include <SDL2/SDL.h>
#include <SDL2/SDL_vulkan.h>
#include <string>
#include <vector>
#include <vulkan/vulkan.h>

namespace poorcraft::core
{
enum class GraphicsAPI
{
    Vulkan,
    OpenGL
};

class Window
{
public:
    static bool initSDL();

    Window(
        const std::string& title,
        int width,
        int height,
        bool resizable = true,
        bool fullscreen = false,
        GraphicsAPI graphicsAPI = GraphicsAPI::Vulkan);
    ~Window();

    Window(const Window&) = delete;
    Window(Window&&) = delete;
    Window& operator=(const Window&) = delete;
    Window& operator=(Window&&) = delete;

    bool isOpen() const;
    bool shouldClose() const;

    void pollEvents();

    SDL_Window* getSDLWindow() const;
    int getWidth() const;
    int getHeight() const;
    GraphicsAPI getGraphicsAPI() const;

    void setVSync(bool enabled);

    bool createVulkanSurface(VkInstance instance, VkSurfaceKHR* surface) const;
    std::vector<const char*> getRequiredVulkanExtensions() const;

private:
    SDL_Window* m_window{nullptr};
    int m_width{0};
    int m_height{0};
    bool m_shouldClose{false};
    GraphicsAPI m_graphicsAPI{GraphicsAPI::Vulkan};
};
} // namespace poorcraft::core

#endif // POORCRAFT_CORE_WINDOW_H
