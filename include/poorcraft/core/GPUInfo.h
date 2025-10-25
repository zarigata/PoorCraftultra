#ifndef POORCRAFT_CORE_GPUINFO_H
#define POORCRAFT_CORE_GPUINFO_H

#include <string>
#include <vector>
#include <vulkan/vulkan.h>

namespace poorcraft::core
{
enum class GPUVendor
{
    Unknown,
    NVIDIA,
    AMD,
    Intel,
    Apple
};

struct GPUInfo
{
    GPUVendor vendor{GPUVendor::Unknown};
    std::string deviceName{};
    uint32_t vendorID{0};
    uint32_t deviceID{0};
    std::string driverVersion{};
};

namespace gpu
{
std::vector<GPUInfo> enumerateGPUs(VkInstance instance);
GPUInfo getGPUInfo(VkPhysicalDevice device);

GPUVendor vendorFromID(uint32_t vendorID);
std::string vendorToString(GPUVendor vendor);

GPUInfo getGPUInfoFromOpenGL();
} // namespace gpu
} // namespace poorcraft::core

#endif // POORCRAFT_CORE_GPUINFO_H
