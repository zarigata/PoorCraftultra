extends Node
class_name AudioManager

signal ambient_changed(biome_id: String)
signal sfx_played(sound_id: String, position: Vector3)

const AMBIENCE_CONFIG_PATH := "res://resources/biome_ambience.json"
const MAX_POOL_SIZE := 32
const POOL_CLEANUP_INTERVAL := 5.0
const BUS_MASTER := "Master"
const BUS_SFX := "SFX"
const BUS_MUSIC := "Music"
const BUS_AMBIENT := "Ambient"

var audio_root: Node = null
var player_pool: Array = []
var player_3d_pool: Array = []
var active_players: Array = []
var ambient_player: AudioStreamPlayer = null
var ambience_config: Dictionary = {}
var current_biome: String = "default"
var current_ambient_stream: AudioStream = null
var _cleanup_timer: float = 0.0
var _tween: Tween = null
var _ambient_temp_player: AudioStreamPlayer = null

func _ready() -> void:
    ErrorLogger.log_info("AudioManager initializing", "AudioManager")
    audio_root = Node.new()
    audio_root.name = "AudioRoot"
    add_child(audio_root)

    ambient_player = AudioStreamPlayer.new()
    ambient_player.bus = BUS_AMBIENT
    ambient_player.autoplay = false
    ambient_player.stream_paused = false
    ambient_player.volume_db = -12.0
    audio_root.add_child(ambient_player)

    _setup_audio_buses()
    _load_ambience_config()
    _initialize_pools()
    _connect_to_game_manager()
    _apply_initial_volumes()
    set_process(true)
    ErrorLogger.log_info("AudioManager ready", "AudioManager")

func play_sfx(stream: AudioStream, volume_db: float = 0.0, pitch_scale: float = 1.0) -> AudioStreamPlayer:
    if stream == null:
        ErrorLogger.log_warning("Attempted to play null SFX stream", "AudioManager")
        return null
    var player := _get_pooled_player()
    if player == null:
        return null
    player.stream = stream
    player.volume_db = volume_db
    player.pitch_scale = pitch_scale
    player.bus = BUS_SFX
    player.play()
    active_players.append(player)
    emit_signal("sfx_played", stream.resource_path, Vector3.ZERO)
    return player

func play_sfx_3d(stream: AudioStream, position: Vector3, volume_db: float = 0.0, max_distance: float = 50.0) -> AudioStreamPlayer3D:
    if stream == null:
        ErrorLogger.log_warning("Attempted to play null 3D SFX stream", "AudioManager")
        return null
    var player := _get_pooled_player_3d()
    if player == null:
        return null
    player.stream = stream
    player.volume_db = volume_db
    player.max_distance = max_distance
    player.global_position = position
    player.bus = BUS_SFX
    player.play()
    active_players.append(player)
    emit_signal("sfx_played", stream.resource_path, position)
    return player

func stop_all_sfx() -> void:
    for player in active_players.duplicate():
        if player is AudioStreamPlayer3D:
            _return_to_pool_3d(player)
        elif player is AudioStreamPlayer:
            _return_to_pool(player)
    active_players.clear()
    ErrorLogger.log_info("All SFX stopped", "AudioManager")

func play_ambient(biome_id: String) -> void:
    var requested_biome := biome_id if biome_id != "" else "default"
    var selection := _select_ambient_stream(requested_biome)
    var stream := selection.get("stream", null)
    var selected_biome_id := selection.get("biome_id", requested_biome)
    var volume_db := float(selection.get("volume_db", -12.0))
    if stream == null:
        _stop_ambient()
        current_biome = requested_biome
        var fallback_id := selection.get("fallback_id", "")
        ErrorLogger.log_warning(
            "No valid ambient loops for biome %s%s" % [
                requested_biome,
                fallback_id != "" and ", fallback %s also missing" % fallback_id or ""
            ],
            "AudioManager"
        )
        emit_signal("ambient_changed", current_biome)
        return
    if current_biome == selected_biome_id and current_ambient_stream == stream and ambient_player.playing:
        return
    _stop_ambient()
    current_biome = selected_biome_id
    current_ambient_stream = stream
    ambient_player.stream = stream
    ambient_player.volume_db = volume_db
    ambient_player.bus = BUS_AMBIENT
    ambient_player.play()
    emit_signal("ambient_changed", current_biome)
    if selection.get("did_fallback", false):
        ErrorLogger.log_info(
            "Ambient fallback to biome %s for requested %s" % [current_biome, requested_biome],
            "AudioManager"
        )
    else:
        ErrorLogger.log_info("Ambient playing for biome %s" % current_biome, "AudioManager")

