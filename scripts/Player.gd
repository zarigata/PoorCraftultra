extends CharacterBody3D
class_name Player

signal state_changed(old_state: State, new_state: State)
signal landed(fall_distance: float)
signal jumped()
signal biome_entered(biome_id: String)
signal item_collected(item_id: String, quantity: int)

enum State { IDLE, WALKING, SPRINTING, JUMPING, FALLING, FLYING }

const WALK_SPEED := 5.0
const SPRINT_SPEED := 8.0
const JUMP_VELOCITY := 10.0
const ACCELERATION := 8.0
const AIR_ACCELERATION := 2.0
const FRICTION := 10.0
const AIR_FRICTION := 1.0
const TERMINAL_VELOCITY := -50.0
const FLY_SPEED := 10.0
const MOUSE_SENSITIVITY_MULTIPLIER := 0.001
const HEAD_BOB_FREQUENCY := 2.0
const HEAD_BOB_AMPLITUDE := 0.05
const FOOTSTEP_INTERVAL := 0.4
const BIOME_CHECK_INTERVAL := 1.0
const FALL_DAMAGE_THRESHOLD := 15.0
const FALL_DAMAGE_MULTIPLIER := 2.0

const STATE_NAMES := ["IDLE", "WALKING", "SPRINTING", "JUMPING", "FALLING", "FLYING"]

var current_state: State = State.IDLE
var head: Node3D = null
var camera: Camera3D = null
var interaction_ray: RayCast3D = null
var animation_player: AnimationPlayer = null
var mouse_captured: bool = false
var mouse_sensitivity: float = 0.3
var gravity: float = 20.0
var is_grounded: bool = false
var fall_start_height: float = 0.0
var head_bob_time: float = 0.0
var footstep_timer: float = 0.0
var biome_check_timer: float = 0.0
var current_biome: String = ""
var fly_mode_enabled: bool = false

var _head_default_position: Vector3 = Vector3.ZERO
var _input_manager: InputManager = null
var _audio_manager: AudioManager = null
var _voxel_world: VoxelWorld = null
var _game_manager: GameManager = null
var _head_pitch: float = 0.0
var _pending_mouse_capture: bool = false
var _footstep_stream_cache := {}
var _mining_system: MiningSystem = null
var _is_mining: bool = false
var _mining_animation: StringName = &"mining_loop"
var _nearby_items: Array[Node] = []
var _pickup_cooldown: float = 0.0
const PICKUP_COOLDOWN_DURATION := 0.1
var _interaction_manager: InteractionManager = null
var _building_system: BuildingSystem = null
var _build_mode_enabled: bool = false
var _build_mode_hold_time: float = 0.0
const BUILD_MODE_HOLD_DURATION := 0.3

func _ready() -> void:
    _locate_autoloads()
    _resolve_child_nodes()
    _initialize_settings()
    _connect_signals()
    _register_with_managers()
    _capture_mouse()
    gravity = ProjectSettings.get_setting("physics/3d/default_gravity", gravity)
    footstep_timer = FOOTSTEP_INTERVAL
    biome_check_timer = BIOME_CHECK_INTERVAL
    fall_start_height = global_position.y
    ErrorLogger.log_info("Player initialized", "Player")

func _process(_delta: float) -> void:
    if _game_manager != null and _game_manager.is_paused:
        if mouse_captured:
            _release_mouse()
    elif _pending_mouse_capture and not mouse_captured:
        _capture_mouse()

func _input(event: InputEvent) -> void:
    if event is InputEventMouseMotion and mouse_captured:
        _apply_mouse_look(event.relative)
        return
    var toggle_capture_pressed := _input_manager != null ? _input_manager.is_action_just_pressed_buffered("toggle_mouse_capture") : Input.is_action_just_pressed("toggle_mouse_capture")
    if toggle_capture_pressed:
        if mouse_captured:
            _release_mouse()
        else:
            _capture_mouse()
    elif OS.is_debug_build():
        var toggle_fly_pressed := _input_manager != null ? _input_manager.is_action_just_pressed_buffered("toggle_fly_mode") : Input.is_action_just_pressed("toggle_fly_mode")
        if toggle_fly_pressed:
            var interact_just_pressed := _input_manager != null ? _input_manager.is_action_just_pressed_buffered("interact") : Input.is_action_just_pressed("interact")
            if interact_just_pressed:
                return
            _toggle_fly_mode()

