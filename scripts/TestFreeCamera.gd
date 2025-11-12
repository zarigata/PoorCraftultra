extends Camera3D
class_name TestFreeCamera

const MOVE_SPEED := 8.0
const FAST_MULTIPLIER := 2.5
const LOOK_SENSITIVITY := 0.005
const PITCH_LIMIT := deg_to_rad(89.0)

var _yaw := 0.0
var _pitch := 0.0
var _mouse_captured := false

func _ready() -> void:
	current = true
	var basis := global_transform.basis
	_yaw = rotation.y
	_pitch = rotation.x
	_capture_mouse(true)

func _exit_tree() -> void:
	_capture_mouse(false)

func _input(event: InputEvent) -> void:
	if event is InputEventMouseMotion and _mouse_captured:
		_yaw -= event.relative.x * LOOK_SENSITIVITY
		_pitch -= event.relative.y * LOOK_SENSITIVITY
		_pitch = clamp(_pitch, -PITCH_LIMIT, PITCH_LIMIT)
		rotation = Vector3(_pitch, _yaw, 0.0)
	elif event.is_action_pressed("ui_cancel"):
		_capture_mouse(!_mouse_captured)

func _process(delta: float) -> void:
	if not _mouse_captured:
		return
	var input_vector := Vector3.ZERO
	if Input.is_action_pressed("move_forward"):
		input_vector.z -= 1.0
	if Input.is_action_pressed("move_backward"):
		input_vector.z += 1.0
	if Input.is_action_pressed("move_left"):
		input_vector.x -= 1.0
	if Input.is_action_pressed("move_right"):
		input_vector.x += 1.0
	if Input.is_action_pressed("jump"):
		input_vector.y += 1.0
	if Input.is_action_pressed("crouch"):
		input_vector.y -= 1.0
	if input_vector == Vector3.ZERO:
		return
	input_vector = input_vector.normalized()
	var speed := MOVE_SPEED
	if Input.is_action_pressed("sprint"):
		speed *= FAST_MULTIPLIER
	global_translate((global_transform.basis * input_vector) * speed * delta)

func _capture_mouse(capture: bool) -> void:
	_mouse_captured = capture
	Input.set_mouse_mode(Input.MOUSE_MODE_CAPTURED if capture else Input.MOUSE_MODE_VISIBLE)
