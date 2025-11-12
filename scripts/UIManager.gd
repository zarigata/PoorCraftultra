extends Node
class_name UIManager

signal theme_changed()
signal ui_registered(ui: CanvasLayer, layer: int)
signal ui_unregistered(ui: CanvasLayer)
signal transition_started(transition_type: String)
signal transition_completed(transition_type: String)
signal resolution_changed(new_size: Vector2)

enum TransitionType { FADE, SLIDE_LEFT, SLIDE_RIGHT, SLIDE_UP, SLIDE_DOWN }
enum ColorBlindMode { NONE, PROTANOPIA, DEUTERANOPIA, TRITANOPIA }

const THEME_CONFIG_PATH := "res://resources/ui_theme_config.json"
const DEFAULT_TRANSITION_DURATION := 0.3
const MIN_UI_SCALE := 0.75
const MAX_UI_SCALE := 1.5
const SAFE_AREA_MARGIN := 20.0
const MIN_TEXT_SCALE := 0.75
const MAX_TEXT_SCALE := 2.0
const COLOR_BLIND_MODE_KEY_MAP := {
	ColorBlindMode.NONE: "none",
	ColorBlindMode.PROTANOPIA: "protanopia",
	ColorBlindMode.DEUTERANOPIA: "deuteranopia",
	ColorBlindMode.TRITANOPIA: "tritanopia",
}

var theme_config: Dictionary = {}
var current_theme: Theme = null
var registered_uis: Dictionary = {}
var transition_overlay: ColorRect = null
var transition_overlay_layer: CanvasLayer = null
var ui_scale: float = 1.0
var color_blind_mode: ColorBlindMode = ColorBlindMode.NONE
var high_contrast_enabled: bool = false
var text_size_multiplier: float = 1.0
var safe_area_margin: float = SAFE_AREA_MARGIN
var _game_manager: GameManager = null
var _is_transitioning: bool = false
var _base_palette: Dictionary = {}

func _ready() -> void:
    ErrorLogger.log_info("UIManager initializing", "UIManager")
    _load_theme_config()
    if typeof(GameManager) != TYPE_NIL and GameManager != null:
        _game_manager = GameManager
        if not _game_manager.is_connected("settings_changed", Callable(self, "_on_settings_changed")):
            _game_manager.connect("settings_changed", Callable(self, "_on_settings_changed"))
        _load_ui_settings()
    else:
        ErrorLogger.log_warning("GameManager not available; using default UI settings", "UIManager")
    _build_theme()
    _create_transition_overlay()
    var viewport := get_viewport()
    if viewport != null and not viewport.is_connected("size_changed", Callable(self, "_on_viewport_size_changed")):
        viewport.connect("size_changed", Callable(self, "_on_viewport_size_changed"))
    _update_transition_overlay_size()
    ErrorLogger.log_info("UIManager ready", "UIManager")

func get_theme() -> Theme:
    return current_theme

func reload_theme() -> void:
    _load_theme_config()
    _build_theme()
    emit_signal("theme_changed")
    ErrorLogger.log_info("UI theme reloaded", "UIManager")

func apply_theme_to_control(node: Node) -> void:
    if node == null:
        return
    if current_theme == null:
        ErrorLogger.log_warning("No active theme to apply", "UIManager")
        return
    _apply_theme_recursive(node)

func get_color(color_name: String) -> Color:
    if not _base_palette.has(color_name):
        ErrorLogger.log_warning("Color %s not found in theme palette" % color_name, "UIManager")
        return Color(1, 1, 1, 1)
    var color: Color = _base_palette[color_name]
    color = _apply_color_blind_palette(color_name, color)
    color = _apply_high_contrast_transformation(color_name, color)
    return color

func get_font_size(size_name: String) -> int:
    var fonts_config := theme_config.get("fonts", {})
    var key := "%s_size" % size_name
    if not fonts_config.has(key):
        ErrorLogger.log_warning("Font size %s not found; using default" % size_name, "UIManager")
        return _scale_font_size(fonts_config.get("default_size", 14))
    return _scale_font_size(int(fonts_config[key]))

func register_ui(ui: CanvasLayer, ui_name: String = "") -> void:
    if ui == null or not (ui is CanvasLayer):
        ErrorLogger.log_warning("Attempted to register invalid UI instance", "UIManager")
        return
    registered_uis[ui] = {
        "layer": ui.layer,
        "name": ui_name
    }
    emit_signal("ui_registered", ui, ui.layer)
    ErrorLogger.log_info("UI registered: %s (layer %s)" % [ui_name if ui_name != "" else ui.name, ui.layer], "UIManager")

