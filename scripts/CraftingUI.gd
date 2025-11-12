extends CanvasLayer
class_name CraftingUI

const RECIPE_BUTTON_HEIGHT := 48
const QUEUE_ENTRY_HEIGHT := 40
const STATUS_DISPLAY_TIME := 2.5
const CATEGORY_OPTIONS := [
	{"label": "All", "value": "all"},
	{"label": "Basic", "value": "basic"},
	{"label": "Tools", "value": "tools"},
	{"label": "Components", "value": "components"},
	{"label": "Furniture", "value": "furniture"},
	{"label": "Smelting", "value": "smelting"},
]
const DEFAULT_MAX_QUEUE_SIZE := 10

@onready var panel: Panel = $Panel
@onready var title_label: Label = $Panel/MainContainer/TitleBar/TitleLabel
@onready var close_button: Button = $Panel/MainContainer/TitleBar/CloseButton
@onready var recipe_search_box: LineEdit = $Panel/MainContainer/ContentContainer/RecipeListContainer/RecipeSearchBar/RecipeSearchBox
@onready var category_filter: OptionButton = $Panel/MainContainer/ContentContainer/RecipeListContainer/RecipeSearchBar/CategoryFilter
@onready var recipe_list: VBoxContainer = $Panel/MainContainer/ContentContainer/RecipeListContainer/RecipeScroll/RecipeList
@onready var recipe_name_label: Label = $Panel/MainContainer/ContentContainer/RecipeDetailsContainer/RecipeNameLabel
@onready var recipe_desc_label: Label = $Panel/MainContainer/ContentContainer/RecipeDetailsContainer/RecipeDescLabel
@onready var inputs_list: VBoxContainer = $Panel/MainContainer/ContentContainer/RecipeDetailsContainer/InputsList
@onready var outputs_list: VBoxContainer = $Panel/MainContainer/ContentContainer/RecipeDetailsContainer/OutputsList
@onready var quantity_spinbox: SpinBox = $Panel/MainContainer/ContentContainer/RecipeDetailsContainer/CraftControls/QuantitySpinBox
@onready var craft_button: Button = $Panel/MainContainer/ContentContainer/RecipeDetailsContainer/CraftControls/CraftButton
@onready var queue_label: Label = $Panel/MainContainer/QueueContainer/QueueLabel
@onready var queue_list: VBoxContainer = $Panel/MainContainer/QueueContainer/QueueScroll/QueueList
@onready var active_craft_panel: Panel = $Panel/MainContainer/ActiveCraftPanel
@onready var active_craft_label: Label = $Panel/MainContainer/ActiveCraftPanel/ActiveCraftContainer/ActiveCraftLabel
@onready var craft_progress_bar: ProgressBar = $Panel/MainContainer/ActiveCraftPanel/ActiveCraftContainer/CraftProgressBar
@onready var time_remaining_label: Label = $Panel/MainContainer/ActiveCraftPanel/ActiveCraftContainer/ActiveCraftButtons/TimeRemainingLabel
@onready var cancel_button: Button = $Panel/MainContainer/ActiveCraftPanel/ActiveCraftContainer/ActiveCraftButtons/CancelButton
@onready var status_label: Label = $Panel/MainContainer/StatusLabel
@onready var status_timer: Timer = $StatusTimer

var _crafting_system: CraftingSystem = null
var _inventory: Inventory = null
var _ui_manager: UIManager = null
var _game_manager: GameManager = null

var _current_station_type: String = "none"
var _selected_recipe_id: String = ""
var _active_recipe_id: String = ""
var _search_filter: String = ""
var _category_filter: String = "all"
var _paused_game: bool = false

var _icon_cache: Dictionary = {}
var _recipe_button_map: Dictionary = {}

func _ready() -> void:
	layer = 65
	visible = false
	status_timer.wait_time = STATUS_DISPLAY_TIME
	status_timer.one_shot = true
	status_label.visible = false
	_setup_category_filter()
	_lookup_autoloads()
	_connect_autoload_signals()
	_connect_ui_signals()
	_apply_theme()
	_apply_layout_scale()
	_register_with_ui_manager()

func _exit_tree() -> void:
	_disconnect_autoload_signals()
	_disconnect_ui_signals()
	if _ui_manager != null:
		_ui_manager.unregister_ui(self)

func open_for_station(station_type: String) -> void:
	_current_station_type = station_type
	title_label.text = "Crafting - %s" % station_type.capitalize()
	_refresh_recipe_list()
	_refresh_queue_display()
	_update_active_craft_panel()
	_show_ui()
	_log_debug("Crafting UI opened for station %s" % station_type)

