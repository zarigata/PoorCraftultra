extends CanvasLayer
class_name InventoryUI

const SLOT_SCENE := preload("res://scenes/inventory_slot.tscn")
const INVENTORY_SLOT_COUNT := 30
const HOTBAR_SLOT_COUNT := 9
const STATUS_DISPLAY_TIME := 2.5
const DEFAULT_PANEL_SIZE := Vector2(64, 64)

@onready var panel: Panel = $Panel
@onready var title_label: Label = $Panel/MainContainer/TitleLabel
@onready var search_box: LineEdit = $Panel/MainContainer/TopBar/SearchBox
@onready var sort_button: Button = $Panel/MainContainer/TopBar/SortButton
@onready var weight_label: Label = $Panel/MainContainer/TopBar/WeightLabel
@onready var status_label: Label = $Panel/MainContainer/StatusLabel
@onready var inventory_grid: GridContainer = $Panel/MainContainer/InventoryScroll/InventoryGrid
@onready var hotbar_container: HBoxContainer = $Panel/MainContainer/HotbarContainer
@onready var status_timer: Timer = $StatusTimer

var _inventory: Inventory = null
var _input_manager: InputManager = null
var _game_manager: GameManager = null
var _mining_system: MiningSystem = null
var _ui_manager: UIManager = null

var _inventory_slots: Array[InventorySlot] = []
var _hotbar_slots: Array[InventorySlot] = []
var _icon_cache: Dictionary = {}
var _search_filter: String = ""
var _show_weight: bool = true
var _show_tooltips: bool = true
var _paused_game: bool = false

func _ready() -> void:
	layer = 60
	visible = false
	status_label.visible = false
	status_timer.wait_time = STATUS_DISPLAY_TIME
	status_timer.one_shot = true
	_lookup_autoloads()
	_create_inventory_slots()
	_create_hotbar_slots()
	_connect_signals()
	_refresh_settings()
	_refresh_all_slots()
	_update_hotbar_highlight()
	_update_weight_label()
	_apply_theme()
	_apply_layout_scale()
	_register_with_ui_manager()

func _exit_tree() -> void:
	if _ui_manager != null:
		if _ui_manager.is_connected("theme_changed", Callable(self, "_on_ui_theme_changed")):
			_ui_manager.disconnect("theme_changed", Callable(self, "_on_ui_theme_changed"))
		if _ui_manager.is_connected("resolution_changed", Callable(self, "_on_ui_resolution_changed")):
			_ui_manager.disconnect("resolution_changed", Callable(self, "_on_ui_resolution_changed"))
		_ui_manager.unregister_ui(self)
	if _game_manager != null and _game_manager.is_connected("settings_changed", Callable(self, "_on_settings_changed")):
		_game_manager.disconnect("settings_changed", Callable(self, "_on_settings_changed"))
	if _inventory != null:
		if _inventory.is_connected("inventory_changed", Callable(self, "_on_inventory_changed")):
			_inventory.disconnect("inventory_changed", Callable(self, "_on_inventory_changed"))
		if _inventory.is_connected("slot_updated", Callable(self, "_on_slot_updated")):
			_inventory.disconnect("slot_updated", Callable(self, "_on_slot_updated"))
		if _inventory.is_connected("hotbar_changed", Callable(self, "_on_hotbar_changed")):
			_inventory.disconnect("hotbar_changed", Callable(self, "_on_hotbar_changed"))
		if _inventory.is_connected("hotbar_selected", Callable(self, "_on_hotbar_selected")):
			_inventory.disconnect("hotbar_selected", Callable(self, "_on_hotbar_selected"))
		if _inventory.is_connected("capacity_exceeded", Callable(self, "_on_capacity_exceeded")):
			_inventory.disconnect("capacity_exceeded", Callable(self, "_on_capacity_exceeded"))
		if _inventory.is_connected("weight_exceeded", Callable(self, "_on_weight_exceeded")):
			_inventory.disconnect("weight_exceeded", Callable(self, "_on_weight_exceeded"))

