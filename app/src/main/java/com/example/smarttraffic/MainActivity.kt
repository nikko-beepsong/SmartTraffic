package com.example.smarttraffic

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import java.io.IOException
import java.util.*

class MainActivity : ComponentActivity() {

    private var bluetoothSocket: BluetoothSocket? = null
    private val mUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dbHelper = DBHelper(this)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION
            ), 1)
        }

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
            gravity = android.view.Gravity.CENTER
        }

        val etUsuario = EditText(this).apply { hint = "ID Operador (Usuario)" }
        val etPassword = EditText(this).apply {
            hint = "Contraseña"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val btnLogin = Button(this).apply { text = "AUTENTICAR NODO" }

        rootLayout.addView(etUsuario)
        rootLayout.addView(etPassword)
        rootLayout.addView(btnLogin)

        val btnConectar = Button(this).apply {
            text = "CONECTAR AL SEMÁFORO"
            visibility = View.GONE
        }
        val btnEmergencia = Button(this).apply {
            text = "EMERGENCIA (TODO ROJO) 🔴"
            visibility = View.GONE
        }
        val btnNormal = Button(this).apply {
            text = "TRÁFICO NORMAL 🟢"
            visibility = View.GONE
        }

        rootLayout.addView(btnConectar)
        rootLayout.addView(btnEmergencia)
        rootLayout.addView(btnNormal)

        setContentView(rootLayout)

        btnLogin.setOnClickListener {
            val user = etUsuario.text.toString().trim()
            val pass = etPassword.text.toString().trim()

            if (dbHelper.validarUsuario(user, pass)) {
                Toast.makeText(this, "Autenticación Exitosa", Toast.LENGTH_SHORT).show()
                etUsuario.visibility = View.GONE
                etPassword.visibility = View.GONE
                btnLogin.visibility = View.GONE

                btnConectar.visibility = View.VISIBLE
                btnEmergencia.visibility = View.VISIBLE
                btnNormal.visibility = View.VISIBLE
            } else {
                Toast.makeText(this, "Credenciales Inválidas de Operador", Toast.LENGTH_LONG).show()
            }
        }

        btnConectar.setOnClickListener { conectarBluetooth() }
        btnEmergencia.setOnClickListener { enviarDatosBluetooth("E") }
        btnNormal.setOnClickListener { enviarDatosBluetooth("N") }
    }

    private fun conectarBluetooth() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Toast.makeText(this, "Enciende el Bluetooth del celular", Toast.LENGTH_LONG).show()
            return
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return

        val dispositivosVinculados = bluetoothAdapter.bondedDevices
        val esp32Device = dispositivosVinculados.find { it.name == "Semaforo_Bluetooth" }

        if (esp32Device != null) {
            try {
                bluetoothSocket = esp32Device.createRfcommSocketToServiceRecord(mUUID)
                bluetoothSocket?.connect()
                Toast.makeText(this, "Conectado de forma segura al hardware", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                Toast.makeText(this, "Error de enlace. Reintente.", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        } else {
            Toast.makeText(this, "Vincule primero 'Semaforo_Bluetooth' en los ajustes de Android", Toast.LENGTH_LONG).show()
        }
    }

    private fun enviarDatosBluetooth(comando: String) {
        if (bluetoothSocket != null && bluetoothSocket!!.isConnected) {
            try {
                bluetoothSocket!!.outputStream.write(comando.toByteArray())
            } catch (e: IOException) {
                Toast.makeText(this, "Falla en el envío de datos", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        } else {
            Toast.makeText(this, "No hay conexión establecida con el semáforo", Toast.LENGTH_SHORT).show()
        }
    }
}
