package top.tau2c.pds.core

/**
 * JNI bridge linking the Kotlin PdsService with the Rust pds-ffi engine.
 */
object NativePds {
    init {
        System.loadLibrary("pds_ffi")
    }

    external fun init(dataDir: String, nodeName: String): Boolean

    external fun handleIpcRequestJson(requestJson: String, callingUid: Int): String

    external fun registerClient(clientName: String, publicKey: ByteArray): String

    external fun approveClient(clientId: String, permissionsJson: String): Boolean
}
