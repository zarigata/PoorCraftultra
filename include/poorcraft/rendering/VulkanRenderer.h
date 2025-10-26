#ifndef POORCRAFT_RENDERING_VULKANRENDERER_H
#define POORCRAFT_RENDERING_VULKANRENDERER_H

#include "poorcraft/rendering/PerformanceMetrics.h"
#include "poorcraft/rendering/Renderer.h"
#include <vulkan/vulkan.h>
#include <cstdint>
#include <limits>
#include <unordered_map>
#include <vector>

#include <array>

#include <chrono>

#include <glm/mat4x4.hpp>

struct ImDrawData;

namespace poorcraft::core
{
class Window;
}

namespace poorcraft::rendering
{
class VulkanRenderer : public Renderer
{
public:
    explicit VulkanRenderer(core::Window& window);
    ~VulkanRenderer() override = default;

    bool initialize() override;
    void shutdown() override;

    void beginFrame() override;
    void clear(float r, float g, float b, float a) override;
    void endFrame() override;

    RendererCapabilities getCapabilities() const override;
    bool isVSyncEnabled() const override;
    void setVSync(bool enabled) override;

    void setViewProjection(const glm::mat4& view, const glm::mat4& projection) override;

    BufferHandle createVertexBuffer(const void* data, std::size_t size) override;
    BufferHandle createIndexBuffer(const void* data, std::size_t size) override;
    void destroyBuffer(BufferHandle handle) override;

    void drawIndexed(BufferHandle vertexBuffer, BufferHandle indexBuffer, std::uint32_t indexCount, const glm::mat4& modelMatrix) override;

    TextureHandle createTexture(const void* data, std::uint32_t width, std::uint32_t height, std::uint32_t channels) override;
    void destroyTexture(TextureHandle handle) override;
    void bindTexture(TextureHandle handle, std::uint32_t slot) override;
    void setLightingParams(const LightingParams& params) override;

    bool initializeUI() override;
    void shutdownUI() override;
    void beginUIPass() override;
    void renderUI() override;

    void beginPerformanceCapture() override;
    void endPerformanceCapture() override;
    PerformanceMetrics getPerformanceMetrics() const override;

    VkInstance getInstance() const { return m_instance; }
    VkPhysicalDevice getPhysicalDevice() const { return m_physicalDevice; }
    core::Window& getWindow() const { return m_window; }

private:
    bool createInstance();
    bool selectPhysicalDevice();
    bool createLogicalDevice();
    bool createSurface();
    bool createSwapchain();
    bool createCommandPool();
    bool allocateCommandBuffers();
    bool createSyncObjects();
    bool createRenderPass();
    bool createPipeline();
    bool createDescriptorSetLayout();
    bool createDescriptorPool();
    bool createDescriptorSets();
    bool createLightingUniformBuffer();
    void updateLightingUniformBuffer();
    bool createTextureSampler(VkSampler& sampler);
    void transitionImageLayout(VkImage image, VkFormat format, VkImageLayout oldLayout, VkImageLayout newLayout);
    void copyBufferToImage(VkBuffer buffer, VkImage image, uint32_t width, uint32_t height);
    bool createDepthResources();
    bool createFramebuffers();
    bool recreateSwapchain();

    bool createImGuiDescriptorPool();
    bool uploadImGuiFonts();
    bool recreateImGuiBackend();
    void destroyImGuiDescriptorPool();

    void destroySwapchain();
    void destroyPipeline();
    void destroyRenderPass();
    void destroyDepthResources();
    void destroyFramebuffers();

    struct BufferResource
    {
        VkBuffer buffer{VK_NULL_HANDLE};
        VkDeviceMemory memory{VK_NULL_HANDLE};
        VkDeviceSize size{0};
        VkBufferUsageFlags usage{};
    };

    bool createDeviceLocalBuffer(const void* data, std::size_t size, VkBufferUsageFlags usage, BufferResource& outBuffer);
    bool createBuffer(VkDeviceSize size, VkBufferUsageFlags usage, VkMemoryPropertyFlags properties, VkBuffer& buffer, VkDeviceMemory& memory);
    void copyBuffer(VkBuffer srcBuffer, VkBuffer dstBuffer, VkDeviceSize size);
    void freeBuffer(BufferResource& bufferResource);
    bool createImage(uint32_t width, uint32_t height, VkFormat format, VkImageTiling tiling, VkImageUsageFlags usage, VkMemoryPropertyFlags properties, VkImage& image, VkDeviceMemory& memory);
    uint32_t findMemoryType(uint32_t typeFilter, VkMemoryPropertyFlags properties) const;

    core::Window& m_window;

