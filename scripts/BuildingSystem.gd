extends Node
class_name BuildingSystem

signal building_placed(building_id: int, piece: BuildingPiece, position: Vector3)
signal building_removed(building_id: int)
signal stability_updated(building_id: int, stability: float)
signal collapse_triggered(building_ids: Array[int])
signal preview_updated(valid: bool, position: Vector3)
signal build_mode_changed(enabled: bool)
signal selected_piece_changed(piece: BuildingPiece)

enum BuildState { DISABLED, SELECTING, PREVIEWING, PLACING }

const PIECES_DB_PATH := "res://resources/data/building_pieces.json"
const PIECES_DB_VERSION := 1
const GRID_SIZE := 1.0
const FLOOR_HEIGHT := 3.0
const MAX_PLACEMENT_DISTANCE := 10.0
const STABILITY_COLLAPSE_THRESHOLD := 5.0
const MAX_STABILITY_GRAPH_SIZE := 1000
const PREVIEW_VALID_COLOR := Color(0.3, 0.8, 0.3, 0.5)
const PREVIEW_INVALID_COLOR := Color(0.8, 0.3, 0.3, 0.5)
const PLACEMENT_SOUND_PATH := "res://assets/audio/sfx/building/"

var pieces_db: Dictionary = {}
var _piece_defs: Dictionary = {}
var placed_buildings: Dictionary = {}
var stability_graph: Dictionary = {}
var current_state: BuildState = BuildState.DISABLED
var selected_piece: BuildingPiece = null
var selected_tier: BuildingPiece.MaterialTier = BuildingPiece.MaterialTier.WOOD
var preview_mesh: MeshInstance3D = null
var preview_position: Vector3 = Vector3.ZERO
var preview_rotation_y: float = 0.0
var preview_valid: bool = false
var _next_building_id: int = 1
var _player: Node = null
var _voxel_world: Node = null
var _inventory: Node = null
var _audio_manager: Node = null
var _game_manager: Node = null

class PlacedBuilding:
	var building_id: int
	var piece: BuildingPiece
	var position: Vector3
	var rotation_y: float
	var stability: float = 100.0
	var mesh_instance: Node3D = null
	var placed_at: int = 0

	func to_dictionary() -> Dictionary:
		return {
			"building_id": building_id,
			"piece_id": piece.piece_id if piece != null else "",
			"position": {"x": position.x, "y": position.y, "z": position.z},
			"rotation_y": rotation_y,
			"stability": stability,
			"placed_at": placed_at,
		}

class StabilityNode:
	var building_id: int
	var neighbors: Array[int] = []
	var is_foundation: bool = false
	var support_distance: int = 255
	var stability: float = 0.0
	var load_in: float = 0.0

func _ready() -> void:
	pieces_db = {}
	_piece_defs.clear()
	placed_buildings.clear()
	stability_graph.clear()
	_load_pieces_database()
	_create_preview_mesh()
	_lookup_autoloads()
	_connect_game_manager()
	_log_info("BuildingSystem initialized")

func _process(delta: float) -> void:
	if current_state == BuildState.PREVIEWING and _player != null:
		update_preview(_player)

func enter_build_mode() -> void:
	if current_state == BuildState.DISABLED:
		current_state = BuildState.SELECTING
		emit_signal("build_mode_changed", true)
		_log_info("Build mode entered")

func exit_build_mode() -> void:
	if current_state != BuildState.DISABLED:
		current_state = BuildState.DISABLED
		selected_piece = null
		preview_valid = false
		_hide_preview_mesh()
		emit_signal("build_mode_changed", false)
		_log_info("Build mode exited")

func is_in_build_mode() -> bool:
	return current_state != BuildState.DISABLED

func select_piece(piece_id: String, tier: BuildingPiece.MaterialTier) -> bool:
	var piece := get_piece_definition(piece_id)
	if piece == null:
		_log_warning("Piece '%s' not found" % piece_id)
		return false
	selected_piece = piece.duplicate_piece()
	if selected_piece == null:
		selected_piece = piece
	selected_piece.material_tier = tier
	selected_tier = tier
	current_state = BuildState.PREVIEWING
	preview_rotation_y = 0.0
	_update_preview_mesh(selected_piece, false)
	emit_signal("selected_piece_changed", selected_piece)
	_log_info("Selected piece '%s'" % piece_id)
	return true

