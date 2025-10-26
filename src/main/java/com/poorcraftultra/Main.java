package com.poorcraftultra;

import com.poorcraftultra.core.Engine;

/**
 * Main entry point for PoorCraft Ultra game engine.
 */
public class Main {

    public static void main(String[] args) {
        Engine engine = new Engine();
        try {
            engine.run();
        } catch (Exception e) {
            System.err.println("Error running engine: " + e.getMessage());
            e.printStackTrace();
        } finally {
            engine.cleanup();
        }
    }
}
