package com.github.livingwithhippos.unchained.downloaddetails.model

import androidx.recyclerview.widget.DiffUtil
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.downloaddetails.view.DownloadDetailsListener
import com.github.livingwithhippos.unchained.utilities.DataBindingAdapter

class StreamingAdapter (listener: DownloadDetailsListener) :
    DataBindingAdapter<Pair<String,String>, DownloadDetailsListener>(
        DiffCallback(), listener
    ) {

    class DiffCallback : DiffUtil.ItemCallback<Pair<String,String>>() {
        override fun areItemsTheSame(oldItem: Pair<String,String>, newItem: Pair<String,String>): Boolean =
            oldItem.first == newItem.first

        override fun areContentsTheSame(oldItem: Pair<String,String>, newItem: Pair<String,String>): Boolean =
            oldItem.second == newItem.second
    }

    override fun getItemViewType(position: Int) = R.layout.item_streaming_download
}