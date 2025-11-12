extends Node3D
class_name StationFurnace

const STATION_TYPE := "furnace"
const INTERACTION_PROMPT := "[E] Smelt"

@onready var fire_glow: Node3D = $FireGlow

var _crafting_system: CraftingSystem = null
var _ui_manager: UIManager = null
var _game_manager: GameManager = null
var _crafting_ui: CraftingUI = null
var _interactable: Interactable = null

func _ready() -> void:
	_lookup_autoloads()
	_setup_interactable()
	_connect_crafting_signals()
	_register_station()
	_sync_fire_glow()

func _exit_tree() -> void:
	_disconnect_crafting_signals()
	_unregister_station()
	_crafting_ui = null

func get_station_type() -> String:
	return STATION_TYPE

func interact(player: Node) -> bool:
	if _crafting_ui == null or not is_instance_valid(_crafting_ui):
		_crafting_ui = _find_crafting_ui()
	if _crafting_ui == null:
		_log_warning("Crafting UI unavailable; cannot open furnace.")
		return false
	if _game_manager != null and _game_manager.is_paused and not _crafting_ui.visible:
		return false
	_crafting_ui.open_for_station(STATION_TYPE)
	return true

func _lookup_autoloads() -> void:
	if typeof(CraftingSystem) != TYPE_NIL and CraftingSystem != null:
		_crafting_system = CraftingSystem
	if typeof(UIManager) != TYPE_NIL and UIManager != null:
		_ui_manager = UIManager
	if typeof(GameManager) != TYPE_NIL and GameManager != null:
		_game_manager = GameManager

func _setup_interactable() -> void:
	_interactable = _find_child_of_type(Interactable)
	var should_add_child := false
	if _interactable == null:
		_log_warning("Interactable component not found; creating default.")
		_interactable = Interactable.new()
		_interactable.name = "Interactable"
		should_add_child = true

	_interactable.prompt_text = INTERACTION_PROMPT
	_interactable.interaction_range = 5.0
	_interactable.interaction_type = "station"
	_interactable.priority = 10
	_interactable.screen_reader_text = "Furnace - Open smelting menu"

	if should_add_child:
		add_child(_interactable)

	if not _interactable.interacted.is_connected(_on_interacted):
		_interactable.interacted.connect(_on_interacted)

func _connect_crafting_signals() -> void:
	if _crafting_system == null:
		return
	if not _crafting_system.recipe_started.is_connected(_on_recipe_started):
		_crafting_system.recipe_started.connect(_on_recipe_started, CONNECT_REFERENCE_COUNTED)
	if not _crafting_system.recipe_completed.is_connected(_on_recipe_completed):
		_crafting_system.recipe_completed.connect(_on_recipe_completed, CONNECT_REFERENCE_COUNTED)
	if not _crafting_system.recipe_cancelled.is_connected(_on_recipe_cancelled):
		_crafting_system.recipe_cancelled.connect(_on_recipe_cancelled, CONNECT_REFERENCE_COUNTED)
	if not _crafting_system.recipe_failed.is_connected(_on_recipe_failed):
		_crafting_system.recipe_failed.connect(_on_recipe_failed, CONNECT_REFERENCE_COUNTED)
	if not _crafting_system.queue_updated.is_connected(_on_queue_updated):
		_crafting_system.queue_updated.connect(_on_queue_updated, CONNECT_REFERENCE_COUNTED)

func _disconnect_crafting_signals() -> void:
	if _crafting_system == null:
		return
	if _crafting_system.recipe_started.is_connected(_on_recipe_started):
		_crafting_system.recipe_started.disconnect(_on_recipe_started)
	if _crafting_system.recipe_completed.is_connected(_on_recipe_completed):
		_crafting_system.recipe_completed.disconnect(_on_recipe_completed)
	if _crafting_system.recipe_cancelled.is_connected(_on_recipe_cancelled):
		_crafting_system.recipe_cancelled.disconnect(_on_recipe_cancelled)
	if _crafting_system.recipe_failed.is_connected(_on_recipe_failed):
		_crafting_system.recipe_failed.disconnect(_on_recipe_failed)
	if _crafting_system.queue_updated.is_connected(_on_queue_updated):
		_crafting_system.queue_updated.disconnect(_on_queue_updated)

func _register_station() -> void:
	if _crafting_system == null:
		return
	_crafting_system.register_station(self, STATION_TYPE)

func _unregister_station() -> void:
	if _crafting_system == null:
		return
	_crafting_system.unregister_station(self, STATION_TYPE)

func _on_interacted(player: Node) -> void:
	interact(player)

func _find_crafting_ui() -> CraftingUI:
	if _ui_manager != null and _ui_manager.has_method("get_ui"):
		var ui := _ui_manager.get_ui("CraftingUI")
		if ui is CraftingUI:
			return ui
	var tree := get_tree()
	if tree != null:
		var root := tree.get_root()
		if root != null:
			var node := root.find_child("CraftingUI", true, false)
			if node is CraftingUI:
				return node
	return null

func _find_child_of_type(type_ref) -> Node:
	for child in get_children():
		if is_instance_of(child, type_ref):
			return child
	return null

func _on_recipe_started(_recipe_id: String, station_type: String) -> void:
	if station_type != STATION_TYPE:
		return
	_set_fire_glow_visible(true)

func _on_recipe_completed(_recipe_id: String, _outputs: Array) -> void:
	_sync_fire_glow()

func _on_recipe_cancelled(_recipe_id: String, _reason: String) -> void:
	_sync_fire_glow()

func _on_recipe_failed(_recipe_id: String, _error: String, _missing: Array) -> void:
	_sync_fire_glow()

func _on_queue_updated(station_type: String) -> void:
	if station_type != STATION_TYPE:
		return
	_sync_fire_glow()

func _sync_fire_glow() -> void:
	_set_fire_glow_visible(_is_craft_active())

func _is_craft_active() -> bool:
	if _crafting_system == null:
		return false
	if not _crafting_system.has_method("get_crafting_status"):
		return false
	var status := _crafting_system.get_crafting_status()
	if typeof(status) != TYPE_DICTIONARY:
		return false
	var active: Dictionary = status.get("active_crafts", {})
	if typeof(active) != TYPE_DICTIONARY:
		return false
	return active.get(STATION_TYPE, null) != null

func _set_fire_glow_visible(visible: bool) -> void:
	if fire_glow != null and is_instance_valid(fire_glow):
		fire_glow.visible = visible

func _log_warning(message: String) -> void:
	if typeof(ErrorLogger) != TYPE_NIL and ErrorLogger != null and ErrorLogger.has_method("log_warning"):
		ErrorLogger.log_warning(message, "StationFurnace")
	else:
		push_warning(message)
