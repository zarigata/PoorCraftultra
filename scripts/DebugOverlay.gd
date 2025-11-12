extends CanvasLayer
class_name DebugOverlay

const UPDATE_INTERVAL := 0.1
const PANEL_SIZE := Vector2(320, 480)
const PANEL_MARGIN := Vector2(12, 12)
const PANEL_PADDING := 12.0
const PANEL_ALPHA := 0.7
const FPS_THRESHOLD_WARN := 30.0
const FPS_THRESHOLD_GOOD := 50.0

var _time_accumulator: float = 0.0
var _labels := {}
var _ui_manager: UIManager = null
var _panel: Panel = null
var _panel_container: VBoxContainer = null

func _ready() -> void:
    layer = 100
    _lookup_ui_manager()
    _build_ui()
    _apply_theme()
    _apply_layout_scale()
    _connect_ui_manager_signals()
    _register_with_ui_manager()
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
    panel.self_modulate.a = PANEL_ALPHA
    panel.anchor_left = 0.0
    panel.anchor_top = 0.0
    panel.anchor_right = 0.0
    panel.anchor_bottom = 0.0
    add_child(panel)
    _panel = panel

    var vbox := VBoxContainer.new()
    vbox.name = "Container"
    vbox.anchor_left = 0
    vbox.anchor_top = 0
    vbox.anchor_right = 1
    vbox.anchor_bottom = 1
    panel.add_child(vbox)
    _panel_container = vbox

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
    _create_stat_label(vbox, "mining_state", "Mining: IDLE")
    _create_stat_label(vbox, "mining_tool", "Tool: Hand")
    _create_stat_label(vbox, "undo_stack", "Undo: 0/50")
    _create_stat_label(vbox, "dropped_items", "Items: 0")
    _create_stat_label(vbox, "inventory_slots", "Inventory: 0/30")
    _create_stat_label(vbox, "inventory_weight", "Weight: 0.0/100.0")
    _create_stat_label(vbox, "hotbar_selected", "Hotbar: 1")
    _create_stat_label(vbox, "ui_scale", "UI Scale: 1.0x")
    _create_stat_label(vbox, "ui_registered", "UIs: 0")
    _create_stat_label(vbox, "crafting_active", "Crafting: N/A")
    _create_stat_label(vbox, "crafting_queues", "Queues: N/A")
    _create_stat_label(vbox, "interaction_focused", "Focus: None")
    _create_stat_label(vbox, "interaction_count", "Interactables: 0")

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
    _update_mining_stats()
    _update_dropped_items_stats()
    _update_inventory_stats()
    _update_ui_stats()
    _update_crafting_stats()
    _update_interaction_stats()

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

func _lookup_ui_manager() -> void:
    if typeof(UIManager) != TYPE_NIL and UIManager != null:
        _ui_manager = UIManager

func _apply_theme() -> void:
    if _ui_manager == null:
        return
    _ui_manager.apply_theme_to_control(self)
    ErrorLogger.log_debug("Theme applied to DebugOverlay", "DebugOverlay")

func _connect_ui_manager_signals() -> void:
    if _ui_manager == null:
        return
    if not _ui_manager.is_connected("theme_changed", Callable(self, "_on_ui_theme_changed")):
        _ui_manager.connect("theme_changed", Callable(self, "_on_ui_theme_changed"))
    if not _ui_manager.is_connected("resolution_changed", Callable(self, "_on_ui_resolution_changed")):
        _ui_manager.connect("resolution_changed", Callable(self, "_on_ui_resolution_changed"))

func _exit_tree() -> void:
    if _ui_manager != null:
        if _ui_manager.is_connected("theme_changed", Callable(self, "_on_ui_theme_changed")):
            _ui_manager.disconnect("theme_changed", Callable(self, "_on_ui_theme_changed"))
        if _ui_manager.is_connected("resolution_changed", Callable(self, "_on_ui_resolution_changed")):
            _ui_manager.disconnect("resolution_changed", Callable(self, "_on_ui_resolution_changed"))
        _ui_manager.unregister_ui(self)

