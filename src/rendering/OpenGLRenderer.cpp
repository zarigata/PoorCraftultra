#include "poorcraft/rendering/OpenGLRenderer.h"

#ifdef POORCRAFT_OPENGL_ENABLED

#    include "poorcraft/core/Window.h"

#    include <SDL2/SDL.h>
#    include <SDL2/SDL_opengl.h>
#    include "poorcraft/world/ChunkMesh.h"
#    include <glm/gtc/type_ptr.hpp>
#    include <algorithm>
#    include <array>
#    include <cstddef>
#    include <iostream>
#    include <string>

#    include <imgui.h>
#    include <backends/imgui_impl_sdl2.h>
#    include <backends/imgui_impl_opengl3.h>

namespace poorcraft::rendering
{
namespace
{
using PFN_glClearColor = void (*)(float, float, float, float);
using PFN_glClear = void (*)(unsigned int);
using PFN_glGetString = const GLubyte* (*)(unsigned int);
using PFN_glGetIntegerv = void (*)(unsigned int, int*);
using PFN_glEnable = void (*)(unsigned int);
using PFN_glDisable = void (*)(unsigned int);
using PFN_glDebugMessageCallback = void (*)(GLDEBUGPROC, const void*);
using PFN_glCreateShader = unsigned int (*)(unsigned int);
using PFN_glShaderSource = void (*)(unsigned int, int, const char**, const int*);
using PFN_glCompileShader = void (*)(unsigned int);
using PFN_glGetShaderiv = void (*)(unsigned int, unsigned int, int*);
using PFN_glGetShaderInfoLog = void (*)(unsigned int, int, int*, char*);
using PFN_glCreateProgram = unsigned int (*)();
using PFN_glAttachShader = void (*)(unsigned int, unsigned int);
using PFN_glLinkProgram = void (*)(unsigned int);
using PFN_glGetProgramiv = void (*)(unsigned int, unsigned int, int*);
using PFN_glGetProgramInfoLog = void (*)(unsigned int, int, int*, char*);
using PFN_glDeleteShader = void (*)(unsigned int);
using PFN_glDeleteProgram = void (*)(unsigned int);
using PFN_glUseProgram = void (*)(unsigned int);
using PFN_glGetUniformLocation = int (*)(unsigned int, const char*);
using PFN_glUniformMatrix4fv = void (*)(int, int, bool, const float*);
using PFN_glGenVertexArrays = void (*)(int, unsigned int*);
using PFN_glBindVertexArray = void (*)(unsigned int);
using PFN_glDeleteVertexArrays = void (*)(int, const unsigned int*);
using PFN_glGenBuffers = void (*)(int, unsigned int*);
using PFN_glBindBuffer = void (*)(unsigned int, unsigned int);
using PFN_glBufferData = void (*)(unsigned int, std::ptrdiff_t, const void*, unsigned int);
using PFN_glDeleteBuffers = void (*)(int, const unsigned int*);
using PFN_glEnableVertexAttribArray = void (*)(unsigned int);
using PFN_glVertexAttribPointer = void (*)(unsigned int, int, unsigned int, bool, int, const void*);
using PFN_glUniform1i = void (*)(int, int);
using PFN_glUniform1f = void (*)(int, float);
using PFN_glUniform3f = void (*)(int, float, float, float);
using PFN_glUniform4f = void (*)(int, float, float, float, float);
using PFN_glDrawElements = void (*)(unsigned int, int, unsigned int, const void*);
using PFN_glGenTextures = void (*)(int, unsigned int*);
using PFN_glDeleteTextures = void (*)(int, const unsigned int*);
using PFN_glBindTexture = void (*)(unsigned int, unsigned int);
using PFN_glTexParameteri = void (*)(unsigned int, unsigned int, int);
using PFN_glTexImage2D = void (*)(unsigned int, int, int, int, int, int, unsigned int, unsigned int, const void*);
using PFN_glActiveTexture = void (*)(unsigned int);
using PFN_glGenQueries = void (*)(int, unsigned int*);
using PFN_glDeleteQueries = void (*)(int, const unsigned int*);
using PFN_glBeginQuery = void (*)(unsigned int, unsigned int);
using PFN_glEndQuery = void (*)(unsigned int);
using PFN_glGetQueryObjectiv = void (*)(unsigned int, unsigned int, int*);
using PFN_glGetQueryObjectui64v = void (*)(unsigned int, unsigned int, unsigned long long*);

PFN_glClearColor s_glClearColor = nullptr;
PFN_glClear s_glClear = nullptr;
PFN_glGetString s_glGetString = nullptr;
PFN_glGetIntegerv s_glGetIntegerv = nullptr;
PFN_glEnable s_glEnable = nullptr;
PFN_glDisable s_glDisable = nullptr;
PFN_glDebugMessageCallback s_glDebugMessageCallback = nullptr;
PFN_glCreateShader s_glCreateShader = nullptr;
PFN_glShaderSource s_glShaderSource = nullptr;
PFN_glCompileShader s_glCompileShader = nullptr;
PFN_glGetShaderiv s_glGetShaderiv = nullptr;
PFN_glGetShaderInfoLog s_glGetShaderInfoLog = nullptr;
PFN_glCreateProgram s_glCreateProgram = nullptr;
PFN_glAttachShader s_glAttachShader = nullptr;
PFN_glLinkProgram s_glLinkProgram = nullptr;
PFN_glGetProgramiv s_glGetProgramiv = nullptr;
PFN_glGetProgramInfoLog s_glGetProgramInfoLog = nullptr;
PFN_glDeleteShader s_glDeleteShader = nullptr;
PFN_glDeleteProgram s_glDeleteProgram = nullptr;
PFN_glUseProgram s_glUseProgram = nullptr;
PFN_glGetUniformLocation s_glGetUniformLocation = nullptr;
PFN_glUniformMatrix4fv s_glUniformMatrix4fv = nullptr;
PFN_glGenVertexArrays s_glGenVertexArrays = nullptr;
PFN_glBindVertexArray s_glBindVertexArray = nullptr;
PFN_glDeleteVertexArrays s_glDeleteVertexArrays = nullptr;
PFN_glGenBuffers s_glGenBuffers = nullptr;
PFN_glBindBuffer s_glBindBuffer = nullptr;
PFN_glBufferData s_glBufferData = nullptr;
PFN_glDeleteBuffers s_glDeleteBuffers = nullptr;
PFN_glEnableVertexAttribArray s_glEnableVertexAttribArray = nullptr;
PFN_glVertexAttribPointer s_glVertexAttribPointer = nullptr;
PFN_glUniform1i s_glUniform1i = nullptr;
PFN_glUniform1f s_glUniform1f = nullptr;
PFN_glUniform3f s_glUniform3f = nullptr;
PFN_glUniform4f s_glUniform4f = nullptr;
PFN_glDrawElements s_glDrawElements = nullptr;
PFN_glGenTextures s_glGenTextures = nullptr;
PFN_glDeleteTextures s_glDeleteTextures = nullptr;
PFN_glBindTexture s_glBindTexture = nullptr;
PFN_glTexParameteri s_glTexParameteri = nullptr;
PFN_glTexImage2D s_glTexImage2D = nullptr;
PFN_glActiveTexture s_glActiveTexture = nullptr;
PFN_glGenQueries s_glGenQueries = nullptr;
PFN_glDeleteQueries s_glDeleteQueries = nullptr;
PFN_glBeginQuery s_glBeginQuery = nullptr;
PFN_glEndQuery s_glEndQuery = nullptr;
PFN_glGetQueryObjectiv s_glGetQueryObjectiv = nullptr;
PFN_glGetQueryObjectui64v s_glGetQueryObjectui64v = nullptr;

constexpr unsigned int GL_COLOR_BUFFER_BIT_CONST = 0x00004000;
constexpr unsigned int GL_DEPTH_BUFFER_BIT_CONST = 0x00000100;
constexpr unsigned int GL_DEBUG_OUTPUT_CONST = 0x92E0;
constexpr unsigned int GL_MAX_TEXTURE_SIZE_CONST = 0x0D33;
constexpr unsigned int GL_VERSION_CONST = 0x1F02;
constexpr unsigned int GL_VENDOR_CONST = 0x1F00;
constexpr unsigned int GL_RENDERER_CONST = 0x1F01;
constexpr unsigned int GL_COMPILE_STATUS_CONST = 0x8B81;
constexpr unsigned int GL_INFO_LOG_LENGTH_CONST = 0x8B84;
constexpr unsigned int GL_LINK_STATUS_CONST = 0x8B82;
constexpr unsigned int GL_ARRAY_BUFFER_CONST = 0x8892;
constexpr unsigned int GL_ELEMENT_ARRAY_BUFFER_CONST = 0x8893;
constexpr unsigned int GL_STATIC_DRAW_CONST = 0x88E4;
constexpr unsigned int GL_FLOAT_CONST = 0x1406;
constexpr unsigned int GL_TRIANGLES_CONST = 0x0004;
constexpr unsigned int GL_UNSIGNED_INT_CONST = 0x1405;
constexpr unsigned int GL_DEPTH_TEST_CONST = 0x0B71;
constexpr unsigned int GL_TEXTURE_2D_CONST = 0x0DE1;
constexpr unsigned int GL_RGBA_CONST = 0x1908;
constexpr unsigned int GL_RGB_CONST = 0x1907;
constexpr unsigned int GL_UNSIGNED_BYTE_CONST = 0x1401;
constexpr unsigned int GL_TEXTURE_MIN_FILTER_CONST = 0x2801;
constexpr unsigned int GL_TEXTURE_MAG_FILTER_CONST = 0x2800;
constexpr unsigned int GL_TEXTURE_WRAP_S_CONST = 0x2802;
constexpr unsigned int GL_TEXTURE_WRAP_T_CONST = 0x2803;
constexpr unsigned int GL_NEAREST_CONST = 0x2600;
constexpr unsigned int GL_LINEAR_CONST = 0x2601;
constexpr unsigned int GL_REPEAT_CONST = 0x2901;
constexpr unsigned int GL_TEXTURE0_CONST = 0x84C0;
constexpr unsigned int GL_QUERY_RESULT_CONST = 0x8866;
constexpr unsigned int GL_TIME_ELAPSED_CONST = 0x88BF;
} // namespace

OpenGLRenderer::OpenGLRenderer(core::Window& window)
    : m_window(window)
{}

Renderer::TextureHandle OpenGLRenderer::createTexture(const void* data, std::uint32_t width, std::uint32_t height, std::uint32_t channels)
{
    if(s_glGenTextures == nullptr || s_glBindTexture == nullptr || s_glTexParameteri == nullptr || s_glTexImage2D == nullptr)
    {
        return 0;
    }

    if(width == 0 || height == 0 || data == nullptr)
    {
        return 0;
    }

    unsigned int textureId = 0;
    s_glGenTextures(1, &textureId);
    if(textureId == 0)
    {
        return 0;
    }

    s_glBindTexture(GL_TEXTURE_2D_CONST, textureId);
    s_glTexParameteri(GL_TEXTURE_2D_CONST, GL_TEXTURE_MIN_FILTER_CONST, GL_NEAREST_CONST);
    s_glTexParameteri(GL_TEXTURE_2D_CONST, GL_TEXTURE_MAG_FILTER_CONST, GL_NEAREST_CONST);
    s_glTexParameteri(GL_TEXTURE_2D_CONST, GL_TEXTURE_WRAP_S_CONST, GL_REPEAT_CONST);
    s_glTexParameteri(GL_TEXTURE_2D_CONST, GL_TEXTURE_WRAP_T_CONST, GL_REPEAT_CONST);

    const unsigned int format = channels >= 4 ? GL_RGBA_CONST : GL_RGB_CONST;
    s_glTexImage2D(
        GL_TEXTURE_2D_CONST,
        0,
        static_cast<int>(format),
        static_cast<int>(width),
        static_cast<int>(height),
        0,
        format,
        GL_UNSIGNED_BYTE_CONST,
        data);

    s_glBindTexture(GL_TEXTURE_2D_CONST, 0);

    const TextureHandle handle = m_nextTextureHandle++;
    m_textures.emplace(handle, TextureResource{textureId, width, height});
    return handle;
}

void OpenGLRenderer::destroyTexture(TextureHandle handle)
{
    if(handle == 0 || handle == m_defaultTexture)
    {
        return;
    }

    if(auto it = m_textures.find(handle); it != m_textures.end())
    {
        if(it->second.id != 0 && s_glDeleteTextures != nullptr)
        {
            s_glDeleteTextures(1, &it->second.id);
        }
        m_textures.erase(it);
    }
}

void OpenGLRenderer::bindTexture(TextureHandle handle, std::uint32_t slot)
{
    if(s_glActiveTexture == nullptr || s_glBindTexture == nullptr)
    {
        return;
    }

    if(slot >= m_activeTextures.size())
    {
        return;
    }

    TextureHandle resolvedHandle = handle;
    if(auto it = m_textures.find(handle); it == m_textures.end())
    {
        resolvedHandle = m_defaultTexture;
    }

    const auto resourceIt = m_textures.find(resolvedHandle);
    if(resourceIt == m_textures.end())
    {
        return;
    }

    s_glActiveTexture(GL_TEXTURE0_CONST + slot);
    s_glBindTexture(GL_TEXTURE_2D_CONST, resourceIt->second.id);
    m_activeTextures[slot] = resolvedHandle;
}

void OpenGLRenderer::setLightingParams(const LightingParams& params)
{
    m_lightingParams = params;
    // Normalize sun direction (xyz components of sunDirAndIntensity)
    glm::vec3 sunDir = glm::vec3(m_lightingParams.sunDirAndIntensity);
    if(glm::length(sunDir) > 0.0f)
    {
        sunDir = glm::normalize(sunDir);
        m_lightingParams.sunDirAndIntensity.x = sunDir.x;
        m_lightingParams.sunDirAndIntensity.y = sunDir.y;
        m_lightingParams.sunDirAndIntensity.z = sunDir.z;
    }
    m_lightingDirty = true;
}

bool OpenGLRenderer::initializeUI()
{
    if(m_imguiInitialized)
    {
        return true;
    }

    IMGUI_CHECKVERSION();
    ImGui::CreateContext();
    ImGuiIO& io = ImGui::GetIO();
    io.ConfigFlags |= ImGuiConfigFlags_NavEnableKeyboard;
    io.ConfigFlags |= ImGuiConfigFlags_NavEnableGamepad;

    if(!ImGui_ImplSDL2_InitForOpenGL(m_window.getSDLWindow(), m_glContext))
    {
        std::cerr << "ImGui_ImplSDL2_InitForOpenGL failed\n";
        return false;
    }

    if(!ImGui_ImplOpenGL3_Init("#version 330"))
    {
        std::cerr << "ImGui_ImplOpenGL3_Init failed\n";
        return false;
    }

    m_imguiInitialized = true;
    return true;
}

void OpenGLRenderer::shutdownUI()
{
    if(!m_imguiInitialized)
    {
        return;
    }

    ImGui_ImplOpenGL3_Shutdown();
    ImGui_ImplSDL2_Shutdown();
    ImGui::DestroyContext();

    m_imguiInitialized = false;
}

void OpenGLRenderer::beginUIPass()
{
    if(!m_imguiInitialized)
    {
        return;
    }

    ImGui_ImplOpenGL3_NewFrame();
    ImGui_ImplSDL2_NewFrame();
    ImGui::NewFrame();
}

void OpenGLRenderer::renderUI()
{
    if(!m_imguiInitialized)
    {
        return;
    }

    ImGui::Render();
    ImDrawData* drawData = ImGui::GetDrawData();
    if(drawData == nullptr)
    {
        return;
    }

    ImGui_ImplOpenGL3_RenderDrawData(drawData);
}

bool OpenGLRenderer::initialize()
{
    if(!createGLContext())
    {
        return false;
    }

    if(!loadGLFunctions())
    {
        std::cerr << "Failed to load OpenGL function pointers\n";
        return false;
    }

#ifndef NDEBUG
    if(s_glEnable != nullptr)
    {
        s_glEnable(GL_DEBUG_OUTPUT_CONST);
    }
    if(s_glDebugMessageCallback != nullptr)
    {
        s_glDebugMessageCallback(
            [](unsigned int /*source*/, unsigned int /*type*/, unsigned int /*id*/, unsigned int /*severity*/, int /*length*/, const char* message, const void* /*userParam*/) {
                std::cerr << "[OpenGL] " << message << '\n';
            },
            nullptr);
    }
#endif

    if(!createShaderProgram())
    {
        std::cerr << "Failed to create OpenGL shader program\n";
        return false;
    }

    if(s_glEnable != nullptr)
    {
        s_glEnable(GL_DEPTH_TEST_CONST);
    }

    if(!initializeUI())
    {
        std::cerr << "Failed to initialize ImGui for OpenGL\n";
        return false;
    }

    applyVSync();

    m_activeTextures.fill(0);
    m_defaultTexture = createDefaultTexture();
    if(m_defaultTexture == 0)
    {
        std::cerr << "Failed to create default texture\n";
        return false;
    }
    for(std::size_t i = 0; i < m_activeTextures.size(); ++i)
    {
        bindTexture(m_defaultTexture, static_cast<std::uint32_t>(i));
    }

    return true;
}

void OpenGLRenderer::shutdown()
{
    shutdownUI();

    for(auto& [handle, resource] : m_vertexBuffers)
    {
        if(resource.buffer != 0 && s_glDeleteBuffers != nullptr)
        {
            s_glDeleteBuffers(1, &resource.buffer);
        }
        if(resource.vao != 0 && s_glDeleteVertexArrays != nullptr)
        {
            s_glDeleteVertexArrays(1, &resource.vao);
        }
    }
    m_vertexBuffers.clear();

    for(auto& [handle, resource] : m_indexBuffers)
    {
        if(resource.buffer != 0 && s_glDeleteBuffers != nullptr)
        {
            s_glDeleteBuffers(1, &resource.buffer);
        }
    }
    m_indexBuffers.clear();

    for(auto& [handle, resource] : m_textures)
    {
        if(resource.id != 0 && s_glDeleteTextures != nullptr)
        {
            s_glDeleteTextures(1, &resource.id);
        }
    }
    m_textures.clear();
    m_nextTextureHandle = 1;
    m_defaultTexture = 0;

    destroyShaderProgram();

    if(m_glContext != nullptr)
    {
        SDL_GL_DeleteContext(m_glContext);
        m_glContext = nullptr;
    }
}

void OpenGLRenderer::beginFrame()
{
    if(s_glUseProgram != nullptr && m_shaderProgram != 0)
    {
        s_glUseProgram(m_shaderProgram);
        applyLightingUniforms();
    }
}

void OpenGLRenderer::clear(float r, float g, float b, float a)
{
    if(s_glClearColor != nullptr)
    {
        s_glClearColor(r, g, b, a);
    }
    if(s_glClear != nullptr)
    {
        s_glClear(GL_COLOR_BUFFER_BIT_CONST | GL_DEPTH_BUFFER_BIT_CONST);
    }
}

void OpenGLRenderer::endFrame()
{
    if(s_glUniformMatrix4fv != nullptr && m_shaderProgram != 0)
    {
        s_glUniformMatrix4fv(m_viewProjLocation, 1, false, glm::value_ptr(m_viewProjection));
    }

    for(const auto& command : m_drawCommands)
    {
        const auto vertexIt = m_vertexBuffers.find(command.vertexBuffer);
        const auto indexIt = m_indexBuffers.find(command.indexBuffer);
        if(vertexIt == m_vertexBuffers.end() || indexIt == m_indexBuffers.end())
        {
            continue;
        }

        const BufferResource& vertexResource = vertexIt->second;
        const BufferResource& indexResource = indexIt->second;

        if(s_glBindVertexArray != nullptr)
        {
            s_glBindVertexArray(vertexResource.vao);
        }

        if(s_glBindBuffer != nullptr)
        {
            s_glBindBuffer(GL_ELEMENT_ARRAY_BUFFER_CONST, indexResource.buffer);
        }

        if(s_glUniformMatrix4fv != nullptr)
        {
            s_glUniformMatrix4fv(m_modelLocation, 1, false, glm::value_ptr(command.modelMatrix));
        }

        if(s_glDrawElements != nullptr)
        {
            s_glDrawElements(GL_TRIANGLES_CONST, static_cast<int>(command.indexCount), GL_UNSIGNED_INT_CONST, nullptr);
        }
    }

    if(s_glBindVertexArray != nullptr)
    {
        s_glBindVertexArray(0);
    }

    m_drawCommands.clear();

    SDL_GL_SwapWindow(m_window.getSDLWindow());
}

void OpenGLRenderer::beginPerformanceCapture()
{
    m_frameCaptureStart = std::chrono::high_resolution_clock::now();
    m_currentMetrics = {};
    if(!m_timerQueriesSupported && !createTimerQueries())
    {
        return;
    }

    if(m_timerQueriesSupported && s_glGenQueries != nullptr)
    {
        s_glQueryCounter(m_timerQueries[0], GL_TIMESTAMP_CONST);
    }
}

void OpenGLRenderer::endPerformanceCapture()
{
    const auto now = std::chrono::high_resolution_clock::now();
    m_currentMetrics.cpu.frameTimeMs = std::chrono::duration<double, std::milli>(now - m_frameCaptureStart).count();
    m_currentMetrics.fps = m_currentMetrics.cpu.frameTimeMs > 0.0 ? 1000.0 / m_currentMetrics.cpu.frameTimeMs : 0.0;

    if(m_timerQueriesSupported && s_glGetQueryObjectiv != nullptr && s_glGetQueryObjectui64v != nullptr)
    {
        GLuint available = 0;
        s_glGetQueryObjectiv(m_timerQueries[3], GL_QUERY_RESULT_AVAILABLE_CONST, reinterpret_cast<GLint*>(&available));
        if(available != 0)
        {
            std::uint64_t startTimestamp = 0;
            std::uint64_t endTimestamp = 0;
            s_glGetQueryObjectui64v(m_timerQueries[0], GL_QUERY_RESULT_CONST, &startTimestamp);
            s_glGetQueryObjectui64v(m_timerQueries[3], GL_QUERY_RESULT_CONST, &endTimestamp);
            const double frameMs = static_cast<double>(endTimestamp - startTimestamp) / 1'000'000.0;
            const double renderPassMs = getQueryResultMs(m_timerQueries[1]);
            const double uiPassMs = getQueryResultMs(m_timerQueries[2]);

            m_currentMetrics.gpu.available = true;
            m_currentMetrics.gpu.renderPassTimeMs = renderPassMs;
            m_currentMetrics.gpu.uiPassTimeMs = std::max(0.0, frameMs - renderPassMs - uiPassMs);
        }
    }

    m_metricsHistory[m_metricsHistoryIndex] = m_currentMetrics;
    m_metricsHistoryIndex = (m_metricsHistoryIndex + 1) % m_metricsHistory.size();

    PerformanceMetrics accumulated{};
    std::size_t count = 0;
    for(const auto& metrics : m_metricsHistory)
    {
        if(metrics.cpu.frameTimeMs <= 0.0)
        {
            continue;
        }
        ++count;
        accumulated.cpu.frameTimeMs += metrics.cpu.frameTimeMs;
        accumulated.gpu.renderPassTimeMs += metrics.gpu.renderPassTimeMs;
        accumulated.gpu.uiPassTimeMs += metrics.gpu.uiPassTimeMs;
        accumulated.fps += metrics.fps;
    }

    if(count > 0)
    {
        m_currentMetrics.cpu.frameTimeMs = accumulated.cpu.frameTimeMs / static_cast<double>(count);
        m_currentMetrics.gpu.renderPassTimeMs = accumulated.gpu.renderPassTimeMs / static_cast<double>(count);
        m_currentMetrics.gpu.uiPassTimeMs = accumulated.gpu.uiPassTimeMs / static_cast<double>(count);
        m_currentMetrics.fps = accumulated.fps / static_cast<double>(count);
        m_currentMetrics.gpu.available = true;
    }
}

PerformanceMetrics OpenGLRenderer::getPerformanceMetrics() const
{
    return m_currentMetrics;
}

bool OpenGLRenderer::createTimerQueries()
{
    if(s_glGenQueries == nullptr || s_glQueryCounter == nullptr || s_glGetQueryObjectiv == nullptr || s_glGetQueryObjectui64v == nullptr)
    {
        return false;
    }

    s_glGenQueries(static_cast<int>(std::size(m_timerQueries)), m_timerQueries);
    m_timerQueriesSupported = true;
    return true;
}

void OpenGLRenderer::destroyTimerQueries()
{
    if(!m_timerQueriesSupported || s_glDeleteQueries == nullptr)
    {
        return;
    }

    s_glDeleteQueries(static_cast<int>(std::size(m_timerQueries)), m_timerQueries);
    m_timerQueriesSupported = false;
}

double OpenGLRenderer::getQueryResultMs(unsigned int query) const
{
    if(s_glGetQueryObjectui64v == nullptr)
    {
        return 0.0;
    }

    std::uint64_t result = 0;
    s_glGetQueryObjectui64v(query, GL_QUERY_RESULT_CONST, &result);
    return static_cast<double>(result) / 1'000'000.0;
}

RendererCapabilities OpenGLRenderer::getCapabilities() const
{
    RendererCapabilities capabilities{};
    capabilities.backend = RendererBackend::OpenGL;

    if(s_glGetString != nullptr)
    {
        const GLubyte* versionStr = s_glGetString(GL_VERSION_CONST);
        if(versionStr != nullptr)
        {
            capabilities.backendVersion = reinterpret_cast<const char*>(versionStr);
        }
    }

    if(s_glGetIntegerv != nullptr)
    {
        int maxTextureSize = 0;
        s_glGetIntegerv(GL_MAX_TEXTURE_SIZE_CONST, &maxTextureSize);
        capabilities.maxTextureSize = static_cast<unsigned int>(maxTextureSize);
    }

    capabilities.supportsRayTracing = false;
    return capabilities;
}

bool OpenGLRenderer::isVSyncEnabled() const
{
    return m_vsyncEnabled;
}

void OpenGLRenderer::setVSync(bool enabled)
{
    if(m_vsyncEnabled == enabled)
    {
        return;
    }

    m_vsyncEnabled = enabled;
    applyVSync();
}

bool OpenGLRenderer::createGLContext()
{
    SDL_GL_SetAttribute(SDL_GL_CONTEXT_MAJOR_VERSION, 4);
    SDL_GL_SetAttribute(SDL_GL_CONTEXT_MINOR_VERSION, 6);
    SDL_GL_SetAttribute(SDL_GL_CONTEXT_PROFILE_MASK, SDL_GL_CONTEXT_PROFILE_CORE);
    SDL_GL_SetAttribute(SDL_GL_DOUBLEBUFFER, 1);

    m_glContext = SDL_GL_CreateContext(m_window.getSDLWindow());
    if(m_glContext == nullptr)
    {
        std::cerr << "Failed to create OpenGL 4.6 context: " << SDL_GetError() << "\n";
        SDL_GL_SetAttribute(SDL_GL_CONTEXT_MAJOR_VERSION, 3);
        SDL_GL_SetAttribute(SDL_GL_CONTEXT_MINOR_VERSION, 3);
        SDL_GL_SetAttribute(SDL_GL_CONTEXT_PROFILE_MASK, SDL_GL_CONTEXT_PROFILE_CORE);
        m_glContext = SDL_GL_CreateContext(m_window.getSDLWindow());
    }

    if(m_glContext == nullptr)
    {
        std::cerr << "Failed to create OpenGL context: " << SDL_GetError() << "\n";
        return false;
    }

    if(SDL_GL_MakeCurrent(m_window.getSDLWindow(), m_glContext) != 0)
    {
        std::cerr << "Failed to make OpenGL context current: " << SDL_GetError() << "\n";
        return false;
    }

    return true;
}

bool OpenGLRenderer::loadGLFunctions()
{
    s_glClearColor = reinterpret_cast<PFN_glClearColor>(SDL_GL_GetProcAddress("glClearColor"));
    s_glClear = reinterpret_cast<PFN_glClear>(SDL_GL_GetProcAddress("glClear"));
    s_glGetString = reinterpret_cast<PFN_glGetString>(SDL_GL_GetProcAddress("glGetString"));
    s_glGetIntegerv = reinterpret_cast<PFN_glGetIntegerv>(SDL_GL_GetProcAddress("glGetIntegerv"));
    s_glEnable = reinterpret_cast<PFN_glEnable>(SDL_GL_GetProcAddress("glEnable"));
    s_glDisable = reinterpret_cast<PFN_glDisable>(SDL_GL_GetProcAddress("glDisable"));
    s_glDebugMessageCallback = reinterpret_cast<PFN_glDebugMessageCallback>(SDL_GL_GetProcAddress("glDebugMessageCallback"));
    s_glCreateShader = reinterpret_cast<PFN_glCreateShader>(SDL_GL_GetProcAddress("glCreateShader"));
    s_glShaderSource = reinterpret_cast<PFN_glShaderSource>(SDL_GL_GetProcAddress("glShaderSource"));
    s_glCompileShader = reinterpret_cast<PFN_glCompileShader>(SDL_GL_GetProcAddress("glCompileShader"));
    s_glGetShaderiv = reinterpret_cast<PFN_glGetShaderiv>(SDL_GL_GetProcAddress("glGetShaderiv"));
    s_glGetShaderInfoLog = reinterpret_cast<PFN_glGetShaderInfoLog>(SDL_GL_GetProcAddress("glGetShaderInfoLog"));
    s_glCreateProgram = reinterpret_cast<PFN_glCreateProgram>(SDL_GL_GetProcAddress("glCreateProgram"));
    s_glAttachShader = reinterpret_cast<PFN_glAttachShader>(SDL_GL_GetProcAddress("glAttachShader"));
    s_glLinkProgram = reinterpret_cast<PFN_glLinkProgram>(SDL_GL_GetProcAddress("glLinkProgram"));
    s_glGetProgramiv = reinterpret_cast<PFN_glGetProgramiv>(SDL_GL_GetProcAddress("glGetProgramiv"));
    s_glGetProgramInfoLog = reinterpret_cast<PFN_glGetProgramInfoLog>(SDL_GL_GetProcAddress("glGetProgramInfoLog"));
    s_glDeleteShader = reinterpret_cast<PFN_glDeleteShader>(SDL_GL_GetProcAddress("glDeleteShader"));
    s_glDeleteProgram = reinterpret_cast<PFN_glDeleteProgram>(SDL_GL_GetProcAddress("glDeleteProgram"));
    s_glUseProgram = reinterpret_cast<PFN_glUseProgram>(SDL_GL_GetProcAddress("glUseProgram"));
    s_glGetUniformLocation = reinterpret_cast<PFN_glGetUniformLocation>(SDL_GL_GetProcAddress("glGetUniformLocation"));
    s_glUniformMatrix4fv = reinterpret_cast<PFN_glUniformMatrix4fv>(SDL_GL_GetProcAddress("glUniformMatrix4fv"));
    s_glGenVertexArrays = reinterpret_cast<PFN_glGenVertexArrays>(SDL_GL_GetProcAddress("glGenVertexArrays"));
    s_glBindVertexArray = reinterpret_cast<PFN_glBindVertexArray>(SDL_GL_GetProcAddress("glBindVertexArray"));
    s_glDeleteVertexArrays = reinterpret_cast<PFN_glDeleteVertexArrays>(SDL_GL_GetProcAddress("glDeleteVertexArrays"));
    s_glGenBuffers = reinterpret_cast<PFN_glGenBuffers>(SDL_GL_GetProcAddress("glGenBuffers"));
    s_glBindBuffer = reinterpret_cast<PFN_glBindBuffer>(SDL_GL_GetProcAddress("glBindBuffer"));
    s_glBufferData = reinterpret_cast<PFN_glBufferData>(SDL_GL_GetProcAddress("glBufferData"));
    s_glDeleteBuffers = reinterpret_cast<PFN_glDeleteBuffers>(SDL_GL_GetProcAddress("glDeleteBuffers"));
    s_glEnableVertexAttribArray = reinterpret_cast<PFN_glEnableVertexAttribArray>(SDL_GL_GetProcAddress("glEnableVertexAttribArray"));
    s_glVertexAttribPointer = reinterpret_cast<PFN_glVertexAttribPointer>(SDL_GL_GetProcAddress("glVertexAttribPointer"));
    s_glUniform1i = reinterpret_cast<PFN_glUniform1i>(SDL_GL_GetProcAddress("glUniform1i"));
    s_glUniform1f = reinterpret_cast<PFN_glUniform1f>(SDL_GL_GetProcAddress("glUniform1f"));
    s_glUniform3f = reinterpret_cast<PFN_glUniform3f>(SDL_GL_GetProcAddress("glUniform3f"));
    s_glDrawElements = reinterpret_cast<PFN_glDrawElements>(SDL_GL_GetProcAddress("glDrawElements"));
    s_glGenTextures = reinterpret_cast<PFN_glGenTextures>(SDL_GL_GetProcAddress("glGenTextures"));
    s_glDeleteTextures = reinterpret_cast<PFN_glDeleteTextures>(SDL_GL_GetProcAddress("glDeleteTextures"));
    s_glBindTexture = reinterpret_cast<PFN_glBindTexture>(SDL_GL_GetProcAddress("glBindTexture"));
    s_glTexParameteri = reinterpret_cast<PFN_glTexParameteri>(SDL_GL_GetProcAddress("glTexParameteri"));
    s_glTexImage2D = reinterpret_cast<PFN_glTexImage2D>(SDL_GL_GetProcAddress("glTexImage2D"));
    s_glActiveTexture = reinterpret_cast<PFN_glActiveTexture>(SDL_GL_GetProcAddress("glActiveTexture"));

    return s_glClearColor != nullptr && s_glClear != nullptr && s_glGetString != nullptr && s_glGetIntegerv != nullptr &&
           s_glEnable != nullptr && s_glCreateShader != nullptr && s_glShaderSource != nullptr && s_glCompileShader != nullptr &&
           s_glGetShaderiv != nullptr && s_glCreateProgram != nullptr && s_glAttachShader != nullptr && s_glLinkProgram != nullptr &&
           s_glGetProgramiv != nullptr && s_glDeleteShader != nullptr && s_glDeleteProgram != nullptr && s_glUseProgram != nullptr &&
           s_glGetUniformLocation != nullptr && s_glUniformMatrix4fv != nullptr && s_glGenVertexArrays != nullptr &&
           s_glBindVertexArray != nullptr && s_glGenBuffers != nullptr && s_glBindBuffer != nullptr && s_glBufferData != nullptr &&
           s_glDeleteBuffers != nullptr && s_glEnableVertexAttribArray != nullptr && s_glVertexAttribPointer != nullptr &&
           s_glUniform1i != nullptr && s_glUniform1f != nullptr && s_glUniform3f != nullptr && s_glDrawElements != nullptr;
}

void OpenGLRenderer::applyVSync()
{
    if(SDL_GL_SetSwapInterval(m_vsyncEnabled ? 1 : 0) != 0)
    {
        std::cerr << "Failed to set swap interval: " << SDL_GetError() << "\n";
    }
}

void OpenGLRenderer::setViewProjection(const glm::mat4& view, const glm::mat4& projection)
{
    m_viewMatrix = view;
    m_projectionMatrix = projection;
    m_viewProjection = projection * view;
    updateProjection();
}

Renderer::BufferHandle OpenGLRenderer::createVertexBuffer(const void* data, std::size_t size)
{
    if(s_glGenBuffers == nullptr || s_glBindBuffer == nullptr || s_glBufferData == nullptr || s_glGenVertexArrays == nullptr || s_glBindVertexArray == nullptr || s_glEnableVertexAttribArray == nullptr || s_glVertexAttribPointer == nullptr)
    {
        return 0;
    }

    unsigned int vbo = 0;
    s_glGenBuffers(1, &vbo);
    if(vbo == 0)
    {
        return 0;
    }

    unsigned int vao = 0;
    s_glGenVertexArrays(1, &vao);
    if(vao == 0)
    {
        s_glDeleteBuffers(1, &vbo);
        return 0;
    }

    s_glBindVertexArray(vao);

    s_glBindBuffer(GL_ARRAY_BUFFER_CONST, vbo);
    s_glBufferData(GL_ARRAY_BUFFER_CONST, static_cast<std::ptrdiff_t>(size), data, GL_STATIC_DRAW_CONST);

    const int stride = sizeof(poorcraft::world::ChunkVertex);
    s_glEnableVertexAttribArray(0);
    s_glVertexAttribPointer(0, 3, GL_FLOAT_CONST, false, stride, reinterpret_cast<const void*>(offsetof(poorcraft::world::ChunkVertex, position)));
    s_glEnableVertexAttribArray(1);
    s_glVertexAttribPointer(1, 3, GL_FLOAT_CONST, false, stride, reinterpret_cast<const void*>(offsetof(poorcraft::world::ChunkVertex, normal)));
    s_glEnableVertexAttribArray(2);
    s_glVertexAttribPointer(2, 2, GL_FLOAT_CONST, false, stride, reinterpret_cast<const void*>(offsetof(poorcraft::world::ChunkVertex, texCoord)));
    s_glEnableVertexAttribArray(3);
    s_glVertexAttribPointer(3, 1, GL_FLOAT_CONST, false, stride, reinterpret_cast<const void*>(offsetof(poorcraft::world::ChunkVertex, ao)));

    s_glBindBuffer(GL_ARRAY_BUFFER_CONST, 0);
    s_glBindVertexArray(0);

    BufferResource resource;
    resource.buffer = vbo;
    resource.size = size;
    resource.target = GL_ARRAY_BUFFER_CONST;
    resource.vao = vao;

    const BufferHandle handle = m_nextBufferHandle++;
    m_vertexBuffers.emplace(handle, resource);
    return handle;
}

Renderer::BufferHandle OpenGLRenderer::createIndexBuffer(const void* data, std::size_t size)
{
    if(s_glGenBuffers == nullptr || s_glBindBuffer == nullptr || s_glBufferData == nullptr)
    {
        return 0;
    }

    unsigned int ibo = 0;
    s_glGenBuffers(1, &ibo);
    if(ibo == 0)
    {
        return 0;
    }

    s_glBindBuffer(GL_ELEMENT_ARRAY_BUFFER_CONST, ibo);
    s_glBufferData(GL_ELEMENT_ARRAY_BUFFER_CONST, static_cast<std::ptrdiff_t>(size), data, GL_STATIC_DRAW_CONST);
    s_glBindBuffer(GL_ELEMENT_ARRAY_BUFFER_CONST, 0);

    BufferResource resource;
    resource.buffer = ibo;
    resource.size = size;
    resource.target = GL_ELEMENT_ARRAY_BUFFER_CONST;

    const BufferHandle handle = m_nextBufferHandle++;
    m_indexBuffers.emplace(handle, resource);
    return handle;
}

void OpenGLRenderer::destroyBuffer(BufferHandle handle)
{
    if(auto it = m_vertexBuffers.find(handle); it != m_vertexBuffers.end())
    {
        if(it->second.buffer != 0 && s_glDeleteBuffers != nullptr)
        {
            s_glDeleteBuffers(1, &it->second.buffer);
        }
        if(it->second.vao != 0 && s_glDeleteVertexArrays != nullptr)
        {
            s_glDeleteVertexArrays(1, &it->second.vao);
        }
        m_vertexBuffers.erase(it);
        return;
    }

    if(auto it = m_indexBuffers.find(handle); it != m_indexBuffers.end())
    {
        if(it->second.buffer != 0 && s_glDeleteBuffers != nullptr)
        {
            s_glDeleteBuffers(1, &it->second.buffer);
        }
        m_indexBuffers.erase(it);
    }
}

void OpenGLRenderer::drawIndexed(BufferHandle vertexBuffer, BufferHandle indexBuffer, std::uint32_t indexCount, const glm::mat4& modelMatrix)
{
    if(indexCount == 0)
    {
        return;
    }

    m_drawCommands.push_back({vertexBuffer, indexBuffer, indexCount, modelMatrix});
}

bool OpenGLRenderer::createShaderProgram()
{
    if(s_glCreateShader == nullptr || s_glShaderSource == nullptr || s_glCompileShader == nullptr || s_glGetShaderiv == nullptr ||
       s_glGetShaderInfoLog == nullptr || s_glCreateProgram == nullptr || s_glAttachShader == nullptr || s_glLinkProgram == nullptr ||
       s_glGetProgramiv == nullptr || s_glGetProgramInfoLog == nullptr || s_glDeleteShader == nullptr || s_glUseProgram == nullptr ||
       s_glGetUniformLocation == nullptr || s_glUniform1i == nullptr)
    {
        return false;
    }

    constexpr const char* kVertexSource = R"GLSL(
        #version 330 core
        layout(location = 0) in vec3 aPosition;
        layout(location = 1) in vec3 aNormal;
        layout(location = 2) in vec2 aTexCoord;
        layout(location = 3) in float aAO;

        uniform mat4 uViewProjection;
        uniform mat4 uModel;

        out vec3 vNormal;
        out vec2 vTexCoord;
        out float vAO;

        void main()
        {
            mat3 normalMatrix = mat3(transpose(inverse(uModel)));
            vNormal = normalize(normalMatrix * aNormal);
            vTexCoord = aTexCoord;
            vAO = aAO;
            gl_Position = uViewProjection * uModel * vec4(aPosition, 1.0);
        }
    )GLSL";

