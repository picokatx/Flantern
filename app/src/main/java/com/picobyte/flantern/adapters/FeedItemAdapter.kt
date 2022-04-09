package com.picobyte.flantern.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.picobyte.flantern.R
import com.picobyte.flantern.databinding.CardFeedItemBinding
import com.picobyte.flantern.types.getDate

data class FeedItem(val title: String, val description: String, val timestamp: Long)
class FeedItemAdapter(
    private val messagesUID: ArrayList<FeedItem>,
) :
    RecyclerView.Adapter<FeedItemAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_message, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItems(
            messagesUID[position],
        )
    }

    override fun getItemCount(): Int {
        return messagesUID.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = CardFeedItemBinding.bind(itemView)
        fun bindItems(item: FeedItem) {
            binding.nameField.text = item.title
            binding.contentField.text = item.description
            binding.timestampField.text = getDate(item.timestamp, "dd-MM-yy")
        }
    }
}
