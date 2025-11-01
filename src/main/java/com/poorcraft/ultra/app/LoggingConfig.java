package com.poorcraft.ultra.app;

/**
 * Logging configuration utility.
 * Configures SLF4J/Logback based on environment variables and logback.xml.
 * 
 * IMPORTANT: This class must NOT create any SLF4J loggers to avoid triggering
 * Logback initialization before the system property is set.
 */
public class LoggingConfig {
    
    /**
     * Sets up logging configuration.
     * Reads POORCRAFT_LOG_LEVEL environment variable to override default level.
     * 
     * MUST be called before any SLF4J logger is created in the application.
     * 
     * @return The configured log level for informational purposes
     */
    public static String setup() {
        // Logback configuration is primarily handled by logback.xml in resources
        // This method sets the system property before Logback initializes
        
        String logLevel = System.getenv("POORCRAFT_LOG_LEVEL");
        if (logLevel == null || logLevel.isEmpty()) {
            logLevel = "INFO";
        }
        
        // Set system property for logback.xml to read
        System.setProperty("poorcraft.log.level", logLevel);
        
        return logLevel;
    }
}
