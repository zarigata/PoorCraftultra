extends Node
class_name Inventory

signal inventory_changed
signal slot_updated(slot_index: int, item_id: String, quantity: int)
signal hotbar_changed(hotbar_index: int, inventory_slot: int)
signal hotbar_selected(hotbar_index: int)
signal item_added(item_id: String, quantity: int, success: bool)
signal item_removed(item_id: String, quantity: int, success: bool)
signal capacity_exceeded(item_id: String, quantity: int)
signal weight_exceeded(current_weight: float, max_weight: float)

const RESOURCES_DB_PATH := "res://resources/data/resources.json"
const RESOURCES_DB_VERSION := 1
const MAX_INVENTORY_SLOTS := 30
const MAX_HOTBAR_SLOTS := 9
const MAX_WEIGHT := 100.0
const INVALID_SLOT := -1

const RESOURCE_ASSIGNABLE_FIELDS := [
	"resource_id",
	"display_name",
	"description",
	"category",
	"tier",
	"stack_size",
	"weight",
	"icon_path",
	"is_consumable",
	"is_tool",
	"tool_resource_path",
	"is_placeable",
	"metadata",
]

var slots: Array[Dictionary] = []
var hotbar_indices: Array[int] = []
var selected_hotbar_slot: int = 0
var resources_db: Dictionary = {}
var _resource_defs: Dictionary = {}
var current_weight: float = 0.0

var _player: Node = null
var _game_manager: GameManager = null
var _auto_sort_on_pickup: bool = false

func _ready() -> void:
	slots = []
	for _i in range(MAX_INVENTORY_SLOTS):
		slots.append(_create_empty_slot())

	hotbar_indices = []
	for _j in range(MAX_HOTBAR_SLOTS):
		hotbar_indices.append(INVALID_SLOT)

	_load_resources_database()
	_update_current_weight()
	_connect_to_game_manager()
	_refresh_auto_sort_setting()

	if typeof(ErrorLogger) != TYPE_NIL and ErrorLogger != null and ErrorLogger.has_method("log_info"):
		ErrorLogger.log_info("Inventory initialized", "Inventory")

func add_item(item_id: String, quantity: int) -> bool:
	if quantity <= 0:
		return false
	var resource := get_resource_definition(item_id)
	if resource == null:
		_log_warning("Attempted to add unknown resource %s" % item_id)
		emit_signal("item_added", item_id, quantity, false)
		return false

	var stack_size := max(1, resource.stack_size)
	var additional_weight := resource.get_total_weight(quantity)
	if current_weight + additional_weight > MAX_WEIGHT + 0.001:
		emit_signal("weight_exceeded", current_weight, MAX_WEIGHT)
		emit_signal("item_added", item_id, quantity, false)
		_log_warning("Weight limit exceeded when adding %s x%d" % [item_id, quantity])
		return false

	if not _has_capacity_for(item_id, quantity, stack_size):
		emit_signal("capacity_exceeded", item_id, quantity)
		emit_signal("item_added", item_id, quantity, false)
		_log_warning("Inventory full when adding %s" % item_id)
		return false

	var remaining := quantity
	for index in range(MAX_INVENTORY_SLOTS):
		if remaining <= 0:
			break
		var slot := slots[index]
		if slot["item_id"] == item_id and slot["quantity"] < stack_size:
			var space_left := stack_size - slot["quantity"]
			var to_add := min(space_left, remaining)
			slot["quantity"] += to_add
			slots[index] = slot
			emit_signal("slot_updated", index, item_id, slot["quantity"])
			remaining -= to_add

	while remaining > 0:
		var empty_slot := _find_empty_slot()
		if empty_slot == INVALID_SLOT:
			break
		var to_place := min(stack_size, remaining)
		slots[empty_slot] = {"item_id": item_id, "quantity": to_place}
		emit_signal("slot_updated", empty_slot, item_id, to_place)
		remaining -= to_place

	_update_current_weight()
	if _auto_sort_on_pickup:
		auto_sort()
	else:
		emit_signal("inventory_changed")

	emit_signal("item_added", item_id, quantity, true)
	return true

