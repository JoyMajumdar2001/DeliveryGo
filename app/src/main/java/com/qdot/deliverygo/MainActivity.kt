package com.qdot.deliverygo

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.qdot.deliverygo.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var loginButton : MaterialButton
    private lateinit var progressIndicator : CircularProgressIndicator
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.deliveryLoginBtn.setOnClickListener {
            showBottomLoginSheet()
        }

        binding.trackButton.setOnClickListener {
            checkTrackId(binding.idText.text.toString().trim())
        }


    }

    @SuppressLint("InflateParams")
    private fun showBottomLoginSheet(){
        val dialog = BottomSheetDialog(this)

        val view = layoutInflater.inflate(R.layout.bottom_sheet_login, null)

        val editTextId = view.findViewById<TextInputEditText>(R.id.unique_id_input)

        loginButton = view.findViewById(R.id.login_button)

        progressIndicator = view.findViewById(R.id.indicatorCircle)

        progressIndicator.visibility = View.GONE

        loginButton.setOnClickListener {
            loginWithUniqueId(editTextId.text.toString())
            progressIndicator.visibility = View.VISIBLE
            loginButton.visibility = View.GONE
        }

        dialog.setContentView(view)
        dialog.show()
    }

    private fun loginWithUniqueId(uniqueId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val client = OkHttpClient()
            val mediaType = "application/json".toMediaType()
            val bodyReq = "{\n" +
                    "      \"dataSource\": \"ClusterDrop\",\n" +
                    "      \"database\": \"deldata\",\n" +
                    "      \"collection\": \"partners\",\n" +
                    "      \"filter\": {\n" +
                    "        \"Uid\": \"" +uniqueId + "\"" +
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
                val partnerInfo = response.body!!.string()
                CoroutineScope(Dispatchers.Main).launch {
                    validateLogin(partnerInfo)
                }
            }
        }
    }

    private fun validateLogin(partnerInfo:String){
        val objJson = JSONObject(partnerInfo)
        if (objJson.isNull("document")){
            progressIndicator.visibility = View.GONE
            loginButton.visibility = View.VISIBLE
            Toast.makeText(this,"Wrong Unique ID",Toast.LENGTH_SHORT).show()
        }else{
            val intent = Intent(this@MainActivity,PartnerActivity::class.java)
            intent.putExtra("data",partnerInfo)
            startActivity(intent)
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
                    "        \"Trackid\": \"" + uniqueId + "\"" +
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
                        Toast.makeText(this@MainActivity,"Tracking id is wrong!",
                            Toast.LENGTH_SHORT).show()
                    }else{
                        val intent = Intent(this@MainActivity,UpdateActivity::class.java)
                        intent.putExtra("tdata",trackInfo)
                        intent.putExtra("partner",false)
                        startActivity(intent)
                    }
                }
            }
        }
    }
}