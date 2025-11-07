extends Node3D

const PLAYER_NAME := "Player"
const CAMERA_NAME := "MainCamera"
const SPAWN_NODE_NAME := "PlayerSpawnPoint"
const SUN_NODE_NAME := "Sun"
const WORLD_ENVIRONMENT_NODE_NAME := "WorldEnvironment"
const VOXEL_WORLD_SCENE := preload("res://scenes/voxel_world.tscn")
const WORLD_CONTAINER_NAME := "WorldContainer"

func _ready() -> void:
    await _initialize_voxel_world()
    _initialize_environment()
    _ensure_camera_exists()
    _ensure_player_spawned()
    _attach_viewer_to_camera()
    _attach_environment_to_camera()

func _ensure_player_spawned() -> void:
    var world := GameManager.get_current_world()
    if world == null:
        return
    if get_node_or_null(PLAYER_NAME) != null:
        return
    var spawn_point := get_node_or_null(SPAWN_NODE_NAME)
    if spawn_point == null or not (spawn_point is Node3D):
        return
    var player := Node3D.new()
    player.name = PLAYER_NAME
    add_child(player)
    player.global_transform = spawn_point.global_transform

func _initialize_voxel_world() -> void:
    var container := get_node_or_null(WORLD_CONTAINER_NAME)
    if container == null:
        ErrorLogger.log_error("WorldContainer node not found in main scene", "Main")
        return

    if container.get_child_count() > 0:
        ErrorLogger.log_debug("Voxel world already exists, skipping initialization", "Main")
        return

    var world_instance := VOXEL_WORLD_SCENE.instantiate()
    if world_instance == null:
        ErrorLogger.log_critical("Failed to instantiate voxel world scene", "Main")
        return

    container.add_child(world_instance)
    ErrorLogger.log_info("Voxel world initialized", "Main")

    if world_instance.has_signal("terrain_ready"):
        await world_instance.terrain_ready
        ErrorLogger.log_debug("Terrain ready signal received", "Main")

func _initialize_environment() -> void:
    var sun := get_node_or_null(SUN_NODE_NAME)
    var world_env := get_node_or_null(WORLD_ENVIRONMENT_NODE_NAME)

    if sun == null or not (sun is DirectionalLight3D):
        ErrorLogger.log_error("Sun node not found or invalid type", "Main")
        return

    if world_env == null or not (world_env is WorldEnvironment):
        ErrorLogger.log_error("WorldEnvironment node not found or invalid type", "Main")
        return

    if typeof(EnvironmentManager) == TYPE_NIL or EnvironmentManager == null or not EnvironmentManager.has_method("initialize"):
        ErrorLogger.log_error("EnvironmentManager not available", "Main")
        return

    var initialized := EnvironmentManager.initialize(sun, world_env)
    if initialized:
        ErrorLogger.log_info("Environment system initialized", "Main")
    else:
        ErrorLogger.log_error("Environment system failed to initialize", "Main")

func _ensure_camera_exists() -> void:
    if get_node_or_null(CAMERA_NAME) != null:
        return
    
    var spawn_point := get_node_or_null(SPAWN_NODE_NAME)
    if spawn_point == null:
        ErrorLogger.log_warning("No spawn point found for camera placement", "Main")
        return
    
    var camera := Camera3D.new()
    camera.name = CAMERA_NAME
    camera.current = true
    add_child(camera)
    camera.global_position = spawn_point.global_position
    ErrorLogger.log_info("Main camera created at spawn point", "Main")

func _attach_viewer_to_camera() -> void:
    var world := GameManager.get_current_world()
    if world == null or not world.has_method("set_tracked_camera"):
        return
    
    var camera := get_node_or_null(CAMERA_NAME)
    if camera == null or not (camera is Camera3D):
        ErrorLogger.log_warning("No camera available to attach viewer", "Main")
        return
    
    world.set_tracked_camera(camera)
    ErrorLogger.log_debug("VoxelViewer attached to camera", "Main")

func _attach_environment_to_camera() -> void:
    var camera := get_node_or_null(CAMERA_NAME)
    if camera == null or not (camera is Camera3D):
        ErrorLogger.log_warning("No camera available for environment tracking", "Main")
        return

    if typeof(EnvironmentManager) != TYPE_NIL and EnvironmentManager != null and EnvironmentManager.has_method("set_tracked_camera"):
        EnvironmentManager.set_tracked_camera(camera)
        ErrorLogger.log_debug("Environment tracking camera", "Main")