func close_ui() -> void:
	_hide_ui()
	_current_station_type = "none"
	_log_debug("Crafting UI closed")

func _input(event: InputEvent) -> void:
	if not visible:
		return
	if event.is_action_pressed("ui_cancel"):
		close_ui()
		get_tree().set_input_as_handled()

func _lookup_autoloads() -> void:
	if typeof(CraftingSystem) != TYPE_NIL and CraftingSystem != null:
		_crafting_system = CraftingSystem
	if typeof(Inventory) != TYPE_NIL and Inventory != null:
		_inventory = Inventory
	if typeof(UIManager) != TYPE_NIL and UIManager != null:
		_ui_manager = UIManager
	if typeof(GameManager) != TYPE_NIL and GameManager != null:
		_game_manager = GameManager

func _connect_autoload_signals() -> void:
	if _crafting_system != null:
		_crafting_system.queue_updated.connect(_on_queue_updated, CONNECT_REFERENCE_COUNTED)
		_crafting_system.recipe_started.connect(_on_recipe_started, CONNECT_REFERENCE_COUNTED)
		_crafting_system.recipe_progress.connect(_on_recipe_progress, CONNECT_REFERENCE_COUNTED)
		_crafting_system.recipe_completed.connect(_on_recipe_completed, CONNECT_REFERENCE_COUNTED)
		_crafting_system.recipe_cancelled.connect(_on_recipe_cancelled, CONNECT_REFERENCE_COUNTED)
		_crafting_system.recipe_failed.connect(_on_recipe_failed, CONNECT_REFERENCE_COUNTED)
	if _inventory != null:
		_inventory.inventory_changed.connect(_on_inventory_changed, CONNECT_REFERENCE_COUNTED)
	if _ui_manager != null:
		_ui_manager.theme_changed.connect(_on_ui_theme_changed, CONNECT_REFERENCE_COUNTED)
		_ui_manager.resolution_changed.connect(_on_ui_resolution_changed, CONNECT_REFERENCE_COUNTED)
	if _game_manager != null:
		_game_manager.settings_changed.connect(_on_settings_changed, CONNECT_REFERENCE_COUNTED)

func _disconnect_autoload_signals() -> void:
	if _crafting_system != null:
		if _crafting_system.queue_updated.is_connected(_on_queue_updated):
			_crafting_system.queue_updated.disconnect(_on_queue_updated)
		if _crafting_system.recipe_started.is_connected(_on_recipe_started):
			_crafting_system.recipe_started.disconnect(_on_recipe_started)
		if _crafting_system.recipe_progress.is_connected(_on_recipe_progress):
			_crafting_system.recipe_progress.disconnect(_on_recipe_progress)
		if _crafting_system.recipe_completed.is_connected(_on_recipe_completed):
			_crafting_system.recipe_completed.disconnect(_on_recipe_completed)
		if _crafting_system.recipe_cancelled.is_connected(_on_recipe_cancelled):
			_crafting_system.recipe_cancelled.disconnect(_on_recipe_cancelled)
		if _crafting_system.recipe_failed.is_connected(_on_recipe_failed):
			_crafting_system.recipe_failed.disconnect(_on_recipe_failed)
	if _inventory != null and _inventory.inventory_changed.is_connected(_on_inventory_changed):
		_inventory.inventory_changed.disconnect(_on_inventory_changed)
	if _ui_manager != null:
		if _ui_manager.theme_changed.is_connected(_on_ui_theme_changed):
			_ui_manager.theme_changed.disconnect(_on_ui_theme_changed)
		if _ui_manager.resolution_changed.is_connected(_on_ui_resolution_changed):
			_ui_manager.resolution_changed.disconnect(_on_ui_resolution_changed)
	if _game_manager != null and _game_manager.settings_changed.is_connected(_on_settings_changed):
		_game_manager.settings_changed.disconnect(_on_settings_changed)

func _connect_ui_signals() -> void:
	close_button.pressed.connect(_on_close_button_pressed)
	recipe_search_box.text_changed.connect(_on_recipe_search_changed)
	category_filter.item_selected.connect(_on_category_filter_changed)
	craft_button.pressed.connect(_on_craft_button_pressed)
	cancel_button.pressed.connect(_on_cancel_button_pressed)
	status_timer.timeout.connect(_on_status_timer_timeout)

