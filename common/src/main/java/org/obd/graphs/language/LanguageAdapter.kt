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
package org.obd.graphs.language

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import org.obd.graphs.commons.R

class LanguageAdapter(
    private val items: Array<String>,
    private val selectedIndex: Int,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.language_item_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_language, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.textView.text = items[position]

        val context = holder.itemView.context

        if (position == selectedIndex) {
            holder.textView.setTypeface(null, Typeface.BOLD)
            holder.textView.setTextColor(ContextCompat.getColor(context, R.color.philippine_green))
        } else {
            holder.textView.setTypeface(null, Typeface.NORMAL)
            holder.textView.setTextColor(ContextCompat.getColor(context, R.color.gray_dark))
            holder.textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
        }

        holder.itemView.setOnClickListener {
            onItemClick(position)
        }
    }

    override fun getItemCount(): Int = items.size
}
