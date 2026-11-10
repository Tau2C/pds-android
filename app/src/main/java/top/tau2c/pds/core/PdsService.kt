package top.tau2c.pds.core

import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.IBinder
import android.util.Log

class PdsService : Service() {

    companion object {
        private const val TAG = "PdsService"
        const val PERMISSION_ACCESS_PDS = "top.tau2c.pds.permission.ACCESS_PDS"
    }

    private var isInitialized = false

    private val binder = object : IPdsService.Stub() {
        override fun executeRequest(requestJson: String?): String {
            enforceCallingPermission()
            if (requestJson == null) {
                return """{"status":"Error","payload":"Empty request payload"}"""
            }
            val callingUid = Binder.getCallingUid()
            return NativePds.handleIpcRequestJson(requestJson, callingUid)
        }

        override fun registerClient(clientName: String?, publicKey: ByteArray?): String {
            enforceCallingPermission()
            if (clientName == null || publicKey == null || publicKey.size != 32) {
                return ""
            }
            return NativePds.registerClient(clientName, publicKey)
        }

        override fun approveClient(clientId: String?, permissionsJson: String?): Boolean {
            enforceCallingPermission()
            if (clientId == null || permissionsJson == null) {
                return false
            }
            return NativePds.approveClient(clientId, permissionsJson)
        }

        override fun isReady(): Boolean {
            return isInitialized
        }
    }

    override fun onCreate() {
        super.onCreate()
        val dataDir = filesDir.resolve("pds_store").absolutePath
        val nodeName = "android-${android.os.Build.MODEL}"
        Log.i(TAG, "Initializing PDS Core engine at: $dataDir")
        isInitialized = NativePds.init(dataDir, nodeName)
        if (!isInitialized) {
            Log.e(TAG, "Failed to initialize PDS Core native storage engine")
        } else {
            Log.i(TAG, "PDS Core native engine initialized successfully")
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    private fun enforceCallingPermission() {
        val callingUid = Binder.getCallingUid()
        val callingPid = Binder.getCallingPid()
        if (callingUid == android.os.Process.myUid()) {
            return
        }
        val permissionResult = checkPermission(PERMISSION_ACCESS_PDS, callingPid, callingUid)
        if (permissionResult != PackageManager.PERMISSION_GRANTED) {
            throw SecurityException("Calling app (UID: $callingUid) does not have permission $PERMISSION_ACCESS_PDS")
        }
    }
}
