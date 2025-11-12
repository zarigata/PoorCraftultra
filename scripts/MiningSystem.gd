extends Node
class_name MiningSystem

signal mining_started(target_position: Vector3, voxel_type: Dictionary)
signal mining_progress(progress: float)
signal mining_completed(target_position: Vector3, drops: Array)
signal mining_cancelled()
signal tool_changed(new_tool: Tool)
signal tool_broke(broken_tool: Tool)
signal edit_applied(edit: Dictionary)
signal edit_undone(edit: Dictionary)

enum MiningState { IDLE, TARGETING, MINING, COOLDOWN }
enum EditOperation { MINE, PLACE, FILL }

const MAX_UNDO_STACK_SIZE := 50
const MAX_MINING_DISTANCE := 10.0
const EDIT_RATE_LIMIT := 10.0
const MAX_EDIT_RADIUS := 5.0
# Snapshot guard to prevent excessive memory usage during large edits; tune via profiling.
const MAX_SNAPSHOT_VOXELS := 4096
const COOLDOWN_DURATION := 0.1
const DEFAULT_TOOL_PATH := "res://resources/tools/tool_hand.tres"
const DEFAULT_SOUND_CATEGORY := "default"
const MIN_MINING_DURATION := 0.1
const DROPPED_ITEM_SCENE_PATH := "res://scenes/dropped_item.tscn"
const BOUNDS_MIN := Vector3(-10000.0, -10000.0, -10000.0)
const BOUNDS_MAX := Vector3(10000.0, 10000.0, 10000.0)

var current_state: MiningState = MiningState.IDLE
var active_tool: Tool = null
var mining_progress: float = 0.0
var mining_target_position: Vector3 = Vector3.ZERO
var mining_edit_center: Vector3 = Vector3.ZERO
var mining_target_voxel: Dictionary = {}
var mining_duration: float = 0.0
var mining_elapsed: float = 0.0
var cooldown_timer: float = 0.0
var undo_stack: Array[Dictionary] = []
var redo_stack: Array[Dictionary] = []
var mining_sound_player: AudioStreamPlayer3D = null

var _player: Node = null
var _voxel_world: VoxelWorld = null
var _audio_manager: AudioManager = null
var _game_manager: GameManager = null
var _rng := RandomNumberGenerator.new()
var _sound_cache: Dictionary = {}
var _current_sound_category: String = DEFAULT_SOUND_CATEGORY
var _has_mining_edit_center: bool = false
var _edit_token_capacity: float = EDIT_RATE_LIMIT
var _edit_token_fill_rate: float = EDIT_RATE_LIMIT
var _edit_tokens: float = EDIT_RATE_LIMIT

func _ready() -> void:
	_rng.randomize()
	_cache_references()
	_load_default_tool()
	_edit_token_capacity = max(EDIT_RATE_LIMIT, 1.0)
	_edit_token_fill_rate = _edit_token_capacity
	_edit_tokens = _edit_token_capacity
	set_process(true)
	if _game_manager != null and not _game_manager.player_registered.is_connected(_on_player_registered):
		_game_manager.player_registered.connect(_on_player_registered)
	ErrorLogger.log_info("MiningSystem initialized", "MiningSystem")

func _process(delta: float) -> void:
	if cooldown_timer > 0.0:
		cooldown_timer = max(cooldown_timer - delta, 0.0)
		if cooldown_timer == 0.0 and current_state == MiningState.COOLDOWN:
			_change_state(MiningState.IDLE)
	if _edit_token_capacity > 0.0:
		_edit_tokens = clamp(_edit_tokens + (_edit_token_fill_rate * delta), 0.0, _edit_token_capacity)
	if current_state == MiningState.MINING:
		mining_elapsed += delta
		if mining_duration > 0.0:
			mining_progress = clamp(mining_elapsed / mining_duration, 0.0, 1.0)
		else:
			mining_progress = 1.0
		emit_signal("mining_progress", mining_progress)
		_update_mining_sound(mining_progress)
		if mining_progress >= 1.0:
			_complete_mining()

