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
package org.obd.graphs.preferences.pid

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TableLayout
import android.widget.TextView
import androidx.core.text.isDigitsOnly
import androidx.recyclerview.widget.RecyclerView
import org.obd.graphs.R
import org.obd.graphs.ui.common.COLOR_DYNAMIC_SELECTOR_SPORT
import org.obd.graphs.ui.common.COLOR_PHILIPPINE_GREEN
import org.obd.graphs.ui.common.COLOR_RAINBOW_INDIGO
import org.obd.graphs.ui.common.setText
import java.util.Collections

private const val TAG = "PID_VIEW"

class PIDsAdapter internal constructor(
    private val root: View,
    context: Context?,
    var data: List<PidDefinitionDetails>,
    private val editModeEnabled: Boolean,
) : RecyclerView.Adapter<PIDsAdapter.ViewHolder>() {
    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var lastSelectedPosition = -1
    var currentSelectedPosition = -1

    class CallableTextWatcher(
        val callable: (pid: PidDefinitionDetails, editable: Editable?) -> Unit,
    ) : TextWatcher {
        var pid: PidDefinitionDetails? = null

        override fun beforeTextChanged(
            p0: CharSequence?,
            p1: Int,
            p2: Int,
            p3: Int,
        ) {}

        override fun onTextChanged(
            p0: CharSequence?,
            p1: Int,
            p2: Int,
            p3: Int,
        ) {}

        override fun afterTextChanged(editable: Editable?) {
            pid?.let {
                callable(it, editable)
            }
        }
    }

    private val formulaTextWatcher =
        CallableTextWatcher { pid, editable ->
            pid.source.formula = editable.toString()
            Log.d(TAG, "Setting new formula=$editable")
        }

    private val upperAlertTextWatcher =
        CallableTextWatcher { pid, editable ->
            if (editable.toString().isEmpty()) {
                pid.source.alert.upperThreshold = null
            } else {
                if (editable.toString().isDigitsOnly()) {
                    Log.d(TAG, "Setting new upperThreshold=$editable")
                    pid.source.alert.upperThreshold = editable.toString().toInt()
                }
            }
        }

    private val lowerAlertTextWatcher =
        CallableTextWatcher { pid, editable ->
            if (editable.toString().isEmpty()) {
                pid.source.alert.lowerThreshold = null
            } else {
                if (editable.toString().isDigitsOnly()) {
                    Log.d(TAG, "Setting new lowerThreshold=$editable")
                    pid.source.alert.lowerThreshold = editable.toString().toInt()
                }
            }
        }

    fun swapItems(
        fromPosition: Int,
        toPosition: Int,
    ) {
        Collections.swap(data, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder = ViewHolder(inflater.inflate(R.layout.item_pids, parent, false))

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        data.elementAt(position).run {
            holder.module.setText(source.resourceFile, COLOR_PHILIPPINE_GREEN, Typeface.NORMAL, 0.7f)

            val description =
                if (source.longDescription == null || source.longDescription.isEmpty()) {
                    source.description
                } else {
                    source.longDescription
                }

            holder.description.setText(description, COLOR_RAINBOW_INDIGO, Typeface.NORMAL, 1f)

            if (source.stable) {
                holder.status.setText("Yes", Color.GRAY, Typeface.NORMAL, 0.7f)
            } else {
                holder.status.setText("No", COLOR_DYNAMIC_SELECTOR_SPORT, Typeface.NORMAL, 0.7f)
            }

            val lowerThreshold = source.alert.lowerThreshold
            val upperThreshold = source.alert.upperThreshold

            if (lowerThreshold != null || upperThreshold != null) {
                var text = ""
                if (lowerThreshold != null) {
                    text += " x<$lowerThreshold"
                }

                if (upperThreshold != null) {
                    text += " x>$upperThreshold"
                }
                holder.formula.text = text
            } else {
                holder.formula.text = ""
            }

            holder.selected.isChecked = checked
            holder.selected.setOnCheckedChangeListener { buttonView, isChecked ->
                if (buttonView.isShown) {
                    checked = isChecked
                }
            }

            if (holder.adapterPosition == currentSelectedPosition) {
                holder.layout.setBackgroundColor(Color.LTGRAY)
            } else {
                holder.layout.setBackgroundColor(Color.WHITE)
            }
        }
    }

    fun updateData(data: List<PidDefinitionDetails>) {
        this.data = data
    }

    override fun getItemCount(): Int = data.size

    inner class ViewHolder internal constructor(
        private val binding: View,
    ) : RecyclerView.ViewHolder(binding) {
        val module: TextView = binding.findViewById(R.id.pid_module)
        val description: TextView = binding.findViewById(R.id.pid_description)
        val status: TextView = binding.findViewById(R.id.pid_status)
        val selected: CheckBox = binding.findViewById(R.id.pid_selected)
        val layout: TableLayout = binding.findViewById(R.id.tablelayout)
        val formula: TextView = binding.findViewById(R.id.pid_formula)


        init {
            selected.visibility = if (editModeEnabled) View.GONE else View.VISIBLE
            status.visibility = if (editModeEnabled) View.GONE else View.VISIBLE
            formula.visibility = if (editModeEnabled) View.VISIBLE else View.GONE

            binding.setOnClickListener {
                val item = data[adapterPosition]

                if (editModeEnabled) {
                    lastSelectedPosition = currentSelectedPosition
                    currentSelectedPosition = adapterPosition

                    binding.isSelected = true
                    notifyItemChanged(adapterPosition)

                    notifyItemChanged(lastSelectedPosition)
                    notifyItemChanged(currentSelectedPosition)

                    val pidDetailsDescription = root.findViewById<TextView>(R.id.pid_details_name)

                    pidDetailsDescription.text =
                        if (item.source.longDescription == null || item.source.longDescription.isEmpty()) {
                            item.source.description
                        } else {
                            item.source.longDescription
                        }

                    root.findViewById<EditText>(R.id.pid_details_calculation_formula).let {
                        it.removeTextChangedListener(formulaTextWatcher)
                        it.setText(item.source.formula)
                        it.clearFocus()
                        formulaTextWatcher.pid = item
                        it.addTextChangedListener(formulaTextWatcher)
                    }

                    root.findViewById<EditText>(R.id.pid_details_alert_lower_threshold).let { edit ->
                        edit.removeTextChangedListener(lowerAlertTextWatcher)

                        edit.setText(
                            if (item.source.alert.lowerThreshold == null) {
                                ""
                            } else {
                                item.source.alert.lowerThreshold
                                    .toString()
                            },
                        )

                        edit.clearFocus()
                        lowerAlertTextWatcher.pid = item
                        edit.addTextChangedListener(lowerAlertTextWatcher)
                    }

                    root.findViewById<EditText>(R.id.pid_details_alert_upper_threshold).let { edit ->
                        edit.removeTextChangedListener(upperAlertTextWatcher)

                        edit.setText(
                            if (item.source.alert.upperThreshold == null) {
                                ""
                            } else {
                                item.source.alert.upperThreshold
                                    .toString()
                            },
                        )
                        edit.clearFocus()
                        upperAlertTextWatcher.pid = item
                        edit.addTextChangedListener(upperAlertTextWatcher)
                    }

                    val pidDetailsSortedMap = root.findViewById<TextView>(R.id.pid_details_supported)
                    if (item.supported) {
                        pidDetailsSortedMap.text = "Yes"
                    } else {
                        pidDetailsSortedMap.text = "No"
                    }
                }
            }
        }
    }
}
