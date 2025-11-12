extends Resource
class_name Tool

@export var tool_id: String = ""
@export var tool_name: String = ""
@export var tool_tier: int = 1:
	set(value):
		var adjusted := max(1, value)
		if adjusted != value:
			_log_warning("Tool tier adjusted to minimum of 1 for %s" % tool_id)
		tool_tier = adjusted
@export var max_durability: int = 100:
	set(value):
		var adjusted := max(1, value)
		if adjusted != value:
			_log_warning("Max durability adjusted to minimum of 1 for %s" % tool_id)
		max_durability = adjusted
		current_durability = clamp(current_durability, 0, max_durability)
@export var current_durability: int = 100:
	set(value):
		var adjusted := clamp(value, 0, max_durability)
		if adjusted != value:
			_log_warning("Current durability clamped to range 0-%d for %s" % [max_durability, tool_id])
		current_durability = adjusted
@export var mining_speed: float = 1.0:
	set(value):
		var adjusted := max(value, 0.01)
		if !is_equal_approx(adjusted, value):
			_log_warning("Mining speed adjusted to minimum of 0.01 for %s" % tool_id)
		mining_speed = adjusted
@export var mining_radius: float = 1.0:
	set(value):
		var adjusted := clamp(value, 0.01, 5.0)
		if !is_equal_approx(adjusted, value):
			_log_warning("Mining radius clamped to 0.01-5.0 for %s" % tool_id)
		mining_radius = adjusted
@export var can_mine: bool = true
@export var can_place: bool = false
@export var allowed_voxel_types: Array[String] = []
@export var efficiency_overrides: Dictionary = {}
@export var icon_path: String = ""

func _init() -> void:
	tool_tier = tool_tier # Ensure setter validation runs
	max_durability = max_durability
	current_durability = current_durability
	mining_speed = mining_speed
	mining_radius = mining_radius
	_validate_configuration()

func get_efficiency_for_voxel(voxel_type_name: String) -> float:
	if not can_mine:
		return 0.0
	if allowed_voxel_types.size() > 0 and not allowed_voxel_types.has(voxel_type_name):
		return 0.0
	if efficiency_overrides.has(voxel_type_name):
		return max(float(efficiency_overrides[voxel_type_name]), 0.0)
	return 1.0

func can_mine_voxel(voxel_type_name: String) -> bool:
	if not can_mine:
		return false
	return allowed_voxel_types.is_empty() or allowed_voxel_types.has(voxel_type_name)

func use_durability(amount: int = 1) -> bool:
	if amount <= 0:
		return current_durability > 0
	current_durability = clamp(current_durability - amount, 0, max_durability)
	return current_durability > 0

func repair(amount: int) -> void:
	if amount <= 0:
		return
	current_durability = clamp(current_durability + amount, 0, max_durability)

func get_durability_percentage() -> float:
	if max_durability <= 0:
		return 0.0
	return float(current_durability) / float(max_durability)

func is_broken() -> bool:
	return current_durability <= 0

func duplicate_tool() -> Tool:
	var duplicate := duplicate(true)
	if duplicate is Tool:
		return duplicate
	return null

func _validate_configuration() -> void:
	if mining_radius > 5.0:
		_log_warning("Mining radius exceeds recommended cap for %s" % tool_id)
	if mining_radius <= 0.0:
		_log_warning("Mining radius must be positive for %s" % tool_id)
	if tool_tier < 1:
		_log_warning("Tool tier below minimum for %s" % tool_id)
	if max_durability <= 0:
		_log_warning("Max durability must be positive for %s" % tool_id)

func _log_warning(message: String) -> void:
	if typeof(ErrorLogger) != TYPE_NIL and ErrorLogger != null and ErrorLogger.has_method("log_warning"):
		ErrorLogger.log_warning(message, "Tool")
	else:
		push_warning(message)
