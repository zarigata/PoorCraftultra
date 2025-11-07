extends Node
class_name EnvironmentManager

signal time_of_day_changed(time_normalized: float, is_day: bool)
signal biome_changed(biome_id: String)
signal lighting_updated()

const CONFIG_PATH := "res://resources/environment_config.json"
const DAWN_TIME := 0.25
const DUSK_TIME := 0.75
const TRANSITION_DURATION := 0.1
const LIGHTING_UPDATE_INTERVAL := 0.1

var sun: DirectionalLight3D = null
var world_environment: WorldEnvironment = null
var environment: Environment = null
var sky_material: ProceduralSkyMaterial = null
var config: Dictionary = {}

var current_time_normalized: float = 0.0
var cycle_enabled: bool = true
var time_scale: float = 1.0
var current_biome: String = "default"
var player_camera: Camera3D = null

var _cycle_duration_seconds: float = 1200.0
var _lighting_timer: float = 0.0
var _last_is_day: bool = false

var _sun_day_rotation: Vector3 = Vector3(-45.0, 45.0, 0.0)
var _sun_night_rotation: Vector3 = Vector3(135.0, 45.0, 0.0)
var _sun_day_energy: float = 1.0
var _sun_night_energy: float = 0.1
var _sun_day_color: Color = Color(1.0, 0.95, 0.9)
var _sun_night_color: Color = Color(0.4, 0.5, 0.7)
var _shadow_enabled_default: bool = true
var _shadow_bias_default: float = 0.1
var _shadow_normal_bias_default: float = 1.0
var _shadow_blur_default: float = 1.0

var _sky_day_top_color: Color = Color(0.4, 0.6, 1.0)
var _sky_day_horizon_color: Color = Color(0.7, 0.8, 1.0)
var _sky_night_top_color: Color = Color(0.05, 0.05, 0.15)
var _sky_night_horizon_color: Color = Color(0.1, 0.15, 0.25)
var _sky_ground_color: Color = Color(0.3, 0.35, 0.4)

var _ambient_day_color: Color = Color(0.3, 0.3, 0.35)
var _ambient_night_color: Color = Color(0.1, 0.1, 0.15)
var _ambient_day_energy: float = 1.0
var _ambient_night_energy: float = 0.3

var _current_ambient_color: Color = _ambient_night_color

var _fog_enabled: bool = true
var _fog_day_color: Color = Color(0.5, 0.7, 1.0)
var _fog_night_color: Color = Color(0.1, 0.15, 0.25)
var _fog_density: float = 0.02
var _fog_mode: String = "exponential"
var _fog_depth_begin: float = 10.0
var _fog_depth_end: float = 200.0

var _current_fog_color: Color = _fog_night_color

var _light_lod_enabled: bool = true
var _shadow_distance_far: float = 400.0
var _shadow_fade_near: float = 150.0
var _shadow_fade_far: float = 350.0

var _disable_shadows_at_night: bool = false
var _night_shadow_multiplier: float = 1.0

var _biome_ambient_tint_strength: float = 1.0
var _biome_fog_tint_strength: float = 1.0

var _biome_overrides: Dictionary = {}

func _ready() -> void:
    ErrorLogger.log_info("EnvironmentManager initializing", "Environment")
    _load_config()
    _apply_config_values()
    current_time_normalized = clamp(_config_get_cycle_start(), 0.0, 1.0)
    _last_is_day = get_is_day()
    _lighting_timer = LIGHTING_UPDATE_INTERVAL
    set_process(true)
    _connect_to_game_manager()

func initialize(sun_node: DirectionalLight3D, world_env_node: WorldEnvironment) -> bool:
    if sun_node == null or world_env_node == null:
        ErrorLogger.log_error("Initialization nodes missing", "Environment")
        return false

    sun = sun_node
    world_environment = world_env_node
    environment = world_environment.environment

    if environment == null:
        ErrorLogger.log_error("WorldEnvironment lacks Environment resource", "Environment")
        return false

    var sky_resource := environment.sky
    if sky_resource == null or not (sky_resource is Sky):
        var sky := Sky.new()
        var material := ProceduralSkyMaterial.new()
        sky.sky_material = material
        environment.sky = sky
        sky_material = material
    else:
        var sky := sky_resource as Sky
        var existing_material := sky.sky_material
        if existing_material is ProceduralSkyMaterial:
            sky_material = existing_material
        else:
            var material := ProceduralSkyMaterial.new()
            sky.sky_material = material
            sky_material = material

    if environment != null:
        environment.background_mode = Environment.BG_SKY

    _apply_environment_config()
    _update_lighting(true)
    _update_light_lod()

    ErrorLogger.log_info("Environment system initialized", "Environment")
    return true

