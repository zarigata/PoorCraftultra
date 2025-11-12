extends Resource
class_name GameResource

enum Category {
	MATERIAL,
	ORE,
	REFINED,
	COMPONENT,
	TOOL,
	CONSUMABLE,
	FURNITURE,
	QUEST_ITEM,
	MISC
}

@export var resource_id: String = ""
@export var display_name: String = ""
@export var description: String = ""
@export var category: Category = Category.MATERIAL
@export var tier: int = 1:
	set(value):
		var adjusted := max(0, value)
		if adjusted != value:
			_log_warning("Tier adjusted to minimum of 0 for resource %s" % resource_id)
		tier = adjusted
@export var stack_size: int = 64:
	set(value):
		var adjusted := max(1, value)
		if adjusted != value:
			_log_warning("Stack size adjusted to minimum of 1 for resource %s" % resource_id)
		stack_size = adjusted
@export var weight: float = 1.0:
	set(value):
		var adjusted := max(value, 0.0)
		if !is_equal_approx(adjusted, value):
			_log_warning("Weight adjusted to minimum of 0.0 for resource %s" % resource_id)
		weight = adjusted
@export var icon_path: String = ""
@export var is_consumable: bool = false
@export var is_tool: bool = false
@export var tool_resource_path: String = ""
@export var is_placeable: bool = false
@export var metadata: Dictionary = {}

var _cached_tool: Tool = null

func _init() -> void:
	resource_id = resource_id
	display_name = display_name
	description = description
	category = category
	tier = tier
	stack_size = stack_size
	weight = weight
	icon_path = icon_path
	is_consumable = is_consumable
	is_tool = is_tool
	tool_resource_path = tool_resource_path
	is_placeable = is_placeable
	metadata = metadata.duplicate(true)
	_validate_configuration()

func _validate_configuration() -> void:
	if resource_id.is_empty():
		_log_error("Resource ID cannot be empty")
	if display_name.is_empty():
		_log_warning("Display name is empty for resource %s" % resource_id)
	if tier < 0:
		_log_warning("Tier below 0 detected for resource %s" % resource_id)
	if stack_size <= 0:
		_log_warning("Stack size must be positive for resource %s" % resource_id)
	if weight < 0.0:
		_log_warning("Weight must be non-negative for resource %s" % resource_id)
	if is_tool and tool_resource_path.is_empty():
		_log_warning("Tool resource path missing for tool resource %s" % resource_id)

func get_total_weight(quantity: int) -> float:
	if quantity <= 0:
		return 0.0
	return weight * float(quantity)

func can_stack_with(other: GameResource) -> bool:
	if other == null:
		return false
	if resource_id.is_empty() or other.resource_id.is_empty():
		return false
	if resource_id != other.resource_id:
		return false
	if is_tool or other.is_tool:
		return false
	return stack_size > 1 and other.stack_size > 1

func duplicate_resource() -> GameResource:
	var duplicate := duplicate(true)
	if duplicate is GameResource:
		return duplicate
	return null

func get_tool_resource() -> Tool:
	if not is_tool:
		return null
	if _cached_tool != null:
		return _cached_tool
	if tool_resource_path.is_empty():
		return null
	if not ResourceLoader.exists(tool_resource_path):
		_log_warning("Tool resource path %s does not exist for %s" % [tool_resource_path, resource_id])
		return null
	var loaded := load(tool_resource_path)
	if loaded is Tool:
		_cached_tool = loaded
		return _cached_tool
	_log_warning("Failed to load Tool resource for %s" % resource_id)
	return null

func _log_warning(message: String) -> void:
	if typeof(ErrorLogger) != TYPE_NIL and ErrorLogger != null and ErrorLogger.has_method("log_warning"):
		ErrorLogger.log_warning(message, "GameResource")
	else:
		push_warning(message)

func _log_error(message: String) -> void:
	if typeof(ErrorLogger) != TYPE_NIL and ErrorLogger != null and ErrorLogger.has_method("log_error"):
		ErrorLogger.log_error(message, "GameResource")
	else:
		push_error(message)