func _lookup_autoloads() -> void:
	if typeof(Inventory) != TYPE_NIL and Inventory != null:
		_inventory = Inventory
	if typeof(InputManager) != TYPE_NIL and InputManager != null:
		_input_manager = InputManager
	if typeof(GameManager) != TYPE_NIL and GameManager != null:
		_game_manager = GameManager
	if typeof(MiningSystem) != TYPE_NIL and MiningSystem != null:
		_mining_system = MiningSystem
	if typeof(UIManager) != TYPE_NIL and UIManager != null:
		_ui_manager = UIManager

func _create_inventory_slots() -> void:
	inventory_grid.columns = 6
	for index in range(INVENTORY_SLOT_COUNT):
		var slot := SLOT_SCENE.instantiate() as InventorySlot
		slot.slot_index = index
		slot.is_hotbar = false
		slot.set_owner(self)
		slot.visible = true
		slot.slot_gui_input.connect(Callable(self, "_on_slot_gui_input").bind(slot))
		_inventory_slots.append(slot)
		inventory_grid.add_child(slot)

func _create_hotbar_slots() -> void:
	hotbar_container.alignment = BoxContainer.ALIGNMENT_CENTER
	for index in range(HOTBAR_SLOT_COUNT):
		var slot := SLOT_SCENE.instantiate() as InventorySlot
		slot.slot_index = -1
		slot.is_hotbar = true
		slot.set_owner(self)
		slot.set_hotkey_label(str(index + 1))
		slot.slot_gui_input.connect(Callable(self, "_on_slot_gui_input").bind(slot))
		_hotbar_slots.append(slot)
		hotbar_container.add_child(slot)

func _connect_signals() -> void:
	search_box.text_changed.connect(_on_search_text_changed)
	sort_button.pressed.connect(_on_sort_button_pressed)
	status_timer.timeout.connect(_on_status_timer_timeout)
	if _inventory != null:
		if not _inventory.is_connected("inventory_changed", Callable(self, "_on_inventory_changed")):
			_inventory.connect("inventory_changed", Callable(self, "_on_inventory_changed"))
		if not _inventory.is_connected("slot_updated", Callable(self, "_on_slot_updated")):
			_inventory.connect("slot_updated", Callable(self, "_on_slot_updated"))
		if not _inventory.is_connected("hotbar_changed", Callable(self, "_on_hotbar_changed")):
			_inventory.connect("hotbar_changed", Callable(self, "_on_hotbar_changed"))
		if not _inventory.is_connected("hotbar_selected", Callable(self, "_on_hotbar_selected")):
			_inventory.connect("hotbar_selected", Callable(self, "_on_hotbar_selected"))
		if not _inventory.is_connected("capacity_exceeded", Callable(self, "_on_capacity_exceeded")):
			_inventory.connect("capacity_exceeded", Callable(self, "_on_capacity_exceeded"))
		if not _inventory.is_connected("weight_exceeded", Callable(self, "_on_weight_exceeded")):
			_inventory.connect("weight_exceeded", Callable(self, "_on_weight_exceeded"))
	if _game_manager != null and not _game_manager.is_connected("settings_changed", Callable(self, "_on_settings_changed")):
		_game_manager.connect("settings_changed", Callable(self, "_on_settings_changed"))
	if _ui_manager != null and not _ui_manager.is_connected("theme_changed", Callable(self, "_on_ui_theme_changed")):
		_ui_manager.connect("theme_changed", Callable(self, "_on_ui_theme_changed"))
	if _ui_manager != null and not _ui_manager.is_connected("resolution_changed", Callable(self, "_on_ui_resolution_changed")):
		_ui_manager.connect("resolution_changed", Callable(self, "_on_ui_resolution_changed"))

func _on_settings_changed(setting_name: String, new_value: Variant) -> void:
	if setting_name.begins_with("ui/"):
		_apply_theme()
		_apply_layout_scale()
		return
	if not setting_name.begins_with("inventory/"):
		return
	_refresh_settings()
	_refresh_all_slots()
	_update_weight_label()

