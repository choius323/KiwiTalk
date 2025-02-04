package com.kiwi.kiwitalk.ui.home

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.kiwi.kiwitalk.R
import com.kiwi.kiwitalk.databinding.FragmentProfileSettingBinding
import com.kiwi.kiwitalk.ui.keyword.SearchKeywordViewModel
import com.kiwi.kiwitalk.ui.keyword.recyclerview.SelectedKeywordAdapter
import com.kiwi.kiwitalk.ui.setImage
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileSettingFragment : Fragment() {

    private var _binding: FragmentProfileSettingBinding? = null
    private val binding get() = _binding!!
    private val profileViewModel: ProfileViewModel by viewModels()
    private val searchKeywordViewModel: SearchKeywordViewModel by activityViewModels()
    private val errorSnackbar: () -> Unit = {
        Snackbar.make(binding.root, "뒤로가기를 실행할 수 없습니다. 앱을 종료해주세요.", Snackbar.LENGTH_SHORT)
            .show()
    }
    lateinit var selectedKeywordAdapter: SelectedKeywordAdapter
    lateinit var backPressedCallback: OnBackPressedCallback

    private val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == AppCompatActivity.RESULT_OK) {
            profileViewModel.setChatImage(
                it.data?.data?.toString() ?: return@registerForActivityResult
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProfileSettingBinding.inflate(inflater, container, false)

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewmodel = profileViewModel

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setListener()
        setAdapter()
        setViewModelObserve()

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        backPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                popBackStackWithNoSave()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, backPressedCallback)
    }

    override fun onDetach() {
        super.onDetach()

        backPressedCallback.remove()
    }

    private fun setListener() {
        binding.tvProfileChangeSelectKeyword.setOnClickListener {
            Navigation.findNavController(binding.root)
                .navigate(R.id.action_profileSettingFragment_to_searchKeywordFragment)
        }

        with(binding.toolbarProfileTitle) {
            setNavigationOnClickListener {
                popBackStackWithNoSave()
            }

            setOnMenuItemClickListener {
                try {
                    profileViewModel.setMySelectedKeyword(searchKeywordViewModel.selectedKeyword.value)
                    profileViewModel.setUpdateProfile()
                    this@ProfileSettingFragment.findNavController().popBackStack()
                    return@setOnMenuItemClickListener true
                } catch (e: Exception) {
                    Log.d("NAV_PROFILE", "setListener: $e")
                    errorSnackbar
                }
                return@setOnMenuItemClickListener false
            }
        }
        binding.btnProfileAddImage.setOnClickListener {
            activityResultLauncher.launch(Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
            })
        }
    }

    private fun setAdapter() {
        selectedKeywordAdapter = SelectedKeywordAdapter()
        binding.rvProfileSelectKeywordList.adapter = selectedKeywordAdapter
    }

    private fun setViewModelObserve() {
        searchKeywordViewModel.selectedKeyword.observe(viewLifecycleOwner) {
            selectedKeywordAdapter.submitList(it)
        }

        profileViewModel.myKeywords.value?.let { list ->
            if (searchKeywordViewModel.selectedKeyword.value == null) {
                list.forEach {
                    searchKeywordViewModel.setSelectedKeywords(it.name)
                }
            }
        }

        profileViewModel.profileImage.observe(viewLifecycleOwner) {
            setImage(binding.ivProfileImage, it)
        }
    }

    private fun popBackStackWithNoSave() {
        try {
            restoreSelectedKeywordFromCurrentUser()
            this@ProfileSettingFragment.findNavController().popBackStack()
        } catch (e: Exception) {
            Log.d("NAV_PROFILE", "setListener: $e")
            errorSnackbar
        }
    }

    private fun restoreSelectedKeywordFromCurrentUser() {
        searchKeywordViewModel.deleteAllSelectedKeyword()
        profileViewModel.myKeywords.value?.let { list ->
            list.forEach {
                searchKeywordViewModel.setSelectedKeywords(it.name)
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}