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
package org.obd.graphs.preferences.dtc

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.Checkable
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.button.MaterialButtonToggleGroup
import org.obd.graphs.R

// A module picker row: the ListView's choice state (scan this module) drives the row's check box. The UDS / OBD-II
// buttons beside it are the module's protocol, not part of the list choice.
class CheckableModuleRow
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs),
    Checkable {
    private val check: CheckBox? get() = findViewById(R.id.dtc_pick_check)

    override fun setChecked(checked: Boolean) {
        check?.isChecked = checked
    }

    override fun isChecked(): Boolean = check?.isChecked == true

    override fun toggle() {
        isChecked = !isChecked
    }
}

// obdMarked: requestKeys set to OBD-II, changed in place by the protocol buttons.
internal class DtcModulePickerAdapter(
    private val context: Context,
    private val labels: List<String>,
    private val requestKeys: List<String>,
    val obdMarked: MutableSet<String>
) : BaseAdapter() {
    override fun getCount() = labels.size

    override fun getItem(position: Int) = labels[position]

    override fun getItemId(position: Int) = position.toLong()

    override fun getView(
        position: Int,
        convertView: View?,
        parent: ViewGroup
    ): View {
        val row = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_dtc_module_pick, parent, false)
        row.findViewById<TextView>(R.id.dtc_pick_label).text = labels[position]
        val key = requestKeys[position]
        row.findViewById<MaterialButtonToggleGroup>(R.id.dtc_pick_protocol).apply {
            // Recycled rows: drop the old row's listener before setting this row's state.
            clearOnButtonCheckedListeners()
            check(if (key in obdMarked) R.id.dtc_pick_obd else R.id.dtc_pick_uds)
            addOnButtonCheckedListener { _, id, checked ->
                if (checked) {
                    if (id == R.id.dtc_pick_obd) obdMarked.add(key) else obdMarked.remove(key)
                }
            }
        }
        return row
    }
}
