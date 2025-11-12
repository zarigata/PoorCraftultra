extends CanvasLayer
class_name TestUI

const WORKBENCH_SCENE := preload("res://scenes/station_workbench.tscn")
const FURNACE_SCENE := preload("res://scenes/station_furnace.tscn")
const BUTTON_CONNECT_FLAGS := CONNECT_REFERENCE_COUNTED
const SPAWN_BASE_DISTANCE := 6.0
const SPAWN_DISTANCE_STEP := 1.5

@onready var panel: Panel = $Panel
@onready var buttons := {
	"add_materials": %AddMaterialsButton,
	"open_workbench": %OpenWorkbenchButton,
	"open_furnace": %OpenFurnaceButton,
	"open_hand": %OpenHandButton,
	"clear_inventory": %ClearInventoryButton,
	"clear_queues": %ClearQueuesButton,
	"save_game": %SaveGameButton,
	"load_game": %LoadGameButton,
	"spawn_station": %SpawnStationButton,
}
@onready var _crafting_ui: CraftingUI = get_parent().get_node_or_null("CraftingUI") as CraftingUI

var _inventory: Object = null
var _crafting_system: Object = null
var _save_manager: Object = null
var _ui_manager: Object = null
var _error_logger: Object = null

var _rng := RandomNumberGenerator.new()
var _spawn_count: int = 0
var _registered_with_ui_manager: bool = false

func _ready() -> void:
	layer = 10
	visible = true
	_rng.randomize()
	_lookup_autoloads()
	_connect_ui_manager()
	_connect_buttons()
	_apply_theme()
	call_deferred("_update_layout")
	_log_info("Test crafting UI ready")

func _exit_tree() -> void:
	_disconnect_ui_manager()

func _lookup_autoloads() -> void:
	if typeof(Inventory) != TYPE_NIL and Inventory != null:
		_inventory = Inventory
	if typeof(CraftingSystem) != TYPE_NIL and CraftingSystem != null:
		_crafting_system = CraftingSystem
	if typeof(SaveManager) != TYPE_NIL and SaveManager != null:
		_save_manager = SaveManager
	if typeof(UIManager) != TYPE_NIL and UIManager != null:
		_ui_manager = UIManager
	if typeof(ErrorLogger) != TYPE_NIL and ErrorLogger != null:
		_error_logger = ErrorLogger

func _connect_buttons() -> void:
	for action in buttons.keys():
		var button := buttons[action]
		if button == null:
			continue
		match action:
			"add_materials":
				button.pressed.connect(func(): _add_test_materials(), BUTTON_CONNECT_FLAGS)
			"open_workbench":
				button.pressed.connect(func(): _open_station_ui("workbench"), BUTTON_CONNECT_FLAGS)
			"open_furnace":
				button.pressed.connect(func(): _open_station_ui("furnace"), BUTTON_CONNECT_FLAGS)
			"open_hand":
				button.pressed.connect(func(): _open_station_ui("none"), BUTTON_CONNECT_FLAGS)
			"clear_inventory":
				button.pressed.connect(func(): _clear_inventory(), BUTTON_CONNECT_FLAGS)
			"clear_queues":
				button.pressed.connect(func(): _clear_all_queues(), BUTTON_CONNECT_FLAGS)
			"save_game":
				button.pressed.connect(func(): _save_game(), BUTTON_CONNECT_FLAGS)
			"load_game":
				button.pressed.connect(func(): _load_game(), BUTTON_CONNECT_FLAGS)
			"spawn_station":
				button.pressed.connect(func(): _spawn_test_station(), BUTTON_CONNECT_FLAGS)

func _connect_ui_manager() -> void:
	if _ui_manager == null:
		return
	if _ui_manager.has_signal("theme_changed"):
		_ui_manager.theme_changed.connect(_on_ui_theme_changed, BUTTON_CONNECT_FLAGS)
	if _ui_manager.has_signal("resolution_changed"):
		_ui_manager.resolution_changed.connect(_on_ui_resolution_changed, BUTTON_CONNECT_FLAGS)
	if _ui_manager.has_method("register_ui"):
		_ui_manager.register_ui(self, "TestCraftingUI")
		_registered_with_ui_manager = true

func _disconnect_ui_manager() -> void:
	if _ui_manager == null:
		return
	if _ui_manager.has_signal("theme_changed") and _ui_manager.theme_changed.is_connected(_on_ui_theme_changed):
		_ui_manager.theme_changed.disconnect(_on_ui_theme_changed)
	if _ui_manager.has_signal("resolution_changed") and _ui_manager.resolution_changed.is_connected(_on_ui_resolution_changed):
		_ui_manager.resolution_changed.disconnect(_on_ui_resolution_changed)
	if _registered_with_ui_manager and _ui_manager.has_method("unregister_ui"):
		_ui_manager.unregister_ui(self)
	_registered_with_ui_manager = false

func _apply_theme() -> void:
	if panel == null:
		return
	if _ui_manager != null and _ui_manager.has_method("apply_theme_to_control"):
		_ui_manager.apply_theme_to_control(panel)
	var scale := 1.0
	if _ui_manager != null and _ui_manager.has_method("get_ui_scale"):
		scale = max(0.1, float(_ui_manager.get_ui_scale()))
	panel.scale = Vector2.ONE * scale
	_update_layout()

