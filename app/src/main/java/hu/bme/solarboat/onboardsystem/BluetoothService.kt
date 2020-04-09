package hu.bme.solarboat.onboardsystem

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Bundle
import android.os.Handler
import android.os.Message
import com.google.gson.JsonIOException
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import com.google.gson.stream.JsonReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream

class BluetoothService(val handler: Handler) {

    private var mConnectThread: ConnectThread? = null
    private var mConnectedThread: ConnectedThread? = null

    var mState = STATE_NONE

    private val mAdapter = BluetoothAdapter.getDefaultAdapter()
    fun connect(bluetoothDevice: BluetoothDevice?) {
        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread!!.cancel()
            mConnectedThread = null
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread!!.cancel()
            mConnectedThread = null
        }

        // Start the thread to connect with the given device
        mConnectThread = ConnectThread(bluetoothDevice)
        mConnectThread?.start()
    }

    companion object {
        const val STATE_NONE = 0 // we're doing nothing
        const val STATE_CONNECTING = 2 // now initiating an outgoing connection
        const val STATE_CONNECTED = 3 // now connected to a remote device
        const val MESSAGE_TOAST = 5
        const val MESSAGE_READ = 6

        const val TOAST = "toast"
    }

    inner class ConnectThread(val bluetoothDevice: BluetoothDevice?) : Thread() {

        val socket = bluetoothDevice?.createInsecureRfcommSocketToServiceRecord(MY_UUID)
        init {
            mState = STATE_CONNECTING
        }

        override fun run() {
            mAdapter.cancelDiscovery()

            var count = 0
            try {
                while (true) {
                    try {
                        count++
                        socket?.connect()
                        break
                    } catch (e: IOException) {
                        println(count)
                        if(count >5) throw e
                    }
                }
            } catch (e: IOException) {
                connectionFailed()
                return
            }

            mConnectThread = null

            connected(socket, bluetoothDevice)
        }

        fun cancel() {
            TODO("Not yet implemented")
        }


    }

    inner class ConnectedThread(val socket: BluetoothSocket?):  Thread() {
        private val mmInStream: InputStream? = socket?.inputStream
        private val mmOutStream: OutputStream? = socket?.outputStream
        init {
            mState = STATE_CONNECTED
        }

        override fun run() {
            val jsonReader = JsonReader(InputStreamReader(mmInStream))
            while (mState == STATE_CONNECTED) {
                try {
                    var message = String()
                    try {
                        val json = JsonParser.parseReader(jsonReader)
                        message = json.asJsonObject.toString()
                    } catch (e: JsonIOException) {
                        e.printStackTrace();
                    } catch (e: JsonSyntaxException) {
                        e.printStackTrace();
                    }

                    handler.obtainMessage(MESSAGE_READ, message).sendToTarget()
                } catch (e: Exception) {
                    connectionLost()
                    break
                }
            }
        }

        fun cancel() {
            TODO("Not yet implemented")
        }

    }

    private fun connectionLost() {
        sendToast("Device connection was lost")
        mState = STATE_NONE
    }

    private fun connected(socket: BluetoothSocket?, bluetoothDevice: BluetoothDevice?) {
        sendToast("Connected")
        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread!!.cancel()
            mConnectThread = null
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread!!.cancel()
            mConnectedThread = null
        }


        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = ConnectedThread(socket)
        mConnectedThread?.start()
    }

    private fun connectionFailed() {
        // Send a failure message back to the Activity
        sendToast("Unable to connect device")
        mState = STATE_NONE
    }

    private fun sendToast(message: String) {
        val msg: Message = handler.obtainMessage(MESSAGE_TOAST)
        val bundle = Bundle()
        bundle.putString(TOAST, message)
        msg.data = bundle
        handler.sendMessage(msg)
    }

}