func get_ui(ui_name: String) -> CanvasLayer:
    for ui in registered_uis.keys():
        if not is_instance_valid(ui):
            continue
        var info: Dictionary = registered_uis.get(ui, {})
        var registered_name := String(info.get("name", ""))
        if ui_name != "" and registered_name == ui_name:
            return ui
    for ui in registered_uis.keys():
        if is_instance_valid(ui) and ui.name == ui_name:
            return ui
    return null

func unregister_ui(ui: CanvasLayer) -> void:
    if ui == null:
        return
    if registered_uis.erase(ui):
        emit_signal("ui_unregistered", ui)
        ErrorLogger.log_info("UI unregistered: %s" % ui.name, "UIManager")

func get_registered_uis() -> Array[CanvasLayer]:
    var entries: Array = registered_uis.keys()
    entries.sort_custom(Callable(self, "_sort_canvas_layers"))
    return entries

func hide_all_uis(except: Array[CanvasLayer] = []) -> void:
    for ui in registered_uis.keys():
        if ui in except:
            continue
        if is_instance_valid(ui):
            ui.visible = false
    ErrorLogger.log_debug("All UIs hidden (except %d)" % except.size(), "UIManager")

func show_all_uis() -> void:
    for ui in registered_uis.keys():
        if is_instance_valid(ui):
            ui.visible = true
    ErrorLogger.log_debug("All UIs shown", "UIManager")

func transition_to_scene(scene_path: String, transition_type: TransitionType = TransitionType.FADE, duration: float = DEFAULT_TRANSITION_DURATION) -> void:
    if _is_transitioning:
        ErrorLogger.log_warning("Transition already in progress", "UIManager")
        return
    if duration <= 0.0:
        duration = DEFAULT_TRANSITION_DURATION
    if not ResourceLoader.exists(scene_path):
        ErrorLogger.log_error("Scene does not exist: %s" % scene_path, "UIManager")
        return
    _is_transitioning = true
    var transition_name := _transition_type_to_string(transition_type)
    emit_signal("transition_started", transition_name)
    match transition_type:
        TransitionType.FADE:
            await fade_out(duration)
        TransitionType.SLIDE_LEFT, TransitionType.SLIDE_RIGHT, TransitionType.SLIDE_UP, TransitionType.SLIDE_DOWN:
            await slide_transition(transition_type, duration, true)
    get_tree().change_scene_to_file(scene_path)
    match transition_type:
        TransitionType.FADE:
            await fade_in(duration)
        TransitionType.SLIDE_LEFT, TransitionType.SLIDE_RIGHT, TransitionType.SLIDE_UP, TransitionType.SLIDE_DOWN:
            await slide_transition(transition_type, duration, false)
    emit_signal("transition_completed", transition_name)
    _is_transitioning = false

func fade_out(duration: float = DEFAULT_TRANSITION_DURATION) -> void:
    if transition_overlay == null:
        return
    _set_transition_overlay_active(true)
    transition_overlay.modulate.a = 0.0
    transition_overlay.color = Color.BLACK
    await _play_fade_tween(0.0, 1.0, duration)

func fade_in(duration: float = DEFAULT_TRANSITION_DURATION) -> void:
    if transition_overlay == null:
        return
    _set_transition_overlay_active(true)
    transition_overlay.modulate.a = 1.0
    await _play_fade_tween(1.0, 0.0, duration)
    _set_transition_overlay_active(false)

func slide_transition(direction: TransitionType, duration: float = DEFAULT_TRANSITION_DURATION, entering: bool = true) -> void:
    if transition_overlay == null:
        return
    _set_transition_overlay_active(true)
    transition_overlay.modulate.a = 1.0
    transition_overlay.color = Color.BLACK
    await _play_slide_tween(direction, duration, entering)
    if not entering:
        transition_overlay.modulate.a = 0.0
        _set_transition_overlay_active(false)

func set_ui_scale(scale: float) -> void:
    var clamped := clampf(scale, MIN_UI_SCALE, MAX_UI_SCALE)
    if is_equal_approx(ui_scale, clamped):
        return
    ui_scale = clamped
    _build_theme()
    emit_signal("theme_changed")
    _persist_setting("ui/scale", ui_scale)
    ErrorLogger.log_info("UI scale set to %.2f" % ui_scale, "UIManager")