func _disconnect_ui_signals() -> void:
	if close_button.pressed.is_connected(_on_close_button_pressed):
		close_button.pressed.disconnect(_on_close_button_pressed)
	if recipe_search_box.text_changed.is_connected(_on_recipe_search_changed):
		recipe_search_box.text_changed.disconnect(_on_recipe_search_changed)
	if category_filter.item_selected.is_connected(_on_category_filter_changed):
		category_filter.item_selected.disconnect(_on_category_filter_changed)
	if craft_button.pressed.is_connected(_on_craft_button_pressed):
		craft_button.pressed.disconnect(_on_craft_button_pressed)
	if cancel_button.pressed.is_connected(_on_cancel_button_pressed):
		cancel_button.pressed.disconnect(_on_cancel_button_pressed)
	if status_timer.timeout.is_connected(_on_status_timer_timeout):
		status_timer.timeout.disconnect(_on_status_timer_timeout)

func _setup_category_filter() -> void:
	category_filter.clear()
	for index in range(CATEGORY_OPTIONS.size()):
		var option := CATEGORY_OPTIONS[index]
		category_filter.add_item(option["label"], index)
	category_filter.select(0)

func _refresh_recipe_list() -> void:
	_recipe_button_map.clear()
	_clear_container(recipe_list)
	if _crafting_system == null:
		return
	var recipes := _crafting_system.get_available_recipes(_current_station_type)
	var lower_filter := _search_filter.to_lower()
	for recipe in recipes:
		if recipe == null:
			continue
		if not lower_filter.is_empty() and not recipe.display_name.to_lower().contains(lower_filter):
			continue
		if _category_filter != "all" and recipe.category.to_lower() != _category_filter:
			continue
		var button := Button.new()
		button.text = "%s (T%d)" % [recipe.display_name, recipe.tier]
		button.custom_minimum_size = Vector2(0, RECIPE_BUTTON_HEIGHT)
		button.size_flags_horizontal = Control.SIZE_EXPAND_FILL
		button.alignment = HORIZONTAL_ALIGNMENT_LEFT
		var icon := _get_icon_for_recipe(recipe)
		if icon != null:
			button.icon = icon
		button.tooltip_text = recipe.description
		button.pressed.connect(_on_recipe_button_pressed.bind(recipe.recipe_id))
		recipe_list.add_child(button)
		_recipe_button_map[recipe.recipe_id] = button
	_highlight_selected_recipe()
	_update_recipe_details()

func _update_recipe_details() -> void:
	if _selected_recipe_id == "":
		recipe_name_label.text = "Select a recipe"
		recipe_desc_label.text = ""
		_clear_container(inputs_list)
		_clear_container(outputs_list)
		craft_button.disabled = true
		return
	var recipe := _crafting_system != null ? _crafting_system.get_recipe_definition(_selected_recipe_id) : null
	if recipe == null:
		_selected_recipe_id = ""
		_update_recipe_details()
		return
	recipe_name_label.text = recipe.display_name
	recipe_desc_label.text = recipe.description
	_clear_container(inputs_list)
	_clear_container(outputs_list)
	var missing := recipe.get_missing_materials(_inventory)
	var missing_lookup := {}
	for entry in missing:
		missing_lookup[entry.get("item_id", "")] = entry
	for input_dict in recipe.inputs:
		_add_requirement_label(inputs_list, input_dict, missing_lookup)
	for output_dict in recipe.outputs:
		_add_output_label(outputs_list, output_dict)
	craft_button.disabled = not recipe.can_craft_with_inventory(_inventory)
	quantity_spinbox.value = 1

func _add_requirement_label(container: VBoxContainer, entry: Dictionary, missing_lookup: Dictionary) -> void:
	var item_id := String(entry.get("item_id", ""))
	var quantity := int(entry.get("quantity", 0))
	var label := Label.new()
	label.horizontal_alignment = HORIZONTAL_ALIGNMENT_LEFT
	var resource := _inventory.get_resource_definition(item_id) if _inventory != null else null
	var display_name := resource.display_name if resource != null else (item_id.is_empty() ? "Unknown" : item_id.capitalize())
	label.text = "%s x%d" % [display_name, quantity]
	var entry_missing := missing_lookup.get(item_id, null)
	if entry_missing != null:
		label.self_modulate = _get_palette_color("error", Color(1.0, 0.4, 0.4))
	else:
		label.self_modulate = _get_palette_color("success", Color(0.6, 1.0, 0.6))
	container.add_child(label)

