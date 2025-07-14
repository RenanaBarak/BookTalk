package com.example.booktalk.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.booktalk.R
import com.example.booktalk.model.Post
import com.squareup.picasso.Picasso

class PostAdapterFeed(
    private val posts: MutableList<Post>,
    private val onPostClick: (Post) -> Unit
) : RecyclerView.Adapter<PostAdapterFeed.PostViewHolder>() {

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvBookTitle: TextView = itemView.findViewById(R.id.tvBookTitle)
        val tvRecommendation: TextView = itemView.findViewById(R.id.tvRecommendation)
        val ivPostImage: ImageView = itemView.findViewById(R.id.ivPostImage)
        val btnEdit: TextView = itemView.findViewById(R.id.btnEdit)
        val btnDelete: TextView = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        holder.tvBookTitle.text = post.bookTitle
        holder.tvRecommendation.text = post.recommendation

        holder.btnEdit.visibility = View.GONE
        holder.btnDelete.visibility = View.GONE

        if (!post.imageUri.isNullOrEmpty()) {
            holder.ivPostImage.visibility = View.VISIBLE
            Picasso.get().load(post.imageUri).into(holder.ivPostImage)
        } else {
            holder.ivPostImage.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            onPostClick(post)
        }
    }

    override fun getItemCount(): Int = posts.size

    fun updatePosts(newPosts: List<Post>) {
        posts.clear()
        posts.addAll(newPosts)
        notifyDataSetChanged()
    }
}