func _apply_theme() -> void:
	if _ui_manager == null:
		ErrorLogger.log_warning("UIManager not available; using default theme", "InventoryUI")
		return
	_ui_manager.apply_theme_to_control(self)
	ErrorLogger.log_debug("Theme applied to InventoryUI", "InventoryUI")

func _register_with_ui_manager() -> void:
	if _ui_manager == null:
		return
	_ui_manager.register_ui(self, "InventoryUI")

func _on_ui_theme_changed() -> void:
	_apply_theme()
	_apply_layout_scale()

func _on_ui_resolution_changed(_new_size: Vector2) -> void:
	_apply_layout_scale()

func _refresh_settings() -> void:
	if _game_manager != null:
		_show_weight = bool(_game_manager.get_setting("inventory/show_weight", true))
		_show_tooltips = bool(_game_manager.get_setting("inventory/show_tooltips", true))
	else:
		_show_weight = true
		_show_tooltips = true
	weight_label.visible = _show_weight

func _on_inventory_changed() -> void:
	_refresh_all_slots()
	_update_weight_label()

func _on_slot_updated(slot_index: int, _item_id: String, _quantity: int) -> void:
	_update_inventory_slot(slot_index)
	_update_hotbar_slots_for_inventory(slot_index)
	_update_weight_label()

func _on_hotbar_changed(hotbar_index: int, _inventory_slot: int) -> void:
	_update_hotbar_slot(hotbar_index)

func _on_hotbar_selected(hotbar_index: int) -> void:
	_update_hotbar_highlight(hotbar_index)

func _on_capacity_exceeded(item_id: String, _quantity: int) -> void:
	_show_status_message("Inventory Full! (%s)" % item_id.capitalize())

func _on_weight_exceeded(current_weight: float, max_weight: float) -> void:
	_show_status_message("Too Heavy! (%.1f/%.1f)" % [current_weight, max_weight])
	_update_weight_label()

func _refresh_all_slots() -> void:
	for i in range(INVENTORY_SLOT_COUNT):
		_update_inventory_slot(i)
	for i in range(HOTBAR_SLOT_COUNT):
		_update_hotbar_slot(i)
	_apply_search_filter()

func _update_inventory_slot(slot_index: int) -> void:
	if _inventory == null or slot_index < 0 or slot_index >= _inventory_slots.size():
		return
	var slot := _inventory_slots[slot_index]
	var slot_data := _inventory.get_slot(slot_index)
	var item_id := String(slot_data.get("item_id", ""))
	var quantity := int(slot_data.get("quantity", 0))
	if item_id.is_empty() or quantity <= 0:
		slot.clear_slot()
		slot.set_search_match(_search_filter.is_empty())
	else:
		var resource := _inventory.get_resource_definition(item_id)
		var icon := _get_icon_for_resource(resource)
		slot.set_slot_data(item_id, quantity, resource, icon, _show_tooltips)
		slot.set_search_match(_does_slot_match_filter(resource))
	_apply_search_filter_for_slot(slot)

func _update_hotbar_slot(hotbar_index: int) -> void:
	if _inventory == null or hotbar_index < 0 or hotbar_index >= _hotbar_slots.size():
		return
	var slot := _hotbar_slots[hotbar_index]
	var hotbar_indices: Array = _inventory.get_hotbar_indices()
	if hotbar_index >= hotbar_indices.size():
		return
	var inventory_slot := int(hotbar_indices[hotbar_index])
	slot.slot_index = inventory_slot
	if inventory_slot == -1:
		slot.clear_slot()
		return
	var slot_data := _inventory.get_slot(inventory_slot)
	var item_id := String(slot_data.get("item_id", ""))
	var quantity := int(slot_data.get("quantity", 0))
	if item_id.is_empty() or quantity <= 0:
		slot.clear_slot()
	else:
		var resource := _inventory.get_resource_definition(item_id)
		var icon := _get_icon_for_resource(resource)
		slot.set_slot_data(item_id, quantity, resource, icon, _show_tooltips)