func _physics_process(delta: float) -> void:
    if _game_manager != null and _game_manager.is_paused:
        _apply_friction(FRICTION, delta)
        return

    var was_grounded := is_on_floor()
    var movement_input := _get_movement_input()

    _handle_mining_input(delta)

    if current_state == State.FLYING:
        _state_flying_process(delta, movement_input)
    else:
        match current_state:
            State.IDLE:
                _state_idle_process(delta, movement_input)
            State.WALKING:
                _state_walking_process(delta, movement_input)
            State.SPRINTING:
                _state_sprinting_process(delta, movement_input)
            State.JUMPING:
                _state_jumping_process(delta, movement_input)
            State.FALLING:
                _state_falling_process(delta, movement_input)
            _:
                _change_state(State.IDLE)

        if current_state != State.FLYING:
            _apply_gravity(delta, was_grounded)

    move_and_slide()

    var now_grounded := is_on_floor()
    if not was_grounded and now_grounded:
        _handle_landing()
    elif not now_grounded and current_state not in [State.JUMPING, State.FLYING, State.FALLING]:
        fall_start_height = global_position.y
        _change_state(State.FALLING)

    is_grounded = now_grounded

    _update_head_bob(delta, movement_input, now_grounded)
    _update_biome(delta)
    _update_pickup(delta)
    _update_interaction(delta)
    _handle_building_input(delta)

func _handle_mining_input(_delta: float) -> void:
    if _mining_system == null or _game_manager == null:
        return

    if _game_manager.is_paused:
        if _is_mining:
            _mining_system.stop_mining()
            _is_mining = false
        return

    var mine_pressed := false
    if _input_manager != null:
        mine_pressed = _input_manager.is_action_pressed_safe("mine")
    else:
        mine_pressed = Input.is_action_pressed("mine")

    if mine_pressed and not _is_mining:
        if _mining_system.can_start_mining() and _mining_system.start_mining(self):
            _is_mining = true
            ErrorLogger.log_debug("Mining started", "Player")
            _play_mining_animation()
    elif not mine_pressed and _is_mining:
        _mining_system.stop_mining()
        _is_mining = false
        ErrorLogger.log_debug("Mining stopped", "Player")
        _stop_mining_animation()

    if _is_mining and is_moving():
        var velocity_threshold := 0.5
        if get_velocity_horizontal() > velocity_threshold:
            _mining_system.stop_mining()
            _is_mining = false
            ErrorLogger.log_debug("Mining cancelled due to movement", "Player")
            _stop_mining_animation()

func _handle_building_input(delta: float) -> void:
    if _building_system == null or _game_manager == null:
        return

    if _game_manager.is_paused:
        if _build_mode_enabled:
            _exit_build_mode()
        return

    var build_pressed := false
    if _input_manager != null:
        build_pressed = _input_manager.is_action_pressed_safe("build_mode")
    else:
        build_pressed = Input.is_action_pressed("build_mode")

    if build_pressed:
        _build_mode_hold_time += delta
        if _build_mode_hold_time >= BUILD_MODE_HOLD_DURATION and not _build_mode_enabled:
            _enter_build_mode()
    else:
        if _build_mode_enabled:
            _exit_build_mode()
        _build_mode_hold_time = 0.0

    if _build_mode_enabled and _building_system.is_in_build_mode():
        _building_system.update_preview(self)

        var place_pressed := false
        if _input_manager != null:
            place_pressed = _input_manager.is_action_just_pressed_buffered("place")
        else:
            place_pressed = Input.is_action_just_pressed("place")

        if place_pressed:
            if _building_system.try_place_building():
                ErrorLogger.log_debug("Building placed", "Player")

        if Input.is_action_just_pressed("rotate_building_cw"):
            _building_system.rotate_preview(90.0)
        elif Input.is_action_just_pressed("rotate_building_ccw"):
            _building_system.rotate_preview(-90.0)

