extends Node3D
class_name StationWorkbench

const STATION_TYPE := "workbench"

var _crafting_system: CraftingSystem = null
var _ui_manager: UIManager = null
var _game_manager: GameManager = null
var _crafting_ui: CraftingUI = null
var _interactable: Interactable = null

func _ready() -> void:
	_lookup_autoloads()
	_setup_interactable()
	_register_station()

func _exit_tree() -> void:
	_unregister_station()
	_crafting_ui = null

func get_station_type() -> String:
	return STATION_TYPE

func interact(player: Node) -> bool:
	if _crafting_ui == null or not is_instance_valid(_crafting_ui):
		_crafting_ui = _find_crafting_ui()
	if _crafting_ui == null:
		_log_warning("Crafting UI unavailable; cannot open workbench.")
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

	_interactable.prompt_text = "[E] Craft"
	_interactable.interaction_range = 5.0
	_interactable.interaction_type = "station"
	_interactable.priority = 10
	_interactable.screen_reader_text = "Workbench - Open crafting menu"

	if should_add_child:
		add_child(_interactable)

	if not _interactable.interacted.is_connected(_on_interacted):
		_interactable.interacted.connect(_on_interacted)

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
	var root := get_tree().get_root() if get_tree() != null else null
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

func _log_warning(message: String) -> void:
	if typeof(ErrorLogger) != TYPE_NIL and ErrorLogger != null and ErrorLogger.has_method("log_warning"):
		ErrorLogger.log_warning(message, "StationWorkbench")
	else:
		push_warning(message)