func set_active_tool(tool: Tool) -> void:
	if tool == null:
		ErrorLogger.log_warning("Attempted to set null tool", "MiningSystem")
		return
	if current_state == MiningState.MINING:
		stop_mining()
	active_tool = tool.duplicate_tool() if tool.has_method("duplicate_tool") else tool
	mining_progress = 0.0
	emit_signal("tool_changed", active_tool)
	ErrorLogger.log_info("Active tool set to %s" % active_tool.tool_name, "MiningSystem")

func get_active_tool() -> Tool:
	return active_tool

func get_tool_durability_percentage() -> float:
	if active_tool == null:
		return 0.0
	return active_tool.get_durability_percentage()

func can_start_mining() -> bool:
	if current_state != MiningState.IDLE:
		return false
	if cooldown_timer > 0.0:
		return false
	if _is_paused():
		return false
	return active_tool != null and not active_tool.is_broken()

func start_mining(player: Node) -> bool:
	if not can_start_mining():
		return false
	if player == null:
		ErrorLogger.log_warning("Mining start requested with null player", "MiningSystem")
		return false
	_player = player
	var ray := _get_player_raycast(player)
	if ray == null or not ray.is_colliding():
		return false
	var collider := ray.get_collider()
	var point := ray.get_collision_point()
	if not _is_within_bounds(point):
		ErrorLogger.log_warning("Mining target out of bounds: %s" % point, "MiningSystem")
		return false
	var player_position := player.global_position if player is Node3D else Vector3.ZERO
	var reach_origin := player_position
	if player.has_method("get_head_position"):
		reach_origin = player.get_head_position()
	elif player.has_method("get_camera_global_position"):
		reach_origin = player.get_camera_global_position()
	elif player.has_method("get_camera_position"):
		reach_origin = player.get_camera_position()
	if reach_origin.distance_to(point) > MAX_MINING_DISTANCE:
		return false
	var voxel_world := _get_voxel_world()
	if voxel_world == null:
		ErrorLogger.log_error("VoxelWorld reference missing for mining", "MiningSystem")
		return false
	if not _is_valid_voxel_world_collider(collider, voxel_world):
		return false
	var normal := ray.get_collision_normal()
	var query_point := _compute_voxel_query_point(point, normal)
	var voxel_type := voxel_world.get_voxel_type_at(query_point)
	if voxel_type.is_empty() and query_point != point:
		voxel_type = voxel_world.get_voxel_type_at(point)
	if voxel_type.is_empty():
		ErrorLogger.log_warning("Voxel type not found at mining point", "MiningSystem")
		return false
	if not _can_mine_voxel(voxel_type, active_tool):
		ErrorLogger.log_warning("Tool %s cannot mine voxel %s" % [active_tool.tool_name, voxel_type.get("name", "Unknown")], "MiningSystem")
		return false
	var duration := _calculate_mining_duration(voxel_type, active_tool)
	if duration == INF:
		return false
	mining_target_position = point
	mining_edit_center = query_point if _is_within_bounds(query_point) else point
	_has_mining_edit_center = true
	mining_target_voxel = voxel_type.duplicate(true)
	mining_duration = max(duration, MIN_MINING_DURATION)
	mining_elapsed = 0.0
	mining_progress = 0.0
	_change_state(MiningState.MINING)
	_start_mining_sound(voxel_type, mining_target_position)
	emit_signal("mining_started", mining_target_position, mining_target_voxel)
	ErrorLogger.log_debug("Mining started at %s" % mining_target_position, "MiningSystem")
	return true

func stop_mining() -> void:
	if current_state != MiningState.MINING:
		return
	_stop_mining_sound()
	mining_progress = 0.0
	mining_elapsed = 0.0
	mining_edit_center = Vector3.ZERO
	_has_mining_edit_center = false
	_change_state(MiningState.IDLE)
	emit_signal("mining_cancelled")
	ErrorLogger.log_debug("Mining cancelled", "MiningSystem")

