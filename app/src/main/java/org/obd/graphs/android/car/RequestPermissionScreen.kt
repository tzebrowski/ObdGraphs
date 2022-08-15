package org.obd.graphs.android.car


import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.provider.Settings
import androidx.car.app.CarAppPermission
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.core.location.LocationManagerCompat
import org.obd.graphs.R


class RequestPermissionScreen @JvmOverloads constructor(
    carContext: CarContext,
    private val mPreSeedMode: Boolean = false
) :
    Screen(carContext) {

    private val mRefreshAction = Action.Builder()
        .setTitle(getCarContext().getString(R.string.refresh_action_title))
        .setBackgroundColor(CarColor.BLUE)
        .setOnClickListener { invalidate() }
        .build()

    override fun onGetTemplate(): Template {
        val headerAction = if (mPreSeedMode) Action.APP_ICON else Action.BACK
        val permissions: MutableList<String> = ArrayList()
        val declaredPermissions: Array<String> = try {
            val info = carContext.packageManager.getPackageInfo(
                carContext.packageName,
                PackageManager.GET_PERMISSIONS
            )
            info.requestedPermissions
        } catch (e: PackageManager.NameNotFoundException) {
            return MessageTemplate.Builder(
                carContext.getString(R.string.package_not_found_error_msg)
            )
                .setHeaderAction(headerAction)
                .addAction(mRefreshAction)
                .build()
        }
        for (declaredPermission in declaredPermissions) {
            // Don't include permissions against the car app host as they are all normal but
            // show up as ungranted by the system.
            if (declaredPermission.startsWith("androidx.car.app")) {
                continue
            }
            try {
                CarAppPermission.checkHasPermission(carContext, declaredPermission)
            } catch (e: SecurityException) {
                permissions.add(declaredPermission)
            }
        }
        if (permissions.isEmpty()) {
            return MessageTemplate.Builder(
                carContext.getString(R.string.permissions_granted_msg)
            )
                .setHeaderAction(headerAction)
                .addAction(
                    Action.Builder()
                        .setTitle(carContext.getString(R.string.close_action_title))
                        .setOnClickListener { finish() }
                        .build())
                .build()
        }
        val message = StringBuilder()
            .append(carContext.getString(R.string.needs_access_msg_prefix))
        for (permission in permissions) {
            message.append(permission)
            message.append("\n")
        }
        val listener: OnClickListener = ParkedOnlyOnClickListener.create {
            carContext.requestPermissions(
                permissions
            ) { approved: List<String?>?, rejected: List<String?>? ->
                CarToast.makeText(
                    carContext,
                    String.format("Approved: %s Rejected: %s", approved, rejected),
                    CarToast.LENGTH_LONG
                ).show()
            }
            if (!carContext.packageManager
                    .hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)
            ) {
                CarToast.makeText(
                    carContext,
                    carContext.getString(R.string.phone_screen_permission_msg),
                    CarToast.LENGTH_LONG
                ).show()
            }
        }
        val action = Action.Builder()
            .setTitle(carContext.getString(R.string.grant_access_action_title))
            .setBackgroundColor(CarColor.BLUE)
            .setOnClickListener(listener)
            .build()
        var action2: Action? = null
        val locationManager =
            carContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!LocationManagerCompat.isLocationEnabled(locationManager)) {
            message.append(
                carContext.getString(R.string.enable_location_permission_on_device_msg)
            )
            message.append("\n")
            action2 = Action.Builder()
                .setTitle(carContext.getString(R.string.enable_location_action_title))
                .setBackgroundColor(CarColor.BLUE)
                .setOnClickListener(ParkedOnlyOnClickListener.create {
                    carContext.startActivity(
                        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK
                        )
                    )
                    if (!carContext.packageManager.hasSystemFeature(
                            PackageManager.FEATURE_AUTOMOTIVE
                        )
                    ) {
                        CarToast.makeText(
                            carContext,
                            carContext.getString(
                                R.string.enable_location_permission_on_phone_msg
                            ),
                            CarToast.LENGTH_LONG
                        ).show()
                    }
                })
                .build()
        }
        val builder = LongMessageTemplate.Builder(message)
            .setTitle(carContext.getString(R.string.required_permissions_title))
            .addAction(action)
            .setHeaderAction(headerAction)
        if (action2 != null) {
            builder.addAction(action2)
        }
        return builder.build()
    }
}
