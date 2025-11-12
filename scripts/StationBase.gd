extends Node3D
class_name StationBase

# DEPRECATED: Replace with Interactable component. See StationWorkbench.gd and StationFurnace.gd for examples.

@onready var interaction_area: Area3D = $InteractionArea

var _crafting_system: CraftingSystem = null
var _ui_manager: UIManager = null
var _game_manager: GameManager = null
var _crafting_ui: CraftingUI = null
var _players_in_range: Array = []
var _interaction_connected: bool = false

func _ready() -> void:
	_lookup_autoloads()
	_setup_interaction_area()
	_register_station()
	set_process_input(true)

func _exit_tree() -> void:
	_disconnect_interaction_area()
	_unregister_station()
	_players_in_range.clear()
	_crafting_ui = null

func _input(event: InputEvent) -> void:
	if not event.is_action_pressed("interact"):
		return
	var player := _get_local_player()
	if player == null:
		return
	if not can_interact(player):
		return
	if interact(player):
		get_tree().set_input_as_handled()

func get_station_type() -> String:
	return "none"

func can_interact(player: Node) -> bool:
	if player == null:
		return false
	_prune_invalid_players()
	return _players_in_range.has(player)

func interact(player: Node) -> bool:
	if not can_interact(player):
		return false
	if not _ensure_crafting_ui():
		return false
	if _game_manager != null and _game_manager.is_paused and not _crafting_ui.visible:
		return false
	_crafting_ui.open_for_station(get_station_type())
	return true

func _lookup_autoloads() -> void:
	if typeof(CraftingSystem) != TYPE_NIL and CraftingSystem != null:
		_crafting_system = CraftingSystem
	if typeof(UIManager) != TYPE_NIL and UIManager != null:
		_ui_manager = UIManager
	if typeof(GameManager) != TYPE_NIL and GameManager != null:
		_game_manager = GameManager

func _setup_interaction_area() -> void:
	if interaction_area == null:
		_log_warning("InteractionArea not found; creating default area.")
		interaction_area = Area3D.new()
		interaction_area.name = "InteractionArea"
		add_child(interaction_area)
	if interaction_area != null:
		interaction_area.monitoring = true
		interaction_area.monitorable = false
		interaction_area.collision_layer = 1 << 5 # Interactables layer
		interaction_area.collision_mask = 1 << 1 # Player layer
	if not _interaction_connected and interaction_area != null:
		interaction_area.body_entered.connect(_on_interaction_body_entered, CONNECT_REFERENCE_COUNTED)
		interaction_area.body_exited.connect(_on_interaction_body_exited, CONNECT_REFERENCE_COUNTED)
		_interaction_connected = true

func _disconnect_interaction_area() -> void:
	if interaction_area == null:
		return
	if interaction_area.body_entered.is_connected(_on_interaction_body_entered):
		interaction_area.body_entered.disconnect(_on_interaction_body_entered)
	if interaction_area.body_exited.is_connected(_on_interaction_body_exited):
		interaction_area.body_exited.disconnect(_on_interaction_body_exited)
	_interaction_connected = false

func _register_station() -> void:
	if _crafting_system == null:
		return
	var station_type := get_station_type()
	if station_type.is_empty() or station_type == "none":
		return
	_crafting_system.register_station(self, station_type)

func _unregister_station() -> void:
	if _crafting_system == null:
		return
	var station_type := get_station_type()
	if station_type.is_empty() or station_type == "none":
		return
	_crafting_system.unregister_station(self, station_type)

func _on_interaction_body_entered(body: Node) -> void:
	if not _is_player(body):
		return
	if not _players_in_range.has(body):
		_players_in_range.append(body)

func _on_interaction_body_exited(body: Node) -> void:
	if body == null:
		return
	_players_in_range.erase(body)

func _is_player(body: Node) -> bool:
	return body != null and body is Player

func _get_local_player() -> Node:
	if _game_manager != null:
		var player := _game_manager.get_player()
		if player != null:
			return player
	_prune_invalid_players()
	return _players_in_range.is_empty() ? null : _players_in_range[0]

func _prune_invalid_players() -> void:
	for player in _players_in_range.duplicate():
		if not is_instance_valid(player) or not _is_player(player):
			_players_in_range.erase(player)

func _ensure_crafting_ui() -> bool:
	if _crafting_ui != null and is_instance_valid(_crafting_ui):
		return true
	_crafting_ui = _find_crafting_ui()
	if _crafting_ui == null:
		_log_warning("Crafting UI unavailable; cannot interact with %s." % get_station_type())
		return false
	return true

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

func _log_warning(message: String) -> void:
	if typeof(ErrorLogger) != TYPE_NIL and ErrorLogger != null and ErrorLogger.has_method("log_warning"):
		ErrorLogger.log_warning(message, "StationBase")
	else:
		push_warning(message)
