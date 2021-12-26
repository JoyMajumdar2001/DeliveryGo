package com.qdot.deliverygo

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Gravity
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.gson.Gson
import com.qdot.deliverygo.databinding.ActivityCreateBinding
import com.qdot.deliverygo.models.OrderDetails
import com.qdot.deliverygo.models.ShipmentType
import com.qdot.deliverygo.models.TrackInfo
import com.thecode.aestheticdialogs.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class CreateActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCreateBinding
    private var secDate : Long = 0
    private lateinit var secTime : String
    private val trackId = randomID()
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        val partner = JSONObject(intent.getStringExtra("data2").toString())

        binding.indicatorCirclePro.visibility = View.GONE
        binding.trackIdNumber.setText(trackId)
        binding.partnerName.setText(partner.getJSONObject("document").getString("name"))

        val datePicker =
            MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()

        datePicker.addOnPositiveButtonClickListener {
            secDate = datePicker.selection!!
            binding.shipmentDate.setText(getDate(datePicker.selection!!))
        }

        val timePicker =
            MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(12)
                .setMinute(10)
                .setTitleText("Select Time")
                .build()

        timePicker.addOnPositiveButtonClickListener {
            secTime = timePicker.hour.toString() + ":" + timePicker.minute.toString()
            binding.shipmentTime.setText(timePicker.hour.toString() + ":" + timePicker.minute.toString())
        }

        binding.shipmentDateLayout.setEndIconOnClickListener {
            datePicker.show(supportFragmentManager,"Date")
        }

        binding.shipmentTimeLayout.setEndIconOnClickListener {
            timePicker.show(supportFragmentManager,"Time")
        }

        binding.registerButton.setOnClickListener {

            val cal = Calendar.getInstance()
            cal.timeInMillis = secDate
            cal.set(Calendar.HOUR_OF_DAY,timePicker.hour)
            cal.set(Calendar.MINUTE, timePicker.minute)

            val orderDetail = OrderDetails(
                trackId,
                binding.applicantName.text.toString(),
                binding.pickupLocation.text.toString(),
                binding.recipientName.text.toString(),
                binding.recipientMob.text.toString(),
                binding.recipientEmail.text.toString().trim(),
                binding.destLocation.text.toString(),
                binding.partnerName.text.toString(),
                partner.getJSONObject("document").getString("Uid"),
                secDate,
                secTime,
                binding.parcelWeight.text.toString(),
                binding.parcelPrice.text.toString(),
                false
            )

            val updateDet = TrackInfo(
                ShipmentType.Shipment_Created.ordinal,
                binding.pickupLocation.text.toString(),
                cal.timeInMillis
            )

            binding.indicatorCirclePro.visibility = View.VISIBLE
            binding.registerButton.visibility = View.GONE

            createOrder(Gson().toJson(orderDetail).toString(),
                Gson().toJson(updateDet).toString())
        }
    }

    private fun createOrder(dataBody: String, dataUpdate: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val client = OkHttpClient()
            val mediaType = "application/json".toMediaType()
            val bodyReq = "{\n" +
                    "      \"dataSource\": \"ClusterDrop\",\n" +
                    "      \"database\": \"deldata\",\n" +
                    "      \"collection\": \"parcels\",\n" +
                    "      \"document\":"+ dataBody + "}"

            val bodyUpdate = "{\n" +
                    "      \"dataSource\": \"ClusterDrop\",\n" +
                    "      \"database\": \"delupdates\",\n" +
                    "      \"collection\": \"" + trackId + "\"," +
                         "\"document\":"+ dataUpdate + "}"

            val request = Request.Builder()
                .url("https://data.mongodb-api.com/app/data-oageq/endpoint/data/beta/action/insertOne")
                .header("Content-Type", "application/json")
                .header("api-key", getString(R.string.apiKey))
                .post(bodyReq.toRequestBody(mediaType))
                .build()
            runCatching {
                val response = client.newCall(request).execute()
                val partnerInfo = response.body!!.string()
                val resObj = JSONObject(partnerInfo)

                if (!resObj.isNull("insertedId")) {
                    val reqUpdate = Request.Builder()
                        .url("https://data.mongodb-api.com/app/data-oageq/endpoint/data/beta/action/insertOne")
                        .header("Content-Type", "application/json")
                        .header("api-key", getString(R.string.apiKey))
                        .post(bodyUpdate.toRequestBody(mediaType))
                        .build()
                    val updateResponse = client.newCall(reqUpdate).execute()
                    val resStr = updateResponse.body!!.string()
                    val resUp = JSONObject(resStr)
                    CoroutineScope(Dispatchers.Main).launch {
                        if (resUp.isNull("insertedId")) {
                            AestheticDialog.Builder(this@CreateActivity, DialogStyle.TOASTER, DialogType.ERROR)
                                .setTitle("Error")
                                .setMessage("Failed to register the order !")
                                .setCancelable(false)
                                .setDarkMode(true)
                                .setGravity(Gravity.TOP)
                                .setAnimation(DialogAnimation.SHRINK)
                                .setOnClickListener(object : OnDialogClickListener {
                                    override fun onClick(dialog: AestheticDialog.Builder) {
                                        dialog.dismiss()
                                        finish()
                                    }
                                }).show()

                        } else {

                            AestheticDialog.Builder(
                                this@CreateActivity,
                                DialogStyle.TOASTER,
                                DialogType.SUCCESS
                            )
                                .setTitle("Shipment created")
                                .setMessage("Order registered successfully !")
                                .setCancelable(false)
                                .setDarkMode(true)
                                .setGravity(Gravity.TOP)
                                .setAnimation(DialogAnimation.SHRINK)
                                .setOnClickListener(object : OnDialogClickListener {
                                    override fun onClick(dialog: AestheticDialog.Builder) {
                                        dialog.dismiss()
                                        finish()
                                    }
                                }).show()
                        }
                    }
                }
            }
        }
    }

    private fun randomID(): String = List(8) {
        (('A'..'Z') + ('0'..'9')).random()
    }.joinToString("")

    @SuppressLint("SimpleDateFormat")
    private fun getDate(milliSeconds : Long):String {
        val dateFormat = SimpleDateFormat("dd MMMM yyyy")
        val cal = Calendar.getInstance()
        cal.timeInMillis = milliSeconds
        return dateFormat.format(cal.time)
    }

}