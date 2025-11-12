extends Resource
class_name BuildingPiece

enum GridType { SQUARE, TRIANGLE_A, TRIANGLE_B }
enum PieceType {
	FOUNDATION,
	FLOOR,
	WALL,
	WALL_DOORFRAME,
	WALL_WINDOW,
	DOOR,
	WINDOW,
	STAIRS,
	RAMP,
	ROOF_SQUARE,
	ROOF_TRIANGLE,
	PILLAR,
	BEAM
}
enum MaterialTier { WOOD, STONE, METAL }

@export var piece_id: String = ""
@export var display_name: String = ""
@export var description: String = ""
@export var piece_type: PieceType = PieceType.FOUNDATION
@export var grid_type: GridType = GridType.SQUARE
@export var material_tier: MaterialTier = MaterialTier.WOOD
@export var dimensions: Vector3 = Vector3(1.0, 0.3, 1.0)
@export var stability_cost: float = 0.5:
	set(value):
		stability_cost = clampf(value, 0.0, 1.0)
		if stability_cost != value:
			_log_warning("Building piece '%s' stability_cost clamped to %.2f" % [piece_id, stability_cost])
@export var load_capacity: float = 100.0:
	set(value):
		load_capacity = max(value, 1.0)
		if load_capacity != value:
			_log_warning("Building piece '%s' load_capacity clamped to %.2f" % [piece_id, load_capacity])
@export var self_weight: float = 10.0:
	set(value):
		self_weight = max(value, 0.1)
		if self_weight != value:
			_log_warning("Building piece '%s' self_weight clamped to %.2f" % [piece_id, self_weight])
@export var requires_foundation: bool = false
@export var can_be_foundation: bool = false
@export var snap_to_grid: bool = true
@export var rotation_increment_deg: float = 90.0:
	set(value):
		rotation_increment_deg = clampf(value, 1.0, 360.0)
		if rotation_increment_deg != value:
			_log_warning("Building piece '%s' rotation_increment_deg clamped to %.2f" % [piece_id, rotation_increment_deg])
@export var mesh_path: String = ""
@export var icon_path: String = ""
@export var category: String = "foundation"
@export var crafting_cost: Dictionary = {}
@export var health: float = 100.0

func _init() -> void:
	stability_cost = stability_cost
	load_capacity = load_capacity
	self_weight = self_weight
	rotation_increment_deg = rotation_increment_deg
	_validate_configuration()

func _validate_configuration() -> void:
	if piece_id.is_empty():
		_log_error("Building piece ID cannot be empty.")
	if display_name.is_empty():
		_log_warning("Building piece '%s' display name is empty." % piece_id)
	if mesh_path.is_empty():
		_log_warning("Building piece '%s' mesh_path is empty." % piece_id)
	if dimensions.x <= 0.0 or dimensions.y <= 0.0 or dimensions.z <= 0.0:
		_log_error("Building piece '%s' dimensions must be greater than zero." % piece_id)
	if stability_cost < 0.0 or stability_cost > 1.0:
		_log_warning("Building piece '%s' stability_cost outside 0-1 range." % piece_id)

func get_effective_capacity(tier_multiplier: float = 1.0) -> float:
	return load_capacity * tier_multiplier * _get_material_capacity_multiplier()

func get_effective_weight() -> float:
	return self_weight * _get_material_weight_multiplier()

func get_grid_size() -> Vector2i:
	return Vector2i.ONE

func get_snap_position(world_position: Vector3, rotation_y: float) -> Vector3:
	if not snap_to_grid:
		return world_position

	var cell := Vector3(floor(world_position.x), world_position.y, floor(world_position.z))
	var snapped := Vector3(cell.x + 0.5, world_position.y, cell.z + 0.5)

	if grid_type == GridType.TRIANGLE_A or grid_type == GridType.TRIANGLE_B:
		var offset := _get_triangle_offset(rotation_y)
		snapped.x += offset.x
		snapped.z += offset.y
	return snapped