func set_time_normalized(time: float) -> void:
    var clamped_time := clampf(time, 0.0, 1.0)
    if is_equal_approx(clamped_time, current_time_normalized):
        return
    current_time_normalized = clamped_time
    _update_lighting()

func get_time_normalized() -> float:
    return current_time_normalized

func set_cycle_enabled(enabled: bool) -> void:
    if cycle_enabled == enabled:
        return
    cycle_enabled = enabled
    ErrorLogger.log_debug("Day/night cycle %s" % ("enabled" if enabled else "disabled"), "Environment")

func set_time_scale(scale: float) -> void:
    var clamped_scale := clampf(scale, 0.1, 10.0)
    if is_equal_approx(clamped_scale, time_scale):
        return
    time_scale = clamped_scale
    ErrorLogger.log_debug("Time scale set to %.2f" % time_scale, "Environment")

func set_biome(biome_id: String) -> void:
    if biome_id == "":
        biome_id = "default"
    if current_biome == biome_id:
        return
    current_biome = biome_id
    _apply_biome_overrides()
    emit_signal("biome_changed", current_biome)
    ErrorLogger.log_debug("Biome set to %s" % current_biome, "Environment")
    if typeof(AudioManager) != TYPE_NIL and AudioManager != null and AudioManager.has_method("play_ambient"):
        AudioManager.play_ambient(current_biome)

func set_tracked_camera(camera: Camera3D) -> void:
    if camera == player_camera:
        return
    player_camera = camera
    ErrorLogger.log_debug("Tracked camera updated", "Environment")

func get_is_day() -> bool:
    return current_time_normalized >= DAWN_TIME and current_time_normalized <= DUSK_TIME

func _process(delta: float) -> void:
    if cycle_enabled:
        var delta_time := delta * time_scale
        if _cycle_duration_seconds <= 0.0:
            _cycle_duration_seconds = 1.0
        current_time_normalized = fmod(current_time_normalized + (delta_time / _cycle_duration_seconds), 1.0)
        if current_time_normalized < 0.0:
            current_time_normalized += 1.0
    _lighting_timer += delta
    if _lighting_timer >= LIGHTING_UPDATE_INTERVAL:
        _lighting_timer = 0.0
        _update_lighting()
        _update_light_lod()

func _update_lighting(force_emit: bool = false) -> void:
    var interpolation := _get_time_interpolation()

    if sun != null:
        var rotation := _sun_night_rotation.lerp(_sun_day_rotation, interpolation)
        sun.rotation_degrees = rotation
        sun.light_color = _sun_night_color.lerp(_sun_day_color, interpolation)
        sun.light_energy = lerpf(_sun_night_energy, _sun_day_energy, interpolation)
        sun.shadow_bias = _shadow_bias_default
        sun.shadow_normal_bias = _shadow_normal_bias_default
        sun.shadow_blur = _shadow_blur_default

    if environment != null:
        var ambient_color := _ambient_night_color.lerp(_ambient_day_color, interpolation)
        var ambient_energy := lerpf(_ambient_night_energy, _ambient_day_energy, interpolation)
        _current_ambient_color = ambient_color
        environment.ambient_light_color = ambient_color
        environment.ambient_light_energy = ambient_energy

        environment.fog_enabled = _fog_enabled
        if _fog_enabled:
            environment.fog_density = _fog_density
            var fog_color := _fog_night_color.lerp(_fog_day_color, interpolation)
            _current_fog_color = fog_color
            environment.fog_light_color = fog_color
            if _fog_mode == "depth":
                environment.fog_mode = Environment.FOG_MODE_DEPTH
                environment.fog_depth_begin = _fog_depth_begin
                environment.fog_depth_end = _fog_depth_end
            else:
                environment.fog_mode = Environment.FOG_MODE_EXPONENTIAL

    if sky_material != null:
        sky_material.sky_top_color = _sky_night_top_color.lerp(_sky_day_top_color, interpolation)
        sky_material.sky_horizon_color = _sky_night_horizon_color.lerp(_sky_day_horizon_color, interpolation)
        sky_material.ground_color = _sky_ground_color

    _apply_biome_overrides()

    var is_day := get_is_day()
    if force_emit or is_day != _last_is_day:
        emit_signal("time_of_day_changed", current_time_normalized, is_day)
        _last_is_day = is_day
        if typeof(AudioManager) != TYPE_NIL and AudioManager != null and AudioManager.has_method("update_ambient_for_time"):
            AudioManager.update_ambient_for_time(is_day)

    emit_signal("lighting_updated")

