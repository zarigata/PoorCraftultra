extends Node
class_name CraftingSystem

signal recipe_started(recipe_id: String, station_type: String)
signal recipe_progress(recipe_id: String, progress: float)
signal recipe_completed(recipe_id: String, outputs: Array)
signal recipe_cancelled(recipe_id: String, reason: String)
signal recipe_failed(recipe_id: String, error: String, missing_materials: Array)
signal queue_updated(station_type: String)
signal station_registered(station: Node, station_type: String)
signal station_unregistered(station: Node, station_type: String)

enum CraftingState { IDLE, CRAFTING, COOLDOWN }
enum StationType { NONE, WORKBENCH, FURNACE, MACHINING_BENCH, FABRICATOR }

const RECIPES_DB_PATH := "res://resources/data/recipes.json"
const RECIPES_DB_VERSION := 1
const MAX_QUEUE_SIZE := 10
const COOLDOWN_DURATION := 0.1
const CRAFTING_SOUND_PATH := "res://assets/audio/sfx/crafting/"
const STATION_INTERACTION_RANGE := 5.0
const DROPPED_ITEM_SCENE_PATH := "res://scenes/dropped_item.tscn"
const INCLUDE_BASIC_RECIPES_AT_STATIONS := true

var recipes_db: Dictionary = {}
var _recipe_defs: Dictionary = {}
var _invalid_recipes: Dictionary = {}
var crafting_queues: Dictionary = {}
var active_crafts: Dictionary = {}
var registered_stations: Dictionary = {}

var _inventory: Inventory = null
var _audio_manager: AudioManager = null
var _game_manager: GameManager = null
var _player: Node = null

class QueueEntry:
	var recipe_id: String = ""
	var quantity: int = 1
	var queued_at: int = 0

	func to_dictionary() -> Dictionary:
		return {
			"recipe_id": recipe_id,
			"quantity": quantity,
			"queued_at": queued_at,
		}

	static func from_dictionary(data: Dictionary) -> QueueEntry:
		var entry := QueueEntry.new()
		entry.recipe_id = data.get("recipe_id", "")
		entry.quantity = int(data.get("quantity", 1))
		entry.queued_at = int(data.get("queued_at", 0))
		return entry

class ActiveCraft:
	var recipe_id: String = ""
	var recipe: Recipe = null
	var progress: float = 0.0
	var elapsed: float = 0.0
	var duration: float = 0.0
	var inputs_consumed: bool = false
	var station_type: String = "none"
	var sound_player: AudioStreamPlayer3D = null
	var queue_entry: QueueEntry = null
	var position: Vector3 = Vector3.ZERO

func _ready() -> void:
	_setup_structures()
	_lookup_autoloads()
	_load_recipes_database()
	_connect_game_manager()
	_log_info("CraftingSystem initialized.")

func _process(delta: float) -> void:
	for station_type in active_crafts.keys():
		var craft: ActiveCraft = active_crafts[station_type]
		if craft == null:
			continue
		craft.elapsed += delta
		craft.progress = clamp(craft.elapsed / max(craft.duration, 0.001), 0.0, 1.0)
		emit_signal("recipe_progress", craft.recipe_id, craft.progress)
		if craft.progress >= 1.0:
			_complete_craft(station_type)

func get_recipe_definition(recipe_id: String) -> Recipe:
	if recipe_id == "":
		return null
	if _invalid_recipes.has(recipe_id):
		return null
	if _recipe_defs.has(recipe_id):
		return _recipe_defs[recipe_id]
	var entry := recipes_db.get(recipe_id, null)
	if entry == null:
		_log_warning("Recipe '%s' not found in database." % recipe_id)
		return null
	var recipe := Recipe.new()
	for key in entry.keys():
		if recipe.has_property(key):
			recipe.set(key, entry[key])
	if recipe.recipe_id.is_empty():
		recipe.recipe_id = recipe_id
	if not _validate_recipe_items(recipe):
		_invalid_recipes[recipe_id] = true
		_log_warning("Invalid items in recipe '%s'; skipping." % recipe_id)
		return null
	_recipe_defs[recipe_id] = recipe
	return recipe

