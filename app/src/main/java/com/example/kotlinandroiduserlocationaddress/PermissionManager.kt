package com.example.kotlinandroiduserlocationaddress

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker

class PermissionManager private constructor() {
    private var context: Context? = null

    private fun init(context: Context) {
        this.context = context
    }

    fun checkPermissions(permissions: Array<String>): Boolean {
        permissions.size

        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(
                    context!!,
                    permission
                ) == PermissionChecker.PERMISSION_DENIED
            ) {
                return false
            }
        }
        return true
    }

    fun askPermissions(activity: Activity, permissions: Array<String>, requestCode: Int) {
        ActivityCompat.requestPermissions(activity, permissions, requestCode)
    }

    fun handlePermissionResult(
        activity: Activity?, grantResults: IntArray
    ): Boolean {
        if (grantResults.isNotEmpty()) {
            var areAllPermissionsGranted = true
            for (grantResult in grantResults) {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(activity, "Permission granted.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(activity, "Permission denied.", Toast.LENGTH_SHORT).show()
                    areAllPermissionsGranted = false
                    break
                }
            }
            return areAllPermissionsGranted
            //showPermissionRational(activity, requestCode);
        }
        return false
    }

    private fun showPermissionRational(activity: Activity, requestCode: Int) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            showMessageOKCancel { dialog: DialogInterface?, which: Int ->
                askPermissions(
                    activity,
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.CAMERA
                    ),
                    requestCode
                )
            }
            return
        }
    }

    private fun showMessageOKCancel(onClickListener: DialogInterface.OnClickListener?) {
        AlertDialog.Builder(context!!)
            .setMessage("You need to allow access to the permission(s)!")
            .setPositiveButton("Ok", onClickListener)
            .setNegativeButton("Cancel", onClickListener)
            .create()
            .show()
    }

    companion object {
        private var instance: PermissionManager? = null
        fun getInstance(context: Context): PermissionManager? {
            if (instance == null) {
                instance = PermissionManager()
            }
            instance!!.init(context)
            return instance
        }
    }
}