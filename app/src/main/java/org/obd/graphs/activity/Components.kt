package org.obd.graphs.activity

import android.view.View
import android.widget.Chronometer
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.obd.graphs.R

fun MainActivity.floatingActionButton(func: (p: FloatingActionButton) -> Unit) {
    func(findViewById(R.id.connect_btn))
}

fun MainActivity.toolbar(func: (p: CoordinatorLayout) -> Unit) {
    func(findViewById(R.id.coordinator_Layout))
}

fun MainActivity.progressBar(func: (p: ProgressBar) -> Unit) {
    func(findViewById(R.id.p_bar))
}

fun MainActivity.timer(func: (p: Chronometer) -> Unit) {
    func(findViewById(R.id.timer))
}

fun MainActivity.bottomAppBar(func: (p: BottomAppBar) -> Unit) {
    func(findViewById(R.id.bottomAppBar))
}

fun MainActivity.navController(func: (p: NavController) -> Unit) {
    func((supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController)
}


fun MainActivity.lockScreenDialogShow(func: (dialogTitle: TextView) -> Unit) {

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