package com.example.smarttraffic

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.security.MessageDigest

class DBHelper(context: Context) : SQLiteOpenHelper(context, "SemaforoDB", null, 1) {

    override fun onCreate(db: SQLiteDatabase?) {
        // 1. Tabla de seguridad OT (ISO 27400)
        db?.execSQL("CREATE TABLE usuarios (id INTEGER PRIMARY KEY AUTOINCREMENT, usuario TEXT UNIQUE, password TEXT)")

        // 2. Tabla de Interoperabilidad IoT (Paso 3 de la Guía): Registro de datos capturados
        db?.execSQL("CREATE TABLE historico_trafico (id INTEGER PRIMARY KEY AUTOINCREMENT, evento TEXT, fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP)")

        // Operador por defecto de INACAP: operador / inacap2026
        val passwordHasheada = hashPassword("inacap2026")
        db?.execSQL("INSERT INTO usuarios (usuario, password) VALUES ('operador', '$passwordHasheada')")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS usuarios")
        db?.execSQL("DROP TABLE IF EXISTS historico_trafico")
        onCreate(db)
    }

    // Hasheo criptográfico SHA-256 para protección de credenciales (ISO 27400)
    fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun validarUsuario(usuario: String, pass: String): Boolean {
        val db = this.readableDatabase
        val passCifrada = hashPassword(pass)
        val cursor = db.rawQuery("SELECT * FROM usuarios WHERE usuario=? AND password=?", arrayOf(usuario, passCifrada))
        val existe = cursor.count > 0
        cursor.close()
        return existe
    }

    // Función para registrar eventos y cumplir con la persistencia de datos IoT
    fun registrarEventoTrafico(evento: String) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("evento", evento)
        }
        db.insert("historico_trafico", null, values)
    }
}