    VkInstance m_instance{VK_NULL_HANDLE};
    VkDebugUtilsMessengerEXT m_debugMessenger{VK_NULL_HANDLE};
    VkPhysicalDevice m_physicalDevice{VK_NULL_HANDLE};
    VkDevice m_device{VK_NULL_HANDLE};
    VkQueue m_graphicsQueue{VK_NULL_HANDLE};
    VkQueue m_presentQueue{VK_NULL_HANDLE};
    VkSurfaceKHR m_surface{VK_NULL_HANDLE};
    VkSwapchainKHR m_swapchain{VK_NULL_HANDLE};
    std::vector<VkImage> m_swapchainImages;
    std::vector<VkImageView> m_swapchainImageViews;
    VkFormat m_swapchainImageFormat{VK_FORMAT_UNDEFINED};
    VkExtent2D m_swapchainExtent{0, 0};
    VkCommandPool m_commandPool{VK_NULL_HANDLE};
    std::vector<VkCommandBuffer> m_commandBuffers;
    std::vector<VkSemaphore> m_imageAvailableSemaphores;
    std::vector<VkSemaphore> m_renderFinishedSemaphores;
    std::vector<VkFence> m_inFlightFences;
    std::vector<VkFence> m_imagesInFlight;
    uint32_t m_currentFrame{0};
    uint32_t m_currentImageIndex{std::numeric_limits<uint32_t>::max()};
    bool m_framebufferResized{false};
    bool m_vsyncEnabled{true};
    bool m_imguiInitialized{false};

    VkRenderPass m_renderPass{VK_NULL_HANDLE};
    VkPipelineLayout m_pipelineLayout{VK_NULL_HANDLE};
    VkPipeline m_graphicsPipeline{VK_NULL_HANDLE};
    std::vector<VkFramebuffer> m_swapchainFramebuffers;

    VkFormat m_depthFormat{VK_FORMAT_D32_SFLOAT};
    std::vector<VkImage> m_depthImages;
    std::vector<VkDeviceMemory> m_depthImageMemory;
    std::vector<VkImageView> m_depthImageViews;

    glm::mat4 m_viewMatrix{1.0f};
    glm::mat4 m_projectionMatrix{1.0f};
    glm::mat4 m_viewProjection{1.0f};

    struct DrawCommand
    {
        BufferHandle vertexBuffer{};
        BufferHandle indexBuffer{};
        std::uint32_t indexCount{};
        glm::mat4 modelMatrix{1.0f};
    };

    std::vector<DrawCommand> m_drawCommands;
    glm::vec4 m_clearColor{0.0f, 0.0f, 0.0f, 1.0f};

    BufferHandle m_nextBufferHandle{1};
    std::unordered_map<BufferHandle, BufferResource> m_vertexBuffers;
    std::unordered_map<BufferHandle, BufferResource> m_indexBuffers;

    struct TextureResource {
        VkImage image{VK_NULL_HANDLE};
        VkDeviceMemory memory{VK_NULL_HANDLE};
        VkImageView imageView{VK_NULL_HANDLE};
        VkSampler sampler{VK_NULL_HANDLE};
        uint32_t width{0};
        uint32_t height{0};
    };

    TextureHandle m_nextTextureHandle{1};
    std::unordered_map<TextureHandle, TextureResource> m_textures;

    VkDescriptorSetLayout m_descriptorSetLayout{VK_NULL_HANDLE};
    VkDescriptorPool m_descriptorPool{VK_NULL_HANDLE};
    std::vector<VkDescriptorSet> m_descriptorSets;

    VkBuffer m_lightingUniformBuffer{VK_NULL_HANDLE};
    VkDeviceMemory m_lightingUniformMemory{VK_NULL_HANDLE};
    void* m_lightingUniformMapped{nullptr};
    LightingParams m_lightingParams{};

    VkDescriptorPool m_imguiDescriptorPool{VK_NULL_HANDLE};
    ImDrawData* m_pendingUiDrawData{nullptr};
    bool m_uiRenderPending{false};

    VkQueryPool m_timestampQueryPool{VK_NULL_HANDLE};
    std::vector<std::uint64_t> m_timestampResults;
    bool m_timestampsSupported{false};
    std::chrono::high_resolution_clock::time_point m_frameCaptureStart{};
    PerformanceMetrics m_currentMetrics{};
    PerformanceMetrics m_smoothedMetrics{};
    std::array<PerformanceMetrics, 60> m_metricsHistory{};
    std::size_t m_metricsHistoryIndex{0};
    std::size_t m_metricsHistoryCount{0};
    bool m_performanceCaptureActive{false};
    double m_timestampPeriodNs{0.0};
    std::uint32_t m_lastDrawCallCount{0};
    std::uint64_t m_lastVertexCount{0};
    std::uint64_t m_lastTriangleCount{0};

    bool createTimestampQueryPool();
    void destroyTimestampQueryPool();
    void recordTimestamp(std::uint32_t queryIndex, VkPipelineStageFlagBits stage);
    double computeTimestampDelta(std::uint32_t startIndex, std::uint32_t endIndex) const;
};
} // namespace poorcraft::rendering

#endif // POORCRAFT_RENDERING_VULKANRENDERER_H





