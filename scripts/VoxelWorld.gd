extends Node3D
class_name VoxelWorld

signal terrain_ready()
signal meshing_error(error_message: String)
signal chunk_loaded(chunk_position: Vector3i)
signal chunk_unloaded(chunk_position: Vector3i)
signal biome_detected(position: Vector3, biome_id: String)
signal voxel_edited(edit: Dictionary)

const CONFIG_PATH := "res://voxel/default_terrain_config.json"
const VOXEL_TYPES_PATH := "res://voxel/voxel_types.json"
const STATS_UPDATE_INTERVAL := 0.5
const MATERIAL_PATH := "res://resources/voxel_flat_material.tres"
const BOUNDS_MIN := Vector3(-10000.0, -10000.0, -10000.0)
const BOUNDS_MAX := Vector3(10000.0, 10000.0, 10000.0)

var terrain: VoxelLodTerrain
var viewer: VoxelViewer
var generator
var noise: FastNoiseLite
var tracked_camera: Camera3D = null

var voxel_tool: VoxelTool = null
var edit_mutex: Mutex = Mutex.new()

var world_seed: int = 0
var config: Dictionary = {}
var voxel_types: Dictionary = {}
var voxel_types_by_id: Dictionary = {}
var voxel_types_by_material_id: Dictionary = {}
var is_initialized: bool = false
var last_detected_biome: String = ""
var _stats_timer: float = 0.0
var _last_loaded_blocks: int = 0
var _last_meshing_drop_counts := {
	"dropped_block_meshes": 0,
	"dropped_block_loads": 0,
}
const MESHER_DROP_THRESHOLD := 5

var stats := {
	"loaded_chunks": 0,
	"meshing_blocks": 0,
	"streaming_blocks": 0,
	"memory_usage_mb": 0.0,
	"dropped_block_meshes": 0,
	"dropped_block_loads": 0,
	"recent_mesher_drop_events": 0,
}

func _ready() -> void:
	ErrorLogger.log_info("VoxelWorld initializing", "VoxelWorld")
	_load_config()
	_load_voxel_types()
	_setup_nodes()
	_apply_config()
	_create_generator()
	_register_with_game_manager()
	is_initialized = true
	_update_stats()
	emit_signal("terrain_ready")

func _process(delta: float) -> void:
	_sync_viewer_to_camera()
	_stats_timer += delta
	if _stats_timer >= STATS_UPDATE_INTERVAL:
		_stats_timer = 0.0
		_update_stats()

func _setup_nodes() -> void:
    terrain = get_node_or_null("Terrain")
    if terrain == null:
        terrain = VoxelLodTerrain.new()
        terrain.name = "Terrain"
        add_child(terrain)
        ErrorLogger.log_warning("VoxelLodTerrain node missing from scene. Created dynamically.", "VoxelWorld")

    viewer = get_node_or_null("Viewer")
    if viewer == null:
        viewer = VoxelViewer.new()
        viewer.name = "Viewer"
        add_child(viewer)
        ErrorLogger.log_warning("VoxelViewer node missing from scene. Created dynamically.", "VoxelWorld")

    if terrain != null and terrain.has_method("get_voxel_tool"):
        voxel_tool = terrain.get_voxel_tool()
        if voxel_tool != null:
            voxel_tool.channel = VoxelBuffer.CHANNEL_SDF
            ErrorLogger.log_info("VoxelTool initialized for editing", "VoxelWorld")
        else:
            ErrorLogger.log_error("VoxelTool not available from terrain", "VoxelWorld")
    else:
        ErrorLogger.log_error("Terrain missing get_voxel_tool method; edits disabled", "VoxelWorld")