func remove_item(item_id: String, quantity: int) -> bool:
	if quantity <= 0:
		return false
	var total_available := get_item_count(item_id)
	if total_available < quantity:
		emit_signal("item_removed", item_id, quantity, false)
		return false

	var remaining := quantity
	for index in range(MAX_INVENTORY_SLOTS - 1, -1, -1):
		if remaining <= 0:
			break
		var slot := slots[index]
		if slot["item_id"] != item_id:
			continue
		var to_remove := min(slot["quantity"], remaining)
		slot["quantity"] -= to_remove
		remaining -= to_remove
		if slot["quantity"] <= 0:
			slots[index] = _create_empty_slot()
			emit_signal("slot_updated", index, "", 0)
		else:
			slots[index] = slot
			emit_signal("slot_updated", index, item_id, slot["quantity"])

	_update_current_weight()
	emit_signal("inventory_changed")
	emit_signal("item_removed", item_id, quantity, true)
	return true

func has_item(item_id: String, quantity: int = 1) -> bool:
	return get_item_count(item_id) >= quantity

func get_item_count(item_id: String) -> int:
	var total := 0
	for slot in slots:
		if slot["item_id"] == item_id:
			total += int(slot["quantity"])
	return total

func move_item(from_slot: int, to_slot: int) -> bool:
	if not _is_valid_slot(from_slot) or not _is_valid_slot(to_slot) or from_slot == to_slot:
		return false
	var source := slots[from_slot]
	var target := slots[to_slot]
	if source["item_id"].is_empty():
		return false

	if target["item_id"] == source["item_id"]:
		var resource := get_resource_definition(source["item_id"])
		if resource == null:
			return false
		var stack_size := max(1, resource.stack_size)
		var total_quantity := source["quantity"] + target["quantity"]
		var new_target_quantity := min(stack_size, total_quantity)
		var remainder := total_quantity - new_target_quantity
		target["quantity"] = new_target_quantity
		source["quantity"] = remainder
		slots[to_slot] = target
		slots[from_slot] = source if remainder > 0 else _create_empty_slot()
		if remainder <= 0:
			emit_signal("slot_updated", from_slot, "", 0)
		else:
			emit_signal("slot_updated", from_slot, source["item_id"], source["quantity"])
		emit_signal("slot_updated", to_slot, target["item_id"], target["quantity"])
	else:
		slots[from_slot] = target
		slots[to_slot] = source
		emit_signal("slot_updated", from_slot, slots[from_slot]["item_id"], slots[from_slot]["quantity"])
		emit_signal("slot_updated", to_slot, slots[to_slot]["item_id"], slots[to_slot]["quantity"])

	emit_signal("inventory_changed")
	return true

func split_stack(slot_index: int, split_quantity: int) -> int:
	if not _is_valid_slot(slot_index) or split_quantity <= 0:
		return INVALID_SLOT
	var slot := slots[slot_index]
	if slot["item_id"].is_empty() or slot["quantity"] <= split_quantity:
		return INVALID_SLOT
	var empty_slot := _find_empty_slot()
	if empty_slot == INVALID_SLOT:
		return INVALID_SLOT

	slot["quantity"] -= split_quantity
	slots[slot_index] = slot
	slots[empty_slot] = {"item_id": slot["item_id"], "quantity": split_quantity}

	emit_signal("slot_updated", slot_index, slot["item_id"], slot["quantity"])
	emit_signal("slot_updated", empty_slot, slot["item_id"], split_quantity)
	emit_signal("inventory_changed")
	return empty_slot

func clear_inventory() -> void:
	for index in range(MAX_INVENTORY_SLOTS):
		slots[index] = _create_empty_slot()
	for hotbar_index in range(MAX_HOTBAR_SLOTS):
		hotbar_indices[hotbar_index] = INVALID_SLOT
	selected_hotbar_slot = 0
	_update_current_weight()
	emit_signal("inventory_changed")

