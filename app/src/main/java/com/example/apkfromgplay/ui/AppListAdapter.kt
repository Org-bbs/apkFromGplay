package com.example.apkfromgplay.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.apkfromgplay.R
import com.example.apkfromgplay.data.model.PlayApp
import com.example.apkfromgplay.databinding.ItemAppBinding

class AppListAdapter(
    private val onDownloadClick: (PlayApp) -> Unit
) : ListAdapter<PlayApp, AppListAdapter.AppViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AppViewHolder(binding, onDownloadClick)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class AppViewHolder(
        private val binding: ItemAppBinding,
        private val onDownloadClick: (PlayApp) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PlayApp) {
            binding.titleTextView.text = item.title
            binding.developerTextView.text = binding.root.context.getString(
                R.string.developer_prefix,
                item.developer
            )
            binding.packageTextView.text = binding.root.context.getString(
                R.string.package_prefix,
                item.packageName
            )
            binding.downloadButton.setOnClickListener { onDownloadClick(item) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<PlayApp>() {
        override fun areItemsTheSame(oldItem: PlayApp, newItem: PlayApp): Boolean {
            return oldItem.packageName == newItem.packageName
        }

        override fun areContentsTheSame(oldItem: PlayApp, newItem: PlayApp): Boolean {
            return oldItem == newItem
        }
    }
}