func get_material_tier_name() -> String:
	match material_tier:
		MaterialTier.WOOD:
			return "Wood"
		MaterialTier.STONE:
			return "Stone"
		MaterialTier.METAL:
			return "Metal"
		_:
			return "Unknown"

func can_place_on(other: BuildingPiece) -> bool:
	if other == null:
		return can_be_foundation

	if piece_type == PieceType.FOUNDATION:
		return true
	if piece_type == PieceType.FLOOR:
		return other.piece_type in [PieceType.FOUNDATION, PieceType.FLOOR, PieceType.PILLAR, PieceType.BEAM]
	if piece_type == PieceType.WALL or piece_type == PieceType.WALL_DOORFRAME or piece_type == PieceType.WALL_WINDOW:
		return other.piece_type in [PieceType.FOUNDATION, PieceType.FLOOR]
	if piece_type == PieceType.ROOF_SQUARE or piece_type == PieceType.ROOF_TRIANGLE:
		return other.piece_type in [PieceType.WALL, PieceType.PILLAR, PieceType.BEAM]
	if piece_type == PieceType.STAIRS or piece_type == PieceType.RAMP:
		return other.piece_type in [PieceType.FLOOR, PieceType.FOUNDATION]
	if piece_type == PieceType.DOOR:
		return other.piece_type == PieceType.WALL_DOORFRAME
	if piece_type == PieceType.WINDOW:
		return other.piece_type == PieceType.WALL_WINDOW
	if piece_type == PieceType.PILLAR:
		return other.piece_type in [PieceType.FOUNDATION, PieceType.FLOOR]
	if piece_type == PieceType.BEAM:
		return other.piece_type in [PieceType.PILLAR, PieceType.WALL]
	return false

func duplicate_piece() -> BuildingPiece:
	var copy := duplicate(true)
	if copy is BuildingPiece:
		return copy
	return null

func _get_material_capacity_multiplier() -> float:
	match material_tier:
		MaterialTier.WOOD:
			return 1.0
		MaterialTier.STONE:
			return 1.2
		MaterialTier.METAL:
			return 1.5
		_:
			return 1.0

func _get_material_weight_multiplier() -> float:
	match material_tier:
		MaterialTier.WOOD:
			return 1.0
		MaterialTier.STONE:
			return 1.5
		MaterialTier.METAL:
			return 2.0
		_:
			return 1.0

func _get_triangle_offset(rotation_y: float) -> Vector2:
	var normalized := _get_rotation_snapped(rotation_y, 90.0)
	var direction := int(round(normalized / 90.0)) % 4
	var half := 0.25
	match direction:
		0:
			return Vector2(half, half) if grid_type == GridType.TRIANGLE_A else Vector2(-half, half)
		1:
			return Vector2(-half, half) if grid_type == GridType.TRIANGLE_A else Vector2(-half, -half)
		2:
			return Vector2(-half, -half) if grid_type == GridType.TRIANGLE_A else Vector2(half, -half)
		3:
			return Vector2(half, -half) if grid_type == GridType.TRIANGLE_A else Vector2(half, half)
		_:
			return Vector2.ZERO

func _get_rotation_snapped(rotation_y: float, increment: float) -> float:
	if increment <= 0.0:
		return fposmod(rotation_y, 360.0)
	var snapped := round(rotation_y / increment) * increment
	return fposmod(snapped, 360.0)

func _log_warning(message: String) -> void:
	if typeof(ErrorLogger) != TYPE_NIL and ErrorLogger != null and ErrorLogger.has_method("log_warning"):
		ErrorLogger.log_warning(message, "BuildingPiece")
	else:
		push_warning(message)

func _log_error(message: String) -> void:
	if typeof(ErrorLogger) != TYPE_NIL and ErrorLogger != null and ErrorLogger.has_method("log_error"):
		ErrorLogger.log_error(message, "BuildingPiece")
	else:
		push_error(message)
