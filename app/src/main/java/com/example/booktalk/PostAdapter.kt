package com.example.booktalk

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

// Data class to hold profile header info
data class ProfileHeaderData(
    val name: String,
    val bio: String,
    val email: String,
    val imageUrl: String? = null
)


class PostAdapter(
    private val posts: MutableList<Post>,
    private val profileData: ProfileHeaderData,
    private val onEditClick: (Post) -> Unit,
    private val onDeleteClick: (Post) -> Unit,
    private val onEditProfileClick: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_POST = 1
    }

    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val username: TextView = itemView.findViewById(R.id.tvProfileUsername)
        val bio: TextView = itemView.findViewById(R.id.tvProfileBio)
        val email: TextView = itemView.findViewById(R.id.tvUserEmail)
        val image: ImageView = itemView.findViewById(R.id.ivProfileImage)
        val btnEdit: Button = itemView.findViewById(R.id.btnEditProfile)
    }

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.tvBookTitle)
        val recommendation: TextView = itemView.findViewById(R.id.tvRecommendation)
        val postImage: ImageView = itemView.findViewById(R.id.ivPostImage)
        val edit: TextView = itemView.findViewById(R.id.btnEdit)
        val delete: TextView = itemView.findViewById(R.id.btnDelete)
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_HEADER else VIEW_TYPE_POST
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_profile_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_post, parent, false)
            PostViewHolder(view)
        }
    }

    override fun getItemCount(): Int = posts.size + 1 // +1 for header

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HeaderViewHolder) {
            holder.username.text = profileData.name
            holder.bio.text = profileData.bio
            holder.email.text = profileData.email
            if (!profileData.imageUrl.isNullOrEmpty()) {
                Picasso.get().load(profileData.imageUrl).into(holder.image)
            }
            holder.btnEdit.setOnClickListener { onEditProfileClick() }
        } else if (holder is PostViewHolder) {
            val post = posts[position - 1] // Adjust for header
            holder.title.text = post.bookTitle
            holder.recommendation.text = post.recommendation

            if (!post.imagePath.isNullOrEmpty()) {
                holder.postImage.visibility = View.VISIBLE
                Picasso.get().load(post.imagePath).into(holder.postImage)
            } else {
                holder.postImage.visibility = View.GONE
            }

            holder.edit.visibility = View.VISIBLE
            holder.delete.visibility = View.VISIBLE
            holder.edit.setOnClickListener { onEditClick(post) }
            holder.delete.setOnClickListener { onDeleteClick(post) }
        }
    }

    fun updatePosts(newPosts: List<Post>) {
        posts.clear()
        posts.addAll(newPosts)
        notifyDataSetChanged()
    }
}
