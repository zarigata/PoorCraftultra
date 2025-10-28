package com.poorcraft.ultra.shared.util;

import org.slf4j.LoggerFactory;

public final class Logger {

    private Logger() {
    }

    public static org.slf4j.Logger getLogger(Class<?> type) {
        return LoggerFactory.getLogger(type);
    }
}