func get_ui_scale() -> float:
    return ui_scale

func get_safe_area_rect() -> Rect2:
    var viewport_rect := get_viewport().get_visible_rect()
    var margin := max(safe_area_margin, 0.0)
    var inset_position := viewport_rect.position + Vector2(margin, margin)
    var inset_size := viewport_rect.size - Vector2(margin * 2.0, margin * 2.0)
    inset_size.x = max(inset_size.x, 0.0)
    inset_size.y = max(inset_size.y, 0.0)
    return Rect2(inset_position, inset_size)

func apply_scale_to_control(control: Control) -> void:
    if control == null:
        return
    control.scale = Vector2.ONE * ui_scale

func set_color_blind_mode(mode: ColorBlindMode) -> void:
    var new_mode := ColorBlindMode.NONE
    match int(mode):
        ColorBlindMode.PROTANOPIA:
            new_mode = ColorBlindMode.PROTANOPIA
        ColorBlindMode.DEUTERANOPIA:
            new_mode = ColorBlindMode.DEUTERANOPIA
        ColorBlindMode.TRITANOPIA:
            new_mode = ColorBlindMode.TRITANOPIA
        _:
            new_mode = ColorBlindMode.NONE
    if color_blind_mode == new_mode:
        return
    color_blind_mode = new_mode
    _build_theme()
    emit_signal("theme_changed")
    _persist_setting("ui/color_blind_mode", int(color_blind_mode))
    ErrorLogger.log_info("Color blind mode set to %s" % _color_blind_mode_to_key(color_blind_mode), "UIManager")

func get_color_blind_mode() -> ColorBlindMode:
    return color_blind_mode

func set_high_contrast(enabled: bool) -> void:
    if high_contrast_enabled == enabled:
        return
    high_contrast_enabled = enabled
    _build_theme()
    emit_signal("theme_changed")
    _persist_setting("ui/high_contrast", high_contrast_enabled)
    ErrorLogger.log_info("High contrast mode %s" % ("enabled" if enabled else "disabled"), "UIManager")

func set_text_size_multiplier(multiplier: float) -> void:
    var clamped := clampf(multiplier, MIN_TEXT_SCALE, MAX_TEXT_SCALE)
    if is_equal_approx(text_size_multiplier, clamped):
        return
    text_size_multiplier = clamped
    _build_theme()
    emit_signal("theme_changed")
    _persist_setting("ui/text_size", text_size_multiplier)
    ErrorLogger.log_info("Text size multiplier set to %.2f" % text_size_multiplier, "UIManager")

func announce_to_screen_reader(text: String) -> void:
    ErrorLogger.log_debug("Screen reader announcement: %s" % text, "UIManager")

func _load_theme_config() -> void:
    var file := FileAccess.open(THEME_CONFIG_PATH, FileAccess.READ)
    if file == null:
        ErrorLogger.log_warning("Theme config missing; using defaults", "UIManager")
        theme_config = _get_default_theme_config()
        return
    var content := file.get_as_text()
    file.close()
    var json := JSON.new()
    var error := json.parse(content)
    if error != OK:
        ErrorLogger.log_error("Failed to parse theme config (code %d)" % error, "UIManager")
        theme_config = _get_default_theme_config()
        return
    var data := json.get_data()
    if typeof(data) != TYPE_DICTIONARY:
        ErrorLogger.log_error("Theme config has invalid structure", "UIManager")
        theme_config = _get_default_theme_config()
        return
    theme_config = data

