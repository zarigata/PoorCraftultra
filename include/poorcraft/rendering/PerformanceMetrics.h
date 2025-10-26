#pragma once

#include <array>
#include <cstdint>

namespace poorcraft::rendering
{
struct CPUMetrics
{
    double frameTimeMs{0.0};
    double updateTimeMs{0.0};
    double cullingTimeMs{0.0};
    double renderSubmitTimeMs{0.0};
    double uiTimeMs{0.0};
};

struct GPUMetrics
{
    double renderPassTimeMs{0.0};
    double uiPassTimeMs{0.0};
    bool available{false};
};

struct RenderStats
{
    std::uint32_t totalChunks{0};
    std::uint32_t visibleChunks{0};
    std::uint32_t drawCalls{0};
    std::uint64_t verticesRendered{0};
    std::uint64_t trianglesRendered{0};
};

struct PerformanceMetrics
{
    CPUMetrics cpu{};
    GPUMetrics gpu{};
    RenderStats stats{};
    double fps{0.0};
};

} // namespace poorcraft::rendering