func undo_last_edit() -> bool:
	if undo_stack.is_empty():
		return false
	var record := undo_stack.pop_back()
	var snapshot: Dictionary = record.get("snapshot", {})
	var edit: Dictionary = record.get("edit", {})
	if _voxel_world == null:
		_voxel_world = _get_voxel_world()
	if _voxel_world == null:
		ErrorLogger.log_error("VoxelWorld unavailable for undo", "MiningSystem")
		return false
	var restored := _voxel_world.restore_voxel_snapshot(snapshot)
	if not restored:
		ErrorLogger.log_warning("Failed to restore voxel snapshot during undo", "MiningSystem")
		return false
	redo_stack.append(record)
	emit_signal("edit_undone", edit)
	ErrorLogger.log_info("Undo applied", "MiningSystem")
	return true

func redo_last_edit() -> bool:
	if redo_stack.is_empty():
		return false
	var record := redo_stack.pop_back()
	var edit: Dictionary = record.get("edit", {})
	var center: Vector3 = edit.get("center", Vector3.ZERO)
	var radius: float = edit.get("radius", 1.0)
	var operation: int = edit.get("operation", EditOperation.MINE)
	var value: float = edit.get("value", 0.0)
	var reapplied := _apply_voxel_edit(center, radius, operation, value, edit.duplicate(true))
	if reapplied:
		ErrorLogger.log_info("Redo applied", "MiningSystem")
	return reapplied

func clear_undo_history() -> void:
	undo_stack.clear()
	redo_stack.clear()
	ErrorLogger.log_debug("Undo history cleared", "MiningSystem")

func get_undo_stack_size() -> int:
	return undo_stack.size()

func get_current_state() -> int:
	return int(current_state)

func _complete_mining() -> void:
	if _voxel_world == null:
		_voxel_world = _get_voxel_world()
	if _voxel_world == null:
		ErrorLogger.log_error("VoxelWorld unavailable to complete mining", "MiningSystem")
		stop_mining()
		return
	mining_progress = 1.0
	emit_signal("mining_progress", mining_progress)
	var radius := active_tool.mining_radius if active_tool != null else 1.0
	radius = clamp(radius, 0.01, MAX_EDIT_RADIUS)
	var edit_center := mining_target_position
	if _has_mining_edit_center and _is_within_bounds(mining_edit_center):
		edit_center = mining_edit_center
	var applied := _apply_voxel_edit(edit_center, radius, EditOperation.MINE, -1.0)
	if not applied:
		stop_mining()
		return
	var tool_broken := false
	if active_tool != null:
		var usable := active_tool.use_durability(1)
		if not usable:
			tool_broken = true
			emit_signal("tool_broke", active_tool)
			ErrorLogger.log_info("Tool broke: %s" % active_tool.tool_name, "MiningSystem")
			_load_default_tool()
	var drops := _calculate_drops(mining_target_voxel)
	_emit_completion(mining_target_position, drops)
	_stop_mining_sound()
	_play_completion_sound(mining_target_voxel, mining_target_position)
	_change_state(MiningState.COOLDOWN)
	cooldown_timer = COOLDOWN_DURATION
	redo_stack.clear()
	mining_edit_center = Vector3.ZERO
	_has_mining_edit_center = false

