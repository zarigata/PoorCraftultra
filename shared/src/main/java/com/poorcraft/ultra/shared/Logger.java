package com.poorcraft.ultra.shared;

import org.slf4j.LoggerFactory;

public final class Logger {

    private final org.slf4j.Logger slf4jLogger;
    private final String name;

    private Logger(org.slf4j.Logger slf4jLogger) {
        this.slf4jLogger = slf4jLogger;
        this.name = slf4jLogger.getName();
    }

    public static Logger getLogger(Class<?> clazz) {
        return new Logger(LoggerFactory.getLogger(clazz));
    }

    public static Logger getLogger(String name) {
        return new Logger(LoggerFactory.getLogger(name));
    }

    public void trace(String message, Object... args) {
        slf4jLogger.trace(message, args);
    }

    public void debug(String message, Object... args) {
        slf4jLogger.debug(message, args);
    }

    public void info(String message, Object... args) {
        slf4jLogger.info(message, args);
    }

    public void warn(String message, Object... args) {
        slf4jLogger.warn(message, args);
    }

    public void error(String message, Object... args) {
        slf4jLogger.error(message, args);
    }

    public void error(String message, Throwable throwable) {
        slf4jLogger.error(message, throwable);
    }

    public boolean isTraceEnabled() {
        return slf4jLogger.isTraceEnabled();
    }

    public boolean isDebugEnabled() {
        return slf4jLogger.isDebugEnabled();
    }

    public boolean isInfoEnabled() {
        return slf4jLogger.isInfoEnabled();
    }

    public boolean isWarnEnabled() {
        return slf4jLogger.isWarnEnabled();
    }

    public boolean isErrorEnabled() {
        return slf4jLogger.isErrorEnabled();
    }

    public void banner(String message) {
        String horizontal = "=".repeat(Math.max(message.length() + 4, 20));
        slf4jLogger.info(horizontal);
        slf4jLogger.info("= {} =", message);
        slf4jLogger.info(horizontal);
    }

    public String getName() {
        return name;
    }
}
