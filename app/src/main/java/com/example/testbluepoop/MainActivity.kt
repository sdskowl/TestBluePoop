package com.example.testbluepoop

import android.Manifest
import android.bluetooth.BluetoothAdapter
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
import androidx.core.app.ActivityCompat
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
            setupBluePoopAdapterListener(applicationContext)
            setupHeadSetBroadcastListener(applicationContext)
        }
    }
    var proxy:BluetoothProfile? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bluetoothManager = getSystemService(BluetoothManager::class.java)
        audioManager = getSystemService(AudioManager::class.java)
        setupBluePoopBroadcastListener(applicationContext)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            launcher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
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

    private fun getHeadsetStateBluetooth(context: Context): String {

        val adapter = bluetoothManager?.adapter
        return when {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED -> {
                "permission failed"
            }

            adapter?.getProfileConnectionState(BluetoothProfile.HEADSET) == BluetoothAdapter.STATE_CONNECTED -> {
                "BluetoothAdapter.STATE_CONNECTED"
            }

            else -> {
                "unexpected"
            }
        }

    }

    private fun setupBluePoopBroadcastListener(context: Context) {
        val bluetoothReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action
                if (action == BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED) {
                    val state = intent.getIntExtra(
                        BluetoothHeadset.EXTRA_STATE,
                        BluetoothHeadset.STATE_DISCONNECTED
                    )
                    if (state == BluetoothHeadset.STATE_CONNECTED) {
                        vm.variantConnectionFirst.update { getHeadsetStateBluetooth(context) }
                    } else if (state == BluetoothHeadset.STATE_DISCONNECTED) {
                        vm.variantConnectionFirst.update { "STATE_DISCONNECTED" }
                    }
                }
            }
        }
        val filter = IntentFilter(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
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
                    this@MainActivity.proxy = proxy
                   if (context.checkSelfPermission(
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED ) {
                       val bluetoothHeadset = proxy as BluetoothHeadset
                       val connectedDevices = bluetoothHeadset.connectedDevices
                       if (connectedDevices.isNotEmpty()) {
                           vm.variantConnectionSecond.update { "STATE_CONNECTED" }
                       } else {
                           vm.variantConnectionSecond.update { "STATE_EMPTY" }
                       }
                   } else {
                       Toast.makeText(context, "permission error", Toast.LENGTH_SHORT)
                           .show()
                   }

                }
            }

            override fun onServiceDisconnected(profile: Int) {
                vm.variantConnectionSecond.update { "wtf $profile" }
                if (profile == BluetoothProfile.HEADSET) vm.variantConnectionSecond.update { "STATE_DISCONNECTED" }
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
    val variantSecond by vm.variantConnectionSecond.collectAsState()
    val manual by vm.headsetManual.collectAsState()
    Column(modifier = modifier) {
        Log(text = "Variant1")
        Log(text = variantFirst)
        Log(text = "Variant2")
        Log(text = variantSecond)
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
