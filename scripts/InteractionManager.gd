extends Node
class_name InteractionManager

signal focused_interactable_changed(old_interactable: Interactable, new_interactable: Interactable)
signal interaction_triggered(interactable: Interactable, player: Node)
signal interaction_failed(interactable: Interactable, player: Node, reason: String)
signal interactable_registered(interactable: Interactable)
signal interactable_unregistered(interactable: Interactable)
signal candidates_updated(candidates: Array[Interactable])

const GLOBAL_INTERACTION_COOLDOWN := 0.2
const MAX_INTERACTION_DISTANCE := 10.0
const RAYCAST_UPDATE_INTERVAL := 0.05
const PRIORITY_DISTANCE_WEIGHT := 1.0
const PRIORITY_MANUAL_WEIGHT := 100.0

var registered_interactables: Array[Interactable] = []
var focused_interactable: Interactable = null
var last_interaction_time: float = 0.0
var _player: Node = null
var _game_manager: Node = null
var _ui_manager: Node = null
var _focus_update_cooldown: float = 0.0
var _parent_to_interactable: Dictionary = {} # Node -> Array[Interactable]

func _ready() -> void:
    _locate_autoloads()
    _connect_signals()
    set_process(true)
    _log_info("InteractionManager initialized")

func _process(delta: float) -> void:
    if _focus_update_cooldown > 0.0:
        _focus_update_cooldown = max(0.0, _focus_update_cooldown - delta)
    _prune_invalid_interactables()

func register_interactable(interactable: Interactable) -> void:
    if interactable == null:
        _log_warning("Attempted to register null interactable")
        return
    if registered_interactables.has(interactable):
        return
    registered_interactables.append(interactable)
    var parent_node := interactable.parent_node
    if parent_node != null and is_instance_valid(parent_node):
        _add_interactable_to_parent_map(parent_node, interactable)
    emit_signal("interactable_registered", interactable)
    _log_debug("Registered interactable '%s'" % interactable.interaction_id)

func unregister_interactable(interactable: Interactable) -> void:
    if interactable == null:
        return
    if registered_interactables.erase(interactable):
        emit_signal("interactable_unregistered", interactable)
        _log_debug("Unregistered interactable '%s'" % interactable.interaction_id)
    _remove_interactable_from_parent_map(interactable)
    if focused_interactable == interactable:
        clear_focus()

func _is_node_ancestor(ancestor: Node, descendant: Node) -> bool:
    if ancestor == null or descendant == null:
        return false
    var current := descendant
    while current != null:
        if current == ancestor:
            return true
        current = current.get_parent()
    return false

func _shares_lineage(node_a: Node, node_b: Node) -> bool:
    if node_a == null or node_b == null:
        return false
    return _is_node_ancestor(node_a, node_b) or _is_node_ancestor(node_b, node_a)

func get_all_interactables() -> Array[Interactable]:
    return registered_interactables.duplicate()

func get_interactables_in_range(position: Vector3, max_range: float = MAX_INTERACTION_DISTANCE) -> Array[Interactable]:
    var results: Array[Interactable] = []
    for interactable in registered_interactables:
        if interactable == null:
            continue
        if not _is_interactable_valid(interactable, true):
            continue
        var distance := interactable.get_distance_to(position)
        if distance <= min(max_range, interactable.interaction_range):
            results.append(interactable)
    results.sort_custom(func(a: Interactable, b: Interactable) -> bool:
        return a.get_distance_to(position) < b.get_distance_to(position)
    )
    return results

func update_focus(player: Node, raycast: RayCast3D) -> void:
    if _focus_update_cooldown > 0.0:
        return
    _focus_update_cooldown = RAYCAST_UPDATE_INTERVAL

    if player != null:
        _player = player

    var candidates := get_interactables_under_crosshair(raycast, player)
    emit_signal("candidates_updated", candidates)
    if candidates.is_empty():
        clear_focus()
        return

    var interactable := candidates[0]
    if interactable == null:
        clear_focus()
        return

    if focused_interactable == interactable:
        return

    var old_focus := focused_interactable
    if old_focus != null:
        old_focus.set_focused(false, player)
    focused_interactable = interactable
    focused_interactable.set_focused(true, player)
    emit_signal("focused_interactable_changed", old_focus, focused_interactable)
    _log_debug("Focus set to '%s'" % interactable.interaction_id)

