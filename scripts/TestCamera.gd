extends Camera3D
class_name TestCamera

@export var move_speed: float = 10.0
@export var fast_multiplier: float = 2.0
@export var look_sensitivity: float = 0.005
@export var pitch_limit_degrees: float = 89.0

var _yaw: float = 0.0
var _pitch: float = 0.0
var _mouse_captured: bool = false

func _ready() -> void:
	current = true
	_yaw = rotation.y
	_pitch = rotation.x
	_capture_mouse(true)

func _exit_tree() -> void:
	_capture_mouse(false)

func _input(event: InputEvent) -> void:
	if event is InputEventMouseMotion and _mouse_captured:
		_yaw -= event.relative.x * look_sensitivity
		_pitch -= event.relative.y * look_sensitivity
		var pitch_limit := deg_to_rad(pitch_limit_degrees)
		_pitch = clamp(_pitch, -pitch_limit, pitch_limit)
		rotation = Vector3(_pitch, _yaw, 0.0)
	elif event.is_action_pressed("toggle_mouse_capture"):
		_capture_mouse(!_mouse_captured)

func _physics_process(delta: float) -> void:
	if not _mouse_captured:
		return
	var direction := Vector3.ZERO
	var basis := global_transform.basis
	if Input.is_action_pressed("move_forward"):
		direction -= basis.z
	if Input.is_action_pressed("move_backward"):
		direction += basis.z
	if Input.is_action_pressed("move_left"):
		direction -= basis.x
	if Input.is_action_pressed("move_right"):
		direction += basis.x
	if Input.is_action_pressed("jump") or Input.is_action_pressed("fly_up"):
		direction += Vector3.UP
	if Input.is_action_pressed("fly_down"):
		direction -= Vector3.UP
	if direction == Vector3.ZERO:
		return
	direction = direction.normalized()
	var speed := move_speed
	if Input.is_action_pressed("sprint"):
		speed *= fast_multiplier
	global_position += direction * speed * delta

func _capture_mouse(enable: bool) -> void:
	_mouse_captured = enable
	Input.set_mouse_mode(Input.MOUSE_MODE_CAPTURED if enable else Input.MOUSE_MODE_VISIBLE)
