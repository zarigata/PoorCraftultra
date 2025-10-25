#ifndef POORCRAFT_CORE_INPUT_H
#define POORCRAFT_CORE_INPUT_H

#include <array>
#include <utility>

#include <SDL2/SDL.h>

namespace poorcraft::core
{
class Input
{
public:
    enum class KeyCode
    {
        W = SDL_SCANCODE_W,
        A = SDL_SCANCODE_A,
        S = SDL_SCANCODE_S,
        D = SDL_SCANCODE_D,
        Space = SDL_SCANCODE_SPACE,
        LeftShift = SDL_SCANCODE_LSHIFT,
        LeftCtrl = SDL_SCANCODE_LCTRL,
        Escape = SDL_SCANCODE_ESCAPE,
        Tab = SDL_SCANCODE_TAB,
        F = SDL_SCANCODE_F,
        Key1 = SDL_SCANCODE_1,
        Key2 = SDL_SCANCODE_2,
        Key3 = SDL_SCANCODE_3,
        Key4 = SDL_SCANCODE_4,
        Key5 = SDL_SCANCODE_5,
        Key6 = SDL_SCANCODE_6,
        Key7 = SDL_SCANCODE_7,
        Key8 = SDL_SCANCODE_8,
        Key9 = SDL_SCANCODE_9
    };

    enum class MouseButton
    {
        Left = SDL_BUTTON_LEFT,
        Right = SDL_BUTTON_RIGHT,
        Middle = SDL_BUTTON_MIDDLE
    };

    struct MouseDelta
    {
        int x;
        int y;
    };

    Input();

    void reset();
    void processEvent(const SDL_Event& event);

    bool isKeyDown(KeyCode key) const;
    bool isKeyPressed(KeyCode key) const;

    bool isMouseButtonDown(MouseButton button) const;
    bool isMouseButtonPressed(MouseButton button) const;

    MouseDelta getMouseDelta() const;
    std::pair<int, int> getMousePosition() const;

    void setRelativeMouseMode(bool enabled);
    bool isRelativeMouseMode() const;

private:
    static constexpr std::size_t kMouseButtonCount = 5;

    void updatePrevState();
    static SDL_Scancode toScancode(KeyCode key);
    static Uint8 toMouseButtonMask(MouseButton button);

    std::array<bool, SDL_NUM_SCANCODES> m_currentKeyState{};
    std::array<bool, SDL_NUM_SCANCODES> m_prevKeyState{};

    std::array<bool, kMouseButtonCount> m_currentMouseState{};
    std::array<bool, kMouseButtonCount> m_prevMouseState{};

    int m_mouseDeltaX{0};
    int m_mouseDeltaY{0};
    int m_mouseX{0};
    int m_mouseY{0};
    bool m_relativeMouseMode{false};
};
} // namespace poorcraft::core

#endif // POORCRAFT_CORE_INPUT_H
