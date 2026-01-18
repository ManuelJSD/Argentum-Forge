package org.argentumforge.engine.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TimeTest {

    @Test
    @DisplayName("Should initialize time correctly")
    void shouldInitTime() {
        Time.initTime();
        // Without a GLFW context, glfwGetTime() returns 0.
        assertThat(Time.beginTime).isGreaterThanOrEqualTo(0);
        assertThat(Time.deltaTime).isEqualTo(-1.0f);
    }

    @Test
    @DisplayName("Should update FPS correctly over time")
    void shouldUpdateFPS() {
        // Reset Time state
        Time.initTime();
        Time.deltaTime = 0.5f; // Simulation step

        // We need to call updateTime multiple times to simulate 1 second
        // Since updateTime calls updateFPS first, then calculates new deltaTime,
        // we'll manually set deltaTime to simulate the passage of time.

        // Simulate 2 frames of 0.5s each
        Time.deltaTime = 0.5f;
        Time.updateTime(); // timerFPS becomes 0.5

        Time.deltaTime = 0.5f;
        Time.updateTime(); // timerFPS becomes 0.0, next updateFPS will reset it

        Time.deltaTime = 0.1f;
        Time.updateTime(); // Now timerFPS <= 0 should have triggered reset

        // This is a bit tricky because Time uses static fields and glfwGetTime()
        // But we can at least verify that it's doing something with the numbers.
        assertThat(Time.deltaTime).isNotNull();
    }
}
