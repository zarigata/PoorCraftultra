extends Node
class_name SaveManager

signal save_started()
signal save_completed(success: bool, save_path: String)
signal load_started()
signal load_completed(success: bool, save_path: String)
signal save_corrupted(save_path: String, error: String)

const SAVE_DIR: String = "user://saves/"
const SAVE_EXTENSION: String = ".rfsave"
const SAVE_VERSION: int = 1
const AUTO_SAVE_INTERVAL: float = 300.0

var current_save_name: String = ""
var auto_save_enabled: bool = true
var auto_save_timer: float = 0.0
var is_saving: bool = false
var is_loading: bool = false

class SaveData:
    var version: int = SAVE_VERSION
    var timestamp: int = Time.get_unix_time_from_system()
    var game_version: String = ""
    var world_seed: int = 0
    var voxel_config: Dictionary = {}
    var player_data: Dictionary = {}
    var voxel_edits: Array = []
    var buildings: Array = []
    var entities: Array = []

    func to_dictionary() -> Dictionary:
        return {
            "version": version,
            "timestamp": timestamp,
            "game_version": game_version,
            "world_seed": world_seed,
            "voxel_config": voxel_config,
            "player_data": player_data,
            "voxel_edits": voxel_edits,
            "buildings": buildings,
            "entities": entities,
        }

func _ready() -> void:
    DirAccess.make_dir_recursive_absolute(SAVE_DIR)
    ErrorLogger.log_info("SaveManager ready", "SaveManager")

func _process(delta: float) -> void:
    if not auto_save_enabled or GameManager.current_state != GameManager.GameState.PLAYING:
        return
    auto_save_timer += delta
    if auto_save_timer >= AUTO_SAVE_INTERVAL:
        auto_save()
        auto_save_timer = 0.0

func _notification(what: int) -> void:
    if what == NOTIFICATION_WM_CLOSE_REQUEST:
        if auto_save_enabled and GameManager.current_state == GameManager.GameState.PLAYING:
            auto_save()

func save_game(save_name: String) -> bool:
    if is_saving:
        ErrorLogger.log_warning("Save already in progress", "SaveManager")
        return false
    is_saving = true
    emit_signal("save_started")

    var save_path := get_save_path(save_name)
    DirAccess.make_dir_recursive_absolute(SAVE_DIR)

    var data := SaveData.new()
    data.game_version = GameManager.game_version
    data.world_seed = 0
    data.player_data = {}

    var world := GameManager.get_current_world()
    if world and world.has_method("serialize_state"):
        var world_state := world.serialize_state()
        data.world_seed = world_state.get("seed", 0)
        data.voxel_config = world_state.get("config", {})
        data.voxel_edits = world_state.get("edits", [])
        ErrorLogger.log_debug("Saved world seed: %d with config" % data.world_seed, "SaveManager")
    else:
        ErrorLogger.log_warning("No voxel world to save", "SaveManager")

    var json := JSON.new()
    var json_string := json.stringify(data.to_dictionary(), "  ")

    var file := FileAccess.open(save_path, FileAccess.WRITE)
    if file == null:
        ErrorLogger.log_error("Failed to open save file for writing: %s" % save_path, "SaveManager")
        emit_signal("save_completed", false, save_path)
        is_saving = false
        return false

    file.store_string(json_string)
    file.close()

    ErrorLogger.log_info("Save complete: %s" % save_path, "SaveManager")
    current_save_name = save_name
    emit_signal("save_completed", true, save_path)
    is_saving = false
    return true

func load_game(save_name: String) -> bool:
    if is_loading:
        ErrorLogger.log_warning("Load already in progress", "SaveManager")
        return false
    is_loading = true
    emit_signal("load_started")

    var save_path := get_save_path(save_name)
    if not FileAccess.file_exists(save_path):
        ErrorLogger.log_error("Save file does not exist: %s" % save_path, "SaveManager")
        emit_signal("load_completed", false, save_path)
        is_loading = false
        return false

    if not validate_save_file(save_path):
        emit_signal("load_completed", false, save_path)
        is_loading = false
        return false

    var file := FileAccess.open(save_path, FileAccess.READ)
    if file == null:
        ErrorLogger.log_error("Failed to open save file: %s" % save_path, "SaveManager")
        emit_signal("load_completed", false, save_path)
        is_loading = false
        return false

    var json_string := file.get_as_text()
    file.close()

    var parsed := JSON.parse_string(json_string)
    if typeof(parsed) != TYPE_DICTIONARY:
        ErrorLogger.log_error("Invalid save data format", "SaveManager")
        emit_signal("load_completed", false, save_path)
        is_loading = false
        return false

    current_save_name = save_name
    ErrorLogger.log_info("Load complete: %s" % save_path, "SaveManager")
    var world := GameManager.get_current_world()
    if world and world.has_method("deserialize_state"):
        var world_data := {
            "seed": parsed.get("world_seed", 0),
            "config": parsed.get("voxel_config", {}),
            "edits": parsed.get("voxel_edits", []),
        }
        world.deserialize_state(world_data)
        ErrorLogger.log_debug("Restored world seed: %d with config" % world_data["seed"], "SaveManager")
    else:
        ErrorLogger.log_warning("No voxel world to restore", "SaveManager")
    emit_signal("load_completed", true, save_path)
    is_loading = false
    return true

