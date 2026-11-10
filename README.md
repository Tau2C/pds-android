# PDS Android (Personal Data Store for Android)

Projekt dostarczający centralną usługę systemową PDS Core na platformę Android (`top.tau2c.pds.core`) oraz bibliotekę kliencką SDK (`top.tau2c.pds.client`) dla aplikacji zewnętrznych.

---

## Architektura

```text
+-----------------------+     +-----------------------+
|  Aplikacja Notatek    |     |  Aplikacja To-Do      |
|  (Klient PDS SDK)     |     |  (Klient PDS SDK)     |
+-----------+-----------+     +-----------+-----------+
            |                             |
            |   Android IPC (AIDL/Binder) |
            +--------------+--------------+
                           |
                           v
        +-------------------------------------+
        |       Aplikacja: PDS Core           |
        |      `top.tau2c.pds.core`           |
        |                                     |
        |  +-------------------------------+  |
        |  |  PdsService (Bound Service)   |  |
        |  +---------------+---------------+  |
        |                  | (JNI)            |
        |                  v                  |
        |  +-------------------------------+  |
        |  |  libpds_ffi.so (Rust cdylib)  |  |
        |  |  - DAG / CAS Blobs / E2EE     |  |
        |  |  - Sync P2P / IPC Server      |  |
        |  +-------------------------------+  |
        |                  |                  |
        |                  v                  |
        |  +-------------------------------+  |
        |  |  Prywatny magazyn aplikacji   |  |
        |  |  /data/data/top.tau2c.pds.core|  |
        |  +-------------------------------+  |
        +-------------------------------------+
```

---

## Moduły projektu

1. **`pds-ffi/`**:
   - Crate Rust typu `cdylib` i `rlib`.
   - Łączy się z silnikiem `pds-rs` i udostępnia eksporty JNI dla Androida:
     - `Java_top_tau2c_pds_core_NativePds_init`
     - `Java_top_tau2c_pds_core_NativePds_handleIpcRequestJson`
     - `Java_top_tau2c_pds_core_NativePds_registerClient`
     - `Java_top_tau2c_pds_core_NativePds_approveClient`

2. **`app/` (`top.tau2c.pds.core`)**:
   - Centralna aplikacja Androida z usługą `PdsService` (`Bound Service`).
   - Weryfikuje tożsamość wywołującego UID (`Binder.getCallingUid()`).
   - Udostępnia interfejs AIDL: `top.tau2c.pds.core.IPdsService`.

3. **`client-sdk/` (`top.tau2c.pds.client`)**:
   - Biblioteka Android AAR przeznaczona do załączenia w aplikacjach trzecich.
   - Zawiera klasę `PdsAndroidClient` zarządzającą łączeniem z `top.tau2c.pds.core.PDS_SERVICE`.

---

## Budowanie natywnej biblioteki Rust dla Androida

Wymaga zainstalowanego Android NDK oraz narzędzia `cargo-ndk`:

```bash
cargo install cargo-ndk
rustup target add aarch64-linux-android x86_64-linux-android

# Kompilacja dla urządzeń ARM64 i emulatora x86_64:
cargo ndk -t arm64-v8a -t x86_64 -o app/src/main/jniLibs build --release --manifest-path pds-ffi/Cargo.toml
```

---

## Sposób użycia w aplikacji klienckiej

1. Dodaj uprawnienie w `AndroidManifest.xml`:
```xml
<uses-permission android:name="top.tau2c.pds.permission.ACCESS_PDS" />

<queries>
    <package android:name="top.tau2c.pds.core" />
</queries>
```

2. Połącz się z usługą:
```kotlin
val pdsClient = PdsAndroidClient(context)
if (pdsClient.connect()) {
    // Połączono z PDS Core
}
```
