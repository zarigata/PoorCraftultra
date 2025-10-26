#include "poorcraft/rendering/VulkanRenderer.h"

#ifdef POORCRAFT_VULKAN_ENABLED

#    include "poorcraft/core/GPUInfo.h"
#    include "poorcraft/core/Window.h"
#    include "poorcraft/world/ChunkMesh.h"

#    include <algorithm>
#    include <array>
#    include <chrono>
#    include <thread>
#    include <cstdint>
#    include <cstring>
#    include <iostream>
#    include <limits>
#    include <optional>
#    include <set>
#    include <stdexcept>
#    include <string>
#    include <tuple>
#    include <utility>

#    include <imgui.h>
#    include <backends/imgui_impl_sdl2.h>
#    include <backends/imgui_impl_vulkan.h>

namespace poorcraft::rendering
{
namespace
{
constexpr uint32_t kMaxFramesInFlight = 2;

const std::vector<const char*> VALIDATION_LAYERS = {
#    ifdef NDEBUG
    // no validation layers in release builds
#    else
    "VK_LAYER_KHRONOS_validation"
#    endif
};

#ifdef NDEBUG
constexpr bool kEnableValidationLayers = false;
#else
constexpr bool kEnableValidationLayers = true;
#endif

struct QueueFamilyIndices
{
    std::optional<uint32_t> graphicsFamily{};
    std::optional<uint32_t> presentFamily{};

    bool isComplete() const
    {
        return graphicsFamily.has_value() && presentFamily.has_value();
    }
};

struct SwapchainSupportDetails
{
    VkSurfaceCapabilitiesKHR capabilities{};
    std::vector<VkSurfaceFormatKHR> formats;
    std::vector<VkPresentModeKHR> presentModes;
};

VKAPI_ATTR VkBool32 VKAPI_CALL debugCallback(
    VkDebugUtilsMessageSeverityFlagBitsEXT messageSeverity,
    VkDebugUtilsMessageTypeFlagsEXT /*messageType*/,
    const VkDebugUtilsMessengerCallbackDataEXT* pCallbackData,
    void* /*pUserData*/)
{
    if(messageSeverity >= VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT)
    {
        std::cerr << "[Vulkan] " << pCallbackData->pMessage << '\n';
    }
    return VK_FALSE;
}

bool checkValidationLayerSupport()
{
    uint32_t layerCount = 0;
    vkEnumerateInstanceLayerProperties(&layerCount, nullptr);
    std::vector<VkLayerProperties> availableLayers(layerCount);
    vkEnumerateInstanceLayerProperties(&layerCount, availableLayers.data());

    for(const char* layerName : VALIDATION_LAYERS)
    {
        bool found = false;
        for(const auto& layerProperties : availableLayers)
        {
            if(std::strcmp(layerName, layerProperties.layerName) == 0)
            {
                found = true;
                break;
            }
        }

        if(!found)
        {
            return false;
        }
    }
    return true;
}

bool VulkanRenderer::recreateImGuiBackend()
{
    if(!m_imguiInitialized)
    {
        return false;
    }

    ImGui_ImplVulkan_Shutdown();

    const auto queues = findQueueFamilies(m_physicalDevice, m_surface);
    if(!queues.graphicsFamily.has_value())
    {
        std::cerr << "Failed to locate graphics queue family for ImGui backend recreation\n";
        return false;
    }

    ImGui_ImplVulkan_InitInfo initInfo{};
    initInfo.Instance = m_instance;
    initInfo.PhysicalDevice = m_physicalDevice;
    initInfo.Device = m_device;
    initInfo.QueueFamily = queues.graphicsFamily.value();
    initInfo.Queue = m_graphicsQueue;
    initInfo.DescriptorPool = m_imguiDescriptorPool;
    initInfo.MinImageCount = static_cast<uint32_t>(m_swapchainImages.size());
    initInfo.ImageCount = static_cast<uint32_t>(m_swapchainImages.size());
    initInfo.MSAASamples = VK_SAMPLE_COUNT_1_BIT;
    initInfo.CheckVkResultFn = nullptr;

    if(!ImGui_ImplVulkan_Init(&initInfo, m_renderPass))
    {
        std::cerr << "ImGui_ImplVulkan_Init failed during backend recreation\n";
        return false;
    }

    if(!uploadImGuiFonts())
    {
        std::cerr << "Failed to upload ImGui fonts during backend recreation\n";
        return false;
    }

    m_uiRenderPending = false;
    m_pendingUiDrawData = nullptr;
    return true;
}

QueueFamilyIndices findQueueFamilies(VkPhysicalDevice device, VkSurfaceKHR surface)
{
    QueueFamilyIndices indices;

    uint32_t queueFamilyCount = 0;
    vkGetPhysicalDeviceQueueFamilyProperties(device, &queueFamilyCount, nullptr);

    std::vector<VkQueueFamilyProperties> queueFamilies(queueFamilyCount);
    vkGetPhysicalDeviceQueueFamilyProperties(device, &queueFamilyCount, queueFamilies.data());

    for(uint32_t i = 0; i < queueFamilyCount; ++i)
    {
        const auto& queueFamily = queueFamilies[i];
        if(queueFamily.queueFlags & VK_QUEUE_GRAPHICS_BIT)
        {
            indices.graphicsFamily = i;
        }

        VkBool32 presentSupport = VK_FALSE;
        vkGetPhysicalDeviceSurfaceSupportKHR(device, i, surface, &presentSupport);

        if(presentSupport)
        {
            indices.presentFamily = i;
        }

        if(indices.isComplete())
        {
            break;
        }
    }

    return indices;
}

bool checkDeviceExtensionSupport(VkPhysicalDevice device)
{
    uint32_t extensionCount = 0;
    vkEnumerateDeviceExtensionProperties(device, nullptr, &extensionCount, nullptr);

    std::vector<VkExtensionProperties> availableExtensions(extensionCount);
    vkEnumerateDeviceExtensionProperties(device, nullptr, &extensionCount, availableExtensions.data());

    std::set<std::string> requiredExtensions = {VK_KHR_SWAPCHAIN_EXTENSION_NAME};

    for(const auto& extension : availableExtensions)
    {
        requiredExtensions.erase(extension.extensionName);
    }

    return requiredExtensions.empty();
}

SwapchainSupportDetails querySwapchainSupport(VkPhysicalDevice device, VkSurfaceKHR surface)
{
    SwapchainSupportDetails details;
    vkGetPhysicalDeviceSurfaceCapabilitiesKHR(device, surface, &details.capabilities);

    uint32_t formatCount = 0;
    vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface, &formatCount, nullptr);
    if(formatCount != 0)
    {
        details.formats.resize(formatCount);
        vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface, &formatCount, details.formats.data());
    }

    uint32_t presentModeCount = 0;
    vkGetPhysicalDeviceSurfacePresentModesKHR(device, surface, &presentModeCount, nullptr);
    if(presentModeCount != 0)
    {
        details.presentModes.resize(presentModeCount);
        vkGetPhysicalDeviceSurfacePresentModesKHR(device, surface, &presentModeCount, details.presentModes.data());
    }

    return details;
}

VkSurfaceFormatKHR chooseSwapSurfaceFormat(const std::vector<VkSurfaceFormatKHR>& availableFormats)
{
    for(const auto& availableFormat : availableFormats)
    {
        if(availableFormat.format == VK_FORMAT_B8G8R8A8_SRGB &&
           availableFormat.colorSpace == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR)
        {
            return availableFormat;
        }
    }

    return availableFormats.front();
}

VkPresentModeKHR chooseSwapPresentMode(const std::vector<VkPresentModeKHR>& availablePresentModes, bool vsyncEnabled)
{
    if(vsyncEnabled)
    {
        if(std::find(availablePresentModes.begin(), availablePresentModes.end(), VK_PRESENT_MODE_MAILBOX_KHR) !=
           availablePresentModes.end())
        {
            return VK_PRESENT_MODE_MAILBOX_KHR;
        }
        return VK_PRESENT_MODE_FIFO_KHR;
    }

    if(std::find(availablePresentModes.begin(), availablePresentModes.end(), VK_PRESENT_MODE_IMMEDIATE_KHR) !=
       availablePresentModes.end())
    {
        return VK_PRESENT_MODE_IMMEDIATE_KHR;
    }

    return VK_PRESENT_MODE_FIFO_KHR;
}

VkExtent2D chooseSwapExtent(const VkSurfaceCapabilitiesKHR& capabilities, int width, int height)
{
    if(capabilities.currentExtent.width != std::numeric_limits<uint32_t>::max())
    {
        return capabilities.currentExtent;
    }

    VkExtent2D actualExtent{static_cast<uint32_t>(width), static_cast<uint32_t>(height)};

    actualExtent.width =
        std::max(capabilities.minImageExtent.width, std::min(capabilities.maxImageExtent.width, actualExtent.width));
    actualExtent.height = std::max(
        capabilities.minImageExtent.height,
        std::min(capabilities.maxImageExtent.height, actualExtent.height));

    return actualExtent;
}

VkResult createDebugUtilsMessengerEXT(
    VkInstance instance,
    const VkDebugUtilsMessengerCreateInfoEXT* pCreateInfo,
    const VkAllocationCallbacks* pAllocator,
    VkDebugUtilsMessengerEXT* pDebugMessenger)
{
    const auto func = reinterpret_cast<PFN_vkCreateDebugUtilsMessengerEXT>(
        vkGetInstanceProcAddr(instance, "vkCreateDebugUtilsMessengerEXT"));
    if(func != nullptr)
    {
        return func(instance, pCreateInfo, pAllocator, pDebugMessenger);
    }
    return VK_ERROR_EXTENSION_NOT_PRESENT;
}

void destroyDebugUtilsMessengerEXT(VkInstance instance, VkDebugUtilsMessengerEXT debugMessenger)
{
    const auto func = reinterpret_cast<PFN_vkDestroyDebugUtilsMessengerEXT>(
        vkGetInstanceProcAddr(instance, "vkDestroyDebugUtilsMessengerEXT"));
    if(func != nullptr)
    {
        func(instance, debugMessenger, nullptr);
    }
}

void populateDebugMessengerCreateInfo(VkDebugUtilsMessengerCreateInfoEXT& createInfo)
{
    createInfo = {};
    createInfo.sType = VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT;
    createInfo.messageSeverity = VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT |
                                 VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT;
    createInfo.messageType = VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT |
                             VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT |
                             VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT;
    createInfo.pfnUserCallback = debugCallback;
}
} // namespace

VulkanRenderer::VulkanRenderer(core::Window& window)
    : m_window(window)
{}

