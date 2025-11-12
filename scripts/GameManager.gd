extends Node
class_name GameManager

signal game_state_changed(old_state: GameState, new_state: GameState)
signal settings_changed(setting_name: String, new_value: Variant)
signal game_paused(is_paused: bool)
signal player_registered(player: Node)

enum GameState { INITIALIZING, MAIN_MENU, LOADING, PLAYING, PAUSED, SAVING, EXITING }
enum GameMode { SINGLEPLAYER, MULTIPLAYER_HOST, MULTIPLAYER_CLIENT }

var current_state: GameState = GameState.INITIALIZING
var game_mode: GameMode = GameMode.SINGLEPLAYER
var game_version: String = "0.1.0-alpha"
var is_paused: bool = false
var settings: Dictionary = {}

const SETTINGS_PATH := "user://settings.cfg"
const DEFAULT_SETTINGS := {
    "graphics/vsync_enabled": true,
    "graphics/max_fps": 60,
    "audio/master_volume": 1.0,
    "audio/sfx_volume": 1.0,
    "audio/music_volume": 0.7,
    "controls/mouse_sensitivity": 0.3,
    "inventory/auto_sort_on_pickup": false,
    "inventory/show_weight": true,
    "inventory/show_tooltips": true,
    "ui/scale": 1.0,
    "ui/color_blind_mode": 0,
    "ui/high_contrast": false,
    "ui/text_size": 1.0,
    "ui/safe_area_margin": 20,
}

func _ready() -> void:
    ErrorLogger.log_info("GameManager initializing", "GameManager")
    load_settings()
    change_state(GameState.MAIN_MENU)

func _notification(what: int) -> void:
    if what == NOTIFICATION_WM_CLOSE_REQUEST:
        quit_game()

var current_world: Node = null
var current_player: Node = null

func load_settings() -> void:
    settings = DEFAULT_SETTINGS.duplicate(true)
    var config := ConfigFile.new()
    var err := config.load(SETTINGS_PATH)
    if err != OK:
        if err != ERR_FILE_NOT_FOUND:
            ErrorLogger.log_warning("Failed to load settings.cfg (code %d)" % err, "GameManager")
        save_settings()
        return
    for section in config.get_sections():
        for key in config.get_section_keys(section):
            var setting_name := "%s/%s" % [section, key]
            settings[setting_name] = config.get_value(section, key, DEFAULT_SETTINGS.get(setting_name, null))

func save_settings() -> void:
    var config := ConfigFile.new()
    for setting_name in settings.keys():
        var parts := setting_name.split("/")
        if parts.size() < 2:
            continue
        var section := parts[0]
        var key := String("/").join(parts.slice(1, parts.size()))
        config.set_value(section, key, settings[setting_name])
    var err := config.save(SETTINGS_PATH)
    if err != OK:
        ErrorLogger.log_error("Failed to save settings.cfg (code %d)" % err, "GameManager")

func get_setting(key: String, default_value: Variant = null) -> Variant:
    if settings.has(key):
        return settings[key]
    ErrorLogger.log_warning("Setting %s not found, using default" % key, "GameManager")
    return default_value

func set_setting(key: String, value: Variant) -> void:
    settings[key] = value
    emit_signal("settings_changed", key, value)
    save_settings()
    ErrorLogger.log_info("Setting %s updated" % key, "GameManager")

func change_state(new_state: GameState) -> void:
    if new_state == current_state:
        ErrorLogger.log_debug("State unchanged: %s" % GameState.keys()[new_state], "GameManager")
        return
    var old_state := current_state
    current_state = new_state
    emit_signal("game_state_changed", old_state, new_state)
    ErrorLogger.log_info("State changed: %s -> %s" % [GameState.keys()[old_state], GameState.keys()[new_state]], "GameManager")

func pause_game() -> void:
    if is_paused:
        return
    is_paused = true
    emit_signal("game_paused", true)
    get_tree().paused = true
    change_state(GameState.PAUSED)

func resume_game() -> void:
    if not is_paused:
        return
    is_paused = false
    emit_signal("game_paused", false)
    get_tree().paused = false
    change_state(GameState.PLAYING)

func quit_game() -> void:
    ErrorLogger.log_info("Quitting game", "GameManager")
    change_state(GameState.EXITING)
    save_settings()
    get_tree().quit()

func get_current_world() -> Node:
    return current_world

func set_current_world(world: Node) -> void:
    if current_world != null and current_world != world:
        ErrorLogger.log_warning("Replacing existing world reference", "GameManager")
    current_world = world
    if world != null:
        ErrorLogger.log_info("World registered: %s" % world.name, "GameManager")
    else:
        ErrorLogger.log_warning("World reference cleared", "GameManager")

func get_player() -> Node:
    return current_player

func set_player(player: Node) -> void:
    if current_player != null and current_player != player:
        ErrorLogger.log_warning("Replacing existing player reference", "GameManager")
    current_player = player
    if player != null:
        ErrorLogger.log_info("Player registered: %s" % player.name, "GameManager")
    else:
        ErrorLogger.log_warning("Player reference cleared", "GameManager")
    emit_signal("player_registered", player)
