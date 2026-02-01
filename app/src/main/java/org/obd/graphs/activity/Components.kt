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
package org.obd.graphs.activity

import android.view.View
import android.widget.Chronometer
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import org.obd.graphs.R

fun MainActivity.floatingActionButton(func: (p: FloatingActionButton) -> Unit) {
    func(findViewById(R.id.connect_btn))
}

fun MainActivity.progressBar(func: (p: ProgressBar) -> Unit) {
    func(findViewById(R.id.p_bar))
}

fun MainActivity.timer(func: (p: Chronometer) -> Unit) {
    func(findViewById(R.id.timer))
}

fun MainActivity.bottomAppBar(func: (p: BottomAppBar) -> Unit) {
    func(findViewById(R.id.bottom_app_bar))
}

fun MainActivity.leftAppBar(func: (p: NavigationView) -> Unit) {
    func(findViewById(R.id.leftNavView))
}

fun MainActivity.navController(func: (p: NavController) -> Unit) {
    func((supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController)
}

fun MainActivity.lockScreenDialogShow(func: (dialogTitle: TextView) -> Unit) {
    lockScreenDialog?.let {
        if (it.isShowing) {
            it.dismiss()
        }
    }

    AlertDialog.Builder(this).run {
        setCancelable(false)
        val dialogView: View = layoutInflater.inflate(R.layout.dialog_screen_lock, null)
        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialog_screen_lock_message_id)
        func(dialogTitle)
        setView(dialogView)
        lockScreenDialog = create()
        lockScreenDialog.show()
    }
}