func assign_hotbar_slot(hotbar_index: int, inventory_slot: int) -> bool:
	if not _is_valid_hotbar_index(hotbar_index):
		return false
	if inventory_slot != INVALID_SLOT and not _is_valid_slot(inventory_slot):
		return false
	hotbar_indices[hotbar_index] = inventory_slot
	emit_signal("hotbar_changed", hotbar_index, inventory_slot)
	return true

func get_hotbar_item(hotbar_index: int) -> Dictionary:
	if not _is_valid_hotbar_index(hotbar_index):
		return _create_empty_slot()
	var slot_index := hotbar_indices[hotbar_index]
	if not _is_valid_slot(slot_index):
		return _create_empty_slot()
	return slots[slot_index]

func select_hotbar_slot(hotbar_index: int) -> void:
	if not _is_valid_hotbar_index(hotbar_index):
		return
	selected_hotbar_slot = hotbar_index
	emit_signal("hotbar_selected", hotbar_index)

func get_selected_hotbar_item() -> Dictionary:
	return get_hotbar_item(selected_hotbar_slot)

func cycle_hotbar(direction: int) -> void:
	if direction == 0:
		return
	var target := (selected_hotbar_slot + direction) % MAX_HOTBAR_SLOTS
	if target < 0:
		target += MAX_HOTBAR_SLOTS
	select_hotbar_slot(target)

func auto_sort() -> void:
	var pinned_slots := {}
	for hotbar_index in range(MAX_HOTBAR_SLOTS):
		var slot_index := hotbar_indices[hotbar_index]
		if _is_valid_slot(slot_index) and not pinned_slots.has(slot_index):
			pinned_slots[slot_index] = slots[slot_index].duplicate(true)

	var populated_slots := []
	for index in range(MAX_INVENTORY_SLOTS):
		if pinned_slots.has(index):
			continue
		var slot := slots[index]
		if slot["item_id"].is_empty():
			continue
		populated_slots.append(slot.duplicate(true))

	populated_slots.sort_custom(func(a, b):
		var resource_a := get_resource_definition(a["item_id"])
		var resource_b := get_resource_definition(b["item_id"])
		if resource_a == null and resource_b == null:
			return false
		if resource_a == null:
			return false
		if resource_b == null:
			return true
		var category_a := int(resource_a.category)
		var category_b := int(resource_b.category)
		if category_a != category_b:
			return category_a < category_b
		var tier_a := int(resource_a.tier)
		var tier_b := int(resource_b.tier)
		if tier_a != tier_b:
			return tier_a < tier_b
		var name_a := String(resource_a.display_name)
		var name_b := String(resource_b.display_name)
		return name_a.naturalnocasecmp_to(name_b) < 0
	)

	var populated_index := 0
	for index in range(MAX_INVENTORY_SLOTS):
		if pinned_slots.has(index):
			slots[index] = pinned_slots[index].duplicate(true)
		else:
			if populated_index < populated_slots.size():
				slots[index] = populated_slots[populated_index].duplicate(true)
				populated_index += 1
			else:
				slots[index] = _create_empty_slot()
		emit_signal("slot_updated", index, slots[index]["item_id"], slots[index]["quantity"])

	for hotbar_index in range(MAX_HOTBAR_SLOTS):
		emit_signal("hotbar_changed", hotbar_index, hotbar_indices[hotbar_index])

	emit_signal("inventory_changed")

func get_resource_definition(item_id: String) -> GameResource:
	if item_id.is_empty():
		return null
	if not resources_db.has(item_id):
		return null
	if _resource_defs.has(item_id):
		return _resource_defs[item_id]
	var definition_dict := resources_db[item_id]
	if typeof(definition_dict) != TYPE_DICTIONARY:
		return null
	var resource := GameResource.new()
	var property_names := {}
	for property in resource.get_property_list():
		var name := String(property.get("name", ""))
		if not name.is_empty():
			property_names[name] = true
	for field in RESOURCE_ASSIGNABLE_FIELDS:
		if definition_dict.has(field) and property_names.has(field):
			resource.set(field, definition_dict[field])
	if typeof(resource.metadata) == TYPE_DICTIONARY:
		resource.metadata = resource.metadata.duplicate(true)
	_resource_defs[item_id] = resource
	return resource