func _get_default_theme_config() -> Dictionary:
    return {
        "version": 1,
        "palette": {
            "primary": {"r": 0.2, "g": 0.3, "b": 0.4, "a": 1.0},
            "secondary": {"r": 0.3, "g": 0.4, "b": 0.5, "a": 1.0},
            "accent": {"r": 0.4, "g": 0.7, "b": 0.9, "a": 1.0},
            "background": {"r": 0.15, "g": 0.15, "b": 0.2, "a": 0.85},
            "panel": {"r": 0.2, "g": 0.2, "b": 0.25, "a": 0.9},
            "text": {"r": 0.9, "g": 0.9, "b": 0.95, "a": 1.0},
            "text_dim": {"r": 0.6, "g": 0.6, "b": 0.65, "a": 1.0},
            "success": {"r": 0.4, "g": 0.8, "b": 0.4, "a": 1.0},
            "warning": {"r": 0.9, "g": 0.7, "b": 0.3, "a": 1.0},
            "error": {"r": 0.9, "g": 0.3, "b": 0.3, "a": 1.0},
            "highlight": {"r": 0.5, "g": 0.7, "b": 1.0, "a": 0.3}
        },
        "fonts": {
            "default_size": 14,
            "title_size": 20,
            "small_size": 12,
            "large_size": 18,
            "use_system_font": true,
            "custom_font_path": ""
        },
        "panels": {
            "corner_radius": 0,
            "border_width": 2,
            "border_color": {"r": 0.3, "g": 0.4, "b": 0.5, "a": 1.0},
            "shadow_enabled": false,
            "content_margin": 12
        },
        "buttons": {
            "normal_bg": {"r": 0.25, "g": 0.3, "b": 0.35, "a": 1.0},
            "hover_bg": {"r": 0.3, "g": 0.4, "b": 0.5, "a": 1.0},
            "pressed_bg": {"r": 0.2, "g": 0.25, "b": 0.3, "a": 1.0},
            "disabled_bg": {"r": 0.2, "g": 0.2, "b": 0.2, "a": 0.5},
            "corner_radius": 0,
            "border_width": 1
        },
        "accessibility": {
            "color_blind_palettes": {
                "protanopia": {
                    "success": {"r": 0.3, "g": 0.6, "b": 0.9, "a": 1.0},
                    "warning": {"r": 0.9, "g": 0.7, "b": 0.3, "a": 1.0},
                    "error": {"r": 0.9, "g": 0.5, "b": 0.1, "a": 1.0},
                    "primary": {"r": 0.25, "g": 0.45, "b": 0.75, "a": 1.0},
                    "secondary": {"r": 0.45, "g": 0.6, "b": 0.8, "a": 1.0},
                    "accent": {"r": 0.95, "g": 0.8, "b": 0.35, "a": 1.0},
                    "panel": {"r": 0.18, "g": 0.2, "b": 0.28, "a": 0.95}
                },
                "deuteranopia": {
                    "success": {"r": 0.3, "g": 0.6, "b": 0.9, "a": 1.0},
                    "warning": {"r": 0.9, "g": 0.7, "b": 0.3, "a": 1.0},
                    "error": {"r": 0.9, "g": 0.5, "b": 0.1, "a": 1.0},
                    "primary": {"r": 0.2, "g": 0.5, "b": 0.75, "a": 1.0},
                    "secondary": {"r": 0.35, "g": 0.55, "b": 0.78, "a": 1.0},
                    "accent": {"r": 0.95, "g": 0.78, "b": 0.45, "a": 1.0},
                    "panel": {"r": 0.18, "g": 0.21, "b": 0.27, "a": 0.95}
                },
                "tritanopia": {
                    "success": {"r": 0.0, "g": 0.8, "b": 0.8, "a": 1.0},
                    "warning": {"r": 0.9, "g": 0.3, "b": 0.5, "a": 1.0},
                    "error": {"r": 0.9, "g": 0.1, "b": 0.1, "a": 1.0},
                    "primary": {"r": 0.35, "g": 0.5, "b": 0.85, "a": 1.0},
                    "secondary": {"r": 0.5, "g": 0.65, "b": 0.9, "a": 1.0},
                    "accent": {"r": 0.95, "g": 0.65, "b": 0.35, "a": 1.0},
                    "panel": {"r": 0.16, "g": 0.2, "b": 0.3, "a": 0.95}
                }
            },
            "high_contrast": {
                "background": {"r": 0.0, "g": 0.0, "b": 0.0, "a": 1.0},
                "text": {"r": 1.0, "g": 1.0, "b": 1.0, "a": 1.0},
                "border_width": 3
            }
        }
    }

