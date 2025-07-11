package com.example.forwarder.presentation.adapter

import android.annotation.SuppressLint
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.forwarder.R
import com.example.forwarder.domain.model.QueryParam
import com.google.android.material.textfield.TextInputEditText
import androidx.recyclerview.widget.ItemTouchHelper

class ApiEditQueryAdapter(
    private val items: MutableList<QueryParam>,
    private val onDelete: (Int) -> Unit,
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit
) : RecyclerView.Adapter<ApiEditQueryAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val editKey: TextInputEditText = view.findViewById(R.id.edit_key)
        val editValue: TextInputEditText = view.findViewById(R.id.edit_value)
        val btnDelete: ImageButton = view.findViewById(R.id.btn_delete)
        val dragHandle: ImageView = view.findViewById(R.id.iv_drag_handle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_api_edit_query, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val param = items[position]
        holder.editKey.setText(param.key ?: "")
        holder.editValue.setText(param.value ?: "")
        // Remove focus change listeners to prevent multiple triggers
        holder.editKey.onFocusChangeListener = null
        holder.editValue.onFocusChangeListener = null
        holder.editKey.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                items[holder.adapterPosition].key = s?.toString()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        holder.editValue.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                items[holder.adapterPosition].value = s?.toString()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        holder.btnDelete.setOnClickListener {
            onDelete(holder.adapterPosition)
        }
        holder.dragHandle.setOnTouchListener { _, _ ->
            onStartDrag(holder)
            false
        }
    }

    override fun getItemCount(): Int = items.size

    fun moveItem(from: Int, to: Int) {
        if (from == to) return
        val item = items.removeAt(from)
        items.add(to, item)
        notifyItemMoved(from, to)
    }
}

class QueryParamTouchHelperCallback(
    private val adapter: ApiEditQueryAdapter
) : ItemTouchHelper.Callback() {
    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        return makeMovementFlags(dragFlags, 0)
    }
    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        adapter.moveItem(viewHolder.adapterPosition, target.adapterPosition)
        return true
    }
    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
    override fun isLongPressDragEnabled(): Boolean = false
    override fun isItemViewSwipeEnabled(): Boolean = false
}

