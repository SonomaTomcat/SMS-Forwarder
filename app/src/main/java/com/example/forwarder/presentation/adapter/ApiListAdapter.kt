package com.example.forwarder.presentation.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.CheckBox
import android.graphics.Color
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.widget.RecyclerView
import com.example.forwarder.R
import com.example.forwarder.domain.model.Api
import com.example.forwarder.domain.repository.ApiRepository

class ApiListAdapter(
    private val items: List<Api>,
    private val onItemClick: (Int) -> Unit,
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit
) : RecyclerView.Adapter<ApiListAdapter.ViewHolder>() {
    var selectionTracker: SelectionTracker<Long>? = null

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long = position.toLong()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textRemark: TextView = itemView.findViewById(R.id.text_remark)
        val textSummary: TextView = itemView.findViewById(R.id.text_summary)
        val dragHandle: ImageView = itemView.findViewById(R.id.iv_drag_handle)
        val checkboxSelect: CheckBox = itemView.findViewById(R.id.checkbox_select)

        fun getItemDetails(): ItemDetailsLookup.ItemDetails<Long> =
            object : ItemDetailsLookup.ItemDetails<Long>() {
                override fun getPosition() = bindingAdapterPosition
                override fun getSelectionKey() = itemId
            }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.item_api_list, parent, false
        )
        return ViewHolder(view)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        if (item.remark.isNotBlank()) {
            holder.textRemark.text = item.remark
            holder.textRemark.visibility = View.VISIBLE
        } else {
            holder.textRemark.text = item.url.baseUrl.ifBlank { "(No Base URL)" }
            holder.textRemark.visibility = View.VISIBLE
        }
        holder.textSummary.text = ApiRepository.getUrl(item)

        val isSel = selectionTracker?.isSelected(holder.itemId) ?: false
        val isSelectionMode = selectionTracker?.hasSelection() ?: false
        holder.checkboxSelect.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
        holder.checkboxSelect.isChecked = isSel
        holder.itemView.isActivated = isSel
        holder.itemView.setBackgroundColor(
            if (isSel) holder.itemView.context.getColor(R.color.selected_item_bg) else Color.TRANSPARENT
        )

        holder.itemView.setOnClickListener {
            if (selectionTracker?.hasSelection() == true) {
                selectionTracker?.select(holder.itemId)
            } else {
                onItemClick(position)
            }
        }
        holder.itemView.setOnLongClickListener {
            selectionTracker?.select(holder.itemId)
            true
        }
        holder.dragHandle.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                onStartDrag(holder)
            }
            false
        }
    }

    override fun getItemCount(): Int = items.size
}

