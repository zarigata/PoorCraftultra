extends Node3D
class_name VoxelWorld

signal terrain_ready()
signal meshing_error(error_message: String)
signal chunk_loaded(chunk_position: Vector3i)
signal chunk_unloaded(chunk_position: Vector3i)
signal biome_detected(position: Vector3, biome_id: String)

const CONFIG_PATH := "res://voxel/default_terrain_config.json"
const VOXEL_TYPES_PATH := "res://voxel/voxel_types.json"
const STATS_UPDATE_INTERVAL := 0.5
const MATERIAL_PATH := "res://resources/voxel_flat_material.tres"

var terrain: VoxelLodTerrain
var viewer: VoxelViewer
var generator
var noise: FastNoiseLite
var tracked_camera: Camera3D = null

var world_seed: int = 0
var config: Dictionary = {}
var voxel_types: Dictionary = {}
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
	if terrain and terrain.has_method("get_voxel_tool"):
		var tool := terrain.get_voxel_tool()
		tool.channel = VoxelBuffer.CHANNEL_SDF
		return tool.get_voxel_f(position)
	return 0.0

func get_voxel_type_at(position: Vector3) -> Dictionary:
	"""Returns the voxel type data at the given position.
	Currently returns a placeholder based on SDF value.
	Future: Read from material channel or custom data."""
	var sdf := get_voxel_at(position)
	if sdf > 0.0:
		return voxel_types.get("air", {})
	elif sdf > -10.0:
		return voxel_types.get("grass", {})
	elif sdf > -50.0:
		return voxel_types.get("dirt", {})
	else:
		return voxel_types.get("stone", {})

func get_material_id_at(position: Vector3) -> int:
	"""Returns the material ID at the given position."""
	var voxel_type := get_voxel_type_at(position)
	return voxel_type.get("material_id", 0)

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
	ErrorLogger.log_debug("set_voxel_at stub invoked", "VoxelWorld")

func serialize_state() -> Dictionary:
	return {
		"seed": world_seed,
		"edits": [],
		"config": config,
	}

func deserialize_state(data: Dictionary) -> void:
	config = data.get("config", config)
	var new_seed := data.get("seed", world_seed)
	set_seed(new_seed)
	_apply_config()
	ErrorLogger.log_info("Voxel world state deserialized with seed %d" % new_seed, "VoxelWorld")

func queue_edit(center: Vector3, radius: float, delta: float) -> void:
	ErrorLogger.log_debug("queue_edit stub invoked", "VoxelWorld")

func get_surface_height(xz: Vector2) -> float:
	ErrorLogger.log_debug("get_surface_height stub invoked", "VoxelWorld")
	return 0.0

func add_edit_listener(callback: Callable) -> void:
	ErrorLogger.log_debug("add_edit_listener stub invoked", "VoxelWorld")

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