func _register_with_ui_manager() -> void:
    if _ui_manager == null:
        return
    _ui_manager.register_ui(self, "DebugOverlay")

func _update_ui_stats() -> void:
    if _ui_manager == null:
        _set_label_text("ui_scale", "UI Scale: N/A")
        _set_label_text("ui_registered", "UIs: N/A")
        return

    var scale := 1.0
    if _ui_manager.has_method("get_ui_scale"):
        scale = _ui_manager.get_ui_scale()
    _set_label_text("ui_scale", "UI Scale: %.2fx" % scale)

    var registered := []
    if _ui_manager.has_method("get_registered_uis"):
        registered = _ui_manager.get_registered_uis()
    _set_label_text("ui_registered", "UIs: %d" % registered.size())

func _update_crafting_stats() -> void:
    var crafting_system := _get_crafting_system()
    if crafting_system == null:
        _set_label_text("crafting_active", "Crafting: N/A")
        _set_label_color("crafting_active", Color(0.7, 0.7, 0.7))
        _set_label_text("crafting_queues", "Queues: N/A")
        _set_label_color("crafting_queues", Color(0.7, 0.7, 0.7))
        return

    var active_crafts := crafting_system.get("active_crafts")
    if typeof(active_crafts) != TYPE_DICTIONARY:
        active_crafts = {}
    var active_count := 0
    if typeof(active_crafts) == TYPE_DICTIONARY:
        for value in active_crafts.values():
            if value != null:
                active_count += 1
    var active_text := "Crafting: None"
    var active_color := Color(0.7, 0.7, 0.7)
    if active_count > 0:
        active_text = "Crafting: %d active" % active_count
        active_color = Color(0.6, 1.0, 0.6)
    _set_label_text("crafting_active", active_text)
    _set_label_color("crafting_active", active_color)

    var queues := crafting_system.get("crafting_queues")
    if typeof(queues) != TYPE_DICTIONARY:
        queues = {}
    var total_in_queue := 0
    if typeof(queues) == TYPE_DICTIONARY:
        for queue_array in queues.values():
            if queue_array is Array:
                for entry in queue_array:
                    if entry != null:
                        total_in_queue += int(entry.quantity)
    var queue_text := "Queues: %d" % total_in_queue
    var queue_color := Color(0.7, 0.7, 0.7)
    if total_in_queue > 0:
        queue_color = Color(0.6, 1.0, 0.6)
    _set_label_text("crafting_queues", queue_text)
    _set_label_color("crafting_queues", queue_color)

func _update_interaction_stats() -> void:
    var interaction_manager := _get_interaction_manager()
    if interaction_manager == null:
        _set_label_text("interaction_focused", "Focus: N/A")
        _set_label_text("interaction_count", "Interactables: N/A")
        _set_label_color("interaction_focused", Color(0.7, 0.7, 0.7))
        _set_label_color("interaction_count", Color(0.7, 0.7, 0.7))
        return

    var focused_text := "Focus: None"
    if interaction_manager.has_method("get_focused_interactable"):
        var focused := interaction_manager.call("get_focused_interactable")
        if focused != null and is_instance_valid(focused):
            var prompt := focused.get_prompt_text() if focused.has_method("get_prompt_text") else "Unknown"
            focused_text = "Focus: %s" % prompt
    _set_label_text("interaction_focused", focused_text)

    var focused_color := Color(0.6, 1.0, 0.6) if focused_text != "Focus: None" else Color(0.7, 0.7, 0.7)
    _set_label_color("interaction_focused", focused_color)

    var count := 0
    if interaction_manager.has_method("get_all_interactables"):
        var interactables := interaction_manager.call("get_all_interactables")
        if typeof(interactables) == TYPE_ARRAY:
            count = interactables.size()
    _set_label_text("interaction_count", "Interactables: %d" % count)
    _set_label_color("interaction_count", Color(0.6, 0.9, 1.0))

func _on_ui_theme_changed() -> void:
    _apply_theme()
    _apply_layout_scale()

func _on_ui_resolution_changed(_new_size: Vector2) -> void:
    _apply_layout_scale()

