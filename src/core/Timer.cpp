#include "poorcraft/core/Timer.h"

namespace poorcraft::core
{
Timer::Timer()
    : m_lastFrameTime(Clock::now())
    , m_windowStart(m_lastFrameTime)
    , m_frameCount(0)
    , m_fps(0.0)
    , m_instantFPS(0.0)
    , m_deltaTime(0.0)
{}

void Timer::tick()
{
    const auto now = Clock::now();
    const std::chrono::duration<double> delta = now - m_lastFrameTime;
    m_deltaTime = delta.count();
    m_lastFrameTime = now;

    if(m_deltaTime > 0.0)
    {
        m_instantFPS = 1.0 / m_deltaTime;
    }
    else
    {
        m_instantFPS = 0.0;
    }

    ++m_frameCount;

    const std::chrono::duration<double> windowDuration = now - m_windowStart;
    if(windowDuration.count() >= 0.5)
    {
        m_fps = m_frameCount / windowDuration.count();
        m_frameCount = 0;
        m_windowStart = now;
    }
}

double Timer::getFPS() const
{
    return m_fps;
}

double Timer::getInstantFPS() const
{
    return m_instantFPS;
}

double Timer::getDeltaTime() const
{
    return m_deltaTime;
}
} // namespace poorcraft::core
