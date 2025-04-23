package com.example.taller2compumovil

import android.Manifest
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.taller2compumovil.databinding.ActivityListaContactosBinding

class ListaContactos : AppCompatActivity() {
    private lateinit var binding: ActivityListaContactosBinding
    private val contactos = mutableListOf<Contacto>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListaContactosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        verificarPermisoContactos()
    }

    private fun verificarPermisoContactos() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            cargarContactos()
        } else {
            solicitarPermisoContactos()
        }
    }

    private fun solicitarPermisoContactos() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_CONTACTS),
            PERMISO_CONTACTOS
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISO_CONTACTOS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                cargarContactos()
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CONTACTS)) {
                    Toast.makeText(this, "El permiso es necesario para mostrar contactos", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Habilita el permiso en Configuración", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun cargarContactos() {
        val cursor: Cursor? = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            null,
            null,
            null
        )

        cursor?.use {
            val nombresote = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            var numeroOrden = 1

            while (it.moveToNext()) {
                val nombre = it.getString(nombresote)
                contactos.add(Contacto(numeroOrden.toString(), nombre))
                numeroOrden++
            }
        }

        val adaptador = ContactosAdapter(this, contactos)
        binding.listaContactos.adapter = adaptador
    }

    companion object {
        private const val PERMISO_CONTACTOS = 1
    }
}