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
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.core.text.isDigitsOnly
import androidx.recyclerview.widget.RecyclerView
import org.obd.graphs.R
import org.obd.graphs.bl.datalogger.serialize
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.getStringSet
import org.obd.graphs.preferences.updateStringSet
import org.obd.graphs.sendBroadcastEvent
import org.obd.graphs.ui.common.COLOR_DYNAMIC_SELECTOR_SPORT
import org.obd.graphs.ui.common.COLOR_PHILIPPINE_GREEN
import org.obd.graphs.ui.common.COLOR_RAINBOW_INDIGO
import org.obd.graphs.ui.common.setText
import org.obd.metrics.pid.PidDefinition
import java.util.*


 private const val LOG_TAG = "PIDsView"

 class PIDsViewAdapter internal constructor(
    private val root: View,
    context: Context?,
    var data: List<PidDefinitionDetails>,
    private val key: String,
    private val detailsViewEnabled: Boolean
    ) : RecyclerView.Adapter<PIDsViewAdapter.ViewHolder>(){

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    fun swapItems(fromPosition: Int, toPosition: Int){
        Collections.swap(data, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        return ViewHolder(inflater.inflate(R.layout.item_pids, parent, false))
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        data.elementAt(position).run {
            holder.module.setText(source.resourceFile, COLOR_PHILIPPINE_GREEN, Typeface.NORMAL, 0.7f)

            val description = if (source.longDescription == null || source.longDescription.isEmpty()) {
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

            holder.selected.isChecked = checked
            holder.selected.setOnCheckedChangeListener { buttonView, isChecked ->
                if (buttonView.isShown) {
                    checked = isChecked
                }
            }
        }

    }

    fun updateData(data: List<PidDefinitionDetails>){
        this.data = data
    }

    override fun getItemCount(): Int {
        return data.size
    }

   private val formulaTextWatcher =  object: TextWatcher {
        var pid: PidDefinitionDetails? = null

        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }

        override fun afterTextChanged(editable: Editable?) {
            pid?.let {
                it.source.formula  = editable.toString()
                Log.d("formulaTextWatcher", "Setting new formula=${editable.toString()}")
            }
        }
    }

    private val upperAlertTextWatcher =  object: TextWatcher {
        var pid: PidDefinitionDetails? = null

        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }

        override fun afterTextChanged(editable: Editable?) {
            pid?.let {
                if (editable.toString().isNotEmpty() && editable.toString().isDigitsOnly()) {
                    it.source.alert.upperThreshold = editable.toString().toInt()
                    Log.d("upperAlertTextWatcher", "Setting new upperAlertTextWatcher=${editable.toString()}")
                }
            }
        }
    }

    private val lowerAlertTextWatcher =  object: TextWatcher {
        var pid: PidDefinitionDetails? = null

        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }

        override fun afterTextChanged(editable: Editable?) {
            pid?.let {
                if (editable.toString().isNotEmpty() && editable.toString().isDigitsOnly()) {
                    it.source.alert.lowerThreshold = editable.toString().toInt()
                    Log.d("lowerAlertTextWatcher", "Setting new upperAlertTextWatcher=${editable.toString()}")
                }
            }
        }
    }


    inner class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val module: TextView = itemView.findViewById(R.id.pid_module)
        val description: TextView = itemView.findViewById(R.id.pid_description)
        val status: TextView = itemView.findViewById(R.id.pid_status)
        val selected: CheckBox = itemView.findViewById(R.id.pid_selected)

        init {
            if (detailsViewEnabled) {
                itemView.setOnClickListener {

                    val item = data[adapterPosition]
                    itemView.isSelected = true
                    notifyItemChanged(adapterPosition)

                    val pidDetailsModule = root.findViewById<TextView>(R.id.pid_details_module)
                    pidDetailsModule.text = item.source.module

                    val pidDetailsDescription = root.findViewById<TextView>(R.id.pid_details_name)

                    pidDetailsDescription.text = if (item.source.longDescription == null || item.source.longDescription.isEmpty()) {
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

                    val pidDetailsFile = root.findViewById<TextView>(R.id.pid_details_file)
                    pidDetailsFile.text = item.source.resourceFile

                    root.findViewById<EditText>(R.id.pid_details_alert_lower_threshold).let{ edit ->
                        edit.removeTextChangedListener(lowerAlertTextWatcher)

                        edit.setText(if (item.source.alert.lowerThreshold == null){
                            ""
                        } else {
                            item.source.alert.lowerThreshold.toString()
                        })

                        edit.clearFocus()
                        lowerAlertTextWatcher.pid = item
                        edit.addTextChangedListener(lowerAlertTextWatcher)
                    }

                    root.findViewById<EditText>(R.id.pid_details_alert_upper_threshold).let{ edit ->
                        edit.removeTextChangedListener(upperAlertTextWatcher)

                        edit.setText(if (item.source.alert.upperThreshold == null){
                            ""
                        } else {
                            item.source.alert.upperThreshold.toString()
                        })
                        edit.clearFocus()
                        upperAlertTextWatcher.pid = item
                        edit.addTextChangedListener(upperAlertTextWatcher)
                    }

                    val pidDetailsSortedMap = root.findViewById<TextView>(R.id.pid_details_supported)
                    if (item.supported){
                        pidDetailsSortedMap.text = "Yes"
                    } else {
                        pidDetailsSortedMap.text = "No"
                    }

                    root.findViewById<Button>(R.id.pid_list_save).apply {
                        setOnClickListener {


                            persistSelection(item.source)
                        }
                    }
                }
            }
        }
    }

    private fun persistSelection(pid: PidDefinition) {
        val newList = data.filter { it.checked }
            .map { it.source.id.toString() }.toList()

        Log.e(LOG_TAG, "Key=$key, selected PIDs=$newList")

        if (Prefs.getStringSet(key).toSet() != newList.toSet()) {
            sendBroadcastEvent("${key}.event.changed")
        }

        pid.serialize()
        Prefs.updateStringSet(key, newList)
    }
}

