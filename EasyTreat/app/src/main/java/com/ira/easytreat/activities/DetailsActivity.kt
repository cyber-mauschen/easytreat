package com.ira.easytreat.activities

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.ai.client.generativeai.Chat
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.ira.easytreat.BuildConfig
import com.ira.easytreat.R
import com.ira.easytreat.database.Record
import com.ira.easytreat.database.TreatDAO
import com.ira.easytreat.utils.UIUtils
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.random.nextInt

class DetailsActivity : AppCompatActivity() {
    companion object {
        const val REQUEST_RECOGNIZE_MEDICINE = "Describe the medicine in this image."
        const val REQUEST_GET_NAME = "Tell the name only."
        const val REQUEST_GET_SHORT_DESCRIPTION = "What is it used for?"
        //const val REQUEST_GET_GUIDANCE = "Show instructions for use."
        const val REQUEST_GET_ALTERNATIVE = "What are the other alternatives?"
        const val REQUEST_TRANSLATE = "Translate to %s this text: %s"

        val generativeModel = GenerativeModel(
            modelName = "gemini-1.5-flash",
            // Access your API key as a Build Configuration variable (see "Set up your API key" above)
            apiKey = BuildConfig.apiKey
        )
        lateinit var chat: Chat
        var languages = arrayOf("English", "German", "French", "Hungarian", "Russian")
    }

