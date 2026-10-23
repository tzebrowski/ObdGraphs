/*
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
package org.obd.graphs.bl.datalogger

import org.obd.metrics.pid.PidDefinitionRegistry

// The registry is reloaded from broadcasts (runAsync) and from the UI thread (locale change), possibly at once.
// Each reload rebuilds the registry, then iterates and writes into it; unserialized, one reload's register()
// hit another's findAll() iteration (ConcurrentModificationException in production).
internal class PidRegistryReload(
    private val registry: () -> PidDefinitionRegistry,
    private val prepare: (PidDefinitionRegistry) -> Unit
) {
    fun reload(load: () -> Unit) =
        synchronized(this) {
            load()
            prepare(registry())
        }
}