func rotate_preview(degrees: float) -> void:
	if selected_piece == null:
		return
	preview_rotation_y = _get_rotation_snapped(preview_rotation_y + degrees, selected_piece.rotation_increment_deg)
	if preview_mesh != null:
		preview_mesh.rotation.y = deg_to_rad(preview_rotation_y)

func cycle_material_tier(direction: int) -> void:
	if selected_piece == null:
		return
	var tier := int(selected_tier) + direction
	var max_tier := int(BuildingPiece.MaterialTier.METAL)
	if tier < 0:
		tier = max_tier
	elif tier > max_tier:
		tier = 0
	selected_tier = BuildingPiece.MaterialTier.values()[tier]
	selected_piece.material_tier = selected_tier
	_update_preview_mesh(selected_piece, preview_valid)
	emit_signal("selected_piece_changed", selected_piece)

func update_preview(player: Node) -> void:
	if selected_piece == null or current_state != BuildState.PREVIEWING:
		_hide_preview_mesh()
		return
	var ray := _get_player_raycast(player)
	if ray == null:
		_hide_preview_mesh()
		return
	var collision := ray.get_collider() if ray.has_method("get_collider") else null
	var collision_point := ray.get_collision_point() if ray.has_method("get_collision_point") else Vector3.ZERO
	if collision == null:
		_hide_preview_mesh()
		return
	var snapped := selected_piece.get_snap_position(collision_point, preview_rotation_y)
	snapped.y = _snap_floor_height(snapped.y)
	var rotation_snapped := _get_rotation_snapped(preview_rotation_y, selected_piece.rotation_increment_deg)
	var validation := _validate_placement(snapped, selected_piece, rotation_snapped)
	preview_valid = validation.get("valid", false)
	preview_position = snapped
	preview_rotation_y = rotation_snapped
	if preview_mesh != null:
		preview_mesh.global_position = preview_position
		preview_mesh.rotation.y = deg_to_rad(preview_rotation_y)
		_set_preview_material_color(preview_valid)
		preview_mesh.visible = true
	emit_signal("preview_updated", preview_valid, preview_position)

func try_place_building() -> bool:
	if not preview_valid or selected_piece == null:
		return false
	var piece := selected_piece
	var validation := _validate_placement(preview_position, piece, preview_rotation_y)
	if not validation.get("valid", false):
		return false
	if not _consume_materials(piece):
		return false
	var building := PlacedBuilding.new()
	building.building_id = _next_building_id
	_next_building_id += 1
	building.piece = piece.duplicate_piece() if piece != null else null
	building.position = preview_position
	building.rotation_y = preview_rotation_y
	building.stability = validation.get("stability", 100.0)
	building.placed_at = Time.get_ticks_msec()
	building.mesh_instance = _instantiate_piece_mesh(piece, building)
	placed_buildings[building.building_id] = building
	_add_to_stability_graph(building.building_id, building)
	recalculate_stability(building.building_id)
	_play_placement_sound(piece, building.position)
	emit_signal("building_placed", building.building_id, piece, building.position)
	return true

func remove_building(building_id: int) -> bool:
	if not placed_buildings.has(building_id):
		return false
	var building: PlacedBuilding = placed_buildings[building_id]
	_remove_mesh_instance(building)
	placed_buildings.erase(building_id)
	_remove_from_stability_graph(building_id)
	recalculate_stability_for_neighbors(building_id)
	emit_signal("building_removed", building_id)
	return true

func recalculate_stability(building_id: int) -> void:
	var component := _get_connected_component(building_id)
	_propagate_stability_bfs(component)
	_check_for_collapse(component)

func recalculate_stability_for_neighbors(building_id: int) -> void:
	var neighbors := _get_neighbors(building_id)
	_propagate_stability_bfs(neighbors)
	_check_for_collapse(neighbors)

func get_building_stability(building_id: int) -> float:
	var building: PlacedBuilding = placed_buildings.get(building_id, null)
	if building == null:
		return 0.0
	return building.stability

func get_all_buildings() -> Array:
	var results: Array = []
	for building_id in placed_buildings.keys():
		var building: PlacedBuilding = placed_buildings[building_id]
		if building != null:
			results.append(building)
	return results

func get_piece_definition(piece_id: String) -> BuildingPiece:
	if piece_id == "":
		return null
	if _piece_defs.has(piece_id):
		return _piece_defs[piece_id]
	var entry: Dictionary = pieces_db.get(piece_id, null)
	if entry == null:
		_log_warning("Piece '%s' missing from database" % piece_id)
		return null
	var piece := BuildingPiece.new()
	for key in entry.keys():
		if piece.has_property(key):
			piece.set(key, entry[key])
	piece._validate_configuration()
	_piece_defs[piece_id] = piece
	return piece

