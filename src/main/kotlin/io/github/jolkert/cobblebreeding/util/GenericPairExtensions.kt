package io.github.jolkert.cobblebreeding.util

import kotlin.random.Random

inline fun <T> Pair<T, T>.neither(predicate: (T) -> Boolean) = !either(predicate)
inline fun <T> Pair<T, T>.either(predicate: (T) -> Boolean) = predicate(this.first) || predicate(this.second)
inline fun <T> Pair<T, T>.both(predicate: (T) -> Boolean) = predicate(this.first) && predicate(this.second)
inline fun <T> Pair<T, T>.onlyOne(predicate: (T) -> Boolean) = predicate(this.first) xor predicate(this.second)

fun <T> Pair<T, T>.elements() = sequence { yield(first); yield(second) }
fun <T> Pair<T, T>.random() = if (Random.nextBoolean()) first else second

inline fun <T> Pair<T, T>.firstMatching(predicate: (T) -> Boolean): T?
{
	return if (predicate(first))
		first
	else if (predicate(second))
		second
	else
		null
}