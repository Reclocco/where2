package com.example.where2

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.where2.databinding.ActivityHistoryBinding
import com.example.where2.databinding.ActivityWelcomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class HistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHistoryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val database: FirebaseDatabase = FirebaseDatabase.getInstance()
        val databaseReference = database.reference
        val userID = FirebaseAuth.getInstance().currentUser?.uid

        val datasetUnparsed = arrayListOf<JourneyInfo>()
        var places = arrayListOf<String>()

        if (!userID.isNullOrBlank()) {
            databaseReference.child("journeys").child(userID).get().addOnSuccessListener {
                it.children.forEach { journey ->
                    places = arrayListOf<String>()

                    journey.child("places").children.forEach { place ->
                        place.children.forEach { parameter ->
                            places.add(parameter.value.toString())
                        }
                    }

                    val polyline = journey.child("polyline").value.toString()
                    Log.i("POLYLINE INFO: ", polyline)

                    datasetUnparsed.add(JourneyInfo(polyline, places.chunked(3)))
                }
                Log.i("PLACE COMPLETE INFO: ", places.toString())
                val adapter = HistoryAdapter(datasetUnparsed)
                binding.spinner.adapter = adapter
                binding.spinner.layoutManager = LinearLayoutManager(this);
                Log.i("DATASET LEN:", adapter.itemCount.toString())
            }
        }
    }
}