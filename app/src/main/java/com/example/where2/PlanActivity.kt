package com.example.where2

import android.content.ContentValues.TAG
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import com.google.android.gms.common.api.Status
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import okhttp3.*
import java.io.IOException
import java.util.*
import okhttp3.OkHttpClient
import kotlin.collections.ArrayList


class PlanActivity : AppCompatActivity() {
    lateinit var startLatLng: LatLng
    lateinit var finishLatLng: LatLng

    private val client = OkHttpClient()

    lateinit var startingPlace: Place
    lateinit var finishingPlace: Place

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plan)

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.my_google_api_key), Locale.US)
        }

        // Initialize the AutocompleteSupportFragment.
        val autocompleteFragment =
            supportFragmentManager.findFragmentById(R.id.autocomplete_fragment)
                    as AutocompleteSupportFragment

        // Specify the types of place data to return.
        autocompleteFragment.setPlaceFields(listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG))

        // Set up a PlaceSelectionListener to handle the response.
        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onError(p0: Status) {
                null
            }

            override fun onPlaceSelected(place: Place) {
                // TODO: Get info about the selected place.
                Log.i(TAG, "Place: ${place.name}, ${place.id}")
                startLatLng = place.latLng

//                startingPlace
            }
        })

        // Initialize the AutocompleteSupportFragment.
        val autocompleteFragment2 =
            supportFragmentManager.findFragmentById(R.id.autocomplete_fragment2)
                    as AutocompleteSupportFragment

        // Specify the types of place data to return.
        autocompleteFragment2.setPlaceFields(listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG))

        // Set up a PlaceSelectionListener to handle the response.
        autocompleteFragment2.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onError(p0: Status) {
                null
            }

            override fun onPlaceSelected(place: Place) {
                // TODO: Get info about the selected place.
                Log.i(TAG, "Place: ${place.name}, ${place.id}")
                finishLatLng = place.latLng
                Log.i(TAG, "Place: ${makeApiCall(place)}")

            }
        })


        val spinner: Spinner = findViewById(R.id.spinner)
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter.createFromResource(
            this,
            R.array.hours_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            spinner.adapter = adapter
        }

        val showRoute: Button = findViewById(R.id.letsGo)
        showRoute.setOnClickListener {
            makeRoute()
        }
    }

    fun makeApiCall(place: Place) {
        val request = Request.Builder()
            .url("https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=" +
                    "${place.latLng.latitude},${place.latLng.longitude}&" +
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
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")

                    for ((name, value) in response.headers) {
                        println("$name: $value")
                    }

                    Log.i(TAG, response.body!!.string())
                }
            }
        })
    }

    private fun makeRoute(){
        val intent = Intent(this, RouteActivity::class.java)
        val args = Bundle()
        args.putParcelable("from_position", startLatLng)
        args.putParcelable("to_position", finishLatLng)
        intent.putExtra("bundle", args)
        startActivity(intent)
    }
}