package com.poe.backend.service;

import java.util.Locale;

final class GameMath {
    /**
     * Pure math helpers for game reward/happiness formulas.
     *
     * Intentional:
     * - no database calls
     * - no Spring dependencies
     *
     * This keeps formulas testable and makes it easier to reason about balancing.
     */
    private GameMath() {
    }

    /**
     * Connect 4 win: stronger than generic score=2 path (~2.5× prior tuning). Uses raised base, tier×moves weight, higher cap.
     */
    static int connect4WinHappinessDeltaPercent(String difficultyLabel, int humanMoves) {
        String d = difficultyLabel == null ? "easy" : difficultyLabel.trim().toLowerCase(Locale.ROOT);
        int tier = switch (d) {
            case "medium" -> 2;
            case "hard" -> 3;
            default -> 1;
        };
        int moves = Math.max(0, humanMoves);
        return Math.min(100, 13 + tier * moves * 3);
    }

    static int happinessDeltaForScore(int score) {
        if (score == 0) {
            return -10;
        }
        if (score == 1) {
            return 0;
        }
        return Math.min(62, (score - 1) * 13);
    }

    /**
     * Reward curve used by Higher/Lower.
     *
     * It's not Fibonacci starting at 1/1; it's "shifted" (1,2,3,5,8,...) and sums the first {@code streak} terms.
     * The running total is capped by {@code maxCap} (from DB config).
     */
    static int shiftedFibonacciReward(int streak, int maxCap) {
        int a = 1;
        int b = 2;
        int total = 0;
        int cap = Math.max(1, maxCap);
        for (int i = 0; i < streak; i++) {
            total += a;
            int next = a + b;
            a = b;
            b = next;
        }
        return Math.min(total, cap);
    }
}