func _build_theme() -> void:
    _base_palette.clear()
    var palette_config := theme_config.get("palette", {})
    for key in palette_config.keys():
        _base_palette[key] = _dictionary_to_color(palette_config[key])
    var theme := Theme.new()
    var panel_style := _create_panel_stylebox()
    theme.set_stylebox("panel", "Panel", panel_style)
    theme.set_stylebox("panel", "PanelContainer", panel_style)
    theme.set_stylebox("panel", "NinePatchRect", panel_style)
    theme.set_stylebox("panel", "PopupMenu", panel_style)
    theme.set_stylebox("panel", "MenuButton", panel_style)
    theme.set_stylebox("panel", "OptionButton", panel_style)
    theme.set_color("font_color", "Label", get_color("text"))
    theme.set_color("font_color", "RichTextLabel", get_color("text"))
    theme.set_color("font_color", "Button", get_color("text"))
    theme.set_color("font_color_disabled", "Button", get_color("text_dim"))
    theme.set_color("font_color", "LineEdit", get_color("text"))
    theme.set_color("placeholder_color", "LineEdit", get_color("text_dim"))
    theme.set_color("font_color", "TextEdit", get_color("text"))
    theme.set_color("font_color", "ProgressBar", get_color("text"))
    var progress_fg := _create_progress_fg_stylebox()
    var progress_bg := _create_progress_bg_stylebox()
    theme.set_stylebox("fg", "ProgressBar", progress_fg)
    theme.set_stylebox("bg", "ProgressBar", progress_bg)
    var button_normal := _create_button_stylebox("normal")
    var button_hover := _create_button_stylebox("hover")
    var button_pressed := _create_button_stylebox("pressed")
    var button_disabled := _create_button_stylebox("disabled")
    theme.set_stylebox("normal", "Button", button_normal)
    theme.set_stylebox("hover", "Button", button_hover)
    theme.set_stylebox("pressed", "Button", button_pressed)
    theme.set_stylebox("disabled", "Button", button_disabled)
    theme.set_stylebox("focus", "Button", button_hover)
    var focus_color := get_color("highlight")
    theme.set_color("focus", "Button", focus_color)
    theme.set_stylebox("normal", "MenuButton", button_normal)
    theme.set_stylebox("hover", "MenuButton", button_hover)
    theme.set_stylebox("pressed", "MenuButton", button_pressed)
    theme.set_stylebox("disabled", "MenuButton", button_disabled)
    theme.set_stylebox("focus", "MenuButton", button_hover)
    theme.set_color("focus", "MenuButton", focus_color)
    theme.set_stylebox("normal", "OptionButton", button_normal)
    theme.set_stylebox("hover", "OptionButton", button_hover)
    theme.set_stylebox("pressed", "OptionButton", button_pressed)
    theme.set_stylebox("disabled", "OptionButton", button_disabled)
    theme.set_stylebox("focus", "OptionButton", button_hover)
    theme.set_color("focus", "OptionButton", focus_color)
    theme.set_constant("outline_size", "Button", int(theme_config.get("buttons", {}).get("border_width", 1)))
    theme.set_constant("outline_size", "MenuButton", int(theme_config.get("buttons", {}).get("border_width", 1)))
    theme.set_constant("outline_size", "OptionButton", int(theme_config.get("buttons", {}).get("border_width", 1)))
    var slider_style := _create_slider_stylebox()
    theme.set_stylebox("grabber", "HSlider", slider_style)
    theme.set_stylebox("grabber", "VSlider", slider_style)
    theme.set_stylebox("grabber_disabled", "HSlider", slider_style)
    theme.set_stylebox("grabber_disabled", "VSlider", slider_style)
    var scrollbar_style := _create_scrollbar_stylebox()
    theme.set_stylebox("scroll", "ScrollBar", scrollbar_style)
    theme.set_stylebox("grabber", "ScrollBar", slider_style)
    theme.set_color("font_color", "PopupMenu", get_color("text"))
    theme.set_color("font_color", "MenuButton", get_color("text"))
    theme.set_color("font_color", "OptionButton", get_color("text"))
    theme.set_color("font_color", "ScrollBar", get_color("text"))
    theme.set_color("font_color", "HSlider", get_color("text"))
    theme.set_color("font_color", "VSlider", get_color("text"))
    _apply_fonts_to_theme(theme)
    current_theme = theme

func _create_panel_stylebox() -> StyleBoxFlat:
    var panel_config := theme_config.get("panels", {})
    var style := StyleBoxFlat.new()
    style.bg_color = get_color("panel")
    style.corner_radius_top_left = int(panel_config.get("corner_radius", 0))
    style.corner_radius_top_right = int(panel_config.get("corner_radius", 0))
    style.corner_radius_bottom_left = int(panel_config.get("corner_radius", 0))
    style.corner_radius_bottom_right = int(panel_config.get("corner_radius", 0))
    var border_width := panel_config.get("border_width", 2)
    style.border_width_left = border_width
    style.border_width_top = border_width
    style.border_width_right = border_width
    style.border_width_bottom = border_width
    style.border_color = _apply_high_contrast_transformation("panel_border", _dictionary_to_color(panel_config.get("border_color", {"r": 0.3, "g": 0.4, "b": 0.5, "a": 1.0})))
    style.shadow_enabled = panel_config.get("shadow_enabled", false)
    var margin := panel_config.get("content_margin", 12)
    style.content_margin_left = margin
    style.content_margin_top = margin
    style.content_margin_right = margin
    style.content_margin_bottom = margin
    return style