func _create_generator() -> void:
    noise = FastNoiseLite.new()
    generator = VoxelGeneratorNoise.new()
    if noise == null or generator == null:
        ErrorLogger.log_error("Failed to allocate noise or generator", "VoxelWorld")
        return

    var noise_config := config.get("noise", {})
    world_seed = config.get("seed", 0)
    noise.seed = world_seed
    noise.frequency = noise_config.get("frequency", 0.01)
    noise.fractal_octaves = noise_config.get("fractal_octaves", 5)
    noise.fractal_lacunarity = noise_config.get("fractal_lacunarity", 2.0)
    noise.fractal_gain = noise_config.get("fractal_gain", 0.5)
    noise.noise_type = _get_noise_type(noise_config.get("type", "OpenSimplex2S"))
    noise.fractal_type = _get_fractal_type(noise_config.get("fractal_type", "FBM"))

    generator.noise = noise
    var generator_config := config.get("generator", {})
    generator.channel = VoxelBuffer.CHANNEL_SDF
    generator.height_start = generator_config.get("height_start", -100.0)
    generator.height_range = generator_config.get("height_range", 300.0)

    if terrain:
        terrain.generator = generator
        if terrain.mesher == null:
            terrain.mesher = VoxelMesherTransvoxel.new()
        var material := load(MATERIAL_PATH)
        if material:
            terrain.material_override = material
        else:
            ErrorLogger.log_warning("Failed to load voxel material at %s" % MATERIAL_PATH, "VoxelWorld")

func _apply_config() -> void:
	var terrain_config := config.get("terrain", {})
	var performance_config := config.get("performance", {})

	if terrain:
		var requested_block_size := terrain_config.get("mesh_block_size", terrain_config.get("chunk_size", 32))
		if requested_block_size != 16 and requested_block_size != 32:
			ErrorLogger.log_warning("Invalid mesh block size %s. Falling back to 32." % requested_block_size, "VoxelWorld")
			requested_block_size = 32
		terrain.mesh_block_size = requested_block_size
		terrain.lod_count = terrain_config.get("lod_count", 4)
		terrain.lod_distance = terrain_config.get("lod_distance", 48.0)
		terrain.view_distance = terrain_config.get("view_distance", 512.0)
		terrain.full_load_mode = performance_config.get("full_load_mode", false)
		terrain.threaded_update = performance_config.get("threaded_update_enabled", true)
		terrain.collision_enabled = terrain_config.get("collision_enabled", true)
		terrain.collision_lod_count = terrain_config.get("collision_lod_count", 2)

	if viewer:
		viewer.requires_collisions = true
		viewer.requires_meshes = true
		viewer.view_distance = terrain_config.get("view_distance", 512.0)

	if performance_config.has("max_concurrent_generations") and terrain and terrain.has_method("set_max_active_block_updates"):
		terrain.set_max_active_block_updates(performance_config.get("max_concurrent_generations"))

func _register_with_game_manager() -> void:
	if typeof(GameManager) != TYPE_NIL and GameManager != null and GameManager.has_method("set_current_world"):
		GameManager.set_current_world(self)
	else:
		ErrorLogger.log_warning("GameManager not available for world registration", "VoxelWorld")

func _sync_viewer_to_camera() -> void:
	"""Syncs the VoxelViewer position to the tracked camera."""
	if viewer == null:
		return
	
	# Auto-detect camera if not set
	if tracked_camera == null:
		tracked_camera = get_viewport().get_camera_3d()
	
	if tracked_camera != null:
		viewer.global_position = tracked_camera.global_position

func set_tracked_camera(camera: Camera3D) -> void:
	"""Sets the camera that the VoxelViewer should follow."""
	tracked_camera = camera
	ErrorLogger.log_info("VoxelViewer now tracking camera: %s" % camera.name, "VoxelWorld")

func get_tracked_camera() -> Camera3D:
	"""Returns the currently tracked camera."""
	return tracked_camera

func _load_config() -> void:
	config = {}
	var file := FileAccess.open(CONFIG_PATH, FileAccess.READ)
	if file == null:
		ErrorLogger.log_warning("Failed to open terrain config at %s" % CONFIG_PATH, "VoxelWorld")
		config = _get_default_config()
		return
	var text := file.get_as_text()
	file.close()
	var parsed := JSON.parse_string(text)
	if typeof(parsed) != TYPE_DICTIONARY:
		ErrorLogger.log_error("Invalid JSON format in terrain config", "VoxelWorld")
		config = _get_default_config()
	else:
		config = parsed

