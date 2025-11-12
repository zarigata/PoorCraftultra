extends Node
class_name Interactable

signal interacted(player: Node)
signal focus_gained(player: Node)
signal focus_lost(player: Node)
signal validation_failed(player: Node, reason: String)
signal prompt_changed(new_prompt: String)

@export var interaction_id: String = ""
@export var prompt_text: String = "[E] Interact" setget _set_prompt_text, _get_prompt_text_property
@export var interaction_range: float = 5.0
@export var priority: int = 0
@export var enabled: bool = true
@export var require_line_of_sight: bool = true
@export var cooldown_duration: float = 0.0
@export var interaction_type: String = "generic"
@export var custom_validation: bool = false
@export_group("Accessibility")
@export var screen_reader_text: String = ""
@export var show_prompt_when_disabled: bool = false

var is_focused: bool = false
var last_interaction_time: float = 0.0
var parent_node: Node3D = null
var _interaction_manager: Node = null
var _ui_manager: Node = null
var _last_failure_reason: String = ""
var _prompt_text: String = "[E] Interact"

func _ready() -> void:
    _cache_parent()
    _locate_autoloads()
    _validate_configuration()
    _ensure_interaction_id()
    _register_with_manager()
    _log_info("registered interactable '%s'" % interaction_id)

func _exit_tree() -> void:
    _unregister_from_manager()
    parent_node = null
    _interaction_manager = null
    _ui_manager = null
    _log_info("unregistered interactable '%s'" % interaction_id)

func can_interact(player: Node) -> bool:
    _last_failure_reason = ""
    if not enabled:
        _last_failure_reason = "disabled"
        return false
    if parent_node == null or not is_instance_valid(parent_node):
        _last_failure_reason = "parent_invalid"
        _log_warning("Parent node invalid for interactable '%s'" % interaction_id)
        return false
    if cooldown_duration > 0.0:
        var now_ms := Time.get_ticks_msec()
        if now_ms - last_interaction_time < int(cooldown_duration * 1000.0):
            _last_failure_reason = "cooldown"
            return false
    var has_position := false
    var player_position := Vector3.ZERO
    if player is Node3D:
        has_position = true
        player_position = player.global_position
    elif player != null and player.has_method("get_world_position"):
        var result_position = player.call("get_world_position")
        if typeof(result_position) == TYPE_VECTOR3:
            has_position = true
            player_position = result_position
    if has_position and not is_in_range(player_position):
        _last_failure_reason = "out_of_range"
        return false
    if custom_validation and parent_node != null and parent_node.has_method("can_interact_custom"):
        var result = parent_node.call("can_interact_custom", player)
        if typeof(result) == TYPE_BOOL:
            if not result:
                _last_failure_reason = "custom_validation"
                return false
        else:
            _last_failure_reason = "custom_validation_invalid"
            return false
    return true

func interact(player: Node) -> bool:
    if not can_interact(player):
        emit_signal("validation_failed", player, get_last_failure_reason())
        if get_last_failure_reason() == "cooldown":
            _log_debug("Interaction blocked by cooldown for '%s'" % interaction_id)
        elif get_last_failure_reason() == "out_of_range":
            _log_debug("Interaction blocked by range for '%s'" % interaction_id)
        return false
    last_interaction_time = Time.get_ticks_msec()
    emit_signal("interacted", player)
    _announce_screen_reader()
    _log_info("interaction triggered for '%s'" % interaction_id)
    return true

func get_world_position() -> Vector3:
    if parent_node != null and is_instance_valid(parent_node):
        return parent_node.global_position
    return Vector3.ZERO

func get_distance_to(position: Vector3) -> float:
    return get_world_position().distance_to(position)

func is_in_range(position: Vector3) -> bool:
    return get_distance_to(position) <= interaction_range

func set_focused(focused: bool, player: Node) -> void:
    if focused == is_focused:
        return
    is_focused = focused
    if focused:
        emit_signal("focus_gained", player)
        _log_debug("focus gained")
    else:
        emit_signal("focus_lost", player)
        _log_debug("focus lost")

