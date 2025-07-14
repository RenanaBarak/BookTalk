package com.example.booktalk.controller

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.booktalk.controller.PostViewModel
import com.example.booktalk.R
import com.example.booktalk.model.Post
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions

class PostsMapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var btnMyLocation: Button
    private lateinit var btnBackProfile: Button
    private lateinit var progressBarMap: View
    private val postVM: PostViewModel by activityViewModels()
    private val posts = mutableListOf<Post>()

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_posts_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnMyLocation = view.findViewById(R.id.btnMyLocation)
        btnMyLocation.setOnClickListener {
            enableMyLocation()
        }

        btnBackProfile = view.findViewById(R.id.btnBackProfile)
        btnBackProfile.setOnClickListener {
            findNavController().navigate(R.id.feedFragment)
        }

        progressBarMap = view.findViewById(R.id.progressBarMap)
        progressBarMap.visibility = View.VISIBLE


        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        postVM.posts.observe(viewLifecycleOwner) { newPosts ->
            posts.clear()
            posts.addAll(newPosts)
            Log.d("PostsMapFragment", "Loaded posts: ${posts.size}")
            if (::googleMap.isInitialized) {
                updateMapMarkers()
            }
        }

        postVM.listenToAllPosts()

        btnBackProfile.bringToFront()
        btnMyLocation.bringToFront()
    }

    override fun onMapReady(map: GoogleMap) {
        Log.d("PostsMapFragment", "onMapReady called")
        googleMap = map

        enableMyLocation()
        updateMapMarkers()

        googleMap.uiSettings.apply {
            isZoomControlsEnabled = true
            isZoomGesturesEnabled = true
        }

        googleMap.setOnInfoWindowClickListener { marker ->
            val postId = marker.tag as? String
            val clickedPost = posts.find { it.id == postId }

            if (clickedPost != null) {
                val action = PostsMapFragmentDirections
                    .actionPostsMapFragmentToPostDetailsFragment(
                        postId = clickedPost.id,
                        bookTitle = clickedPost.bookTitle,
                        recommendation = clickedPost.recommendation,
                        imageUri = clickedPost.imageUri ?: ""
                    )
                findNavController().navigate(action)
            }
        }
        progressBarMap.visibility = View.GONE
    }


    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap.isMyLocationEnabled = true
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation()
            } else {
                Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateMapMarkers() {
        googleMap.clear()
        if (posts.isEmpty()) return

        val boundsBuilder = LatLngBounds.Builder()
        var hasLocations = false

        for (post in posts) {
            val lat = post.latitude
            val lng = post.longitude
            if (lat != null && lng != null) {
                val pos = LatLng(lat, lng)
                val marker = googleMap.addMarker(
                    MarkerOptions()
                        .position(pos)
                        .title(post.bookTitle)
                )
                marker?.tag = post.id
                boundsBuilder.include(pos)
                hasLocations = true
            } else {
                Log.d("PostsMapFragment", "Skipping post without location: ${post.bookTitle}")
            }
        }

        if (hasLocations) {
            val bounds = boundsBuilder.build()
            val padding = 150
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
        } else {
            val israel = LatLng(31.0461, 34.8516)
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(israel, 7f))
            Toast.makeText(requireContext(), "No posts with location to show on the map", Toast.LENGTH_SHORT).show()

        }
    }
}