package com.sunnychung.application.multiplatform.giantlogviewer.extension

internal fun Double.coerceToLong(): Long {
    return when {
        !java.lang.Double.isFinite(this) || this <= 0.0 -> 0L
        this >= Long.MAX_VALUE.toDouble() -> Long.MAX_VALUE
        else -> toLong()
    }
}

internal fun Long.toClampedInt(): Int {
    return coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()
}

internal infix fun Long.saturatedAdd(other: Long): Long {
    if (this <= 0L) {
        return other.coerceAtLeast(0L)
    }
    if (other <= 0L) {
        return this
    }
    return if (other > Long.MAX_VALUE - this) {
        Long.MAX_VALUE
    } else {
        this + other
    }
}

internal infix fun Long.saturatedMultiply(other: Long): Long {
    if (this <= 0L || other <= 0L) {
        return 0L
    }
    return if (this > Long.MAX_VALUE / other) {
        Long.MAX_VALUE
    } else {
        this * other
    }
}

internal val IntRange.safeEndExclusive: Int
    get() = if (last == Int.MAX_VALUE) Int.MAX_VALUE else last + 1

internal val LongRange.safeEndExclusive: Long
    get() = if (last == Long.MAX_VALUE) Long.MAX_VALUE else last + 1L

internal fun LongRange.forwardLength(): Long {
    return if (isEmpty()) 0L else endExclusive - start
}

internal fun Int.floorMod(modulus: Int): Int {
    if (modulus <= 0) {
        return 0
    }
    return ((this % modulus) + modulus) % modulus
}
