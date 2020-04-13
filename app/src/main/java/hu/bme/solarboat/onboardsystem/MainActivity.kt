package hu.bme.solarboat.onboardsystem

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import hu.bme.solarboat.onboardsystem.service.BluetoothService.Companion.MESSAGE_READ
import hu.bme.solarboat.onboardsystem.service.BluetoothService.Companion.MESSAGE_TOAST
import hu.bme.solarboat.onboardsystem.service.BluetoothService.Companion.TOAST
import hu.bme.solarboat.onboardsystem.jsonData.ArduinoData
import hu.bme.solarboat.onboardsystem.service.BluetoothService
import java.util.*

val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
const val REQUEST_DEVICE = 0
const val REQUEST_ENABLE_BT = 1

class MainActivity : AppCompatActivity() {

    private lateinit var bluetoothService: BluetoothService
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_SHORT).show()
            finish()
        }
        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)

            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
        bluetoothService = BluetoothService(mHandler)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.connect_device -> {
                val deviceIntent = Intent(this, DeviceListActivity::class.java)
                startActivityForResult(deviceIntent, REQUEST_DEVICE)
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_ENABLE_BT -> {
                if (resultCode != Activity.RESULT_OK) {
                    Toast.makeText(this, "Bluetooth was not enabled.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            REQUEST_DEVICE -> {
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data)
                }
            }
        }
    }

    private fun connectDevice(data: Intent?) {
        val extras = data?.extras ?: return
        val address = extras.getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS)

        val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(address)
        bluetoothService.connect(bluetoothDevice)
    }

    private val mHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MESSAGE_TOAST -> {
                    Toast.makeText(baseContext, msg.data.getString(TOAST), Toast.LENGTH_SHORT)
                        .show()
                }
                MESSAGE_READ -> {
                    if (msg.obj !is String) {
                        return
                    }
                    val message = msg.obj as String
                    println(message)
                    processData(message)

                }
                else -> super.handleMessage(msg)
            }
        }
    }

    private fun processData(message: String) {
        val data = gson.fromJson(message, ArduinoData::class.java)

        val tiltX = findViewById<TextView>(R.id.tilt_x)
        val tiltY = findViewById<TextView>(R.id.tilt_y)
        val tiltZ = findViewById<TextView>(R.id.tilt_z)
        val accelerationX = findViewById<TextView>(R.id.acceleration_x)
        val accelerationY = findViewById<TextView>(R.id.acceleration_y)
        val accelerationZ = findViewById<TextView>(R.id.acceleration_z)
        val messageTv = findViewById<TextView>(R.id.message)


        tiltX.text = data.tilt.x.toString()
        tiltY.text = data.tilt.y.toString()
        tiltZ.text = data.tilt.z.toString()
        accelerationX.text = data.acceleration.x.toString()
        accelerationY.text = data.acceleration.y.toString()
        accelerationZ.text = data.acceleration.z.toString()
        messageTv.text = data.error.message
    }
}