func _add_output_label(container: VBoxContainer, entry: Dictionary) -> void:
	var item_id := String(entry.get("item_id", ""))
	var quantity := int(entry.get("quantity", 0))
	var label := Label.new()
	label.horizontal_alignment = HORIZONTAL_ALIGNMENT_LEFT
	var resource := _inventory.get_resource_definition(item_id) if _inventory != null else null
	var display_name := resource.display_name if resource != null else (item_id.is_empty() ? "Unknown" : item_id.capitalize())
	label.text = "%s x%d" % [display_name, quantity]
	label.self_modulate = _get_palette_color("text", Color(1, 1, 1))
	container.add_child(label)

func _refresh_queue_display() -> void:
	_clear_container(queue_list)
	var queue: Array = []
	var max_queue_size := DEFAULT_MAX_QUEUE_SIZE
	if _crafting_system != null:
		queue = _crafting_system.get_queue_for_station(_current_station_type)
		var autoload_queue_limit := _crafting_system.get("MAX_QUEUE_SIZE")
		if typeof(autoload_queue_limit) in [TYPE_INT, TYPE_FLOAT]:
			max_queue_size = max(int(autoload_queue_limit), 1)
	queue_label.text = "Crafting Queue (%d/%d)" % [queue.size(), max_queue_size]
	for index in range(queue.size()):
		var entry := queue[index]
		if entry == null:
			continue
		var recipe := _crafting_system.get_recipe_definition(entry.recipe_id) if _crafting_system != null else null
		var panel_entry := Panel.new()
		panel_entry.custom_minimum_size = Vector2(0, QUEUE_ENTRY_HEIGHT)
		var container := HBoxContainer.new()
		container.size_flags_horizontal = Control.SIZE_EXPAND_FILL
		panel_entry.add_child(container)
		var label := Label.new()
		label.size_flags_horizontal = Control.SIZE_EXPAND_FILL
		label.text = "%s x%d" % [recipe.display_name if recipe != null else entry.recipe_id, entry.quantity]
		container.add_child(label)
		var cancel := Button.new()
		cancel.text = "Cancel"
		cancel.custom_minimum_size = Vector2(80, 32)
		cancel.pressed.connect(_on_queue_entry_cancel_pressed.bind(index))
		container.add_child(cancel)
		queue_list.add_child(panel_entry)

func _update_active_craft_panel() -> void:
	if _crafting_system == null:
		active_craft_panel.visible = false
		return
	var status := _crafting_system.get_crafting_status()
	var info := status.get("active_crafts", {}).get(_current_station_type, null)
	if info == null:
		active_craft_panel.visible = false
		_active_recipe_id = ""
		return
	active_craft_panel.visible = true
	_active_recipe_id = String(info.get("recipe_id", ""))
	var recipe := _crafting_system.get_recipe_definition(_active_recipe_id)
	active_craft_label.text = recipe != null ? "Crafting: %s" % recipe.display_name : "Crafting"
	var progress := float(info.get("progress", 0.0))
	craft_progress_bar.value = clamp(progress, 0.0, 1.0)
	var duration := float(info.get("duration", 0.0))
	var elapsed := float(info.get("elapsed", progress * duration))
	var remaining := max(duration - elapsed, 0.0)
	time_remaining_label.text = "Time: %.1fs" % remaining

func _on_queue_updated(station_type: String) -> void:
	if station_type != _current_station_type:
		return
	_refresh_queue_display()
	_update_active_craft_panel()

func _on_recipe_started(recipe_id: String, station_type: String) -> void:
	if station_type != _current_station_type:
		return
	_active_recipe_id = recipe_id
	_update_active_craft_panel()

func _on_recipe_progress(recipe_id: String, _progress: float) -> void:
	if recipe_id != _active_recipe_id:
		return
	_update_active_craft_panel()

func _on_recipe_completed(recipe_id: String, _outputs: Array) -> void:
	var recipe := _crafting_system != null ? _crafting_system.get_recipe_definition(recipe_id) : null
	var name := recipe != null ? recipe.display_name : recipe_id.capitalize()
	_show_status_message("Crafted: %s" % name)
	_update_active_craft_panel()
	_refresh_queue_display()
	_update_recipe_details()

func _on_recipe_cancelled(_recipe_id: String, _reason: String) -> void:
	_show_status_message("Crafting cancelled")
	_update_active_craft_panel()
	_refresh_queue_display()

func _on_recipe_failed(recipe_id: String, error: String, missing: Array) -> void:
	if recipe_id == _selected_recipe_id:
		_update_recipe_details()
	_show_status_message("Cannot craft %s: %s" % [recipe_id.capitalize(), error])

func _on_inventory_changed() -> void:
	_update_recipe_details()

