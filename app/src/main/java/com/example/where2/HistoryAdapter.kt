package com.example.where2

import android.content.Intent
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.Serializable


class HistoryAdapter(private val dataSet: ArrayList<JourneyInfo>) :
    RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var origin: TextView = view.findViewById(R.id.origin)
        var destination: TextView = view.findViewById(R.id.destination)
        var stops = emptyList<List<String>>()
        var polyline: String = ""

        init {
            itemView.setOnClickListener {
                val intent = Intent(it.context, HistoricRouteActivity::class.java)
                intent.putExtra("stops", stops as Serializable)
                intent.putExtra("polyline", polyline)
                it.context.startActivity(intent)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.past_journey, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.origin.text = dataSet[position].places?.get(0)?.get(0) + ", " + dataSet[position].places?.get(1)?.get(0)?: "None"
        holder.destination.text = dataSet[position].places?.size?.let {
            dataSet[position].places?.get(it-1)?.get(0) + ", " + dataSet[position].places?.get(it-1)?.get(1)
        }
            ?: "None"
        holder.stops = dataSet[position].places!!
        holder.polyline = dataSet[position].polyline!!
    }

    override fun getItemCount() = dataSet.size

}