bool VulkanRenderer::initialize()
{
    if(!createInstance())
    {
        std::cerr << "Failed to create Vulkan instance\n";
        return false;
    }

    if(!createSurface())
    {
        std::cerr << "Failed to create Vulkan surface\n";
        return false;
    }

    if(!selectPhysicalDevice())
    {
        std::cerr << "Failed to select Vulkan physical device\n";
        return false;
    }

    if(!createLogicalDevice())
    {
        std::cerr << "Failed to create Vulkan logical device\n";
        return false;
    }

    if(!createSwapchain())
    {
        std::cerr << "Failed to create Vulkan swapchain\n";
        return false;
    }

    if(!createRenderPass())
    {
        std::cerr << "Failed to create Vulkan render pass\n";
        return false;
    }

    if(!createDescriptorSetLayout())
    {
        std::cerr << "Failed to create descriptor set layout\n";
        return false;
    }

    if(!createDescriptorPool())
    {
        std::cerr << "Failed to create descriptor pool\n";
        return false;
    }

    if(!createLightingUniformBuffer())
    {
        std::cerr << "Failed to create lighting uniform buffer\n";
        return false;
    }

    if(!createPipeline())
    {
        std::cerr << "Failed to create Vulkan graphics pipeline\n";
        return false;
    }

    if(!createDescriptorSets())
    {
        std::cerr << "Failed to create descriptor sets\n";
        return false;
    }

    if(!createDepthResources())
    {
        std::cerr << "Failed to create depth resources\n";
        return false;
    }

    if(!createFramebuffers())
    {
        std::cerr << "Failed to create swapchain framebuffers\n";
        return false;
    }

    if(!createCommandPool())
    {
        std::cerr << "Failed to create Vulkan command pool\n";
        return false;
    }

    if(!allocateCommandBuffers())
    {
        std::cerr << "Failed to allocate Vulkan command buffers\n";
        return false;
    }

    if(!createSyncObjects())
    {
        std::cerr << "Failed to create Vulkan sync objects\n";
        return false;
    }

    if(!createImGuiDescriptorPool())
    {
        std::cerr << "Failed to create ImGui descriptor pool\n";
        return false;
    }

    if(!initializeUI())
    {
        std::cerr << "Failed to initialize ImGui backend\n";
        return false;
    }

    return true;
}

void VulkanRenderer::shutdown()
{
    if(m_device == VK_NULL_HANDLE)
    {
        return;
    }

    vkDeviceWaitIdle(m_device);

    shutdownUI();
    destroyImGuiDescriptorPool();

    for(size_t i = 0; i < m_imageAvailableSemaphores.size(); ++i)
    {
        vkDestroySemaphore(m_device, m_imageAvailableSemaphores[i], nullptr);
        vkDestroySemaphore(m_device, m_renderFinishedSemaphores[i], nullptr);
        vkDestroyFence(m_device, m_inFlightFences[i], nullptr);
    }

    for(auto& [handle, resource] : m_textures)
    {
        if(resource.sampler != VK_NULL_HANDLE)
        {
            vkDestroySampler(m_device, resource.sampler, nullptr);
        }
        if(resource.imageView != VK_NULL_HANDLE)
        {
            vkDestroyImageView(m_device, resource.imageView, nullptr);
        }
        if(resource.image != VK_NULL_HANDLE)
        {
            vkDestroyImage(m_device, resource.image, nullptr);
        }
        if(resource.memory != VK_NULL_HANDLE)
        {
            vkFreeMemory(m_device, resource.memory, nullptr);
        }
    }
    m_textures.clear();

    if(m_lightingUniformMapped != nullptr)
    {
        vkUnmapMemory(m_device, m_lightingUniformMemory);
        m_lightingUniformMapped = nullptr;
    }
    if(m_lightingUniformBuffer != VK_NULL_HANDLE)
    {
        vkDestroyBuffer(m_device, m_lightingUniformBuffer, nullptr);
        m_lightingUniformBuffer = VK_NULL_HANDLE;
    }
    if(m_lightingUniformMemory != VK_NULL_HANDLE)
    {
        vkFreeMemory(m_device, m_lightingUniformMemory, nullptr);
        m_lightingUniformMemory = VK_NULL_HANDLE;
    }

    if(m_descriptorPool != VK_NULL_HANDLE)
    {
        vkDestroyDescriptorPool(m_device, m_descriptorPool, nullptr);
        m_descriptorPool = VK_NULL_HANDLE;
    }

    if(m_descriptorSetLayout != VK_NULL_HANDLE)
    {
        vkDestroyDescriptorSetLayout(m_device, m_descriptorSetLayout, nullptr);
        m_descriptorSetLayout = VK_NULL_HANDLE;
    }

    destroyFramebuffers();
    destroyDepthResources();
    destroyPipeline();
    destroyRenderPass();
    destroySwapchain();

    if(m_commandPool != VK_NULL_HANDLE)
    {
        vkDestroyCommandPool(m_device, m_commandPool, nullptr);
        m_commandPool = VK_NULL_HANDLE;
    }

    if(m_device != VK_NULL_HANDLE)
    {
        vkDestroyDevice(m_device, nullptr);
        m_device = VK_NULL_HANDLE;
    }

    if(m_surface != VK_NULL_HANDLE)
    {
        vkDestroySurfaceKHR(m_instance, m_surface, nullptr);
        m_surface = VK_NULL_HANDLE;
    }

    if(m_instance != VK_NULL_HANDLE)
    {
        destroyDebugUtilsMessengerEXT(m_instance, m_debugMessenger);
        vkDestroyInstance(m_instance, nullptr);
        m_instance = VK_NULL_HANDLE;
    }
}

void VulkanRenderer::beginFrame()
{
    if(m_swapchain == VK_NULL_HANDLE)
    {
        if(!recreateSwapchain())
        {
            return;
        }
        if(m_swapchain == VK_NULL_HANDLE)
        {
            return;
        }
    }

    vkWaitForFences(m_device, 1, &m_inFlightFences[m_currentFrame], VK_TRUE, UINT64_MAX);

    uint32_t imageIndex = 0;
    const VkResult acquireResult = vkAcquireNextImageKHR(
        m_device,
        m_swapchain,
        UINT64_MAX,
        m_imageAvailableSemaphores[m_currentFrame],
        VK_NULL_HANDLE,
        &imageIndex);

    if(acquireResult == VK_ERROR_OUT_OF_DATE_KHR || acquireResult == VK_SUBOPTIMAL_KHR)
    {
        recreateSwapchain();
        return;
    }

    if(acquireResult != VK_SUCCESS)
    {
        std::cerr << "Failed to acquire Vulkan swapchain image\n";
        return;
    }

    vkResetFences(m_device, 1, &m_inFlightFences[m_currentFrame]);

    VkCommandBuffer commandBuffer = m_commandBuffers[m_currentFrame];

    vkResetCommandBuffer(commandBuffer, 0);

    VkCommandBufferBeginInfo beginInfo{};
    beginInfo.sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO;
    beginInfo.flags = VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT;

    vkBeginCommandBuffer(commandBuffer, &beginInfo);

    VkRenderPassBeginInfo renderPassInfo{};
    renderPassInfo.sType = VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO;
    renderPassInfo.renderPass = m_renderPass;
    renderPassInfo.framebuffer = m_swapchainFramebuffers[imageIndex];
    renderPassInfo.renderArea.offset = {0, 0};
    renderPassInfo.renderArea.extent = m_swapchainExtent;

    std::array<VkClearValue, 2> clearValues{};
    clearValues[0].color = {{m_clearColor.x, m_clearColor.y, m_clearColor.z, m_clearColor.w}};
    clearValues[1].depthStencil = {1.0f, 0};
    renderPassInfo.clearValueCount = static_cast<uint32_t>(clearValues.size());
    renderPassInfo.pClearValues = clearValues.data();

    vkCmdBeginRenderPass(commandBuffer, &renderPassInfo, VK_SUBPASS_CONTENTS_INLINE);

    vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, m_graphicsPipeline);

    VkViewport viewport{};
    viewport.x = 0.0f;
    viewport.y = 0.0f;
    viewport.width = static_cast<float>(m_swapchainExtent.width);
    viewport.height = static_cast<float>(m_swapchainExtent.height);
    viewport.minDepth = 0.0f;
    viewport.maxDepth = 1.0f;
    vkCmdSetViewport(commandBuffer, 0, 1, &viewport);

    VkRect2D scissor{};
    scissor.offset = {0, 0};
    scissor.extent = m_swapchainExtent;
    vkCmdSetScissor(commandBuffer, 0, 1, &scissor);

    m_currentImageIndex = imageIndex;
}

void VulkanRenderer::clear(float r, float g, float b, float a)
{
    m_clearColor = {r, g, b, a};
}

void VulkanRenderer::endFrame()
{
    if(m_swapchain == VK_NULL_HANDLE || m_currentImageIndex == std::numeric_limits<uint32_t>::max())
    {
        return;
    }

    VkCommandBuffer commandBuffer = m_commandBuffers[m_currentFrame];

    // Bind descriptor set for textures and lighting
    if(m_currentFrame < m_descriptorSets.size() && m_descriptorSets[m_currentFrame] != VK_NULL_HANDLE)
    {
        vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, m_pipelineLayout,
                                0, 1, &m_descriptorSets[m_currentFrame], 0, nullptr);
    }

    for(const auto& command : m_drawCommands)
    {
        auto vertexIt = m_vertexBuffers.find(command.vertexBuffer);
        auto indexIt = m_indexBuffers.find(command.indexBuffer);
        if(vertexIt == m_vertexBuffers.end() || indexIt == m_indexBuffers.end())
        {
            continue;
        }

        VkBuffer vertexBuffers[] = {vertexIt->second.buffer};
        VkDeviceSize offsets[] = {0};
        vkCmdBindVertexBuffers(commandBuffer, 0, 1, vertexBuffers, offsets);
        vkCmdBindIndexBuffer(commandBuffer, indexIt->second.buffer, 0, VK_INDEX_TYPE_UINT32);

        struct PushConstantData
        {
            glm::mat4 view;
            glm::mat4 projection;
            glm::mat4 model;
        } pushData{m_viewMatrix, m_projectionMatrix, command.modelMatrix};

        vkCmdPushConstants(
            commandBuffer,
            m_pipelineLayout,
            VK_SHADER_STAGE_VERTEX_BIT,
            0,
            sizeof(PushConstantData),
            &pushData);

        vkCmdDrawIndexed(commandBuffer, command.indexCount, 1, 0, 0, 0);
    }
    m_drawCommands.clear();

    if(m_uiRenderPending && m_pendingUiDrawData != nullptr)
    {
        ImGui_ImplVulkan_RenderDrawData(m_pendingUiDrawData, commandBuffer);
    }
    m_pendingUiDrawData = nullptr;
    m_uiRenderPending = false;

    vkCmdEndRenderPass(commandBuffer);

    vkEndCommandBuffer(commandBuffer);

    VkSemaphore waitSemaphores[] = {m_imageAvailableSemaphores[m_currentFrame]};
    VkPipelineStageFlags waitStages[] = {VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT};
    VkSemaphore signalSemaphores[] = {m_renderFinishedSemaphores[m_currentFrame]};

    VkSubmitInfo submitInfo{};
    submitInfo.sType = VK_STRUCTURE_TYPE_SUBMIT_INFO;
    submitInfo.waitSemaphoreCount = 1;
    submitInfo.pWaitSemaphores = waitSemaphores;
    submitInfo.pWaitDstStageMask = waitStages;
    submitInfo.commandBufferCount = 1;
    submitInfo.pCommandBuffers = &m_commandBuffers[m_currentFrame];
    submitInfo.signalSemaphoreCount = 1;
    submitInfo.pSignalSemaphores = signalSemaphores;

    if(vkQueueSubmit(m_graphicsQueue, 1, &submitInfo, m_inFlightFences[m_currentFrame]) != VK_SUCCESS)
    {
        std::cerr << "Failed to submit Vulkan command buffer\n";
    }

    VkPresentInfoKHR presentInfo{};
    presentInfo.sType = VK_STRUCTURE_TYPE_PRESENT_INFO_KHR;
    presentInfo.waitSemaphoreCount = 1;
    presentInfo.pWaitSemaphores = signalSemaphores;
    presentInfo.swapchainCount = 1;
    presentInfo.pSwapchains = &m_swapchain;
    presentInfo.pImageIndices = &m_currentImageIndex;

    const VkResult presentResult = vkQueuePresentKHR(m_presentQueue, &presentInfo);

    if(presentResult == VK_ERROR_OUT_OF_DATE_KHR || presentResult == VK_SUBOPTIMAL_KHR)
    {
        recreateSwapchain();
    }
    else if(presentResult != VK_SUCCESS)
    {
        std::cerr << "Failed to present Vulkan swapchain image\n";
    }

    m_currentFrame = (m_currentFrame + 1) % kMaxFramesInFlight;
    m_currentImageIndex = std::numeric_limits<uint32_t>::max();
}

