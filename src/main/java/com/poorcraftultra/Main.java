package com.poorcraftultra;

import com.poorcraftultra.core.Game;

public final class Main {
    private Main() {
        throw new UnsupportedOperationException("Main is a utility class");
    }

    public static void main(String[] args) {
        System.out.println("Starting PoorCraftUltra...");
        try {
            Game game = new Game();
            game.run();
            System.out.println("PoorCraftUltra shut down gracefully.");
        } catch (Exception exception) {
            System.err.println("PoorCraftUltra encountered a fatal error.");
            exception.printStackTrace();
            System.exit(-1);
        }
    }
}