func _create_button_stylebox(state: String) -> StyleBoxFlat:
    var button_config := theme_config.get("buttons", {})
    var style := StyleBoxFlat.new()
    var key := "normal_bg"
    match state:
        "hover":
            key = "hover_bg"
        "pressed":
            key = "pressed_bg"
        "disabled":
            key = "disabled_bg"
        _:
            key = "normal_bg"
    var color_dict := button_config.get(key, {"r": 0.25, "g": 0.3, "b": 0.35, "a": 1.0})
    style.bg_color = _apply_high_contrast_transformation("button_%s" % state, _dictionary_to_color(color_dict))
    var radius := int(button_config.get("corner_radius", 0))
    style.corner_radius_top_left = radius
    style.corner_radius_top_right = radius
    style.corner_radius_bottom_left = radius
    style.corner_radius_bottom_right = radius
    var border_width := int(button_config.get("border_width", 1))
    if high_contrast_enabled:
        var accessibility := theme_config.get("accessibility", {})
        var high_contrast := accessibility.get("high_contrast", {})
        var contrast_border := int(high_contrast.get("border_width", 3))
        border_width = max(border_width, contrast_border, 2)
    style.border_width_left = border_width
    style.border_width_top = border_width
    style.border_width_right = border_width
    style.border_width_bottom = border_width
    style.border_color = high_contrast_enabled ? get_color("text") : get_color("accent")
    return style

func _create_progress_fg_stylebox() -> StyleBoxFlat:
    var style := StyleBoxFlat.new()
    style.bg_color = get_color("accent")
    var radius := int(theme_config.get("buttons", {}).get("corner_radius", 0))
    style.corner_radius_top_left = radius
    style.corner_radius_top_right = radius
    style.corner_radius_bottom_left = radius
    style.corner_radius_bottom_right = radius
    return style

func _create_progress_bg_stylebox() -> StyleBoxFlat:
    var style := StyleBoxFlat.new()
    var panel_color := get_color("panel")
    style.bg_color = panel_color.darkened(0.15)
    style.border_color = panel_color
    style.border_width_left = 1
    style.border_width_top = 1
    style.border_width_right = 1
    style.border_width_bottom = 1
    var radius := int(theme_config.get("buttons", {}).get("corner_radius", 0))
    style.corner_radius_top_left = radius
    style.corner_radius_top_right = radius
    style.corner_radius_bottom_left = radius
    style.corner_radius_bottom_right = radius
    return style

func _apply_fonts_to_theme(theme: Theme) -> void:
    var fonts_config := theme_config.get("fonts", {})
    var base_font: Font = null
    var custom_path := String(fonts_config.get("custom_font_path", ""))
    var use_system_font := fonts_config.get("use_system_font", true)
    if custom_path != "":
        var resource := load(custom_path)
        if resource is Font:
            base_font = resource
        else:
            ErrorLogger.log_warning("Custom font not found: %s" % custom_path, "UIManager")
    if base_font == null and use_system_font:
        base_font = ThemeDB.get_default_theme().get_font("font", "Label")
    if base_font == null:
        ErrorLogger.log_warning("No base font available; using default theme font", "UIManager")
        return
    var default_font := _create_font_variation(base_font, fonts_config.get("default_size", 14))
    var title_font := _create_font_variation(base_font, fonts_config.get("title_size", 20))
    var small_font := _create_font_variation(base_font, fonts_config.get("small_size", 12))
    var large_font := _create_font_variation(base_font, fonts_config.get("large_size", 18))
    theme.set_font("font", "Control", default_font)
    theme.set_font("font", "Label", default_font)
    theme.set_font("font", "Button", default_font)
    theme.set_font("font", "LineEdit", default_font)
    theme.set_font("font", "TextEdit", default_font)
    theme.set_font("font", "RichTextLabel", default_font)
    theme.set_font("font", "ProgressBar", default_font)
    theme.set_font("font_title", "Control", title_font)
    theme.set_font("font_small", "Control", small_font)
    theme.set_font("font_large", "Control", large_font)

func _create_font_variation(base_font: Font, size: int) -> Font:
    var variation := FontVariation.new()
    variation.base_font = base_font
    variation.size = _scale_font_size(size)
    return variation