func _update_hotbar_slots_for_inventory(inventory_slot: int) -> void:
	if _inventory == null:
		return
	var hotbar_indices: Array = _inventory.get_hotbar_indices()
	for i in range(min(HOTBAR_SLOT_COUNT, hotbar_indices.size())):
		if int(hotbar_indices[i]) == inventory_slot:
			_update_hotbar_slot(i)

func _update_hotbar_highlight(selected: int = -1) -> void:
	if selected == -1 and _inventory != null:
		selected = _inventory.get_selected_hotbar_index()
	for index in range(_hotbar_slots.size()):
		_hotbar_slots[index].set_highlighted(index == selected)

func _apply_search_filter() -> void:
	for slot in _inventory_slots:
		_apply_search_filter_for_slot(slot)

func _apply_search_filter_for_slot(slot: InventorySlot) -> void:
	if _search_filter.is_empty():
		slot.visible = true
		return
	if slot.item_id.is_empty():
		slot.visible = false
		return
	var resource := _inventory.get_resource_definition(slot.item_id) if _inventory != null else null
	slot.visible = resource != null and _does_slot_match_filter(resource)

func _does_slot_match_filter(resource: GameResource) -> bool:
	if resource == null:
		return false
	if _search_filter.is_empty():
		return true
	return resource.display_name.to_lower().contains(_search_filter)

func _update_weight_label() -> void:
	if not _show_weight:
		weight_label.visible = false
		return
	weight_label.visible = true
	if _inventory == null:
		weight_label.text = "Weight: N/A"
		weight_label.self_modulate = _get_palette_color("text_dim", Color(0.7, 0.7, 0.7))
		return
	var current := float(_inventory.get_current_weight())
	var max_weight := float(Inventory.MAX_WEIGHT)
	weight_label.text = "Weight: %.1f/%.1f" % [current, max_weight]
	var ratio := max_weight > 0.0 ? current / max_weight : 0.0
	var color := _get_palette_color("success", Color(0.6, 1.0, 0.6))
	if ratio > 0.9:
		color = _get_palette_color("error", Color(1.0, 0.4, 0.4))
	elif ratio > 0.7:
		color = _get_palette_color("warning", Color(1.0, 0.8, 0.4))
	weight_label.self_modulate = color

func _apply_layout_scale() -> void:
	if _ui_manager == null or panel == null:
		return
	var scale := _ui_manager.get_ui_scale()
	var safe_rect := _ui_manager.get_safe_area_rect()
	panel.scale = Vector2.ONE * scale
	var panel_size := panel.get_combined_minimum_size() * panel.scale
	var target_position := safe_rect.position + (safe_rect.size - panel_size) * 0.5
	panel.position = target_position

func _get_palette_color(name: String, fallback: Color) -> Color:
	return _ui_manager.get_color(name) if _ui_manager != null else fallback

const PLACEHOLDER_ICON_PATH := "res://assets/icons/items/_placeholder.png"

func _get_icon_for_resource(resource: GameResource) -> Texture2D:
	var path := ""
	if resource != null:
		path = resource.icon_path
		if not path.is_empty() and _icon_cache.has(path):
			return _icon_cache[path]
		if path.is_empty() or not ResourceLoader.exists(path):
			path = PLACEHOLDER_ICON_PATH
	else:
		path = PLACEHOLDER_ICON_PATH
	if path.is_empty():
		return null
	if _icon_cache.has(path):
		return _icon_cache[path]
	if not ResourceLoader.exists(path):
		return null
	var texture := load(path)
	if texture is Texture2D:
		_icon_cache[path] = texture
		return texture
	return null

func _on_search_text_changed(new_text: String) -> void:
	_search_filter = new_text.strip_edges().to_lower()
	_apply_search_filter()

func _on_sort_button_pressed() -> void:
	if _inventory != null:
		_inventory.auto_sort()

