package top.tau2c.pds.client

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import top.tau2c.pds.core.IPdsService

/**
 * High-level Android SDK client for integrating with the central PDS Core background service.
 */
class PdsAndroidClient(private val context: Context) {

    companion object {
        const val PDS_SERVICE_ACTION = "top.tau2c.pds.core.PDS_SERVICE"
        const val PDS_PACKAGE_NAME = "top.tau2c.pds.core"
    }

    private var pdsService: IPdsService? = null
    private var isBound = false

    interface ConnectionListener {
        fun onConnected(client: PdsAndroidClient)
        fun onDisconnected()
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            pdsService = IPdsService.Stub.asInterface(service)
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            pdsService = null
            isBound = false
        }
    }

    /**
     * Binds to the background PDS Core service.
     */
    fun connect(): Boolean {
        val intent = Intent(PDS_SERVICE_ACTION).apply {
            setPackage(PDS_PACKAGE_NAME)
        }
        isBound = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        return isBound
    }

    /**
     * Unbinds from the PDS Core service.
     */
    fun disconnect() {
        if (isBound) {
            context.unbindService(serviceConnection)
            isBound = false
            pdsService = null
        }
    }

    fun isConnected(): Boolean {
        return isBound && pdsService != null
    }

    /**
     * Sends a raw JSON request to PDS and returns the raw JSON response.
     */
    fun executeRawRequest(requestJson: String): String {
        val service = pdsService ?: throw IllegalStateException("PdsService is not connected")
        return service.executeRequest(requestJson)
    }

    /**
     * Registers this client application.
     */
    fun register(clientName: String, publicKey: ByteArray): String {
        val service = pdsService ?: throw IllegalStateException("PdsService is not connected")
        return service.registerClient(clientName, publicKey)
    }

    /**
     * Checks if PDS storage engine is ready.
     */
    fun isReady(): Boolean {
        return pdsService?.isReady ?: false
    }
}