func get_available_recipes(station_type: String = "none") -> Array:
	var results: Array = []
	var allow_basic := INCLUDE_BASIC_RECIPES_AT_STATIONS and station_type != "none"
	for recipe_id in recipes_db.keys():
		var recipe: Recipe = get_recipe_definition(recipe_id)
		if recipe == null:
			continue
		if recipe.required_station != station_type:
			if not (allow_basic and recipe.required_station == "none"):
				continue
		if not recipe.is_unlocked_by_default:
			continue
		results.append(recipe)
	results.sort_custom(Callable(self, "_compare_recipes"))
	return results

func _compare_recipes(a: Recipe, b: Recipe) -> bool:
	if a == null and b == null:
		return false
	if a == null:
		return true
	if b == null:
		return false
	var category_a := String(a.category) if a.category != null else ""
	var category_b := String(b.category) if b.category != null else ""
	if category_a == category_b:
		var tier_a := int(a.tier) if a.tier != null else 0
		var tier_b := int(b.tier) if b.tier != null else 0
		if tier_a == tier_b:
			var name_a := String(a.display_name) if a.display_name != null else ""
			var name_b := String(b.display_name) if b.display_name != null else ""
			return name_a < name_b
		return tier_a < tier_b
	return category_a < category_b

func get_all_recipes() -> Array:
	var res: Array = []
	for recipe_id in recipes_db.keys():
		var recipe := get_recipe_definition(recipe_id)
		if recipe != null:
			res.append(recipe)
	return res

func queue_recipe(recipe_id: String, quantity: int = 1, station_type: String = "none") -> bool:
	var recipe := get_recipe_definition(recipe_id)
	if recipe == null:
		_emit_recipe_failed(recipe_id, "unknown_recipe", [])
		return false
	var station_mismatch := recipe.required_station != station_type
	var basic_at_station := station_type != "none" and recipe.required_station == "none"
	if station_mismatch and not basic_at_station:
		_emit_recipe_failed(recipe_id, "invalid_station", [])
		return false
	var queue: Array = crafting_queues.get(station_type, [])
	if queue.size() >= MAX_QUEUE_SIZE:
		_emit_recipe_failed(recipe_id, "queue_full", [])
		return false
	var validation := _validate_recipe_materials(recipe)
	if not validation.get("valid", false):
		_emit_recipe_failed(recipe_id, "missing_materials", validation.get("missing", []))
		return false
	var entry := QueueEntry.new()
	entry.recipe_id = recipe_id
	entry.quantity = max(quantity, 1)
	entry.queued_at = Time.get_ticks_msec()
	queue.append(entry)
	crafting_queues[station_type] = queue
	_log_debug("Queued %s at %s (basic allowed: %s)" % [recipe_id, station_type, recipe.required_station == "none"])
	emit_signal("queue_updated", station_type)
	if active_crafts.get(station_type, null) == null:
		_start_next_craft(station_type)
	return true

func cancel_recipe(station_type: String, queue_index: int = 0) -> bool:
	var queue: Array = crafting_queues.get(station_type, [])
	if queue_index < 0 or queue_index >= queue.size():
		return false
	if queue_index == 0 and active_crafts.get(station_type, null) != null:
		_cancel_active_craft(station_type, true)
		return true
	var entry: QueueEntry = queue[queue_index]
	queue.remove_at(queue_index)
	crafting_queues[station_type] = queue
	emit_signal("queue_updated", station_type)
	emit_signal("recipe_cancelled", entry.recipe_id, "queue_cancelled")
	if active_crafts.get(station_type, null) == null:
		_start_next_craft(station_type)
	return true

func cancel_all_recipes(station_type: String) -> void:
	_cancel_active_craft(station_type, true)
	crafting_queues[station_type] = []
	emit_signal("queue_updated", station_type)

func get_queue_for_station(station_type: String) -> Array:
	return crafting_queues.get(station_type, []).duplicate()

func get_active_craft_progress(station_type: String) -> float:
	var craft: ActiveCraft = active_crafts.get(station_type, null)
	if craft == null:
		return 0.0
	return craft.progress