    constexpr const char* kFragmentSource = R"GLSL(
        #version 330 core
        in vec3 vNormal;
        in vec2 vTexCoord;
        in float vAO;

        out vec4 FragColor;

        uniform sampler2D uTexture;
        uniform vec3 uSunDirection;
        uniform vec3 uSunColor;
        uniform float uSunIntensity;
        uniform vec3 uAmbientColor;
        uniform float uAmbientIntensity;

        void main()
        {
            vec4 texSample = texture(uTexture, vTexCoord);
            vec3 normal = normalize(vNormal);
            float diffuse = max(dot(normal, -uSunDirection), 0.0);
            vec3 lighting = uAmbientColor * uAmbientIntensity + uSunColor * uSunIntensity * diffuse;
            float ao = clamp(vAO, 0.0, 1.0);
            vec3 color = texSample.rgb * lighting * ao;
            FragColor = vec4(color, texSample.a);
        }
    )GLSL";

    unsigned int vertexShader = s_glCreateShader(GL_VERTEX_SHADER);
    s_glShaderSource(vertexShader, 1, &kVertexSource, nullptr);
    s_glCompileShader(vertexShader);

    int success = 0;
    s_glGetShaderiv(vertexShader, GL_COMPILE_STATUS_CONST, &success);
    if(success == 0)
    {
        int logLength = 0;
        s_glGetShaderiv(vertexShader, GL_INFO_LOG_LENGTH_CONST, &logLength);
        std::string log(static_cast<size_t>(logLength), '\0');
        s_glGetShaderInfoLog(vertexShader, logLength, nullptr, log.data());
        std::cerr << "OpenGL vertex shader compilation failed: " << log << '\n';
        s_glDeleteShader(vertexShader);
        return false;
    }

    unsigned int fragmentShader = s_glCreateShader(GL_FRAGMENT_SHADER);
    s_glShaderSource(fragmentShader, 1, &kFragmentSource, nullptr);
    s_glCompileShader(fragmentShader);
    s_glGetShaderiv(fragmentShader, GL_COMPILE_STATUS_CONST, &success);
    if(success == 0)
    {
        int logLength = 0;
        s_glGetShaderiv(fragmentShader, GL_INFO_LOG_LENGTH_CONST, &logLength);
        std::string log(static_cast<size_t>(logLength), '\0');
        s_glGetShaderInfoLog(fragmentShader, logLength, nullptr, log.data());
        std::cerr << "OpenGL fragment shader compilation failed: " << log << '\n';
        s_glDeleteShader(vertexShader);
        s_glDeleteShader(fragmentShader);
        return false;
    }

    unsigned int program = s_glCreateProgram();
    s_glAttachShader(program, vertexShader);
    s_glAttachShader(program, fragmentShader);
    s_glLinkProgram(program);
    s_glGetProgramiv(program, GL_LINK_STATUS_CONST, &success);
    s_glDeleteShader(vertexShader);
    s_glDeleteShader(fragmentShader);

    if(success == 0)
    {
        int logLength = 0;
        s_glGetProgramiv(program, GL_INFO_LOG_LENGTH_CONST, &logLength);
        std::string log(static_cast<size_t>(logLength), '\0');
        s_glGetProgramInfoLog(program, logLength, nullptr, log.data());
        std::cerr << "OpenGL shader program link failed: " << log << '\n';
        s_glDeleteProgram(program);
        return false;
    }

    m_shaderProgram = program;
    m_viewProjLocation = s_glGetUniformLocation(program, "uViewProjection");
    m_modelLocation = s_glGetUniformLocation(program, "uModel");
    m_textureLocation = s_glGetUniformLocation(program, "uTexture");
    m_sunDirLocation = s_glGetUniformLocation(program, "uSunDirection");
    m_sunColorLocation = s_glGetUniformLocation(program, "uSunColor");
    m_sunIntensityLocation = s_glGetUniformLocation(program, "uSunIntensity");
    m_ambientColorLocation = s_glGetUniformLocation(program, "uAmbientColor");
    m_ambientIntensityLocation = s_glGetUniformLocation(program, "uAmbientIntensity");

    s_glUseProgram(m_shaderProgram);
    if(m_textureLocation >= 0)
    {
        s_glUniform1i(m_textureLocation, 0);
    }
    updateProjection();
    applyLightingUniforms();

    return true;
}

