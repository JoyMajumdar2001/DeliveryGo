package com.qdot.deliverygo

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.qdot.deliverygo.databinding.ActivityPartnerBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class PartnerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPartnerBinding
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPartnerBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        val partner = JSONObject(intent.getStringExtra("data").toString())
        binding.firstLetterText.text = partner.getJSONObject("document").getString("name").substring(0,1)
        binding.userNameText.text = partner.getJSONObject("document").getString("name") + " (" +
                partner.getJSONObject("document").getString("Uid") + ") "

        binding.createButton.setOnClickListener {
            val intent1 = Intent(this,CreateActivity::class.java)
            intent1.putExtra("data2",intent.getStringExtra("data").toString())
            startActivity(intent1)
        }

        binding.updateButton.setOnClickListener {
            checkTrackId(binding.trackIdText.text.toString().trim())
        }
    }

    private fun checkTrackId(uniqueId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val client = OkHttpClient()
            val mediaType = "application/json".toMediaType()
            val bodyReq = "{\n" +
                    "      \"dataSource\": \"ClusterDrop\",\n" +
                    "      \"database\": \"deldata\",\n" +
                    "      \"collection\": \"parcels\",\n" +
                    "      \"filter\": {\n" +
                    "        \"Trackid\": \"" +uniqueId + "\"" +
                    "      }\n" +
                    "}"
            val request = Request.Builder()
                .url("https://data.mongodb-api.com/app/data-oageq/endpoint/data/beta/action/findOne")
                .header("Content-Type", "application/json")
                .header("api-key", getString(R.string.apiKey))
                .post(bodyReq.toRequestBody(mediaType))
                .build()
            runCatching {
                val response = client.newCall(request).execute()
                val trackInfo = response.body!!.string()
                val objJson = JSONObject(trackInfo)
                withContext(Dispatchers.Main){
                    if (objJson.isNull("document")){
                        Toast.makeText(this@PartnerActivity,"Tracking id is wrong!",
                        Toast.LENGTH_SHORT).show()
                    }else{
                        val intent = Intent(this@PartnerActivity,UpdateActivity::class.java)
                        intent.putExtra("tdata",trackInfo)
                        intent.putExtra("partner",true)
                        startActivity(intent)
                    }
                }
            }
        }
    }
}