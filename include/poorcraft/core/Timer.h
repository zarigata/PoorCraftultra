#ifndef POORCRAFT_CORE_TIMER_H
#define POORCRAFT_CORE_TIMER_H

#include <chrono>

namespace poorcraft::core
{
class Timer
{
public:
    Timer();

    void tick();

    double getFPS() const;
    double getInstantFPS() const;
    double getDeltaTime() const;

private:
    using Clock = std::chrono::high_resolution_clock;

    Clock::time_point m_lastFrameTime;
    Clock::time_point m_windowStart;
    int m_frameCount;
    double m_fps;
    double m_instantFPS;
    double m_deltaTime;
};
} // namespace poorcraft::core

#endif // POORCRAFT_CORE_TIMER_H
