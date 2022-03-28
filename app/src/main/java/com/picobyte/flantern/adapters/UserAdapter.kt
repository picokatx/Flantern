package com.picobyte.flantern.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.picobyte.flantern.R
import com.picobyte.flantern.databinding.CardUserBinding
import com.picobyte.flantern.types.Status
import com.picobyte.flantern.types.User

class UserAdapter(
    private val groupUID: String,
    private val usersUID: ArrayList<Pair<String, User>>
) :
    RecyclerView.Adapter<UserAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_user, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItems(groupUID, usersUID[position].second)
    }

    override fun getItemCount(): Int {
        return usersUID.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = CardUserBinding.bind(itemView)
        fun bindItems(groupUID: String, user: User) {
            binding.userName.text = user.name
            binding.userDesc.text = user.description
            when (user.status) {
                Status.ACTIVE.ordinal -> binding.userProfile.drawable.setTint(
                    ContextCompat.getColor(itemView.context, R.color.status_active)
                )
                Status.DONOTDISTURB.ordinal -> binding.userProfile.drawable.setTint(
                    ContextCompat.getColor(itemView.context, R.color.label_text_danger)
                )
                Status.IDLE.ordinal -> binding.userProfile.drawable.setTint(
                    ContextCompat.getColor(itemView.context, R.color.label_text_admin)
                )
                Status.SLEEP.ordinal -> binding.userProfile.drawable.setTint(
                    ContextCompat.getColor(itemView.context, R.color.body_text_secondary)
                )
            }
            //todo: load user profile
            binding.userProfile.setImageResource(R.mipmap.flantern_logo_foreground)
        }
    }
}