extends Resource
class_name Recipe

const VALID_STATIONS := ["none", "workbench", "furnace", "machining_bench", "fabricator"]

@export var recipe_id: String = ""
@export var display_name: String = ""
@export var description: String = ""
@export var tier: int = 1:
	set(value):
		tier = max(value, 1)
		if tier != value:
			_log_warning("Recipe '%s' tier clamped to %d" % [recipe_id, tier])
@export var crafting_time: float = 1.0:
	set(value):
		crafting_time = max(value, 0.1)
		if crafting_time != value:
			_log_warning("Recipe '%s' crafting_time clamped to %.2f" % [recipe_id, crafting_time])
@export var required_station: String = "none"
@export var inputs: Array[Dictionary] = []
@export var outputs: Array[Dictionary] = []
@export var fuel_required: Dictionary = {}
@export var batch_size: int = 1:
	set(value):
		batch_size = max(value, 1)
		if batch_size != value:
			_log_warning("Recipe '%s' batch_size clamped to %d" % [recipe_id, batch_size])
@export var icon_path: String = ""
@export var category: String = "general"
@export var is_unlocked_by_default: bool = true
@export var unlock_requirements: Dictionary = {}

func _init() -> void:
	tier = tier
	crafting_time = crafting_time
	batch_size = batch_size
	_validate_configuration()

func _validate_configuration() -> void:
	if recipe_id.is_empty():
		_log_error("Recipe ID cannot be empty.")
	if display_name.is_empty():
		_log_warning("Recipe '%s' display name is empty." % recipe_id)
	if inputs.is_empty():
		_log_error("Recipe '%s' must define at least one input." % recipe_id)
	if outputs.is_empty():
		_log_error("Recipe '%s' must define at least one output." % recipe_id)
	if not VALID_STATIONS.has(required_station):
		_log_warning("Recipe '%s' has unknown station '%s'." % [recipe_id, required_station])

	for entry in inputs:
		_validate_io_entry(entry, "input")

	for entry in outputs:
		_validate_io_entry(entry, "output")

	if not fuel_required.is_empty():
		_validate_io_entry(fuel_required, "fuel")

func _validate_io_entry(entry: Dictionary, entry_type: String) -> void:
	var item_id := entry.get("item_id", "")
	var quantity := int(entry.get("quantity", 0))
	if item_id == "":
		_log_error("Recipe '%s' %s is missing item_id." % [recipe_id, entry_type])
	if quantity <= 0:
		_log_error("Recipe '%s' %s '%s' has invalid quantity %d." % [recipe_id, entry_type, item_id, quantity])

func get_total_input_weight(inventory: Inventory) -> float:
	if inventory == null:
		return 0.0
	var total_weight := 0.0
	for entry in inputs:
		var item_id: String = entry.get("item_id", "")
		var quantity: int = entry.get("quantity", 0)
		if item_id == "" or quantity <= 0:
			continue
		var definition := inventory.get_resource_definition(item_id)
		if definition == null:
			continue
		total_weight += definition.get_total_weight(quantity)
	return total_weight

func can_craft_with_inventory(inventory: Inventory) -> bool:
	if inventory == null:
		return false
	for entry in inputs:
		var item_id: String = entry.get("item_id", "")
		var quantity: int = entry.get("quantity", 0)
		if item_id == "" or quantity <= 0:
			continue
		if not inventory.has_item(item_id, quantity):
			return false
	if requires_fuel():
		var fuel_id := get_fuel_item_id()
		var fuel_qty := get_fuel_quantity()
		if fuel_id != "" and fuel_qty > 0 and not inventory.has_item(fuel_id, fuel_qty):
			return false
	return true

func get_missing_materials(inventory: Inventory) -> Array[Dictionary]:
	var missing: Array[Dictionary] = []
	if inventory == null:
		for entry in inputs:
			var item_id := entry.get("item_id", "")
			var quantity := int(entry.get("quantity", 0))
			if item_id != "" and quantity > 0:
				missing.append({
					"item_id": item_id,
					"required": quantity,
					"available": 0,
					"missing": quantity,
				})
		if requires_fuel():
			missing.append({
				"item_id": get_fuel_item_id(),
				"required": get_fuel_quantity(),
				"available": 0,
				"missing": get_fuel_quantity(),
			})
		return missing

	for entry in inputs:
		var item_id: String = entry.get("item_id", "")
		var quantity: int = entry.get("quantity", 0)
		if item_id == "" or quantity <= 0:
			continue
		var available := inventory.get_item_count(item_id)
		if available < quantity:
			missing.append({
				"item_id": item_id,
				"required": quantity,
				"available": available,
				"missing": quantity - available,
			})

	if requires_fuel():
		var fuel_id := get_fuel_item_id()
		var fuel_qty := get_fuel_quantity()
		if fuel_id != "" and fuel_qty > 0:
			var available_fuel := inventory.get_item_count(fuel_id)
			if available_fuel < fuel_qty:
				missing.append({
					"item_id": fuel_id,
					"required": fuel_qty,
					"available": available_fuel,
					"missing": fuel_qty - available_fuel,
				})
	return missing

func requires_fuel() -> bool:
	return not fuel_required.is_empty() and fuel_required.get("item_id", "") != "" and int(fuel_required.get("quantity", 0)) > 0

func get_fuel_item_id() -> String:
	return fuel_required.get("item_id", "")

func get_fuel_quantity() -> int:
	return int(fuel_required.get("quantity", 0))

func get_primary_output() -> Dictionary:
	if outputs.is_empty():
		return {}
	return outputs[0]

func duplicate_recipe() -> Recipe:
	var copy := duplicate(true)
	if copy is Recipe:
		return copy
	return null

func _log_warning(message: String) -> void:
	if typeof(ErrorLogger) != TYPE_NIL and ErrorLogger != null and ErrorLogger.has_method("log_warning"):
		ErrorLogger.log_warning(message, "Recipe")
	else:
		push_warning(message)

func _log_error(message: String) -> void:
	if typeof(ErrorLogger) != TYPE_NIL and ErrorLogger != null and ErrorLogger.has_method("log_error"):
		ErrorLogger.log_error(message, "Recipe")
	else:
		push_error(message)
