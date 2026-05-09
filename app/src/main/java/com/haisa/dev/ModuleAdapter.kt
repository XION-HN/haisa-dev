package com.haisa.dev

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.haisa.sdk.model.ModuleInfo

/**
 * 模块列表适配器
 * 
 * 展示模块商店中的所有可用模块
 */
class ModuleAdapter(
    private var modules: List<ModuleInfo>,
    private val onModuleClick: (ModuleInfo) -> Unit
) : RecyclerView.Adapter<ModuleAdapter.ModuleViewHolder>() {

    class ModuleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameText: TextView = itemView.findViewById(R.id.module_name)
        val descText: TextView = itemView.findViewById(R.id.module_description)
        val versionText: TextView = itemView.findViewById(R.id.module_version)
        val sizeText: TextView = itemView.findViewById(R.id.module_size)
        val installButton: Button = itemView.findViewById(R.id.module_action_button)
        val progressBar: ProgressBar = itemView.findViewById(R.id.module_progress)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModuleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_module, parent, false)
        return ModuleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ModuleViewHolder, position: Int) {
        val module = modules[position]
        
        holder.nameText.text = module.name
        holder.descText.text = module.description
        holder.versionText.text = "v${module.version}"
        holder.sizeText.text = "${module.sizeInMB} MB"
        
        if (module.isInstalled) {
            holder.installButton.text = "管理"
            holder.installButton.setBackgroundResource(R.drawable.bg_manage_button)
        } else {
            holder.installButton.text = "安装"
            holder.installButton.setBackgroundResource(R.drawable.bg_install_button)
        }
        
        holder.installButton.setOnClickListener {
            onModuleClick(module)
        }
    }

    override fun getItemCount(): Int = modules.size

    fun updateData(newModules: List<ModuleInfo>) {
        modules = newModules
        notifyDataSetChanged()
    }
}
