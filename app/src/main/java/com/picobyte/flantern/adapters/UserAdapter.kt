package com.picobyte.flantern.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.picobyte.flantern.MainActivity
import com.picobyte.flantern.R
import com.picobyte.flantern.databinding.CardUserBinding
import com.picobyte.flantern.types.SelectableType
import com.picobyte.flantern.types.Status
import com.picobyte.flantern.types.User

class UserAdapter(
    private val usersUID: ArrayList<Pair<Pair<String, String>, User>>
) :
    RecyclerView.Adapter<UserAdapter.ViewHolder>() {
    val selected: ArrayList<String> = ArrayList<String>()
    var selectable = SelectableType.NONE
    var groupUID: String? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_user, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItems(
            usersUID[position].first.second,
            usersUID[position].second,
            selectable,
            selected,
            groupUID
        )
    }

    override fun getItemCount(): Int {
        return usersUID.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = CardUserBinding.bind(itemView)
        fun bindItems(key: String, user: User, selectable: SelectableType, selected: ArrayList<String>, groupUID: String?) {
            binding.userAdmin.visibility = View.GONE
            binding.selectedIcon.visibility = View.INVISIBLE
            if (groupUID!=null) {
                (itemView.context as MainActivity).requests.isAdmin(key, groupUID) {
                    if (it) {
                        binding.userAdmin.visibility = View.VISIBLE
                    }
                }
            }
            if (selectable == SelectableType.MULTI) {
                binding.root.isClickable = true
                binding.root.setOnClickListener {
                    val idx = selected.indexOf(key)
                    if (idx == -1) {
                        binding.selectedIcon.visibility = View.VISIBLE
                        selected.add(key)
                    } else {
                        binding.selectedIcon.visibility = View.INVISIBLE
                        selected.removeAt(idx)
                    }
                }
            } else if (selectable == SelectableType.SINGLE) {
                binding.root.isClickable = true
                binding.root.setOnClickListener {
                    if (selected.size==0) {
                        selected.add(key)
                    } else {
                        selected[0] = key
                    }
                }
            }
            binding.userName.text = user.name
            binding.userDesc.text = user.description
            when (user.status) {
                Status.ACTIVE.ordinal -> {
                    binding.userStatus.setImageResource(R.drawable.active)
                }
                Status.DONOTDISTURB.ordinal -> {
                    binding.userStatus.setImageResource(R.drawable.do_not_disturb)
                }
                Status.IDLE.ordinal -> {
                    binding.userStatus.setImageResource(R.drawable.idle)
                }
                Status.SLEEP.ordinal -> {
                    binding.userStatus.setImageResource(R.drawable.offline)
                }
            }
            //todo: load user profile
            binding.userProfile.setImageResource(R.mipmap.flantern_logo_foreground)
        }
    }
}