func _apply_theme_recursive(node: Node) -> void:
    if node is Control:
        var control := node as Control
        control.theme = current_theme
    for child in node.get_children():
        if child is Node:
            _apply_theme_recursive(child)

func _apply_color_blind_palette(color_name: String, source: Color) -> Color:
    if color_blind_mode == ColorBlindMode.NONE:
        return source
    var accessibility := theme_config.get("accessibility", {})
    var palettes := accessibility.get("color_blind_palettes", {})
    var mode_key := _color_blind_mode_to_key(color_blind_mode)
    if palettes.has(mode_key) and palettes[mode_key].has(color_name):
        return _dictionary_to_color(palettes[mode_key][color_name])
    return source

func _apply_high_contrast_transformation(color_name: String, source: Color) -> Color:
    if not high_contrast_enabled:
        return source
    var accessibility := theme_config.get("accessibility", {})
    var high_contrast := accessibility.get("high_contrast", {})
    var background_color := high_contrast.get("background", {"r": 0, "g": 0, "b": 0, "a": 1})
    var text_color := high_contrast.get("text", {"r": 1, "g": 1, "b": 1, "a": 1})
    if color_name.find("text") != -1:
        return _dictionary_to_color(text_color)
    if color_name.find("border") != -1:
        return _dictionary_to_color(text_color)
    if color_name == "accent":
        return _dictionary_to_color(text_color)
    return _dictionary_to_color(background_color)

func _scale_font_size(base_size: int) -> int:
    var scaled := float(base_size) * ui_scale * text_size_multiplier
    return int(clampf(round(scaled), 8.0, 48.0))

func _dictionary_to_color(data: Dictionary) -> Color:
    return Color(data.get("r", 1.0), data.get("g", 1.0), data.get("b", 1.0), data.get("a", 1.0))

func _create_transition_overlay() -> void:
    if transition_overlay_layer != null and is_instance_valid(transition_overlay_layer):
        return
    transition_overlay_layer = CanvasLayer.new()
    transition_overlay_layer.layer = 200
    transition_overlay_layer.follow_viewport_enabled = true
    add_child(transition_overlay_layer)
    transition_overlay = ColorRect.new()
    transition_overlay.color = Color.BLACK
    transition_overlay.position = Vector2.ZERO
    transition_overlay.custom_minimum_size = get_viewport().get_visible_rect().size
    transition_overlay.size = transition_overlay.custom_minimum_size
    transition_overlay_layer.add_child(transition_overlay)
    transition_overlay_layer.visible = false
    transition_overlay.mouse_filter = Control.MOUSE_FILTER_STOP
    transition_overlay.focus_mode = Control.FOCUS_NONE
    transition_overlay.modulate = Color(1, 1, 1, 0)

func _set_transition_overlay_active(active: bool) -> void:
    if transition_overlay == null or transition_overlay_layer == null:
        return
    transition_overlay_layer.visible = active
    if active:
        transition_overlay.focus_mode = Control.FOCUS_ALL
        transition_overlay.grab_focus()
    else:
        if transition_overlay.has_focus():
            transition_overlay.release_focus()
        transition_overlay.focus_mode = Control.FOCUS_NONE

func _play_fade_tween(from_alpha: float, to_alpha: float, duration: float) -> void:
    var tween := create_tween()
    transition_overlay.modulate.a = from_alpha
    tween.tween_property(transition_overlay, "modulate:a", to_alpha, max(duration, 0.01)).set_trans(Tween.TRANS_SINE).set_ease(Tween.EASE_IN_OUT)
    await tween.finished

func _play_slide_tween(direction: TransitionType, duration: float, entering: bool) -> void:
    var viewport_size := get_viewport().get_visible_rect().size
    transition_overlay.custom_minimum_size = viewport_size
    transition_overlay.size = viewport_size
    var offset := _get_slide_offset(direction, viewport_size)
    var from_pos := Vector2.ZERO
    var to_pos := Vector2.ZERO
    if entering:
        from_pos = offset
        to_pos = Vector2.ZERO
    else:
        from_pos = Vector2.ZERO
        to_pos = offset
    transition_overlay.position = from_pos
    var tween := create_tween()
    tween.tween_property(transition_overlay, "position", to_pos, max(duration, 0.01)).set_trans(Tween.TRANS_CUBIC).set_ease(Tween.EASE_IN_OUT)
    await tween.finished

