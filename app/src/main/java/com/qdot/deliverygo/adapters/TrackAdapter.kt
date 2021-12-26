package com.qdot.deliverygo.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.qdot.deliverygo.databinding.TrackLayoutBinding
import com.qdot.deliverygo.models.ShipmentType
import com.qdot.deliverygo.models.TrackInfo
import java.text.SimpleDateFormat
import java.util.*

class TrackAdapter(private var dataList : MutableList<TrackInfo>) :
    RecyclerView.Adapter<TrackAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: TrackLayoutBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = TrackLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        return ViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder){
            with(dataList[position]){
                when(this.Dtype){
                    ShipmentType.Shipment_Created.ordinal -> binding.statementText.text = "Shipment has been created at " + this.Nloc
                    ShipmentType.Arrived.ordinal -> binding.statementText.text = "Shipment has arrived at " + this.Nloc
                    ShipmentType.Left.ordinal -> binding.statementText.text = "Shipment has left " + this.Nloc
                    ShipmentType.Out_For_Delivery.ordinal -> binding.statementText.text = "Shipment has out for delivery at " + this.Nloc
                    ShipmentType.Delivered.ordinal -> binding.statementText.text = "Shipment has been delivered at " + this.Nloc
                }
                binding.timeText.text = getDate(this.Time)
            }
        }
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    @SuppressLint("SimpleDateFormat")
    private fun getDate(milliSeconds : Long):String {
        val dateFormat = SimpleDateFormat("dd MMMM yyyy hh:mm a")
        val cal = Calendar.getInstance()
        cal.timeInMillis = milliSeconds
        return dateFormat.format(cal.time)
    }

}