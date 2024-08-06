package com.ira.easytreat.activities

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.Button
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
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.ai.client.generativeai.Chat
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.content
import com.ira.easytreat.BuildConfig
import com.ira.easytreat.R
import com.ira.easytreat.database.Record
import com.ira.easytreat.database.TreatDAO
import com.ira.easytreat.utils.PreferenceManager
import com.ira.easytreat.utils.UIUtils
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.random.nextInt


class DetailsActivity : AppCompatActivity() {
    companion object {
        const val REQUEST_RECOGNIZE_MEDICINE = "What is medicine on the photo?"
        const val REQUEST_GET_NAME = "Tell the name of this medicine only."
        const val REQUEST_GET_SHORT_DESCRIPTION = "What is this medicine used for?"
        const val REQUEST_GET_ALTERNATIVE = "What are the other alternatives?"
        const val REQUEST_GET_INGREDIENTS = "What ingredients it consists of?"
        const val REQUEST_GET_ALERGENTS = "What allergens are in the composition?"
        const val REQUEST_TRANSLATE = "Translate to %s this text: %s"
        const val REQUEST_TRANSLATE_TO_LANGUAGE = "Translate response to %s"

        val dangerousContentSafety = SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.NONE)
        val generativeModel = GenerativeModel(
            modelName = "gemini-1.5-flash",
            //modelName = "gemini-1.5-pro-001",
            //modelName = "text-embedding-004",
            // Access your API key as a Build Configuration variable (see "Set up your API key" above)
            apiKey = BuildConfig.apiKey,
            safetySettings = listOf(dangerousContentSafety)
        )
        lateinit var chat: Chat
        var languages = arrayOf("English", "German", "French", "Hungarian", "Russian")
    }

    private lateinit var toolbar: Toolbar
    private lateinit var progressView: ConstraintLayout
    private lateinit var errorView: ConstraintLayout
    private lateinit var detailsView: ConstraintLayout
    private lateinit var recordImageView: ImageView
    private lateinit var descriptionTextView: TextView
    private lateinit var descriptionImageButton: ImageButton
    private lateinit var alternativesImageButton: ImageButton
    private lateinit var chatImageButton: ImageButton
    private lateinit var ingredientsImageButton: ImageButton
    private lateinit var allergensImageButton: ImageButton
    private lateinit var selectedTabTextView: TextView
    private lateinit var chatEditText: EditText
    private lateinit var sendImageButton: ImageButton
    private lateinit var reloadImageButton: ImageButton
    private lateinit var errorOkButton: Button
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
        errorView = findViewById(R.id.errorView)
        detailsView = findViewById(R.id.detailsView)
        recordImageView = findViewById(R.id.recordImageView)
        descriptionTextView = findViewById(R.id.descriptionTextView)
        descriptionImageButton = findViewById(R.id.descriptionLabelImageButton)
        ingredientsImageButton = findViewById(R.id.ingredientsLabelImageButton)
        alternativesImageButton = findViewById(R.id.alternativesLabelImageButton)
        allergensImageButton = findViewById(R.id.allergensLabelImageButton)
        selectedTabTextView = findViewById(R.id.selectedTabTextView)
        chatImageButton = findViewById(R.id.chatLabelImageButton)
        chatEditText = findViewById(R.id.chatEditText)
        sendImageButton = findViewById(R.id.sendButton)
        errorOkButton = findViewById(R.id.okButton)
        scrollView = findViewById(R.id.scrollView)
        reloadImageButton = findViewById(R.id.reloadImageButton)

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
                toolbar.title = record?.name
                descriptionTextView.text = record?.description

                bitmap?.let {
                    val inputContent = content {
                        image(it)
                        text(REQUEST_RECOGNIZE_MEDICINE)
                        role = "user"
                    }
                    chat = generativeModel.startChat(history = listOf(inputContent)
                    )
                }
            }
        }

        descriptionImageButton.setOnClickListener {
            this.descriptionTextView.text = record?.description
            unselectAllButtons()
            descriptionImageButton.setColorFilter(ContextCompat.getColor(this, R.color.black), PorterDuff.Mode.SRC_IN)
            selectedTabTextView.text = getString(R.string.description_label)
            chatEditText.visibility = View.GONE
            sendImageButton.visibility = View.GONE
            reloadImageButton.visibility = View.VISIBLE
            selectedTab = 0
        }
        ingredientsImageButton.setOnClickListener {
            var value = ""
            record?.ingredients?.let {
                value = it
            }
            if (!value.isEmpty()) {
                this.descriptionTextView.text = value
            } else {
                if (record != null) {
                    this.descriptionTextView.text = ""
                    progressView.visibility = View.VISIBLE
                    lifecycleScope.launch {
                        val ingredients = getIngredients()
                        if (ingredients != null) {
                            record!!.ingredients = ingredients
                            database.updateRecord(record!!)
                        }
                        runOnUiThread {
                            descriptionTextView.text = ingredients
                            progressView.visibility = View.GONE
                        }
                    }
                } else {
                    if (imagePath != null) {
                        recogniseMedecine(imagePath!!)
                    }
                }
            }
            unselectAllButtons()
            ingredientsImageButton.setColorFilter(ContextCompat.getColor(this, R.color.black), PorterDuff.Mode.SRC_IN)
            selectedTabTextView.text = getString(R.string.ingredients_label)
            chatEditText.visibility = View.GONE
            sendImageButton.visibility = View.GONE
            reloadImageButton.visibility = View.VISIBLE
            selectedTab = 1
        }
        alternativesImageButton.setOnClickListener {
            var value = ""
            record?.alternatives?.let {
                value = it
            }
            if (!value.isEmpty()) {
                this.descriptionTextView.text = value
            } else {
                if (record != null) {
                    this.descriptionTextView.text = ""
                    progressView.visibility = View.VISIBLE
                    lifecycleScope.launch {
                        val alternatives = getAlternatives()
                        if (alternatives != null) {
                            record!!.alternatives = alternatives
                            database.updateRecord(record!!)
                        }
                        runOnUiThread {
                            descriptionTextView.text = alternatives
                            progressView.visibility = View.GONE
                        }
                    }
                } else {
                    if (imagePath != null) {
                        recogniseMedecine(imagePath!!)
                    }
                }
            }
            unselectAllButtons()
            alternativesImageButton.setColorFilter(ContextCompat.getColor(this, R.color.black), PorterDuff.Mode.SRC_IN)
            selectedTabTextView.text = getString(R.string.alternative_label)
            chatEditText.visibility = View.GONE
            sendImageButton.visibility = View.GONE
            reloadImageButton.visibility = View.VISIBLE
            selectedTab = 2
        }
        allergensImageButton.setOnClickListener {
            var value = ""
            record?.ingredients?.let {
                value = it
            }
            if (!value.isEmpty()) {
                this.descriptionTextView.text = value
            } else {
                if (record != null) {
                    this.descriptionTextView.text = ""
                    progressView.visibility = View.VISIBLE
                    lifecycleScope.launch {
                        val allergens = getAllergens()
                        if (allergens != null) {
                            record!!.alternatives = allergens
                            database.updateRecord(record!!)
                        }
                        runOnUiThread {
                            descriptionTextView.text = allergens
                            progressView.visibility = View.GONE
                        }
                    }
                } else {
                    if (imagePath != null) {
                        recogniseMedecine(imagePath!!)
                    }
                }
            }
            unselectAllButtons()
            allergensImageButton.setColorFilter(ContextCompat.getColor(this, R.color.black), PorterDuff.Mode.SRC_IN)
            selectedTabTextView.text = getString(R.string.allergens_label)
            chatEditText.visibility = View.GONE
            sendImageButton.visibility = View.GONE
            reloadImageButton.visibility = View.VISIBLE
            selectedTab = 3
        }
        chatImageButton.setOnClickListener({
            this.descriptionTextView.text = chatText
            unselectAllButtons()
            chatImageButton.setColorFilter(ContextCompat.getColor(this, R.color.black), PorterDuff.Mode.SRC_IN)
            selectedTabTextView.text = getString(R.string.chat_label)
            chatEditText.visibility = View.VISIBLE
            sendImageButton.visibility = View.VISIBLE
            reloadImageButton.visibility = View.GONE
            selectedTab = 4
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

        reloadImageButton.setOnClickListener {
            if (record == null) {
                val bitmap = UIUtils.getBitmapFromFilePath(imagePath!!)
                if (bitmap != null) {
                    runOnUiThread {
                        progressView.visibility = View.VISIBLE
                    }
                    lifecycleScope.launch {
                        val result = recognizeImage(bitmap)
                        if (result != null) {
                            val range = IntRange(0, Int.MAX_VALUE)
                            val id = Random.nextInt(range)
                            record = Record(
                                id,
                                result,
                                imagePath!!,
                                "",
                                null,
                                null,
                                null,
                                java.sql.Date(System.currentTimeMillis())
                            )
                            database.insertRecord(record!!)
                            reloadData()
                            runOnUiThread {
                                // Update UI elements on the main thread using runOnUiThread
                                progressView.visibility = View.GONE
                                toolbar.title = result
                                recordImageView.setImageBitmap(bitmap)
                            }
                        }
                        runOnUiThread {
                            // Update UI elements on the main thread using runOnUiThread
                            progressView.visibility = View.GONE
                            recordImageView.setImageBitmap(bitmap)
                        }
                    }
                }
            } else {
                reloadData()
            }
        }
        errorOkButton.setOnClickListener {
            errorView.visibility = View.GONE
        }

        errorView.visibility = View.GONE
        errorView.isEnabled = false
    }

    private fun reloadData() {
        // reload data
        runOnUiThread {
            progressView.visibility = View.VISIBLE
        }
        lifecycleScope.launch {
            var response: String? = ""
            if (selectedTab == 0) {
                response = getDescription()
                if (response != null) {
                    record!!.description = response
                    database.updateRecord(record!!)
                }
            } else if (selectedTab == 1) {
                response = getIngredients()
                if (response != null) {
                    record!!.ingredients = response
                    database.updateRecord(record!!)
                }
            } else if (selectedTab == 2) {
                response = getAlternatives()
                if (response != null) {
                    record!!.alternatives = response
                    database.updateRecord(record!!)
                }
            } else if (selectedTab == 3) {
                response = getAllergens()
                if (response != null) {
                    record!!.allergens = response
                    database.updateRecord(record!!)
                }
            }
            runOnUiThread {
                descriptionTextView.text = response
                progressView.visibility = View.GONE
            }
        }
    }

    private fun unselectAllButtons() {
        descriptionImageButton.setColorFilter(ContextCompat.getColor(this, R.color.white), PorterDuff.Mode.SRC_IN)
        ingredientsImageButton.setColorFilter(ContextCompat.getColor(this, R.color.white), PorterDuff.Mode.SRC_IN)
        alternativesImageButton.setColorFilter(ContextCompat.getColor(this, R.color.white), PorterDuff.Mode.SRC_IN)
        allergensImageButton.setColorFilter(ContextCompat.getColor(this, R.color.white), PorterDuff.Mode.SRC_IN)
        chatImageButton.setColorFilter(ContextCompat.getColor(this, R.color.white), PorterDuff.Mode.SRC_IN)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.details_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
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
                            record!!.ingredients = result
                        } else if (selectedTab == 2) {
                            record!!.alternatives = result
                        } else if (selectedTab == 3) {
                            record!!.allergens = result
                        }
                        progressView.visibility = View.GONE
                    }
                }
            }
        }
    }

    fun recogniseMedecine(imagePath: String) {
        progressView.visibility = View.VISIBLE
        val bitmap = UIUtils.getBitmapFromFilePath(imagePath)
        if (bitmap != null) {
            lifecycleScope.launch {
                val result = recognizeImage(bitmap)
                if (result != null) {
                    val description = getDescription()
                    if (description != null) {
                        val range = IntRange(0, Int.MAX_VALUE)
                        val id = Random.nextInt(range)
                        record = Record(id, result, imagePath, description, null, null, null, java.sql.Date(System.currentTimeMillis()))
                        database.insertRecord(record!!)
                        runOnUiThread {
                            // Update UI elements on the main thread using runOnUiThread
                            progressView.visibility = View.GONE
                            toolbar.title = result
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
        } else {
            val context = this
            val builder = AlertDialog.Builder(context)
                .setTitle("Error")
                .setMessage("Image is not available")
                .setPositiveButton("OK") { dialog, which ->
                    dialog.dismiss()
                    finish()
                }
            val dialog = builder.create()
            dialog.show()
        }
    }

    suspend fun requestMessage(message: String): String? {
        //for (attempt in 1..3) {
            try {
                var messageToRequest = message
                val language = PreferenceManager.getLanguage(this, "")
                if (!language.isEmpty() && !language.equals("English")) {
                    messageToRequest += " " + String.format(REQUEST_TRANSLATE_TO_LANGUAGE, language)
                }
                val response = chat.sendMessage(messageToRequest)
                return response.text
            } catch (exc: Exception) {
                exc.printStackTrace()
            }
        //}
        showErrorDialog("Can't recognise it. Try again.")
        return null
    }

    suspend fun getDescription(): String? {
        return requestMessage(REQUEST_GET_SHORT_DESCRIPTION)
    }

    suspend fun sendRequest(request: String): String? {
        return requestMessage(request)
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

    suspend fun getAlternatives(): String? {
        return requestMessage(REQUEST_GET_ALTERNATIVE)
    }

    suspend fun getIngredients(): String? {
        return requestMessage(REQUEST_GET_INGREDIENTS)
    }

    suspend fun getAllergens(): String? {
        return requestMessage(REQUEST_GET_ALERGENTS)
    }

    fun showErrorDialog(message: String) {
        errorView.visibility = View.VISIBLE
    }

    suspend fun recognizeImage(bitmap: Bitmap): String? {
        //for (attempt in 1..3) {
            try {
                val inputContent = content {
                    image(bitmap)
                    text(REQUEST_RECOGNIZE_MEDICINE)
                    role = "user"
                }
                chat = generativeModel.startChat(history = listOf(inputContent))
                return requestMessage(REQUEST_GET_NAME)
            } catch (exc: Exception) {
                exc.printStackTrace()
            }
        //}
        showErrorDialog("Can't recognise image")
        return null
    }
}