func _locate_autoloads() -> void:
    if typeof(InputManager) != TYPE_NIL and InputManager != null:
        _input_manager = InputManager
    if typeof(AudioManager) != TYPE_NIL and AudioManager != null:
        _audio_manager = AudioManager
    if typeof(GameManager) != TYPE_NIL and GameManager != null:
        _game_manager = GameManager
        _voxel_world = _game_manager.get_current_world()
    if typeof(MiningSystem) != TYPE_NIL and MiningSystem != null:
        _mining_system = MiningSystem
    if typeof(InteractionManager) != TYPE_NIL and InteractionManager != null:
        _interaction_manager = InteractionManager
    if typeof(BuildingSystem) != TYPE_NIL and BuildingSystem != null:
        _building_system = BuildingSystem

func _resolve_child_nodes() -> void:
    head = get_node_or_null("Head")
    if head == null:
        ErrorLogger.log_error("Player head node missing", "Player")
    else:
        _head_default_position = head.position
    camera = head != null and head.get_node_or_null("Camera") or null
    if camera == null:
        ErrorLogger.log_error("Player camera node missing", "Player")
    interaction_ray = camera != null and camera.get_node_or_null("InteractionRay") or null
    if interaction_ray == null:
        ErrorLogger.log_warning("Interaction ray not found; interactions disabled", "Player")
    animation_player = head != null and head.get_node_or_null("AnimationPlayer") or get_node_or_null("AnimationPlayer")
    if animation_player == null:
        ErrorLogger.log_warning("AnimationPlayer not found; mining animations disabled", "Player")

func _initialize_settings() -> void:
    mouse_sensitivity = _get_mouse_sensitivity()
    if camera != null and _game_manager != null:
        var fov := _game_manager.get_setting("graphics/camera_fov", null)
        if fov != null:
            camera.fov = float(fov)

func _connect_signals() -> void:
    if _game_manager != null:
        if not _game_manager.settings_changed.is_connected(_on_settings_changed):
            _game_manager.settings_changed.connect(_on_settings_changed)
        if not _game_manager.game_paused.is_connected(_on_game_paused):
            _game_manager.game_paused.connect(_on_game_paused)
    _ensure_voxel_world_reference()
    _connect_mining_signals()
    # Item pickup is handled via DroppedItem's Area3D detection
    # No explicit signal connection needed here

func _connect_mining_signals() -> void:
    if _mining_system == null:
        return
    if _mining_system.mining_started.is_connected(_on_mining_started):
        _mining_system.mining_started.disconnect(_on_mining_started)
    if _mining_system.mining_completed.is_connected(_on_mining_completed):
        _mining_system.mining_completed.disconnect(_on_mining_completed)
    if _mining_system.mining_cancelled.is_connected(_on_mining_cancelled):
        _mining_system.mining_cancelled.disconnect(_on_mining_cancelled)
    _mining_system.mining_started.connect(_on_mining_started)
    _mining_system.mining_completed.connect(_on_mining_completed)
    _mining_system.mining_cancelled.connect(_on_mining_cancelled)

func _register_with_managers() -> void:
    if _game_manager != null:
        _game_manager.set_player(self)
    else:
        ErrorLogger.log_warning("GameManager not available for player registration", "Player")

func _ensure_voxel_world_reference() -> void:
    if _game_manager == null:
        return
    var world := _game_manager.get_current_world()
    if world == _voxel_world:
        return
    if _voxel_world != null and _voxel_world.has_signal("biome_detected"):
        if _voxel_world.biome_detected.is_connected(_on_biome_detected):
            _voxel_world.biome_detected.disconnect(_on_biome_detected)
    _voxel_world = world
    if _voxel_world != null and _voxel_world.has_signal("biome_detected") and not _voxel_world.biome_detected.is_connected(_on_biome_detected):
        _voxel_world.biome_detected.connect(_on_biome_detected)

func _apply_mouse_look(relative: Vector2) -> void:
    var sensitivity := mouse_sensitivity * MOUSE_SENSITIVITY_MULTIPLIER
    var yaw_delta := -relative.x * sensitivity
    var pitch_delta := -relative.y * sensitivity

    rotate_y(yaw_delta)

    _head_pitch = clamp(_head_pitch + pitch_delta, deg_to_rad(-89.0), deg_to_rad(89.0))
    if head != null:
        head.rotation.x = _head_pitch

