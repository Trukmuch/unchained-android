package com.github.livingwithhippos.unchained.lists.view

import androidx.recyclerview.widget.DiffUtil
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.data.model.TorrentItem
import com.github.livingwithhippos.unchained.utilities.DataBindingPagingAdapter

class TorrentListPagingAdapter(listener: TorrentListListener) :
    DataBindingPagingAdapter<TorrentItem, TorrentListListener>(DiffCallback(), listener) {

    class DiffCallback : DiffUtil.ItemCallback<TorrentItem>() {
        override fun areItemsTheSame(oldItem: TorrentItem, newItem: TorrentItem): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: TorrentItem, newItem: TorrentItem): Boolean {
            return oldItem.hash == newItem.hash
        }
    }

    override fun getItemViewType(position: Int) = R.layout.item_list_torrent
}

interface TorrentListListener {
    fun onClick(item: TorrentItem)
}