func _apply_layout_scale() -> void:
    if _panel == null:
        return
    var scale := 1.0
    if _ui_manager != null and _ui_manager.has_method("get_ui_scale"):
        scale = _ui_manager.get_ui_scale()
    var scaled_size := PANEL_SIZE * scale
    var margin := PANEL_MARGIN * scale
    _panel.custom_minimum_size = scaled_size
    _panel.size = scaled_size
    _panel.offset_left = margin.x
    _panel.offset_top = margin.y
    _panel.offset_right = margin.x + scaled_size.x
    _panel.offset_bottom = margin.y + scaled_size.y
    if _panel_container != null:
        var padding := PANEL_PADDING * scale
        _panel_container.offset_left = padding
        _panel_container.offset_top = padding
        _panel_container.offset_right = -padding
        _panel_container.offset_bottom = -padding

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

func _update_mining_stats() -> void:
    var mining_system := _get_mining_system()
    if mining_system == null:
        _set_label_text("mining_state", "Mining: N/A")
        _set_label_text("mining_tool", "Tool: N/A")
        _set_label_text("undo_stack", "Undo: N/A")
        return

    var state_name := "UNKNOWN"
    if mining_system.has_method("get_current_state"):
        var state_index := int(mining_system.call("get_current_state"))
        var state_names := ["IDLE", "TARGETING", "MINING", "COOLDOWN"]
        if state_index >= 0 and state_index < state_names.size():
            state_name = state_names[state_index]

    var progress_value := mining_system.get("mining_progress")
    var progress := progress_value if typeof(progress_value) == TYPE_FLOAT else 0.0

    var state_text := "Mining: %s" % state_name
    if state_name == "MINING":
        state_text += " (%.0f%%)" % (progress * 100.0)
    _set_label_text("mining_state", state_text)

    var state_color := Color(0.7, 0.7, 0.7)
    match state_name:
        "MINING":
            state_color = Color(1.0, 0.9, 0.4)
        "COOLDOWN":
            state_color = Color(1.0, 0.6, 0.4)
    _set_label_color("mining_state", state_color)

    var tool_name := "None"
    var durability := 0.0
    if mining_system.has_method("get_active_tool"):
        var tool := mining_system.call("get_active_tool")
        if tool != null and tool is Tool:
            tool_name = tool.tool_name
            durability = tool.get_durability_percentage()

    var tool_text := "Tool: %s" % tool_name
    if durability > 0.0:
        tool_text += " (%.0f%%)" % (durability * 100.0)
    _set_label_text("mining_tool", tool_text)

    var tool_color := Color(0.6, 1.0, 0.6)
    if durability < 0.25:
        tool_color = Color(1.0, 0.4, 0.4)
    elif durability < 0.5:
        tool_color = Color(1.0, 0.8, 0.4)
    _set_label_color("mining_tool", tool_color)

    var undo_size := 0
    if mining_system.has_method("get_undo_stack_size"):
        undo_size = int(mining_system.call("get_undo_stack_size"))
    _set_label_text("undo_stack", "Undo: %d/50" % undo_size)

func _update_dropped_items_stats() -> void:
    """Updates dropped items statistics."""
    var tree := get_tree()
    if tree == null:
        _set_label_text("dropped_items", "Items: N/A")
        return

    var item_count := 0
    var root := tree.current_scene
    if root != null:
        item_count = _count_dropped_items_recursive(root)

    _set_label_text("dropped_items", "Items: %d" % item_count)

    var color := Color(0.6, 1.0, 0.6)
    if item_count > 100:
        color = Color(1.0, 0.4, 0.4)
    elif item_count > 50:
        color = Color(1.0, 0.8, 0.4)
    _set_label_color("dropped_items", color)

func _count_dropped_items_recursive(node: Node) -> int:
    """Recursively counts DroppedItem instances in scene tree."""
    var count := 0
    if node is RigidBody3D and node.get_script() != null:
        var script_path := String(node.get_script().resource_path)
        if script_path.ends_with("DroppedItem.gd"):
            count += 1

    for child in node.get_children():
        if child is Node:
            count += _count_dropped_items_recursive(child)

    return count

