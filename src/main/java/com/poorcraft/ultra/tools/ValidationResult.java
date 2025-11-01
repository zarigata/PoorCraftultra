package com.poorcraft.ultra.tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Data class for asset validation results.
 * 
 * Immutable record containing validation status, messages, and error/warning lists.
 */
public record ValidationResult(
    boolean valid,
    String message,
    List<String> errors,
    List<String> warnings
) {
    
    public ValidationResult {
        // Make lists immutable
        errors = errors != null ? Collections.unmodifiableList(new ArrayList<>(errors)) : List.of();
        warnings = warnings != null ? Collections.unmodifiableList(new ArrayList<>(warnings)) : List.of();
        
        // Default message if not provided
        if (message == null || message.isBlank()) {
            if (valid) {
                message = "All assets valid";
            } else {
                message = errors.size() + " error(s) found";
            }
        }
    }
    
    /**
     * Create a successful validation result with no errors or warnings.
     */
    public static ValidationResult success() {
        return new ValidationResult(true, "All assets valid", List.of(), List.of());
    }
    
    /**
     * Create a failed validation result with errors.
     * 
     * @param errors List of error messages
     */
    public static ValidationResult failure(List<String> errors) {
        if (errors == null || errors.isEmpty()) {
            throw new IllegalArgumentException("Failure result must have at least one error");
        }
        
        String message = errors.size() + " error(s) found";
        return new ValidationResult(false, message, errors, List.of());
    }
    
    /**
     * Create a successful validation result with warnings.
     * 
     * @param warnings List of warning messages
     */
    public static ValidationResult withWarnings(List<String> warnings) {
        if (warnings == null || warnings.isEmpty()) {
            return success();
        }
        
        String message = "All assets valid (" + warnings.size() + " warning(s))";
        return new ValidationResult(true, message, List.of(), warnings);
    }
    
    @Override
    public String toString() {
        return String.format("ValidationResult[valid=%s, errors=%d, warnings=%d]",
                           valid, errors.size(), warnings.size());
    }
}
