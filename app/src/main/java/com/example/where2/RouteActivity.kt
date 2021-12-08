
// Copyright 2020 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

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




class RouteActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    private lateinit var bundle:Bundle
    private lateinit var fromPosition: LatLng
    private lateinit var toPosition: LatLng
    private lateinit var centerLatLng: LatLng

    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        bundle = intent.getParcelableExtra<Bundle>("bundle")!!
        fromPosition = bundle.getParcelable("from_position")!!
        toPosition = bundle.getParcelable("to_position")!!
        centerLatLng = LatLng((fromPosition.latitude+toPosition.latitude) /2 % 90,
            (fromPosition.longitude+toPosition.longitude) / 2 % 180)

        getPlaces(centerLatLng)
    }

    fun makeLogs(placeList: ArrayList<String>) {
        Log.i("WILL IT EVER WORK?? ", placeList.toString())
    }

    private fun getDirections(placeList: ArrayList<String>) {
        val request = Request.Builder()
            .url("https://maps.googleapis.com/maps/api/directions/json?" +
                    "origin=Chicago%2C%20IL&" +
                    "destination=Los%20Angeles%2C%20CA&" +
                    "waypoints=Joplin%2C%20MO%7COklahoma%20City%2C%20OK&" +
                    "key=AIzaSyDJwo248qnVnalWoobX8rPkxX0C-MGht_4")
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

    private fun getPlaces(place: LatLng) {
        lateinit var jsonObject: JSONArray
        val request = Request.Builder()
            .url("https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=" +
                    "${place.latitude},${place.longitude}&" +
                    "radius=1500&" +
                    "type=restaurant&" +
                    "key=AIzaSyDJwo248qnVnalWoobX8rPkxX0C-MGht_4")
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
                    jsonObject = JSONObject(responseString).get("results") as JSONArray

                    Log.i(ContentValues.TAG, responseString)
                    Log.i("SOMETHING???", (jsonObject[0] as JSONObject).get("name").toString())

                    val places = arrayListOf<String>()

                    for(i in 0 until jsonObject.length()) {
                        places.add((jsonObject[i] as JSONObject).get("name").toString())
                    }

                    Log.i("EVEN MORE???", places.toString())
                    makeLogs(places)
                    getDirections(places)
                }
            }
        })
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
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


