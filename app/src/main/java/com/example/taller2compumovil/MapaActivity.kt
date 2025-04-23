package com.example.taller2compumovil

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.taller2compumovil.databinding.ActivityMapaBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import org.osmdroid.api.IMapController
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.events.MapEventsReceiver
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.views.overlay.TilesOverlay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MapaActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMapaBinding
    private var marcadorPresionado: Marker? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var puntoInicio: GeoPoint? = null
    private lateinit var roadManager: RoadManager
    private var roadOverlay: Polyline? = null
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private var marcadorActual: Marker? = null
    private var ultimaUbicacion: android.location.Location? = null
    private val jsonFileName = "ubicaciones.json"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //lo q sale en la pres
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        // Verificar y solicitar permisos
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            iniciarMapa()
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION]) //no se sin esto no compila xdd
    private fun iniciarMapa() {

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        Configuration.getInstance().userAgentValue = applicationContext.packageName

        binding = ActivityMapaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        roadManager = OSRMRoadManager(this, "ANDROID")

        binding.osmMap.setTileSource(TileSourceFactory.MAPNIK)
        binding.osmMap.setMultiTouchControls(true)

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                puntoInicio = GeoPoint(location.latitude, location.longitude)

                // Configurar el mapa con la ubicación actual
                val mapControlador: IMapController = binding.osmMap.controller
                mapControlador.setZoom(18.0)
                mapControlador.setCenter(puntoInicio)
            } else {
                Toast.makeText(this, "No se pudo conseguir la ubicación", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Error con ubicación", Toast.LENGTH_SHORT).show()
        } //la vdd esto me lo dijo chat pq no sabia


        // Configuración del cuadro de texto
        binding.ubicacion.setOnEditorActionListener { _, _, _ ->
            val direccion = binding.ubicacion.text.toString()
            if (direccion.isNotEmpty()) {
                val mapControlador: IMapController = binding.osmMap.controller
                buscarDireccion(direccion, mapControlador)
            } else {
                Toast.makeText(this, "Ingresa una dirección", Toast.LENGTH_SHORT).show()
            }
            true
        }

        // Agregar eventos al mapa
        binding.osmMap.overlays.add(createOverlayEvents())

        // Iniciar actualizaciones de ubicación
        actualizarUbi()
    }


    @SuppressLint("ServiceCast")
    override fun onResume() { //honestly esto me lo cambio chat pq el context no me servia:(
        super.onResume()
        binding.osmMap.onResume()

        // Configurar el mapa
        val mapController: IMapController = binding.osmMap.controller
        mapController.setZoom(18.0)
        puntoInicio?.let { mapController.setCenter(it) }

        // Detectar el nivel de luz ambiental
        val sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

            val lightSensorListener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    val lightLevel = event?.values?.get(0) ?: return
                    if (lightLevel < 10) { // Umbral para luz baja
                        binding.osmMap.overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)
                    } else {
                        binding.osmMap.overlayManager.tilesOverlay.setColorFilter(null)
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            sensorManager.registerListener(lightSensorListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)

    }


    private fun calcularRuta(destino: GeoPoint) {
        if (puntoInicio == null) {
            Toast.makeText(this, "Ubicación actual no disponible", Toast.LENGTH_SHORT).show()
            return
        }

        // Crear una lista de puntos para la ruta
        val puntos = arrayListOf(puntoInicio, destino)

        // Calcular la ruta
        val caminos: Road = roadManager.getRoad(puntos)

        // Verificar si la ruta es válida
        if (caminos.mStatus != Road.STATUS_OK) {
            Toast.makeText(this, "Error al calcular la ruta", Toast.LENGTH_SHORT).show()
            return
        }

        roadOverlay?.let { binding.osmMap.overlays.remove(it) }
        roadOverlay = RoadManager.buildRoadOverlay(caminos)
        roadOverlay?.outlinePaint?.color = Color.BLUE
        roadOverlay?.outlinePaint?.strokeWidth = 10f
        binding.osmMap.overlays.add(roadOverlay)
        binding.osmMap.invalidate()
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION]) //no se sin esto no compila xdd
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                iniciarMapa()
            } else {
                Toast.makeText(this, "Permiso denegado >:(", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun buscarDireccion(direccion: String, mapControlador: IMapController) {
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val resultados = geocoder.getFromLocationName(direccion, 1) // no se como se usa el deprecated
            if (!resultados.isNullOrEmpty()) {
                val ubicacion = resultados[0]
                val punto = GeoPoint(ubicacion.latitude, ubicacion.longitude)

                mapControlador.setCenter(punto)
                mapControlador.setZoom(18.0)

                marcadorActual?.let { binding.osmMap.overlays.remove(it) } //si ya hay uno pos lo borra
                marcadorPresionado?.let { binding.osmMap.overlays.remove(it) }

                marcadorActual = crearMarcador(punto, direccion, null, R.drawable.pinsito)
                marcadorActual?.let { binding.osmMap.overlays.add(it) }
                binding.osmMap.invalidate()

                // Calcular y mostrar la ruta
                calcularRuta(punto)
            } else {
                Toast.makeText(this, "Dirección no encontrada :((", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show()
        }
    }


    private fun createOverlayEvents(): MapEventsOverlay { //copiado de la pres nejejej
        val overlayEventos = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                return false
            }

            override fun longPressHelper(p: GeoPoint): Boolean {
                longPressOnMap(p)
                return true
            }
        })
        return overlayEventos
    }



    private fun longPressOnMap(p: GeoPoint) {

        // Eliminar marcador anterior si existe
        marcadorPresionado?.let { binding.osmMap.overlays.remove(it) }
        marcadorActual?.let { binding.osmMap.overlays.remove(it) }

        // Obtener dirección con Geocoder
        val geocoder = Geocoder(this, Locale.getDefault())
        val direccion = try {
            val resultados = geocoder.getFromLocation(p.latitude, p.longitude, 1)
            if (!resultados.isNullOrEmpty()) {
                resultados[0].getAddressLine(0) // da la dirección completa
            } else {
                "Dirección no encontrada"
            }
        } catch (e: Exception) {
            "Error al obtener dirección"
        }

        marcadorPresionado = crearMarcador(p, direccion, null, R.drawable.pinsito)
        marcadorPresionado?.let { binding.osmMap.overlays.add(it) }
        binding.osmMap.invalidate()


        val posicionActual = puntoInicio ?: GeoPoint(0.0, 0.0)
        val locationActual = android.location.Location("").apply {
            latitude = posicionActual.latitude
            longitude = posicionActual.longitude
        }
        val locationMarcador = android.location.Location("").apply {
            latitude = p.latitude
            longitude = p.longitude
        }
        val distancia = locationActual.distanceTo(locationMarcador) // Distancia en metros

        // Mostrar un Toast con la distancia
        Toast.makeText(this, "Distancia aproximada: ${distancia.toInt()} metros", Toast.LENGTH_SHORT).show()
        calcularRuta(p)
    }

    private fun crearMarcador(p: GeoPoint, title: String?, descr: String?, iconID: Int): Marker {
        val marcador = Marker(binding.osmMap)
        marcador.position = p
        marcador.title = title ?: "ns q titulo le pongo xdd"
        descr?.let { marcador.subDescription = it }
        val myIcon = resources.getDrawable(iconID, theme)
        marcador.icon = myIcon
        marcador.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        return marcador
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun actualizarUbi()
    {
        val locationRequest = LocationRequest.create().apply {
            interval = 10000 // 10s
            fastestInterval = 5000 // 5s
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val nuevaUbicacion = locationResult.lastLocation ?: return

                // Calcular la distancia desde la última ubicación
                if (ultimaUbicacion != null)
                {

                    val distancia = ultimaUbicacion!!.distanceTo(nuevaUbicacion)
                    if (distancia > 30)
                    {
                        registrarUbi(nuevaUbicacion)
                        actualizarMarcadorEnMapa(nuevaUbicacion)
                    }
                }

                ultimaUbicacion = nuevaUbicacion
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    private fun registrarUbi(ubicacion: android.location.Location) {
        val jsonFile = File(filesDir, jsonFileName)
        val jsonArray = if (jsonFile.exists()) {
            JSONArray(jsonFile.readText())
        } else {
            JSONArray()
        }

        val jsonObject = JSONObject().apply {
            put("latitud", ubicacion.latitude)
            put("longitud", ubicacion.longitude)
            put("fechaHora", SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(Date()))
        }

        jsonArray.put(jsonObject)
        jsonFile.writeText(jsonArray.toString())
    } // la vdd no se como revisar q esto funcione pero creo q si esta bien xdd

    private fun actualizarMarcadorEnMapa(ubicacion: android.location.Location) {
        val nuevaPosicion = GeoPoint(ubicacion.latitude, ubicacion.longitude)

        // Actualizar el marcador en el mapa
        marcadorActual?.let { binding.osmMap.overlays.remove(it) }
        marcadorActual = crearMarcador(nuevaPosicion, "Última posición", null, R.drawable.pinsito)
        marcadorActual?.let { binding.osmMap.overlays.add(it) }

        // Mover la cámara a la nueva posición
        val mapControlador: IMapController = binding.osmMap.controller
        mapControlador.setCenter(nuevaPosicion)
        mapControlador.setZoom(18.0)

        binding.osmMap.invalidate()
    }
}