func get_available_pieces(category: String = "") -> Array:
	var results: Array = []
	for piece_id in pieces_db.keys():
		var piece := get_piece_definition(piece_id)
		if piece == null:
			continue
		if category != "" and piece.category != category:
			continue
		results.append(piece)
	results.sort_custom(Callable(self, "_compare_pieces"))
	return results

func get_buildings_at_position(position: Vector3, radius: float = 0.5) -> Array[int]:
	var ids: Array[int] = []
	var radius_sq := radius * radius
	for building_id in placed_buildings.keys():
		var building: PlacedBuilding = placed_buildings[building_id]
		if building == null:
			continue
		if building.position.distance_squared_to(position) <= radius_sq:
			ids.append(building_id)
	return ids

func serialize() -> Array:
	var data: Array = []
	for building_id in placed_buildings.keys():
		var building: PlacedBuilding = placed_buildings[building_id]
		if building == null or building.piece == null:
			continue
		data.append(building.to_dictionary())
	return data

func deserialize(data: Array) -> bool:
	placed_buildings.clear()
	stability_graph.clear()
	_next_building_id = 1
	if data.is_empty():
		return true
	for entry in data:
		var piece_id := entry.get("piece_id", "")
		var piece := get_piece_definition(piece_id)
		if piece == null:
			continue
		var building := PlacedBuilding.new()
		building.building_id = int(entry.get("building_id", _next_building_id))
		_next_building_id = max(_next_building_id, building.building_id + 1)
		building.piece = piece
		var pos_dict: Dictionary = entry.get("position", {})
		building.position = Vector3(
			float(pos_dict.get("x", 0.0)),
			float(pos_dict.get("y", 0.0)),
			float(pos_dict.get("z", 0.0))
		)
		building.rotation_y = float(entry.get("rotation_y", 0.0))
		building.stability = float(entry.get("stability", 100.0))
		building.placed_at = int(entry.get("placed_at", Time.get_ticks_msec()))
		building.mesh_instance = _instantiate_piece_mesh(piece, building)
		placed_buildings[building.building_id] = building
		_add_to_stability_graph(building.building_id, building)
	for building_id in placed_buildings.keys():
		recalculate_stability(building_id)
	return true

func place_blueprint(piece_id: String, origin: Vector3, rotation_y: float) -> Dictionary:
	var piece := get_piece_definition(piece_id)
	if piece == null:
		return {"ok": false, "reason": "unknown_piece"}
	var snapped := piece.get_snap_position(origin, rotation_y)
	snapped.y = _snap_floor_height(snapped.y)
	var validation := _validate_placement(snapped, piece, rotation_y)
	if not validation.get("valid", false):
		return {"ok": false, "reason": validation.get("reason", "invalid"), "missing": validation.get("missing", [])}
	var building := PlacedBuilding.new()
	building.building_id = _next_building_id
	_next_building_id += 1
	building.piece = piece
	building.position = snapped
	building.rotation_y = _get_rotation_snapped(rotation_y, piece.rotation_increment_deg)
	building.stability = validation.get("stability", 100.0)
	building.placed_at = Time.get_ticks_msec()
	building.mesh_instance = _instantiate_piece_mesh(piece, building)
	placed_buildings[building.building_id] = building
	_add_to_stability_graph(building.building_id, building)
	recalculate_stability(building.building_id)
	emit_signal("building_placed", building.building_id, piece, building.position)
	return {"ok": true, "building_id": building.building_id}

func get_blueprint_queue() -> Array:
	return []

func queue_blueprint(piece_id: String, position: Vector3, rotation_y: float) -> bool:
	return false

func _validate_placement(position: Vector3, piece: BuildingPiece, rotation_y: float) -> Dictionary:
	var result := {"valid": false, "reason": "unknown"}
	if piece == null:
		result.reason = "missing_piece"
		return result
	if _player == null:
		result.reason = "no_player"
		return result
	if position.distance_to(_player.global_position) > MAX_PLACEMENT_DISTANCE:
		result.reason = "out_of_range"
		return result
	if _check_building_collision(position, piece, rotation_y):
		result.reason = "collision"
		return result
	if not _has_required_materials(piece):
		result.reason = "missing_materials"
		result.missing = _missing_materials(piece)
		return result
	var stability := _calculate_predicted_stability(position, piece)
	if stability < STABILITY_COLLAPSE_THRESHOLD:
		result.reason = "unstable"
		result.stability = stability
		return result
	result.valid = true
	result.reason = "ok"
	result.stability = stability
	return result