func _update_layout() -> void:
	if panel == null:
		return
	var size := panel.get_combined_minimum_size()
	if panel.scale != Vector2.ONE:
		size *= panel.scale
	if _ui_manager != null and _ui_manager.has_method("get_safe_area_rect"):
		var rect: Rect2 = _ui_manager.get_safe_area_rect()
		panel.position = rect.position + (rect.size - size) * 0.5
	else:
		var viewport := get_viewport_rect()
		panel.position = (viewport.size - size) * 0.5

func _add_test_materials() -> void:
	if _inventory == null or not _inventory.has_method("add_item"):
		_log_warning("Inventory autoload unavailable; cannot add test materials")
		return
	var materials := [
		{"id": "wood", "qty": 10},
		{"id": "stone", "qty": 10},
		{"id": "iron_ore", "qty": 10},
		{"id": "coal", "qty": 10},
		{"id": "planks", "qty": 5},
		{"id": "sticks", "qty": 5},
	]
	var all_success := true
	for entry in materials:
		var success := _inventory.add_item(String(entry["id"]), int(entry["qty"]))
		if not success:
			_log_warning("Failed to add %s x%d" % [entry["id"], entry["qty"]])
			all_success = false
	if all_success:
		_log_info("Added test crafting materials to inventory")

func _open_station_ui(station_type: String) -> void:
	var crafting_ui := _get_crafting_ui()
	if crafting_ui == null:
		_log_warning("CraftingUI instance not available; cannot open %s" % station_type)
		return
	crafting_ui.open_for_station(station_type)

func _clear_inventory() -> void:
	if _inventory == null or not _inventory.has_method("clear_inventory"):
		_log_warning("Inventory autoload unavailable; cannot clear inventory")
		return
	_inventory.clear_inventory()
	_log_info("Inventory cleared via test UI")

func _clear_all_queues() -> void:
	if _crafting_system == null or not _crafting_system.has_method("cancel_all_recipes"):
		_log_warning("CraftingSystem unavailable; cannot clear queues")
		return
	for station in ["workbench", "furnace", "none"]:
		_crafting_system.cancel_all_recipes(station)
	_log_info("Requested clearing of all crafting queues")

func _save_game() -> void:
	if _save_manager == null or not _save_manager.has_method("save_game"):
		_log_warning("SaveManager unavailable; cannot save game")
		return
	_save_manager.save_game()
	_log_info("Save requested from test UI")

func _load_game() -> void:
	if _save_manager == null or not _save_manager.has_method("load_game"):
		_log_warning("SaveManager unavailable; cannot load game")
		return
	_save_manager.load_game()
	_log_info("Load requested from test UI")

func _spawn_test_station() -> void:
	var root := get_tree().current_scene
	if root == null or not (root is Node3D):
		_log_warning("Current scene unavailable; cannot spawn test station")
		return
	var use_workbench := (_spawn_count % 2) == 0
	var packed: PackedScene = use_workbench ? WORKBENCH_SCENE : FURNACE_SCENE
	if packed == null:
		_log_warning("Packed scene missing for spawn request")
		return
	var instance := packed.instantiate()
	if instance == null or not (instance is Node3D):
		_log_warning("Failed to instantiate test station scene")
		return
	var index := _spawn_count
	_spawn_count += 1
	var distance := SPAWN_BASE_DISTANCE + SPAWN_DISTANCE_STEP * float(index)
	var angle := _rng.randf_range(0.0, TAU)
	var position := Vector3(cos(angle) * distance, 0.0, sin(angle) * distance)
	instance.position = position
	instance.name = "%s_%d" % [use_workbench ? "TestWorkbench" : "TestFurnace", _spawn_count]
	root.add_child(instance)
	_log_info("Spawned %s at %s" % [instance.name, position])

func _get_crafting_ui() -> CraftingUI:
	if _crafting_ui != null and is_instance_valid(_crafting_ui):
		return _crafting_ui
	var parent := get_parent()
	if parent != null:
		_crafting_ui = parent.get_node_or_null("CraftingUI") as CraftingUI
	if (_crafting_ui == null or not is_instance_valid(_crafting_ui)) and _ui_manager != null and _ui_manager.has_method("get_ui"):
		var found := _ui_manager.get_ui("CraftingUI")
		if found is CraftingUI:
			_crafting_ui = found
	return _crafting_ui if is_instance_valid(_crafting_ui) else null

func _on_ui_theme_changed() -> void:
	_apply_theme()

func _on_ui_resolution_changed(_new_size: Vector2) -> void:
	_update_layout()

func _log_info(message: String) -> void:
	if _error_logger != null and _error_logger.has_method("log_info"):
		_error_logger.log_info(message, "TestUI")
	else:
		print("[TestUI] %s" % message)

func _log_warning(message: String) -> void:
	if _error_logger != null and _error_logger.has_method("log_warning"):
		_error_logger.log_warning(message, "TestUI")
	else:
		push_warning("[TestUI] %s" % message)
