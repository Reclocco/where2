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


class HistoricRouteActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    private lateinit var stops: List<List<String>>
    private lateinit var polyline: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        stops = intent.getSerializableExtra("stops") as List<List<String>>
        polyline = intent.getStringExtra("polyline").toString()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        stops.forEach { place ->
            val point = place?.let { LatLng(place[0].toDouble(), it[1].toDouble()) }
            point?.let {
                MarkerOptions()
                    .position(it)
                    .title(place[2])
            }?.let { mMap.addMarker(it) }
        }

        val destination = LatLng(stops[stops.size-1][0].toDouble(), stops[stops.size-1][1].toDouble())

        mMap.addPolyline(PolylineOptions().addAll(PolyUtil.decode(polyline)))
        mMap.animateCamera(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder().target(destination)
                    .zoom(13f).build()
            )
        )
    }
}