func _get_default_config() -> Dictionary:
	return {
		"version": 1,
		"seed": 0,
		"noise": {},
		"generator": {},
		"terrain": {},
		"performance": {},
	}

func _load_voxel_types() -> void:
	voxel_types = {}
	voxel_types_by_id.clear()
	voxel_types_by_material_id.clear()
	var file := FileAccess.open(VOXEL_TYPES_PATH, FileAccess.READ)
	if file == null:
		ErrorLogger.log_warning("Failed to open voxel types at %s" % VOXEL_TYPES_PATH, "VoxelWorld")
		return
	var text := file.get_as_text()
	file.close()
	var parsed := JSON.parse_string(text)
	if typeof(parsed) != TYPE_DICTIONARY:
		ErrorLogger.log_error("Invalid JSON format in voxel types", "VoxelWorld")
		return
	voxel_types = parsed.get("types", {})
	for type_key in voxel_types.keys():
		var type_data := voxel_types[type_key]
		if typeof(type_data) != TYPE_DICTIONARY:
			continue
		var type_id := int(type_data.get("id", -1))
		if type_id >= 0:
			voxel_types_by_id[type_id] = type_data
		var material_id := int(type_data.get("material_id", -1))
		if material_id >= 0:
			voxel_types_by_material_id[material_id] = type_data
	ErrorLogger.log_info("Loaded %d voxel types" % voxel_types.size(), "VoxelWorld")

func _update_stats() -> void:
	if terrain == null:
		return

	var loaded_blocks := terrain.debug_get_mesh_block_count()
	stats["loaded_chunks"] = loaded_blocks

	var terrain_stats := terrain.get_statistics() if terrain.has_method("get_statistics") else {}

	var remaining_main := terrain_stats.get("remaining_main_thread_blocks", 0)
	var remaining_stream := terrain_stats.get("remaining_streaming_blocks", 0)
	var loading_blocks := terrain_stats.get("loading_blocks", 0)
	var streaming_updates := terrain_stats.get("streaming_tasks", 0)

	stats["meshing_blocks"] = max(remaining_main, loading_blocks)
	stats["streaming_blocks"] = max(remaining_stream, streaming_updates)
	stats["memory_usage_mb"] = _estimate_memory_usage_mb(loaded_blocks)

	_monitor_block_events(loaded_blocks)
	_monitor_meshing_errors(terrain_stats)

func _estimate_memory_usage_mb(loaded_blocks: int) -> float:
	var bytes_per_block := 256 * 1024 # Rough heuristic (~256 KB per block)
	return float(loaded_blocks * bytes_per_block) / (1024.0 * 1024.0)

func _monitor_block_events(current_loaded: int) -> void:
	if current_loaded == _last_loaded_blocks:
		return
	if current_loaded > _last_loaded_blocks:
		var diff := current_loaded - _last_loaded_blocks
		ErrorLogger.log_debug("Mesh block count increased by %d" % diff, "VoxelWorld")
		# Emit chunk_loaded signals for newly loaded chunks
		# Note: Exact positions are not available from mesh block count alone,
		# so we emit a generic signal with zero position as a placeholder
		for i in range(diff):
			emit_signal("chunk_loaded", Vector3i.ZERO)
	else:
		var diff := _last_loaded_blocks - current_loaded
		ErrorLogger.log_debug("Mesh block count decreased by %d" % diff, "VoxelWorld")
		# Emit chunk_unloaded signals for unloaded chunks
		for i in range(diff):
			emit_signal("chunk_unloaded", Vector3i.ZERO)
	_last_loaded_blocks = current_loaded