func _check_building_collision(position: Vector3, piece: BuildingPiece, rotation_y: float) -> bool:
	var bbox := _calculate_aabb(position, piece, rotation_y)
	for building_id in placed_buildings.keys():
		var other: PlacedBuilding = placed_buildings[building_id]
		if other == null or other.piece == null:
			continue
		var other_bbox := _calculate_aabb(other.position, other.piece, other.rotation_y)
		if bbox.intersects(other_bbox):
			return true
	return false

func _calculate_predicted_stability(position: Vector3, piece: BuildingPiece) -> float:
	if piece.can_be_foundation:
		return 100.0
	var neighbors := _find_adjacent_buildings(position)
	var best_stability := 0.0
	for neighbor_id in neighbors:
		var building: PlacedBuilding = placed_buildings.get(neighbor_id, null)
		if building == null:
			continue
		var neighbor_piece := building.piece
		if neighbor_piece == null:
			continue
		if not piece.can_place_on(neighbor_piece):
			continue
		best_stability = max(best_stability, building.stability * piece.stability_cost)
	return best_stability

func _add_to_stability_graph(building_id: int, building: PlacedBuilding) -> void:
	if placed_buildings.size() > MAX_STABILITY_GRAPH_SIZE:
		_log_warning("Stability graph exceeded max size")
	var node := StabilityNode.new()
	node.building_id = building_id
	node.is_foundation = building.piece.can_be_foundation if building.piece != null else false
	node.stability = building.stability
	var neighbors := _find_adjacent_buildings(building.position)
	for neighbor_id in neighbors:
		node.neighbors.append(neighbor_id)
		var neighbor_node: StabilityNode = stability_graph.get(neighbor_id, null)
		if neighbor_node != null and not neighbor_node.neighbors.has(building_id):
			neighbor_node.neighbors.append(building_id)
	stability_graph[building_id] = node

func _remove_from_stability_graph(building_id: int) -> void:
	if not stability_graph.has(building_id):
		return
	var node: StabilityNode = stability_graph[building_id]
	for neighbor_id in node.neighbors:
		var neighbor_node: StabilityNode = stability_graph.get(neighbor_id, null)
		if neighbor_node != null:
			neighbor_node.neighbors.erase(building_id)
	stability_graph.erase(building_id)

func _propagate_stability_bfs(component_ids: Array[int]) -> void:
	if component_ids.is_empty():
		return
	var queue: Array[int] = []
	var visited: Dictionary = {}
	for building_id in component_ids:
		var node: StabilityNode = stability_graph.get(building_id, null)
		if node == null:
			continue
		node.support_distance = 255
		node.stability = 0.0
		if node.is_foundation:
			node.support_distance = 0
			node.stability = 100.0
			queue.append(building_id)
			visited[building_id] = true
	while not queue.is_empty():
		var current_id := queue.pop_front()
		var current_node: StabilityNode = stability_graph.get(current_id, null)
		if current_node == null:
			continue
		var current_building: PlacedBuilding = placed_buildings.get(current_id, null)
		for neighbor_id in current_node.neighbors:
			var neighbor_node: StabilityNode = stability_graph.get(neighbor_id, null)
			if neighbor_node == null:
				continue
			var neighbor_building: PlacedBuilding = placed_buildings.get(neighbor_id, null)
			if neighbor_building == null or neighbor_building.piece == null:
				continue
			var decay := neighbor_building.piece.stability_cost
			var propagated := current_node.stability * decay
			if propagated > neighbor_node.stability:
				neighbor_node.stability = propagated
				neighbor_node.support_distance = min(current_node.support_distance + 1, neighbor_node.support_distance)
				neighbor_building.stability = neighbor_node.stability
				if not visited.has(neighbor_id):
					queue.append(neighbor_id)
					visited[neighbor_id] = true
	for building_id in component_ids:
		var node: StabilityNode = stability_graph.get(building_id, null)
		if node == null:
			continue
		emit_signal("stability_updated", building_id, node.stability)