func _apply_voxel_edit(center: Vector3, radius: float, operation: int, value: float = 0.0, descriptor_override: Dictionary = {}) -> bool:
	if _voxel_world == null:
		_voxel_world = _get_voxel_world()
	if _voxel_world == null:
		ErrorLogger.log_error("VoxelWorld unavailable for voxel edit", "MiningSystem")
		return false
	if radius <= 0.0 or radius > MAX_EDIT_RADIUS:
		ErrorLogger.log_warning("Edit radius %.2f invalid" % radius, "MiningSystem")
		return false
	if not _is_within_bounds(center):
		ErrorLogger.log_warning("Voxel edit center out of bounds: %s" % center, "MiningSystem")
		return false
	if not _validate_edit_rate():
		return false
	var descriptor := descriptor_override.duplicate(true) if not descriptor_override.is_empty() else {}
	var is_new_edit := descriptor_override.is_empty()
	if descriptor.is_empty():
		descriptor = {
			"center": center,
			"radius": radius,
			"operation": operation,
			"value": value,
		}
	descriptor["timestamp"] = Time.get_ticks_msec()
	var snapshot := _voxel_world.capture_voxel_snapshot(center, radius)
	if snapshot.size() > MAX_SNAPSHOT_VOXELS:
		ErrorLogger.log_warning("Voxel snapshot too large (%d) for edit; limit is %d" % [snapshot.size(), MAX_SNAPSHOT_VOXELS], "MiningSystem")
		return false
	var applied := _voxel_world.apply_edit(descriptor)
	if not applied:
		ErrorLogger.log_warning("Voxel edit failed", "MiningSystem")
		return false
	var record := {
		"edit": descriptor,
		"snapshot": snapshot,
	}
	undo_stack.append(record)
	if undo_stack.size() > MAX_UNDO_STACK_SIZE:
		undo_stack.pop_front()
	if is_new_edit:
		redo_stack.clear()
	emit_signal("edit_applied", descriptor)
	return true

func _calculate_mining_duration(voxel_type: Dictionary, tool: Tool) -> float:
	if voxel_type.is_empty() or tool == null:
		return INF
	var base_time := float(voxel_type.get("mining_time_base", 1.0))
	var hardness := max(float(voxel_type.get("hardness", 1.0)), 0.1)
	var efficiency := tool.get_efficiency_for_voxel(voxel_type.get("name", ""))
	if efficiency <= 0.0:
		return INF
	var duration := (base_time * hardness) / max(tool.mining_speed * efficiency, 0.01)
	return max(duration, MIN_MINING_DURATION)

func _can_mine_voxel(voxel_type: Dictionary, tool: Tool) -> bool:
	if voxel_type.is_empty() or tool == null:
		return false
	if not voxel_type.get("is_solid", true):
		return false
	var required_tier := int(voxel_type.get("required_tool_tier", 0))
	if tool.tool_tier < required_tier:
		return false
	return tool.can_mine_voxel(voxel_type.get("name", ""))

func _calculate_drops(voxel_type: Dictionary) -> Array:
	var drops: Array = []
	var definitions: Array = voxel_type.get("drops", [])
	for definition in definitions:
		if typeof(definition) != TYPE_DICTIONARY:
			continue
		var chance := float(definition.get("chance", 1.0))
		if chance <= 0.0:
			continue
		if _rng.randf() > chance:
			continue
		var min_quantity := int(definition.get("quantity_min", 1))
		var max_quantity := int(definition.get("quantity_max", min_quantity))
		if max_quantity < min_quantity:
			max_quantity = min_quantity
		var quantity := _rng.randi_range(min_quantity, max_quantity)
		drops.append({
			"item_id": String(definition.get("item_id", "")),
			"quantity": quantity,
		})
	return drops

func _start_mining_sound(voxel_type: Dictionary, position: Vector3) -> void:
	_stop_mining_sound()
	var category := String(voxel_type.get("mining_sound", DEFAULT_SOUND_CATEGORY))
	var stream := _get_sound_stream("%s_loop" % category)
	if stream == null and category != DEFAULT_SOUND_CATEGORY:
		stream = _get_sound_stream("%s_loop" % DEFAULT_SOUND_CATEGORY)
	if stream == null:
		return
	_current_sound_category = category
	if _audio_manager != null:
		mining_sound_player = _audio_manager.play_sfx_3d(stream, position, -6.0, 32.0)
	else:
		mining_sound_player = AudioStreamPlayer3D.new()
		mining_sound_player.stream = stream
		mining_sound_player.bus = "SFX"
		add_child(mining_sound_player)
		mining_sound_player.global_position = position
		mining_sound_player.volume_db = -6.0
		mining_sound_player.play()

func _update_mining_sound(progress: float) -> void:
	if mining_sound_player == null:
		return
	var volume := lerp(-10.0, -2.0, clamp(progress, 0.0, 1.0))
	mining_sound_player.volume_db = volume

