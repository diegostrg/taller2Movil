package com.example.taller2compumovil

        import android.Manifest
        import android.app.Activity
        import android.content.ContentValues
        import android.content.Intent
        import android.content.pm.PackageManager
        import android.graphics.Bitmap
        import android.graphics.BitmapFactory
        import android.net.Uri
        import android.os.Build
        import android.os.Bundle
        import android.provider.MediaStore
        import android.widget.Toast
        import androidx.activity.result.contract.ActivityResultContracts
        import androidx.appcompat.app.AppCompatActivity
        import androidx.core.app.ActivityCompat
        import androidx.core.content.ContextCompat
        import com.example.taller2compumovil.databinding.ActivityGaleriaImagenesBinding
        import android.media.ExifInterface
        import android.graphics.Matrix

        class GaleriaImagenes : AppCompatActivity() {

            private lateinit var binding: ActivityGaleriaImagenesBinding
            private var imageUri: Uri? = null

            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                binding = ActivityGaleriaImagenesBinding.inflate(layoutInflater)
                setContentView(binding.root)

                binding.btnGaleria.setOnClickListener {
                    if (verificarPermiso(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        abrirGaleria()
                    } else {
                        solicitarPermiso(Manifest.permission.READ_EXTERNAL_STORAGE, PERMISO_GALERIA)
                    }
                }

                binding.btnCamara.setOnClickListener {
                    if (verificarPermiso(Manifest.permission.CAMERA)) {
                        abrirCamara()
                    } else {
                        solicitarPermiso(Manifest.permission.CAMERA, PERMISO_CAMARA)
                    }
                }
            }

            private fun verificarPermiso(permiso: String): Boolean {
                return ContextCompat.checkSelfPermission(this, permiso) == PackageManager.PERMISSION_GRANTED
            }

            private fun solicitarPermiso(permiso: String, requestCode: Int) {
                ActivityCompat.requestPermissions(this, arrayOf(permiso), requestCode)
            }

            private fun abrirGaleria() {
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                galeriaLauncher.launch(intent)
            }

            private fun abrirCamara() {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.TITLE, "Nueva Imagen")
                    put(MediaStore.Images.Media.DESCRIPTION, "Desde la cámara")
                }
                imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
                }
                camaraLauncher.launch(intent)
            }

            private fun redimensionarBitmap(bitmap: Bitmap, tamaño: Int): Bitmap {
                return Bitmap.createScaledBitmap(bitmap, tamaño, tamaño, true)
            }

            private fun ajustarOrientacion(bitmap: Bitmap, uri: Uri): Bitmap {
                val inputStream = contentResolver.openInputStream(uri)
                val exif = ExifInterface(inputStream!!)
                val orientacion = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                val matriz = Matrix()

                when (orientacion) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> matriz.postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> matriz.postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> matriz.postRotate(270f)
                }

                inputStream.close()
                return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matriz, true)
            }

            private val galeriaLauncher =
                registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        val data: Intent? = result.data
                        val uri = data?.data
                        if (uri != null) {
                            val inputStream = contentResolver.openInputStream(uri)
                            val bitmap = BitmapFactory.decodeStream(inputStream)
                            val bitmapAjustado = ajustarOrientacion(bitmap, uri)
                            val bitmapRedimensionado = redimensionarBitmap(bitmapAjustado, 500) // Tamaño cuadrado
                            binding.imagen.setImageBitmap(bitmapRedimensionado)
                        }
                    }
                }

            private val camaraLauncher =
                registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                        val bitmapAjustado = ajustarOrientacion(bitmap, imageUri!!)
                        val bitmapRedimensionado = redimensionarBitmap(bitmapAjustado, 500) // Tamaño cuadrado
                        binding.imagen.setImageBitmap(bitmapRedimensionado)
                        Toast.makeText(this, "Imagen guardada en la galería", Toast.LENGTH_SHORT).show()
                    }
                }

            override fun onRequestPermissionsResult(
                requestCode: Int,
                permissions: Array<out String>,
                grantResults: IntArray
            ) {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    when (requestCode) {
                        PERMISO_GALERIA -> abrirGaleria()
                        PERMISO_CAMARA -> abrirCamara()
                    }
                } else {
                    Toast.makeText(this, "El permiso es necesario para continuar", Toast.LENGTH_SHORT).show()
                }
            }

            companion object {
                private const val PERMISO_GALERIA = 1
                private const val PERMISO_CAMARA = 2
            }
        }