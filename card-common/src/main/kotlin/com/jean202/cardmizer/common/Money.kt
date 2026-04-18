package com.jean202.cardmizer.common

@JvmInline
value class Money private constructor(val amount: Long) : Comparable<Money> {

    companion object {
        val ZERO = Money(0L)

        @JvmStatic
        fun won(amount: Long): Money {
            require(amount >= 0L) { "Money amount must be zero or positive" }
            return Money(amount)
        }
    }

    operator fun plus(other: Money): Money = Money(Math.addExact(amount, other.amount))

    operator fun minus(other: Money): Money {
        val result = Math.subtractExact(amount, other.amount)
        require(result >= 0L) { "Money amount must not become negative" }
        return Money(result)
    }

    fun add(other: Money): Money = plus(other)
    fun subtract(other: Money): Money = minus(other)

    fun isGreaterThanOrEqual(other: Money): Boolean = amount >= other.amount

    override fun compareTo(other: Money): Int = amount.compareTo(other.amount)
}
