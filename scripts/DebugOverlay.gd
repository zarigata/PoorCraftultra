extends CanvasLayer
class_name DebugOverlay

const UPDATE_INTERVAL := 0.1
const PANEL_SIZE := Vector2(320, 320)
const PANEL_MARGIN := Vector2(12, 12)
const PANEL_ALPHA := 0.7
const FPS_THRESHOLD_WARN := 30.0
const FPS_THRESHOLD_GOOD := 50.0

var _time_accumulator: float = 0.0
var _labels := {}

func _ready() -> void:
    layer = 100
    _build_ui()
    hide()
    ErrorLogger.log_debug("DebugOverlay ready", "DebugOverlay")

func _process(delta: float) -> void:
    _time_accumulator += delta
    if _time_accumulator >= UPDATE_INTERVAL:
        _time_accumulator = 0.0
        _update_stats()

func _input(event: InputEvent) -> void:
    if event.is_action_pressed("debug_overlay") and event.is_pressed():
        toggle_visibility()

func toggle_visibility() -> void:
    visible = not visible
    var state_text := visible ? "shown" : "hidden"
    ErrorLogger.log_debug("Debug overlay %s" % state_text, "DebugOverlay")

func _build_ui() -> void:
    var panel := Panel.new()
    panel.name = "Panel"
    panel.set_anchors_and_offsets_preset(Control.PRESET_TOP_LEFT, Control.PRESET_MODE_MINSIZE, PANEL_MARGIN)
    panel.size = PANEL_SIZE
    panel.self_modulate.a = PANEL_ALPHA
    add_child(panel)

    var vbox := VBoxContainer.new()
    vbox.name = "Container"
    vbox.anchor_left = 0
    vbox.anchor_top = 0
    vbox.anchor_right = 1
    vbox.anchor_bottom = 1
    vbox.offset_left = 12
    vbox.offset_top = 12
    vbox.offset_right = -12
    vbox.offset_bottom = -12
    panel.add_child(vbox)

    var title := Label.new()
    title.text = "RetroForge Debug"
    title.add_theme_color_override("font_color", Color(0.8, 0.95, 1.0))
    title.add_theme_font_size_override("font_size", 18)
    vbox.add_child(title)

    var separator := HSeparator.new()
    vbox.add_child(separator)

    _create_stat_label(vbox, "fps", "FPS: 0")
    _create_stat_label(vbox, "frame_time", "Frame Time: 0 ms")
    _create_stat_label(vbox, "memory", "Memory: 0 MB")
    _create_stat_label(vbox, "game_state", "Game State: Unknown")
    _create_stat_label(vbox, "input_mode", "Input Mode: Unknown")
    _create_stat_label(vbox, "player_position", "Player: (0, 0, 0)")
    _create_stat_label(vbox, "player_velocity", "Velocity: 0.0 m/s")
    _create_stat_label(vbox, "player_state", "State: IDLE")
    _create_stat_label(vbox, "player_grounded", "Grounded: Yes")
    _create_stat_label(vbox, "chunk_count", "Chunks: 0")
    _create_stat_label(vbox, "entity_count", "Entities: 0")
    _create_stat_label(vbox, "time_of_day", "Time: 00:00 (Day)")
    _create_stat_label(vbox, "biome", "Biome: Unknown")
    _create_stat_label(vbox, "audio_pool", "Audio Pool: 0/32")

func _create_stat_label(container: VBoxContainer, key: String, text: String) -> void:
    var label := Label.new()
    label.name = key.capitalize()
    label.text = text
    label.add_theme_font_override("font", _get_monospace_font())
    container.add_child(label)
    _labels[key] = label

func _get_monospace_font() -> Font:
    var default_font := ThemeDB.fallback_font
    return default_font

func _update_stats() -> void:
    if not visible:
        return
    _update_fps()
    _update_frame_time()
    _update_memory()
    _update_game_state()
    _update_input_mode()
    _update_player_stats()
    _update_chunk_stats()
    _update_entity_stats()
    _update_environment_stats()
    _update_audio_stats()

func _update_fps() -> void:
    var fps := Engine.get_frames_per_second()
    _set_label_text("fps", "FPS: %.1f" % fps)
    var color := Color(0.6, 1.0, 0.6)
    if fps < FPS_THRESHOLD_WARN:
        color = Color(1.0, 0.4, 0.4)
    elif fps < FPS_THRESHOLD_GOOD:
        color = Color(1.0, 0.8, 0.4)
    _set_label_color("fps", color)

