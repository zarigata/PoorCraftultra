package com.poorcraft.ultra.ui;

import com.jme3.cursors.plugins.JmeCursor;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.RawInputListener;

/**
 * Test-only input device stubs tailored for unit tests to avoid pulling a full jME runtime.
 */
final class TestInputDevices {

    private TestInputDevices() {
        // Utility holder
    }

    static final class StubKeyInput implements KeyInput {
        private RawInputListener listener;

        @Override
        public void initialize() {
            // no-op
        }

        @Override
        public void update() {
            // no-op
        }

        @Override
        public void destroy() {
            // no-op
        }

        @Override
        public boolean isInitialized() {
            return true;
        }

        @Override
        public void setInputListener(RawInputListener listener) {
            this.listener = listener;
        }

        RawInputListener getListener() {
            return listener;
        }

        @Override
        public long getInputTimeNanos() {
            return System.nanoTime();
        }

        @Override
        public String getKeyName(int key) {
            return "Key-" + key;
        }
    }

    static final class StubMouseInput implements MouseInput {
        private RawInputListener listener;
        private boolean cursorVisible = true;

        @Override
        public void initialize() {
            // no-op
        }

        @Override
        public void update() {
            // no-op
        }

        @Override
        public void destroy() {
            // no-op
        }

        @Override
        public boolean isInitialized() {
            return true;
        }

        @Override
        public void setInputListener(RawInputListener listener) {
            this.listener = listener;
        }

        RawInputListener getListener() {
            return listener;
        }

        @Override
        public long getInputTimeNanos() {
            return System.nanoTime();
        }

        @Override
        public void setCursorVisible(boolean visible) {
            cursorVisible = visible;
        }

        boolean isCursorVisible() {
            return cursorVisible;
        }

        @Override
        public int getButtonCount() {
            return 3;
        }

        @Override
        public void setNativeCursor(JmeCursor cursor) {
            // no-op
        }
    }
}
