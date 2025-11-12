extends CanvasLayer
class_name MiningProgressUI

@onready var container: Control = $Container
@onready var voxel_label: Label = $Container/VBox/VoxelLabel
@onready var progress_bar: ProgressBar = $Container/VBox/ProgressBar
@onready var tool_label: Label = $Container/VBox/ToolLabel

var _mining_system: MiningSystem = null
var _is_visible: bool = false
var _ui_manager: UIManager = null

func _ready() -> void:
	layer = 50
	_lookup_mining_system()
	_lookup_ui_manager()
	_hide_ui()
	if _mining_system != null:
		_connect_signals()
		_update_tool_label()
	else:
		ErrorLogger.log_warning("MiningSystem not available; MiningProgressUI idle", "MiningProgressUI")
	_apply_theme()
	_apply_layout_scale()
	_connect_ui_manager_signals()
	_register_with_ui_manager()
	ErrorLogger.log_info("MiningProgressUI ready", "MiningProgressUI")

func _exit_tree() -> void:
	_disconnect_signals()
	if _ui_manager != null:
		if _ui_manager.is_connected("theme_changed", Callable(self, "_on_ui_theme_changed")):
			_ui_manager.disconnect("theme_changed", Callable(self, "_on_ui_theme_changed"))
		if _ui_manager.is_connected("resolution_changed", Callable(self, "_on_ui_resolution_changed")):
			_ui_manager.disconnect("resolution_changed", Callable(self, "_on_ui_resolution_changed"))
		_ui_manager.unregister_ui(self)

func _process(_delta: float) -> void:
	# Intentionally left for future animations
	pass

func _on_mining_started(_target_position: Vector3, voxel_type: Dictionary) -> void:
	_show_ui()
	voxel_label.text = "Mining: %s" % voxel_type.get("name", "Unknown")
	progress_bar.value = 0.0
	_update_tool_label()

func _on_mining_progress(progress: float) -> void:
	progress_bar.value = clamp(progress, 0.0, 1.0)
	_update_tool_label()

func _on_mining_completed(_target_position: Vector3, _drops: Array) -> void:
	_hide_ui()

func _on_mining_cancelled() -> void:
	_hide_ui()

func _on_tool_changed(_new_tool: Tool) -> void:
	_update_tool_label()

func _hide_ui() -> void:
	_is_visible = false
	visible = false

func _show_ui() -> void:
	_is_visible = true
	visible = true

func _update_tool_label() -> void:
	if _mining_system == null:
		tool_label.text = "Tool: N/A"
		tool_label.self_modulate = _get_palette_color("text_dim", Color(0.7, 0.7, 0.7))
		return
	var tool := _mining_system.get_active_tool() if _mining_system.has_method("get_active_tool") else null
	if tool == null:
		tool_label.text = "Tool: None"
		tool_label.self_modulate = _get_palette_color("text_dim", Color(0.7, 0.7, 0.7))
		return
	var durability := tool.get_durability_percentage() if tool.has_method("get_durability_percentage") else 1.0
	tool_label.text = "%s (%.0f%%)" % [tool.tool_name, durability * 100.0]
	var color := _get_palette_color("success", Color(0.6, 1.0, 0.6))
	if durability < 0.25:
		color = _get_palette_color("error", Color(1.0, 0.4, 0.4))
	elif durability < 0.5:
		color = _get_palette_color("warning", Color(1.0, 0.8, 0.4))
	tool_label.self_modulate = color

func _connect_signals() -> void:
	if _mining_system == null:
		return
	if not _mining_system.mining_started.is_connected(_on_mining_started):
		_mining_system.mining_started.connect(_on_mining_started)
	if not _mining_system.mining_progress.is_connected(_on_mining_progress):
		_mining_system.mining_progress.connect(_on_mining_progress)
	if not _mining_system.mining_completed.is_connected(_on_mining_completed):
		_mining_system.mining_completed.connect(_on_mining_completed)
	if not _mining_system.mining_cancelled.is_connected(_on_mining_cancelled):
		_mining_system.mining_cancelled.connect(_on_mining_cancelled)
	if not _mining_system.tool_changed.is_connected(_on_tool_changed):
		_mining_system.tool_changed.connect(_on_tool_changed)

func _disconnect_signals() -> void:
	if _mining_system == null:
		return
	if _mining_system.mining_started.is_connected(_on_mining_started):
		_mining_system.mining_started.disconnect(_on_mining_started)
	if _mining_system.mining_progress.is_connected(_on_mining_progress):
		_mining_system.mining_progress.disconnect(_on_mining_progress)
	if _mining_system.mining_completed.is_connected(_on_mining_completed):
		_mining_system.mining_completed.disconnect(_on_mining_completed)
	if _mining_system.mining_cancelled.is_connected(_on_mining_cancelled):
		_mining_system.mining_cancelled.disconnect(_on_mining_cancelled)
	if _mining_system.tool_changed.is_connected(_on_tool_changed):
		_mining_system.tool_changed.disconnect(_on_tool_changed)

func _lookup_mining_system() -> void:
	if typeof(MiningSystem) != TYPE_NIL and MiningSystem != null:
		_mining_system = MiningSystem
		return
	var tree := get_tree()
	if tree and tree.has_node("/root/MiningSystem"):
		_mining_system = tree.get_node("/root/MiningSystem")

func _lookup_ui_manager() -> void:
	if typeof(UIManager) != TYPE_NIL and UIManager != null:
		_ui_manager = UIManager

func _apply_theme() -> void:
	if _ui_manager == null:
		ErrorLogger.log_warning("UIManager not available; using default theme", "MiningProgressUI")
		return
	_ui_manager.apply_theme_to_control(self)
	ErrorLogger.log_debug("Theme applied to MiningProgressUI", "MiningProgressUI")
	_apply_layout_scale()

func _apply_layout_scale() -> void:
	if _ui_manager == null or container == null:
		return
	var scale := _ui_manager.get_ui_scale()
	var safe_rect := _ui_manager.get_safe_area_rect()
	container.scale = Vector2.ONE * scale
	var container_size := container.get_combined_minimum_size() * container.scale
	var target_position := safe_rect.position + (safe_rect.size - container_size) * 0.5
	container.position = target_position

func _get_palette_color(name: String, fallback: Color) -> Color:
	return _ui_manager.get_color(name) if _ui_manager != null else fallback

func _register_with_ui_manager() -> void:
	if _ui_manager == null:
		return
	_ui_manager.register_ui(self, "MiningProgressUI")

func _connect_ui_manager_signals() -> void:
	if _ui_manager == null:
		return
	if not _ui_manager.is_connected("theme_changed", Callable(self, "_on_ui_theme_changed")):
		_ui_manager.connect("theme_changed", Callable(self, "_on_ui_theme_changed"))
 	if not _ui_manager.is_connected("resolution_changed", Callable(self, "_on_ui_resolution_changed")):
 		_ui_manager.connect("resolution_changed", Callable(self, "_on_ui_resolution_changed"))

func _on_ui_theme_changed() -> void:
	_apply_theme()
	_apply_layout_scale()

func _on_ui_resolution_changed(_new_size: Vector2) -> void:
	_apply_layout_scale()