void VulkanRenderer::setViewProjection(const glm::mat4& view, const glm::mat4& projection)
{
    m_viewMatrix = view;
    m_projectionMatrix = projection;
}

Renderer::BufferHandle VulkanRenderer::createVertexBuffer(const void* data, std::size_t size)
{
    BufferResource resource{};
    if(!createDeviceLocalBuffer(data, size, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, resource))
    {
        return 0;
    }

    const BufferHandle handle = m_nextBufferHandle++;
    resource.usage = VK_BUFFER_USAGE_VERTEX_BUFFER_BIT;
    m_vertexBuffers.emplace(handle, resource);
    return handle;
}

Renderer::BufferHandle VulkanRenderer::createIndexBuffer(const void* data, std::size_t size)
{
    BufferResource resource{};
    if(!createDeviceLocalBuffer(data, size, VK_BUFFER_USAGE_INDEX_BUFFER_BIT, resource))
    {
        return 0;
    }

    const BufferHandle handle = m_nextBufferHandle++;
    resource.usage = VK_BUFFER_USAGE_INDEX_BUFFER_BIT;
    m_indexBuffers.emplace(handle, resource);
    return handle;
}

void VulkanRenderer::destroyBuffer(BufferHandle handle)
{
    if(auto it = m_vertexBuffers.find(handle); it != m_vertexBuffers.end())
    {
        freeBuffer(it->second);
        m_vertexBuffers.erase(it);
        return;
    }

    if(auto it = m_indexBuffers.find(handle); it != m_indexBuffers.end())
    {
        freeBuffer(it->second);
        m_indexBuffers.erase(it);
    }
}

void VulkanRenderer::drawIndexed(BufferHandle vertexBuffer, BufferHandle indexBuffer, std::uint32_t indexCount, const glm::mat4& modelMatrix)
{
    m_drawCommands.push_back({vertexBuffer, indexBuffer, indexCount, modelMatrix});
}

Renderer::TextureHandle VulkanRenderer::createTexture(const void* data, std::uint32_t width, std::uint32_t height, std::uint32_t channels)
{
    if(!data || width == 0 || height == 0)
    {
        return 0;
    }

    const VkDeviceSize imageSize = width * height * 4; // Always RGBA
    std::vector<uint8_t> rgbaData;
    const uint8_t* sourceData = static_cast<const uint8_t*>(data);

    // Convert to RGBA if needed
    if(channels == 3)
    {
        rgbaData.resize(imageSize);
        for(uint32_t i = 0; i < width * height; ++i)
        {
            rgbaData[i * 4 + 0] = sourceData[i * 3 + 0];
            rgbaData[i * 4 + 1] = sourceData[i * 3 + 1];
            rgbaData[i * 4 + 2] = sourceData[i * 3 + 2];
            rgbaData[i * 4 + 3] = 255;
        }
        sourceData = rgbaData.data();
    }

    // Create staging buffer
    VkBuffer stagingBuffer = VK_NULL_HANDLE;
    VkDeviceMemory stagingMemory = VK_NULL_HANDLE;
    if(!createBuffer(imageSize, VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                     VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                     stagingBuffer, stagingMemory))
    {
        return 0;
    }

    void* mappedData = nullptr;
    vkMapMemory(m_device, stagingMemory, 0, imageSize, 0, &mappedData);
    std::memcpy(mappedData, sourceData, imageSize);
    vkUnmapMemory(m_device, stagingMemory);

    // Create device-local image
    VkImage image = VK_NULL_HANDLE;
    VkDeviceMemory imageMemory = VK_NULL_HANDLE;
    if(!createImage(width, height, VK_FORMAT_R8G8B8A8_UNORM, VK_IMAGE_TILING_OPTIMAL,
                    VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT,
                    VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, image, imageMemory))
    {
        vkDestroyBuffer(m_device, stagingBuffer, nullptr);
        vkFreeMemory(m_device, stagingMemory, nullptr);
        return 0;
    }

    // Transition to TRANSFER_DST
    transitionImageLayout(image, VK_FORMAT_R8G8B8A8_UNORM, VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);

    // Copy buffer to image
    copyBufferToImage(stagingBuffer, image, width, height);

    // Transition to SHADER_READ_ONLY
    transitionImageLayout(image, VK_FORMAT_R8G8B8A8_UNORM, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);

    vkDestroyBuffer(m_device, stagingBuffer, nullptr);
    vkFreeMemory(m_device, stagingMemory, nullptr);

    // Create image view
    VkImageViewCreateInfo viewInfo{};
    viewInfo.sType = VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO;
    viewInfo.image = image;
    viewInfo.viewType = VK_IMAGE_VIEW_TYPE_2D;
    viewInfo.format = VK_FORMAT_R8G8B8A8_UNORM;
    viewInfo.subresourceRange.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
    viewInfo.subresourceRange.baseMipLevel = 0;
    viewInfo.subresourceRange.levelCount = 1;
    viewInfo.subresourceRange.baseArrayLayer = 0;
    viewInfo.subresourceRange.layerCount = 1;

    VkImageView imageView = VK_NULL_HANDLE;
    if(vkCreateImageView(m_device, &viewInfo, nullptr, &imageView) != VK_SUCCESS)
    {
        vkDestroyImage(m_device, image, nullptr);
        vkFreeMemory(m_device, imageMemory, nullptr);
        return 0;
    }

    // Create sampler
    VkSampler sampler = VK_NULL_HANDLE;
    if(!createTextureSampler(sampler))
    {
        vkDestroyImageView(m_device, imageView, nullptr);
        vkDestroyImage(m_device, image, nullptr);
        vkFreeMemory(m_device, imageMemory, nullptr);
        return 0;
    }

    const TextureHandle handle = m_nextTextureHandle++;
    TextureResource resource{};
    resource.image = image;
    resource.memory = imageMemory;
    resource.imageView = imageView;
    resource.sampler = sampler;
    resource.width = width;
    resource.height = height;
    m_textures.emplace(handle, resource);

    return handle;
}

void VulkanRenderer::destroyTexture(TextureHandle handle)
{
    auto it = m_textures.find(handle);
    if(it == m_textures.end())
    {
        return;
    }

    const TextureResource& resource = it->second;
    if(resource.sampler != VK_NULL_HANDLE)
    {
        vkDestroySampler(m_device, resource.sampler, nullptr);
    }
    if(resource.imageView != VK_NULL_HANDLE)
    {
        vkDestroyImageView(m_device, resource.imageView, nullptr);
    }
    if(resource.image != VK_NULL_HANDLE)
    {
        vkDestroyImage(m_device, resource.image, nullptr);
    }
    if(resource.memory != VK_NULL_HANDLE)
    {
        vkFreeMemory(m_device, resource.memory, nullptr);
    }

    m_textures.erase(it);
}

void VulkanRenderer::bindTexture(TextureHandle handle, std::uint32_t slot)
{
    auto it = m_textures.find(handle);
    if(it == m_textures.end() || m_currentFrame >= m_descriptorSets.size())
    {
        return;
    }

    const TextureResource& resource = it->second;
    VkDescriptorImageInfo imageInfo{};
    imageInfo.imageLayout = VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
    imageInfo.imageView = resource.imageView;
    imageInfo.sampler = resource.sampler;

    VkWriteDescriptorSet descriptorWrite{};
    descriptorWrite.sType = VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET;
    descriptorWrite.dstSet = m_descriptorSets[m_currentFrame];
    descriptorWrite.dstBinding = 0; // Atlas texture binding
    descriptorWrite.dstArrayElement = 0;
    descriptorWrite.descriptorType = VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
    descriptorWrite.descriptorCount = 1;
    descriptorWrite.pImageInfo = &imageInfo;

    vkUpdateDescriptorSets(m_device, 1, &descriptorWrite, 0, nullptr);
}

void VulkanRenderer::setLightingParams(const LightingParams& params)
{
    m_lightingParams = params;
    if(glm::length(m_lightingParams.sunDirection) > 0.0f)
    {
        m_lightingParams.sunDirection = glm::normalize(m_lightingParams.sunDirection);
    }
    updateLightingUniformBuffer();
}

RendererCapabilities VulkanRenderer::getCapabilities() const
{
    RendererCapabilities capabilities{};
    capabilities.backend = RendererBackend::Vulkan;
    if(m_physicalDevice != VK_NULL_HANDLE)
    {
        VkPhysicalDeviceProperties properties{};
        vkGetPhysicalDeviceProperties(m_physicalDevice, &properties);
        capabilities.maxTextureSize = properties.limits.maxImageDimension2D;
        capabilities.backendVersion = std::to_string(VK_API_VERSION_MAJOR(properties.apiVersion)) + '.' +
                                      std::to_string(VK_API_VERSION_MINOR(properties.apiVersion)) + '.' +
                                      std::to_string(VK_API_VERSION_PATCH(properties.apiVersion));

        VkPhysicalDeviceAccelerationStructureFeaturesKHR accelFeatures{};
        accelFeatures.sType =
            VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_ACCELERATION_STRUCTURE_FEATURES_KHR;
        VkPhysicalDeviceRayTracingPipelineFeaturesKHR rayFeatures{};
        rayFeatures.sType = VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_RAY_TRACING_PIPELINE_FEATURES_KHR;
        rayFeatures.pNext = &accelFeatures;

        VkPhysicalDeviceFeatures2 features2{};
        features2.sType = VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2;
        features2.pNext = &rayFeatures;

        vkGetPhysicalDeviceFeatures2(m_physicalDevice, &features2);
        capabilities.supportsRayTracing = rayFeatures.rayTracingPipeline == VK_TRUE;
    }
    return capabilities;
}

bool VulkanRenderer::isVSyncEnabled() const
{
    return m_vsyncEnabled;
}

void VulkanRenderer::setVSync(bool enabled)
{
    if(m_vsyncEnabled == enabled)
    {
        return;
    }

    m_vsyncEnabled = enabled;
    recreateSwapchain();
}

