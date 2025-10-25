#include "poorcraft/core/GPUInfo.h"

#include <gtest/gtest.h>
#include <vulkan/vulkan.h>

namespace
{
VkInstance createMinimalInstance()
{
    VkApplicationInfo appInfo{};
    appInfo.sType = VK_STRUCTURE_TYPE_APPLICATION_INFO;
    appInfo.pApplicationName = "PoorCraftGPUInfoTest";
    appInfo.applicationVersion = VK_MAKE_VERSION(0, 1, 0);
    appInfo.pEngineName = "PoorCraft";
    appInfo.engineVersion = VK_MAKE_VERSION(0, 1, 0);
    appInfo.apiVersion = VK_API_VERSION_1_2;

    VkInstanceCreateInfo createInfo{};
    createInfo.sType = VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
    createInfo.pApplicationInfo = &appInfo;

#if defined(__APPLE__)
    const std::vector<const char*> extensions = {VK_KHR_PORTABILITY_ENUMERATION_EXTENSION_NAME};
    createInfo.enabledExtensionCount = static_cast<uint32_t>(extensions.size());
    createInfo.ppEnabledExtensionNames = extensions.data();
    createInfo.flags |= VK_INSTANCE_CREATE_ENUMERATE_PORTABILITY_BIT_KHR;
#endif

    VkInstance instance = VK_NULL_HANDLE;
    if(vkCreateInstance(&createInfo, nullptr, &instance) != VK_SUCCESS)
    {
        return VK_NULL_HANDLE;
    }
    return instance;
}
} // namespace

TEST(GPUDetectionTest, VendorFromID)
{
    using poorcraft::core::GPUVendor;
    using poorcraft::core::gpu::vendorFromID;

    EXPECT_EQ(vendorFromID(0x10DE), GPUVendor::NVIDIA);
    EXPECT_EQ(vendorFromID(0x1002), GPUVendor::AMD);
    EXPECT_EQ(vendorFromID(0x1022), GPUVendor::AMD);
    EXPECT_EQ(vendorFromID(0x8086), GPUVendor::Intel);
    EXPECT_EQ(vendorFromID(0x106B), GPUVendor::Apple);
    EXPECT_EQ(vendorFromID(0xFFFF), GPUVendor::Unknown);
}

TEST(GPUDetectionTest, VendorToString)
{
    using poorcraft::core::GPUVendor;
    using poorcraft::core::gpu::vendorToString;

    EXPECT_EQ(vendorToString(GPUVendor::NVIDIA), "NVIDIA");
    EXPECT_EQ(vendorToString(GPUVendor::AMD), "AMD");
    EXPECT_EQ(vendorToString(GPUVendor::Intel), "Intel");
    EXPECT_EQ(vendorToString(GPUVendor::Apple), "Apple");
    EXPECT_EQ(vendorToString(GPUVendor::Unknown), "Unknown");
}

TEST(GPUDetectionTest, EnumerateGPUs)
{
#ifdef POORCRAFT_VULKAN_ENABLED
    VkInstance instance = createMinimalInstance();
    if(instance == VK_NULL_HANDLE)
    {
        GTEST_SKIP() << "Vulkan instance creation failed; skipping GPU enumeration test.";
    }

    const auto gpus = poorcraft::core::gpu::enumerateGPUs(instance);
    vkDestroyInstance(instance, nullptr);

    ASSERT_FALSE(gpus.empty());
#else
    GTEST_SKIP() << "Vulkan backend disabled; skipping GPU enumeration test.";
#endif
}
