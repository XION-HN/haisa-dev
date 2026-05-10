package com.haisa.dev.ui.store

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.textfield.TextInputEditText
import com.haisa.dev.R
import com.haisa.dev.ui.store.adapter.ModuleListAdapter
import com.haisa.sdk.model.ModuleInfo

class ModuleStoreFragment : Fragment() {

    private lateinit var viewModel: ModuleStoreViewModel
    private lateinit var adapter: ModuleListAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyView: TextView
    private lateinit var searchInput: TextInputEditText

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_module_store, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[ModuleStoreViewModel::class.java]

        recyclerView = view.findViewById(R.id.module_list)
        swipeRefresh = view.findViewById(R.id.swipe_refresh)
        progressBar = view.findViewById(R.id.progress_bar)
        emptyView = view.findViewById(R.id.empty_view)
        searchInput = view.findViewById(R.id.search_input)

        setupRecyclerView()
        setupSearch()
        setupSwipeRefresh()
        observeState()

        viewModel.loadModules()
    }

    private fun setupRecyclerView() {
        adapter = ModuleListAdapter(
            onClick = { module -> navigateToDetail(module) },
            onInstallClick = { module -> viewModel.installModule(module.id, module.version) }
        )
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun setupSearch() {
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.search(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener {
            viewModel.loadModules()
        }
    }

    private fun observeState() {
        viewModel.state.observe(viewLifecycleOwner) { state ->
            swipeRefresh.isRefreshing = false
            progressBar.visibility = if (state.installingModuleId != null) View.VISIBLE else View.GONE
            progressBar.progress = state.installProgress
            adapter.submitList(state.filteredModules)
            emptyView.visibility = if (state.filteredModules.isEmpty() && !state.isLoading) View.VISIBLE else View.GONE
            view?.findViewById<ProgressBar>(R.id.loading_indicator)?.visibility =
                if (state.isLoading) View.VISIBLE else View.GONE
            state.errorMessage?.let {
                if (isAdded) {
                    com.google.android.material.snackbar.Snackbar
                        .make(requireView(), it, com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                        .show()
                    viewModel.clearError()
                }
            }
        }
    }

    private fun navigateToDetail(module: ModuleInfo) {
        val bundle = Bundle().apply {
            putString("module_id", module.id)
            putString("module_name", module.name)
            putString("module_version", module.version)
            putString("module_description", module.description)
            putInt("module_size", module.sizeInMB)
            putBoolean("module_installed", module.isInstalled)
            putString("module_installed_version", module.installedVersion)
            putStringArrayList("module_dependencies", ArrayList(module.dependencies))
        }
        findNavController().navigate(R.id.action_store_to_detail, bundle)
    }
}
