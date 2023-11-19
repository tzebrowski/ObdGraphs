/**
 * Copyright 2019-2023, Tomasz Żebrowski
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package org.obd.graphs.bl.query


class Query : java.io.Serializable {

    private val strategies: Map<QueryStrategyType, QueryStrategy> = mutableMapOf<QueryStrategyType, QueryStrategy>().apply {
        this[QueryStrategyType.SHARED_QUERY] = SharedQueryStrategy()
        this[QueryStrategyType.DRAG_RACING_QUERY] = DragRacingQueryStrategy()
        this[QueryStrategyType.INDIVIDUAL_QUERY_FOR_EACH_VIEW] = QueryStrategy()
    }

    private var strategy: QueryStrategyType = QueryStrategyType.SHARED_QUERY

    fun getPIDs(): MutableSet<Long> = strategies[strategy]?.getPIDs() ?: mutableSetOf()

    fun getStrategy(): QueryStrategyType = strategy

    fun setStrategy(queryStrategyType: QueryStrategyType): Query {
        this.strategy = queryStrategyType
        return this
    }

    fun update(newPIDs: Set<Long>): Query {
        strategies[strategy]?.update(newPIDs)
        return this
    }
}