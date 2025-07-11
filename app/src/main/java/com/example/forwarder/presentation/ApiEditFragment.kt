package com.example.forwarder.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.app.AlertDialog
import android.os.Build
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.annotation.RequiresApi
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.forwarder.R
import com.example.forwarder.databinding.FragmentApiEditBinding
import com.example.forwarder.domain.model.Api
import com.example.forwarder.util.UrlManager
import com.example.forwarder.domain.repository.ApiRepository
import com.google.android.material.switchmaterial.SwitchMaterial
import androidx.core.widget.doAfterTextChanged
import com.example.forwarder.presentation.adapter.ApiEditQueryAdapter
import com.example.forwarder.presentation.adapter.QueryParamTouchHelperCallback

class ApiEditFragment : Fragment() {
    private lateinit var binding: FragmentApiEditBinding
    private val viewModel: ApiViewModel by viewModels()
    private lateinit var urlManager: UrlManager
    private var api = Api()
    private lateinit var repository: ApiRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_api_edit, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Initialize UrlManager, ViewModel and Repository
        urlManager = UrlManager(requireContext()) { resId, args -> getString(resId, *args) }
        viewModel.urlManager = urlManager
        repository = ApiRepository(requireContext())

        // Check if Base URL is https only when the input field loses focus
        binding.editBaseUrl.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val url = binding.editBaseUrl.text?.toString() ?: ""
                if (url.isNotBlank() && !url.lowercase().startsWith("https://")) {
                    // Not https, show warning
                    AlertDialog.Builder(requireContext())
                        .setTitle(R.string.warning_title)
                        .setMessage(R.string.http_warning_message)
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                    viewModel.tlsVisibility.value = View.GONE
                } else if (url.lowercase().startsWith("https://")) {
                    viewModel.tlsVisibility.value = View.VISIBLE
                }
            }
        }

        // Handle TLS input visibility
        arguments?.getParcelable("api", Api::class.java)?.let { loaded ->
            api = loaded
        } ?: run {
            api = Api() // Fallback for older API levels
        }
        viewModel.setApi(api)

        // Inject dependencies into ViewModel
        val indexArg = arguments?.getInt("api_index", -1) ?: -1
        viewModel.apiIndex = indexArg
        viewModel.repository = repository
        viewModel.getStringRes = { resId -> getString(resId) }
        viewModel.onSaved = { parentFragmentManager.popBackStack() }

        // Delete Parse URL button and related logic
        binding.editRemark.doAfterTextChanged {
            viewModel.remarkError.value = null
        }

        // HTTP Method Spinner binding
        val methodAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.http_methods,
            android.R.layout.simple_spinner_item
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        binding.spinnerMethod.adapter = methodAdapter
        // Set spinner selection from ViewModel
        val methodList = resources.getStringArray(R.array.http_methods)
        binding.spinnerMethod.setSelection(methodList.indexOf(viewModel.method.value ?: "POST"))
        // Update ViewModel when spinner changes
        binding.spinnerMethod.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                viewModel.method.value = methodList[position]
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        viewModel.method.observe(viewLifecycleOwner) { value ->
            val idx = methodList.indexOf(value ?: "POST")
            if (binding.spinnerMethod.selectedItemPosition != idx) {
                binding.spinnerMethod.setSelection(idx)
            }
        }

        // Query param RecyclerView initialization
        val queryAdapter = ApiEditQueryAdapter(
            viewModel.queryList.value ?: mutableListOf(),
            onDelete = { idx ->
                viewModel.removeQueryParam(idx)
                binding.recyclerQueryParams.adapter?.notifyItemRemoved(idx)
            },
            onStartDrag = { vh ->
                (binding.recyclerQueryParams.itemAnimator as? androidx.recyclerview.widget.SimpleItemAnimator)?.supportsChangeAnimations = false
                (binding.recyclerQueryParams.getTag(R.id.recycler_query_params) as? ItemTouchHelper)?.startDrag(vh)
            }
        )
        val itemTouchHelper = ItemTouchHelper(QueryParamTouchHelperCallback(queryAdapter))
        binding.recyclerQueryParams.setTag(R.id.recycler_query_params, itemTouchHelper)
        itemTouchHelper.attachToRecyclerView(binding.recyclerQueryParams)
        binding.recyclerQueryParams.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = queryAdapter
        }
        // Observe data changes and refresh
        viewModel.queryList.observe(viewLifecycleOwner) {
            queryAdapter.notifyDataSetChanged()
        }
        binding.btnAddQueryParam.setOnClickListener {
            viewModel.addQueryParam()
            queryAdapter.notifyItemInserted((viewModel.queryList.value?.size ?: 1) - 1)
        }

        // Setup toolbar menu with delete action
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_api_edit, menu)
                menu.findItem(R.id.action_delete)?.isVisible = (arguments?.getInt("api_index", -1) ?: -1) >= 0
            }
            override fun onMenuItemSelected(item: MenuItem): Boolean {
                if (item.itemId == R.id.action_delete) {
                    showDeleteDialog(arguments?.getInt("api_index", -1) ?: -1) {
                        parentFragmentManager.popBackStack()
                    }
                    return true
                } else if (item.itemId == R.id.action_view_json) {
                    // Show JSON dialog
                    val json = viewModel.repository.toJson(viewModel.toApi())
                    AlertDialog.Builder(requireContext())
                        .setTitle(getString(R.string.view_json))
                        .setMessage(json)
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                    return true
                } else if (item.itemId == R.id.action_send_example) {
                    // Show dialog to fill sender and content, call ApiRepository's testExampleMessage
                    val api = viewModel.toApi()
                    repository.testExampleMessage(requireContext(), api) { result ->
                        AlertDialog.Builder(requireContext())
                            .setTitle("Test Example Message")
                            .setMessage(if (result) "Message sent successfully." else "Failed to send message.")
                            .setPositiveButton(android.R.string.ok, null)
                            .show()
                    }
                    return true
                }
                return false
            }
        }, viewLifecycleOwner)
    }

    private fun showDeleteDialog(apiIndex: Int, onDeleted: () -> Unit) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete)
            .setMessage(R.string.confirm_delete)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val list = repository.loadApiList()
                if (apiIndex in list.indices) {
                    list.removeAt(apiIndex)
                    repository.saveApiList(list)
                }
                onDeleted()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // No need to nullify binding as it's now a lateinit var
    }
}