func _stop_mining_sound() -> void:
	if mining_sound_player == null:
		return
	mining_sound_player.stop()
	if _audio_manager == null and mining_sound_player.is_inside_tree():
		mining_sound_player.queue_free()
	mining_sound_player = null

func _play_completion_sound(voxel_type: Dictionary, position: Vector3) -> void:
	var category := String(voxel_type.get("mining_sound", DEFAULT_SOUND_CATEGORY))
	var stream := _get_sound_stream("%s_break" % category)
	if stream == null and category != DEFAULT_SOUND_CATEGORY:
		stream = _get_sound_stream("%s_break" % DEFAULT_SOUND_CATEGORY)
	if stream == null:
		return
	if _audio_manager != null:
		_audio_manager.play_sfx_3d(stream, position, -4.0, 32.0)
		return
	var player := AudioStreamPlayer3D.new()
	player.stream = stream
	player.bus = "SFX"
	player.global_position = position
	player.volume_db = -4.0
	add_child(player)
	player.finished.connect(player.queue_free)
	player.play()

func _get_sound_stream(key: String) -> AudioStream:
	if _sound_cache.has(key):
		return _sound_cache[key]
	var path := "res://assets/audio/sfx/mining/%s.ogg" % key
	if not ResourceLoader.exists(path):
		ErrorLogger.log_warning("Mining sound missing: %s" % path, "MiningSystem")
		_sound_cache[key] = null
		return null
	var stream := ResourceLoader.load(path)
	if stream is AudioStreamOggVorbis:
		(stream as AudioStreamOggVorbis).loop = key.ends_with("_loop")
	elif stream is AudioStreamWAV and key.ends_with("_loop"):
		(stream as AudioStreamWAV).loop_mode = AudioStreamWAV.LOOP_FORWARD
	_sound_cache[key] = stream
	return stream

func _emit_completion(position: Vector3, drops: Array) -> void:
	_spawn_item_drops(position, drops)
	emit_signal("mining_completed", position, drops)
	ErrorLogger.log_debug("Mining complete with %d drops" % drops.size(), "MiningSystem")

func _spawn_item_drops(position: Vector3, drops: Array) -> void:
	if drops.is_empty():
		return
	if not ResourceLoader.exists(DROPPED_ITEM_SCENE_PATH):
		ErrorLogger.log_warning("Dropped item scene missing at %s; skipping item spawn" % DROPPED_ITEM_SCENE_PATH, "MiningSystem")
		return
	var dropped_scene := ResourceLoader.load(DROPPED_ITEM_SCENE_PATH)
	if dropped_scene == null or not (dropped_scene is PackedScene):
		ErrorLogger.log_warning("Failed to load dropped item scene as PackedScene", "MiningSystem")
		return
	var parent: Node = _voxel_world
	if parent == null or not parent.is_inside_tree():
		parent = get_tree().current_scene
	if parent == null:
		ErrorLogger.log_warning("No valid parent for dropped items", "MiningSystem")
		return
	for drop in drops:
		if typeof(drop) != TYPE_DICTIONARY:
			continue
		var item_id := String(drop.get("item_id", ""))
		var quantity := int(drop.get("quantity", 0))
		if item_id.is_empty() or quantity <= 0:
			continue
		var instance := dropped_scene.instantiate()
		if instance == null:
			continue
		if instance.has_method("set_item_data"):
			instance.set_item_data(item_id, quantity)
		parent.add_child(instance)
		var offset := Vector3(
			_rng.randf_range(-0.5, 0.5),
			_rng.randf_range(0.2, 0.6),
			_rng.randf_range(-0.5, 0.5)
		)
		if instance is Node3D:
			(instance as Node3D).global_position = position + offset
		if instance is RigidBody3D:
			var impulse := Vector3(
				_rng.randf_range(-1.0, 1.0),
				_rng.randf_range(0.5, 1.5),
				_rng.randf_range(-1.0, 1.0)
			) * 2.0
			(instance as RigidBody3D).apply_impulse(Vector3.ZERO, impulse)

