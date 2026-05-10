package com.haisa.dev.ui.store.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.haisa.dev.R
import com.haisa.sdk.model.ModuleInfo

class ModuleListAdapter(
    private val onClick: (ModuleInfo) -> Unit,
    private val onInstallClick: (ModuleInfo) -> Unit
) : ListAdapter<ModuleInfo, ModuleListAdapter.ModuleViewHolder>(ModuleDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModuleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_module_card, parent, false)
        return ModuleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ModuleViewHolder, position: Int) {
        val module = getItem(position)
        holder.nameText.text = module.name
        holder.descText.text = module.description
        holder.versionText.text = "v${module.version}"
        holder.sizeText.text = "${module.sizeInMB} MB"
        holder.categoryChip.text = if (module.id.startsWith("env-")) "env" else "tool"

        if (module.dependencies.isNotEmpty()) {
            holder.depCountText.text = "${module.dependencies.size} deps"
            holder.depCountText.visibility = android.view.View.VISIBLE
        } else {
            holder.depCountText.visibility = android.view.View.GONE
        }

        if (module.isInstalled) {
            holder.actionButton.text = "Manage"
        } else {
            holder.actionButton.text = "Install"
        }

        holder.actionButton.setOnClickListener { onInstallClick(module) }
        holder.itemView.setOnClickListener { onClick(module) }
    }

    class ModuleViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        val nameText: TextView = itemView.findViewById(R.id.module_name)
        val descText: TextView = itemView.findViewById(R.id.module_description)
        val versionText: TextView = itemView.findViewById(R.id.module_version)
        val sizeText: TextView = itemView.findViewById(R.id.module_size)
        val depCountText: TextView = itemView.findViewById(R.id.module_dependencies_count)
        val categoryChip: com.google.android.material.chip.Chip = itemView.findViewById(R.id.module_category_chip)
        val actionButton: com.google.android.material.button.MaterialButton = itemView.findViewById(R.id.module_action_button)
    }

    companion object ModuleDiffCallback : DiffUtil.ItemCallback<ModuleInfo>() {
        override fun areItemsTheSame(oldItem: ModuleInfo, newItem: ModuleInfo): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ModuleInfo, newItem: ModuleInfo): Boolean {
            return oldItem == newItem
        }
    }
}