    private lateinit var toolbar: Toolbar
    private lateinit var progressView: ConstraintLayout
    private lateinit var detailsView: ConstraintLayout
    private lateinit var titleTextView: TextView
    private lateinit var recordImageView: ImageView
    private lateinit var descriptionTextView: TextView
    private lateinit var descriptionLabelTextView: TextView
    private lateinit var guidanceLabelTextView: TextView
    private lateinit var chatLabelTextView: TextView
    private lateinit var chatEditText: EditText
    private lateinit var sendImageButton: ImageButton
    private lateinit var scrollView: ScrollView
    private lateinit var database: TreatDAO
    private var record: Record? = null
    private var imagePath: String? = null
    private var chatText: String = ""
    private var selectedTab: Int = 0
    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_details)
        setSupportActionBar(findViewById(R.id.toolbar))
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        progressView = findViewById(R.id.progressView)
        detailsView = findViewById(R.id.detailsView)
        titleTextView = findViewById(R.id.titleTextView)
        recordImageView = findViewById(R.id.recordImageView)
        descriptionTextView = findViewById(R.id.descriptionTextView)
        descriptionLabelTextView = findViewById(R.id.descriptionLabelTextView)
        guidanceLabelTextView = findViewById(R.id.guidanceLabelTextView)
        chatLabelTextView = findViewById(R.id.chatLabelTextView)
        chatEditText = findViewById(R.id.chatEditText)
        sendImageButton = findViewById(R.id.sendButton)
        scrollView = findViewById(R.id.scrollView)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        database = TreatDAO(this)

        imagePath = intent.extras?.getString(MainActivity.EXTRA_IMAGE_PATH)
        if (imagePath != null) {
            recogniseMedecine(imagePath!!)
        }
        val recordId = intent.extras?.getInt(MainActivity.EXTRA_ITEM_ID)
        if (recordId != null && recordId > 0) {
            val storedRecord = database.getRecord(recordId)
            if (storedRecord != null) {
                record = storedRecord
                progressView.visibility = View.GONE
                imagePath = record?.imagePath
                val bitmap = UIUtils.getBitmapFromFilePath(imagePath!!)
                recordImageView.setImageBitmap(bitmap)
                titleTextView.text = record?.name
                descriptionTextView.text = record?.description

                bitmap?.let {
                    val inputContent = content {
                        image(it)
                        text(REQUEST_RECOGNIZE_MEDICINE)
                        role = "user"
                    }
                    chat = generativeModel.startChat(history = listOf(inputContent))
                }
            }
        }

        descriptionLabelTextView.setOnClickListener {
            this.descriptionTextView.text = record?.description
            descriptionLabelTextView.background = getDrawable(R.drawable.selected_item)
            guidanceLabelTextView.background = getDrawable(R.drawable.unselected_item)
            chatLabelTextView.background = getDrawable(R.drawable.unselected_item)
            chatEditText.visibility = View.GONE
            sendImageButton.visibility = View.GONE
            selectedTab = 0
        }
        guidanceLabelTextView.setOnClickListener({
            this.descriptionTextView.text = record?.guidance
            descriptionLabelTextView.background = getDrawable(R.drawable.unselected_item)
            guidanceLabelTextView.background = getDrawable(R.drawable.selected_item)
            chatLabelTextView.background = getDrawable(R.drawable.unselected_item)
            chatEditText.visibility = View.GONE
            sendImageButton.visibility = View.GONE
            selectedTab = 1
        })
        chatLabelTextView.setOnClickListener({
            this.descriptionTextView.text = chatText
            descriptionLabelTextView.background = getDrawable(R.drawable.unselected_item)
            guidanceLabelTextView.background = getDrawable(R.drawable.unselected_item)
            chatLabelTextView.background = getDrawable(R.drawable.selected_item)
            chatEditText.visibility = View.VISIBLE
            sendImageButton.visibility = View.VISIBLE
            selectedTab = 2
        })

        sendImageButton.setOnClickListener {
            if (chatEditText.text.toString().length > 0) {
                chatText += "\n----------------------\n" + chatEditText.text.toString()
                descriptionTextView.text = chatText
                progressView.visibility = View.VISIBLE
                lifecycleScope.launch {
                    val result = sendRequest(chatEditText.text.toString())
                    if (result != null) {
                        runOnUiThread {
                            // Update UI elements on the main thread using runOnUiThread
                            chatEditText.text.clear()
                            chatText += "\n----------------------\n" + result
                            descriptionTextView.text = chatText
                            scrollView.post {
                                scrollView.fullScroll(ScrollView.FOCUS_DOWN)
                            }
                        }
                    }
                    progressView.visibility = View.GONE
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.details_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        } else if (item.itemId == R.id.action_save) {
            if (record != null) {
                if (database.getRecord(record!!.id) != null) {
                    database.updateRecord(record!!)
                    val builder = AlertDialog.Builder(this)
                        .setTitle(getString(R.string.done))
                        .setMessage(getString(R.string.record_saved))
                        .setPositiveButton(getString(R.string.ok)) { dialog, which ->
                            dialog.dismiss()
                            finish()
                        }
                    val dialog = builder.create()
                    dialog.show()
                    return true
                }
                val result = database.insertRecord(record!!)
                if (result > 0) {
                    val builder = AlertDialog.Builder(this)
                        .setTitle(getString(R.string.done))
                        .setMessage(getString(R.string.record_saved))
                        .setPositiveButton(getString(R.string.ok)) { dialog, which ->
                            dialog.dismiss()
                            finish()
                        }
                    val dialog = builder.create()
                    dialog.show()
                    return true
                } else {
                    val builder = AlertDialog.Builder(this)
                        .setTitle(getString(R.string.warning))
                        .setMessage(getString(R.string.record_already_saved))
                        .setPositiveButton(getString(R.string.ok)) { dialog, which ->
                            dialog.dismiss()
                        }
                    val dialog = builder.create()
                    dialog.show()
                }
            }
        } else if (item.itemId == R.id.action_reload) {
            if (imagePath != null) {
                recogniseMedecine(imagePath!!)
            }
        }  else if (item.itemId == R.id.action_language) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(getString(R.string.select_language))
            builder.setItems(languages) { _, which ->
                translateToLanguage(languages[which])
            }
            builder.show()
        }
        return false
    }

    fun translateToLanguage(language: String) {
        lifecycleScope.launch {
            if (record != null) {
                val text = descriptionTextView.text.toString()
                progressView.visibility = View.VISIBLE
                val result = translateToLanguage(text, language)
                runOnUiThread {
                    // Update UI elements on the main thread using runOnUiThread
                    if (result != null) {
                        descriptionTextView.text = result
                        if (selectedTab == 0) {
                            record!!.description = result
                        } else if (selectedTab == 1) {
                            record!!.guidance = result
                        }
                        progressView.visibility = View.GONE
                    }
                }
            }
        }
    }

    fun recogniseMedecine(imagePath: String) {
        progressView.visibility = View.VISIBLE
        val bitmap = UIUtils.getBitmapFromFilePath(imagePath!!)
        if (bitmap != null) {
            lifecycleScope.launch {
                val result = recognizeImage(bitmap)
                if (result != null) {
                    val description = getDescription()
                    val guidance = getAlternatives()
                    if (description != null) {
                        val range = IntRange(0, Int.MAX_VALUE)
                        val id = Random.nextInt(range)
                        record = Record(id, result, imagePath!!, description, guidance, false, false, false)
                        runOnUiThread {
                            // Update UI elements on the main thread using runOnUiThread
                            progressView.visibility = View.GONE
                            titleTextView.text = result
                            descriptionTextView.text = description
                            recordImageView.setImageBitmap(bitmap)
                        }
                    }
                }
                runOnUiThread {
                    // Update UI elements on the main thread using runOnUiThread
                    progressView.visibility = View.GONE
                    recordImageView.setImageBitmap(bitmap)
                }
            }
        }
    }

    suspend fun getDescription(): String? {
        try {
            val response = chat.sendMessage(REQUEST_GET_SHORT_DESCRIPTION)
            print(response.text)
            return response.text
        } catch (exc: Exception) {
            exc.printStackTrace()
            showErrorDialog("Can't recognise it. Try again.")
        }
        return null
    }

    suspend fun sendRequest(request: String): String? {
        try {
            val response = chat.sendMessage(request)
            print(response.text)
            return response.text
        } catch (exc: Exception) {
            exc.printStackTrace()
            showErrorDialog("Can't recognise it. Try again.")
        }
        return null
    }

    suspend fun translateToLanguage(text: String, language: String): String? {
        try {
            val response = chat.sendMessage(String.format(REQUEST_TRANSLATE, language, text))
            print(response.text)
            return response.text
        } catch (exc: Exception) {
            exc.printStackTrace()
            showErrorDialog("Can't recognise it. Try again.")
        }
        return null
    }

    /*
    suspend fun getGuidance(): String? {
        try {
            val response = chat.sendMessage(REQUEST_GET_GUIDANCE)
            print(response.text)
            return response.text
        } catch (exc: Exception) {
            exc.printStackTrace()
            showErrorDialog("Can't recognise it. Try again.")
        }
        return null
    }
    */

    suspend fun getAlternatives(): String? {
        try {
            val response = chat.sendMessage(REQUEST_GET_ALTERNATIVE)
            print(response.text)
            return response.text
        } catch (exc: Exception) {
            exc.printStackTrace()
            showErrorDialog("Can't recognise it. Try again.")
        }
        return null
    }

    fun showErrorDialog(message: String) {
        val context = this
        val builder = AlertDialog.Builder(context)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, which ->
                dialog.dismiss()
                //finish()
            }
        val dialog = builder.create()
        dialog.show()
    }

    suspend fun recognizeImage(bitmap: Bitmap): String? {
        try {
            val inputContent = content {
                image(bitmap)
                text(REQUEST_RECOGNIZE_MEDICINE)
                role = "user"
            }
            chat = generativeModel.startChat(history = listOf(inputContent))
            val response = chat.sendMessage(REQUEST_GET_NAME)

            print(response.text)
            return response.text
        } catch (exc: Exception) {
            exc.printStackTrace()
            val message = exc.message
            print(message)
            when (exc.message) {
                "Content generation stopped. Reason: SAFETY" -> {
                    showErrorDialog("Can't recognise it. Try again: " + exc.message)
                }
                else -> {
                    showErrorDialog("Can't recognise image: " + exc.message)
                }
            }
        }
        return null
    }
}