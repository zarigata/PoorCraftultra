extends RigidBody3D
class_name DroppedItem

signal item_picked_up(item_id: String, quantity: int)
signal item_despawned(item_id: String, quantity: int, reason: String)
signal item_stacked(other_item: DroppedItem)

const LIFETIME_DURATION := 300.0
const DESPAWN_WARNING_TIME := 30.0
const PICKUP_RADIUS := 1.5
const STACK_MERGE_RADIUS := 1.0
const STACK_CHECK_DELAY := 0.5
const BOUNCE_VELOCITY_THRESHOLD := 0.5
const PICKUP_SOUND_PATH := "res://assets/audio/sfx/items/pickup.ogg"
const DESPAWN_SOUND_PATH := "res://assets/audio/sfx/items/despawn.ogg"
const BOUNCE_SOUND_PATH := "res://assets/audio/sfx/items/bounce.ogg"

@export var item_id: String = ""
@export var quantity: int = 1
@export var network_id: int = -1
@export var spawner_peer_id: int = 1

var lifetime_remaining: float = LIFETIME_DURATION
var is_on_ground: bool = false
var can_be_picked_up: bool = false
var pickup_delay_timer: float = 0.0
var stack_check_timer: float = 0.0
var is_despawning: bool = false
var is_fading: bool = false
var pickup_area: Area3D = null
var mesh_instance: MeshInstance3D = null
var _audio_manager: AudioManager = null
var _game_manager: GameManager = null
var _last_bounce_time: float = -1000.0
var _despawn_started: bool = false
var _fade_tween: Tween = null

func _ready() -> void:
    set_physics_process(true)
    set_multiplayer_authority(spawner_peer_id)
    contact_monitor = true
    max_contacts_reported = max(max_contacts_reported, 4)
    collision_layer = 1 << 2
    collision_mask = 1 | (1 << 1) | (1 << 4)
    pickup_area = get_node_or_null("PickupArea")
    if pickup_area == null:
        pickup_area = Area3D.new()
        pickup_area.name = "PickupArea"
        add_child(pickup_area)
    pickup_area.monitoring = true
    pickup_area.monitorable = false
    pickup_area.collision_layer = 1 << 2
    pickup_area.collision_mask = 1 << 1
    if not pickup_area.body_entered.is_connected(_on_body_entered):
        pickup_area.body_entered.connect(_on_body_entered)
    var pickup_shape := pickup_area.get_node_or_null("PickupShape")
    if pickup_shape == null:
        pickup_shape = CollisionShape3D.new()
        pickup_shape.name = "PickupShape"
        pickup_area.add_child(pickup_shape)
    if pickup_shape.shape == null or not (pickup_shape.shape is SphereShape3D):
        var sphere_shape := SphereShape3D.new()
        sphere_shape.radius = PICKUP_RADIUS
        pickup_shape.shape = sphere_shape
    else:
        pickup_shape.shape.radius = PICKUP_RADIUS
    mesh_instance = get_node_or_null("MeshInstance")
    if mesh_instance == null:
        mesh_instance = get_node_or_null("Mesh")
    if mesh_instance != null:
        mesh_instance.modulate = Color.WHITE
    if not body_entered.is_connected(_on_body_entered):
        body_entered.connect(_on_body_entered)
    pickup_delay_timer = 0.2
    lifetime_remaining = LIFETIME_DURATION
    _audio_manager = _get_audio_manager()
    _game_manager = _get_game_manager()
    ErrorLogger.log_debug("DroppedItem initialized with %dx %s" % [quantity, item_id], "DroppedItem")

func _physics_process(delta: float) -> void:
    if _game_manager != null and _game_manager.is_paused:
        return
    if _despawn_started:
        return
    if pickup_delay_timer > 0.0:
        pickup_delay_timer -= delta
        if pickup_delay_timer <= 0.0:
            can_be_picked_up = true
    lifetime_remaining -= delta
    if lifetime_remaining <= DESPAWN_WARNING_TIME and not is_fading:
        _start_despawn_fade()
    if lifetime_remaining <= 0.0 and not _despawn_started:
        _despawn("timeout")
    if is_on_ground and stack_check_timer > 0.0:
        stack_check_timer -= delta
        if stack_check_timer <= 0.0:
            _check_for_stacking()