func register_station(station: Node, station_type: String) -> void:
	if station == null:
		return
	var list: Array = registered_stations.get(station_type, [])
	if not list.has(station):
		list.append(station)
	registered_stations[station_type] = list
	emit_signal("station_registered", station, station_type)
	_log_info("Station registered: %s" % station_type)

func unregister_station(station: Node, station_type: String) -> void:
	var list: Array = registered_stations.get(station_type, [])
	if list.has(station):
		list.erase(station)
	registered_stations[station_type] = list
	emit_signal("station_unregistered", station, station_type)
	if list.is_empty():
		cancel_all_recipes(station_type)
	_log_info("Station unregistered: %s" % station_type)

func get_nearest_station(station_type: String, from_position: Vector3) -> Node:
	var nearest: Node = null
	var nearest_distance := INF
	var stations: Array = registered_stations.get(station_type, [])
	for station in stations:
		if not station or not station is Node3D:
			continue
		var distance := station.global_position.distance_to(from_position)
		if distance < nearest_distance:
			nearest_distance = distance
			nearest = station
	return nearest

func is_station_in_range(station: Node, player_position: Vector3) -> bool:
	if station == null or not station is Node3D:
		return false
	return station.global_position.distance_to(player_position) <= STATION_INTERACTION_RANGE

func serialize() -> Dictionary:
	var data := {
		"queues": {},
		"active_crafts": {}
	}
	for station_type in crafting_queues.keys():
		var queue: Array = crafting_queues[station_type]
		if queue.is_empty():
			continue
		var serialized_queue: Array = []
		for entry in queue:
			serialized_queue.append(entry.to_dictionary())
		data["queues"][station_type] = serialized_queue
	for station_type in active_crafts.keys():
		var craft: ActiveCraft = active_crafts[station_type]
		if craft == null:
			continue
		data["active_crafts"][station_type] = {
			"recipe_id": craft.recipe_id,
			"elapsed": craft.elapsed,
			"duration": craft.duration,
			"inputs_consumed": craft.inputs_consumed,
		}
	return data

func deserialize(data: Dictionary) -> bool:
	if data.is_empty():
		return true
	crafting_queues.clear()
	active_crafts.clear()
	_setup_structures()
	var queue_data: Dictionary = data.get("queues", {})
	for station_type in queue_data.keys():
		var raw_queue: Array = queue_data[station_type]
		var queue: Array = []
		for raw_entry in raw_queue:
			var entry := QueueEntry.from_dictionary(raw_entry)
			if get_recipe_definition(entry.recipe_id) == null:
				_log_warning("Skipping unknown recipe '%s' during deserialize." % entry.recipe_id)
				continue
			queue.append(entry)
		crafting_queues[station_type] = queue
	var active_data: Dictionary = data.get("active_crafts", {})
	for station_type in active_data.keys():
		var raw_craft: Dictionary = active_data[station_type]
		var recipe := get_recipe_definition(raw_craft.get("recipe_id", ""))
		if recipe == null:
			continue
		var craft := ActiveCraft.new()
		craft.recipe_id = recipe.recipe_id
		craft.recipe = recipe
		craft.elapsed = float(raw_craft.get("elapsed", 0.0))
		craft.duration = max(float(raw_craft.get("duration", recipe.crafting_time)), 0.1)
		craft.inputs_consumed = bool(raw_craft.get("inputs_consumed", true))
		craft.progress = clamp(craft.elapsed / craft.duration, 0.0, 1.0)
		craft.station_type = station_type
		active_crafts[station_type] = craft
	for station_type in crafting_queues.keys():
		emit_signal("queue_updated", station_type)
	return true

func queue_craft_for_companion(recipe_id: String, quantity: int, station_type: String) -> Dictionary:
	var success := queue_recipe(recipe_id, quantity, station_type)
	if success:
		return {"ok": true}
	return {"ok": false, "reason": "queue_failed"}

func get_crafting_status() -> Dictionary:
	return {
		"queues": crafting_queues.duplicate(true),
		"active_crafts": _serialize_active_crafts(),
	}

