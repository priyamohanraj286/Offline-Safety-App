package com.example.offlinesafety

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.offlinesafety.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SosService : Service() {

    private var bluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
    private var bluetoothScanner: BluetoothLeScanner? = null
    private var mediaPlayer: MediaPlayer? = null

    private val CHANNEL_ID = "sos_channel"
    private val NOTIF_ID = 1

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START_SCAN" -> startScanning()
            "STOP_SCAN" -> stopScanning()
            "START_ADVERTISE" -> startAdvertising()
            "STOP_ADVERTISE" -> stopAdvertising()
        }

        // Show notification only if scanning or advertising active
        val ongoing = (bluetoothScanner != null) || (bluetoothLeAdvertiser != null)
        if (ongoing) {
            startForeground(NOTIF_ID, createNotification())
        } else {
            stopForeground(true)
            stopSelf()
        }

        return START_STICKY
    }

    private fun createNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SOS Service",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Offline Safety Active")
            .setContentText("Listening / Broadcasting SOS")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }

    // ---------------- SCANNING ----------------
    private fun startScanning() {
        val manager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = manager.adapter
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Missing BLUETOOTH_SCAN permission", Toast.LENGTH_SHORT).show()
            return
        }
        bluetoothScanner = adapter.bluetoothLeScanner
        bluetoothScanner?.startScan(
            null,
            ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(),
            scanCallback
        )
    }

    private fun stopScanning() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
            == PackageManager.PERMISSION_GRANTED
        ) {
            bluetoothScanner?.stopScan(scanCallback)
            bluetoothScanner = null
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val data: ByteArray? = result.scanRecord?.getManufacturerSpecificData(0x1234)
            if (data != null && data.isNotEmpty()) {
                val msg = try {
                    String(data, Charsets.UTF_8)
                } catch (_: Exception) {
                    "SOS"
                }
                Toast.makeText(applicationContext, "🚨 SOS: $msg", Toast.LENGTH_LONG).show()
                playAlertSound()
            }
        }
    }

    private fun playAlertSound() {
        try {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer.create(this, R.raw.sos_alert)
                mediaPlayer?.setOnCompletionListener {
                    it.reset()
                    it.release()
                    mediaPlayer = null
                }
            }
            mediaPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ---------------- ADVERTISING ----------------
    private fun startAdvertising() {
        val manager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = manager.adapter
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Missing BLUETOOTH_ADVERTISE permission", Toast.LENGTH_SHORT).show()
            return
        }
        bluetoothLeAdvertiser = adapter.bluetoothLeAdvertiser

        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(this@SosService)
            val user = db.userDao().getUser()
            val travel = db.travelDao().getLatestTravel()

            val msg = buildString {
                append("SOS:")
                append(user?.name ?: "Unknown")
                travel?.let { append(":C${it.coachNumber}-S${it.seatNumber}") }
            }

            val data = AdvertiseData.Builder()
                .addManufacturerData(0x1234, msg.toByteArray())
                .setIncludeDeviceName(false)
                .build()
            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(false)
                .build()
            if (ActivityCompat.checkSelfPermission(
                    this@SosService,
                    Manifest.permission.BLUETOOTH_ADVERTISE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                bluetoothLeAdvertiser?.startAdvertising(settings, data, advertiseCallback)
            }
        }
    }

    private fun stopAdvertising() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_ADVERTISE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
            bluetoothLeAdvertiser = null
        }
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Toast.makeText(applicationContext, "SOS broadcast started", Toast.LENGTH_SHORT).show()
        }
        override fun onStartFailure(errorCode: Int) {
            Toast.makeText(applicationContext, "Failed SOS: $errorCode", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopScanning()
        stopAdvertising()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}