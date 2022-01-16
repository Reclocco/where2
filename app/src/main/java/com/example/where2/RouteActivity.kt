package com.example.where2

import android.content.ContentValues
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import okhttp3.OkHttpClient
import kotlin.math.pow
import kotlin.math.sqrt


class RouteActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    private lateinit var bundle:Bundle
    private lateinit var fromPosition: LatLng
    private lateinit var toPosition: LatLng
    private lateinit var centerLatLng: LatLng
    private var timeLimit: Int = 0

    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        timeLimit = intent.getIntExtra("time", 1)
        bundle = intent.getParcelableExtra<Bundle>("bundle")!!
        fromPosition = bundle.getParcelable("from_position")!!
        toPosition = bundle.getParcelable("to_position")!!
        centerLatLng = LatLng((fromPosition.latitude+toPosition.latitude) /2 % 90,
            (fromPosition.longitude+toPosition.longitude) / 2 % 180)

        getPlaces(centerLatLng)
    }

    fun makeLogs(placeList: ArrayList<String>) {
        Log.i("PLACES: ", placeList.toString())
    }

    private fun getDirections(placeList: ArrayList<String>) {
        val request = Request.Builder()
            .url("https://maps.googleapis.com/maps/api/directions/json?" +
                    "origin=Chicago%2C%20IL&" +
                    "destination=Los%20Angeles%2C%20CA&" +
                    "waypoints=Joplin%2C%20MO%7COklahoma%20City%2C%20OK&" +
                    "AIzaSyDJwo248qnVnalWoobX8rPkxX0C-MGht_4")

                    .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) throw IOException("Unexpected code $it")

                    for ((name, value) in it.headers) {
                        Log.i("HEADERS: ", "$name: $value")
                    }

                    val responseString = it.body!!.string()
                    Log.i("DIRECTIONS RESPONSE", responseString)
                }
            }
        })
    }

    private fun getReasonablePlaces(
        placeStart: LatLng,
        placeFinish: LatLng,
        places: List<List<String>>
    ) {
        val request = Request.Builder()
            .url("https://maps.googleapis.com/maps/api/distancematrix/json?departure_time=now&" +
                    "origins=${placeStart.latitude}%2C${placeStart.longitude}&" +
                    "destinations=${placeFinish.latitude}%2C${placeFinish.longitude}&" +
                    "mode=walking&" +
                    "key=AIzaSyDJwo248qnVnalWoobX8rPkxX0C-MGht_4")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) throw IOException("Unexpected code $it")

                    val responseString = it.body!!.string()
                    val baseTime = (((((JSONObject(responseString).get("rows") as JSONArray)[0] as JSONObject).get("elements") as JSONArray)[0] as JSONObject).get("duration") as JSONObject).get("value").toString().toInt()
                    Log.i("MATRIX RESPONSE", responseString)
                    Log.i("BASE TRIP TIME", baseTime.toString())
                    Log.i("MAX TRIP TIME", timeLimit.toString())

                    var placesToVisit = 0
                    val absGeoDist = sqrt(((placeStart.latitude - placeFinish.latitude) % 90).pow(2) + ((placeStart.longitude - placeFinish.longitude) % 90).pow(2))
                    var placeGeoOffsetDist = 0.0

                    while (timeLimit - baseTime - placesToVisit*15*60 >= 0) {
                        while (true){
                            for (place in places) {
                                placeGeoOffsetDist = sqrt(((place[0].toInt() - placeFinish.latitude) % 90).pow(2) + ((place[1].toInt() - placeFinish.longitude) % 180).pow(2)) +
                                        sqrt(((placeStart.latitude - place[0].toInt()) % 90).pow(2) + ((placeStart.longitude - place[1].toInt()) % 180).pow(2))
                            }
                        }
                    }
                }
            }
        })
    }


    private fun getPlaces(place: LatLng) {
        lateinit var jsonArray: JSONArray
        val request = Request.Builder()
            .url("https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                    "location=${place.latitude},${place.longitude}&" +
                    "radius=1500&" +
                    "type=tourist_attraction&" +
                    "key=AIzaSyDJwo248qnVnalWoobX8rPkxX0C-MGht_4")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) throw IOException("Unexpected code $it")

                    val responseString = it.body!!.string()
                    jsonArray = JSONObject(responseString).get("results") as JSONArray

                    Log.i(ContentValues.TAG, responseString)

                    val places = arrayListOf<String>()

                    for(i in 0 until jsonArray.length()) {
                        if(!((jsonArray[i] as JSONObject).get("types")
                                    as JSONArray).toString().contains("amusement_park")) {
                            places.add((jsonArray[i] as JSONObject).get("name").toString())
                        }
                    }

                    val placesLocation = arrayListOf<String>()

                    for(i in 0 until jsonArray.length()) {
                        if(!((jsonArray[i] as JSONObject).get("types")
                                    as JSONArray).toString().contains("amusement_park")) {
                            placesLocation.add((((jsonArray[i] as JSONObject).get("geometry")
                                    as JSONObject).get("location") as JSONObject).get("lat").toString()
                            )
                            placesLocation.add((((jsonArray[i] as JSONObject).get("geometry")
                                    as JSONObject).get("location") as JSONObject).get("lng").toString()
                            )
                        }
                    }

                    makeLogs(places)
//                    getDirections(places)
//                    getOptimalPlaces(placesLocation.chunked(2))
                    getReasonablePlaces(fromPosition, toPosition, placesLocation.chunked(2))
                }
            }
        })
    }
    
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Add a marker in Sydney and move the camera
        val sydney = LatLng(-34.0, 151.0)
        mMap.addMarker(MarkerOptions()
            .position(sydney)
            .title("Marker in Sydney"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))

        // Add a start maker and move the camera
        val startPoint = fromPosition?.let { LatLng(it.latitude, fromPosition.longitude) }
        startPoint?.let {
            MarkerOptions()
                .position(it)
                .title("Marker start")
        }?.let { mMap.addMarker(it) }
        startPoint?.let { CameraUpdateFactory.newLatLng(it) }?.let { mMap.moveCamera(it) }

        // Add a finish marker and move the camera
        val finishPoint = toPosition?.let { LatLng(toPosition.latitude, it.longitude) }
        finishPoint?.let {
            MarkerOptions()
                .position(it)
                .title("Marker finish")
        }?.let { mMap.addMarker(it) }
        finishPoint?.let { CameraUpdateFactory.newLatLng(it) }?.let { mMap.moveCamera(it) }

        // Add a finish marker and move the camera
        val middlePoint = centerLatLng?.let { LatLng(centerLatLng.latitude, it.longitude) }
        middlePoint?.let {
            MarkerOptions()
                .position(it)
                .title("Marker middle")
        }?.let { mMap.addMarker(it) }
        middlePoint?.let { CameraUpdateFactory.newLatLng(it) }?.let { mMap.moveCamera(it) }
    }
}