func is_inventory_full() -> bool:
	if _find_empty_slot() != INVALID_SLOT:
		return false
	for slot in slots:
		if slot["item_id"].is_empty():
			return false
		var resource := get_resource_definition(slot["item_id"])
		var stack_size := 1
		if resource != null:
			stack_size = max(1, resource.stack_size)
		if slot["quantity"] < stack_size:
			return false
	return true

func get_empty_slot_count() -> int:
	var count := 0
	for slot in slots:
		if slot["item_id"].is_empty():
			count += 1
	return count

func get_weight_percentage() -> float:
	return current_weight / MAX_WEIGHT if MAX_WEIGHT > 0.0 else 0.0

func get_slot_count() -> int:
	return MAX_INVENTORY_SLOTS

func get_slot(index: int) -> Dictionary:
	if not _is_valid_slot(index):
		return _create_empty_slot()
	return slots[index]

func get_current_weight() -> float:
	return current_weight

func get_hotbar_indices() -> Array:
	return hotbar_indices.duplicate()

func get_selected_hotbar_index() -> int:
	return selected_hotbar_slot

func serialize() -> Dictionary:
	var backpack := []
	for index in range(MAX_INVENTORY_SLOTS):
		var slot := slots[index]
		if slot["item_id"].is_empty() or slot["quantity"] <= 0:
			continue
		var resource := get_resource_definition(slot["item_id"])
		if resource == null:
			_log_warning("Skipping unknown item %s during save" % slot["item_id"])
			continue
		backpack.append({
			"slot": index,
			"item_id": slot["item_id"],
			"quantity": slot["quantity"],
		})

	return {
		"backpack": backpack,
		"hotbar_indices": hotbar_indices.duplicate(),
		"selected_hotbar": selected_hotbar_slot,
	}

func deserialize(data: Dictionary) -> bool:
	if data.is_empty():
		return false
	clear_inventory()

	var backpack := data.get("backpack", [])
	for entry in backpack:
		var slot_index := int(entry.get("slot", INVALID_SLOT))
		var item_id := String(entry.get("item_id", ""))
		var quantity := int(entry.get("quantity", 0))
		if not _is_valid_slot(slot_index) or item_id.is_empty() or quantity <= 0:
			continue
		var resource := get_resource_definition(item_id)
		if resource == null:
			_log_warning("Skipping unknown item %s during load" % item_id)
			continue
		var stack_size := max(1, resource.stack_size)
		quantity = clamp(quantity, 1, stack_size)
		slots[slot_index] = {"item_id": item_id, "quantity": quantity}
		emit_signal("slot_updated", slot_index, item_id, quantity)

	var hotbar_data := data.get("hotbar_indices", [])
	if hotbar_data.size() == MAX_HOTBAR_SLOTS:
		for i in range(MAX_HOTBAR_SLOTS):
			var hotbar_slot := int(hotbar_data[i])
			if _is_valid_slot(hotbar_slot) or hotbar_slot == INVALID_SLOT:
				hotbar_indices[i] = hotbar_slot
				emit_signal("hotbar_changed", i, hotbar_indices[i])

	selected_hotbar_slot = clamp(int(data.get("selected_hotbar", 0)), 0, MAX_HOTBAR_SLOTS - 1)
	emit_signal("hotbar_selected", selected_hotbar_slot)

	_update_current_weight()
	emit_signal("inventory_changed")
	return true

func _load_resources_database() -> void:
	var file := FileAccess.open(RESOURCES_DB_PATH, FileAccess.READ)
	if file == null:
		_log_error("Failed to load resources database at %s" % RESOURCES_DB_PATH)
		resources_db = {}
		return
	var raw_json := file.get_as_text()
	file.close()
	var parsed := JSON.parse_string(raw_json)
	if typeof(parsed) != TYPE_DICTIONARY:
		_log_error("Invalid resources database format")
		resources_db = {}
		return
	var version := int(parsed.get("version", 1))
	if version != RESOURCES_DB_VERSION:
		_log_warning("Unexpected resources database version %d" % version)
	resources_db = parsed.get("resources", {})
	if typeof(resources_db) != TYPE_DICTIONARY:
		resources_db = {}
	_resource_defs.clear()

