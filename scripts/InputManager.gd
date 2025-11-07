extends Node
class_name InputManager

signal input_action_remapped(action_name: String, new_event: InputEvent)
signal input_mode_changed(mode: InputMode)

enum InputMode { KEYBOARD_MOUSE, GAMEPAD, TOUCH }

const BUFFER_DURATION: float = 0.1
const INPUT_MAPPINGS_PATH: String = "user://input_mappings.cfg"

var current_input_mode: InputMode = InputMode.KEYBOARD_MOUSE
var input_enabled: bool = true
var action_buffer: Dictionary = {}
var _default_mappings: Dictionary = {}

func _ready() -> void:
    _capture_default_mappings()
    load_input_mappings()
    ErrorLogger.log_info("InputManager ready", "InputManager")
    ErrorLogger.log_debug("Actions available: %s" % ", ".join(get_all_actions()), "InputManager")

func _notification(what: int) -> void:
    if what == NOTIFICATION_WM_CLOSE_REQUEST:
        save_input_mappings()

func _input(event: InputEvent) -> void:
    if event is InputEventKey or event is InputEventMouseButton or event is InputEventMouseMotion:
        _set_input_mode(InputMode.KEYBOARD_MOUSE)
    elif event is InputEventJoypadButton or event is InputEventJoypadMotion:
        _set_input_mode(InputMode.GAMEPAD)
    elif event is InputEventScreenTouch or event is InputEventScreenDrag:
        _set_input_mode(InputMode.TOUCH)

func _process(delta: float) -> void:
    var to_remove := []
    for action in action_buffer.keys():
        action_buffer[action] -= delta
        if action_buffer[action] <= 0.0:
            to_remove.append(action)
    for action in to_remove:
        action_buffer.erase(action)

func buffer_action(action: String) -> void:
    action_buffer[action] = BUFFER_DURATION

func is_action_just_pressed_buffered(action: String) -> bool:
    if not input_enabled or _is_game_paused():
        return false
    if action_buffer.has(action):
        action_buffer.erase(action)
        return true
    return Input.is_action_just_pressed(action)

func is_action_pressed_safe(action: String) -> bool:
    if not input_enabled or _is_game_paused():
        return false
    return Input.is_action_pressed(action)

func get_movement_vector() -> Vector2:
    if not input_enabled or _is_game_paused():
        return Vector2.ZERO
    var x := 0.0
    var y := 0.0
    if Input.is_action_pressed("move_left"):
        x -= 1.0
    if Input.is_action_pressed("move_right"):
        x += 1.0
    if Input.is_action_pressed("move_forward"):
        y -= 1.0
    if Input.is_action_pressed("move_backward"):
        y += 1.0
    var vec := Vector2(x, y)
    if vec.length() > 1.0:
        vec = vec.normalized()
    return vec

func get_mouse_sensitivity() -> float:
    return GameManager.get_setting("controls/mouse_sensitivity", 0.3)

func remap_action(action_name: String, new_event: InputEvent) -> bool:
    if new_event == null:
        ErrorLogger.log_warning("Attempted to remap with null event", "InputManager")
        return false
    if not InputMap.has_action(action_name):
        ErrorLogger.log_error("Action %s does not exist" % action_name, "InputManager")
        return false
    InputMap.action_erase_events(action_name)
    InputMap.action_add_event(action_name, new_event)
    buffer_action(action_name)
    emit_signal("input_action_remapped", action_name, new_event)
    save_input_mappings()
    ErrorLogger.log_info("Action %s remapped" % action_name, "InputManager")
    return true

func reset_action_to_default(action_name: String) -> void:
    if not _default_mappings.has(action_name):
        ErrorLogger.log_warning("No default mapping stored for %s" % action_name, "InputManager")
        return
    InputMap.action_erase_events(action_name)
    for event in _default_mappings[action_name]:
        InputMap.action_add_event(action_name, event)
    save_input_mappings()
    ErrorLogger.log_info("Action %s reset to default" % action_name, "InputManager")

func get_action_events(action_name: String) -> Array:
    return InputMap.action_get_events(action_name)

func save_input_mappings() -> void:
    var config := ConfigFile.new()
    var actions := get_all_actions()
    for action_name in actions:
        var events := InputMap.action_get_events(action_name)
        var serializable_events := []
        for event in events:
            serializable_events.append(_serialize_input_event(event))
        config.set_value("actions", action_name, serializable_events)
    var err := config.save(INPUT_MAPPINGS_PATH)
    if err != OK:
        ErrorLogger.log_error("Failed to save input mappings (code %d)" % err, "InputManager")

