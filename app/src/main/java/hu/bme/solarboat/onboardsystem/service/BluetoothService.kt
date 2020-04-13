package hu.bme.solarboat.onboardsystem.service

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
import java.util.*

val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")

const val STATE_NONE = 0 // we're doing nothing
const val STATE_CONNECTING = 1 // now initiating an outgoing connection
const val STATE_CONNECTED = 2 // now connected to a remote device


class BluetoothService(val mHandler: Handler) {
    private val mAdapter = BluetoothAdapter.getDefaultAdapter()
    private var mConnectThread: ConnectThread? = null
    private var mConnectedThread: ConnectedThread? = null

    var mState = STATE_NONE

    @Synchronized
    fun connect(bluetoothDevice: BluetoothDevice?) {
        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread!!.cancel()
                mConnectThread = null
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread!!.cancel()
            mConnectedThread = null
        }


        // Start the thread to connect with the given device
        mConnectThread = ConnectThread(bluetoothDevice).apply {
            start()
        }
    }

    @Synchronized
    private fun connected(socket: BluetoothSocket?) {
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

    @Synchronized
    fun stop() {
        if (mConnectThread != null) {
            mConnectThread!!.cancel()
            mConnectThread = null
        }

        if (mConnectedThread != null) {
            mConnectedThread!!.cancel()
            mConnectedThread = null
        }
        mState = STATE_NONE
    }

    fun write(out: ByteArray) {
        var connectedThread: ConnectedThread
        synchronized(this) {
            if (mState != STATE_CONNECTED) return
            connectedThread = mConnectedThread!!
        }
        connectedThread.write(out)
    }

    private fun connectionFailed() {
        // Send a failure message back to the Activity
        sendToast("Unable to connect device")
        mState = STATE_NONE
    }

    private fun connectionLost() {
        sendToast("Device connection was lost")
        mState = STATE_NONE
    }

    private fun sendToast(message: String) {
        val msg: Message = mHandler.obtainMessage(MESSAGE_TOAST)
        val bundle = Bundle()
        bundle.putString(TOAST, message)
        msg.data = bundle
        mHandler.sendMessage(msg)
    }

    inner class ConnectThread(device: BluetoothDevice?) : Thread() {
        private lateinit var mmSocket: BluetoothSocket

        init {
            try {
                mmSocket = device?.createInsecureRfcommSocketToServiceRecord(MY_UUID)!!
            } catch (e: IOException) {
                e.printStackTrace()
            }
            mState = STATE_CONNECTING
        }

        override fun run() {
            //Cancel discovery to faster connection
            mAdapter.cancelDiscovery()

            var count = 0
            val maxTry = 5

            while (true) {
                try {
                    try {
                        count++
                        mmSocket.connect()
                        break
                    } catch (e: IOException) {
                        if (count > maxTry) throw e
                    }
                } catch (e: IOException) {
                    try {
                        mmSocket.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                    connectionFailed()
                    return
                }
            }

            synchronized(this) { mConnectThread = null }

            connected(mmSocket)
        }

        fun cancel() {
            try {
                mmSocket.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    inner class ConnectedThread(val mmSocket: BluetoothSocket?) : Thread() {
        private lateinit var mmInStream: InputStream
        private lateinit var mmOutStream: OutputStream

        init {
            try {
                mmInStream = mmSocket?.inputStream!!
                mmOutStream = mmSocket.outputStream
            } catch (e: IOException) {
                e.printStackTrace()
            }
            mState = STATE_CONNECTED
        }

        override fun run() {
            val jsonReader = JsonReader(InputStreamReader(mmInStream))

            while (mmSocket?.isConnected == true) {
                try {
                    val json = JsonParser.parseReader(jsonReader)
                    val message = json.asJsonObject.toString()
                    mHandler.obtainMessage(MESSAGE_READ, message).sendToTarget()
                } catch (e: JsonIOException) {
                    e.printStackTrace()
                } catch (e: JsonSyntaxException) {
                    e.printStackTrace()
                } catch (e: Exception) {
                    connectionLost()
                    break
                }
            }
        }

        fun write(buffer: ByteArray) {
            try {
                mmOutStream.write(buffer)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}