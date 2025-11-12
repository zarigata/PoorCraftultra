extends Panel
class_name InventorySlot

signal slot_gui_input(event: InputEvent)

var slot_index: int = -1
var is_hotbar: bool = false
var item_id: String = ""
var quantity: int = 0
var owner_ui: InventoryUI = null

@onready var icon: TextureRect = $Icon
@onready var quantity_label: Label = $QuantityLabel
@onready var highlight: Panel = $Highlight
@onready var hotkey_label: Label = $HotkeyLabel

func _ready() -> void:
	mouse_filter = Control.MOUSE_FILTER_STOP
	_clear_visuals()

func set_owner(ui: InventoryUI) -> void:
	owner_ui = ui

const PLACEHOLDER_ICON := preload("res://assets/icons/items/_placeholder.png")

func set_slot_data(new_item_id: String, new_quantity: int, resource: GameResource, icon_texture: Texture2D, show_tooltip: bool) -> void:
	item_id = new_item_id
	quantity = max(new_quantity, 0)
	var texture_to_use := icon_texture if icon_texture != null else PLACEHOLDER_ICON
	icon.texture = texture_to_use
	var is_placeholder := texture_to_use == PLACEHOLDER_ICON and (icon_texture == null or icon_texture == PLACEHOLDER_ICON)
	icon.self_modulate = Color(1, 1, 1, 0.35) if is_placeholder else Color(1, 1, 1, 1)

	quantity_label.text = quantity > 1 ? str(quantity) : ""
	quantity_label.visible = quantity > 1

	if show_tooltip and resource != null:
		var tooltip := resource.display_name
		if not resource.description.is_empty():
			tooltip += "\n" + resource.description
		if resource.weight > 0.0:
			tooltip += "\nWeight: %.2f" % resource.weight
		tooltip += "\nStack: %d" % resource.stack_size
		hint_tooltip = tooltip
	else:
		hint_tooltip = ""

func clear_slot() -> void:
	_clear_visuals()
	hint_tooltip = ""
	item_id = ""
	quantity = 0

func set_highlighted(highlighted: bool) -> void:
	highlight.visible = highlighted

func set_hotkey_label(text: String) -> void:
	hotkey_label.text = text
	hotkey_label.visible = not text.is_empty()

func set_search_match(is_match: bool) -> void:
	modulate = Color(1, 1, 1, 1) if is_match else Color(1, 1, 1, 0.35)

func _gui_input(event: InputEvent) -> void:
	emit_signal("slot_gui_input", event)

func _get_drag_data(at_position: Vector2) -> Variant:
	if owner_ui == null or item_id.is_empty() or quantity <= 0:
		return null
	var preview := owner_ui._create_drag_preview(self)
	if preview != null:
		set_drag_preview(preview)
	return owner_ui._build_drag_data(self)

func _can_drop_data(at_position: Vector2, data: Variant) -> bool:
	return owner_ui != null and owner_ui._can_drop_data_on_slot(self, data)

func _drop_data(at_position: Vector2, data: Variant) -> void:
	if owner_ui != null:
		owner_ui._handle_drop_on_slot(self, data)

func _clear_visuals() -> void:
	icon.texture = PLACEHOLDER_ICON
	icon.self_modulate = Color(1, 1, 1, 0.15)
	quantity_label.visible = false
	quantity_label.text = ""
	highlight.visible = false
	hotkey_label.visible = is_hotbar
