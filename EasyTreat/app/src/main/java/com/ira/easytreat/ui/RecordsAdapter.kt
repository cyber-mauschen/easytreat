package com.ira.easytreat.ui

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.ira.easytreat.R
import com.ira.easytreat.activities.DetailsActivity
import com.ira.easytreat.activities.MainActivity
import com.ira.easytreat.database.Record
import com.ira.easytreat.database.TreatDAO
import com.ira.easytreat.utils.UIUtils

interface RecordsListener {
    fun onRecordsListUpdated(recordsList: ArrayList<Record>)
}

class RecordsAdapter(private val context: Context, private val listener: RecordsListener) : RecyclerView.Adapter<RecordsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.record_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val dao = TreatDAO(context)
        val dataList = dao.getRecords()
        val record = dataList[position]
        viewHolder.titleTextView.text = record.name
        viewHolder.subtitleTextView.text = record.description
        if (record.imagePath != null) {
            val bitmap = UIUtils.getBitmapFromFilePath(record.imagePath)
            if (bitmap != null) {
                viewHolder.recordImageView.setImageBitmap(bitmap)
            }
        }
        viewHolder.deleteButton.setOnClickListener {
            val builder = AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.warning))
                .setMessage(context.getString(R.string.delete_message))
                .setPositiveButton(context.getString(R.string.no)) { dialog, which ->
                    dialog.dismiss()
                }
                .setNegativeButton(context.getString(R.string.yes)) { dialog, which ->
                    dialog.dismiss()
                    val dao = TreatDAO(context)
                    if (dao.deleteRecord(record) > 0) {
                        this.notifyDataSetChanged()
                        listener.onRecordsListUpdated(dao.getRecords())
                    }
                }
            val dialog = builder.create()
            dialog.show()
        }
        viewHolder.itemView.setOnClickListener {
            val intent = Intent(context, DetailsActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_ITEM_ID, record.id)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemCount(): Int {
        val dao = TreatDAO(context)
        val dataList = dao.getRecords()
        return dataList.size
    }

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView
        val subtitleTextView: TextView
        val recordImageView: ImageView
        val deleteButton: ImageButton

        init {
            titleTextView = itemView.findViewById(R.id.titleTextView)
            subtitleTextView = itemView.findViewById(R.id.subtitleTextView)
            recordImageView = itemView.findViewById(R.id.recordImageView)
            deleteButton = itemView.findViewById(R.id.deleteButton)
        }
    }
}