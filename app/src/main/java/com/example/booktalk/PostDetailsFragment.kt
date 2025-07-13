package com.example.booktalk

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import android.widget.ImageView
import com.squareup.picasso.Picasso

class PostDetailsFragment : Fragment() {

    private val args: PostDetailsFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_post_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bookTitleTextView = view.findViewById<TextView>(R.id.tvBookTitle)
        val recommendationTextView = view.findViewById<TextView>(R.id.tvRecommendation)
        val imageView = view.findViewById<ImageView>(R.id.ivPostImage)
        val btnBackToMap = view.findViewById<Button>(R.id.btnBackToMap)

        bookTitleTextView.text = args.bookTitle
        recommendationTextView.text = args.recommendation

        if (imageView != null) {
            if (!args.imageUri.isNullOrEmpty()) {
                imageView.visibility = View.VISIBLE
                Picasso.get().load(args.imageUri).into(imageView)
            } else {
                imageView.visibility = View.GONE
            }
        }

        btnBackToMap.setOnClickListener {
            findNavController().navigateUp()
        }
    }
}
