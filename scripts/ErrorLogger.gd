extends Node
class_name ErrorLogger

signal crash_reported(crash_path: String)

const LOG_DIR: String = "user://logs/"
const LOG_FILE_NAME: String = "retroforge.log"
const MAX_LOG_SIZE: int = 10485760
const MAX_LOG_FILES: int = 5
const CRASH_DIR: String = "user://crashes/"
const FLUSH_INTERVAL_SECONDS: float = 5.0

enum LogLevel { DEBUG, INFO, WARNING, ERROR, CRITICAL }

var log_file: FileAccess
var current_log_level: LogLevel = LogLevel.INFO
var log_to_console: bool = true
var log_to_file: bool = true
var error_count: int = 0
var warning_count: int = 0
var _time_since_flush: float = 0.0

static var _level_labels := {
    LogLevel.DEBUG: "DEBUG",
    LogLevel.INFO: "INFO",
    LogLevel.WARNING: "WARNING",
    LogLevel.ERROR: "ERROR",
    LogLevel.CRITICAL: "CRITICAL",
}

static var _level_colors := {
    LogLevel.DEBUG: "#9e9e9e",
    LogLevel.INFO: "#b2dfdb",
    LogLevel.WARNING: "#ffca28",
    LogLevel.ERROR: "#ef5350",
    LogLevel.CRITICAL: "#d50000",
}

func _ready() -> void:
    _ensure_directories()
    _open_log_file()
    _write_session_header()
    log_info("ErrorLogger initialized", "ErrorLogger")
    set_process(true)

func _notification(what: int) -> void:
    if what == NOTIFICATION_WM_CLOSE_REQUEST:
        _write_session_footer()
        _close_log_file()
    elif what == NOTIFICATION_PREDELETE:
        _write_session_footer()
        _close_log_file()

func log_debug(message: String, context: String = "") -> void:
    _log(LogLevel.DEBUG, message, context, false)

func log_info(message: String, context: String = "") -> void:
    _log(LogLevel.INFO, message, context, false)

func log_warning(message: String, context: String = "") -> void:
    warning_count += 1
    push_warning(_format_push_message(message, context))
    _log(LogLevel.WARNING, message, context, false)

func log_error(message: String, context: String = "") -> void:
    error_count += 1
    push_error(_format_push_message(message, context))
    _log(LogLevel.ERROR, message, context, true)

func log_critical(message: String, context: String = "") -> void:
    error_count += 1
    push_error(_format_push_message(message, context))
    _log(LogLevel.CRITICAL, message, context, true)
    report_crash(message)

func report_crash(error_message: String, stack_trace: Array = []) -> void:
    var timestamp := Time.get_datetime_string_from_system()
    var crash_file_name := "crash_%s.txt" % timestamp.replace(":", "-")
    var crash_path := CRASH_DIR + crash_file_name
    DirAccess.make_dir_recursive_absolute(CRASH_DIR)

    var crash_content := PackedStringArray()
    crash_content.append("RetroForge Crash Report")
    crash_content.append("Timestamp: %s" % timestamp)
    crash_content.append("Message: %s" % error_message)
    crash_content.append("\nSystem Info:")
    var system_info := get_system_info()
    for key in system_info.keys():
        crash_content.append("  %s: %s" % [key, system_info[key]])

    var recent_logs := _get_recent_log_excerpt()
    if recent_logs.size() > 0:
        crash_content.append("\nRecent Logs:")
        crash_content.append_array(recent_logs)

    if stack_trace.is_empty():
        stack_trace = _get_stack_strings()
    if stack_trace.size() > 0:
        crash_content.append("\nStack Trace:")
        for line in stack_trace:
            crash_content.append("  %s" % line)

    var crash_file := FileAccess.open(crash_path, FileAccess.WRITE)
    if crash_file:
        crash_file.store_string("\n".join(crash_content))
        crash_file.close()
        emit_signal("crash_reported", crash_path)
    else:
        push_error("Failed to write crash report: %s" % crash_path)

func get_system_info() -> Dictionary:
    var info := {
        "OS": OS.get_name(),
        "OSVersion": OS.get_version(),
        "Processor": OS.get_processor_name(),
        "ProcessorCount": OS.get_processor_count(),
        "MemoryUsage": OS.get_static_memory_usage(),
        "MemoryPeak": OS.get_static_memory_peak_usage(),
        "GPU": RenderingServer.get_video_adapter_name(),
        "GPUVendor": RenderingServer.get_video_adapter_vendor(),
        "DisplaySize": DisplayServer.screen_get_size(DisplayServer.get_primary_screen()),
        "GodotVersion": Engine.get_version_info().get("string", "Unknown"),
    }
    var game_manager := _get_game_manager()
    if game_manager:
        info["GameVersion"] = game_manager.game_version
        info["GameState"] = game_manager.GameState.keys()[game_manager.current_state]
    return info

func rotate_log_file() -> void:
    _close_log_file()
    var log_path := LOG_DIR + LOG_FILE_NAME
    var dir := DirAccess.open(LOG_DIR)
    if dir:
        var last_slot := "%sretroforge_%d.log" % [LOG_DIR, MAX_LOG_FILES]
        if FileAccess.file_exists(last_slot):
            DirAccess.remove_absolute(last_slot)
        for i in range(MAX_LOG_FILES - 1, 0, -1):
            var src := "%sretroforge_%d.log" % [LOG_DIR, i]
            var dst := "%sretroforge_%d.log" % [LOG_DIR, i + 1]
            if FileAccess.file_exists(src):
                if FileAccess.file_exists(dst):
                    DirAccess.remove_absolute(dst)
                DirAccess.rename_absolute(src, dst)
        if FileAccess.file_exists(log_path):
            DirAccess.rename_absolute(log_path, "%sretroforge_1.log" % LOG_DIR)
    _open_log_file(true)
    _time_since_flush = 0.0

