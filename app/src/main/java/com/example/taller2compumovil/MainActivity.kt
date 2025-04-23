package com.example.taller2compumovil

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.taller2compumovil.databinding.ActivityMainBinding

/* Para copiar y pegar xdd

class NombreClase : AppCompatActivity() {

    private lateinit var binding: Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Lo del binding
        binding = Binding.inflate(layoutInflater)
        setContentView(binding.root)

    }
}

 */


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.imagenContactos.setOnClickListener {
            val intent = Intent(this, ListaContactos::class.java)
            startActivity(intent)
        }

        binding.imagenCamara.setOnClickListener(){
            val intent = Intent(this, GaleriaImagenes::class.java)
            startActivity(intent)
        }

        binding.imagenMapa.setOnClickListener(){
            val intent = Intent(this, MapaActivity::class.java)
            startActivity(intent)
        }

    }
}