func stop_ambient() -> void:
    _stop_ambient()
    current_biome = "default"
    ErrorLogger.log_info("Ambient stopped", "AudioManager")

func update_ambient_for_time(is_day: bool) -> void:
    if current_biome == "":
        return
    var selection := _select_ambient_stream(current_biome, is_day)
    var stream := selection.get("stream", null)
    if stream == null or stream == current_ambient_stream:
        return
    if selection.get("did_fallback", false):
        ErrorLogger.log_info(
            "Ambient fallback stream %s for biome %s" % [selection.get("biome_id", current_biome), current_biome],
            "AudioManager"
        )
    _crossfade_ambient(selection)

func set_master_volume(volume_linear: float) -> void:
    _set_bus_volume(BUS_MASTER, volume_linear)

func set_sfx_volume(volume_linear: float) -> void:
    _set_bus_volume(BUS_SFX, volume_linear)

func set_music_volume(volume_linear: float) -> void:
    _set_bus_volume(BUS_MUSIC, volume_linear)

func set_ambient_volume(volume_linear: float) -> void:
    _set_bus_volume(BUS_AMBIENT, volume_linear)

func get_master_volume() -> float:
    return _get_bus_volume(BUS_MASTER)

func get_active_player_count() -> int:
    return active_players.size()

func get_max_pool_size() -> int:
    return MAX_POOL_SIZE

func _process(delta: float) -> void:
    _cleanup_timer += delta
    if _cleanup_timer >= POOL_CLEANUP_INTERVAL:
        _cleanup_timer = 0.0
        _cleanup_pool()
    _reclaim_finished_players()

func _setup_audio_buses() -> void:
    var master_index := AudioServer.get_bus_index(BUS_MASTER)
    if master_index == -1:
        ErrorLogger.log_error("Master bus not found; cannot configure audio buses", "AudioManager")
        return
    _ensure_bus_exists(BUS_SFX, BUS_MASTER)
    _ensure_bus_exists(BUS_MUSIC, BUS_MASTER)
    _ensure_bus_exists(BUS_AMBIENT, BUS_MASTER)

func _ensure_bus_exists(bus_name: String, send_to: String) -> void:
    if AudioServer.get_bus_index(bus_name) != -1:
        return
    AudioServer.add_bus()
    var index := AudioServer.get_bus_count() - 1
    AudioServer.set_bus_name(index, bus_name)
    AudioServer.set_bus_send(index, send_to)

func _load_ambience_config() -> void:
    var file := FileAccess.open(AMBIENCE_CONFIG_PATH, FileAccess.READ)
    if file == null:
        ErrorLogger.log_warning("Missing biome ambience config", "AudioManager")
        ambience_config = _get_default_ambience_config()
        return
    var json := JSON.parse_string(file.get_as_text())
    file.close()
    if typeof(json) != TYPE_DICTIONARY:
        ErrorLogger.log_error("Invalid biome ambience config, using default", "AudioManager")
        ambience_config = _get_default_ambience_config()
        return
    ambience_config = json
    ErrorLogger.log_info("Biome ambience config loaded", "AudioManager")

func _initialize_pools() -> void:
    for i in range(8):
        var player := AudioStreamPlayer.new()
        player.bus = BUS_SFX
        audio_root.add_child(player)
        player.finished.connect(_on_player_finished.bind(player))
        player_pool.append(player)
    for i in range(8):
        var player3d := AudioStreamPlayer3D.new()
        player3d.bus = BUS_SFX
        audio_root.add_child(player3d)
        player3d.finished.connect(_on_player_finished_3d.bind(player3d))
        player_3d_pool.append(player3d)

func _connect_to_game_manager() -> void:
    var manager := _get_game_manager()
    if manager != null and not manager.settings_changed.is_connected(_on_settings_changed):
        manager.settings_changed.connect(_on_settings_changed)

func _apply_initial_volumes() -> void:
    var manager := _get_game_manager()
    if manager == null:
        return
    set_master_volume(float(manager.get_setting("audio/master_volume", 1.0)))
    set_sfx_volume(float(manager.get_setting("audio/sfx_volume", 1.0)))
    set_music_volume(float(manager.get_setting("audio/music_volume", 0.7)))
    set_ambient_volume(float(manager.get_setting("audio/ambient_volume", 1.0)))