func _apply_gravity(delta: float, was_grounded: bool) -> void:
    if fly_mode_enabled:
        return
    if was_grounded and velocity.y < 0.0:
        velocity.y = 0.0
    velocity.y = max(velocity.y - gravity * delta, TERMINAL_VELOCITY)

func _apply_movement(direction: Vector3, target_speed: float, accel: float, delta: float) -> void:
    if direction == Vector3.ZERO:
        return
    var target_velocity := direction * target_speed
    var horizontal_velocity := Vector3(velocity.x, 0.0, velocity.z)
    var t := clamp(accel * delta, 0.0, 1.0)
    horizontal_velocity = horizontal_velocity.lerp(target_velocity, t)
    velocity.x = horizontal_velocity.x
    velocity.z = horizontal_velocity.z

func _apply_friction(amount: float, delta: float) -> void:
    var horizontal := Vector2(velocity.x, velocity.z)
    var factor := max(1.0 - amount * delta, 0.0)
    horizontal *= factor
    velocity.x = horizontal.x
    velocity.z = horizontal.y

func _get_movement_input() -> Vector2:
    if _input_manager != null:
        return _input_manager.get_movement_vector()
    return Vector2.ZERO

func _get_movement_direction(input_vector: Vector2) -> Vector3:
    if input_vector == Vector2.ZERO:
        return Vector3.ZERO
    var forward := -global_transform.basis.z
    forward.y = 0.0
    forward = forward.normalized()
    var right := global_transform.basis.x
    right.y = 0.0
    right = right.normalized()
    var direction := (right * input_vector.x) + (forward * input_vector.y)
    return direction.normalized()

func _state_idle_process(delta: float, movement_input: Vector2) -> void:
    if movement_input != Vector2.ZERO:
        _change_state(State.WALKING)
        return
    if _is_jump_pressed():
        _change_state(State.JUMPING)
        return
    _apply_friction(FRICTION, delta)
    _reset_head_bob(delta)

func _state_walking_process(delta: float, movement_input: Vector2) -> void:
    if _is_jump_pressed():
        _change_state(State.JUMPING)
        return
    if movement_input == Vector2.ZERO:
        _change_state(State.IDLE)
        return
    if _is_sprint_pressed():
        _change_state(State.SPRINTING)
        return

    var direction := _get_movement_direction(movement_input)
    _apply_movement(direction, WALK_SPEED, ACCELERATION, delta)

    footstep_timer -= delta
    if footstep_timer <= 0.0:
        _play_footstep()
        footstep_timer = FOOTSTEP_INTERVAL

func _state_sprinting_process(delta: float, movement_input: Vector2) -> void:
    if _is_jump_pressed():
        _change_state(State.JUMPING)
        return
    if movement_input == Vector2.ZERO:
        _change_state(State.IDLE)
        return
    if not _is_sprint_pressed():
        _change_state(State.WALKING)
        return

    var direction := _get_movement_direction(movement_input)
    _apply_movement(direction, SPRINT_SPEED, ACCELERATION, delta)

    footstep_timer -= delta
    if footstep_timer <= 0.0:
        _play_footstep()
        footstep_timer = FOOTSTEP_INTERVAL * 0.7

func _state_jumping_process(delta: float, movement_input: Vector2) -> void:
    var direction := _get_movement_direction(movement_input)
    if direction == Vector3.ZERO:
        _apply_friction(AIR_FRICTION, delta)
    else:
        _apply_movement(direction, WALK_SPEED, AIR_ACCELERATION, delta)
    if velocity.y <= 0.0:
        _change_state(State.FALLING)

func _state_falling_process(delta: float, movement_input: Vector2) -> void:
    var direction := _get_movement_direction(movement_input)
    if direction == Vector3.ZERO:
        _apply_friction(AIR_FRICTION, delta)
    else:
        _apply_movement(direction, WALK_SPEED, AIR_ACCELERATION, delta)