func _on_slot_gui_input(event: InputEvent, slot: InventorySlot) -> void:
	if event is InputEventMouseButton and event.button_index == MOUSE_BUTTON_RIGHT and event.pressed and not slot.item_id.is_empty():
		_show_status_message("%s x%d" % [slot.item_id.capitalize(), slot.quantity])

func _create_drag_preview(slot: InventorySlot) -> Control:
	var preview := Panel.new()
	preview.size = DEFAULT_PANEL_SIZE
	preview.custom_minimum_size = DEFAULT_PANEL_SIZE
	preview.self_modulate = Color(1, 1, 1, 0.85)
	var tex := TextureRect.new()
	tex.texture = slot.icon.texture
	tex.expand_mode = TextureRect.EXPAND_IGNORE_SIZE
	tex.stretch_mode = TextureRect.STRETCH_KEEP_ASPECT_CENTERED
	tex.anchor_right = 1.0
	tex.anchor_bottom = 1.0
	tex.offset_right = 0
	tex.offset_bottom = 0
	preview.add_child(tex)
	if slot.quantity > 1:
		var label := Label.new()
		label.text = str(slot.quantity)
		label.horizontal_alignment = HORIZONTAL_ALIGNMENT_RIGHT
		label.vertical_alignment = VERTICAL_ALIGNMENT_BOTTOM
		label.anchor_right = 1.0
		label.anchor_bottom = 1.0
		label.offset_right = -6
		label.offset_bottom = -4
		preview.add_child(label)
	return preview

func _build_drag_data(slot: InventorySlot) -> Dictionary:
	var data := {
		"item_id": slot.item_id,
		"quantity": slot.quantity,
	}
	if slot.is_hotbar:
		var hotbar_index := _hotbar_slots.find(slot)
		var inventory_slot := -1
		if _inventory != null:
			var hotbar_indices := _inventory.get_hotbar_indices()
			if hotbar_index >= 0 and hotbar_index < hotbar_indices.size():
				inventory_slot = int(hotbar_indices[hotbar_index])
		data["source"] = "hotbar"
		data["hotbar_index"] = hotbar_index
		data["inventory_slot"] = inventory_slot
	else:
		data["source"] = "inventory"
		data["slot_index"] = slot.slot_index
	return data

func _can_drop_data_on_slot(slot: InventorySlot, data: Variant) -> bool:
	if typeof(data) != TYPE_DICTIONARY:
		return false
	var source := String(data.get("source", ""))
	if slot.is_hotbar:
		return source in ["inventory", "hotbar"] and not String(data.get("item_id", "")).is_empty()
	else:
		return source in ["inventory", "hotbar"]

func _handle_drop_on_slot(slot: InventorySlot, data: Variant) -> void:
	if _inventory == null or typeof(data) != TYPE_DICTIONARY:
		return
	var source := String(data.get("source", ""))
	if slot.is_hotbar:
		var target_hotbar := _hotbar_slots.find(slot)
		if target_hotbar == -1:
			return
		if source == "inventory":
			var source_slot := int(data.get("slot_index", -1))
			_inventory.assign_hotbar_slot(target_hotbar, source_slot)
		elif source == "hotbar":
			var source_hotbar := int(data.get("hotbar_index", -1))
			if source_hotbar == target_hotbar:
				return
			var hotbar_indices: Array = _inventory.get_hotbar_indices()
			if source_hotbar < 0 or source_hotbar >= hotbar_indices.size():
				return
			if target_hotbar < 0 or target_hotbar >= hotbar_indices.size():
				return
			var source_slot := int(hotbar_indices[source_hotbar])
			var target_slot := int(hotbar_indices[target_hotbar])
			_inventory.assign_hotbar_slot(source_hotbar, target_slot)
			_inventory.assign_hotbar_slot(target_hotbar, source_slot)
	else:
		var target_slot := slot.slot_index
		if source == "inventory":
			var source_slot := int(data.get("slot_index", -1))
			if source_slot != target_slot:
				_inventory.move_item(source_slot, target_slot)
		elif source == "hotbar":
			var hotbar_index := int(data.get("hotbar_index", -1))
			_inventory.assign_hotbar_slot(hotbar_index, target_slot)

