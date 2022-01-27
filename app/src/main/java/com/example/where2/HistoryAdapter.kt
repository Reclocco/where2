package com.example.where2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HistoryAdapter(private val dataSet: Array<JourneyInfo>) :
    RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var origin: TextView = view.findViewById(R.id.origin)
        var destination: TextView = view.findViewById(R.id.destination)
        var stops = emptyList<List<String>>()
        var polyline: String = ""
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.past_journey, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.origin.text = dataSet[position].places?.get(0)?.get(2) ?: "None"
        holder.destination.text = dataSet[position].places?.size?.let {
            dataSet[position].places?.get(
                it-1
            )?.get(2)
        }
            ?: "None"
        holder.stops = dataSet[position].places!!
        holder.polyline = dataSet[position].polyline!!
    }

    override fun getItemCount() = dataSet.size

}