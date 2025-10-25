#include "poorcraft/core/Input.h"

#include <iostream>

namespace poorcraft::core
{
namespace
{
constexpr std::size_t buttonIndex(Uint8 sdlButton)
{
    switch(sdlButton)
    {
    case SDL_BUTTON_LEFT:
        return 0;
    case SDL_BUTTON_RIGHT:
        return 1;
    case SDL_BUTTON_MIDDLE:
        return 2;
    case SDL_BUTTON_X1:
        return 3;
    case SDL_BUTTON_X2:
        return 4;
    default:
        return 0;
    }
}
} // namespace

Input::Input()
{
    SDL_SetHint(SDL_HINT_MOUSE_RELATIVE_SCALING, "0");
    SDL_SetHint(SDL_HINT_MOUSE_RELATIVE_SYSTEM_SCALE, "0");

    m_currentKeyState.fill(false);
    m_prevKeyState.fill(false);
    m_currentMouseState.fill(false);
    m_prevMouseState.fill(false);
}

void Input::reset()
{
    updatePrevState();
    m_mouseDeltaX = 0;
    m_mouseDeltaY = 0;
}

void Input::processEvent(const SDL_Event& event)
{
    switch(event.type)
    {
    case SDL_KEYDOWN:
        if(!event.key.repeat)
        {
            const SDL_Scancode scancode = event.key.keysym.scancode;
            if(scancode < SDL_NUM_SCANCODES)
            {
                m_currentKeyState[scancode] = true;
            }
        }
        break;
    case SDL_KEYUP:
    {
        const SDL_Scancode scancode = event.key.keysym.scancode;
        if(scancode < SDL_NUM_SCANCODES)
        {
            m_currentKeyState[scancode] = false;
        }
        break;
    }
    case SDL_MOUSEMOTION:
        if(m_relativeMouseMode)
        {
            m_mouseDeltaX += event.motion.xrel;
            m_mouseDeltaY += event.motion.yrel;
        }
        m_mouseX = event.motion.x;
        m_mouseY = event.motion.y;
        break;
    case SDL_MOUSEBUTTONDOWN:
    {
        const auto index = buttonIndex(event.button.button);
        if(index < kMouseButtonCount)
        {
            m_currentMouseState[index] = true;
        }
        break;
    }
    case SDL_MOUSEBUTTONUP:
    {
        const auto index = buttonIndex(event.button.button);
        if(index < kMouseButtonCount)
        {
            m_currentMouseState[index] = false;
        }
        break;
    }
    default:
        break;
    }
}

bool Input::isKeyDown(KeyCode key) const
{
    const auto scancode = toScancode(key);
    return m_currentKeyState[scancode];
}

bool Input::isKeyPressed(KeyCode key) const
{
    const auto scancode = toScancode(key);
    return m_currentKeyState[scancode] && !m_prevKeyState[scancode];
}

bool Input::isMouseButtonDown(MouseButton button) const
{
    const auto mask = toMouseButtonMask(button);
    const auto index = buttonIndex(mask);
    return index < kMouseButtonCount ? m_currentMouseState[index] : false;
}

bool Input::isMouseButtonPressed(MouseButton button) const
{
    const auto mask = toMouseButtonMask(button);
    const auto index = buttonIndex(mask);
    if(index >= kMouseButtonCount)
    {
        return false;
    }
    return m_currentMouseState[index] && !m_prevMouseState[index];
}

Input::MouseDelta Input::getMouseDelta() const
{
    return {m_mouseDeltaX, m_mouseDeltaY};
}

std::pair<int, int> Input::getMousePosition() const
{
    return {m_mouseX, m_mouseY};
}

void Input::setRelativeMouseMode(bool enabled)
{
    if(SDL_SetRelativeMouseMode(enabled ? SDL_TRUE : SDL_FALSE) != 0)
    {
        std::cerr << "Failed to set relative mouse mode: " << SDL_GetError() << std::endl;
        return;
    }

    m_relativeMouseMode = enabled;
    m_mouseDeltaX = 0;
    m_mouseDeltaY = 0;
}

bool Input::isRelativeMouseMode() const
{
    return m_relativeMouseMode;
}

void Input::updatePrevState()
{
    m_prevKeyState = m_currentKeyState;
    m_prevMouseState = m_currentMouseState;
}

SDL_Scancode Input::toScancode(KeyCode key)
{
    return static_cast<SDL_Scancode>(key);
}

Uint8 Input::toMouseButtonMask(MouseButton button)
{
    return static_cast<Uint8>(button);
}
} // namespace poorcraft::core