func _find_slot_with_item(item_id: String) -> int:
	for index in range(MAX_INVENTORY_SLOTS):
		var slot := slots[index]
		if slot["item_id"] == item_id:
			return index
	return INVALID_SLOT

func _find_empty_slot() -> int:
	for index in range(MAX_INVENTORY_SLOTS):
		if slots[index]["item_id"].is_empty():
			return index
	return INVALID_SLOT

func _calculate_total_weight() -> float:
	var total := 0.0
	for index in range(MAX_INVENTORY_SLOTS):
		var slot := slots[index]
		if slot["item_id"].is_empty() or slot["quantity"] <= 0:
			continue
		var resource := get_resource_definition(slot["item_id"])
		if resource == null:
			continue
		total += resource.get_total_weight(slot["quantity"])
	return total

func _update_current_weight() -> void:
	current_weight = _calculate_total_weight()

func _has_capacity_for(item_id: String, quantity: int, stack_size: int) -> bool:
	var remaining := quantity
	for slot in slots:
		if slot["item_id"] != item_id:
			continue
		var space_left := max(stack_size - int(slot["quantity"]), 0)
		remaining -= space_left
		if remaining <= 0:
			return true

	for slot in slots:
		if not slot["item_id"].is_empty():
			continue
		remaining -= stack_size
		if remaining <= 0:
			return true

	return remaining <= 0

func _connect_to_game_manager() -> void:
	if typeof(GameManager) == TYPE_NIL or GameManager == null:
		_log_warning("GameManager not available for Inventory")
		return
	_game_manager = GameManager
	if not GameManager.is_connected("player_registered", Callable(self, "_on_player_registered")):
		GameManager.connect("player_registered", Callable(self, "_on_player_registered"))
	if not GameManager.is_connected("settings_changed", Callable(self, "_on_settings_changed")):
		GameManager.connect("settings_changed", Callable(self, "_on_settings_changed"))
	if GameManager.get_player() != null:
		_on_player_registered(GameManager.get_player())

func _refresh_auto_sort_setting() -> void:
	if typeof(GameManager) == TYPE_NIL or GameManager == null:
		_auto_sort_on_pickup = false
		return
	_auto_sort_on_pickup = bool(GameManager.get_setting("inventory/auto_sort_on_pickup", false))

func _on_player_registered(player: Node) -> void:
	_player = player
	if _player == null:
		return
	if _player.has_signal("item_collected"):
		if not _player.is_connected("item_collected", Callable(self, "_on_item_collected")):
			_player.connect("item_collected", Callable(self, "_on_item_collected"))

func _on_item_collected(item_id: String, quantity: int) -> void:
	var success := add_item(item_id, quantity)
	if not success:
		_log_warning("Failed to collect %s x%d" % [item_id, quantity])

func _on_settings_changed(setting_name: String, new_value: Variant) -> void:
	if setting_name == "inventory/auto_sort_on_pickup":
		_auto_sort_on_pickup = bool(new_value)

func _is_valid_slot(index: int) -> bool:
	return index >= 0 and index < MAX_INVENTORY_SLOTS

func _is_valid_hotbar_index(index: int) -> bool:
	return index >= 0 and index < MAX_HOTBAR_SLOTS

func _create_empty_slot() -> Dictionary:
	return {"item_id": "", "quantity": 0}

func _log_warning(message: String) -> void:
	if typeof(ErrorLogger) != TYPE_NIL and ErrorLogger != null and ErrorLogger.has_method("log_warning"):
		ErrorLogger.log_warning(message, "Inventory")
	else:
		push_warning(message)

func _log_error(message: String) -> void:
	if typeof(ErrorLogger) != TYPE_NIL and ErrorLogger != null and ErrorLogger.has_method("log_error"):
		ErrorLogger.log_error(message, "Inventory")
	else:
		push_error(message)