bool VulkanRenderer::createInstance()
{
    if(kEnableValidationLayers && !checkValidationLayerSupport())
    {
        std::cerr << "Validation layers requested but not available\n";
        return false;
    }

    VkApplicationInfo appInfo{};
    appInfo.sType = VK_STRUCTURE_TYPE_APPLICATION_INFO;
    appInfo.pApplicationName = "PoorCraft";
    appInfo.applicationVersion = VK_MAKE_API_VERSION(0, 0, 1, 0);
    appInfo.pEngineName = "PoorCraft";
    appInfo.engineVersion = VK_MAKE_API_VERSION(0, 1, 0, 0);
    appInfo.apiVersion = VK_API_VERSION_1_2;

    auto extensions = m_window.getRequiredVulkanExtensions();
    if(kEnableValidationLayers)
    {
        extensions.push_back(VK_EXT_DEBUG_UTILS_EXTENSION_NAME);
    }

    VkInstanceCreateInfo createInfo{};
    createInfo.sType = VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
    createInfo.pApplicationInfo = &appInfo;
    createInfo.enabledExtensionCount = static_cast<uint32_t>(extensions.size());
    createInfo.ppEnabledExtensionNames = extensions.data();

    VkDebugUtilsMessengerCreateInfoEXT debugCreateInfo{};
    if(kEnableValidationLayers)
    {
        createInfo.enabledLayerCount = static_cast<uint32_t>(VALIDATION_LAYERS.size());
        createInfo.ppEnabledLayerNames = VALIDATION_LAYERS.data();

        populateDebugMessengerCreateInfo(debugCreateInfo);
        createInfo.pNext = &debugCreateInfo;
    }
    else
    {
        createInfo.enabledLayerCount = 0;
        createInfo.pNext = nullptr;
    }

    if(vkCreateInstance(&createInfo, nullptr, &m_instance) != VK_SUCCESS)
    {
        return false;
    }

    if(kEnableValidationLayers)
    {
        populateDebugMessengerCreateInfo(debugCreateInfo);
        if(createDebugUtilsMessengerEXT(m_instance, &debugCreateInfo, nullptr, &m_debugMessenger) != VK_SUCCESS)
        {
            std::cerr << "Failed to set up debug messenger\n";
        }
    }

    return true;
}

bool VulkanRenderer::selectPhysicalDevice()
{
    uint32_t deviceCount = 0;
    vkEnumeratePhysicalDevices(m_instance, &deviceCount, nullptr);
    if(deviceCount == 0)
    {
        return false;
    }

    std::vector<VkPhysicalDevice> devices(deviceCount);
    vkEnumeratePhysicalDevices(m_instance, &deviceCount, devices.data());

    auto rateDeviceSuitability = [&](VkPhysicalDevice device) {
        VkPhysicalDeviceProperties deviceProperties{};
        VkPhysicalDeviceFeatures deviceFeatures{};
        vkGetPhysicalDeviceProperties(device, &deviceProperties);
        vkGetPhysicalDeviceFeatures(device, &deviceFeatures);

        if(!deviceFeatures.geometryShader)
        {
            return 0;
        }

        if(!checkDeviceExtensionSupport(device))
        {
            return 0;
        }

        QueueFamilyIndices indices = findQueueFamilies(device, m_surface);
        if(!indices.isComplete())
        {
            return 0;
        }

        SwapchainSupportDetails swapchainSupport = querySwapchainSupport(device, m_surface);
        if(swapchainSupport.formats.empty() || swapchainSupport.presentModes.empty())
        {
            return 0;
        }

        int score = 0;

        if(deviceProperties.deviceType == VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU)
        {
            score += 1000;
        }

        score += static_cast<int>(deviceProperties.limits.maxImageDimension2D);

        return score;
    };

    VkPhysicalDevice bestDevice = VK_NULL_HANDLE;
    int bestScore = 0;

    for(const auto& device : devices)
    {
        int score = rateDeviceSuitability(device);
        if(score > bestScore)
        {
            bestScore = score;
            bestDevice = device;
        }
    }

    if(bestDevice == VK_NULL_HANDLE)
    {
        return false;
    }

    m_physicalDevice = bestDevice;
    return true;
}

bool VulkanRenderer::createLogicalDevice()
{
    const QueueFamilyIndices indices = findQueueFamilies(m_physicalDevice, m_surface);

    std::set<uint32_t> uniqueQueues = {indices.graphicsFamily.value(), indices.presentFamily.value()};

    std::vector<VkDeviceQueueCreateInfo> queueCreateInfos;
    const float queuePriority = 1.0f;

    for(uint32_t queueFamily : uniqueQueues)
    {
        VkDeviceQueueCreateInfo queueCreateInfo{};
        queueCreateInfo.sType = VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO;
        queueCreateInfo.queueFamilyIndex = queueFamily;
        queueCreateInfo.queueCount = 1;
        queueCreateInfo.pQueuePriorities = &queuePriority;
        queueCreateInfos.push_back(queueCreateInfo);
    }

    VkPhysicalDeviceFeatures deviceFeatures{};
    deviceFeatures.samplerAnisotropy = VK_TRUE;

    VkDeviceCreateInfo createInfo{};
    createInfo.sType = VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO;
    createInfo.queueCreateInfoCount = static_cast<uint32_t>(queueCreateInfos.size());
    createInfo.pQueueCreateInfos = queueCreateInfos.data();
    createInfo.pEnabledFeatures = &deviceFeatures;

    const std::vector<const char*> deviceExtensions = {VK_KHR_SWAPCHAIN_EXTENSION_NAME};
    createInfo.enabledExtensionCount = static_cast<uint32_t>(deviceExtensions.size());
    createInfo.ppEnabledExtensionNames = deviceExtensions.data();

    if(kEnableValidationLayers)
    {
        createInfo.enabledLayerCount = static_cast<uint32_t>(VALIDATION_LAYERS.size());
        createInfo.ppEnabledLayerNames = VALIDATION_LAYERS.data();
    }
    else
    {
        createInfo.enabledLayerCount = 0;
    }

    if(vkCreateDevice(m_physicalDevice, &createInfo, nullptr, &m_device) != VK_SUCCESS)
    {
        return false;
    }

    vkGetDeviceQueue(m_device, indices.graphicsFamily.value(), 0, &m_graphicsQueue);
    vkGetDeviceQueue(m_device, indices.presentFamily.value(), 0, &m_presentQueue);

    return true;
}

bool VulkanRenderer::createSurface()
{
    if(!m_window.createVulkanSurface(m_instance, &m_surface))
    {
        return false;
    }
    return true;
}

bool VulkanRenderer::createSwapchain()
{
    SwapchainSupportDetails swapchainSupport = querySwapchainSupport(m_physicalDevice, m_surface);

    VkSurfaceFormatKHR surfaceFormat = chooseSwapSurfaceFormat(swapchainSupport.formats);
    VkPresentModeKHR presentMode = chooseSwapPresentMode(swapchainSupport.presentModes, m_vsyncEnabled);
    VkExtent2D extent = chooseSwapExtent(
        swapchainSupport.capabilities,
        m_window.getWidth(),
        m_window.getHeight());

    if(extent.width == 0 || extent.height == 0)
    {
        std::cerr << "[Vulkan] Skipping swapchain creation because framebuffer extent is zero\n";
        return false;
    }

    uint32_t imageCount = swapchainSupport.capabilities.minImageCount + 1;
    if(swapchainSupport.capabilities.maxImageCount > 0 && imageCount > swapchainSupport.capabilities.maxImageCount)
    {
        imageCount = swapchainSupport.capabilities.maxImageCount;
    }

    VkSwapchainCreateInfoKHR createInfo{};
    createInfo.sType = VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR;
    createInfo.surface = m_surface;
    createInfo.minImageCount = imageCount;
    createInfo.imageFormat = surfaceFormat.format;
    createInfo.imageColorSpace = surfaceFormat.colorSpace;
    createInfo.imageExtent = extent;
    createInfo.imageArrayLayers = 1;
    createInfo.imageUsage = VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;

    const QueueFamilyIndices indices = findQueueFamilies(m_physicalDevice, m_surface);
    uint32_t queueFamilyIndices[] = {indices.graphicsFamily.value(), indices.presentFamily.value()};

    if(indices.graphicsFamily != indices.presentFamily)
    {
        createInfo.imageSharingMode = VK_SHARING_MODE_CONCURRENT;
        createInfo.queueFamilyIndexCount = 2;
        createInfo.pQueueFamilyIndices = queueFamilyIndices;
    }
    else
    {
        createInfo.imageSharingMode = VK_SHARING_MODE_EXCLUSIVE;
        createInfo.queueFamilyIndexCount = 0;
        createInfo.pQueueFamilyIndices = nullptr;
    }

    createInfo.preTransform = swapchainSupport.capabilities.currentTransform;
    createInfo.compositeAlpha = VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR;
    createInfo.presentMode = presentMode;
    createInfo.clipped = VK_TRUE;
    createInfo.oldSwapchain = VK_NULL_HANDLE;

    if(vkCreateSwapchainKHR(m_device, &createInfo, nullptr, &m_swapchain) != VK_SUCCESS)
    {
        return false;
    }

    vkGetSwapchainImagesKHR(m_device, m_swapchain, &imageCount, nullptr);
    m_swapchainImages.resize(imageCount);
    vkGetSwapchainImagesKHR(m_device, m_swapchain, &imageCount, m_swapchainImages.data());

    m_swapchainImageFormat = surfaceFormat.format;
    m_swapchainExtent = extent;
    m_imagesInFlight.assign(m_swapchainImages.size(), VK_NULL_HANDLE);

    m_swapchainImageViews.resize(m_swapchainImages.size());
    for(size_t i = 0; i < m_swapchainImages.size(); ++i)
    {
        VkImageViewCreateInfo viewInfo{};
        viewInfo.sType = VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO;
        viewInfo.image = m_swapchainImages[i];
        viewInfo.viewType = VK_IMAGE_VIEW_TYPE_2D;
        viewInfo.format = m_swapchainImageFormat;
        viewInfo.components.r = VK_COMPONENT_SWIZZLE_IDENTITY;
        viewInfo.components.g = VK_COMPONENT_SWIZZLE_IDENTITY;
        viewInfo.components.b = VK_COMPONENT_SWIZZLE_IDENTITY;
        viewInfo.components.a = VK_COMPONENT_SWIZZLE_IDENTITY;
        viewInfo.subresourceRange.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
        viewInfo.subresourceRange.baseMipLevel = 0;
        viewInfo.subresourceRange.levelCount = 1;
        viewInfo.subresourceRange.baseArrayLayer = 0;
        viewInfo.subresourceRange.layerCount = 1;

        if(vkCreateImageView(m_device, &viewInfo, nullptr, &m_swapchainImageViews[i]) != VK_SUCCESS)
        {
            return false;
        }
    }

    return true;
}