func _serialize_active_crafts() -> Dictionary:
	var data := {}
	for station_type in active_crafts.keys():
		var craft: ActiveCraft = active_crafts[station_type]
		if craft == null:
			continue
		data[station_type] = {
			"recipe_id": craft.recipe_id,
			"progress": craft.progress,
			"elapsed": craft.elapsed,
			"duration": craft.duration,
		}
	return data

func _setup_structures() -> void:
	crafting_queues = {
		"none": [],
		"workbench": [],
		"furnace": [],
		"machining_bench": [],
		"fabricator": [],
	}
	active_crafts = {
		"none": null,
		"workbench": null,
		"furnace": null,
		"machining_bench": null,
		"fabricator": null,
	}
	registered_stations = {
		"none": [],
		"workbench": [],
		"furnace": [],
		"machining_bench": [],
		"fabricator": [],
	}

func _lookup_autoloads() -> void:
	if typeof(Inventory) != TYPE_NIL and Inventory != null:
		_inventory = Inventory
	if typeof(AudioManager) != TYPE_NIL and AudioManager != null:
		_audio_manager = AudioManager
	if typeof(GameManager) != TYPE_NIL and GameManager != null:
		_game_manager = GameManager

func _connect_game_manager() -> void:
	if _game_manager != null and _game_manager.has_signal("player_registered"):
		if not _game_manager.is_connected("player_registered", Callable(self, "_on_player_registered")):
			_game_manager.player_registered.connect(_on_player_registered)

func _start_next_craft(station_type: String) -> void:
	var queue: Array = crafting_queues.get(station_type, [])
	if queue.is_empty():
		return
	var entry: QueueEntry = queue[0]
	if entry == null:
		queue.remove_at(0)
		crafting_queues[station_type] = queue
		return
	var recipe := get_recipe_definition(entry.recipe_id)
	if recipe == null:
		queue.remove_at(0)
		crafting_queues[station_type] = queue
		emit_signal("queue_updated", station_type)
		return
	var validation := _validate_recipe_materials(recipe)
	if not validation.get("valid", false):
		_emit_recipe_failed(recipe.recipe_id, "missing_materials", validation.get("missing", []))
		queue.remove_at(0)
		crafting_queues[station_type] = queue
		emit_signal("queue_updated", station_type)
		_start_next_craft(station_type)
		return
	var consumed := _consume_recipe_materials(recipe)
	if not consumed:
		_emit_recipe_failed(recipe.recipe_id, "consume_failed", [])
		queue.remove_at(0)
		crafting_queues[station_type] = queue
		emit_signal("queue_updated", station_type)
		_start_next_craft(station_type)
		return
	var craft := ActiveCraft.new()
	craft.recipe_id = recipe.recipe_id
	craft.recipe = recipe
	craft.duration = max(recipe.crafting_time, 0.1)
	craft.elapsed = 0.0
	craft.progress = 0.0
	craft.inputs_consumed = true
	craft.station_type = station_type
	craft.queue_entry = entry
	craft.position = _determine_craft_position(station_type)
	craft.sound_player = _start_crafting_sound(station_type, craft.position)
	active_crafts[station_type] = craft
	emit_signal("recipe_started", recipe.recipe_id, station_type)
	crafting_queues[station_type] = queue

func _complete_craft(station_type: String) -> void:
	var craft: ActiveCraft = active_crafts.get(station_type, null)
	if craft == null:
		return
	var recipe := craft.recipe
	if recipe == null:
		active_crafts[station_type] = null
		return
	var outputs := []
	for entry in recipe.outputs:
		outputs.append(entry)
	var all_added := true
	if _inventory != null:
		for output in recipe.outputs:
			var item_id: String = output.get("item_id", "")
			var quantity: int = output.get("quantity", 0)
			if item_id == "" or quantity <= 0:
				continue
			var added := _inventory.add_item(item_id, quantity)
			if not added:
				all_added = false
	if not all_added:
		_spawn_item_drops(recipe, station_type, craft.position)
	_stop_crafting_sound(station_type)
	_play_completion_sound(station_type, craft.position)
	_finalize_queue_after_completion(station_type, craft)
	emit_signal("recipe_completed", recipe.recipe_id, outputs)
	active_crafts[station_type] = null
	_start_next_craft(station_type)

