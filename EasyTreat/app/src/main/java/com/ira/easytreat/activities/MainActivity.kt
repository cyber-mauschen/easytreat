package com.ira.easytreat.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.ListView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.ai.client.generativeai.GenerativeModel
import com.ira.easytreat.BuildConfig
import com.ira.easytreat.R
import com.ira.easytreat.database.Record
import com.ira.easytreat.database.TreatDAO
import com.ira.easytreat.ui.RecordsAdapter
import com.ira.easytreat.ui.RecordsListener
import com.ira.easytreat.utils.PreferenceManager

class MainActivity : AppCompatActivity(), RecordsListener {
    companion object {
        const val EXTRA_IMAGE_PATH = "image_path"
        const val EXTRA_ITEM_ID = "item_id"
    }

    private lateinit var toolbar: Toolbar
    private lateinit var recordsListView: RecyclerView
    private lateinit var emptyListTextView: TextView
    private lateinit var cameraButton: ImageButton
    private lateinit var adapter: RecordsAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        recordsListView = findViewById(R.id.recordsListView)
        emptyListTextView = findViewById(R.id.emptyListTextLabel)
        cameraButton = findViewById(R.id.cameraButton)

        recordsListView.layoutManager = LinearLayoutManager(this)

        cameraButton.setOnClickListener({
            val intent = Intent(this, CameraActivity::class.java)
            startForResult.launch(intent)
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_language) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(getString(R.string.select_language))
            builder.setItems(DetailsActivity.languages) { _, which ->
                PreferenceManager.saveLanguage(this, DetailsActivity.languages[which])
            }
            builder.show()
        }
        return false
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()
        val recordsList = TreatDAO(this).getRecords()
        if (recordsList.size > 0) {
            emptyListTextView.visibility = View.GONE
        }
        adapter = RecordsAdapter(this, this)
        recordsListView.adapter = adapter
        adapter.notifyDataSetChanged()
    }

    private val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            // Handle the result here
            val data = result.data as Intent
            val path = data.extras?.getString(CameraActivity.EXTRA_IMAGE_PATH)
            Log.d("MainActivity", "Result: " + path)
            // ...
            val intent = Intent(this, DetailsActivity::class.java).apply {
                putExtra(EXTRA_IMAGE_PATH, path)
            }
            startActivity(intent)
        }
    }

    override fun onRecordsListUpdated(recordsList: ArrayList<Record>) {
        if (recordsList.size > 0) {
            emptyListTextView.visibility = View.GONE
        } else {
            emptyListTextView.visibility = View.VISIBLE
        }
    }
}