bool VulkanRenderer::createRenderPass()
{
    VkAttachmentDescription colorAttachment{};
    colorAttachment.format = m_swapchainImageFormat;
    colorAttachment.samples = VK_SAMPLE_COUNT_1_BIT;
    colorAttachment.loadOp = VK_ATTACHMENT_LOAD_OP_CLEAR;
    colorAttachment.storeOp = VK_ATTACHMENT_STORE_OP_STORE;
    colorAttachment.stencilLoadOp = VK_ATTACHMENT_LOAD_OP_DONT_CARE;
    colorAttachment.stencilStoreOp = VK_ATTACHMENT_STORE_OP_DONT_CARE;
    colorAttachment.initialLayout = VK_IMAGE_LAYOUT_UNDEFINED;
    colorAttachment.finalLayout = VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;

    VkAttachmentDescription depthAttachment{};
    depthAttachment.format = m_depthFormat;
    depthAttachment.samples = VK_SAMPLE_COUNT_1_BIT;
    depthAttachment.loadOp = VK_ATTACHMENT_LOAD_OP_CLEAR;
    depthAttachment.storeOp = VK_ATTACHMENT_STORE_OP_DONT_CARE;
    depthAttachment.stencilLoadOp = VK_ATTACHMENT_LOAD_OP_DONT_CARE;
    depthAttachment.stencilStoreOp = VK_ATTACHMENT_STORE_OP_DONT_CARE;
    depthAttachment.initialLayout = VK_IMAGE_LAYOUT_UNDEFINED;
    depthAttachment.finalLayout = VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL;

    VkAttachmentReference colorAttachmentRef{};
    colorAttachmentRef.attachment = 0;
    colorAttachmentRef.layout = VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL;

    VkAttachmentReference depthAttachmentRef{};
    depthAttachmentRef.attachment = 1;
    depthAttachmentRef.layout = VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL;

    VkSubpassDescription subpass{};
    subpass.pipelineBindPoint = VK_PIPELINE_BIND_POINT_GRAPHICS;
    subpass.colorAttachmentCount = 1;
    subpass.pColorAttachments = &colorAttachmentRef;
    subpass.pDepthStencilAttachment = &depthAttachmentRef;

    VkSubpassDependency dependency{};
    dependency.srcSubpass = VK_SUBPASS_EXTERNAL;
    dependency.dstSubpass = 0;
    dependency.srcStageMask = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT | VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT;
    dependency.srcAccessMask = 0;
    dependency.dstStageMask = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT | VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT;
    dependency.dstAccessMask = VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT | VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT;

    std::array<VkAttachmentDescription, 2> attachments = {colorAttachment, depthAttachment};
    VkRenderPassCreateInfo renderPassInfo{};
    renderPassInfo.sType = VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO;
    renderPassInfo.attachmentCount = static_cast<uint32_t>(attachments.size());
    renderPassInfo.pAttachments = attachments.data();
    renderPassInfo.subpassCount = 1;
    renderPassInfo.pSubpasses = &subpass;
    renderPassInfo.dependencyCount = 1;
    renderPassInfo.pDependencies = &dependency;

    if(vkCreateRenderPass(m_device, &renderPassInfo, nullptr, &m_renderPass) != VK_SUCCESS)
    {
        return false;
    }

    return true;
}

bool VulkanRenderer::createPipeline()
{
    // Basic SPIR-V shaders compiled offline (layout matching ChunkVertex: vec3 position, vec3 normal, vec2 uv)
    static constexpr uint32_t vertexShaderCode[] = {
        0x07230203,0x00010000,0x0008000a,0x00000018,0x00000000,0x00020011,0x00000001,0x0006000b,
        0x00000001,0x4c534c47,0x6474732e,0x3035342e,0x00000000,0x0003000e,0x00000000,0x00000001,
        0x0007000f,0x00000000,0x00000004,0x6e69616d,0x00000000,0x00000009,0x0000000d,0x00030003,
        0x00000002,0x000001c2,0x00040005,0x00000004,0x6e69616d,0x00000000,0x00050005,0x00000009,
        0x6f505f69,0x69746973,0x00006e6f,0x00050005,0x0000000d,0x6f4e5f69,0x616d726f,0x0000006c,
        0x00050005,0x00000010,0x70756c6d,0x6f4D5f76,0x006c6564,0x00050005,0x00000013,0x65766e69,
        0x6f50775f,0x00000000,0x00050005,0x00000016,0x636f7250,0x6a65565f,0x00000000,0x00050048,
        0x00000010,0x00000000,0x00000023,0x00000000,0x00050048,0x00000010,0x00000001,0x00000023,
        0x00000040,0x00050048,0x00000010,0x00000002,0x00000023,0x00000080,0x00030047,0x00000010,
        0x00000002,0x00040047,0x00000009,0x0000001e,0x00000000,0x00040047,0x0000000d,0x0000001e,
        0x00000001,0x00040047,0x00000016,0x0000001e,0x00000002,0x00040047,0x00000013,0x0000001e,
        0x00000001,0x00020013,0x00000002,0x00030021,0x00000003,0x00000002,0x00030016,0x00000006,
        0x00000020,0x00040017,0x00000007,0x00000006,0x00000003,0x00040017,0x00000008,0x00000006,
        0x00000002,0x00040020,0x00000009,0x00000001,0x00000007,0x00040020,0x0000000d,0x00000001,
        0x00000007,0x0004002b,0x00000006,0x0000000f,0x3f800000,0x0006001e,0x00000010,0x00000007,
        0x00000007,0x00000007,0x00000007,0x00040020,0x00000011,0x00000009,0x00000010,0x0004003b,
        0x00000011,0x00000012,0x00000009,0x00040020,0x00000013,0x00000001,0x00000008,0x00040020,
        0x00000016,0x00000001,0x00000008,0x00040017,0x00000017,0x00000006,0x00000004,0x00040020,
        0x00000018,0x00000003,0x00000017,0x0004003b,0x00000018,0x00000019,0x00000003,0x00040017,
        0x0000001a,0x00000006,0x00000004,0x00050036,0x00000002,0x00000004,0x00000000,0x00000003,
        0x000200f8,0x00000005,0x0004003d,0x00000007,0x0000000a,0x00000009,0x00050051,0x00000006,
        0x0000000b,0x0000000a,0x00000000,0x00050051,0x00000006,0x0000000c,0x0000000a,0x00000001,
        0x00050051,0x00000006,0x0000000e,0x0000000a,0x00000002,0x00050083,0x00000006,0x0000000f,
        0x0000000f,0x0000000e,0x00070050,0x00000017,0x00000014,0x0000000b,0x0000000c,0x0000000f,
        0x0000000f,0x0004003d,0x00000010,0x00000015,0x00000012,0x0008004f,0x0000001a,0x0000001b,
        0x00000015,0x00000015,0x00000000,0x00000001,0x00000002,0x00000003,0x00050091,0x0000001a,
        0x0000001c,0x0000001b,0x00000014,0x0004003d,0x00000010,0x0000001d,0x00000012,0x0008004f,
        0x0000001a,0x0000001e,0x0000001d,0x0000001d,0x00000004,0x00000005,0x00000006,0x00000007,
        0x00050091,0x0000001a,0x0000001f,0x0000001e,0x0000001c,0x0003003e,0x00000019,0x0000001f,
        0x000100fd,0x00010038};

    static constexpr uint32_t fragmentShaderCode[] = {
        0x07230203,0x00010000,0x0008000a,0x00000008,0x00000000,0x00020011,0x00000001,0x0006000b,
        0x00000001,0x4c534c47,0x6474732e,0x3035342e,0x00000000,0x0003000e,0x00000000,0x00000001,
        0x0007000f,0x00000004,0x00000004,0x6e69616d,0x00000000,0x00000005,0x00000006,0x00030003,
        0x00000002,0x000001c2,0x00040005,0x00000004,0x6e69616d,0x00000000,0x00050005,0x00000005,
        0x6f4e5f69,0x616d726f,0x0000006c,0x00050005,0x00000006,0x6f435f6f,0x726f6c6c,0x00000000,
        0x00040047,0x00000005,0x0000001e,0x00000000,0x00040047,0x00000006,0x0000001e,0x00000000,
        0x00020013,0x00000002,0x00030021,0x00000003,0x00000002,0x00030016,0x00000007,0x00000020,
        0x00040017,0x00000008,0x00000007,0x00000004,0x00040020,0x00000005,0x00000001,0x00000008,
        0x00040020,0x00000006,0x00000003,0x00000008,0x0004003b,0x00000006,0x00000007,0x00000003,
        0x00050036,0x00000002,0x00000004,0x00000000,0x00000003,0x000200f8,0x00000005,0x0004003d,
        0x00000008,0x00000006,0x00000005,0x0003003e,0x00000007,0x00000006,0x000100fd,0x00010038};

    VkShaderModuleCreateInfo vertCreateInfo{};
    vertCreateInfo.sType = VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO;
    vertCreateInfo.codeSize = sizeof(vertexShaderCode);
    vertCreateInfo.pCode = vertexShaderCode;

    VkShaderModule vertModule = VK_NULL_HANDLE;
    if(vkCreateShaderModule(m_device, &vertCreateInfo, nullptr, &vertModule) != VK_SUCCESS)
    {
        return false;
    }

    VkShaderModuleCreateInfo fragCreateInfo{};
    fragCreateInfo.sType = VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO;
    fragCreateInfo.codeSize = sizeof(fragmentShaderCode);
    fragCreateInfo.pCode = fragmentShaderCode;

    VkShaderModule fragModule = VK_NULL_HANDLE;
    if(vkCreateShaderModule(m_device, &fragCreateInfo, nullptr, &fragModule) != VK_SUCCESS)
    {
        vkDestroyShaderModule(m_device, vertModule, nullptr);
        return false;
    }

    VkPipelineShaderStageCreateInfo vertStageInfo{};
    vertStageInfo.sType = VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO;
    vertStageInfo.stage = VK_SHADER_STAGE_VERTEX_BIT;
    vertStageInfo.module = vertModule;
    vertStageInfo.pName = "main";

    VkPipelineShaderStageCreateInfo fragStageInfo{};
    fragStageInfo.sType = VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO;
    fragStageInfo.stage = VK_SHADER_STAGE_FRAGMENT_BIT;
    fragStageInfo.module = fragModule;
    fragStageInfo.pName = "main";

    VkPipelineShaderStageCreateInfo shaderStages[] = {vertStageInfo, fragStageInfo};

    VkVertexInputBindingDescription binding{};
    binding.binding = 0;
    binding.stride = sizeof(world::ChunkVertex);
    binding.inputRate = VK_VERTEX_INPUT_RATE_VERTEX;

    std::array<VkVertexInputAttributeDescription, 4> attributes{};
    attributes[0].location = 0;
    attributes[0].binding = 0;
    attributes[0].format = VK_FORMAT_R32G32B32_SFLOAT;
    attributes[0].offset = offsetof(world::ChunkVertex, position);
    attributes[1].location = 1;
    attributes[1].binding = 0;
    attributes[1].format = VK_FORMAT_R32G32B32_SFLOAT;
    attributes[1].offset = offsetof(world::ChunkVertex, normal);
    attributes[2].location = 2;
    attributes[2].binding = 0;
    attributes[2].format = VK_FORMAT_R32G32_SFLOAT;
    attributes[2].offset = offsetof(world::ChunkVertex, texCoord);
    attributes[3].location = 3;
    attributes[3].binding = 0;
    attributes[3].format = VK_FORMAT_R32_SFLOAT;
    attributes[3].offset = offsetof(world::ChunkVertex, ao);

    VkPipelineVertexInputStateCreateInfo vertexInputInfo{};
    vertexInputInfo.sType = VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO;
    vertexInputInfo.vertexBindingDescriptionCount = 1;
    vertexInputInfo.pVertexBindingDescriptions = &binding;
    vertexInputInfo.vertexAttributeDescriptionCount = static_cast<uint32_t>(attributes.size());
    vertexInputInfo.pVertexAttributeDescriptions = attributes.data();

    VkPipelineInputAssemblyStateCreateInfo inputAssembly{};
    inputAssembly.sType = VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO;
    inputAssembly.topology = VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST;
    inputAssembly.primitiveRestartEnable = VK_FALSE;

    VkPipelineViewportStateCreateInfo viewportState{};
    viewportState.sType = VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO;
    viewportState.viewportCount = 1;
    viewportState.scissorCount = 1;

    VkPipelineRasterizationStateCreateInfo rasterizer{};
    rasterizer.sType = VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO;
    rasterizer.depthClampEnable = VK_FALSE;
    rasterizer.rasterizerDiscardEnable = VK_FALSE;
    rasterizer.polygonMode = VK_POLYGON_MODE_FILL;
    rasterizer.cullMode = VK_CULL_MODE_BACK_BIT;
    rasterizer.frontFace = VK_FRONT_FACE_COUNTER_CLOCKWISE;
    rasterizer.depthBiasEnable = VK_FALSE;

    VkPipelineMultisampleStateCreateInfo multisampling{};
    multisampling.sType = VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO;
    multisampling.rasterizationSamples = VK_SAMPLE_COUNT_1_BIT;

    VkPipelineDepthStencilStateCreateInfo depthStencil{};
    depthStencil.sType = VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO;
    depthStencil.depthTestEnable = VK_TRUE;
    depthStencil.depthWriteEnable = VK_TRUE;
    depthStencil.depthCompareOp = VK_COMPARE_OP_LESS;
    depthStencil.depthBoundsTestEnable = VK_FALSE;
    depthStencil.stencilTestEnable = VK_FALSE;

    VkPipelineColorBlendAttachmentState colorBlendAttachment{};
    colorBlendAttachment.colorWriteMask = VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT | VK_COLOR_COMPONENT_B_BIT |
                                          VK_COLOR_COMPONENT_A_BIT;
    colorBlendAttachment.blendEnable = VK_FALSE;

    VkPipelineColorBlendStateCreateInfo colorBlending{};
    colorBlending.sType = VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO;
    colorBlending.logicOpEnable = VK_FALSE;
    colorBlending.attachmentCount = 1;
    colorBlending.pAttachments = &colorBlendAttachment;

    VkPushConstantRange pushConstant{};
    pushConstant.stageFlags = VK_SHADER_STAGE_VERTEX_BIT;
    pushConstant.offset = 0;
    pushConstant.size = sizeof(glm::mat4) * 3;

    VkPipelineLayoutCreateInfo pipelineLayoutInfo{};
    pipelineLayoutInfo.sType = VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO;
    pipelineLayoutInfo.pushConstantRangeCount = 1;
    pipelineLayoutInfo.pPushConstantRanges = &pushConstant;
    pipelineLayoutInfo.setLayoutCount = 1;
    pipelineLayoutInfo.pSetLayouts = &m_descriptorSetLayout;

    if(vkCreatePipelineLayout(m_device, &pipelineLayoutInfo, nullptr, &m_pipelineLayout) != VK_SUCCESS)
    {
        vkDestroyShaderModule(m_device, fragModule, nullptr);
        vkDestroyShaderModule(m_device, vertModule, nullptr);
        return false;
    }

    VkGraphicsPipelineCreateInfo pipelineInfo{};
    pipelineInfo.sType = VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO;
    pipelineInfo.stageCount = static_cast<uint32_t>(std::size(shaderStages));
    pipelineInfo.pStages = shaderStages;
    pipelineInfo.pVertexInputState = &vertexInputInfo;
    pipelineInfo.pInputAssemblyState = &inputAssembly;
    pipelineInfo.pViewportState = &viewportState;
    pipelineInfo.pRasterizationState = &rasterizer;
    pipelineInfo.pMultisampleState = &multisampling;
    pipelineInfo.pDepthStencilState = &depthStencil;
    pipelineInfo.pColorBlendState = &colorBlending;
    pipelineInfo.layout = m_pipelineLayout;
    pipelineInfo.renderPass = m_renderPass;
    pipelineInfo.subpass = 0;

    if(vkCreateGraphicsPipelines(m_device, VK_NULL_HANDLE, 1, &pipelineInfo, nullptr, &m_graphicsPipeline) != VK_SUCCESS)
    {
        vkDestroyPipelineLayout(m_device, m_pipelineLayout, nullptr);
        m_pipelineLayout = VK_NULL_HANDLE;
        vkDestroyShaderModule(m_device, fragModule, nullptr);
        vkDestroyShaderModule(m_device, vertModule, nullptr);
        return false;
    }

    vkDestroyShaderModule(m_device, fragModule, nullptr);
    vkDestroyShaderModule(m_device, vertModule, nullptr);

    return true;
}

