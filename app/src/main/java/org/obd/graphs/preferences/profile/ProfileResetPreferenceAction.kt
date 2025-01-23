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
package org.obd.graphs.preferences.profile

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import org.obd.graphs.R
import org.obd.graphs.activity.navigateToPreferencesScreen
import org.obd.graphs.profile.profile

class ProfileResetPreferenceAction(
    context: Context,
    attrs: AttributeSet?,
) : Preference(context, attrs) {
    init {
        setOnPreferenceClickListener {
            val builder = AlertDialog.Builder(context)
            val title = context.getString(R.string.pref_profile_reset_confirmation_dialog)
            val yes = context.getString(R.string.trip_delete_dialog_ask_question_yes)
            val no = context.getString(R.string.trip_delete_dialog_ask_question_no)

            builder
                .setMessage(title)
                .setCancelable(false)
                .setPositiveButton(yes) { _, _ ->
                    profile.reset()
                    navigateToPreferencesScreen("pref.profiles")
                }.setNegativeButton(no) { dialog, _ ->
                    dialog.dismiss()
                }
            val alert = builder.create()
            alert.show()
            true
        }
    }
}