func _show_status_message(text: String) -> void:
	status_label.text = text
	status_label.visible = true
	status_timer.start()

func _on_status_timer_timeout() -> void:
	status_label.visible = false

func toggle_inventory() -> void:
	if visible:
		hide_inventory()
	else:
		show_inventory()

func show_inventory() -> void:
	if _inventory == null:
		return
	visible = true
	if _game_manager != null and not _game_manager.is_paused:
		_game_manager.pause_game()
		_paused_game = true
	else:
		_paused_game = false
	Input.set_mouse_mode(Input.MOUSE_MODE_VISIBLE)
	_refresh_all_slots()
	search_box.grab_focus()

func hide_inventory() -> void:
	visible = false
	search_box.release_focus()
	if _paused_game and _game_manager != null:
		_game_manager.resume_game()
		_paused_game = false
	Input.set_mouse_mode(Input.MOUSE_MODE_CAPTURED)


func _input(event: InputEvent) -> void:
	if event.is_action_pressed("inventory"):
		toggle_inventory()
		get_tree().set_input_as_handled()
		return
	if event.is_action_pressed("hotbar_next"):
		if _inventory != null:
			_inventory.cycle_hotbar(1)
		get_tree().set_input_as_handled()
		return
	if event.is_action_pressed("hotbar_prev"):
		if _inventory != null:
			_inventory.cycle_hotbar(-1)
		get_tree().set_input_as_handled()
		return
	if event.is_action_pressed("undo_edit"):
		if _mining_system != null:
			_mining_system.undo_last_edit()
		get_tree().set_input_as_handled()
		return
	if event.is_action_pressed("redo_edit"):
		if _mining_system != null:
			_mining_system.redo_last_edit()
		get_tree().set_input_as_handled()
		return
	if event is InputEventKey and event.pressed and not event.echo:
		if search_box.has_focus() and not event.ctrl_pressed:
			return
		if _inventory == null:
			return
		for i in range(HOTBAR_SLOT_COUNT):
			var action_name := "hotbar_%d" % (i + 1)
			if event.is_action_pressed(action_name):
				_inventory.select_hotbar_slot(i)
				get_tree().set_input_as_handled()
				return

func _exit_tree() -> void:
	if _inventory != null:
		if _inventory.is_connected("inventory_changed", Callable(self, "_on_inventory_changed")):
			_inventory.disconnect("inventory_changed", Callable(self, "_on_inventory_changed"))
		if _inventory.is_connected("slot_updated", Callable(self, "_on_slot_updated")):
			_inventory.disconnect("slot_updated", Callable(self, "_on_slot_updated"))
		if _inventory.is_connected("hotbar_changed", Callable(self, "_on_hotbar_changed")):
			_inventory.disconnect("hotbar_changed", Callable(self, "_on_hotbar_changed"))
		if _inventory.is_connected("hotbar_selected", Callable(self, "_on_hotbar_selected")):
			_inventory.disconnect("hotbar_selected", Callable(self, "_on_hotbar_selected"))
		if _inventory.is_connected("capacity_exceeded", Callable(self, "_on_capacity_exceeded")):
			_inventory.disconnect("capacity_exceeded", Callable(self, "_on_capacity_exceeded"))
		if _inventory.is_connected("weight_exceeded", Callable(self, "_on_weight_exceeded")):
			_inventory.disconnect("weight_exceeded", Callable(self, "_on_weight_exceeded"))
	if _game_manager != null and _game_manager.is_connected("settings_changed", Callable(self, "_on_settings_changed")):
		_game_manager.disconnect("settings_changed", Callable(self, "_on_settings_changed"))
	if _ui_manager != null:
		if _ui_manager.is_connected("theme_changed", Callable(self, "_on_ui_theme_changed")):
			_ui_manager.disconnect("theme_changed", Callable(self, "_on_ui_theme_changed"))
		_ui_manager.unregister_ui(self)