func _cancel_active_craft(station_type: String, refund: bool = true) -> void:
	var craft: ActiveCraft = active_crafts.get(station_type, null)
	if craft == null:
		return
	if refund and craft.inputs_consumed and craft.recipe != null:
		_refund_recipe_materials(craft.recipe)
	_stop_crafting_sound(station_type)
	active_crafts[station_type] = null
	var queue := crafting_queues.get(station_type, [])
	var entry: QueueEntry = craft.queue_entry
	if entry != null:
		var index := queue.find(entry)
		if index != -1:
			entry.quantity = max(entry.quantity - 1, 0)
			if entry.quantity <= 0:
				queue.remove_at(index)
			else:
				queue[index] = entry
	crafting_queues[station_type] = queue
	emit_signal("queue_updated", station_type)
	emit_signal("recipe_cancelled", craft.recipe_id, "cancelled")
	if not queue.is_empty():
		_start_next_craft(station_type)

func _validate_recipe_materials(recipe: Recipe) -> Dictionary:
	var missing: Array = []
	if _inventory == null:
		return {"valid": false, "missing": missing}
	for input in recipe.inputs:
		var item_id: String = input.get("item_id", "")
		var quantity: int = input.get("quantity", 0)
		if item_id == "" or quantity <= 0:
			continue
		if _inventory.get_item_count(item_id) < quantity:
			missing.append({
				"item_id": item_id,
				"required": quantity,
				"available": _inventory.get_item_count(item_id),
				"missing": max(quantity - _inventory.get_item_count(item_id), 0),
			})
	if recipe.requires_fuel():
		var fuel_id := recipe.get_fuel_item_id()
		var fuel_qty := recipe.get_fuel_quantity()
		if _inventory.get_item_count(fuel_id) < fuel_qty:
			missing.append({
				"item_id": fuel_id,
				"required": fuel_qty,
				"available": _inventory.get_item_count(fuel_id),
				"missing": max(fuel_qty - _inventory.get_item_count(fuel_id), 0),
			})
	return {"valid": missing.is_empty(), "missing": missing}

func _consume_recipe_materials(recipe: Recipe) -> bool:
	if _inventory == null:
		return false
	var consumed: Array = []
	for entry in recipe.inputs:
		var item_id: String = entry.get("item_id", "")
		var quantity: int = entry.get("quantity", 0)
		if item_id == "" or quantity <= 0:
			continue
		if not _inventory.remove_item(item_id, quantity):
			for consumed_entry in consumed:
				_inventory.add_item(consumed_entry[0], consumed_entry[1])
			return false
		consumed.append([item_id, quantity])
	if recipe.requires_fuel():
		var fuel_id := recipe.get_fuel_item_id()
		var fuel_qty := recipe.get_fuel_quantity()
		if not _inventory.remove_item(fuel_id, fuel_qty):
			for consumed_entry in consumed:
				_inventory.add_item(consumed_entry[0], consumed_entry[1])
			return false
		consumed.append([fuel_id, fuel_qty])
	return true

func _refund_recipe_materials(recipe: Recipe) -> void:
	if _inventory == null:
		return
	for entry in recipe.inputs:
		var item_id: String = entry.get("item_id", "")
		var quantity: int = entry.get("quantity", 0)
		if item_id != "" and quantity > 0:
			_inventory.add_item(item_id, quantity)
	if recipe.requires_fuel():
		var fuel_id := recipe.get_fuel_item_id()
		var fuel_qty := recipe.get_fuel_quantity()
		if fuel_id != "" and fuel_qty > 0:
			_inventory.add_item(fuel_id, fuel_qty)

func _start_crafting_sound(station_type: String, position: Vector3) -> AudioStreamPlayer3D:
	if _audio_manager == null:
		return null
	var stream := _load_crafting_stream("%s_loop.ogg" % station_type)
	if stream == null:
		stream = _load_crafting_stream("default_loop.ogg")
	if stream == null:
		return null
	return _audio_manager.play_sfx_3d(stream, position)