func get_prompt_text() -> String:
    if enabled:
        return _prompt_text
    if show_prompt_when_disabled:
        return "%s (Disabled)" % _prompt_text
    return ""

func get_screen_reader_text() -> String:
    return screen_reader_text if screen_reader_text.strip_edges() != "" else _prompt_text

func get_last_failure_reason() -> String:
    return _last_failure_reason

func _set_prompt_text(value: String) -> void:
    var new_text := value if value != null else ""
    if typeof(new_text) != TYPE_STRING:
        new_text = str(new_text)
    if _prompt_text == new_text:
        return
    _prompt_text = new_text
    emit_signal("prompt_changed", get_prompt_text())

func _get_prompt_text_property() -> String:
    return _prompt_text

func _cache_parent() -> void:
    if parent_node != null:
        return
    var parent_candidate := get_parent()
    if parent_candidate == null:
        _log_error("Interactable requires a Node3D parent")
        return
    if parent_candidate is Node3D:
        parent_node = parent_candidate
    else:
        _log_error("Interactable parent must be Node3D, got %s" % parent_candidate.get_class())

func _locate_autoloads() -> void:
    if typeof(InteractionManager) != TYPE_NIL and InteractionManager != null:
        _interaction_manager = InteractionManager
    elif get_tree() != null and get_tree().has_node("/root/InteractionManager"):
        _interaction_manager = get_tree().get_node("/root/InteractionManager")
    if typeof(UIManager) != TYPE_NIL and UIManager != null:
        _ui_manager = UIManager
    elif get_tree() != null and get_tree().has_node("/root/UIManager"):
        _ui_manager = get_tree().get_node("/root/UIManager")

func _validate_configuration() -> void:
    if prompt_text.strip_edges() == "":
        prompt_text = "[E] Interact"
        _log_warning("Prompt text empty; default applied for '%s'" % name)
    if interaction_range <= 0.0:
        interaction_range = 0.1
        _log_warning("Interaction range too small; clamped for '%s'" % interaction_id)
    if parent_node == null:
        _log_error("Interactable missing valid Node3D parent")

func _ensure_interaction_id() -> void:
    if interaction_id.strip_edges() != "":
        return
    var base_name := parent_node.name if parent_node != null else name
    interaction_id = "%s_%d" % [base_name, get_instance_id()]

func _register_with_manager() -> void:
    if _interaction_manager == null:
        _log_warning("InteractionManager not found; interactable not registered")
        return
    if _interaction_manager.has_method("register_interactable"):
        _interaction_manager.call("register_interactable", self)

func _unregister_from_manager() -> void:
    if _interaction_manager == null:
        return
    if _interaction_manager.has_method("unregister_interactable"):
        _interaction_manager.call("unregister_interactable", self)

func _announce_screen_reader() -> void:
    if _ui_manager == null:
        return
    if _ui_manager.has_method("announce_to_screen_reader"):
        _ui_manager.call("announce_to_screen_reader", get_screen_reader_text())

func _log_info(message: String) -> void:
    if typeof(ErrorLogger) != TYPE_NIL and ErrorLogger != null:
        ErrorLogger.log_info(message, "Interactable")
    else:
        push_warning(message)

func _log_warning(message: String) -> void:
    if typeof(ErrorLogger) != TYPE_NIL and ErrorLogger != null:
        ErrorLogger.log_warning(message, "Interactable")
    else:
        push_warning(message)

func _log_error(message: String) -> void:
    if typeof(ErrorLogger) != TYPE_NIL and ErrorLogger != null:
        ErrorLogger.log_error(message, "Interactable")
    else:
        push_error(message)

func _log_debug(message: String) -> void:
    if typeof(ErrorLogger) != TYPE_NIL and ErrorLogger != null:
        ErrorLogger.log_debug(message, "Interactable")