func set_item_data(new_item_id: String, new_quantity: int) -> void:
    if new_item_id.strip_edges() == "" or new_quantity <= 0:
        ErrorLogger.log_warning("Invalid item data assigned to DroppedItem", "DroppedItem")
        return
    item_id = new_item_id
    quantity = new_quantity
    _update_mesh_color()

func get_item_data() -> Dictionary:
    return {
        "item_id": item_id,
        "quantity": quantity,
    }

func get_lifetime_remaining() -> float:
    return lifetime_remaining

func extend_lifetime(seconds: float) -> void:
    lifetime_remaining = min(lifetime_remaining + seconds, LIFETIME_DURATION)

func can_stack_with(other: DroppedItem) -> bool:
    if other == null:
        return false
    return other.item_id == item_id and other != self

func merge_with(other: DroppedItem) -> void:
    if other == null or not can_stack_with(other):
        return
    other.quantity += quantity
    emit_signal("item_stacked", other)
    ErrorLogger.log_debug("Stacked %dx %s into existing pile" % [quantity, item_id], "DroppedItem")
    queue_free()

func _on_body_entered(body: Node) -> void:
    if body == null:
        return
    if body is Node3D and body.has_method("request_item_pickup") and can_be_picked_up and not _despawn_started:
        if body.request_item_pickup(self):
            _pickup_by_player(body)
        return
    if body is StaticBody3D or (body is CollisionObject3D and body.collision_layer & 1) != 0:
        is_on_ground = true
        stack_check_timer = STACK_CHECK_DELAY
        var velocity_magnitude := linear_velocity.length()
        if velocity_magnitude >= BOUNCE_VELOCITY_THRESHOLD:
            _play_bounce_sound()

@rpc("any_peer", "call_remote", "reliable")
func _pickup_by_player(player: Node) -> void:
    if is_despawning:
        return
    var mp := multiplayer
    if mp != null and mp.has_multiplayer_peer() and not is_multiplayer_authority():
        return
    can_be_picked_up = false
    emit_signal("item_picked_up", item_id, quantity)
    if _audio_manager != null:
        var stream := _load_audio(PICKUP_SOUND_PATH)
        if stream != null:
            _audio_manager.play_sfx_3d(stream, global_position, -6.0)
    _spawn_pickup_particles()
    ErrorLogger.log_info("Item picked up: %dx %s" % [quantity, item_id], "DroppedItem")
    queue_free()

func _check_for_stacking() -> void:
    if is_despawning:
        return
    var world := get_world_3d()
    if world == null:
        return
    var space_state := world.direct_space_state
    if space_state == null:
        return
    var sphere := SphereShape3D.new()
    sphere.radius = STACK_MERGE_RADIUS
    var params := PhysicsShapeQueryParameters3D.new()
    params.shape = sphere
    params.transform = Transform3D(Basis.IDENTITY, global_position)
    params.collide_with_areas = false
    params.collide_with_bodies = true
    params.collision_mask = 1 << 2
    params.exclude = [get_rid()]
    var result := space_state.intersect_shape(params, 16)
    for entry in result:
        var collider := entry.get("collider", null)
        if collider != null and collider is DroppedItem and can_stack_with(collider):
            merge_with(collider)
            return

@rpc("any_peer", "call_remote", "reliable")
func _despawn(reason: String) -> void:
    if _despawn_started:
        return
    var mp := multiplayer
    if mp != null and mp.has_multiplayer_peer() and not is_multiplayer_authority():
        return
    _despawn_started = true
    is_despawning = true
    is_fading = false
    can_be_picked_up = false
    emit_signal("item_despawned", item_id, quantity, reason)
    if _audio_manager != null:
        var stream := _load_audio(DESPAWN_SOUND_PATH)
        if stream != null:
            _audio_manager.play_sfx_3d(stream, global_position, -12.0)
    _spawn_despawn_particles()
    _kill_fade_tween()
    _fade_tween = create_tween()
    if mesh_instance != null:
        _fade_tween.tween_property(mesh_instance, "modulate:a", 0.0, 0.5)
    _fade_tween.finished.connect(func():
        queue_free()
    )
    ErrorLogger.log_info("Item despawned (%s): %dx %s" % [reason, quantity, item_id], "DroppedItem")