func _get_pooled_player() -> AudioStreamPlayer:
    if not player_pool.is_empty():
        return player_pool.pop_back()
    var total_players := player_pool.size()
    for player in active_players:
        if player is AudioStreamPlayer and player != ambient_player:
            total_players += 1
    if total_players >= MAX_POOL_SIZE:
        var reused := _take_active_audio_player()
        if reused != null:
            reused.stop()
            return reused
        ErrorLogger.log_warning("Audio player pool exhausted for 2D players", "AudioManager")
        return null
    var player := AudioStreamPlayer.new()
    player.bus = BUS_SFX
    audio_root.add_child(player)
    player.finished.connect(_on_player_finished.bind(player))
    return player

func _get_pooled_player_3d() -> AudioStreamPlayer3D:
    if not player_3d_pool.is_empty():
        return player_3d_pool.pop_back()
    var total_players := player_3d_pool.size()
    for player in active_players:
        if player is AudioStreamPlayer3D:
            total_players += 1
    if total_players >= MAX_POOL_SIZE:
        var reused := _take_active_audio_player_3d()
        if reused != null:
            reused.stop()
            return reused
        ErrorLogger.log_warning("Audio 3D player pool exhausted", "AudioManager")
        return null
    var player := AudioStreamPlayer3D.new()
    player.bus = BUS_SFX
    audio_root.add_child(player)
    player.finished.connect(_on_player_finished_3d.bind(player))
    return player

func _return_to_pool(player: AudioStreamPlayer) -> void:
    if player == null:
        return
    player.stop()
    player.stream = null
    player.pitch_scale = 1.0
    player.volume_db = 0.0
    if player in active_players:
        active_players.erase(player)
    if not player_pool.has(player):
        player_pool.append(player)

func _return_to_pool_3d(player: AudioStreamPlayer3D) -> void:
    if player == null:
        return
    player.stop()
    player.stream = null
    player.volume_db = 0.0
    player.max_distance = 50.0
    if player in active_players:
        active_players.erase(player)
    if not player_3d_pool.has(player):
        player_3d_pool.append(player)

func _reclaim_finished_players() -> void:
    for player in active_players.duplicate():
        if player != null and not player.playing:
            if player is AudioStreamPlayer3D:
                _return_to_pool_3d(player)
            else:
                _return_to_pool(player)

func _cleanup_pool() -> void:
    while player_pool.size() > MAX_POOL_SIZE:
        var player := player_pool.pop_back()
        player.queue_free()
    while player_3d_pool.size() > MAX_POOL_SIZE:
        var player3d := player_3d_pool.pop_back()
        player3d.queue_free()

func _pick_ambient_stream_for_time(biome_data: Dictionary, is_day: Variant = null) -> Dictionary:
    var loops := biome_data.get("ambient_loops", [])
    if loops.is_empty():
        return {}
    var target_time := is_day
    if target_time == null and typeof(EnvironmentManager) != TYPE_NIL and EnvironmentManager != null and EnvironmentManager.has_method("get_is_day"):
        target_time = EnvironmentManager.get_is_day()
    var chosen := loops[0]
    for entry in loops:
        var time_of_day := entry.get("time_of_day", "all")
        if target_time == null or time_of_day == "all" or (target_time and time_of_day == "day") or (not target_time and time_of_day == "night"):
            chosen = entry
            break
    var path := chosen.get("path", "")
    if path == "":
        return {}
    if not ResourceLoader.exists(path):
        ErrorLogger.log_warning("Ambient stream not found: %s" % path, "AudioManager")
        return {}
    var stream := ResourceLoader.load(path)
    if stream != null:
        if stream is AudioStreamOggVorbis:
            (stream as AudioStreamOggVorbis).loop = true
        elif stream is AudioStreamWAV:
            (stream as AudioStreamWAV).loop_mode = AudioStreamWAV.LOOP_FORWARD
        return {
            "stream": stream,
            "volume_db": float(chosen.get("volume_db", -12.0)),
        }
    return {}

func _crossfade_ambient(selection: Dictionary) -> void:
    if ambient_player == null:
        return
    var new_stream := selection.get("stream", null)
    if new_stream == null:
        return
    var new_volume_db := float(selection.get("volume_db", ambient_player.volume_db))
    _kill_tween()
    _cleanup_temp_ambient_player()
    _tween = create_tween()
    var old_player := ambient_player
    var temp_player := AudioStreamPlayer.new()
    temp_player.bus = BUS_AMBIENT
    temp_player.stream = new_stream
    temp_player.volume_db = -80.0
    temp_player.autoplay = false
    audio_root.add_child(temp_player)
    temp_player.play()
    _ambient_temp_player = temp_player
    _tween.tween_property(temp_player, "volume_db", new_volume_db, 1.5)
    _tween.parallel().tween_property(old_player, "volume_db", -80.0, 1.5)
    _tween.finished.connect(func():
        old_player.stop()
        old_player.stream = new_stream
        old_player.volume_db = new_volume_db
        old_player.play()
        if temp_player.is_inside_tree():
            temp_player.get_parent().remove_child(temp_player)
        temp_player.queue_free()
        _ambient_temp_player = null
        _tween = null
    )
    current_ambient_stream = new_stream
    emit_signal("ambient_changed", current_biome)