func _monitor_meshing_errors(terrain_stats: Dictionary) -> void:
	var dropped_meshes := terrain_stats.get("dropped_block_meshes", 0)
	var dropped_loads := terrain_stats.get("dropped_block_loads", 0)
	var new_mesh_drops := dropped_meshes - _last_meshing_drop_counts["dropped_block_meshes"]
	var new_load_drops := dropped_loads - _last_meshing_drop_counts["dropped_block_loads"]
	_last_meshing_drop_counts["dropped_block_meshes"] = dropped_meshes
	_last_meshing_drop_counts["dropped_block_loads"] = dropped_loads

	var total_new_drops := max(new_mesh_drops, 0) + max(new_load_drops, 0)
	if total_new_drops >= MESHER_DROP_THRESHOLD:
		var message := "Voxel mesher dropped %d blocks recently (meshes: %d, loads: %d)" % [total_new_drops, max(new_mesh_drops, 0), max(new_load_drops, 0)]
		ErrorLogger.log_error(message, "VoxelWorld")
		emit_signal("meshing_error", message)

func get_seed() -> int:
	return world_seed

func set_seed(new_seed: int) -> void:
	world_seed = new_seed
	if noise:
		noise.seed = new_seed
	regenerate_terrain()

func regenerate_terrain() -> void:
	if terrain and terrain.has_method("invalidate_lod"):
		terrain.invalidate_lod()
		ErrorLogger.log_info("Terrain regeneration requested", "VoxelWorld")

func get_stats() -> Dictionary:
	return stats.duplicate(true)

func get_voxel_at(position: Vector3) -> float:
	var tool := _ensure_voxel_tool()
	if tool == null:
		return 0.0
	edit_mutex.lock()
	var previous_channel := tool.channel
	tool.channel = VoxelBuffer.CHANNEL_SDF
	var value := tool.get_voxel_f(position)
	tool.channel = previous_channel
	edit_mutex.unlock()
	return value

func get_voxel_type_at(position: Vector3) -> Dictionary:
	"""Returns voxel type data at the given position.
	Prioritizes material and type channels, falling back to SDF heuristics as a last resort."""
	var material_id := get_material_id_at(position)
	if material_id >= 0 and voxel_types_by_material_id.has(material_id):
		return voxel_types_by_material_id[material_id]
	var channel_type_id := _read_voxel_int(position, VoxelBuffer.CHANNEL_TYPE)
	if channel_type_id >= 0 and voxel_types_by_id.has(channel_type_id):
		return voxel_types_by_id[channel_type_id]
	var sdf := get_voxel_at(position)
	if sdf > 0.0:
		return voxel_types.get("air", {})
	elif sdf > -10.0:
		return voxel_types.get("grass", {})
	elif sdf > -50.0:
		return voxel_types.get("dirt", {})
	return voxel_types.get("stone", {})

func get_material_id_at(position: Vector3) -> int:
	"""Returns the material ID at the given position using the voxel material channel."""
	return _read_voxel_int(position, VoxelBuffer.CHANNEL_MATERIAL)

func get_voxel_type_by_name(type_name: String) -> Dictionary:
	"""Returns voxel type data by name."""
	return voxel_types.get(type_name, {})

func get_all_voxel_types() -> Dictionary:
	"""Returns all registered voxel types."""
	return voxel_types.duplicate(true)

func get_biome_at(position: Vector3) -> String:
	"""Returns the biome ID at the given world position.
	Currently returns a placeholder based on height.
	Future: Implement proper biome detection using noise layers or biome map."""

	var y := position.y
	if y > 100.0:
		return "storm_peaks"
	elif y > 50.0:
		return "verdant_woods"
	elif y > 0.0:
		return "amber_dunes"
	else:
		return "mirelow"

	# TODO: Replace with proper biome detection system.
	# - Use dedicated biome noise layers or textures
	# - Incorporate temperature/moisture gradients
	# - Cache results for performance

func check_biome_at_position(position: Vector3) -> void:
	"""Checks biome at position and emits signal if changed.
	Should be called periodically by the player controller."""
	var biome := get_biome_at(position)
	if biome != last_detected_biome:
		last_detected_biome = biome
		emit_signal("biome_detected", position, biome)
		ErrorLogger.log_debug("Biome changed to: %s" % biome, "VoxelWorld")
		if typeof(EnvironmentManager) != TYPE_NIL and EnvironmentManager != null and EnvironmentManager.has_method("set_biome"):
			EnvironmentManager.set_biome(biome)

