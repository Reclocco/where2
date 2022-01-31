package com.example.where2

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.android.PolyUtil
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import okhttp3.OkHttpClient
import kotlin.math.pow
import kotlin.math.sqrt
import com.google.android.gms.maps.model.CameraPosition

import com.example.where2.JourneyInfo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase


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

                    val encodedPoints = (((JSONObject(responseString).get("routes") as JSONArray)[0] as JSONObject).get("overview_polyline") as JSONObject).get("points").toString()

                    var journeyPlaces = arrayListOf<String>()
                    journeyPlaces.add(fromPosition.latitude.toString())
                    journeyPlaces.add(fromPosition.longitude.toString())
                    journeyPlaces.add("Start")

                    for (place in placeList) {
                        journeyPlaces.add(place.lat)
                        journeyPlaces.add(place.lng)
                        journeyPlaces.add(place.name)
                    }

                    journeyPlaces.add(toPosition.latitude.toString())
                    journeyPlaces.add(toPosition.longitude.toString())
                    journeyPlaces.add("Finish")

                    val journey = JourneyInfo(encodedPoints, journeyPlaces.chunked(3))
                    val userID = FirebaseAuth.getInstance().currentUser?.uid
                    if (userID != null) {
                        Log.i("FIREBASE USER", userID)
                        val database: FirebaseDatabase = FirebaseDatabase.getInstance()
                        val databaseReference = database.reference

                        Log.i("ADDING JOURNEY TO FIREBASE", userID)
                        databaseReference.child("journeys").child(userID).push().setValue(journey)
                    }
                    else { Log.i("FIREBASE USER", "NONE") }



                    val decodedPoints = PolyUtil.decode(encodedPoints)
                    drawDirections(decodedPoints)
                    showPlacesPins(placeList)
                }
            }
        })
    }

    private fun drawDirections(decodedPoints: List<LatLng>) {
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

    private fun showPlacesPins(nicePlaces: List<PlaceCalculated>) {
        this@RouteActivity.runOnUiThread(Runnable {
            for (place in nicePlaces) {
                val nicePlace = place?.let { LatLng(place.lat.toDouble(), it.lng.toDouble()) }
                nicePlace?.let {
                    MarkerOptions()
                        .position(it)
                        .title(place.name)
                }?.let { mMap.addMarker(it) }
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

                    var placesToVisit = 0
                    var placeGeoOffsetDist = arrayListOf<PlaceCalculated>()
                    var finalPlaces = arrayListOf<PlaceCalculated>()

                    for (place in places) {
                        placeGeoOffsetDist.add(PlaceCalculated(place[2],
                            sqrt(((place[0].toDouble() - placeFinish.latitude) % 90).pow(2) + ((place[1].toDouble() - placeFinish.longitude) % 180).pow(2)) +
                                sqrt(((placeStart.latitude - place[0].toDouble()) % 90).pow(2) + ((placeStart.longitude - place[1].toDouble()) % 180).pow(2)),
                            place[0],
                            place[1]))
                    }

                    val sortedPlaces = placeGeoOffsetDist.sortedWith(compareBy({ it.dist }))

                    while (timeLimit - baseTime - placesToVisit*15*60 >= 0 &&  placesToVisit < sortedPlaces.size) {
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
                    getReasonablePlaces(fromPosition, toPosition, placesLocation.chunked(3))
                }
            }
        })
    }

    private fun enableMyLocation() {
        if (!::mMap.isInitialized) return
        // [START maps_check_location_permission]
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
        } else {
            // Permission to access the location is missing. Show rationale and request permission
            requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                Manifest.permission.ACCESS_FINE_LOCATION, true
            )
        }
    }
    
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

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

        getPlaces(centerLatLng)
        enableMyLocation()
    }

    fun requestPermission(
        activity: AppCompatActivity, requestId: Int,
        permission: String, finishActivity: Boolean
    ) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
            RationaleDialog.newInstance(requestId, finishActivity)
                .show(activity.supportFragmentManager, "dialog")
        } else {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(permission),
                requestId
            )
        }
    }

    class RationaleDialog : DialogFragment() {
        private var finishActivity = false
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val requestCode =
                arguments?.getInt(ARGUMENT_PERMISSION_REQUEST_CODE) ?: 0
            finishActivity =
                arguments?.getBoolean(ARGUMENT_FINISH_ACTIVITY) ?: false
            return AlertDialog.Builder(activity)
                .setMessage(R.string.permission_rationale_location)
                .setPositiveButton(android.R.string.ok) { dialog, which -> // After click on Ok, request the permission.
                    ActivityCompat.requestPermissions(
                        requireActivity(),
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        requestCode
                    )
                    // Do not finish the Activity while requesting permission.
                    finishActivity = false
                }
                .setNegativeButton(android.R.string.cancel, null)
                .create()
        }

        override fun onDismiss(dialog: DialogInterface) {
            super.onDismiss(dialog)
            if (finishActivity) {
                Toast.makeText(
                    activity,
                    R.string.permission_required_toast,
                    Toast.LENGTH_SHORT
                ).show()
                activity?.finish()
            }
        }

        companion object {
            private const val ARGUMENT_PERMISSION_REQUEST_CODE = "requestCode"
            private const val ARGUMENT_FINISH_ACTIVITY = "finish"

            fun newInstance(requestCode: Int, finishActivity: Boolean): RationaleDialog {
                val arguments = Bundle().apply {
                    putInt(ARGUMENT_PERMISSION_REQUEST_CODE, requestCode)
                    putBoolean(ARGUMENT_FINISH_ACTIVITY, finishActivity)
                }
                return RationaleDialog().apply {
                    this.arguments = arguments
                }
            }
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}


