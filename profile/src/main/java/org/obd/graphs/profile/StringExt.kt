 /**
 * Copyright 2019-2026, Tomasz Żebrowski
 *
 * <p>Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.obd.graphs.profile

internal fun String.toCamelCase() = split('_').joinToString(" ", transform = String::capitalize)

internal fun String.isArray() = startsWith("[") || endsWith("]")

internal fun String.isBoolean(): Boolean = startsWith("false") || startsWith("true")

internal fun String.isNumeric(): Boolean = matches(Regex("-?\\d+"))

internal fun String.toBoolean(): Boolean = startsWith("true")

internal fun String.toStringSet(): MutableSet<String> =
    this
        .replace("[", "")
        .replace("]", "")
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .toMutableSet()