func _stop_crafting_sound(station_type: String) -> void:
	var craft: ActiveCraft = active_crafts.get(station_type, null)
	if craft == null or craft.sound_player == null:
		return
	craft.sound_player.stop()
	craft.sound_player = null

func _play_completion_sound(station_type: String, position: Vector3) -> void:
	if _audio_manager == null:
		return
	var stream := _load_crafting_stream("%s_complete.ogg" % station_type)
	if stream == null:
		stream = _load_crafting_stream("default_complete.ogg")
	if stream == null:
		return
	_audio_manager.play_sfx_3d(stream, position)

func _spawn_item_drops(recipe: Recipe, station_type: String, position: Vector3) -> void:
	var outputs: Array = []
	for entry in recipe.outputs:
		outputs.append(entry)
	_spawn_dropped_items(outputs, position)
	_log_warning("Inventory full, outputs dropped for recipe '%s'." % recipe.recipe_id)

func _spawn_dropped_items(drops: Array, position: Vector3) -> void:
	if drops.is_empty():
		return
	if not ResourceLoader.exists(DROPPED_ITEM_SCENE_PATH):
		_log_warning("Dropped item scene missing at %s" % DROPPED_ITEM_SCENE_PATH)
		return
	var scene := ResourceLoader.load(DROPPED_ITEM_SCENE_PATH)
	if scene == null or not (scene is PackedScene):
		_log_warning("Failed to load dropped item scene")
		return
	var parent := get_tree().current_scene
	if parent == null:
		_log_warning("No current scene to spawn drops")
		return
	for drop in drops:
		var item_id := String(drop.get("item_id", ""))
		var quantity := int(drop.get("quantity", 0))
		if item_id.is_empty() or quantity <= 0:
			continue
		var instance := scene.instantiate()
		if instance == null:
			continue
		parent.add_child(instance)
		if instance.has_method("set_item_data"):
			instance.set_item_data(item_id, quantity)
		if instance is Node3D:
			(instance as Node3D).global_position = position

func _load_recipes_database() -> void:
	var file := FileAccess.open(RECIPES_DB_PATH, FileAccess.READ)
	if file == null:
		_log_error("Failed to open recipes database at %s" % RECIPES_DB_PATH)
		return
	var json_text := file.get_as_text()
	file.close()
	var json := JSON.new()
	var error := json.parse(json_text)
	if error != OK:
		_log_error("Failed to parse recipes database: %s" % JSON.get_error_message(error))
		return
	var data := json.data
	if typeof(data) != TYPE_DICTIONARY:
		_log_error("Invalid recipes database format.")
		return
	if data.get("version", -1) != RECIPES_DB_VERSION:
		_log_warning("Recipes database version mismatch. Expected %d, got %d." % [RECIPES_DB_VERSION, data.get("version", -1)])
	_invalid_recipes.clear()
	recipes_db = _filter_valid_recipes(data.get("recipes", {}))
	_recipe_defs.clear()

func _filter_valid_recipes(source: Dictionary) -> Dictionary:
	var result := {}
	if typeof(source) != TYPE_DICTIONARY:
		return result
	for recipe_id in source.keys():
		var entry := source.get(recipe_id, {})
		if typeof(entry) != TYPE_DICTIONARY:
			_invalid_recipes[recipe_id] = true
			_log_warning("Recipe '%s' has invalid format; skipping." % recipe_id)
			continue
		var recipe := Recipe.new()
		for key in entry.keys():
			if recipe.has_property(key):
				recipe.set(key, entry[key])
		if recipe.recipe_id.is_empty():
			recipe.recipe_id = String(recipe_id)
		if _validate_recipe_items(recipe):
			result[recipe_id] = entry
		else:
			_invalid_recipes[recipe_id] = true
	return result

