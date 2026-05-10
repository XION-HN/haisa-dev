package com.haisa.dev.ui.store

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.haisa.sdk.HaisaEnvironment
import com.haisa.sdk.model.InstallProgress
import com.haisa.sdk.model.InstallStatus
import com.haisa.sdk.model.ModuleInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

data class ModuleStoreState(
    val modules: List<ModuleInfo> = emptyList(),
    val filteredModules: List<ModuleInfo> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val installingModuleId: String? = null,
    val installProgress: Int = 0,
    val installStatus: InstallStatus = InstallStatus.IDLE,
    val errorMessage: String? = null
)

class ModuleStoreViewModel(application: Application) : AndroidViewModel(application) {

    private val haisa = HaisaEnvironment.getInstance(application)

    private val _state = MutableLiveData(ModuleStoreState())
    val state: LiveData<ModuleStoreState> = _state

    private var currentInstallJob: Job? = null

    fun loadModules() {
        viewModelScope.launch {
            updateState { copy(isLoading = true, errorMessage = null) }
            try {
                val modules = haisa.getAvailableModules()
                updateState { copy(modules = modules, filteredModules = applyFilter(modules, searchQuery), isLoading = false) }
            } catch (e: Exception) {
                updateState { copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun search(query: String) {
        updateState { copy(searchQuery = query, filteredModules = applyFilter(modules, query)) }
    }

    fun installModule(moduleId: String, version: String? = null) {
        currentInstallJob?.cancel()
        currentInstallJob = viewModelScope.launch {
            updateState { copy(installingModuleId = moduleId, installProgress = 0, installStatus = InstallStatus.DOWNLOADING) }
            try {
                haisa.installModule(moduleId, version).collect { progress ->
                    updateState {
                        copy(
                            installProgress = progress.progressPercent,
                            installStatus = progress.status,
                            errorMessage = if (progress.status == InstallStatus.ERROR) progress.message else null
                        )
                    }
                    if (progress.status == InstallStatus.FINISHED || progress.status == InstallStatus.ERROR) {
                        updateState { copy(installingModuleId = null) }
                        loadModules()
                    }
                }
            } catch (e: Exception) {
                updateState { copy(installingModuleId = null, installStatus = InstallStatus.ERROR, errorMessage = e.message) }
            }
        }
    }

    fun uninstallModule(moduleId: String, version: String?) {
        viewModelScope.launch {
            try {
                val manager = com.haisa.sdk.service.ModuleManager.getInstance(getApplication())
                manager.uninstallModule(moduleId, version)
                loadModules()
            } catch (e: Exception) {
                updateState { copy(errorMessage = e.message) }
            }
        }
    }

    fun switchVersion(moduleId: String, version: String) {
        viewModelScope.launch {
            try {
                haisa.switchModuleVersion(moduleId, version)
                loadModules()
            } catch (e: Exception) {
                updateState { copy(errorMessage = e.message) }
            }
        }
    }

    fun clearError() {
        updateState { copy(errorMessage = null) }
    }

    private fun applyFilter(modules: List<ModuleInfo>, query: String): List<ModuleInfo> {
        if (query.isBlank()) return modules
        val lowerQuery = query.lowercase()
        return modules.filter {
            it.name.lowercase().contains(lowerQuery) ||
            it.id.lowercase().contains(lowerQuery) ||
            it.description.lowercase().contains(lowerQuery)
        }
    }

    private fun updateState(transform: ModuleStoreState.() -> ModuleStoreState) {
        _state.value = _state.value?.transform() ?: ModuleStoreState().transform()
    }

    override fun onCleared() {
        super.onCleared()
        currentInstallJob?.cancel()
    }
}
