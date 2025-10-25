#include "poorcraft/core/GPUInfo.h"

#include <algorithm>
#include <cctype>
#include <cstring>
#include <sstream>
#ifdef POORCRAFT_OPENGL_ENABLED
#    ifdef _WIN32
#        include <windows.h>
#    endif
#    include <SDL2/SDL.h>
#    include <SDL2/SDL_opengl.h>
#endif

namespace poorcraft::core::gpu
{
namespace
{
std::string formatDriverVersion(uint32_t version)
{
    std::ostringstream oss;
    oss << VK_VERSION_MAJOR(version) << '.' << VK_VERSION_MINOR(version) << '.' << VK_VERSION_PATCH(version);
    return oss.str();
}
}

GPUVendor vendorFromID(uint32_t vendorID)
{
    switch(vendorID)
    {
    case 0x10DE:
        return GPUVendor::NVIDIA;
    case 0x1002:
    case 0x1022:
        return GPUVendor::AMD;
    case 0x8086:
        return GPUVendor::Intel;
    case 0x106B:
        return GPUVendor::Apple;
    default:
        return GPUVendor::Unknown;
    }
}

std::string vendorToString(GPUVendor vendor)
{
    switch(vendor)
    {
    case GPUVendor::NVIDIA:
        return "NVIDIA";
    case GPUVendor::AMD:
        return "AMD";
    case GPUVendor::Intel:
        return "Intel";
    case GPUVendor::Apple:
        return "Apple";
    default:
        return "Unknown";
    }
}

GPUInfo getGPUInfo(VkPhysicalDevice device)
{
    VkPhysicalDeviceProperties properties{};
    vkGetPhysicalDeviceProperties(device, &properties);

    GPUInfo info{};
    info.vendorID = properties.vendorID;
    info.deviceID = properties.deviceID;
    info.vendor = vendorFromID(properties.vendorID);
    info.deviceName = properties.deviceName;
    info.driverVersion = formatDriverVersion(properties.driverVersion);
    return info;
}

std::vector<GPUInfo> enumerateGPUs(VkInstance instance)
{
    uint32_t deviceCount = 0;
    if(vkEnumeratePhysicalDevices(instance, &deviceCount, nullptr) != VK_SUCCESS || deviceCount == 0)
    {
        return {};
    }

    std::vector<VkPhysicalDevice> devices(deviceCount);
    if(vkEnumeratePhysicalDevices(instance, &deviceCount, devices.data()) != VK_SUCCESS)
    {
        return {};
    }

    std::vector<GPUInfo> infos;
    infos.reserve(deviceCount);
    for(const auto& device : devices)
    {
        infos.push_back(getGPUInfo(device));
    }

    return infos;
}

GPUInfo getGPUInfoFromOpenGL()
{
    GPUInfo info{};
#ifdef POORCRAFT_OPENGL_ENABLED
    const GLubyte* vendor = glGetString(GL_VENDOR);
    const GLubyte* renderer = glGetString(GL_RENDERER);

    if(vendor != nullptr)
    {
        const std::string vendorStr(reinterpret_cast<const char*>(vendor));
        const std::string lowerVendor = [vendorStr]() {
            std::string lower = vendorStr;
            std::transform(lower.begin(), lower.end(), lower.begin(), [](unsigned char c) { return static_cast<char>(std::tolower(c)); });
            return lower;
        }();

        if(lowerVendor.find("nvidia") != std::string::npos)
        {
            info.vendor = GPUVendor::NVIDIA;
        }
        else if(lowerVendor.find("amd") != std::string::npos || lowerVendor.find("ati") != std::string::npos)
        {
            info.vendor = GPUVendor::AMD;
        }
        else if(lowerVendor.find("intel") != std::string::npos)
        {
            info.vendor = GPUVendor::Intel;
        }
        else if(lowerVendor.find("apple") != std::string::npos)
        {
            info.vendor = GPUVendor::Apple;
        }
    }

    if(renderer != nullptr)
    {
        info.deviceName = reinterpret_cast<const char*>(renderer);
    }

    info.driverVersion = "OpenGL";
#else
    (void)info;
#endif
    return info;
}
} // namespace poorcraft::core::gpu
