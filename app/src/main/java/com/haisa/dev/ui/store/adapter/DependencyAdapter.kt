package com.haisa.dev.ui.store.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.haisa.dev.R

class DependencyAdapter(
    private val dependencies: List<String>
) : RecyclerView.Adapter<DependencyAdapter.DepViewHolder>() {

    class DepViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        val text: TextView = itemView.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DepViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return DepViewHolder(view)
    }

    override fun onBindViewHolder(holder: DepViewHolder, position: Int) {
        holder.text.text = dependencies[position]
    }

    override fun getItemCount(): Int = dependencies.size
}
