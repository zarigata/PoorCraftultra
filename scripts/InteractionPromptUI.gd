extends CanvasLayer
class_name InteractionPromptUI

const FADE_DURATION := 0.15

@onready var prompt_panel: PanelContainer = $Container/PromptPanel
@onready var prompt_label: Label = $Container/PromptPanel/Margins/PromptLabel

var _interaction_manager: Node = null
var _ui_manager: Node = null
var _game_manager: Node = null
var _is_showing: bool = false
var _fade_tween: Tween = null
var _focused_prompt_source: Interactable = null

func _ready() -> void:
    layer = 45
    prompt_panel.visible = false
    _locate_autoloads()
    _connect_signals()
    _apply_theme()
    _register_with_ui_manager()
    _log_debug("InteractionPromptUI ready")

func _exit_tree() -> void:
    _disconnect_signals()
    _unregister_from_ui_manager()
    if _fade_tween != null and _fade_tween.is_running():
        _fade_tween.kill()
        _fade_tween = null

func _on_focused_changed(old_interactable: Interactable, new_interactable: Interactable) -> void:
    _detach_prompt_listener(old_interactable)
    if new_interactable == null:
        _focused_prompt_source = null
        _hide_prompt()
        return
    _attach_prompt_listener(new_interactable)
    var text := new_interactable.get_prompt_text() if new_interactable.has_method("get_prompt_text") else "[E]"
    if text.strip_edges() == "":
        _hide_prompt()
        return
    _show_prompt(text)
    _announce_screen_reader(new_interactable)

func _show_prompt(text: String) -> void:
    prompt_label.text = text
    if _is_showing:
        return
    _is_showing = true
    prompt_panel.visible = true
    prompt_panel.modulate.a = 0.0
    _start_fade(1.0)

func _hide_prompt() -> void:
    if not _is_showing:
        return
    _is_showing = false
    _start_fade(0.0, func():
        prompt_panel.visible = false
    )

func _start_fade(target_alpha: float, on_complete: Callable = Callable()) -> void:
    if _fade_tween != null and _fade_tween.is_running():
        _fade_tween.kill()
    _fade_tween = create_tween()
    _fade_tween.tween_property(prompt_panel, "modulate:a", target_alpha, FADE_DURATION)
    if on_complete != Callable():
        _fade_tween.finished.connect(on_complete)

func _apply_theme() -> void:
    if _ui_manager != null and _ui_manager.has_method("apply_theme_to_control"):
        _ui_manager.call("apply_theme_to_control", prompt_panel)

func _register_with_ui_manager() -> void:
    if _ui_manager != null and _ui_manager.has_method("register_ui"):
        _ui_manager.call("register_ui", self, "InteractionPromptUI")

func _unregister_from_ui_manager() -> void:
    if _ui_manager != null and _ui_manager.has_method("unregister_ui"):
        _ui_manager.call("unregister_ui", self)

func _locate_autoloads() -> void:
    if typeof(InteractionManager) != TYPE_NIL and InteractionManager != null:
        _interaction_manager = InteractionManager
    elif get_tree().has_node("/root/InteractionManager"):
        _interaction_manager = get_tree().get_node("/root/InteractionManager")
    if typeof(UIManager) != TYPE_NIL and UIManager != null:
        _ui_manager = UIManager
    elif get_tree().has_node("/root/UIManager"):
        _ui_manager = get_tree().get_node("/root/UIManager")
    if typeof(GameManager) != TYPE_NIL and GameManager != null:
        _game_manager = GameManager
    elif get_tree().has_node("/root/GameManager"):
        _game_manager = get_tree().get_node("/root/GameManager")

func _connect_signals() -> void:
    if _interaction_manager != null and _interaction_manager.has_signal("focused_interactable_changed"):
        _interaction_manager.focused_interactable_changed.connect(_on_focused_changed)
    if _game_manager != null and _game_manager.has_signal("game_paused"):
        if not _game_manager.game_paused.is_connected(_on_game_paused):
            _game_manager.game_paused.connect(_on_game_paused)

func _disconnect_signals() -> void:
    if _interaction_manager != null and _interaction_manager.has_signal("focused_interactable_changed") and _interaction_manager.focused_interactable_changed.is_connected(_on_focused_changed):
        _interaction_manager.focused_interactable_changed.disconnect(_on_focused_changed)
    if _game_manager != null and _game_manager.has_signal("game_paused") and _game_manager.game_paused.is_connected(_on_game_paused):
        _game_manager.game_paused.disconnect(_on_game_paused)
    _detach_prompt_listener(_focused_prompt_source)
    _focused_prompt_source = null

func _announce_screen_reader(interactable: Interactable) -> void:
    if _ui_manager == null:
        return
    if not _ui_manager.has_method("announce_to_screen_reader"):
        return
    var text := interactable.get_screen_reader_text() if interactable.has_method("get_screen_reader_text") else interactable.get_prompt_text()
    if text.strip_edges() == "":
        return
    _ui_manager.call("announce_to_screen_reader", text)

func _on_game_paused(paused: bool) -> void:
    if paused:
        _hide_prompt()

func _log_debug(message: String) -> void:
    if typeof(ErrorLogger) != TYPE_NIL and ErrorLogger != null:
        ErrorLogger.log_debug(message, "InteractionPromptUI")

func _attach_prompt_listener(interactable: Interactable) -> void:
    if interactable == null or not is_instance_valid(interactable):
        _focused_prompt_source = null
        return
    if interactable == _focused_prompt_source and interactable.prompt_changed.is_connected(_on_prompt_changed):
        return
    _detach_prompt_listener(_focused_prompt_source)
    _focused_prompt_source = interactable
    if not interactable.prompt_changed.is_connected(_on_prompt_changed):
        interactable.prompt_changed.connect(_on_prompt_changed)

func _detach_prompt_listener(interactable: Interactable) -> void:
    if interactable == null or not is_instance_valid(interactable):
        return
    if interactable.prompt_changed.is_connected(_on_prompt_changed):
        interactable.prompt_changed.disconnect(_on_prompt_changed)

func _on_prompt_changed(new_prompt: String) -> void:
    if new_prompt.strip_edges() == "":
        _hide_prompt()
        return
    _show_prompt(new_prompt)