func _validate_recipe_items(recipe: Recipe) -> bool:
	if recipe == null:
		return false
	var inventory := _get_inventory()
	if inventory == null:
		_log_warning("Inventory unavailable while validating recipe '%s'; assuming valid." % recipe.recipe_id)
		return true
	for entry in recipe.inputs:
		var item_id := String(entry.get("item_id", ""))
		if item_id.is_empty() or inventory.get_resource_definition(item_id) == null:
			_log_error("Recipe '%s' references unknown input '%s'." % [recipe.recipe_id, item_id])
			return false
	for entry in recipe.outputs:
		var item_id := String(entry.get("item_id", ""))
		if item_id.is_empty() or inventory.get_resource_definition(item_id) == null:
			_log_error("Recipe '%s' references unknown output '%s'." % [recipe.recipe_id, item_id])
			return false
	if recipe.requires_fuel():
		var fuel_id := recipe.get_fuel_item_id()
		if fuel_id.is_empty() or inventory.get_resource_definition(fuel_id) == null:
			_log_error("Recipe '%s' references unknown fuel '%s'." % [recipe.recipe_id, fuel_id])
			return false
	return true

func _get_inventory() -> Inventory:
	if _inventory != null and is_instance_valid(_inventory):
		return _inventory
	if typeof(Inventory) != TYPE_NIL and Inventory != null:
		_inventory = Inventory
		return _inventory
	var tree := get_tree()
	if tree != null and tree.has_node("/root/Inventory"):
		var node := tree.get_node("/root/Inventory")
		if node is Inventory:
			_inventory = node
			return _inventory
	return null

func _connect_signals() -> void:
	pass

func _on_player_registered(player: Node) -> void:
	_player = player

func _emit_recipe_failed(recipe_id: String, error: String, missing_materials: Array) -> void:
	emit_signal("recipe_failed", recipe_id, error, missing_materials)

func _log_info(message: String) -> void:
	if typeof(ErrorLogger) != TYPE_NIL and ErrorLogger != null and ErrorLogger.has_method("log_info"):
		ErrorLogger.log_info(message, "CraftingSystem")
	else:
		print("[CraftingSystem] %s" % message)

func _log_warning(message: String) -> void:
	if typeof(ErrorLogger) != TYPE_NIL and ErrorLogger != null and ErrorLogger.has_method("log_warning"):
		ErrorLogger.log_warning(message, "CraftingSystem")
	else:
		push_warning(message)

func _log_error(message: String) -> void:
	if typeof(ErrorLogger) != TYPE_NIL and ErrorLogger != null and ErrorLogger.has_method("log_error"):
		ErrorLogger.log_error(message, "CraftingSystem")
	else:
		push_error(message)

func _log_debug(message: String) -> void:
	if typeof(ErrorLogger) != TYPE_NIL and ErrorLogger != null and ErrorLogger.has_method("log_debug"):
		ErrorLogger.log_debug(message, "CraftingSystem")
	else:
		print("[CraftingSystem] %s" % message)

func _determine_craft_position(station_type: String) -> Vector3:
	var origin := Vector3.ZERO
	if _player is Node3D:
		origin = (_player as Node3D).global_position
	var station := get_nearest_station(station_type, origin)
	if station != null and station is Node3D:
		return (station as Node3D).global_position
	return origin

func _load_crafting_stream(filename: String) -> AudioStream:
	if filename.is_empty():
		return null
	var path := "%s%s" % [CRAFTING_SOUND_PATH, filename]
	if not ResourceLoader.exists(path):
		return null
	var stream := ResourceLoader.load(path)
	return stream if stream is AudioStream else null

func _finalize_queue_after_completion(station_type: String, craft: ActiveCraft) -> void:
	var entry: QueueEntry = craft.queue_entry
	if entry == null:
		emit_signal("queue_updated", station_type)
		return
	var queue: Array = crafting_queues.get(station_type, [])
	var index := queue.find(entry)
	if index == -1:
		emit_signal("queue_updated", station_type)
		return
	entry.quantity = max(entry.quantity - 1, 0)
	if entry.quantity <= 0:
		queue.remove_at(index)
	else:
		queue[index] = entry
	crafting_queues[station_type] = queue
	emit_signal("queue_updated", station_type)