func get_focused_interactable() -> Interactable:
    return focused_interactable

func clear_focus() -> void:
    if focused_interactable != null:
        var old_focus := focused_interactable
        focused_interactable.set_focused(false, _player)
        focused_interactable = null
        emit_signal("focused_interactable_changed", old_focus, null)
        _log_debug("Focus cleared from '%s'" % old_focus.interaction_id)
    emit_signal("candidates_updated", [])

func try_interact(player: Node) -> bool:
    if not can_interact():
        return false
    var interactable := focused_interactable
    if interactable == null:
        return false
    var success := interactable.interact(player)
    if success:
        last_interaction_time = Time.get_ticks_msec()
        emit_signal("interaction_triggered", interactable, player)
    else:
        var reason := interactable.get_last_failure_reason() if interactable.has_method("get_last_failure_reason") else "interactable_rejected"
        emit_signal("interaction_failed", interactable, player, reason)
    return success

func can_interact() -> bool:
    if focused_interactable == null:
        return false
    if _player == null or not is_instance_valid(_player):
        return false
    if _game_manager != null and bool(_game_manager.get("is_paused")):
        return false
    var now_ms := Time.get_ticks_msec()
    if now_ms - last_interaction_time < int(GLOBAL_INTERACTION_COOLDOWN * 1000.0):
        return false
    return focused_interactable.can_interact(_player)

func _collect_interactables_for_collider(collider: Node) -> Array[Interactable]:
    var results: Array[Interactable] = []
    var current := collider
    var depth := 0
    while current != null and depth < 8:
        if _parent_to_interactable.has(current):
            var interactables: Array = _parent_to_interactable[current]
            for interactable in interactables:
                if interactable != null and is_instance_valid(interactable) and not results.has(interactable):
                    results.append(interactable)
        current = current.get_parent()
        depth += 1
    return results

func get_interactables_under_crosshair(raycast: RayCast3D, player: Node) -> Array[Interactable]:
    if raycast == null or not raycast.is_colliding():
        return []
    var collider_object := raycast.get_collider()
    if collider_object == null or not (collider_object is Node):
        return []
    var collider: Node = collider_object
    var candidates := _collect_interactables_for_collider(collider)
    if candidates.is_empty():
        return []

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

    var valid_candidates: Array[Interactable] = []
    for interactable in candidates:
        if interactable == null:
            continue
        if not _is_interactable_valid(interactable, true):
            continue
        if interactable.require_line_of_sight:
            var parent_node := interactable.parent_node
            if parent_node == null or not is_instance_valid(parent_node):
                continue
            if not _shares_lineage(parent_node, collider):
                continue
        if has_position:
            var distance := interactable.get_distance_to(player_position)
            if distance > min(interactable.interaction_range, MAX_INTERACTION_DISTANCE):
                continue
        valid_candidates.append(interactable)

    if valid_candidates.is_empty():
        return []

    valid_candidates.sort_custom(func(a: Interactable, b: Interactable) -> bool:
        return _calculate_priority_score(a, player_position) > _calculate_priority_score(b, player_position)
    )
    return valid_candidates

func _calculate_priority_score(interactable: Interactable, player_position: Vector3) -> float:
    var distance := interactable.get_distance_to(player_position)
    var manual_priority := float(interactable.priority) * PRIORITY_MANUAL_WEIGHT
    var distance_score := -distance * PRIORITY_DISTANCE_WEIGHT
    return manual_priority + distance_score

func _get_highest_priority_interactable(candidates: Array[Interactable], player_position: Vector3) -> Interactable:
    var best: Interactable = null
    var best_score := -INF
    for interactable in candidates:
        var score := _calculate_priority_score(interactable, player_position)
        if best == null or score > best_score:
            best = interactable
            best_score = score
    return best

func _is_interactable_valid(interactable: Interactable, allow_disabled_prompt: bool = false) -> bool:
    if interactable == null:
        return false
    if not is_instance_valid(interactable):
        return false
    if interactable.parent_node == null or not is_instance_valid(interactable.parent_node):
        return false
    if interactable.enabled:
        return true
    return allow_disabled_prompt and interactable.show_prompt_when_disabled

