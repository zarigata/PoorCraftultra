extends CanvasLayer
class_name BuildingWheelUI

const WHEEL_RADIUS := 120.0
const WHEEL_INNER_RADIUS := 36.0
const WHEEL_START_ANGLE := -90.0
const WHEEL_GAP_DEG := 2.5
const RING_COLOR := Color(0.12, 0.12, 0.14, 0.9)
const HIGHLIGHT_COLOR := Color(0.35, 0.65, 1.0, 0.85)
const OUTLINE_COLOR := Color(1.0, 1.0, 1.0, 0.12)
const TEXT_COLOR := Color(0.9, 0.9, 0.95, 1.0)
const DEADZONE := 0.25

@onready var radial_menu: Control = $RadialMenu
@onready var wheel_container: Control = $RadialMenu/WheelContainer
@onready var info_panel: Panel = $RadialMenu/InfoPanel
@onready var piece_name_label: Label = $RadialMenu/InfoPanel/InfoContainer/PieceNameLabel
@onready var piece_desc_label: Label = $RadialMenu/InfoPanel/InfoContainer/PieceDescLabel
@onready var materials_list: VBoxContainer = $RadialMenu/InfoPanel/InfoContainer/MaterialsList
@onready var tier_label: Label = $RadialMenu/InfoPanel/InfoContainer/TierSelector/TierLabel
@onready var tier_prev_button: Button = $RadialMenu/InfoPanel/InfoContainer/TierSelector/TierPrevButton
@onready var tier_value_label: Label = $RadialMenu/InfoPanel/InfoContainer/TierSelector/TierValueLabel
@onready var tier_next_button: Button = $RadialMenu/InfoPanel/InfoContainer/TierSelector/TierNextButton
@onready var hint_label: Label = $RadialMenu/HintLabel

var _building_system: BuildingSystem = null
var _inventory: Inventory = null
var _ui_manager: UIManager = null
var _game_manager: GameManager = null
var _input_manager: InputManager = null

var _categories: Array[String] = ["foundation", "walls", "floors", "roofs", "structural"]
var _category_pieces: Dictionary = {}
var _hover_index: int = -1
var _selected_category: String = "foundation"
var _wheel_center: Vector2 = Vector2.ZERO
var _is_open: bool = false
var _stick_dir: Vector2 = Vector2.ZERO

func _ready() -> void:
	visible = false
	set_process(false)
	set_process_unhandled_input(false)
	_lookup_autoloads()
	_connect_signals()
	_apply_theme()
	_register_with_ui_manager()
	radial_menu.draw.connect(_on_radial_menu_draw)
	_update_tier_text()
	_hint_position_update()

func _exit_tree() -> void:
	_disconnect_signals()
	if _ui_manager != null:
		_ui_manager.unregister_ui(self)

func open_wheel(screen_position: Vector2) -> void:
	if _building_system == null:
		return
	_wheel_center = screen_position
	wheel_container.position = screen_position - wheel_container.size * 0.5
	_hint_position_update()
	_refresh_category_cache()
	_hover_index = -1
	_is_open = true
	visible = true
	radial_menu.queue_redraw()
	set_process_unhandled_input(true)
	set_process(true)
	_update_tier_text()
	_log_debug("Building wheel opened at %s" % screen_position)

func close_wheel() -> void:
	_is_open = false
	visible = false
	set_process_unhandled_input(false)
	set_process(false)
	_hover_index = -1
	radial_menu.queue_redraw()
	_log_debug("Building wheel closed")

func _process(_delta: float) -> void:
	if not _is_open:
		return
	if _stick_dir.length() >= DEADZONE:
		var index := _index_from_vector(_stick_dir * WHEEL_RADIUS)
		if index != _hover_index:
			_hover_index = index
			radial_menu.queue_redraw()

func _unhandled_input(event: InputEvent) -> void:
	if not _is_open:
		return
	if event is InputEventMouseMotion:
		_update_hover_from_point(event.position)
	elif event is InputEventMouseButton:
		_handle_mouse_button(event)
	elif event is InputEventJoypadMotion:
		_handle_joypad_motion(event)
	elif event.is_action_pressed("ui_right"):
		_cycle_hover(1)
	elif event.is_action_pressed("ui_left"):
		_cycle_hover(-1)
	elif event.is_action_pressed("ui_accept"):
		_confirm_selection()
	elif event.is_action_pressed("ui_cancel"):
		_cancel_selection()

func _handle_mouse_button(event: InputEventMouseButton) -> void:
	if not event.pressed:
		return
	if event.button_index == MOUSE_BUTTON_LEFT:
		_confirm_selection()
	elif event.button_index == MOUSE_BUTTON_RIGHT:
		_cancel_selection()
	elif event.button_index == MOUSE_BUTTON_WHEEL_UP:
		_on_tier_next_pressed()
	elif event.button_index == MOUSE_BUTTON_WHEEL_DOWN:
		_on_tier_prev_pressed()