func load_input_mappings() -> void:
    var config := ConfigFile.new()
    var err := config.load(INPUT_MAPPINGS_PATH)
    if err != OK:
        if err != ERR_FILE_NOT_FOUND:
            ErrorLogger.log_warning("Failed to load input mappings (code %d)" % err, "InputManager")
        return
    var actions := config.get_section_keys("actions")
    for action_name in actions:
        var event_dicts := config.get_value("actions", action_name, [])
        InputMap.action_erase_events(action_name)
        for event_dict in event_dicts:
            var event := _deserialize_input_event(event_dict)
            if event:
                InputMap.action_add_event(action_name, event)
    ErrorLogger.log_info("Custom input mappings loaded", "InputManager")

func enable_input() -> void:
    input_enabled = true

func disable_input() -> void:
    input_enabled = false
    action_buffer.clear()

func get_all_actions() -> Array:
    return InputMap.get_actions()

func handle_ui_navigation() -> void:
    pass

func vibrate_controller(strength: float, duration: float) -> void:
    pass

func get_most_recent_input_mode() -> InputMode:
    return current_input_mode

func _capture_default_mappings() -> void:
    _default_mappings.clear()
    for action_name in InputMap.get_actions():
        var events := []
        for event in InputMap.action_get_events(action_name):
            events.append(event.duplicate())
        _default_mappings[action_name] = events

func _set_input_mode(mode: InputMode) -> void:
    if current_input_mode == mode:
        return
    current_input_mode = mode
    emit_signal("input_mode_changed", mode)
    ErrorLogger.log_info("Input mode changed to %s" % InputMode.keys()[mode], "InputManager")

func _serialize_input_event(event: InputEvent) -> Dictionary:
    var data := {"class": event.get_class()}
    if event is InputEventKey:
        data.merge({
            "keycode": event.keycode,
            "physical_keycode": event.physical_keycode,
            "unicode": event.unicode,
            "pressed": event.pressed,
            "shift": event.shift_pressed,
            "alt": event.alt_pressed,
            "ctrl": event.ctrl_pressed,
            "meta": event.meta_pressed,
        })
    elif event is InputEventMouseButton:
        data.merge({
            "button_index": event.button_index,
            "pressed": event.pressed,
            "double_click": event.double_click,
        })
    elif event is InputEventJoypadButton:
        data.merge({
            "button_index": event.button_index,
            "pressed": event.pressed,
        })
    elif event is InputEventJoypadMotion:
        data.merge({
            "axis": event.axis,
            "axis_value": event.axis_value,
        })
    elif event is InputEventScreenTouch:
        data.merge({"index": event.index})
    return data

func _deserialize_input_event(data: Dictionary) -> InputEvent:
    if not data.has("class"):
        ErrorLogger.log_warning("Serialized input event missing class", "InputManager")
        return null
    var class_name := data["class"]
    var event: InputEvent
    match class_name:
        "InputEventKey":
            event = InputEventKey.new()
            event.keycode = data.get("keycode", 0)
            event.physical_keycode = data.get("physical_keycode", 0)
            event.unicode = data.get("unicode", 0)
            event.pressed = data.get("pressed", false)
            event.shift_pressed = data.get("shift", false)
            event.alt_pressed = data.get("alt", false)
            event.ctrl_pressed = data.get("ctrl", false)
            event.meta_pressed = data.get("meta", false)
        "InputEventMouseButton":
            event = InputEventMouseButton.new()
            event.button_index = data.get("button_index", 0)
            event.pressed = data.get("pressed", false)
            event.double_click = data.get("double_click", false)
        "InputEventJoypadButton":
            event = InputEventJoypadButton.new()
            event.button_index = data.get("button_index", 0)
            event.pressed = data.get("pressed", false)
        "InputEventJoypadMotion":
            event = InputEventJoypadMotion.new()
            event.axis = data.get("axis", 0)
            event.axis_value = data.get("axis_value", 0.0)
        "InputEventScreenTouch":
            event = InputEventScreenTouch.new()
            event.index = data.get("index", 0)
        _:
            ErrorLogger.log_warning("Unsupported input event type: %s" % class_name, "InputManager")
            return null
    return event

func _is_game_paused() -> bool:
    return GameManager.is_paused
