package com.leekleak.core.charts.model

data class BarData(
    val x: String = "",
    val y1: Long = 0L,
    val y2: Long = 0L
) {
    operator fun plus(other: BarData): BarData {
        return this.copy(
            y1 = this.y1 + other.y1,
            y2 = this.y2 + other.y2
        )
    }
}

data class ScrollableBarData(
    val label: String? = null,
    val highlight: Boolean = false,
    val monthLabel: String? = null,
    val y1: Long = 0L,
    val y2: Long = 0L
) {
    operator fun plus(other: ScrollableBarData): ScrollableBarData {
        return this.copy(
            y1 = this.y1 + other.y1,
            y2 = this.y2 + other.y2
        )
    }
}