package com.poe.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GameMathTest {
    @Test
    void shiftedFibonacciRewardFollowsExpectedSeries() {
        assertEquals(0, GameMath.shiftedFibonacciReward(0, 200));
        assertEquals(1, GameMath.shiftedFibonacciReward(1, 200));
        assertEquals(3, GameMath.shiftedFibonacciReward(2, 200));
        assertEquals(6, GameMath.shiftedFibonacciReward(3, 200));
        assertEquals(11, GameMath.shiftedFibonacciReward(4, 200));
    }

    @Test
    void shiftedFibonacciRewardHasCap() {
        assertEquals(200, GameMath.shiftedFibonacciReward(30, 200));
        assertEquals(35, GameMath.shiftedFibonacciReward(30, 35));
    }

    @Test
    void happinessDeltaThresholdsMatchDesign() {
        assertEquals(-10, GameMath.happinessDeltaForScore(0));
        assertEquals(0, GameMath.happinessDeltaForScore(1));
        assertEquals(13, GameMath.happinessDeltaForScore(2));
        assertEquals(62, GameMath.happinessDeltaForScore(8));
    }

    @Test
    void connect4WinHappinessUsesRaisedBasePlusTierTimesMoves() {
        assertEquals(43, GameMath.connect4WinHappinessDeltaPercent("easy", 10));
        assertEquals(73, GameMath.connect4WinHappinessDeltaPercent("medium", 10));
        assertEquals(100, GameMath.connect4WinHappinessDeltaPercent("hard", 10));
        assertEquals(13, GameMath.connect4WinHappinessDeltaPercent("easy", 0));
    }

    @Test
    void connect4WinHappinessIsCapped() {
        assertEquals(100, GameMath.connect4WinHappinessDeltaPercent("hard", 100));
    }
}
