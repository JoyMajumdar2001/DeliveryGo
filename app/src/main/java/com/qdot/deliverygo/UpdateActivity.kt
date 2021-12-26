package com.qdot.deliverygo

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import com.qdot.deliverygo.adapters.TrackAdapter
import com.qdot.deliverygo.databinding.ActivityUpdateBinding
import com.qdot.deliverygo.models.ShipmentType
import com.qdot.deliverygo.models.TrackInfo
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class UpdateActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUpdateBinding
    private lateinit var rvAdapter: TrackAdapter
    private var trackRecord : MutableList<TrackInfo> = ArrayList()
    private lateinit var trackId : String
    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpdateBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val objData = JSONObject(intent.getStringExtra("tdata").toString()).getJSONObject("document")
        trackId = objData.getString("Trackid")
        binding.trackIdNo.text = trackId
        binding.recName.text = objData.getString("Recname")
        binding.deliveryAdd.text = objData.getString("Destloc")

        if (intent.getBooleanExtra("partner",false)){
            binding.updateBtn.visibility = View.VISIBLE
        }else{
            binding.updateBtn.visibility = View.GONE
        }

        val layoutManager: RecyclerView.LayoutManager =
            LinearLayoutManager(this@UpdateActivity)

        binding.updateRecyclerView.layoutManager = layoutManager

        CoroutineScope(Dispatchers.IO).launch {
            val client = OkHttpClient()
            val mediaType = "application/json".toMediaType()
            val bodyReq = "{\n" +
                    "      \"dataSource\": \"ClusterDrop\",\n" +
                    "      \"database\": \"delupdates\",\n" +
                    "      \"collection\":\"" + trackId + "\"}"

            val request = Request.Builder()
                .url("https://data.mongodb-api.com/app/data-oageq/endpoint/data/beta/action/find")
                .header("Content-Type", "application/json")
                .header("api-key", getString(R.string.apiKey))
                .post(bodyReq.toRequestBody(mediaType))
                .build()

            runCatching {
                val response = client.newCall(request).execute()
                val resStr = response.body!!.string()
                val obj  = JSONObject(resStr)
                val listDoc = obj.getJSONArray("documents")
                for (i in 0 until listDoc.length()){
                    trackRecord.add(Gson().fromJson(listDoc[i].toString(),TrackInfo::class.java))
                }

                withContext(Dispatchers.Main){
                    rvAdapter = TrackAdapter(trackRecord)
                    binding.updateRecyclerView.adapter = rvAdapter
                }
            }

        }

        binding.updateBtn.setOnClickListener {
            addAnTrack()
        }

    }

    @SuppressLint("InflateParams")
    private fun addAnTrack() {
        val dialog = BottomSheetDialog(this)

        var secType = ShipmentType.Arrived.ordinal

        val view = layoutInflater.inflate(R.layout.add_update_layout, null)

        val editTextLoc = view.findViewById<TextInputEditText>(R.id.hubLocation)

        val chipGroup = view.findViewById<ChipGroup>(R.id.chipsGroup)

        val submitBtn = view.findViewById<MaterialButton>(R.id.submitBtn)

        chipGroup.setOnCheckedChangeListener { _, checkedId ->
            when(checkedId){
                R.id.c1 -> secType = ShipmentType.Shipment_Created.ordinal
                R.id.c2 -> secType = ShipmentType.Arrived.ordinal
                R.id.c3 -> secType = ShipmentType.Left.ordinal
                R.id.c4 -> secType = ShipmentType.Out_For_Delivery.ordinal
                R.id.c5 -> secType = ShipmentType.Delivered.ordinal
            }
        }

        submitBtn.setOnClickListener {

            val trackInfoObj = TrackInfo(secType,
                editTextLoc.text.toString(),
                System.currentTimeMillis())

            CoroutineScope(Dispatchers.IO).launch {
                val client = OkHttpClient()
                val mediaType = "application/json".toMediaType()

                val bodyReq = "{\n" +
                        "      \"dataSource\": \"ClusterDrop\",\n" +
                        "      \"database\": \"delupdates\",\n" +
                        "      \"collection\": \"" + trackId + "\"," +
                        "\"document\":"+ Gson().toJson(trackInfoObj).toString() + "}"

                val request = Request.Builder()
                    .url("https://data.mongodb-api.com/app/data-oageq/endpoint/data/beta/action/insertOne")
                    .header("Content-Type", "application/json")
                    .header("api-key", getString(R.string.apiKey))
                    .post(bodyReq.toRequestBody(mediaType))
                    .build()

                runCatching {
                    val updateResponse = client.newCall(request).execute()
                    val resStr = updateResponse.body!!.string()
                    val resUp = JSONObject(resStr)

                    if (secType == ShipmentType.Delivered.ordinal){
                        if (resUp.isNull("insertedId")){
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@UpdateActivity, "Can not update!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }else{
                            val client1 = OkHttpClient()
                            val mediaType1 = "application/json".toMediaType()

                            val bodyReq1 = "{\n" +
                                    "      \"dataSource\": \"ClusterDrop\",\n" +
                                    "      \"database\": \"deldata\",\n" +
                                    "      \"collection\": \"parcels\",\n" +
                                    "      \"filter\": { \"Trackid\": \"" + trackId +
                                    "\" },\n" +
                                    "      \"update\": {\n" +
                                    "          \"\$set\": {\n" +
                                    "              \"Delivered\": true\n" +
                                    "          }\n" +
                                    "      }\n" +
                                    "  }"

                            val request1 = Request.Builder()
                                .url("https://data.mongodb-api.com/app/data-oageq/endpoint/data/beta/action/updateOne")
                                .header("Content-Type", "application/json")
                                .header("api-key", getString(R.string.apiKey))
                                .post(bodyReq1.toRequestBody(mediaType1))
                                .build()

                            val updateResponse1 = client1.newCall(request1).execute()
                            val resStr1 = updateResponse1.body!!.string()
                            val objIt = JSONObject(resStr1)
                            withContext(Dispatchers.Main){
                                if (objIt.isNull("modifiedCount")){
                                    Toast.makeText(
                                        this@UpdateActivity, "Can not update!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }else{
                                    Toast.makeText(
                                        this@UpdateActivity, "Updated successfully!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }

                        }
                    }else {
                        if (resUp.isNull("insertedId")){
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@UpdateActivity, "Can not update!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }else{
                            Toast.makeText(
                                this@UpdateActivity, "Updated successfully!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }

        dialog.setContentView(view)
        dialog.show()
    }
}