void OpenGLRenderer::destroyShaderProgram()
{
    if(m_shaderProgram != 0 && s_glDeleteProgram != nullptr)
    {
        s_glDeleteProgram(m_shaderProgram);
        m_shaderProgram = 0;
    }
}

void OpenGLRenderer::updateProjection()
{
    if(m_shaderProgram == 0 || s_glUniformMatrix4fv == nullptr || s_glUseProgram == nullptr)
    {
        return;
    }

    s_glUseProgram(m_shaderProgram);
    s_glUniformMatrix4fv(m_viewProjLocation, 1, false, glm::value_ptr(m_viewProjection));
}

void OpenGLRenderer::applyLightingUniforms()
{
    if(m_shaderProgram == 0 || !m_lightingDirty)
    {
        return;
    }

    if(s_glUseProgram == nullptr || s_glUniform3f == nullptr || s_glUniform1f == nullptr || s_glUniform1i == nullptr)
    {
        return;
    }

    s_glUseProgram(m_shaderProgram);

    if(m_sunDirLocation >= 0)
    {
        s_glUniform3f(m_sunDirLocation, m_lightingParams.sunDirAndIntensity.x, m_lightingParams.sunDirAndIntensity.y, m_lightingParams.sunDirAndIntensity.z);
    }
    if(m_sunColorLocation >= 0)
    {
        s_glUniform3f(m_sunColorLocation, m_lightingParams.sunColor.x, m_lightingParams.sunColor.y, m_lightingParams.sunColor.z);
    }
    if(m_sunIntensityLocation >= 0)
    {
        s_glUniform1f(m_sunIntensityLocation, m_lightingParams.sunDirAndIntensity.w);
    }
    if(m_ambientColorLocation >= 0)
    {
        s_glUniform3f(m_ambientColorLocation, m_lightingParams.ambientColorAndIntensity.x, m_lightingParams.ambientColorAndIntensity.y, m_lightingParams.ambientColorAndIntensity.z);
    }
    if(m_ambientIntensityLocation >= 0)
    {
        s_glUniform1f(m_ambientIntensityLocation, m_lightingParams.ambientColorAndIntensity.w);
    }
    if(m_textureLocation >= 0)
    {
        s_glUniform1i(m_textureLocation, 0);
    }

    m_lightingDirty = false;
}

Renderer::TextureHandle OpenGLRenderer::createDefaultTexture()
{
    constexpr std::uint32_t kWhitePixel = 0xFFFFFFFF;
    return createTexture(&kWhitePixel, 1, 1, 4);
}
} // namespace poorcraft::rendering

#endif // POORCRAFT_OPENGL_ENABLED
