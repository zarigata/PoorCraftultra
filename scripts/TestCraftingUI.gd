extends CanvasLayer
class_name TestCraftingUI

@onready var add_materials_button: Button = $Control/VBoxContainer/AddMaterialsButton
@onready var open_hand_button: Button = $Control/VBoxContainer/OpenHandButton
@onready var open_workbench_button: Button = $Control/VBoxContainer/OpenWorkbenchButton
@onready var open_furnace_button: Button = $Control/VBoxContainer/OpenFurnaceButton
@onready var clear_inventory_button: Button = $Control/VBoxContainer/ClearInventoryButton
@onready var clear_queues_button: Button = $Control/VBoxContainer/ClearQueuesButton
@onready var status_label: Label = $Control/VBoxContainer/StatusLabel

var _inventory: Inventory = null
var _crafting_system: CraftingSystem = null
var _ui_manager: UIManager = null

func _ready() -> void:
	_lookup_autoloads()
	_connect_buttons()
	_connect_signals()
	status_label.text = ""

func _exit_tree() -> void:
	_disconnect_signals()

func _lookup_autoloads() -> void:
	if typeof(Inventory) != TYPE_NIL and Inventory != null:
		_inventory = Inventory
	if typeof(CraftingSystem) != TYPE_NIL and CraftingSystem != null:
		_crafting_system = CraftingSystem
	if typeof(UIManager) != TYPE_NIL and UIManager != null:
		_ui_manager = UIManager

func _connect_buttons() -> void:
	add_materials_button.pressed.connect(_on_add_materials_pressed)
	open_hand_button.pressed.connect(_on_open_hand_pressed)
	open_workbench_button.pressed.connect(_on_open_workbench_pressed)
	open_furnace_button.pressed.connect(_on_open_furnace_pressed)
	clear_inventory_button.pressed.connect(_on_clear_inventory_pressed)
	clear_queues_button.pressed.connect(_on_clear_queues_pressed)

func _connect_signals() -> void:
	if _crafting_system != null:
		_crafting_system.queue_updated.connect(_on_queue_updated, CONNECT_REFERENCE_COUNTED)
		_crafting_system.recipe_completed.connect(_on_recipe_completed, CONNECT_REFERENCE_COUNTED)
		_crafting_system.recipe_failed.connect(_on_recipe_failed, CONNECT_REFERENCE_COUNTED)

func _disconnect_signals() -> void:
	if _crafting_system != null:
		if _crafting_system.queue_updated.is_connected(_on_queue_updated):
			_crafting_system.queue_updated.disconnect(_on_queue_updated)
		if _crafting_system.recipe_completed.is_connected(_on_recipe_completed):
			_crafting_system.recipe_completed.disconnect(_on_recipe_completed)
		if _crafting_system.recipe_failed.is_connected(_on_recipe_failed):
			_crafting_system.recipe_failed.disconnect(_on_recipe_failed)

func _on_add_materials_pressed() -> void:
	if _inventory == null:
		_show_status("Inventory unavailable", true)
		return
	var grants := {
		"wood": 20,
		"stone": 20,
		"coal": 10,
		"iron_ore": 10,
		"copper_ore": 10
	}
	var added_any := false
	for item_id in grants.keys():
		var quantity := int(grants[item_id])
		if _inventory.add_item(item_id, quantity):
			added_any = true
	if added_any:
		_show_status("Materials added", false)
	else:
		_show_status("Failed to add materials", true)

func _on_open_hand_pressed() -> void:
	_open_crafting_ui("none")

func _on_open_workbench_pressed() -> void:
	_open_crafting_ui("workbench")

func _on_open_furnace_pressed() -> void:
	_open_crafting_ui("furnace")

func _open_crafting_ui(station_type: String) -> void:
	var crafting_ui := _get_crafting_ui()
	if crafting_ui == null:
		_show_status("Crafting UI unavailable", true)
		return
	crafting_ui.open_for_station(station_type)
	_show_status("Opened crafting UI for %s" % station_type, false)

func _on_clear_inventory_pressed() -> void:
	if _inventory == null:
		_show_status("Inventory unavailable", true)
		return
	_inventory.clear_inventory()
	_show_status("Inventory cleared", false)

func _on_clear_queues_pressed() -> void:
	if _crafting_system == null:
		_show_status("CraftingSystem unavailable", true)
		return
	for station in ["none", "workbench", "furnace"]:
		_crafting_system.cancel_all_recipes(station)
	_show_status("Queues cleared", false)

func _on_queue_updated(station_type: String) -> void:
	_show_status("Queue updated: %s" % station_type, false)

func _on_recipe_completed(recipe_id: String, _outputs: Array) -> void:
	_show_status("Completed: %s" % recipe_id, false)

func _on_recipe_failed(recipe_id: String, reason: String, _missing: Array) -> void:
	_show_status("Failed %s: %s" % [recipe_id, reason], true)

func _show_status(message: String, is_error: bool) -> void:
	status_label.text = message
	status_label.self_modulate = Color(1, 0.5, 0.5) if is_error else Color(0.6, 1, 0.6)

func _get_crafting_ui() -> CraftingUI:
	if _ui_manager != null and _ui_manager.has_method("get_ui"):
		var ui := _ui_manager.get_ui("CraftingUI")
		if ui is CraftingUI:
			return ui
	var root := get_tree().get_root()
	if root != null:
		var node := root.find_child("CraftingUI", true, false)
		if node is CraftingUI:
			return node
	return null
