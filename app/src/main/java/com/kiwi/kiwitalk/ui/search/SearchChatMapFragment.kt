package com.kiwi.kiwitalk.ui.search

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.Navigation
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.ktx.awaitMap
import com.google.maps.android.ktx.mapClickEvents
import com.kiwi.domain.model.Marker
import com.kiwi.domain.model.keyword.Keyword
import com.kiwi.kiwitalk.R
import com.kiwi.kiwitalk.databinding.FragmentSearchChatMapBinding
import com.kiwi.kiwitalk.model.ClusterMarker
import com.kiwi.kiwitalk.model.ClusterMarker.Companion.toClusterMarker
import com.kiwi.kiwitalk.ui.keyword.recyclerview.SelectedKeywordAdapter
import com.kiwi.kiwitalk.ui.newchat.NewChatActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SearchChatMapFragment : Fragment() {
    private var _binding: FragmentSearchChatMapBinding? = null
    val binding get() = _binding!!
    private val viewModel: SearchChatMapViewModel by viewModels()

    private lateinit var clusterManager: ClusterManager<ClusterMarker>
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var map: GoogleMap

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>
    private lateinit var bottomSheetCallback: BottomSheetBehavior.BottomSheetCallback

    private lateinit var activityResultLauncher: ActivityResultLauncher<Array<String>>
    private var permissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_search_chat_map, container, false)
        initPermissionLauncher()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.vm = viewModel
        initMap()
        initToolbar()

        viewModel.getMarkerList(37.0, 127.0)
        initBottomSheetCallBack()

        /* TODO 마커 클릭으로 바꿔야함 */
        binding.fabCreateChat.setOnClickListener {
            // newChatActivity로 바꾸는 코드로 대체해야함
//            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
//            viewModel.getPlaceInfo(Marker("messaging:-149653492", 1.0, 1.0, listOf()))
            startActivity(Intent(requireContext(), NewChatActivity::class.java))
        }

        val adapter = SelectedKeywordAdapter()
        binding.layoutMarkerInfoPreview.rvChatKeywords.adapter = adapter
        viewModel.placeChatInfo.observe(viewLifecycleOwner) {
            adapter.submitList(it.getPopularChat().keywords.map { Keyword(it, 0) }.toMutableList())
        }
    }

    @SuppressLint("MissingPermission")
    private fun initPermissionLauncher() {
        activityResultLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissionResult ->
                if (permissionResult.values.all { it }) {
                    map.uiSettings.isMyLocationButtonEnabled = true
                    map.isMyLocationEnabled = true
                    getDeviceLocation()
                } else {
                    map.uiSettings.isMyLocationButtonEnabled = false
                    map.isMyLocationEnabled = false
                    Toast.makeText(requireContext(), "권한이 필요합니다", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun initMap() {
        val mapFragment =
            childFragmentManager.findFragmentById(R.id.fragment_searchChat_map_container) as? SupportMapFragment
                ?: return
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        viewLifecycleOwner.lifecycleScope.launch {
            map = mapFragment.awaitMap()
            getDeviceLocation(permissions)
            setUpCluster()
            setupMapClickListener()
        }
        viewModel.location.observe(viewLifecycleOwner) {
            moveToLocation(it)
            viewModel.location.removeObservers(viewLifecycleOwner)
        }

    }

    private fun setUpCluster() {
        clusterManager = ClusterManager(requireContext(), map)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.markerList.collect {
                    clusterManager.addItem(it.toClusterMarker())
                    clusterManager.cluster()
                }
            }
        }

        map.setOnCameraIdleListener(clusterManager)
    }

    private fun setupMapClickListener() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                map.mapClickEvents().collectLatest {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                }
            }
        }
        clusterManager.setOnClusterItemClickListener { item ->
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            viewModel.getPlaceInfo(Marker(item.cid, item.x, item.y, item.keywords))
            false
        }
        clusterManager.setOnClusterClickListener { cluster ->
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            cluster.items.forEach {
                Log.d("SearchChatActivity", "forEach: $it")
            }
            false
        }
    }

    @SuppressLint("MissingPermission")
    private fun getDeviceLocation() {
        fusedLocationClient.lastLocation.addOnCompleteListener(requireActivity()) { task ->
            task.addOnSuccessListener { location: Location? ->
                location ?: return@addOnSuccessListener
                viewModel.setDeviceLocation(location)
            }
        }
    }

    private fun moveToLocation(location: Location?) {
        Log.d(TAG, "moveToLocation: $location")
        location ?: return
        map.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(location.latitude, location.longitude), 15f
            )
        )
    }

    private fun getDeviceLocation(permissions: Array<String>) {
        activityResultLauncher.launch(permissions)
    }

    private fun initBottomSheetCallBack() {
        bottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_DRAGGING -> {
                        binding.layoutMarkerInfoPreview.rootLayout.visibility = View.GONE
                        binding.tvDetail.visibility = View.VISIBLE
                    }
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        binding.layoutMarkerInfoPreview.rootLayout.visibility = View.VISIBLE
                        binding.tvDetail.visibility = View.GONE
                    }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        }

        bottomSheetBehavior = BottomSheetBehavior.from(binding.layoutBottomSheet)
        bottomSheetBehavior.addBottomSheetCallback(bottomSheetCallback)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    private fun initToolbar() {
        binding.searchChatMapToolbar.setNavigationOnClickListener {
            activity?.finish()
        }
        binding.searchChatMapToolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.item_action_search_keyword -> {
                    Navigation.findNavController(binding.root).navigate(
                        R.id.action_searchChatFragment_to_searchKeywordFragment,
                        bundleOf("keywords" to viewModel.keywords.value)
                    )
                    true
                }
                else -> false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
        bottomSheetBehavior.removeBottomSheetCallback(bottomSheetCallback)
    }

    companion object {
        const val TAG = "SearchChatMapFragment"
    }
}