func _update_frame_time() -> void:
    var frame_time := Performance.get_monitor(Performance.TIME_PROCESS) * 1000.0
    _set_label_text("frame_time", "Frame Time: %.2f ms" % frame_time)

func _update_memory() -> void:
    var memory := OS.get_static_memory_usage() / (1024.0 * 1024.0)
    var world := _get_voxel_world()
    if world:
        var stats := world.get_stats()
        var voxel_mem := stats.get("memory_usage_mb", 0.0)
        _set_label_text("memory", "Memory: %.2f MB (Voxel: %.2f MB)" % [memory, voxel_mem])
    else:
        _set_label_text("memory", "Memory: %.2f MB" % memory)

func _update_game_state() -> void:
    var state_name := "Unknown"
    var game_manager := _get_game_manager()
    if game_manager:
        var state_index := game_manager.current_state
        var state_keys := game_manager.GameState.keys()
        if state_index >= 0 and state_index < state_keys.size():
            state_name = state_keys[state_index]
    _set_label_text("game_state", "Game State: %s" % state_name)

func _update_input_mode() -> void:
    var mode_name := "Unknown"
    var input_manager := _get_input_manager()
    if input_manager:
        var mode_index := input_manager.current_input_mode
        var mode_keys := input_manager.InputMode.keys()
        if mode_index >= 0 and mode_index < mode_keys.size():
            mode_name = mode_keys[mode_index]
    _set_label_text("input_mode", "Input Mode: %s" % mode_name)

func _update_player_stats() -> void:
    var game_manager := _get_game_manager()
    if game_manager == null:
        _set_label_text("player_position", "Player: N/A")
        _set_label_text("player_velocity", "Velocity: N/A")
        _set_label_text("player_state", "State: N/A")
        _set_label_text("player_grounded", "Grounded: N/A")
        return

    var player := game_manager.get_player()
    if player == null or not (player is Node3D):
        _set_label_text("player_position", "Player: Not spawned")
        _set_label_text("player_velocity", "Velocity: N/A")
        _set_label_text("player_state", "State: N/A")
        _set_label_text("player_grounded", "Grounded: N/A")
        return

    var position := player.global_position
    _set_label_text("player_position", "Player: (%.1f, %.1f, %.1f)" % [position.x, position.y, position.z])

    var velocity_mag := 0.0
    if player.has_method("get_velocity_horizontal"):
        velocity_mag = float(player.call("get_velocity_horizontal"))
    _set_label_text("player_velocity", "Velocity: %.2f m/s" % velocity_mag)
    var velocity_color := Color(0.6, 1.0, 0.6) if velocity_mag > 0.1 else Color(0.7, 0.7, 0.7)
    _set_label_color("player_velocity", velocity_color)

    var state_name := "UNKNOWN"
    if player.has_method("get_current_state"):
        var state_index := int(player.call("get_current_state"))
        var state_names := ["IDLE", "WALKING", "SPRINTING", "JUMPING", "FALLING", "FLYING"]
        if state_index >= 0 and state_index < state_names.size():
            state_name = state_names[state_index]
    _set_label_text("player_state", "State: %s" % state_name)
    var state_color := Color(0.7, 0.7, 0.7)
    match state_name:
        "SPRINTING":
            state_color = Color(0.4, 0.8, 1.0)
        "WALKING":
            state_color = Color(0.6, 1.0, 0.6)
        "JUMPING", "FALLING":
            state_color = Color(1.0, 0.9, 0.4)
        "FLYING":
            state_color = Color(1.0, 0.4, 1.0)
    _set_label_color("player_state", state_color)

    var grounded := false
    if player is CharacterBody3D:
        grounded = player.is_on_floor()
    var grounded_text := "Yes" if grounded else "No"
    _set_label_text("player_grounded", "Grounded: %s" % grounded_text)
    var grounded_color := Color(0.6, 1.0, 0.6) if grounded else Color(1.0, 0.9, 0.4)
    _set_label_color("player_grounded", grounded_color)

