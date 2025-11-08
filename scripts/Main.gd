extends Node3D

const PLAYER_NAME := "Player"
const SPAWN_NODE_NAME := "PlayerSpawnPoint"
const SUN_NODE_NAME := "Sun"
const WORLD_ENVIRONMENT_NODE_NAME := "WorldEnvironment"
const VOXEL_WORLD_SCENE := preload("res://scenes/voxel_world.tscn")
const WORLD_CONTAINER_NAME := "WorldContainer"
const PLAYER_SCENE := preload("res://scenes/player.tscn")

func _ready() -> void:
    _connect_player_registered_signal()
    await _initialize_voxel_world()
    _initialize_environment()
    _ensure_player_spawned()
    _attach_viewer_to_camera()
    _attach_environment_to_camera()

func _ensure_player_spawned() -> void:
    var world := GameManager.get_current_world()
    if world == null:
        ErrorLogger.log_warning("Cannot spawn player: voxel world not initialized", "Main")
        return
    if get_node_or_null(PLAYER_NAME) != null:
        ErrorLogger.log_debug("Player already exists, skipping spawn", "Main")
        return
    var spawn_point := get_node_or_null(SPAWN_NODE_NAME)
    if spawn_point == null or not (spawn_point is Node3D):
        ErrorLogger.log_error("PlayerSpawnPoint node not found or invalid type", "Main")
        return
    var player_instance := PLAYER_SCENE.instantiate()
    if player_instance == null:
        ErrorLogger.log_critical("Failed to instantiate player scene", "Main")
        return

    player_instance.name = PLAYER_NAME
    add_child(player_instance)
    player_instance.global_position = spawn_point.global_position
    ErrorLogger.log_info("Player spawned at %s" % spawn_point.global_position, "Main")

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

func _connect_player_registered_signal() -> void:
    if typeof(GameManager) == TYPE_NIL or GameManager == null:
        ErrorLogger.log_error("GameManager not available for player registration signal", "Main")
        return
    if not GameManager.player_registered.is_connected(_on_player_registered):
        GameManager.player_registered.connect(_on_player_registered)

func _attach_viewer_to_camera() -> void:
    var world := GameManager.get_current_world()
    if world == null or not world.has_method("set_tracked_camera"):
        ErrorLogger.log_warning("VoxelWorld not available for viewer attachment", "Main")
        return
    var player := GameManager.get_player()
    if player == null or not player.has_method("get_camera"):
        ErrorLogger.log_warning("No player available for viewer attachment", "Main")
        return
    var camera := player.get_camera()
    if camera == null or not (camera is Camera3D):
        ErrorLogger.log_warning("Player camera not found", "Main")
        return
    world.set_tracked_camera(camera)
    ErrorLogger.log_debug("VoxelViewer attached to player camera", "Main")

func _attach_environment_to_camera() -> void:
    var player := GameManager.get_player()
    if player == null or not player.has_method("get_camera"):
        ErrorLogger.log_warning("No player available for environment tracking", "Main")
        return
    var camera := player.get_camera()
    if camera == null or not (camera is Camera3D):
        ErrorLogger.log_warning("Player camera not found for environment tracking", "Main")
        return

    if typeof(EnvironmentManager) != TYPE_NIL and EnvironmentManager != null and EnvironmentManager.has_method("set_tracked_camera"):
        EnvironmentManager.set_tracked_camera(camera)
        ErrorLogger.log_debug("Environment tracking player camera", "Main")

func _on_player_registered(player: Node) -> void:
    if player == null:
        ErrorLogger.log_warning("Player registration signal received with null player", "Main")
        return
    _attach_viewer_to_camera()
    _attach_environment_to_camera()
