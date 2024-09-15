/**
 * Copyright 2019-2024, Tomasz Żebrowski
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
package org.obd.graphs.preferences

import android.annotation.SuppressLint
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import org.obd.graphs.getContext

val Prefs: SharedPreferences by lazy {
   PreferenceManager.getDefaultSharedPreferences(
       getContext()!!
    )
}

fun SharedPreferences.Editor.updatePreference(
    prefName: String,
    value: Any?
) {
    when (value) {
        is String -> {
            putString(prefName, value)
        }
        is Set<*> -> {
            putStringSet(prefName, value as MutableSet<String>?)
        }
        is Int -> {
            putInt(prefName, value)
        }
        is Boolean -> {
            putBoolean(prefName, value)
        }
    }
}

fun SharedPreferences.updateBoolean(key: String, value: Boolean): SharedPreferences.Editor {
    edit().putBoolean(key, value).apply()
    return edit()
}

fun SharedPreferences.updateString(key: String, value: String?): SharedPreferences.Editor {
    edit().putString(key, value).apply()
    return edit()
}

@SuppressLint("ApplySharedPref")
fun SharedPreferences.updateInt(key: String, value: Int){
   edit().putInt(key, value).commit()
}

@SuppressLint("ApplySharedPref")
fun SharedPreferences.updateStringSet(key: String, list: List<String>) {
    edit().putStringSet(key, list.map { l -> l }.toSet()).commit()
}

@SuppressLint("ApplySharedPref")
fun SharedPreferences.updateLongSet(key: String, list: List<Long>) {
    edit().putStringSet(key, list.map { l -> l.toString() }.toSet()).commit()
}

fun SharedPreferences.getLongSet(key: String, defaults: MutableSet<String> = mutableSetOf()): Set<Long> {
    return getStringSet(key,defaults)?.map { s -> s.toLong() }?.toSet()!!
}

fun SharedPreferences.getStringSet(key: String): MutableSet<String> {
    return getStringSet(key, emptySet())!!
}

fun SharedPreferences.isEnabled(key: String): Boolean {
    return getBoolean(key, false)
}

fun SharedPreferences.getS(name: String, default: String): String {
    return getString(name, default)!!
}

fun SharedPreferences.getString(name: String): String? {
    return getString(name, null)
}