func _update_inventory_stats() -> void:
    var inventory := _get_inventory()
    if inventory == null:
        _set_label_text("inventory_slots", "Inventory: N/A")
        _set_label_text("inventory_weight", "Weight: N/A")
        _set_label_text("hotbar_selected", "Hotbar: N/A")
        _set_label_color("inventory_slots", Color(0.7, 0.7, 0.7))
        _set_label_color("inventory_weight", Color(0.7, 0.7, 0.7))
        _set_label_color("hotbar_selected", Color(0.7, 0.7, 0.7))
        return

    var total_slots := 30
    if inventory.has_method("get_slot_count"):
        total_slots = int(inventory.call("get_slot_count"))

    var empty_slots := 0
    if inventory.has_method("get_empty_slot_count"):
        empty_slots = int(inventory.call("get_empty_slot_count"))

    var used_slots := max(total_slots - empty_slots, 0)
    _set_label_text("inventory_slots", "Inventory: %d/%d" % [used_slots, total_slots])

    var usage_ratio := 0.0
    if total_slots > 0:
        usage_ratio = float(used_slots) / float(total_slots)

    var slots_color := Color(0.6, 1.0, 0.6)
    if usage_ratio > 0.9:
        slots_color = Color(1.0, 0.4, 0.4)
    elif usage_ratio > 0.7:
        slots_color = Color(1.0, 0.8, 0.4)
    _set_label_color("inventory_slots", slots_color)

    var current_weight_variant := inventory.get("current_weight")
    var current_weight := float(current_weight_variant) if typeof(current_weight_variant) in [TYPE_FLOAT, TYPE_INT] else 0.0

    var max_weight := 100.0
    var max_weight_variant := inventory.get("MAX_WEIGHT")
    if typeof(max_weight_variant) in [TYPE_FLOAT, TYPE_INT]:
        max_weight = float(max_weight_variant)

    _set_label_text("inventory_weight", "Weight: %.1f/%.1f" % [current_weight, max_weight])

    var weight_ratio := 0.0
    if max_weight > 0.0:
        weight_ratio = current_weight / max_weight

    var weight_color := Color(0.6, 1.0, 0.6)
    if weight_ratio > 0.9:
        weight_color = Color(1.0, 0.4, 0.4)
    elif weight_ratio > 0.7:
        weight_color = Color(1.0, 0.8, 0.4)
    _set_label_color("inventory_weight", weight_color)

    var selected_hotbar_variant := inventory.get("selected_hotbar_slot")
    var selected_hotbar := int(selected_hotbar_variant) if typeof(selected_hotbar_variant) in [TYPE_INT, TYPE_FLOAT] else 0
    _set_label_text("hotbar_selected", "Hotbar: %d" % (selected_hotbar + 1))
    _set_label_color("hotbar_selected", Color(0.6, 0.9, 1.0))

func _get_inventory() -> Node:
    if typeof(Inventory) != TYPE_NIL and Inventory != null:
        return Inventory
    var tree := get_tree()
    if tree and tree.has_node("/root/Inventory"):
        return tree.get_node("/root/Inventory")
    return null

func _get_mining_system() -> Node:
    if typeof(MiningSystem) != TYPE_NIL and MiningSystem != null:
        return MiningSystem
    var tree := get_tree()
    if tree and tree.has_node("/root/MiningSystem"):
        return tree.get_node("/root/MiningSystem")
    return null

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

func _get_crafting_system() -> Node:
    if typeof(CraftingSystem) != TYPE_NIL and CraftingSystem != null:
        return CraftingSystem
    var tree := get_tree()
    if tree and tree.has_node("/root/CraftingSystem"):
        return tree.get_node("/root/CraftingSystem")
    return null

func _get_interaction_manager() -> Node:
    if typeof(InteractionManager) != TYPE_NIL and InteractionManager != null:
        return InteractionManager
    var tree := get_tree()
    if tree and tree.has_node("/root/InteractionManager"):
        return tree.get_node("/root/InteractionManager")
    return null
