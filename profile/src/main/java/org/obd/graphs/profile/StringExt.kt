package org.obd.graphs.profile

internal fun String.toCamelCase() =
    split('_').joinToString(" ", transform = String::capitalize)

internal fun String.isArray() = startsWith("[") || endsWith("]")
internal fun String.isBoolean(): Boolean = startsWith("false") || startsWith("true")
internal fun String.isNumeric(): Boolean = matches(Regex("-?\\d+"))
internal fun String.toBoolean(): Boolean = startsWith("true")