func _state_flying_process(delta: float, movement_input: Vector2) -> void:
    if not fly_mode_enabled:
        _change_state(State.IDLE)
        return

    var basis := head != null ? head.global_transform.basis : global_transform.basis
    var forward := -basis.z.normalized()
    var right := basis.x.normalized()
    var up := basis.y.normalized()

    var direction := Vector3.ZERO
    direction += forward * movement_input.y
    direction += right * movement_input.x

    var descend_pressed := false
    var ascend_pressed := false
    if _input_manager != null:
        descend_pressed = _input_manager.is_action_pressed_safe("fly_down")
        ascend_pressed = _input_manager.is_action_pressed_safe("fly_up") or _input_manager.is_action_pressed_safe("jump")
    else:
        descend_pressed = Input.is_action_pressed("fly_down")
        ascend_pressed = Input.is_action_pressed("fly_up") or Input.is_action_pressed("jump")

    if descend_pressed:
        direction -= up
    if ascend_pressed:
        direction += up

    if direction != Vector3.ZERO:
        direction = direction.normalized()
        velocity = direction * FLY_SPEED
    else:
        velocity = velocity.move_toward(Vector3.ZERO, ACCELERATION * delta)

func _change_state(new_state: State) -> void:
    if new_state == current_state:
        return
    var old_state := current_state
    _exit_state(old_state)
    current_state = new_state
    _enter_state(new_state)
    emit_signal("state_changed", old_state, new_state)
    var old_name := STATE_NAMES[min(old_state, STATE_NAMES.size() - 1)]
    var new_name := STATE_NAMES[min(new_state, STATE_NAMES.size() - 1)]
    ErrorLogger.log_debug("State changed: %s -> %s" % [old_name, new_name], "Player")

func _enter_state(state: State) -> void:
    match state:
        State.IDLE:
            _reset_head_bob(0.0)
        State.WALKING:
            footstep_timer = FOOTSTEP_INTERVAL
        State.SPRINTING:
            footstep_timer = FOOTSTEP_INTERVAL * 0.7
        State.JUMPING:
            velocity.y = JUMP_VELOCITY
            fall_start_height = global_position.y
            emit_signal("jumped")
        State.FALLING:
            fall_start_height = max(fall_start_height, global_position.y)
        State.FLYING:
            velocity = Vector3.ZERO
            fall_start_height = global_position.y
            self.motion_mode = CharacterBody3D.MOTION_MODE_FLOATING

func _exit_state(state: State) -> void:
    match state:
        State.FLYING:
            self.motion_mode = CharacterBody3D.MOTION_MODE_GROUNDED
            fly_mode_enabled = false

func _handle_landing() -> void:
    var fall_distance := fall_start_height - global_position.y
    if fall_distance > 0.0:
        emit_signal("landed", fall_distance)
        var damage := _calculate_fall_damage(fall_distance)
        if damage > 0.0:
            ErrorLogger.log_debug("Fall damage calculated: %.2f" % damage, "Player")
    if current_state in [State.FALLING, State.JUMPING]:
        var has_input := _get_movement_input() != Vector2.ZERO
        _change_state(has_input and State.WALKING or State.IDLE)
    fall_start_height = global_position.y

func _reset_head_bob(delta: float) -> void:
    if head == null:
        return
    head_bob_time = 0.0
    if delta > 0.0:
        head.position = head.position.lerp(_head_default_position, min(delta * 10.0, 1.0))
    else:
        head.position = _head_default_position

func _update_head_bob(delta: float, movement_input: Vector2, grounded: bool) -> void:
    if head == null:
        return
    if grounded and movement_input != Vector2.ZERO and current_state in [State.WALKING, State.SPRINTING]:
        var frequency := HEAD_BOB_FREQUENCY * (current_state == State.SPRINTING ? 1.3 : 1.0)
        var amplitude := HEAD_BOB_AMPLITUDE * (current_state == State.SPRINTING ? 1.2 : 1.0)
        head_bob_time += delta * frequency
        var offset := sin(head_bob_time * TAU) * amplitude
        var head_pos := head.position
        head_pos.y = _head_default_position.y + offset
        head.position = head_pos
    else:
        _reset_head_bob(delta)

func _update_pickup(delta: float) -> void:
    if _pickup_cooldown > 0.0:
        _pickup_cooldown = max(_pickup_cooldown - delta, 0.0)