func _get_slide_offset(direction: TransitionType, viewport_size: Vector2) -> Vector2:
    match direction:
        TransitionType.SLIDE_LEFT:
            return Vector2(viewport_size.x, 0)
        TransitionType.SLIDE_RIGHT:
            return Vector2(-viewport_size.x, 0)
        TransitionType.SLIDE_UP:
            return Vector2(0, viewport_size.y)
        TransitionType.SLIDE_DOWN:
            return Vector2(0, -viewport_size.y)
        _:
            return Vector2.ZERO

func _on_settings_changed(setting_name: String, new_value: Variant) -> void:
    if not setting_name.begins_with("ui/"):
        return
    match setting_name:
        "ui/scale":
            if not is_equal_approx(float(new_value), ui_scale):
                set_ui_scale(float(new_value))
        "ui/color_blind_mode":
            var normalized_mode := _normalize_color_blind_mode(int(new_value))
            if normalized_mode != color_blind_mode:
                set_color_blind_mode(normalized_mode)
        "ui/high_contrast":
            if bool(new_value) != high_contrast_enabled:
                set_high_contrast(bool(new_value))
        "ui/text_size":
            if not is_equal_approx(float(new_value), text_size_multiplier):
                set_text_size_multiplier(float(new_value))
        "ui/safe_area_margin":
            var margin := max(float(new_value), 0.0)
            if not is_equal_approx(safe_area_margin, margin):
                safe_area_margin = margin
                _persist_setting("ui/safe_area_margin", safe_area_margin)
        _:
            pass

func _load_ui_settings() -> void:
    if _game_manager == null:
        return
    ui_scale = clampf(float(_game_manager.get_setting("ui/scale", ui_scale)), MIN_UI_SCALE, MAX_UI_SCALE)
    color_blind_mode = _normalize_color_blind_mode(int(_game_manager.get_setting("ui/color_blind_mode", color_blind_mode)))
    high_contrast_enabled = bool(_game_manager.get_setting("ui/high_contrast", high_contrast_enabled))
    text_size_multiplier = clampf(float(_game_manager.get_setting("ui/text_size", text_size_multiplier)), MIN_TEXT_SCALE, MAX_TEXT_SCALE)
    safe_area_margin = max(float(_game_manager.get_setting("ui/safe_area_margin", SAFE_AREA_MARGIN)), 0.0)
    ErrorLogger.log_info("UI settings loaded (scale=%.2f, text=%.2f, cb=%s, hc=%s)" % [ui_scale, text_size_multiplier, _color_blind_mode_to_key(color_blind_mode), str(high_contrast_enabled)], "UIManager")

func _on_viewport_size_changed() -> void:
    _update_transition_overlay_size()
    emit_signal("resolution_changed", get_viewport().get_visible_rect().size)
    ErrorLogger.log_debug("Viewport size changed", "UIManager")

func _update_transition_overlay_size() -> void:
    if transition_overlay == null:
        return
    var viewport_size := get_viewport().get_visible_rect().size
    transition_overlay.custom_minimum_size = viewport_size
    transition_overlay.size = viewport_size

func _sort_canvas_layers(a: CanvasLayer, b: CanvasLayer) -> bool:
    var layer_a := 0
    var layer_b := 0
    if is_instance_valid(a):
        layer_a = a.layer
    if is_instance_valid(b):
        layer_b = b.layer
    if layer_a == layer_b:
        return a.get_instance_id() < b.get_instance_id()
    return layer_a < layer_b

func _persist_setting(setting_name: String, value: Variant) -> void:
    if _game_manager == null:
        return
    if _game_manager.get_setting(setting_name, value) == value:
        return
    _game_manager.set_setting(setting_name, value)

func _transition_type_to_string(transition_type: TransitionType) -> String:
    match transition_type:
        TransitionType.FADE:
            return "fade"
        TransitionType.SLIDE_LEFT:
            return "slide_left"
        TransitionType.SLIDE_RIGHT:
            return "slide_right"
        TransitionType.SLIDE_UP:
            return "slide_up"
        TransitionType.SLIDE_DOWN:
            return "slide_down"
        _:
            return "unknown"

func _color_blind_mode_to_key(mode: ColorBlindMode) -> String:
    return COLOR_BLIND_MODE_KEY_MAP.get(mode, "none")

func _normalize_color_blind_mode(value: int) -> int:
    return clamp(value, ColorBlindMode.NONE, ColorBlindMode.TRITANOPIA)
