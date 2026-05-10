package com.haisa.dev.ui.store

import android.os.Bundle
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
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.haisa.dev.R
import com.haisa.dev.ui.store.adapter.DependencyAdapter
import com.haisa.sdk.model.InstallStatus

class ModuleDetailFragment : Fragment() {

    private lateinit var viewModel: ModuleStoreViewModel
    private lateinit var nameText: TextView
    private lateinit var idText: TextView
    private lateinit var descriptionText: TextView
    private lateinit var versionText: TextView
    private lateinit var sizeText: TextView
    private lateinit var statusText: TextView
    private lateinit var installedVersionText: TextView
    private lateinit var installedVersionRow: View
    private lateinit var dependenciesCard: MaterialCardView
    private lateinit var dependenciesList: RecyclerView
    private lateinit var installProgress: ProgressBar
    private lateinit var actionButton: MaterialButton
    private lateinit var uninstallButton: MaterialButton
    private lateinit var chipGroup: ChipGroup

    private var moduleId: String? = null
    private var moduleVersion: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        moduleId = arguments?.getString("module_id")
        moduleVersion = arguments?.getString("module_version")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_module_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[ModuleStoreViewModel::class.java]

        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar_detail)
        toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        nameText = view.findViewById(R.id.detail_module_name)
        idText = view.findViewById(R.id.detail_module_id)
        descriptionText = view.findViewById(R.id.detail_description)
        versionText = view.findViewById(R.id.detail_version)
        sizeText = view.findViewById(R.id.detail_size)
        statusText = view.findViewById(R.id.detail_install_status)
        installedVersionText = view.findViewById(R.id.detail_installed_version)
        installedVersionRow = view.findViewById(R.id.detail_installed_version_row)
        dependenciesCard = view.findViewById(R.id.detail_dependencies_card)
        dependenciesList = view.findViewById(R.id.detail_dependencies_list)
        installProgress = view.findViewById(R.id.detail_install_progress)
        actionButton = view.findViewById(R.id.detail_action_button)
        uninstallButton = view.findViewById(R.id.detail_uninstall_button)
        chipGroup = view.findViewById(R.id.detail_category_chips)

        populateViews()
        observeState()
    }

    private fun populateViews() {
        val args = arguments ?: return

        nameText.text = args.getString("module_name", "")
        idText.text = args.getString("module_id", "")
        descriptionText.text = args.getString("module_description", "")
        versionText.text = args.getString("module_version", "")
        sizeText.text = "${args.getInt("module_size", 0)} MB"

        val isInstalled = args.getBoolean("module_installed", false)
        statusText.text = if (isInstalled) "Installed" else "Not installed"
        statusText.setTextColor(
            if (isInstalled) resources.getColor(android.R.color.holo_green_dark, null)
            else resources.getColor(android.R.color.darker_gray, null)
        )

        if (isInstalled) {
            installedVersionRow.visibility = View.VISIBLE
            installedVersionText.text = args.getString("module_installed_version", "")
            actionButton.text = "Reinstall"
            uninstallButton.visibility = View.VISIBLE
        } else {
            installedVersionRow.visibility = View.GONE
            actionButton.text = "Install Module"
            uninstallButton.visibility = View.GONE
        }

        val id = moduleId ?: ""
        val parts = id.split("-")
        if (parts.isNotEmpty()) {
            val chip = Chip(requireContext()).apply {
                text = parts.first()
                isClickable = false
            }
            chipGroup.addView(chip)
        }

        val dependencies = args.getStringArrayList("module_dependencies") ?: emptyList()
        if (dependencies.isNotEmpty()) {
            dependenciesCard.visibility = View.VISIBLE
            dependenciesList.layoutManager = LinearLayoutManager(requireContext())
            dependenciesList.adapter = DependencyAdapter(dependencies)
        } else {
            dependenciesCard.visibility = View.GONE
        }

        actionButton.setOnClickListener {
            moduleId?.let { id -> viewModel.installModule(id, moduleVersion) }
        }

        uninstallButton.setOnClickListener {
            moduleId?.let { id ->
                viewModel.uninstallModule(id, arguments?.getString("module_installed_version"))
            }
        }
    }

    private fun observeState() {
        viewModel.state.observe(viewLifecycleOwner) { state ->
            val isThisModule = state.installingModuleId == moduleId
            installProgress.visibility = if (isThisModule && state.installStatus != InstallStatus.IDLE) View.VISIBLE else View.GONE
            installProgress.progress = state.installProgress
            actionButton.isEnabled = !isThisModule

            if (isThisModule && (state.installStatus == InstallStatus.FINISHED || state.installStatus == InstallStatus.ERROR)) {
                actionButton.text = if (state.installStatus == InstallStatus.FINISHED) "Reinstall" else "Install Module"
                statusText.text = if (state.installStatus == InstallStatus.FINISHED) "Installed" else "Install failed"
                if (state.installStatus == InstallStatus.FINISHED) {
                    uninstallButton.visibility = View.VISIBLE
                    installedVersionRow.visibility = View.VISIBLE
                    installedVersionText.text = moduleVersion
                }
            }

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
}
