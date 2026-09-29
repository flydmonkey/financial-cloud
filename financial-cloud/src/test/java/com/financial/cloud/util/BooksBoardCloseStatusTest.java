package com.financial.cloud.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BooksBoardCloseStatusTest {

    @Test
    void closedWhenCurrentAfterFocus() {
        assertThat(BooksBoardCloseStatus.resolve("2026-10", "2026-09"))
                .isEqualTo(BooksBoardCloseStatus.CLOSED);
    }

    @Test
    void openWhenEqual() {
        assertThat(BooksBoardCloseStatus.resolve("2026-09", "2026-09"))
                .isEqualTo(BooksBoardCloseStatus.OPEN);
    }

    @Test
    void behindWhenCurrentBeforeFocus() {
        assertThat(BooksBoardCloseStatus.resolve("2026-08", "2026-09"))
                .isEqualTo(BooksBoardCloseStatus.BEHIND);
    }

    @Test
    void unknownOnBlank() {
        assertThat(BooksBoardCloseStatus.resolve(null, "2026-09"))
                .isEqualTo(BooksBoardCloseStatus.UNKNOWN);
        assertThat(BooksBoardCloseStatus.resolve("2026-09", " "))
                .isEqualTo(BooksBoardCloseStatus.UNKNOWN);
        assertThat(BooksBoardCloseStatus.resolve("bad", "2026-09"))
                .isEqualTo(BooksBoardCloseStatus.UNKNOWN);
    }
}