bool VulkanRenderer::createCommandPool()
{
    QueueFamilyIndices queueFamilyIndices = findQueueFamilies(m_physicalDevice, m_surface);

    VkCommandPoolCreateInfo poolInfo{};
    poolInfo.sType = VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO;
    poolInfo.queueFamilyIndex = queueFamilyIndices.graphicsFamily.value();
    poolInfo.flags = VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT;

    if(vkCreateCommandPool(m_device, &poolInfo, nullptr, &m_commandPool) != VK_SUCCESS)
    {
        return false;
    }

    return true;
}

bool VulkanRenderer::allocateCommandBuffers()
{
    m_commandBuffers.resize(kMaxFramesInFlight);

    VkCommandBufferAllocateInfo allocInfo{};
    allocInfo.sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO;
    allocInfo.commandPool = m_commandPool;
    allocInfo.level = VK_COMMAND_BUFFER_LEVEL_PRIMARY;
    allocInfo.commandBufferCount = static_cast<uint32_t>(m_commandBuffers.size());

    if(vkAllocateCommandBuffers(m_device, &allocInfo, m_commandBuffers.data()) != VK_SUCCESS)
    {
        return false;
    }

    return true;
}

bool VulkanRenderer::createSyncObjects()
{
    m_imageAvailableSemaphores.resize(kMaxFramesInFlight);
    m_renderFinishedSemaphores.resize(kMaxFramesInFlight);
    m_inFlightFences.resize(kMaxFramesInFlight);
    m_imagesInFlight.resize(m_swapchainImages.size(), VK_NULL_HANDLE);

    VkSemaphoreCreateInfo semaphoreInfo{};
    semaphoreInfo.sType = VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO;

    VkFenceCreateInfo fenceInfo{};
    fenceInfo.sType = VK_STRUCTURE_TYPE_FENCE_CREATE_INFO;
    fenceInfo.flags = VK_FENCE_CREATE_SIGNALED_BIT;

    for(size_t i = 0; i < kMaxFramesInFlight; ++i)
    {
        if(vkCreateSemaphore(m_device, &semaphoreInfo, nullptr, &m_imageAvailableSemaphores[i]) != VK_SUCCESS ||
           vkCreateSemaphore(m_device, &semaphoreInfo, nullptr, &m_renderFinishedSemaphores[i]) != VK_SUCCESS ||
           vkCreateFence(m_device, &fenceInfo, nullptr, &m_inFlightFences[i]) != VK_SUCCESS)
        {
            return false;
        }
    }

    return true;
}

bool VulkanRenderer::recreateSwapchain()
{
    int width = m_window.getWidth();
    int height = m_window.getHeight();

    while((width == 0 || height == 0) && !m_window.shouldClose())
    {
        m_window.pollEvents();
        std::this_thread::sleep_for(std::chrono::milliseconds(50));
        width = m_window.getWidth();
        height = m_window.getHeight();
    }

    if(width == 0 || height == 0)
    {
        return false;
    }

    vkDeviceWaitIdle(m_device);

    destroyFramebuffers();
    destroyDepthResources();
    destroyPipeline();
    destroyRenderPass();
    destroySwapchain();

    if(!createSwapchain())
    {
        return false;
    }

    if(!createRenderPass())
    {
        return false;
    }

    if(!createPipeline())
    {
        return false;
    }

    if(!createDepthResources())
    {
        return false;
    }

    if(!createFramebuffers())
    {
        return false;
    }

    if(m_imguiInitialized)
    {
        ImGui_ImplVulkan_SetMinImageCount(static_cast<uint32_t>(m_swapchainImages.size()));
        if(!recreateImGuiBackend())
        {
            return false;
        }
    }

    return true;
}

void VulkanRenderer::destroySwapchain()
{
    for(auto imageView : m_swapchainImageViews)
    {
        vkDestroyImageView(m_device, imageView, nullptr);
    }
    m_swapchainImageViews.clear();
    m_swapchainImages.clear();

    if(m_swapchain != VK_NULL_HANDLE)
    {
        vkDestroySwapchainKHR(m_device, m_swapchain, nullptr);
        m_swapchain = VK_NULL_HANDLE;
    }

    m_currentImageIndex = std::numeric_limits<uint32_t>::max();
    m_swapchainExtent = {0, 0};
}

void VulkanRenderer::destroyPipeline()
{
    if(m_graphicsPipeline != VK_NULL_HANDLE)
    {
        vkDestroyPipeline(m_device, m_graphicsPipeline, nullptr);
        m_graphicsPipeline = VK_NULL_HANDLE;
    }
    if(m_pipelineLayout != VK_NULL_HANDLE)
    {
        vkDestroyPipelineLayout(m_device, m_pipelineLayout, nullptr);
        m_pipelineLayout = VK_NULL_HANDLE;
    }
}

void VulkanRenderer::destroyRenderPass()
{
    if(m_renderPass != VK_NULL_HANDLE)
    {
        vkDestroyRenderPass(m_device, m_renderPass, nullptr);
        m_renderPass = VK_NULL_HANDLE;
    }
}

void VulkanRenderer::destroyDepthResources()
{
    for(size_t i = 0; i < m_depthImageViews.size(); ++i)
    {
        if(m_depthImageViews[i] != VK_NULL_HANDLE)
        {
            vkDestroyImageView(m_device, m_depthImageViews[i], nullptr);
            m_depthImageViews[i] = VK_NULL_HANDLE;
        }
        if(m_depthImages[i] != VK_NULL_HANDLE)
        {
            vkDestroyImage(m_device, m_depthImages[i], nullptr);
            m_depthImages[i] = VK_NULL_HANDLE;
        }
        if(m_depthImageMemory[i] != VK_NULL_HANDLE)
        {
            vkFreeMemory(m_device, m_depthImageMemory[i], nullptr);
            m_depthImageMemory[i] = VK_NULL_HANDLE;
        }
    }
    m_depthImageViews.clear();
    m_depthImages.clear();
    m_depthImageMemory.clear();
}

void VulkanRenderer::destroyFramebuffers()
{
    for(auto framebuffer : m_swapchainFramebuffers)
    {
        vkDestroyFramebuffer(m_device, framebuffer, nullptr);
    }
    m_swapchainFramebuffers.clear();
}