func delete_save(save_name: String) -> bool:
    var save_path := get_save_path(save_name)
    if not FileAccess.file_exists(save_path):
        ErrorLogger.log_warning("Save file not found: %s" % save_path, "SaveManager")
        return false
    var result := DirAccess.remove_absolute(save_path) == OK
    if result:
        ErrorLogger.log_info("Deleted save: %s" % save_path, "SaveManager")
    else:
        ErrorLogger.log_error("Failed to delete save: %s" % save_path, "SaveManager")
    return result

func get_save_list() -> Array:
    var saves := []
    var dir := DirAccess.open(SAVE_DIR)
    if dir == null:
        return saves
    dir.list_dir_begin()
    var name := dir.get_next()
    while name != "":
        if not dir.current_is_dir() and name.ends_with(SAVE_EXTENSION):
            var save_name := name.trim_suffix(SAVE_EXTENSION)
            var save_path := SAVE_DIR + name
            var metadata := _read_save_metadata(save_path)
            metadata["name"] = save_name
            saves.append(metadata)
        name = dir.get_next()
    dir.list_dir_end()
    return saves

func save_exists(save_name: String) -> bool:
    return FileAccess.file_exists(get_save_path(save_name))

func auto_save() -> void:
    if GameManager.current_state != GameManager.GameState.PLAYING:
        return
    ErrorLogger.log_info("Auto-saving game", "SaveManager")
    save_game("autosave")

func validate_save_file(save_path: String) -> bool:
    var file := FileAccess.open(save_path, FileAccess.READ)
    if file == null:
        ErrorLogger.log_error("Unable to open save file for validation: %s" % save_path, "SaveManager")
        emit_signal("save_corrupted", save_path, "cannot_open")
        return false
    var json_string := file.get_as_text()
    file.close()
    var json := JSON.new()
    var parse_result := json.parse(json_string)
    if parse_result != OK:
        ErrorLogger.log_error("Invalid JSON in save file: %s" % save_path, "SaveManager")
        emit_signal("save_corrupted", save_path, "invalid_json")
        return false
    var data := json.data
    if typeof(data) != TYPE_DICTIONARY:
        emit_signal("save_corrupted", save_path, "invalid_data_type")
        return false
    if data.get("version", -1) != SAVE_VERSION:
        emit_signal("save_corrupted", save_path, "unsupported_version")
        return false
    return true

func enable_auto_save() -> void:
    auto_save_enabled = true

func disable_auto_save() -> void:
    auto_save_enabled = false

func get_last_save_time() -> int:
    var latest := 0
    var dir := DirAccess.open(SAVE_DIR)
    if dir == null:
        return latest
    dir.list_dir_begin()
    var name := dir.get_next()
    while name != "":
        if not dir.current_is_dir() and name.ends_with(SAVE_EXTENSION):
            var info := dir.get_modified_time(name)
            latest = max(latest, info)
        name = dir.get_next()
    dir.list_dir_end()
    return latest

func get_save_path(save_name: String) -> String:
    return "%s%s%s" % [SAVE_DIR, save_name, SAVE_EXTENSION]

func register_saveable(node: Node) -> void:
    pass

func on_before_save() -> void:
    pass

func on_after_load() -> void:
    pass

func compress_save_data(data: Dictionary) -> PackedByteArray:
    var json_string := JSON.stringify(data)
    return json_string.to_utf8_buffer()

func decompress_save_data(compressed: PackedByteArray) -> Dictionary:
    if compressed.is_empty():
        return {}
    var json_string := compressed.get_string_from_utf8()
    var parsed := JSON.parse_string(json_string)
    return parsed if typeof(parsed) == TYPE_DICTIONARY else {}

func _read_save_metadata(path: String) -> Dictionary:
    var metadata := {
        "timestamp": 0,
        "game_version": "",
        "playtime": 0.0,
    }
    var file := FileAccess.open(path, FileAccess.READ)
    if file == null:
        return metadata
    var json_string := file.get_as_text()
    file.close()
    var parsed := JSON.parse_string(json_string)
    if typeof(parsed) != TYPE_DICTIONARY:
        return metadata
    metadata["timestamp"] = parsed.get("timestamp", 0)
    metadata["game_version"] = parsed.get("game_version", "")
    metadata["playtime"] = parsed.get("playtime", 0.0)
    return metadata
