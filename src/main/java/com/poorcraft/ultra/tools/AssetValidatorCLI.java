package com.poorcraft.ultra.tools;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * CLI wrapper for AssetValidator (used by Gradle task).
 * 
 * Validates assets and exits with code 0 (success) or 1 (failure).
 */
public class AssetValidatorCLI {
    
    public static void main(String[] args) {
        // Parse args
        if (args.length != 1) {
            System.err.println("Usage: AssetValidatorCLI <assets-root-path>");
            System.err.println("Example: AssetValidatorCLI assets/");
            System.exit(1);
        }
        
        Path assetsRoot = Paths.get(args[0]);
        
        // Validate
        AssetValidator validator = new AssetValidator();
        ValidationResult result = validator.validate(assetsRoot);
        
        // Print result
        if (result.valid()) {
            // Success
            System.out.println("[OK] Asset validation passed");
            
            // Print warnings if any
            if (!result.warnings().isEmpty()) {
                System.out.println("Warnings:");
                for (String warning : result.warnings()) {
                    System.out.println("  - " + warning);
                }
            }
            
            System.exit(0);
        } else {
            // Failure
            System.err.println("[FAIL] Asset validation failed:");
            for (String error : result.errors()) {
                System.err.println("  - " + error);
            }
            
            System.err.println();
            System.err.println("Run './scripts/dev/gen-assets.sh' (Unix) or 'scripts\\dev\\gen-assets.bat' (Windows) to regenerate assets.");
            
            System.exit(1);
        }
    }
}
