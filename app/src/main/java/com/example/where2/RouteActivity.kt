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
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.libraries.places.api.model.Place
import com.google.maps.android.PolyUtil
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import okhttp3.OkHttpClient
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.properties.Delegates
import com.google.android.gms.maps.model.CameraPosition


class PlaceCalculated(n: String, d: Double, c1: String, c2: String) {
    var name: String = n
    var dist: Double = d
    var lat: String = c1
    var lng: String = c2
}


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
    }

    fun makeLogs(placeList: ArrayList<String>) {
        Log.i("PLACES: ", placeList.toString())
    }

    private fun getDirections(placeStart: LatLng,
                              placeFinish: LatLng,
                              placeList: ArrayList<PlaceCalculated>) {
        var waypoints = ""
        for (place in placeList) {
            waypoints += "|"
            waypoints += place.lat
            waypoints += "%2C"
            waypoints += place.lng
        }

        val request: Request
        if (waypoints != "") {
            request = Request.Builder()
                .url("https://maps.googleapis.com/maps/api/directions/json?" +
                        "origin=${placeStart.latitude}%2C${placeStart.longitude}&" +
                        "destination=${placeFinish.latitude}%2C${placeFinish.longitude}&" +
                        "waypoints=optimize:true${waypoints}&" +
                        "mode=walking&" +
                        "key=AIzaSyDJwo248qnVnalWoobX8rPkxX0C-MGht_4")
                .build()
        } else {
            request = Request.Builder()
                .url("https://maps.googleapis.com/maps/api/directions/json?" +
                        "origin=${placeStart.latitude}%2C${placeStart.longitude}&" +
                        "destination=${placeFinish.latitude}%2C${placeFinish.longitude}&" +
                        "mode=walking&" +
                        "key=AIzaSyDJwo248qnVnalWoobX8rPkxX0C-MGht_4")
                .build()
        }


        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) throw IOException("Unexpected code $it")

                    val responseString = it.body!!.string()
                    Log.i("DIRECTIONS RESPONSE", responseString)

                    val encodedPoints = (((JSONObject(responseString).get("routes") as JSONArray)[0] as JSONObject).get("overview_polyline") as JSONObject).get("points").toString()
                    Log.i("DIRECTIONS ENCODED POINTS", encodedPoints)

                    val decodedPoints = PolyUtil.decode(encodedPoints)
                    drawPolyline(decodedPoints)
                }
            }
        })
    }

    private fun drawPolyline(decodedPoints: List<LatLng>) {
        this@RouteActivity.runOnUiThread(Runnable {
            mMap.addPolyline(PolylineOptions().addAll(decodedPoints))
            mMap.animateCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder().target(fromPosition)
                        .zoom(13f).build()
                )
            )
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
                    var placeGeoOffsetDist = arrayListOf<PlaceCalculated>()
                    var finalPlaces = arrayListOf<PlaceCalculated>()

                    for (place in places) {
                        Log.i("PLACES SORTED NAME SHASHUMGA", place.toString())

                        placeGeoOffsetDist.add(PlaceCalculated(place[2],
                            sqrt(((place[0].toDouble() - placeFinish.latitude) % 90).pow(2) + ((place[1].toDouble() - placeFinish.longitude) % 180).pow(2)) +
                                sqrt(((placeStart.latitude - place[0].toDouble()) % 90).pow(2) + ((placeStart.longitude - place[1].toDouble()) % 180).pow(2)),
                            place[0],
                            place[1]))
                    }

                    val sortedPlaces = placeGeoOffsetDist.sortedWith(compareBy({ it.dist }))
                    for (i in sortedPlaces.indices) {
                        Log.i("PLACES SORTED NAME", sortedPlaces[i].name)
                        Log.i("PLACES SORTED DIST", sortedPlaces[i].dist.toString())
                    }

                    while (timeLimit - baseTime - placesToVisit*15*60 >= 0) {
                        finalPlaces.add(sortedPlaces[placesToVisit])
                        placesToVisit += 1
                    }

                    for (i in 0 until finalPlaces.size) {
                        Log.i("PLACES FINAL NAME", finalPlaces[i].name)
                    }

                    getDirections(placeStart, placeFinish, finalPlaces)
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

                            placesLocation.add((jsonArray[i] as JSONObject).get("name").toString())
                        }
                    }

                    makeLogs(places)
//                    getDirections(places)
//                    getOptimalPlaces(placesLocation.chunked(2))
                    Log.i("PLACES SORTED BABABOEY", placesLocation.chunked(3).toString())
                    getReasonablePlaces(fromPosition, toPosition, placesLocation.chunked(3))
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

        getPlaces(centerLatLng)
    }
}