func _update_chunk_stats() -> void:
    var chunk_text := "Chunks: 0 (stub)"
    var world := _get_voxel_world()
    if world:
        var stats := world.get_stats()
        var loaded := stats.get("loaded_chunks", 0)
        var meshing := stats.get("meshing_blocks", 0)
        var streaming := stats.get("streaming_blocks", 0)
        chunk_text = "Chunks: %d (M:%d S:%d)" % [loaded, meshing, streaming]
    _set_label_text("chunk_count", chunk_text)

func _update_entity_stats() -> void:
    _set_label_text("entity_count", "Entities: 0 (stub)")

func _set_label_text(key: String, text: String) -> void:
    if _labels.has(key):
        _labels[key].text = text

func _set_label_color(key: String, color: Color) -> void:
    if _labels.has(key):
        _labels[key].self_modulate = color

func _update_environment_stats() -> void:
    var env_manager := _get_environment_manager()
    if env_manager:
        var time_norm := float(env_manager.call("get_time_normalized")) if env_manager.has_method("get_time_normalized") else 0.0
        var is_day := bool(env_manager.call("get_is_day")) if env_manager.has_method("get_is_day") else false
        var total_minutes := time_norm * 24.0 * 60.0
        var total_minutes_int := int(total_minutes)
        var hours := int(total_minutes_int / 60)
        var minutes := total_minutes_int % 60
        var time_str := "%02d:%02d" % [hours, minutes]
        var period := "Day" if is_day else "Night"
        _set_label_text("time_of_day", "Time: %s (%s)" % [time_str, period])

        var color := Color(1.0, 0.9, 0.4) if is_day else Color(0.4, 0.6, 1.0)
        _set_label_color("time_of_day", color)

        var biome := env_manager.get("current_biome") if env_manager != null else "Unknown"
        if biome == null or biome == "":
            biome = "Unknown"
        _set_label_text("biome", "Biome: %s" % biome)
    else:
        _set_label_text("time_of_day", "Time: N/A")
        _set_label_text("biome", "Biome: N/A")

func _update_audio_stats() -> void:
    var audio_manager := _get_audio_manager()
    if audio_manager:
        var active := "N/A"
        if audio_manager.has_method("get_active_player_count"):
            active = str(int(audio_manager.call("get_active_player_count")))

        var max_pool := "N/A"
        if audio_manager.has_method("get_max_pool_size"):
            max_pool = str(int(audio_manager.call("get_max_pool_size")))

        if active == "N/A" or max_pool == "N/A":
            _set_label_text("audio_pool", "Audio Pool: N/A")
            _set_label_color("audio_pool", Color(1.0, 0.8, 0.4))
            return

        var active_int := int(active)
        var max_pool_int := max(int(max_pool), 1)

        _set_label_text("audio_pool", "Audio Pool: %d/%d" % [active_int, max_pool_int])

        var usage_ratio := float(active_int) / float(max_pool_int)
        var color := Color(0.6, 1.0, 0.6)
        if usage_ratio > 0.8:
            color = Color(1.0, 0.4, 0.4)
        elif usage_ratio > 0.5:
            color = Color(1.0, 0.8, 0.4)
        _set_label_color("audio_pool", color)
    else:
        _set_label_text("audio_pool", "Audio Pool: N/A")

func _get_environment_manager() -> Node:
    if typeof(EnvironmentManager) != TYPE_NIL and EnvironmentManager != null:
        return EnvironmentManager
    var tree := get_tree()
    if tree and tree.has_node("/root/EnvironmentManager"):
        return tree.get_node("/root/EnvironmentManager")
    return null

func _get_audio_manager() -> Node:
    if typeof(AudioManager) != TYPE_NIL and AudioManager != null:
        return AudioManager
    var tree := get_tree()
    if tree and tree.has_node("/root/AudioManager"):
        return tree.get_node("/root/AudioManager")
    return null

func _get_game_manager() -> Node:
    if typeof(GameManager) != TYPE_NIL and GameManager != null:
        return GameManager
    var tree := get_tree()
    if tree and tree.has_node("/root/GameManager"):
        return tree.get_node("/root/GameManager")
    return null

func _get_input_manager() -> Node:
    if typeof(InputManager) != TYPE_NIL and InputManager != null:
        return InputManager
    var tree := get_tree()
    if tree and tree.has_node("/root/InputManager"):
        return tree.get_node("/root/InputManager")
    return null

func _get_voxel_world() -> Node:
    var game_manager := _get_game_manager()
    if game_manager and game_manager.has_method("get_current_world"):
        return game_manager.get_current_world()
    return null