bool VulkanRenderer::createDepthResources()
{
    VkFormat depthFormat = VK_FORMAT_D32_SFLOAT;

    m_depthImages.resize(m_swapchainImages.size(), VK_NULL_HANDLE);
    m_depthImageMemory.resize(m_swapchainImages.size(), VK_NULL_HANDLE);
    m_depthImageViews.resize(m_swapchainImages.size(), VK_NULL_HANDLE);

    for(size_t i = 0; i < m_swapchainImages.size(); ++i)
    {
        if(!createImage(
               m_swapchainExtent.width,
               m_swapchainExtent.height,
               depthFormat,
               VK_IMAGE_TILING_OPTIMAL,
               VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT,
               VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT,
               m_depthImages[i],
               m_depthImageMemory[i]))
        {
            return false;
        }

        VkImageViewCreateInfo viewInfo{};
        viewInfo.sType = VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO;
        viewInfo.image = m_depthImages[i];
        viewInfo.viewType = VK_IMAGE_VIEW_TYPE_2D;
        viewInfo.format = depthFormat;
        viewInfo.subresourceRange.aspectMask = VK_IMAGE_ASPECT_DEPTH_BIT;
        viewInfo.subresourceRange.baseMipLevel = 0;
        viewInfo.subresourceRange.levelCount = 1;
        viewInfo.subresourceRange.baseArrayLayer = 0;
        viewInfo.subresourceRange.layerCount = 1;

        if(vkCreateImageView(m_device, &viewInfo, nullptr, &m_depthImageViews[i]) != VK_SUCCESS)
        {
            return false;
        }
    }

    m_depthFormat = depthFormat;
    return true;
}

bool VulkanRenderer::createFramebuffers()
{
    m_swapchainFramebuffers.resize(m_swapchainImageViews.size());

    for(size_t i = 0; i < m_swapchainImageViews.size(); ++i)
    {
        std::array<VkImageView, 2> attachments = {m_swapchainImageViews[i], m_depthImageViews[i]};

        VkFramebufferCreateInfo framebufferInfo{};
        framebufferInfo.sType = VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO;
        framebufferInfo.renderPass = m_renderPass;
        framebufferInfo.attachmentCount = static_cast<uint32_t>(attachments.size());
        framebufferInfo.pAttachments = attachments.data();
        framebufferInfo.width = m_swapchainExtent.width;
        framebufferInfo.height = m_swapchainExtent.height;
        framebufferInfo.layers = 1;

        if(vkCreateFramebuffer(m_device, &framebufferInfo, nullptr, &m_swapchainFramebuffers[i]) != VK_SUCCESS)
        {
            return false;
        }
    }

    return true;
}

bool VulkanRenderer::initializeUI()
{
    if(m_imguiInitialized)
    {
        return true;
    }

    if(m_imguiDescriptorPool == VK_NULL_HANDLE)
    {
        if(!createImGuiDescriptorPool())
        {
            return false;
        }
    }

    IMGUI_CHECKVERSION();
    ImGui::CreateContext();
    ImGuiIO& io = ImGui::GetIO();
    io.ConfigFlags |= ImGuiConfigFlags_NavEnableKeyboard;
    io.ConfigFlags |= ImGuiConfigFlags_NavEnableGamepad;

    if(!ImGui_ImplSDL2_InitForVulkan(m_window.getSDLWindow()))
    {
        std::cerr << "ImGui_ImplSDL2_InitForVulkan failed\n";
        return false;
    }

    const auto queues = findQueueFamilies(m_physicalDevice, m_surface);
    if(!queues.graphicsFamily.has_value())
    {
        std::cerr << "Failed to locate graphics queue family for ImGui\n";
        return false;
    }

    ImGui_ImplVulkan_InitInfo initInfo{};
    initInfo.Instance = m_instance;
    initInfo.PhysicalDevice = m_physicalDevice;
    initInfo.Device = m_device;
    initInfo.QueueFamily = queues.graphicsFamily.value();
    initInfo.Queue = m_graphicsQueue;
    initInfo.DescriptorPool = m_imguiDescriptorPool;
    initInfo.MinImageCount = static_cast<uint32_t>(m_swapchainImages.size());
    initInfo.ImageCount = static_cast<uint32_t>(m_swapchainImages.size());
    initInfo.MSAASamples = VK_SAMPLE_COUNT_1_BIT;
    initInfo.CheckVkResultFn = nullptr;

    if(!ImGui_ImplVulkan_Init(&initInfo, m_renderPass))
    {
        std::cerr << "ImGui_ImplVulkan_Init failed\n";
        return false;
    }

    if(!uploadImGuiFonts())
    {
        std::cerr << "Failed to upload ImGui fonts\n";
        return false;
    }

    m_imguiInitialized = true;
    m_uiRenderPending = false;
    m_pendingUiDrawData = nullptr;
    return true;
}

void VulkanRenderer::shutdownUI()
{
    if(!m_imguiInitialized)
    {
        return;
    }

    ImGui_ImplVulkan_Shutdown();
    ImGui_ImplSDL2_Shutdown();
    ImGui::DestroyContext();

    m_imguiInitialized = false;
    m_uiRenderPending = false;
    m_pendingUiDrawData = nullptr;
}

void VulkanRenderer::beginUIPass()
{
    if(!m_imguiInitialized)
    {
        return;
    }

    ImGui_ImplVulkan_NewFrame();
    ImGui_ImplSDL2_NewFrame();
    ImGui::NewFrame();
}

void VulkanRenderer::renderUI()
{
    if(!m_imguiInitialized)
    {
        return;
    }

    ImGui::Render();
    m_pendingUiDrawData = ImGui::GetDrawData();
    m_uiRenderPending = (m_pendingUiDrawData != nullptr);
}

bool VulkanRenderer::createDeviceLocalBuffer(const void* data, std::size_t size, VkBufferUsageFlags usage, BufferResource& outBuffer)
{
    VkBuffer stagingBuffer = VK_NULL_HANDLE;
    VkDeviceMemory stagingMemory = VK_NULL_HANDLE;
    if(!createBuffer(size, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, stagingBuffer, stagingMemory))
    {
        return false;
    }

    void* mappedData = nullptr;
    vkMapMemory(m_device, stagingMemory, 0, size, 0, &mappedData);
    std::memcpy(mappedData, data, size);
    vkUnmapMemory(m_device, stagingMemory);

    if(!createBuffer(size, usage | VK_BUFFER_USAGE_TRANSFER_DST_BIT, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, outBuffer.buffer, outBuffer.memory))
    {
        vkDestroyBuffer(m_device, stagingBuffer, nullptr);
        vkFreeMemory(m_device, stagingMemory, nullptr);
        return false;
    }

    outBuffer.size = size;

    copyBuffer(stagingBuffer, outBuffer.buffer, size);

    vkDestroyBuffer(m_device, stagingBuffer, nullptr);
    vkFreeMemory(m_device, stagingMemory, nullptr);

    return true;
}

bool VulkanRenderer::createBuffer(VkDeviceSize size, VkBufferUsageFlags usage, VkMemoryPropertyFlags properties, VkBuffer& buffer, VkDeviceMemory& memory)
{
    VkBufferCreateInfo bufferInfo{};
    bufferInfo.sType = VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO;
    bufferInfo.size = size;
    bufferInfo.usage = usage;
    bufferInfo.sharingMode = VK_SHARING_MODE_EXCLUSIVE;

    if(vkCreateBuffer(m_device, &bufferInfo, nullptr, &buffer) != VK_SUCCESS)
    {
        return false;
    }

    VkMemoryRequirements memRequirements;
    vkGetBufferMemoryRequirements(m_device, buffer, &memRequirements);

    VkMemoryAllocateInfo allocInfo{};
    allocInfo.sType = VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO;
    allocInfo.allocationSize = memRequirements.size;
    allocInfo.memoryTypeIndex = findMemoryType(memRequirements.memoryTypeBits, properties);

    if(vkAllocateMemory(m_device, &allocInfo, nullptr, &memory) != VK_SUCCESS)
    {
        vkDestroyBuffer(m_device, buffer, nullptr);
        buffer = VK_NULL_HANDLE;
        return false;
    }

    vkBindBufferMemory(m_device, buffer, memory, 0);

    return true;
}

void VulkanRenderer::copyBuffer(VkBuffer srcBuffer, VkBuffer dstBuffer, VkDeviceSize size)

    VkCommandBufferBeginInfo beginInfo{};
    beginInfo.sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO;
    beginInfo.flags = VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT;

    vkBeginCommandBuffer(commandBuffer, &beginInfo);

    VkBufferCopy copyRegion{};
    copyRegion.size = size;
    vkCmdCopyBuffer(commandBuffer, srcBuffer, dstBuffer, 1, &copyRegion);

    vkEndCommandBuffer(commandBuffer);

    VkSubmitInfo submitInfo{};
    submitInfo.sType = VK_STRUCTURE_TYPE_SUBMIT_INFO;
    submitInfo.commandBufferCount = 1;
    submitInfo.pCommandBuffers = &commandBuffer;

    vkQueueSubmit(m_graphicsQueue, 1, &submitInfo, VK_NULL_HANDLE);
    vkQueueWaitIdle(m_graphicsQueue);

    vkFreeCommandBuffers(m_device, m_commandPool, 1, &commandBuffer);
}

void VulkanRenderer::freeBuffer(BufferResource& bufferResource)
{
    if(bufferResource.buffer != VK_NULL_HANDLE)
    {
        vkDestroyBuffer(m_device, bufferResource.buffer, nullptr);
        bufferResource.buffer = VK_NULL_HANDLE;
    }
    if(bufferResource.memory != VK_NULL_HANDLE)
    {
        vkFreeMemory(m_device, bufferResource.memory, nullptr);
        bufferResource.memory = VK_NULL_HANDLE;
    }
    bufferResource.size = 0;
    bufferResource.usage = 0;
}

bool VulkanRenderer::createImage(uint32_t width, uint32_t height, VkFormat format, VkImageTiling tiling, VkImageUsageFlags usage, VkMemoryPropertyFlags properties, VkImage& image, VkDeviceMemory& memory)
{
    VkImageCreateInfo imageInfo{};
    imageInfo.sType = VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO;
    imageInfo.imageType = VK_IMAGE_TYPE_2D;
    imageInfo.extent.width = width;
    imageInfo.extent.height = height;
    imageInfo.extent.depth = 1;
    imageInfo.mipLevels = 1;
    imageInfo.arrayLayers = 1;
    imageInfo.format = format;
    imageInfo.tiling = tiling;
    imageInfo.initialLayout = VK_IMAGE_LAYOUT_UNDEFINED;
    imageInfo.usage = usage;
    imageInfo.samples = VK_SAMPLE_COUNT_1_BIT;
    imageInfo.sharingMode = VK_SHARING_MODE_EXCLUSIVE;

    if(vkCreateImage(m_device, &imageInfo, nullptr, &image) != VK_SUCCESS)
    {
        return false;
    }

    VkMemoryRequirements memRequirements;
    vkGetImageMemoryRequirements(m_device, image, &memRequirements);

    VkMemoryAllocateInfo allocInfo{};
    allocInfo.sType = VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO;
    allocInfo.allocationSize = memRequirements.size;
    allocInfo.memoryTypeIndex = findMemoryType(memRequirements.memoryTypeBits, properties);

    if(vkAllocateMemory(m_device, &allocInfo, nullptr, &memory) != VK_SUCCESS)
    {
        vkDestroyImage(m_device, image, nullptr);
        image = VK_NULL_HANDLE;
        return false;
    }

    vkBindImageMemory(m_device, image, memory, 0);

    return true;
}