func _handle_joypad_motion(event: InputEventJoypadMotion) -> void:
	if abs(event.axis_value) < DEADZONE:
		return
	if event.axis == JOY_AXIS_LEFT_X:
		_stick_dir.x = event.axis_value
	elif event.axis == JOY_AXIS_LEFT_Y:
		_stick_dir.y = event.axis_value

func _cycle_hover(direction: int) -> void:
	if _categories.is_empty():
		return
	if _hover_index == -1:
		_hover_index = 0 if direction >= 0 else _categories.size() - 1
	else:
		_hover_index = (_hover_index + direction) % _categories.size()
	if _hover_index < 0:
		_hover_index += _categories.size()
	radial_menu.queue_redraw()

func _confirm_selection() -> void:
	var category := _get_hover_category()
	if category == "":
		return
	_on_category_selected(category)

func _cancel_selection() -> void:
	if _building_system != null:
		_building_system.exit_build_mode()
	close_wheel()

func _get_hover_category() -> String:
	if _hover_index < 0 or _hover_index >= _categories.size():
		return ""
	return _categories[_hover_index]

func _update_hover_from_point(point: Vector2) -> void:
	var vector := point - _wheel_center
	if vector.length() < WHEEL_INNER_RADIUS:
		_hover_index = -1
	else:
		_hover_index = _index_from_vector(vector)
	radial_menu.queue_redraw()

func _index_from_vector(vector: Vector2) -> int:
	if vector.length() < WHEEL_INNER_RADIUS:
		return -1
	var angle := rad_to_deg(atan2(vector.y, vector.x))
	angle = fposmod(angle - WHEEL_START_ANGLE, 360.0)
	var slice_angle := 360.0 / max(1, _categories.size())
	var adjusted := angle + slice_angle * 0.5
	var index := int(floor(adjusted / slice_angle)) % max(1, _categories.size())
	return clamp(index, 0, _categories.size() - 1)

func _on_radial_menu_draw() -> void:
	if not _is_open:
		return
	if _categories.is_empty():
		return
	var draw_origin := _wheel_center
	radial_menu.draw_circle(draw_origin, WHEEL_RADIUS, RING_COLOR)
	radial_menu.draw_circle(draw_origin, WHEEL_INNER_RADIUS, Color(0, 0, 0, 0.55))
	var slice_angle := 360.0 / _categories.size()
	for index in _categories.size():
		var start_angle := WHEEL_START_ANGLE + index * slice_angle + WHEEL_GAP_DEG
		var end_angle := WHEEL_START_ANGLE + (index + 1) * slice_angle - WHEEL_GAP_DEG
		var poly := _arc_ring_poly(draw_origin, WHEEL_INNER_RADIUS, WHEEL_RADIUS, start_angle, end_angle, 16)
		var color := HIGHLIGHT_COLOR if index == _hover_index else RING_COLOR
		radial_menu.draw_polygon(poly, PackedColorArray([color]))
		var mid_angle := deg_to_rad((start_angle + end_angle) * 0.5)
		var text_pos := draw_origin + Vector2(cos(mid_angle), sin(mid_angle)) * (WHEEL_RADIUS - 45.0)
		radial_menu.draw_string(_get_font(), text_pos, _categories[index].capitalize(), HORIZONTAL_ALIGNMENT_CENTER, -1, TEXT_COLOR)
	var outline_radius := WHEEL_RADIUS + 2.0
	radial_menu.draw_arc(draw_origin, outline_radius, deg_to_rad(WHEEL_START_ANGLE), deg_to_rad(360.0), 64, OUTLINE_COLOR, 2.0)
	radial_menu.draw_circle(draw_origin, WHEEL_INNER_RADIUS, Color(0, 0, 0, 0.55))

func _get_font() -> Font:
	var theme_font := get_theme_font("font", "Label")
	return theme_font if theme_font != null else DynamicFont.new()

func _arc_ring_poly(center: Vector2, r0: float, r1: float, a0: float, a1: float, steps: int) -> PackedVector2Array:
	var points: PackedVector2Array = []
	var step := max(1, steps)
	for i in range(step + 1):
		var angle := deg_to_rad(lerp(a0, a1, float(i) / step))
		points.append(center + Vector2(cos(angle), sin(angle)) * r1)
	for i in range(step, -1, -1):
		var angle := deg_to_rad(lerp(a0, a1, float(i) / step))
		points.append(center + Vector2(cos(angle), sin(angle)) * r0)
	return points

func _on_category_selected(category: String) -> void:
	_selected_category = category
	var pieces: Array = _category_pieces.get(category, [])
	if pieces.is_empty():
		return
	var piece := pieces[0] as BuildingPiece
	_on_piece_selected(piece)