func set_voxel_at(position: Vector3, value: float) -> void:
    if voxel_tool == null:
        ErrorLogger.log_warning("VoxelTool not available for set_voxel_at", "VoxelWorld")
        return

    edit_mutex.lock()
    voxel_tool.set_voxel_f(position, value)
    edit_mutex.unlock()

func serialize_state() -> Dictionary:
    return {
        "seed": world_seed,
        "edits": _serialize_edits(),
        "config": config,
    }

func deserialize_state(data: Dictionary) -> void:
    config = data.get("config", config)
    var new_seed := data.get("seed", world_seed)
    set_seed(new_seed)
    _apply_config()
    var edits := data.get("edits", [])
    for edit in edits:
        apply_edit(edit)
    ErrorLogger.log_info("Voxel world state deserialized with seed %d" % new_seed, "VoxelWorld")

func queue_edit(center: Vector3, radius: float, delta: float) -> void:
    var operation := delta < 0.0 ? 0 : 1
    var edit := {
        "center": center,
        "radius": radius,
        "operation": operation,
        "value": delta,
        "timestamp": Time.get_ticks_msec(),
    }
    apply_edit(edit)

func get_surface_height(xz: Vector2) -> float:
	ErrorLogger.log_debug("get_surface_height stub invoked", "VoxelWorld")
	return 0.0

func add_edit_listener(callback: Callable) -> void:
    ErrorLogger.log_debug("add_edit_listener stub invoked", "VoxelWorld")

func apply_edit(edit: Dictionary) -> bool:
    if voxel_tool == null:
        ErrorLogger.log_error("VoxelTool not initialized", "VoxelWorld")
        return false

    var center: Vector3 = edit.get("center", Vector3.ZERO)
    var radius: float = float(edit.get("radius", 1.0))
    var operation: int = int(edit.get("operation", 0))
    var value: float = float(edit.get("value", 0.0))

    if radius <= 0.0 or radius > 5.0:
        ErrorLogger.log_warning("Invalid edit radius: %.2f" % radius, "VoxelWorld")
        return false
    if not _is_within_bounds(center):
        ErrorLogger.log_warning("Edit center out of bounds: %s" % center, "VoxelWorld")
        return false

    edit_mutex.lock()
    var previous_mode := voxel_tool.mode
    var previous_scale := voxel_tool.sdf_scale

    match operation:
        0:
            voxel_tool.mode = VoxelTool.MODE_REMOVE
            voxel_tool.do_sphere(center, radius)
        1:
            voxel_tool.mode = VoxelTool.MODE_ADD
            voxel_tool.do_sphere(center, radius)
        2:
            voxel_tool.sdf_scale = value
            voxel_tool.mode = VoxelTool.MODE_ADD if value >= 0.0 else VoxelTool.MODE_REMOVE
            voxel_tool.do_sphere(center, radius)
        _:
            voxel_tool.mode = previous_mode
            voxel_tool.sdf_scale = previous_scale
            edit_mutex.unlock()
            ErrorLogger.log_warning("Unknown edit operation: %s" % operation, "VoxelWorld")
            return false

    voxel_tool.mode = previous_mode
    voxel_tool.sdf_scale = previous_scale
    edit_mutex.unlock()

    emit_signal("voxel_edited", edit)
    ErrorLogger.log_debug("Voxel edit applied at %s radius %.2f" % [center, radius], "VoxelWorld")
    return true

func capture_voxel_snapshot(center: Vector3, radius: float) -> Dictionary:
	var tool := _ensure_voxel_tool()
	if tool == null:
		return {}

	var snapshot := {}
	var radius_int := int(ceil(radius))
	var radius_squared := radius * radius
	var min_bound := Vector3i(int(floor(center.x)) - radius_int, int(floor(center.y)) - radius_int, int(floor(center.z)) - radius_int)
	var max_bound := Vector3i(int(floor(center.x)) + radius_int, int(floor(center.y)) + radius_int, int(floor(center.z)) + radius_int)

	edit_mutex.lock()
	var previous_channel := tool.channel
	tool.channel = VoxelBuffer.CHANNEL_SDF
	for x in range(min_bound.x, max_bound.x + 1):
		for y in range(min_bound.y, max_bound.y + 1):
			for z in range(min_bound.z, max_bound.z + 1):
				var grid_pos := Vector3i(x, y, z)
				var sample_center := Vector3(x, y, z) + Vector3.ONE * 0.5
				if sample_center.distance_squared_to(center) > radius_squared:
					continue
				if snapshot.has(grid_pos):
					continue
				snapshot[grid_pos] = tool.get_voxel_f(Vector3(x, y, z))
	tool.channel = previous_channel
	edit_mutex.unlock()

	ErrorLogger.log_debug("Captured snapshot of %d voxels" % snapshot.size(), "VoxelWorld")
	return snapshot