# Directional lights have no positional attenuation, so shadow LOD should rely on
# configuration and view settings rather than the light's location in space.
func _update_light_lod() -> void:
    if sun == null:
        return

    if not _light_lod_enabled:
        sun.shadow_enabled = _shadow_enabled_default
        sun.directional_shadow_max_distance = _shadow_distance_far
        sun.directional_shadow_fade_start = _shadow_fade_near
        return

    var shadow_enabled := _shadow_enabled_default
    var max_distance := _shadow_distance_far
    if not get_is_day():
        if _disable_shadows_at_night:
            shadow_enabled = false
        else:
            max_distance *= clampf(_night_shadow_multiplier, 0.0, 1.0)

    sun.shadow_enabled = shadow_enabled
    if not shadow_enabled:
        return

    var fade_limit := _shadow_fade_far
    if fade_limit <= 0.0 or fade_limit > max_distance:
        fade_limit = max_distance

    if player_camera != null:
        var camera_far := player_camera.far
        if camera_far > 0.0:
            fade_limit = minf(fade_limit, camera_far)
            max_distance = minf(max_distance, camera_far)

    fade_limit = minf(fade_limit, max_distance)

    var fade_start := clampf(_shadow_fade_near, 0.0, fade_limit)
    if fade_start >= fade_limit and fade_limit > 0.0:
        fade_start = fade_limit * 0.9

    sun.directional_shadow_max_distance = max_distance
    sun.directional_shadow_fade_start = fade_start

func _apply_environment_config() -> void:
    if sun != null:
        sun.shadow_enabled = _shadow_enabled_default
        sun.shadow_bias = _shadow_bias_default
        sun.shadow_normal_bias = _shadow_normal_bias_default
        sun.shadow_blur = _shadow_blur_default
    if environment != null and environment.fog_enabled != _fog_enabled:
        environment.fog_enabled = _fog_enabled

func _apply_biome_overrides() -> void:
    if environment == null or _biome_overrides.is_empty():
        return
    var overrides := _biome_overrides.get(current_biome, _biome_overrides.get("default", null))
    if overrides == null:
        return

    if overrides.has("ambient_color"):
        var ambient_tint := _color_from_dict(overrides["ambient_color"], _current_ambient_color)
        var ambient_strength := clampf(float(overrides.get("ambient_tint_strength", _biome_ambient_tint_strength)), 0.0, 1.0)
        environment.ambient_light_color = _current_ambient_color.lerp(ambient_tint, ambient_strength)

    if overrides.has("fog_color"):
        var fog_tint := _color_from_dict(overrides["fog_color"], _current_fog_color)
        var fog_strength := clampf(float(overrides.get("fog_tint_strength", _biome_fog_tint_strength)), 0.0, 1.0)
        environment.fog_light_color = _current_fog_color.lerp(fog_tint, fog_strength)

func _get_time_interpolation() -> float:
    var time := current_time_normalized
    var dawn_start := DAWN_TIME - TRANSITION_DURATION * 0.5
    var dawn_end := DAWN_TIME + TRANSITION_DURATION * 0.5
    var dusk_start := DUSK_TIME - TRANSITION_DURATION * 0.5
    var dusk_end := DUSK_TIME + TRANSITION_DURATION * 0.5

    if time < dawn_start or time > dusk_end:
        return 0.0
    if time > dawn_end and time < dusk_start:
        return 1.0
    if time <= dawn_end:
        return _smoothstep(dawn_start, dawn_end, time)
    if time >= dusk_start:
        return 1.0 - _smoothstep(dusk_start, dusk_end, time)
    return 0.0

func _smoothstep(edge0: float, edge1: float, x: float) -> float:
    var t := clampf((x - edge0) / maxf(edge1 - edge0, 0.0001), 0.0, 1.0)
    return t * t * (3.0 - 2.0 * t)

func _load_config() -> void:
    var file := FileAccess.open(CONFIG_PATH, FileAccess.READ)
    if file == null:
        ErrorLogger.log_warning("Failed to load environment config, using defaults", "Environment")
        config = _get_default_config()
        return
    var text := file.get_as_text()
    file.close()
    var json := JSON.new()
    var result := json.parse(text)
    if result != OK:
        ErrorLogger.log_error("Invalid environment config JSON (code %d)" % result, "Environment")
        config = _get_default_config()
        return
    var data := json.get_data()
    if typeof(data) != TYPE_DICTIONARY:
        ErrorLogger.log_error("Environment config root must be Dictionary", "Environment")
        config = _get_default_config()
        return
    config = data
    ErrorLogger.log_info("Environment config loaded", "Environment")