func _check_for_collapse(component_ids: Array[int]) -> void:
	var collapsed: Array[int] = []
	for building_id in component_ids:
		var building: PlacedBuilding = placed_buildings.get(building_id, null)
		if building == null:
			continue
		if building.stability < STABILITY_COLLAPSE_THRESHOLD:
			collapsed.append(building_id)
	for building_id in collapsed:
		remove_building(building_id)
	if not collapsed.is_empty():
		emit_signal("collapse_triggered", collapsed)

func _get_connected_component(start_id: int) -> Array[int]:
	var visited: Dictionary = {}
	var queue: Array[int] = []
	queue.append(start_id)
	visited[start_id] = true
	var component: Array[int] = []
	while not queue.is_empty():
		var current := queue.pop_front()
		component.append(current)
		var node: StabilityNode = stability_graph.get(current, null)
		if node == null:
			continue
		for neighbor_id in node.neighbors:
			if not visited.has(neighbor_id):
				visited[neighbor_id] = true
				queue.append(neighbor_id)
	return component

func _get_neighbors(building_id: int) -> Array[int]:
	var node: StabilityNode = stability_graph.get(building_id, null)
	if node == null:
		return []
	return node.neighbors.duplicate()

func _find_adjacent_buildings(position: Vector3) -> Array[int]:
	var neighbors: Array[int] = []
	for building_id in placed_buildings.keys():
		var building: PlacedBuilding = placed_buildings[building_id]
		if building == null:
			continue
		if building.position.distance_to(position) <= (GRID_SIZE * 1.5):
			neighbors.append(building_id)
	return neighbors

func _calculate_aabb(position: Vector3, piece: BuildingPiece, rotation_y: float) -> AABB:
	var half_extents := piece.dimensions * 0.5
	return AABB(position - half_extents, piece.dimensions)

func _consume_materials(piece: BuildingPiece) -> bool:
	if _inventory == null or piece == null:
		return true
	for item_id in piece.crafting_cost.keys():
		var quantity := int(piece.crafting_cost[item_id])
		if not _inventory.has_item(item_id, quantity):
			return false
	for item_id in piece.crafting_cost.keys():
		var quantity := int(piece.crafting_cost[item_id])
		_inventory.remove_item(item_id, quantity)
	return true

func _has_required_materials(piece: BuildingPiece) -> bool:
	if _inventory == null:
		return true
	for item_id in piece.crafting_cost.keys():
		var quantity := int(piece.crafting_cost[item_id])
		if not _inventory.has_item(item_id, quantity):
			return false
	return true

func _missing_materials(piece: BuildingPiece) -> Array:
	var missing: Array = []
	if _inventory == null:
		return missing
	for item_id in piece.crafting_cost.keys():
		var quantity := int(piece.crafting_cost[item_id])
		var available := _inventory.get_item_count(item_id)
		if available < quantity:
			missing.append({
				"item_id": item_id,
				"required": quantity,
				"available": available,
				"missing": quantity - available,
			})
	return missing

func _instantiate_piece_mesh(piece: BuildingPiece, building: PlacedBuilding) -> Node3D:
	var scene := _load_scene(piece.mesh_path)
	if scene == null:
		return null
	var instance := scene.instantiate()
	if instance == null or not instance is Node3D:
		return null
	instance.global_position = building.position
	instance.rotation.y = deg_to_rad(building.rotation_y)
	if get_tree() != null and get_tree().current_scene != null:
		get_tree().current_scene.add_child(instance)
	return instance

func _remove_mesh_instance(building: PlacedBuilding) -> void:
	if building.mesh_instance != null and is_instance_valid(building.mesh_instance):
		building.mesh_instance.queue_free()
		building.mesh_instance = null

func _set_preview_material_color(valid: bool) -> void:
	if preview_mesh == null:
		return
	var material := StandardMaterial3D.new()
	material.albedo_color = PREVIEW_VALID_COLOR if valid else PREVIEW_INVALID_COLOR
	material.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
	preview_mesh.set_surface_override_material(0, material)

func _create_preview_mesh() -> void:
	preview_mesh = MeshInstance3D.new()
	preview_mesh.visible = false
	add_child(preview_mesh)

func _update_preview_mesh(piece: BuildingPiece, valid: bool) -> void:
	if preview_mesh == null or piece == null:
		return
	var scene := _load_scene(piece.mesh_path)
	if scene != null:
		var mesh_instance := scene.instantiate()
		if mesh_instance is MeshInstance3D:
			preview_mesh.mesh = mesh_instance.mesh
		else:
			preview_mesh.mesh = _fallback_box_mesh(piece)
	else:
		preview_mesh.mesh = _fallback_box_mesh(piece)
	_set_preview_material_color(valid)
	preview_mesh.visible = true

