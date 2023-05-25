package com.example.testbluepoop

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.testbluepoop.ui.theme.TestBluePoopTheme
import kotlinx.coroutines.flow.update

class MainActivity : ComponentActivity() {
    val TAG = javaClass.simpleName
    val vm = Vm()
    val listenersList = mutableListOf<BroadcastReceiver>()
    var bluetoothManager: BluetoothManager? = null
    var audioManager: AudioManager? = null
    val launcher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) {
            setupBluePoopBroadcastListener(applicationContext)
            setupBluePoopAdapterListener(applicationContext)
            setupHeadSetBroadcastListener(applicationContext)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bluetoothManager = getSystemService(BluetoothManager::class.java)
        audioManager = getSystemService(AudioManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            launcher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            setupBluePoopBroadcastListener(applicationContext)
            setupBluePoopAdapterListener(applicationContext)
            setupHeadSetBroadcastListener(applicationContext)
        }
        setContent {
            TestBluePoopTheme {
                // A surface container using the 'background' color from the theme
                Box(modifier = Modifier.fillMaxSize()) {
                    MainContent(vm, modifier = Modifier.align(Alignment.Center))
                }

            }
        }
    }

    private fun setupBluePoopBroadcastListener(context: Context) {
        val bluetoothReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action
                when (action) {
                    BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                        val state = intent.getIntExtra(
                            BluetoothHeadset.EXTRA_STATE,
                            BluetoothHeadset.STATE_DISCONNECTED
                        )
                        if (state == BluetoothHeadset.STATE_CONNECTED) {
                            vm.variantConnectionFirst.update { "STATE_CONNECTED" }
                        } else if (state == BluetoothHeadset.STATE_DISCONNECTED) {
                            vm.variantConnectionFirst.update { "STATE_DISCONNECTED" }
                        }
                    }

                    BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED -> {
                        val state = intent.getIntExtra(
                            BluetoothHeadset.EXTRA_STATE,
                            BluetoothHeadset.STATE_AUDIO_DISCONNECTED
                        )
                        if (state == BluetoothHeadset.STATE_AUDIO_CONNECTED) {
                            vm.variantConnectionFirst.update { "STATE_CONNECTED" }
                        } else {
                            vm.variantConnectionFirst.update { "STATE_DISCONNECTED" }
                        }
                    }
                }

            }
        }
        val filter = IntentFilter(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
        filter.addAction(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED)
        context.registerReceiver(bluetoothReceiver, filter)
        listenersList.add(bluetoothReceiver)
    }

    private fun setupHeadSetBroadcastListener(context: Context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                if (intent.action == AudioManager.ACTION_HEADSET_PLUG) {
                    vm.headsetManual.update { intent.getIntExtra("state", -1).toString() }
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_HEADSET_PLUG)
        context.registerReceiver(receiver, filter)
        listenersList.add(receiver)
    }

    private fun setupBluePoopAdapterListener(context: Context) {
        val listener = object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                if (profile == BluetoothProfile.HEADSET) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (context.checkSelfPermission(
                                Manifest.permission.BLUETOOTH_CONNECT
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            helper(proxy)
                        } else {
                            Toast.makeText(context, "permission error", Toast.LENGTH_SHORT)
                                .show()
                        }
                    } else {
                        helper(proxy)
                    }
                }
            }

            override fun onServiceDisconnected(profile: Int) {
                if (profile == BluetoothProfile.HEADSET) vm.variantConnectionFirst.update { "STATE_DISCONNECTED" }
            }

            @SuppressLint("MissingPermission")
            fun helper(proxy: BluetoothProfile?) {
                val bluetoothHeadset = proxy as BluetoothHeadset
                val connectedDevices = bluetoothHeadset.connectedDevices
                if (connectedDevices.isNotEmpty()) {
                    var connected = false
                    var supportsAudio = false
                    // check for a device that supports audio and is connected in our connected bluetooth devices.
                    for (device in proxy.connectedDevices) {
                        connected =
                            proxy.getConnectionState(device) == BluetoothProfile.STATE_CONNECTED
                        supportsAudio =
                            device.bluetoothClass.hasService(BluetoothClass.Service.AUDIO)
                        // we have found a connected device that supports audio, stop iterating and emit a success
                        if (connected && supportsAudio) {
                            break
                        }
                    }
                    if (connected && supportsAudio) vm.variantConnectionFirst.update { "STATE_CONNECTED" }
                } else {
                    vm.variantConnectionFirst.update { "STATE_DISCONNECTED" }
                }
            }
        }
        bluetoothManager?.adapter?.getProfileProxy(context, listener, BluetoothProfile.HEADSET)
    }

    override fun onDestroy() {
        listenersList.forEach { unregisterReceiver(it) }
        super.onDestroy()
    }
}

@Composable
fun MainContent(vm: Vm, modifier: Modifier = Modifier) {
    val variantFirst by vm.variantConnectionFirst.collectAsState()
    val manual by vm.headsetManual.collectAsState()
    Column(modifier = modifier) {
        Log(text = "Headphone state")
        Log(text = variantFirst)
        Log(text = "headset manual(not bluepoop)")
        Log(text = manual)
    }
}

@Composable
fun Log(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier
    )
}