func _apply_config_values() -> void:
    var cycle := config.get("day_night_cycle", {})
    cycle_enabled = cycle.get("enabled", true)
    _cycle_duration_seconds = maxf(cycle.get("cycle_duration_seconds", 1200.0), 1.0)
    time_scale = clampf(cycle.get("time_scale", 1.0), 0.1, 10.0)

    var sun_config := config.get("sun", {})
    _sun_day_rotation = _vector3_from_dict(sun_config.get("day_rotation", {}), _sun_day_rotation)
    _sun_night_rotation = _vector3_from_dict(sun_config.get("night_rotation", {}), _sun_night_rotation)
    _sun_day_energy = sun_config.get("day_energy", _sun_day_energy)
    _sun_night_energy = sun_config.get("night_energy", _sun_night_energy)
    _sun_day_color = _color_from_dict(sun_config.get("day_color", {}), _sun_day_color)
    _sun_night_color = _color_from_dict(sun_config.get("night_color", {}), _sun_night_color)
    _shadow_enabled_default = sun_config.get("shadow_enabled", _shadow_enabled_default)
    _shadow_bias_default = sun_config.get("shadow_bias", _shadow_bias_default)
    _shadow_normal_bias_default = sun_config.get("shadow_normal_bias", _shadow_normal_bias_default)
    _shadow_blur_default = sun_config.get("shadow_blur", _shadow_blur_default)

    var sky_config := config.get("sky", {})
    _sky_day_top_color = _color_from_dict(sky_config.get("day_top_color", {}), _sky_day_top_color)
    _sky_day_horizon_color = _color_from_dict(sky_config.get("day_horizon_color", {}), _sky_day_horizon_color)
    _sky_night_top_color = _color_from_dict(sky_config.get("night_top_color", {}), _sky_night_top_color)
    _sky_night_horizon_color = _color_from_dict(sky_config.get("night_horizon_color", {}), _sky_night_horizon_color)
    _sky_ground_color = _color_from_dict(sky_config.get("ground_color", {}), _sky_ground_color)

    var ambient_config := config.get("ambient", {})
    _ambient_day_color = _color_from_dict(ambient_config.get("day_color", {}), _ambient_day_color)
    _ambient_night_color = _color_from_dict(ambient_config.get("night_color", {}), _ambient_night_color)
    _ambient_day_energy = ambient_config.get("day_energy", _ambient_day_energy)
    _ambient_night_energy = ambient_config.get("night_energy", _ambient_night_energy)

    var fog_config := config.get("fog", {})
    _fog_enabled = fog_config.get("enabled", _fog_enabled)
    _fog_day_color = _color_from_dict(fog_config.get("day_color", {}), _fog_day_color)
    _fog_night_color = _color_from_dict(fog_config.get("night_color", {}), _fog_night_color)
    _fog_density = fog_config.get("density", _fog_density)
    _fog_mode = fog_config.get("mode", _fog_mode)
    _fog_depth_begin = float(fog_config.get("fog_depth_begin", _fog_depth_begin))
    _fog_depth_end = float(fog_config.get("fog_depth_end", _fog_depth_end))
    if _fog_depth_end < _fog_depth_begin:
        _fog_depth_end = _fog_depth_begin

    var performance := config.get("performance", {})
    _light_lod_enabled = performance.get("light_lod_enabled", _light_lod_enabled)
    if performance.has("light_lod_distance"):
        _shadow_distance_far = maxf(float(performance.get("light_lod_distance", _shadow_distance_far)), 0.0)
    if performance.has("shadow_fade_start"):
        _shadow_fade_near = maxf(float(performance.get("shadow_fade_start", _shadow_fade_near)), 0.0)
    if performance.has("shadow_fade_end"):
        _shadow_fade_far = maxf(float(performance.get("shadow_fade_end", _shadow_fade_far)), 0.0)
    _shadow_fade_near = clampf(_shadow_fade_near, 0.0, _shadow_distance_far)
    _shadow_fade_far = minf(_shadow_fade_far, _shadow_distance_far)
    if _shadow_fade_near >= _shadow_distance_far and _shadow_distance_far > 0.0:
        _shadow_fade_near = _shadow_distance_far * 0.9
    if _shadow_fade_far <= _shadow_fade_near:
        _shadow_fade_far = _shadow_distance_far

    _disable_shadows_at_night = performance.get("disable_shadows_at_night", _disable_shadows_at_night)
    _night_shadow_multiplier = clampf(float(performance.get("night_shadow_multiplier", _night_shadow_multiplier)), 0.0, 1.0)

    var biome_settings := config.get("biome_settings", {})
    _biome_ambient_tint_strength = clampf(float(biome_settings.get("ambient_tint_strength", _biome_ambient_tint_strength)), 0.0, 1.0)
    _biome_fog_tint_strength = clampf(float(biome_settings.get("fog_tint_strength", _biome_fog_tint_strength)), 0.0, 1.0)

    _biome_overrides = config.get("biomes", {})

