 /**
 * Copyright 2019-2025, Tomasz Żebrowski
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
package org.obd.graphs.ui.common

import android.text.SpannableStringBuilder
import android.text.style.RelativeSizeSpan
import android.widget.Toast
import org.obd.graphs.getContext
import org.obd.graphs.runAsync

private var toast: Toast? = null

fun toast(
    id: Int,
    vararg formatArgs: String,
) {
    getContext()?.let {
        val text = it.resources.getString(id, *formatArgs)
        val biggerText = SpannableStringBuilder(text)
        biggerText.setSpan(RelativeSizeSpan(1.0f), 0, text.length, 0)

        if (toast != null) {
            toast?.cancel()
        }

        toast =
            Toast.makeText(
                it,
                biggerText,
                Toast.LENGTH_LONG,
            )

        toast?.run {
            runAsync {
                show()
            }
        }
    }
}