func _on_piece_selected(piece: BuildingPiece) -> void:
	if piece == null or _building_system == null:
		return
	_building_system.select_piece(piece.piece_id, _building_system.selected_tier)
	_update_piece_info(piece)
	close_wheel()

func _update_piece_info(piece: BuildingPiece) -> void:
	if piece == null:
		piece_name_label.text = "Select a piece"
		piece_desc_label.text = ""
		_clear_materials_list()
		return
	piece_name_label.text = piece.display_name
	piece_desc_label.text = piece.description
	_update_materials_list(piece)
	_update_tier_text()

func _update_materials_list(piece: BuildingPiece) -> void:
	_clear_materials_list()
	if piece == null:
		return
	for item_id in piece.crafting_cost.keys():
		var quantity := int(piece.crafting_cost[item_id])
		var label := Label.new()
		label.text = "%s x%d" % [item_id.capitalize(), quantity]
		label.horizontal_alignment = HORIZONTAL_ALIGNMENT_LEFT
		var has_material := _inventory == null or _inventory.has_item(item_id, quantity)
		label.self_modulate = Color(0.6, 1.0, 0.6) if has_material else Color(1.0, 0.5, 0.5)
		materials_list.add_child(label)

func _clear_materials_list() -> void:
	for child in materials_list.get_children():
		child.queue_free()

func _on_tier_prev_pressed() -> void:
	if _building_system != null:
		_building_system.cycle_material_tier(-1)
		_update_tier_text()

func _on_tier_next_pressed() -> void:
	if _building_system != null:
		_building_system.cycle_material_tier(1)
		_update_tier_text()

func _update_tier_text() -> void:
	if _building_system == null:
		tier_value_label.text = "Wood"
		return
	var tier := _building_system.selected_tier
	tier_value_label.text = _tier_name(tier)

func _tier_name(tier: BuildingPiece.MaterialTier) -> String:
	match tier:
		BuildingPiece.MaterialTier.STONE:
			return "Stone"
		BuildingPiece.MaterialTier.METAL:
			return "Metal"
		_:
			return "Wood"

func _lookup_autoloads() -> void:
	if typeof(BuildingSystem) != TYPE_NIL and BuildingSystem != null:
		_building_system = BuildingSystem
	if typeof(Inventory) != TYPE_NIL and Inventory != null:
		_inventory = Inventory
	if typeof(UIManager) != TYPE_NIL and UIManager != null:
		_ui_manager = UIManager
	if typeof(GameManager) != TYPE_NIL and GameManager != null:
		_game_manager = GameManager
	if typeof(InputManager) != TYPE_NIL and InputManager != null:
		_input_manager = InputManager

func _connect_signals() -> void:
	if _building_system != null:
		if not _building_system.is_connected("selected_piece_changed", Callable(self, "_update_piece_info")):
			_building_system.selected_piece_changed.connect(_update_piece_info)
		if not _building_system.is_connected("build_mode_changed", Callable(self, "_on_build_mode_changed")):
			_building_system.build_mode_changed.connect(_on_build_mode_changed)
	tier_prev_button.pressed.connect(_on_tier_prev_pressed)
	tier_next_button.pressed.connect(_on_tier_next_pressed)

func _disconnect_signals() -> void:
	if _building_system != null:
		if _building_system.is_connected("selected_piece_changed", Callable(self, "_update_piece_info")):
			_building_system.disconnect("selected_piece_changed", Callable(self, "_update_piece_info"))
		if _building_system.is_connected("build_mode_changed", Callable(self, "_on_build_mode_changed")):
			_building_system.disconnect("build_mode_changed", Callable(self, "_on_build_mode_changed"))
	tier_prev_button.pressed.disconnect(_on_tier_prev_pressed)
	tier_next_button.pressed.disconnect(_on_tier_next_pressed)

func _on_build_mode_changed(enabled: bool) -> void:
	if not enabled:
		close_wheel()

func _refresh_category_cache() -> void:
	_category_pieces.clear()
	if _building_system == null:
		return
	for category in _categories:
		_category_pieces[category] = _building_system.get_available_pieces(category)

func _hint_position_update() -> void:
	var viewport_size := get_viewport().get_visible_rect().size
	hint_label.position = Vector2(0, viewport_size.y - hint_label.size.y - 48.0)
	hint_label.size = Vector2(viewport_size.x, hint_label.size.y)

func _apply_theme() -> void:
	if _ui_manager != null:
		_ui_manager.apply_theme_to_control(self)

func _register_with_ui_manager() -> void:
	if _ui_manager != null:
		_ui_manager.register_ui(self, "BuildingWheelUI")

func _log_debug(message: String) -> void:
	if typeof(ErrorLogger) != TYPE_NIL and ErrorLogger != null and ErrorLogger.has_method("log_debug"):
		ErrorLogger.log_debug(message, "BuildingWheelUI")