func _stop_ambient() -> void:
    _kill_tween()
    _cleanup_temp_ambient_player()
    if ambient_player != null and ambient_player.playing:
        ambient_player.stop()
    if ambient_player != null:
        ambient_player.stream = null
    current_ambient_stream = null

func _set_bus_volume(bus_name: String, volume_linear: float) -> void:
    var index := AudioServer.get_bus_index(bus_name)
    if index == -1:
        return
    AudioServer.set_bus_volume_db(index, linear_to_db(clampf(volume_linear, 0.0, 1.0)))

func _get_bus_volume(bus_name: String) -> float:
    var index := AudioServer.get_bus_index(bus_name)
    if index == -1:
        return 1.0
    return db_to_linear(AudioServer.get_bus_volume_db(index))

func _select_ambient_stream(biome_id: String, is_day: Variant = null) -> Dictionary:
    var biomes := ambience_config.get("biomes", {})
    var fallback_id := ambience_config.get("default_biome", "")
    var normalized := biome_id
    if normalized == "":
        normalized = "default"
    var candidates: Array = [normalized]
    if fallback_id != "" and not candidates.has(fallback_id):
        candidates.append(fallback_id)
    for candidate in candidates:
        var biome_data := biomes.get(candidate, null)
        if biome_data == null:
            continue
        var pick := _pick_ambient_stream_for_time(biome_data, is_day)
        var stream := pick.get("stream", null)
        if stream != null:
            return {
                "stream": stream,
                "biome_id": candidate,
                "did_fallback": candidate != normalized,
                "fallback_id": fallback_id,
                "volume_db": pick.get("volume_db", -12.0),
            }
    return {
        "stream": null,
        "biome_id": normalized,
        "did_fallback": false,
        "fallback_id": fallback_id,
    }

func _kill_tween() -> void:
    if _tween != null:
        if _tween.is_valid():
            _tween.kill()
        _tween = null

func _cleanup_temp_ambient_player() -> void:
    if _ambient_temp_player == null:
        return
    if _ambient_temp_player.playing:
        _ambient_temp_player.stop()
    if _ambient_temp_player.is_inside_tree():
        _ambient_temp_player.get_parent().remove_child(_ambient_temp_player)
    _ambient_temp_player.queue_free()
    _ambient_temp_player = null

func _take_active_audio_player() -> AudioStreamPlayer:
    for i in range(active_players.size()):
        var candidate := active_players[i]
        if candidate is AudioStreamPlayer and candidate != ambient_player:
            active_players.remove_at(i)
            return candidate
    return null

func _take_active_audio_player_3d() -> AudioStreamPlayer3D:
    for i in range(active_players.size()):
        var candidate := active_players[i]
        if candidate is AudioStreamPlayer3D:
            active_players.remove_at(i)
            return candidate
    return null

func _on_settings_changed(setting_name: String, new_value: Variant) -> void:
    match setting_name:
        "audio/master_volume":
            set_master_volume(float(new_value))
        "audio/sfx_volume":
            set_sfx_volume(float(new_value))
        "audio/music_volume":
            set_music_volume(float(new_value))
        "audio/ambient_volume":
            set_ambient_volume(float(new_value))
        _:
            pass

func _get_game_manager() -> GameManager:
    if typeof(GameManager) != TYPE_NIL and GameManager != null:
        return GameManager
    var tree := get_tree()
    if tree and tree.has_node("/root/GameManager"):
        return tree.get_node("/root/GameManager")
    return null

func _get_default_ambience_config() -> Dictionary:
    return {
        "version": 1,
        "biomes": {
            "default": {
                "name": "Default",
                "ambient_loops": [],
                "random_sounds": [],
            },
        },
        "default_biome": "default",
    }

func _on_player_finished(player: AudioStreamPlayer) -> void:
    _return_to_pool(player)

func _on_player_finished_3d(player: AudioStreamPlayer3D) -> void:
    _return_to_pool_3d(player)