func set_log_level(level: LogLevel) -> void:
    current_log_level = level
    _log(LogLevel.INFO, "Log level set to %s" % _level_labels[level], "ErrorLogger", false)

func get_log_path() -> String:
    return LOG_DIR + LOG_FILE_NAME

func clear_logs() -> void:
    var dir := DirAccess.open(LOG_DIR)
    if dir:
        dir.list_dir_begin()
        var name := dir.get_next()
        while name != "":
            if not dir.current_is_dir():
                DirAccess.remove_absolute(LOG_DIR + name)
            name = dir.get_next()
        dir.list_dir_end()
    _open_log_file(true)

func get_error_count() -> int:
    return error_count

func get_warning_count() -> int:
    return warning_count

func assert_with_log(condition: bool, message: String, context: String = "") -> void:
    if condition:
        return
    log_error("Assertion failed: %s" % message, context)

func _log(level: LogLevel, message: String, context: String, include_stack: bool) -> void:
    if not _should_log(level):
        return

    var timestamp := Time.get_datetime_string_from_system()
    var label := _level_labels[level]
    var context_text := context if context != "" else "Global"
    var log_line := "[%s] [%s] [%s] %s" % [timestamp, label, context_text, message]

    if include_stack:
        var stack_lines := _get_stack_strings()
        if stack_lines.size() > 0:
            log_line += "\n" + "\n".join(stack_lines)

    if log_to_console:
        var color := _level_colors[level]
        print_rich("[color=%s]%s[/color]" % [color, log_line])

    if log_to_file:
        var force_flush := level == LogLevel.ERROR or level == LogLevel.CRITICAL
        _write_to_file(log_line, force_flush)
        if force_flush:
            _flush_log_file()

func _should_log(level: LogLevel) -> bool:
    return int(level) >= int(current_log_level)

func _write_to_file(text: String, force_flush: bool = false) -> void:
    if log_file == null:
        return
    if log_file.get_length() >= MAX_LOG_SIZE:
        rotate_log_file()
    log_file.seek_end()
    log_file.store_line(text)
    if force_flush:
        _flush_log_file()
    _time_since_flush = 0.0

func _process(delta: float) -> void:
    _time_since_flush += delta
    if _time_since_flush >= FLUSH_INTERVAL_SECONDS:
        _flush_log_file()
        _time_since_flush = 0.0

func _get_recent_log_excerpt(max_lines: int = 200) -> PackedStringArray:
    var result := PackedStringArray()
    var log_path := LOG_DIR + LOG_FILE_NAME
    if not FileAccess.file_exists(log_path):
        return result
    var file := FileAccess.open(log_path, FileAccess.READ)
    if file == null:
        return result
    var lines := []
    while file.get_position() < file.get_length():
        lines.append(file.get_line())
    file.close()
    var start := max(lines.size() - max_lines, 0)
    for i in range(start, lines.size()):
        result.append(lines[i])
    return result

func _get_stack_strings() -> PackedStringArray:
    var stack := PackedStringArray()
    var trace := get_stack()
    for frame in trace:
        if frame.has("function") and frame.has("source") and frame.has("line"):
            stack.append("%s:%s (%s)" % [frame["source"], frame["line"], frame["function"]])
    return stack

func _format_push_message(message: String, context: String) -> String:
    return context == "" ? message : "%s: %s" % [context, message]

func _ensure_directories() -> void:
    DirAccess.make_dir_recursive_absolute(LOG_DIR)
    DirAccess.make_dir_recursive_absolute(CRASH_DIR)

func _open_log_file(reset: bool = false) -> void:
    _close_log_file()
    var path := LOG_DIR + LOG_FILE_NAME
    if reset and FileAccess.file_exists(path):
        DirAccess.remove_absolute(path)
    log_file = FileAccess.open(path, FileAccess.READ_WRITE)
    if log_file:
        log_file.seek_end()

func _close_log_file() -> void:
    if log_file != null:
        _flush_log_file()
        log_file.close()
        log_file = null

func _write_session_header() -> void:
    if not log_to_file:
        return
    var header := PackedStringArray()
    header.append("==== RetroForge Session Start ====")
    header.append("Timestamp: %s" % Time.get_datetime_string_from_system())
    header.append("Godot: %s" % Engine.get_version_info().get("string", "Unknown"))
    header.append("OS: %s %s" % [OS.get_name(), OS.get_version()])
    var game_manager := _get_game_manager()
    if game_manager:
        header.append("Game Version: %s" % game_manager.game_version)
    header.append("===============================")
    for line in header:
        _write_to_file(line)

func _write_session_footer() -> void:
    if not log_to_file:
        return
    var footer := PackedStringArray()
    footer.append("===============================")
    footer.append("Session End: %s" % Time.get_datetime_string_from_system())
    footer.append("Warnings: %d" % warning_count)
    footer.append("Errors: %d" % error_count)
    footer.append("===============================")
    for line in footer:
        _write_to_file(line)

func _flush_log_file() -> void:
    if log_file != null:
        log_file.flush()

func _get_game_manager() -> Node:
    if typeof(GameManager) != TYPE_NIL and GameManager != null:
        return GameManager
    var tree := get_tree()
    if tree and tree.has_node("/root/GameManager"):
        return tree.get_node("/root/GameManager")
    return null