uint32_t VulkanRenderer::findMemoryType(uint32_t typeFilter, VkMemoryPropertyFlags properties) const
{
    VkPhysicalDeviceMemoryProperties memProperties;
    vkGetPhysicalDeviceMemoryProperties(m_physicalDevice, &memProperties);

    for(uint32_t i = 0; i < memProperties.memoryTypeCount; ++i)
    {
        if((typeFilter & (1 << i)) != 0 && (memProperties.memoryTypes[i].propertyFlags & properties) == properties)
        {
            return i;
        }
    }

    throw std::runtime_error("Failed to find suitable memory type");
}

bool VulkanRenderer::createTextureSampler(VkSampler& sampler)
{
    VkSamplerCreateInfo samplerInfo{};
    samplerInfo.sType = VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO;
    samplerInfo.magFilter = VK_FILTER_NEAREST;
    samplerInfo.minFilter = VK_FILTER_NEAREST;
    samplerInfo.addressModeU = VK_SAMPLER_ADDRESS_MODE_REPEAT;
    samplerInfo.addressModeV = VK_SAMPLER_ADDRESS_MODE_REPEAT;
    samplerInfo.addressModeW = VK_SAMPLER_ADDRESS_MODE_REPEAT;
    samplerInfo.anisotropyEnable = VK_FALSE;
    samplerInfo.maxAnisotropy = 1.0f;
    samplerInfo.borderColor = VK_BORDER_COLOR_INT_OPAQUE_BLACK;
    samplerInfo.unnormalizedCoordinates = VK_FALSE;
    samplerInfo.compareEnable = VK_FALSE;
    samplerInfo.compareOp = VK_COMPARE_OP_ALWAYS;
    samplerInfo.mipmapMode = VK_SAMPLER_MIPMAP_MODE_NEAREST;
    samplerInfo.mipLodBias = 0.0f;
    samplerInfo.minLod = 0.0f;
    samplerInfo.maxLod = 0.0f;

    return vkCreateSampler(m_device, &samplerInfo, nullptr, &sampler) == VK_SUCCESS;
}

void VulkanRenderer::transitionImageLayout(VkImage image, VkFormat format, VkImageLayout oldLayout, VkImageLayout newLayout)
{
    VkCommandBuffer commandBuffer = VK_NULL_HANDLE;
    VkCommandBufferAllocateInfo allocInfo{};
    allocInfo.sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO;
    allocInfo.level = VK_COMMAND_BUFFER_LEVEL_PRIMARY;
    allocInfo.commandPool = m_commandPool;
    allocInfo.commandBufferCount = 1;
    vkAllocateCommandBuffers(m_device, &allocInfo, &commandBuffer);

    VkCommandBufferBeginInfo beginInfo{};
    beginInfo.sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO;
    beginInfo.flags = VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT;
    vkBeginCommandBuffer(commandBuffer, &beginInfo);

    VkImageMemoryBarrier barrier{};
    barrier.sType = VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER;
    barrier.oldLayout = oldLayout;
    barrier.newLayout = newLayout;
    barrier.srcQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED;
    barrier.dstQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED;
    barrier.image = image;
    barrier.subresourceRange.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
    barrier.subresourceRange.baseMipLevel = 0;
    barrier.subresourceRange.levelCount = 1;
    barrier.subresourceRange.baseArrayLayer = 0;
    barrier.subresourceRange.layerCount = 1;

    VkPipelineStageFlags sourceStage = 0;
    VkPipelineStageFlags destinationStage = 0;

    if(oldLayout == VK_IMAGE_LAYOUT_UNDEFINED && newLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
    {
        barrier.srcAccessMask = 0;
        barrier.dstAccessMask = VK_ACCESS_TRANSFER_WRITE_BIT;
        sourceStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
        destinationStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
    }
    else if(oldLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL && newLayout == VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
    {
        barrier.srcAccessMask = VK_ACCESS_TRANSFER_WRITE_BIT;
        barrier.dstAccessMask = VK_ACCESS_SHADER_READ_BIT;
        sourceStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
        destinationStage = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;
    }

    vkCmdPipelineBarrier(commandBuffer, sourceStage, destinationStage, 0, 0, nullptr, 0, nullptr, 1, &barrier);

    vkEndCommandBuffer(commandBuffer);

    VkSubmitInfo submitInfo{};
    submitInfo.sType = VK_STRUCTURE_TYPE_SUBMIT_INFO;
    submitInfo.commandBufferCount = 1;
    submitInfo.pCommandBuffers = &commandBuffer;

    vkQueueSubmit(m_graphicsQueue, 1, &submitInfo, VK_NULL_HANDLE);
    vkQueueWaitIdle(m_graphicsQueue);

    vkFreeCommandBuffers(m_device, m_commandPool, 1, &commandBuffer);
}

void VulkanRenderer::copyBufferToImage(VkBuffer buffer, VkImage image, uint32_t width, uint32_t height)
{
    VkCommandBuffer commandBuffer = VK_NULL_HANDLE;
    VkCommandBufferAllocateInfo allocInfo{};
    allocInfo.sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO;
    allocInfo.level = VK_COMMAND_BUFFER_LEVEL_PRIMARY;
    allocInfo.commandPool = m_commandPool;
    allocInfo.commandBufferCount = 1;
    vkAllocateCommandBuffers(m_device, &allocInfo, &commandBuffer);

    VkCommandBufferBeginInfo beginInfo{};
    beginInfo.sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO;
    beginInfo.flags = VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT;
    vkBeginCommandBuffer(commandBuffer, &beginInfo);

    VkBufferImageCopy region{};
    region.bufferOffset = 0;
    region.bufferRowLength = 0;
    region.bufferImageHeight = 0;
    region.imageSubresource.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
    region.imageSubresource.mipLevel = 0;
    region.imageSubresource.baseArrayLayer = 0;
    region.imageSubresource.layerCount = 1;
    region.imageOffset = {0, 0, 0};
    region.imageExtent = {width, height, 1};

    vkCmdCopyBufferToImage(commandBuffer, buffer, image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, 1, &region);

    vkEndCommandBuffer(commandBuffer);

    VkSubmitInfo submitInfo{};
    submitInfo.sType = VK_STRUCTURE_TYPE_SUBMIT_INFO;
    submitInfo.commandBufferCount = 1;
    submitInfo.pCommandBuffers = &commandBuffer;

    vkQueueSubmit(m_graphicsQueue, 1, &submitInfo, VK_NULL_HANDLE);
    vkQueueWaitIdle(m_graphicsQueue);

    vkFreeCommandBuffers(m_device, m_commandPool, 1, &commandBuffer);
}

bool VulkanRenderer::createDescriptorSetLayout()
{
    std::array<VkDescriptorSetLayoutBinding, 2> bindings{};
    
    // Binding 0: Combined image sampler (atlas texture)
    bindings[0].binding = 0;
    bindings[0].descriptorType = VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
    bindings[0].descriptorCount = 1;
    bindings[0].stageFlags = VK_SHADER_STAGE_FRAGMENT_BIT;
    bindings[0].pImmutableSamplers = nullptr;

    // Binding 1: Uniform buffer (lighting params)
    bindings[1].binding = 1;
    bindings[1].descriptorType = VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER;
    bindings[1].descriptorCount = 1;
    bindings[1].stageFlags = VK_SHADER_STAGE_FRAGMENT_BIT;
    bindings[1].pImmutableSamplers = nullptr;

    VkDescriptorSetLayoutCreateInfo layoutInfo{};
    layoutInfo.sType = VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO;
    layoutInfo.bindingCount = static_cast<uint32_t>(bindings.size());
    layoutInfo.pBindings = bindings.data();

    return vkCreateDescriptorSetLayout(m_device, &layoutInfo, nullptr, &m_descriptorSetLayout) == VK_SUCCESS;
}

bool VulkanRenderer::createDescriptorPool()
{
    std::array<VkDescriptorPoolSize, 2> poolSizes{};
    poolSizes[0].type = VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
    poolSizes[0].descriptorCount = kMaxFramesInFlight;
    poolSizes[1].type = VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER;
    poolSizes[1].descriptorCount = kMaxFramesInFlight;

    VkDescriptorPoolCreateInfo poolInfo{};
    poolInfo.sType = VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO;
    poolInfo.poolSizeCount = static_cast<uint32_t>(poolSizes.size());
    poolInfo.pPoolSizes = poolSizes.data();
    poolInfo.maxSets = kMaxFramesInFlight;

    return vkCreateDescriptorPool(m_device, &poolInfo, nullptr, &m_descriptorPool) == VK_SUCCESS;
}

bool VulkanRenderer::createLightingUniformBuffer()
{
    const VkDeviceSize bufferSize = sizeof(LightingParams);

    if(!createBuffer(bufferSize, VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT,
                     VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                     m_lightingUniformBuffer, m_lightingUniformMemory))
    {
        return false;
    }

    vkMapMemory(m_device, m_lightingUniformMemory, 0, bufferSize, 0, &m_lightingUniformMapped);
    return true;
}

void VulkanRenderer::updateLightingUniformBuffer()
{
    if(m_lightingUniformMapped != nullptr)
    {
        std::memcpy(m_lightingUniformMapped, &m_lightingParams, sizeof(LightingParams));
    }
}

bool VulkanRenderer::createDescriptorSets()
{
    std::vector<VkDescriptorSetLayout> layouts(kMaxFramesInFlight, m_descriptorSetLayout);
    VkDescriptorSetAllocateInfo allocInfo{};
    allocInfo.sType = VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO;
    allocInfo.descriptorPool = m_descriptorPool;
    allocInfo.descriptorSetCount = kMaxFramesInFlight;
    allocInfo.pSetLayouts = layouts.data();

    m_descriptorSets.resize(kMaxFramesInFlight);
    if(vkAllocateDescriptorSets(m_device, &allocInfo, m_descriptorSets.data()) != VK_SUCCESS)
    {
        return false;
    }

    // Write uniform buffer descriptors
    for(size_t i = 0; i < kMaxFramesInFlight; ++i)
    {
        VkDescriptorBufferInfo bufferInfo{};
        bufferInfo.buffer = m_lightingUniformBuffer;
        bufferInfo.offset = 0;
        bufferInfo.range = sizeof(LightingParams);

        VkWriteDescriptorSet descriptorWrite{};
        descriptorWrite.sType = VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET;
        descriptorWrite.dstSet = m_descriptorSets[i];
        descriptorWrite.dstBinding = 1; // Lighting UBO binding
        descriptorWrite.dstArrayElement = 0;
        descriptorWrite.descriptorType = VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER;
        descriptorWrite.descriptorCount = 1;
        descriptorWrite.pBufferInfo = &bufferInfo;

        vkUpdateDescriptorSets(m_device, 1, &descriptorWrite, 0, nullptr);
    }

    return true;
}

} // namespace poorcraft::rendering

#endif // POORCRAFT_VULKAN_ENABLED