func _prune_invalid_interactables() -> void:
    var removed := false
    for i in range(registered_interactables.size() - 1, -1, -1):
        var interactable := registered_interactables[i]
        if not _is_interactable_valid(interactable, true):
            _remove_interactable_from_parent_map(interactable)
            registered_interactables.remove_at(i)
            removed = true
            emit_signal("interactable_unregistered", interactable)
    var parents := _parent_to_interactable.keys()
    for parent in parents:
        if not _parent_to_interactable.has(parent):
            continue
        var interactables: Array = _parent_to_interactable[parent]
        for j in range(interactables.size() - 1, -1, -1):
            var interactable := interactables[j]
            if interactable == null or not is_instance_valid(interactable) or not registered_interactables.has(interactable):
                interactables.remove_at(j)
                removed = true
        if interactables.is_empty() or parent == null or not is_instance_valid(parent):
            _parent_to_interactable.erase(parent)
            removed = true
    if removed:
        _log_debug("Pruned invalid interactables")
    if focused_interactable != null and not _is_interactable_valid(focused_interactable, true):
        clear_focus()

func _add_interactable_to_parent_map(parent_node: Node, interactable: Interactable) -> void:
    if parent_node == null or interactable == null:
        return
    if not _parent_to_interactable.has(parent_node):
        _parent_to_interactable[parent_node] = []
    var interactables: Array = _parent_to_interactable[parent_node]
    if not interactables.has(interactable):
        interactables.append(interactable)

func _remove_interactable_from_parent_map(interactable: Interactable) -> void:
    if interactable == null:
        return
    var parent_node := interactable.parent_node
    if parent_node != null and _parent_to_interactable.has(parent_node):
        var interactables: Array = _parent_to_interactable[parent_node]
        interactables.erase(interactable)
        if interactables.is_empty():
            _parent_to_interactable.erase(parent_node)
        return
    for parent in _parent_to_interactable.keys():
        var interactables: Array = _parent_to_interactable[parent]
        if interactables.erase(interactable):
            if interactables.is_empty():
                _parent_to_interactable.erase(parent)
            break

func _locate_autoloads() -> void:
    if typeof(GameManager) != TYPE_NIL and GameManager != null:
        _game_manager = GameManager
    elif get_tree().has_node("/root/GameManager"):
        _game_manager = get_tree().get_node("/root/GameManager")

    if typeof(UIManager) != TYPE_NIL and UIManager != null:
        _ui_manager = UIManager
    elif get_tree().has_node("/root/UIManager"):
        _ui_manager = get_tree().get_node("/root/UIManager")

func _connect_signals() -> void:
    if _game_manager != null and _game_manager.has_signal("player_registered"):
        if not _game_manager.player_registered.is_connected(_on_player_registered):
            _game_manager.player_registered.connect(_on_player_registered)
    if _game_manager != null and _game_manager.has_signal("game_paused"):
        if not _game_manager.game_paused.is_connected(_on_game_paused):
            _game_manager.game_paused.connect(_on_game_paused)

func _disconnect_signals() -> void:
    if _game_manager == null:
        return
    if _game_manager.has_signal("player_registered") and _game_manager.player_registered.is_connected(_on_player_registered):
        _game_manager.player_registered.disconnect(_on_player_registered)
    if _game_manager.has_signal("game_paused") and _game_manager.game_paused.is_connected(_on_game_paused):
        _game_manager.game_paused.disconnect(_on_game_paused)

func _exit_tree() -> void:
    _disconnect_signals()

func _on_player_registered(player: Node) -> void:
    _player = player
    _log_debug("Player registered with InteractionManager")

func _on_game_paused(paused: bool) -> void:
    if paused:
        clear_focus()

func _log_info(message: String) -> void:
    if typeof(ErrorLogger) != TYPE_NIL and ErrorLogger != null:
        ErrorLogger.log_info(message, "InteractionManager")
    else:
        push_warning(message)

func _log_warning(message: String) -> void:
    if typeof(ErrorLogger) != TYPE_NIL and ErrorLogger != null:
        ErrorLogger.log_warning(message, "InteractionManager")
    else:
        push_warning(message)

func _log_debug(message: String) -> void:
    if typeof(ErrorLogger) != TYPE_NIL and ErrorLogger != null:
        ErrorLogger.log_debug(message, "InteractionManager")