func _update_interaction(delta: float) -> void:
    if _interaction_manager == null:
        return

    if interaction_ray != null and interaction_ray.enabled:
        _interaction_manager.update_focus(self, interaction_ray)
    else:
        _interaction_manager.clear_focus()

    if _game_manager != null and _game_manager.is_paused:
        return

    var interact_pressed := false
    if _input_manager != null:
        interact_pressed = _input_manager.is_action_just_pressed_buffered("interact")
    else:
        interact_pressed = Input.is_action_just_pressed("interact")

    if interact_pressed:
        if _interaction_manager.can_interact():
            var success := _interaction_manager.try_interact(self)
            if success:
                ErrorLogger.log_debug("Interaction triggered", "Player")
        else:
            ErrorLogger.log_debug("Cannot interact (cooldown or no focus)", "Player")

func _play_footstep() -> void:
    _ensure_voxel_world_reference()
    if _audio_manager == null or _voxel_world == null:
        return
    var feet_position := global_position + Vector3(0.0, -1.0, 0.0)
    var voxel_type := _voxel_world.get_voxel_type_at(feet_position) if _voxel_world.has_method("get_voxel_type_at") else {}
    var material_name := String(voxel_type.get("name", "default")).to_lower()

    if not _footstep_stream_cache.has(material_name):
        var stream: AudioStream = null
        var primary_path := "res://assets/audio/sfx/footsteps/%s_walk.ogg" % material_name
        if ResourceLoader.exists(primary_path):
            stream = ResourceLoader.load(primary_path)
        else:
            var fallback_path := "res://assets/audio/sfx/footsteps/default_walk.ogg"
            if ResourceLoader.exists(fallback_path):
                stream = ResourceLoader.load(fallback_path)
        _footstep_stream_cache[material_name] = stream

    var cached_stream: AudioStream = _footstep_stream_cache[material_name]
    if cached_stream:
        _audio_manager.play_sfx_3d(cached_stream, global_position)

func _update_biome(delta: float) -> void:
    _ensure_voxel_world_reference()
    if _voxel_world == null or not _voxel_world.has_method("check_biome_at_position"):
        return
    biome_check_timer -= delta
    if biome_check_timer <= 0.0:
        biome_check_timer = BIOME_CHECK_INTERVAL
        _voxel_world.check_biome_at_position(global_position)

func _calculate_fall_damage(fall_distance: float) -> float:
    if fall_distance <= FALL_DAMAGE_THRESHOLD:
        return 0.0
    return (fall_distance - FALL_DAMAGE_THRESHOLD) * FALL_DAMAGE_MULTIPLIER

func _capture_mouse() -> void:
    if _game_manager != null and _game_manager.is_paused:
        _pending_mouse_capture = true
        return
    Input.mouse_mode = Input.MOUSE_MODE_CAPTURED
    mouse_captured = true
    _pending_mouse_capture = false
    ErrorLogger.log_debug("Mouse captured", "Player")

func _release_mouse() -> void:
    Input.mouse_mode = Input.MOUSE_MODE_VISIBLE
    mouse_captured = false
    ErrorLogger.log_debug("Mouse released", "Player")

func _get_mouse_sensitivity() -> float:
    if _input_manager != null:
        return _input_manager.get_mouse_sensitivity()
    if _game_manager != null:
        return float(_game_manager.get_setting("controls/mouse_sensitivity", mouse_sensitivity))
    return mouse_sensitivity

func _on_settings_changed(setting_name: String, new_value: Variant) -> void:
    if setting_name == "controls/mouse_sensitivity":
        mouse_sensitivity = float(new_value)
    elif setting_name == "graphics/camera_fov" and camera != null:
        camera.fov = float(new_value)

func _on_game_paused(paused: bool) -> void:
    if paused:
        _release_mouse()
        if _is_mining and _mining_system != null:
            _mining_system.stop_mining()
            _is_mining = false
            _stop_mining_animation()
            ErrorLogger.log_debug("Mining stopped", "Player")
        if _build_mode_enabled and _building_system != null:
            _exit_build_mode()
    else:
        _pending_mouse_capture = true

func _on_mining_started(_position: Vector3, _voxel: Dictionary) -> void:
    _play_mining_animation()