func _spawn_pickup_particles() -> void:
    var particle := _create_particles(Color(1.0, 0.9, 0.5))
    if particle != null:
        particle.initial_velocity = 2.0
        particle.initial_velocity_random = 0.5
        particle.gravity = Vector3(0, -5, 0)
        particle.amount = 20
        particle.emitting = true

func _spawn_despawn_particles() -> void:
    var particle := _create_particles(Color(0.8, 0.8, 0.8))
    if particle != null:
        particle.initial_velocity = 1.0
        particle.initial_velocity_random = 0.3
        particle.gravity = Vector3(0, -2, 0)
        particle.amount = 12
        particle.emitting = true

func _start_despawn_fade() -> void:
    is_fading = true
    _kill_fade_tween()
    _fade_tween = create_tween()
    if mesh_instance != null:
        _fade_tween.tween_property(mesh_instance, "modulate:a", 0.3, DESPAWN_WARNING_TIME)

func _play_bounce_sound() -> void:
    var now := float(Time.get_ticks_msec()) * 0.001
    if now - _last_bounce_time < 0.2:
        return
    _last_bounce_time = now
    if _audio_manager != null:
        var stream := _load_audio(BOUNCE_SOUND_PATH)
        if stream != null:
            _audio_manager.play_sfx_3d(stream, global_position, -10.0)

func _create_particles(color: Color) -> CPUParticles3D:
    var particle := CPUParticles3D.new()
    particle.one_shot = true
    particle.preprocess = 0.0
    particle.lifetime = 0.5
    particle.explosiveness = 0.8
    particle.emission_shape = CPUParticles3D.EMISSION_SHAPE_SPHERE
    particle.emission_sphere_radius = 0.3
    var material := StandardMaterial3D.new()
    material.albedo_color = color
    material.emission_enabled = true
    material.emission = color * 1.5
    material.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
    particle.material_override = material
    add_child(particle)
    particle.finished.connect(func():
        if particle.is_inside_tree():
            particle.queue_free()
    )
    return particle

func _update_mesh_color() -> void:
    if mesh_instance == null:
        return
    var color := _get_color_for_item(item_id)
    mesh_instance.modulate = color

func _get_color_for_item(id: String) -> Color:
    if id.is_empty():
        return Color(0.8, 0.8, 0.8)
    var hash := 0
    for char in id:
        hash = (hash * 31 + int(char.unicode_at(0))) & 0xFFFFFF
    var hue := float(hash % 360) / 360.0
    return Color.from_hsv(hue, 0.6, 0.9)

func _kill_fade_tween() -> void:
    if _fade_tween != null and _fade_tween.is_valid():
        _fade_tween.kill()
    _fade_tween = null

func _load_audio(path: String) -> AudioStream:
    if path == "" or not ResourceLoader.exists(path):
        ErrorLogger.log_warning("Audio stream missing: %s" % path, "DroppedItem")
        return null
    var stream := ResourceLoader.load(path)
    if stream == null:
        ErrorLogger.log_warning("Failed to load audio stream: %s" % path, "DroppedItem")
    return stream

func _get_audio_manager() -> AudioManager:
    if typeof(AudioManager) != TYPE_NIL and AudioManager != null:
        return AudioManager
    var tree := get_tree()
    if tree != null and tree.has_node("/root/AudioManager"):
        return tree.get_node("/root/AudioManager")
    ErrorLogger.log_warning("AudioManager not available for DroppedItem", "DroppedItem")
    return null

func _get_game_manager() -> GameManager:
    if typeof(GameManager) != TYPE_NIL and GameManager != null:
        return GameManager
    var tree := get_tree()
    if tree != null and tree.has_node("/root/GameManager"):
        return tree.get_node("/root/GameManager")
    return null