func restore_voxel_snapshot(snapshot: Dictionary) -> bool:
	var tool := _ensure_voxel_tool()
	if tool == null or snapshot.is_empty():
		return false

	edit_mutex.lock()
	var previous_channel := tool.channel
	tool.channel = VoxelBuffer.CHANNEL_SDF
	for pos in snapshot.keys():
		var grid_pos := pos if pos is Vector3i else Vector3i(int(round(pos.x)), int(round(pos.y)), int(round(pos.z)))
		var value := float(snapshot[pos])
		tool.set_voxel_f(Vector3(grid_pos), value)
	tool.channel = previous_channel
	edit_mutex.unlock()

	ErrorLogger.log_debug("Restored snapshot of %d voxels" % snapshot.size(), "VoxelWorld")
	return true

func _read_voxel_int(position: Vector3, channel: int, default_value: int = -1) -> int:
	var tool := _ensure_voxel_tool()
	if tool == null:
		return default_value
	edit_mutex.lock()
	var previous_channel := tool.channel
	tool.channel = channel
	var grid_pos := Vector3i(int(floor(position.x)), int(floor(position.y)), int(floor(position.z)))
	var value := tool.get_voxel(grid_pos)
	tool.channel = previous_channel
	edit_mutex.unlock()
	return int(value)

func _ensure_voxel_tool() -> VoxelTool:
	if voxel_tool != null:
		return voxel_tool
	if terrain != null and terrain.has_method("get_voxel_tool"):
		voxel_tool = terrain.get_voxel_tool()
		if voxel_tool != null:
			voxel_tool.channel = VoxelBuffer.CHANNEL_SDF
			ErrorLogger.log_debug("VoxelTool reinitialized", "VoxelWorld")
	return voxel_tool

func _is_within_bounds(position: Vector3) -> bool:
    return position.x >= BOUNDS_MIN.x and position.x <= BOUNDS_MAX.x and position.y >= BOUNDS_MIN.y and position.y <= BOUNDS_MAX.y and position.z >= BOUNDS_MIN.z and position.z <= BOUNDS_MAX.z

func _serialize_edits() -> Array:
    # Placeholder for future serialization integration
    return []

func _get_noise_type(name: String) -> int:
	match name:
		"OpenSimplex2":
			return FastNoiseLite.TYPE_OPENSIMPLEX2
		"OpenSimplex2S":
			return FastNoiseLite.TYPE_OPENSIMPLEX2S
		"Cellular":
			return FastNoiseLite.TYPE_CELLULAR
		"Perlin":
			return FastNoiseLite.TYPE_PERLIN
		"Value":
			return FastNoiseLite.TYPE_VALUE
		"ValueCubic":
			return FastNoiseLite.TYPE_VALUE_CUBIC
		_:
			return FastNoiseLite.TYPE_OPENSIMPLEX2S

func _get_fractal_type(name: String) -> int:
	match name:
		"FBM":
			return FastNoiseLite.FRACTAL_FBM
		"Ridged":
			return FastNoiseLite.FRACTAL_RIDGED
		"PingPong":
			return FastNoiseLite.FRACTAL_PING_PONG
		"DomainWarpProgressive":
			return FastNoiseLite.FRACTAL_DOMAIN_WARP_PROGRESSIVE
		"DomainWarpIndependent":
			return FastNoiseLite.FRACTAL_DOMAIN_WARP_INDEPENDENT
		_:
			return FastNoiseLite.FRACTAL_FBM