func _on_mining_completed(_position: Vector3, _drops: Array) -> void:
    _is_mining = false
    _stop_mining_animation()
    if _mining_system == null:
        return
    var mine_pressed := false
    if _input_manager != null:
        mine_pressed = _input_manager.is_action_pressed_safe("mine")
    else:
        mine_pressed = Input.is_action_pressed("mine")
    if mine_pressed and _mining_system.can_start_mining() and _mining_system.start_mining(self):
        _is_mining = true
        _play_mining_animation()

func _on_mining_cancelled() -> void:
    _stop_mining_animation()

func _play_mining_animation() -> void:
    if animation_player == null:
        return
    if _mining_animation == StringName():
        return
    if not animation_player.has_animation(_mining_animation):
        return
    animation_player.play(_mining_animation, -1.0, 1.0, true)

func _stop_mining_animation() -> void:
    if animation_player == null:
        return
    if _mining_animation == StringName():
        return
    if not animation_player.has_animation(_mining_animation):
        return
    if animation_player.is_playing() and animation_player.current_animation == _mining_animation:
        animation_player.stop()

func _on_biome_detected(_position: Vector3, biome_id: String) -> void:
    if biome_id == current_biome:
        return
    current_biome = biome_id
    emit_signal("biome_entered", biome_id)

func _toggle_fly_mode() -> void:
    fly_mode_enabled = not fly_mode_enabled
    if fly_mode_enabled:
        _change_state(State.FLYING)
        ErrorLogger.log_info("Fly mode enabled", "Player")
    else:
        _change_state(State.IDLE)
        ErrorLogger.log_info("Fly mode disabled", "Player")

func request_item_pickup(item: Node) -> bool:
    """Public method for DroppedItem to request pickup.
    Returns true if pickup was accepted."""
    if _game_manager != null and _game_manager.is_paused:
        return false

    if _pickup_cooldown > 0.0:
        return false

    return _on_item_pickup_requested(item)

func _on_item_pickup_requested(item: Node) -> bool:
    if item == null:
        ErrorLogger.log_warning("Pickup requested with null item", "Player")
        return false
    if not item.has_method("get_item_data"):
        ErrorLogger.log_warning("Pickup item missing get_item_data()", "Player")
        return false

    var item_data := item.get_item_data()
    if typeof(item_data) != TYPE_DICTIONARY:
        ErrorLogger.log_warning("Pickup item data invalid type", "Player")
        return false
    if not item_data.has("item_id") or not item_data.has("quantity"):
        ErrorLogger.log_warning("Pickup item data missing fields", "Player")
        return false

    var item_id := String(item_data.get("item_id", "")).strip_edges()
    var quantity := int(item_data.get("quantity", 0))
    if item_id.is_empty() or quantity <= 0:
        ErrorLogger.log_warning("Pickup item data contains invalid values", "Player")
        return false

    emit_signal("item_collected", item_id, quantity)
    ErrorLogger.log_info("Collected item: %dx %s" % [quantity, item_id], "Player")
    _pickup_cooldown = PICKUP_COOLDOWN_DURATION
    return true

func _is_jump_pressed() -> bool:
    if _input_manager != null:
        return _input_manager.is_action_just_pressed_buffered("jump")
    return Input.is_action_just_pressed("jump")

func _is_sprint_pressed() -> bool:
    if _input_manager != null:
        return _input_manager.is_action_pressed_safe("sprint")
    return Input.is_action_pressed("sprint")

func _is_mining_input_pressed() -> bool:
    if _input_manager != null:
        return _input_manager.is_action_pressed_safe("mine")
    return Input.is_action_pressed("mine")

func get_camera() -> Camera3D:
    return camera

func get_surface_normal() -> Vector3:
    return Vector3.UP

func get_interaction_ray() -> RayCast3D:
    return interaction_ray

func get_head_position() -> Vector3:
    return head != null ? head.global_position : global_position

func get_current_state() -> State:
    return current_state

func get_velocity_horizontal() -> float:
    return Vector2(velocity.x, velocity.z).length()

func is_moving() -> bool:
    return current_state in [State.WALKING, State.SPRINTING]

func teleport(position: Vector3) -> void:
    global_position = position
    velocity = Vector3.ZERO
    fall_start_height = position.y
    ErrorLogger.log_info("Player teleported to %s" % position, "Player")