func _validate_edit_rate() -> bool:
	if _edit_token_capacity <= 0.0:
		return true
	if _edit_tokens < 1.0:
		ErrorLogger.log_warning("Edit rate limit exceeded", "MiningSystem")
		return false
	_edit_tokens -= 1.0
	return true

func _is_within_bounds(position: Vector3) -> bool:
	return position.x >= BOUNDS_MIN.x and position.x <= BOUNDS_MAX.x and position.y >= BOUNDS_MIN.y and position.y <= BOUNDS_MAX.y and position.z >= BOUNDS_MIN.z and position.z <= BOUNDS_MAX.z

func _get_player_raycast(player: Node) -> RayCast3D:
	if player.has_method("get_interaction_ray"):
		return player.get_interaction_ray()
	return null

func _is_valid_voxel_world_collider(collider: Object, voxel_world: VoxelWorld) -> bool:
	if collider == null or voxel_world == null:
		return false
	if collider == voxel_world:
		return true
	var collider_node := collider if collider is Node else null
	if collider_node == null:
		return false
	if voxel_world.terrain != null and collider_node == voxel_world.terrain:
		return true
	if voxel_world.is_ancestor_of(collider_node):
		return true
	if voxel_world.terrain != null and voxel_world.terrain.is_ancestor_of(collider_node):
		return true
	return false

func _compute_voxel_query_point(point: Vector3, normal: Vector3) -> Vector3:
	if normal == Vector3.ZERO:
		return point
	var offset_distance := active_tool != null ? active_tool.mining_radius * 0.5 : 0.5
	if offset_distance <= 0.0:
		offset_distance = 0.5
	return point - normal.normalized() * offset_distance

func _get_voxel_world() -> VoxelWorld:
	if _voxel_world != null:
		return _voxel_world
	if _game_manager != null:
		_voxel_world = _game_manager.get_current_world()
	return _voxel_world

func _load_default_tool() -> void:
	var resource := ResourceLoader.load(DEFAULT_TOOL_PATH)
	if resource == null:
		ErrorLogger.log_error("Default tool resource missing", "MiningSystem")
		return
	if resource is Tool:
		active_tool = (resource as Tool).duplicate_tool()
	else:
		ErrorLogger.log_error("Default tool resource invalid type", "MiningSystem")
		return
	emit_signal("tool_changed", active_tool)
	ErrorLogger.log_info("Default tool equipped", "MiningSystem")

func _cache_references() -> void:
	var tree := get_tree()
	if typeof(GameManager) != TYPE_NIL and GameManager != null:
		_game_manager = GameManager
	elif tree and tree.has_node("/root/GameManager"):
		_game_manager = tree.get_node("/root/GameManager")
	if typeof(AudioManager) != TYPE_NIL and AudioManager != null:
		_audio_manager = AudioManager
	elif tree and tree.has_node("/root/AudioManager"):
		_audio_manager = tree.get_node("/root/AudioManager")
	if _game_manager != null:
		_player = _game_manager.get_player()
		_voxel_world = _game_manager.get_current_world()

func _on_player_registered(player: Node) -> void:
	_player = player
	_voxel_world = _get_voxel_world()
	ErrorLogger.log_debug("Player reference updated", "MiningSystem")

func _change_state(new_state: MiningState) -> void:
	if current_state == new_state:
		return
	var old_name := _state_name(current_state)
	var new_name := _state_name(new_state)
	current_state = new_state
	ErrorLogger.log_debug("Mining state changed: %s -> %s" % [old_name, new_name], "MiningSystem")

func _state_name(state: MiningState) -> String:
	match state:
		MiningState.IDLE:
			return "IDLE"
		MiningState.TARGETING:
			return "TARGETING"
		MiningState.MINING:
			return "MINING"
		MiningState.COOLDOWN:
			return "COOLDOWN"
		_:
			return "UNKNOWN"

func _is_paused() -> bool:
	return _game_manager != null and _game_manager.is_paused
*** End-of-File ***