func _fallback_box_mesh(piece: BuildingPiece) -> Mesh:
	var box := BoxMesh.new()
	box.size = piece.dimensions
	return box

func _hide_preview_mesh() -> void:
	if preview_mesh != null:
		preview_mesh.visible = false

func _snap_floor_height(value: float) -> float:
	return round(value / FLOOR_HEIGHT) * FLOOR_HEIGHT

func _get_rotation_snapped(rotation_y: float, increment: float) -> float:
	if increment <= 0.0:
		return fposmod(rotation_y, 360.0)
	return fposmod(round(rotation_y / increment) * increment, 360.0)

func _get_player_raycast(player: Node) -> RayCast3D:
	if player == null:
		return null
	if player.has_method("get_interaction_ray"):
		return player.get_interaction_ray()
	if player.has_node("InteractionRay"):
		return player.get_node("InteractionRay")
	return null

func _load_pieces_database() -> void:
	var file := FileAccess.open(PIECES_DB_PATH, FileAccess.READ)
	if file == null:
		_log_error("Failed to open building pieces database at %s" % PIECES_DB_PATH)
		return
	var json := file.get_as_text()
	var parse := JSON.parse_string(json)
	if typeof(parse) != TYPE_DICTIONARY:
		_log_error("Invalid building pieces database format")
		return
	var version := int(parse.get("version", -1))
	if version != PIECES_DB_VERSION:
		_log_warning("Building pieces database version mismatch (expected %d, got %d)" % [PIECES_DB_VERSION, version])
	var pieces := parse.get("pieces", {})
	pieces_db = pieces
	_piece_defs.clear()

func _compare_pieces(a: BuildingPiece, b: BuildingPiece) -> bool:
	if a == null and b == null:
		return false
	if a == null:
		return true
	if b == null:
		return false
	if a.material_tier == b.material_tier:
		if a.piece_type == b.piece_type:
			return a.display_name < b.display_name
		return int(a.piece_type) < int(b.piece_type)
	return int(a.material_tier) < int(b.material_tier)

func _lookup_autoloads() -> void:
	if typeof(GameManager) != TYPE_NIL and GameManager != null:
		_game_manager = GameManager
	if typeof(Inventory) != TYPE_NIL and Inventory != null:
		_inventory = Inventory
	if typeof(AudioManager) != TYPE_NIL and AudioManager != null:
		_audio_manager = AudioManager
	if typeof(VoxelWorld) != TYPE_NIL and VoxelWorld != null:
		_voxel_world = VoxelWorld

func _connect_game_manager() -> void:
	if _game_manager != null and _game_manager.has_signal("player_registered"):
		if not _game_manager.is_connected("player_registered", Callable(self, "_on_player_registered")):
			_game_manager.player_registered.connect(_on_player_registered)

func _on_player_registered(player: Node) -> void:
	_player = player

func _play_placement_sound(piece: BuildingPiece, position: Vector3) -> void:
	if _audio_manager == null:
		return
	var tier_name := piece.get_material_tier_name().to_lower()
	var path := "%s%s_place.ogg" % [PLACEMENT_SOUND_PATH, tier_name]
	if _audio_manager.has_method("play_sfx_3d"):
		_audio_manager.play_sfx_3d(path, position)

func _load_scene(path: String) -> PackedScene:
	if path.is_empty():
		return null
	if not ResourceLoader.exists(path):
		_log_warning("Scene path '%s' missing for building piece" % path)
		return null
	var resource := ResourceLoader.load(path)
	if resource is PackedScene:
		return resource
	return null

func _calculate_aabb_centered(position: Vector3, dimensions: Vector3) -> AABB:
	return AABB(position - dimensions * 0.5, dimensions)

func _log_info(message: String) -> void:
	if typeof(ErrorLogger) != TYPE_NIL and ErrorLogger != null and ErrorLogger.has_method("log_info"):
		ErrorLogger.log_info(message, "BuildingSystem")

func _log_warning(message: String) -> void:
	if typeof(ErrorLogger) != TYPE_NIL and ErrorLogger != null and ErrorLogger.has_method("log_warning"):
		ErrorLogger.log_warning(message, "BuildingSystem")

func _log_error(message: String) -> void:
	if typeof(ErrorLogger) != TYPE_NIL and ErrorLogger != null and ErrorLogger.has_method("log_error"):
		ErrorLogger.log_error(message, "BuildingSystem")
