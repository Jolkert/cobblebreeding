package io.github.jolkert.cobblebreeding.util

fun <T> List<T>.combinationsWithSelf(): List<Pair<T, T>>
{
	val list = mutableListOf<Pair<T, T>>()
	for (i in this.indices)
		for (j in (i + 1..<this.size))
			list.add(this[i] to this[j])

	return list
}

fun <T> Collection<T>.containsAny(other: Collection<T>): Boolean
{
	for (item in other)
		if (this.contains(item))
			return true

	return false
}