func _on_settings_changed(setting_name: String, _value: Variant) -> void:
	if setting_name.begins_with("ui/"):
		_apply_theme()
		_apply_layout_scale()

func _on_ui_theme_changed() -> void:
	_apply_theme()
	_apply_layout_scale()

func _on_ui_resolution_changed(_new_size: Vector2) -> void:
	_apply_layout_scale()

func _on_close_button_pressed() -> void:
	close_ui()

func _on_recipe_button_pressed(recipe_id: String) -> void:
	_selected_recipe_id = recipe_id
	_highlight_selected_recipe()
	_update_recipe_details()

func _highlight_selected_recipe() -> void:
	for recipe_id in _recipe_button_map.keys():
		var button: Button = _recipe_button_map[recipe_id]
		if button == null:
			continue
		button.button_pressed = (recipe_id == _selected_recipe_id)

func _on_recipe_search_changed(new_text: String) -> void:
	_search_filter = new_text.strip_edges()
	_refresh_recipe_list()

func _on_category_filter_changed(index: int) -> void:
	if index >= 0 and index < CATEGORY_OPTIONS.size():
		_category_filter = String(CATEGORY_OPTIONS[index]["value"])
	else:
		_category_filter = "all"
	_refresh_recipe_list()

func _on_craft_button_pressed() -> void:
	if _crafting_system == null or _selected_recipe_id == "" or _current_station_type == "":
		return
	var quantity := int(quantity_spinbox.value)
	quantity = clamp(quantity, 1, int(quantity_spinbox.max_value))
	if _crafting_system.queue_recipe(_selected_recipe_id, quantity, _current_station_type):
		var recipe := _crafting_system.get_recipe_definition(_selected_recipe_id)
		var name := recipe != null ? recipe.display_name : _selected_recipe_id.capitalize()
		_show_status_message("Queued: %s x%d" % [name, quantity])
		quantity_spinbox.value = 1
		_refresh_queue_display()
		_update_recipe_details()

func _on_cancel_button_pressed() -> void:
	if _crafting_system == null:
		return
	_crafting_system.cancel_recipe(_current_station_type, 0)

func _on_queue_entry_cancel_pressed(index: int) -> void:
	if _crafting_system == null:
		return
	_crafting_system.cancel_recipe(_current_station_type, index)

func _show_ui() -> void:
	visible = true
	if _game_manager != null and not _game_manager.is_paused:
		_game_manager.pause_game()
		_paused_game = true
	else:
		_paused_game = false
	Input.set_mouse_mode(Input.MOUSE_MODE_VISIBLE)

func _hide_ui() -> void:
	visible = false
	if _paused_game and _game_manager != null:
		_game_manager.resume_game()
		_paused_game = false
	Input.set_mouse_mode(Input.MOUSE_MODE_CAPTURED)

func _apply_theme() -> void:
	if _ui_manager != null:
		_ui_manager.apply_theme_to_control(self)

func _apply_layout_scale() -> void:
	if _ui_manager == null or panel == null:
		return
	var scale := _ui_manager.get_ui_scale()
	var safe_rect := _ui_manager.get_safe_area_rect()
	panel.scale = Vector2.ONE * scale
	var panel_size := panel.get_combined_minimum_size() * panel.scale
	panel.position = safe_rect.position + (safe_rect.size - panel_size) * 0.5

func _register_with_ui_manager() -> void:
	if _ui_manager != null:
		_ui_manager.register_ui(self, "CraftingUI")

func _get_icon_for_recipe(recipe: Recipe) -> Texture2D:
	if recipe == null:
		return null
	var path := recipe.icon_path
	if path.is_empty() and not recipe.outputs.is_empty():
		var first_output := recipe.outputs[0]
		var item_id := String(first_output.get("item_id", ""))
		var resource := _inventory.get_resource_definition(item_id) if _inventory != null else null
		path = resource.icon_path if resource != null else path
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

func _clear_container(container: Node) -> void:
	for child in container.get_children():
		child.queue_free()

func _show_status_message(text: String) -> void:
	status_label.text = text
	status_label.visible = true
	status_timer.start()

func _on_status_timer_timeout() -> void:
	status_label.visible = false

func _get_palette_color(name: String, fallback: Color) -> Color:
	return _ui_manager.get_color(name) if _ui_manager != null else fallback

func _log_debug(message: String) -> void:
	if typeof(ErrorLogger) != TYPE_NIL and ErrorLogger != null and ErrorLogger.has_method("log_debug"):
		ErrorLogger.log_debug(message, "CraftingUI")
	else:
		print("[CraftingUI] %s" % message)