func _config_get_cycle_start() -> float:
    var cycle := config.get("day_night_cycle", {})
    return cycle.get("start_time_normalized", 0.25)

func _get_default_config() -> Dictionary:
    return {
        "version": 1,
        "day_night_cycle": {
            "enabled": true,
            "cycle_duration_seconds": 1200.0,
            "start_time_normalized": 0.25,
            "time_scale": 1.0,
        },
        "sun": {
            "day_rotation": {"x": -45.0, "y": 45.0, "z": 0.0},
            "night_rotation": {"x": 135.0, "y": 45.0, "z": 0.0},
            "day_energy": 1.0,
            "night_energy": 0.1,
            "day_color": {"r": 1.0, "g": 0.95, "b": 0.9},
            "night_color": {"r": 0.4, "g": 0.5, "b": 0.7},
            "shadow_enabled": true,
            "shadow_bias": 0.1,
            "shadow_normal_bias": 1.0,
            "shadow_blur": 1.0,
        },
        "sky": {
            "day_top_color": {"r": 0.4, "g": 0.6, "b": 1.0},
            "day_horizon_color": {"r": 0.7, "g": 0.8, "b": 1.0},
            "night_top_color": {"r": 0.05, "g": 0.05, "b": 0.15},
            "night_horizon_color": {"r": 0.1, "g": 0.15, "b": 0.25},
            "ground_color": {"r": 0.3, "g": 0.35, "b": 0.4},
        },
        "ambient": {
            "day_color": {"r": 0.3, "g": 0.3, "b": 0.35},
            "night_color": {"r": 0.1, "g": 0.1, "b": 0.15},
            "day_energy": 1.0,
            "night_energy": 0.3,
        },
        "fog": {
            "enabled": true,
            "day_color": {"r": 0.5, "g": 0.7, "b": 1.0},
            "night_color": {"r": 0.1, "g": 0.15, "b": 0.25},
            "density": 0.02,
            "mode": "exponential",
            "fog_depth_begin": 10.0,
            "fog_depth_end": 200.0,
        },
        "performance": {
            "light_lod_enabled": true,
            "light_lod_distance": 200.0,
            "shadow_fade_start": 150.0,
            "shadow_fade_end": 200.0,
            "disable_shadows_at_night": false,
            "night_shadow_multiplier": 0.5,
        },
        "biome_settings": {
            "ambient_tint_strength": 1.0,
            "fog_tint_strength": 1.0,
        },
    }

func _connect_to_game_manager() -> void:
    var manager := _get_game_manager()
    if manager != null and not manager.settings_changed.is_connected(_on_settings_changed):
        manager.settings_changed.connect(_on_settings_changed)

func _on_settings_changed(setting_name: String, new_value: Variant) -> void:
    match setting_name:
        "graphics/shadows_enabled":
            _shadow_enabled_default = bool(new_value)
            if sun != null:
                sun.shadow_enabled = _shadow_enabled_default
            ErrorLogger.log_info("Shadow setting updated via GameManager", "Environment")
        _:
            pass

func _get_game_manager() -> GameManager:
    if typeof(GameManager) != TYPE_NIL and GameManager != null:
        return GameManager
    var tree := get_tree()
    if tree and tree.has_node("/root/GameManager"):
        return tree.get_node("/root/GameManager")
    return null

func _vector3_from_dict(data: Dictionary, fallback: Vector3) -> Vector3:
    if typeof(data) != TYPE_DICTIONARY:
        return fallback
    return Vector3(
        float(data.get("x", fallback.x)),
        float(data.get("y", fallback.y)),
        float(data.get("z", fallback.z))
    )

func _color_from_dict(data: Dictionary, fallback: Color) -> Color:
    if typeof(data) != TYPE_DICTIONARY:
        return fallback
    return Color(
        float(data.get("r", fallback.r)),
        float(data.get("g", fallback.g)),
        float(data.get("b", fallback.b)),
        float(data.get("a", fallback.a))
    )
