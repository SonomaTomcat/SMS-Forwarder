package com.example.forwarder.presentation.model

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.forwarder.R
import com.example.forwarder.domain.model.Api
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.example.forwarder.presentation.adapter.ApiListAdapter
import com.example.forwarder.domain.repository.ApiRepository
import com.example.forwarder.presentation.ApiEditFragment
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StableIdKeyProvider
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.ItemDetailsLookup

class ApiManagerFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var addButton: FloatingActionButton
    private val apiList = mutableListOf<Api>()
    private lateinit var adapter: ApiListAdapter
    private lateinit var repository: ApiRepository
    private var selectedIndex: Int? = null
    private val selectedItems = mutableSetOf<Int>()
    private var isMultiSelectMode = false
    private var actionMode: androidx.appcompat.view.ActionMode? = null
    private lateinit var itemTouchHelper: ItemTouchHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_api_manager, container, false)
        recyclerView = view.findViewById(R.id.recycler_api_list)
        addButton = view.findViewById(R.id.btn_add_api)
        setHasOptionsMenu(true)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = ApiRepository(requireContext())
        loadApiList()
        adapter = ApiListAdapter(
            apiList,
            onItemClick = { position ->
                // Handle item click (SelectionTracker manages multi-select now)
                if (adapter.selectionTracker?.hasSelection() == true) {
                    adapter.selectionTracker?.select(position.toLong())
                } else {
                    selectedIndex = position
                    val fragment = ApiEditFragment().apply {
                        arguments = Bundle().apply {
                            putInt("api_index", position)
                            putParcelable("api", apiList[position])
                        }
                    }
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.settings_container, fragment)
                        .addToBackStack(null)
                        .commit()
                }
            },
            onStartDrag = { viewHolder ->
                itemTouchHelper.startDrag(viewHolder)
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        // Enable drag & drop sorting via handle only
        itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.adapterPosition
                val to = target.adapterPosition
                if (from != to) {
                    val item = apiList.removeAt(from)
                    apiList.add(to, item)
                    adapter.notifyItemMoved(from, to)
                    saveApiList()
                }
                return true
            }
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
            override fun isLongPressDragEnabled(): Boolean = false
            override fun isItemViewSwipeEnabled(): Boolean = false
        })
        itemTouchHelper.attachToRecyclerView(recyclerView)

        // Set up selection tracker for long-press selection
        val selectionTracker = SelectionTracker.Builder<Long>(
            "apiSelection",
            recyclerView,
            StableIdKeyProvider(recyclerView),
            object : ItemDetailsLookup<Long>() {
                override fun getItemDetails(e: MotionEvent): ItemDetailsLookup.ItemDetails<Long>? {
                    val view = recyclerView.findChildViewUnder(e.x, e.y) ?: return null
                    val holder = recyclerView.getChildViewHolder(view) as ApiListAdapter.ViewHolder
                    return holder.getItemDetails()
                }
            },
            StorageStrategy.createLongStorage()
        ).withSelectionPredicate(SelectionPredicates.createSelectAnything())
         .build()
        adapter.selectionTracker = selectionTracker

        addButton.setOnClickListener {
            val fragment = ApiEditFragment()
            val bundle = Bundle()
            bundle.putInt("api_index", -1)
            fragment.arguments = bundle
            parentFragmentManager.beginTransaction()
                .replace(R.id.settings_container, fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun enterMultiSelectMode() {
        if (isMultiSelectMode) return
        isMultiSelectMode = true
        activity?.invalidateOptionsMenu() // Refresh the options menu
        actionMode = (activity as? androidx.appcompat.app.AppCompatActivity)?.startSupportActionMode(actionModeCallback)
        adapter.notifyDataSetChanged()
    }

    private fun exitMultiSelectMode() {
        isMultiSelectMode = false
        selectedItems.clear()
        actionMode?.finish()
        activity?.invalidateOptionsMenu() // Recover the main menu
        adapter.notifyDataSetChanged()
    }

    private fun toggleSelection(position: Int) {
        if (selectedItems.contains(position)) {
            selectedItems.remove(position)
        } else {
            selectedItems.add(position)
        }
        adapter.notifyItemChanged(position)
        actionMode?.title = selectedItems.size.toString()
        if (selectedItems.isEmpty()) {
            exitMultiSelectMode()
        }
    }

    private val actionModeCallback = object : androidx.appcompat.view.ActionMode.Callback {
        override fun onCreateActionMode(mode: androidx.appcompat.view.ActionMode?, menu: Menu?): Boolean {
            mode?.menuInflater?.inflate(R.menu.menu_api_settings, menu)
            menu?.findItem(R.id.action_delete)?.isVisible = true
            return true
        }
        override fun onPrepareActionMode(mode: androidx.appcompat.view.ActionMode?, menu: Menu?) = false
        override fun onActionItemClicked(mode: androidx.appcompat.view.ActionMode?, item: MenuItem?): Boolean {
            return when (item?.itemId) {
                R.id.action_delete -> {
                    if (selectedItems.isNotEmpty()) {
                        showBatchDeleteDialog(selectedItems.sortedDescending())
                    }
                    true
                }
                else -> false
            }
        }
        override fun onDestroyActionMode(mode: androidx.appcompat.view.ActionMode?) {
            exitMultiSelectMode()
        }
    }

    private fun showBatchDeleteDialog(indices: List<Int>) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_selected)
            .setMessage(R.string.confirm_delete_selected)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                for (i in indices) {
                    if (i in apiList.indices) {
                        apiList.removeAt(i)
                    }
                }
                saveApiList()
                adapter.notifyDataSetChanged()
                exitMultiSelectMode()
                Toast.makeText(requireContext(), getString(R.string.delete_selected), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showDeleteDialog(apiIndex: Int, onDeleted: () -> Unit) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete)
            .setMessage(R.string.confirm_delete)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                if (apiIndex in apiList.indices) {
                    apiList.removeAt(apiIndex)
                    saveApiList()
                    adapter.notifyDataSetChanged()
                }
                onDeleted()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (!isMultiSelectMode) {
            super.onCreateOptionsMenu(menu, inflater)
            inflater.inflate(R.menu.menu_api_settings, menu)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_delete -> {
                val index = selectedIndex ?: 0
                if (apiList.isNotEmpty() && index in apiList.indices) {
                    showDeleteDialog(index) {
                        selectedIndex = null
                    }
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadApiList() {
        apiList.clear()
        apiList.addAll(repository.loadApiList())
    }

    private fun saveApiList() {
        repository.saveApiList(apiList)
    }
}

