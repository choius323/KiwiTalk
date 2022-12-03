package com.kiwi.kiwitalk.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.Navigation
import com.kiwi.kiwitalk.R
import com.kiwi.kiwitalk.databinding.FragmentChatListBinding
import com.kiwi.kiwitalk.ui.search.SearchChatActivity
import dagger.hilt.android.AndroidEntryPoint
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.models.Channel
import io.getstream.chat.android.ui.channel.list.viewmodel.ChannelListViewModel
import io.getstream.chat.android.ui.channel.list.viewmodel.factory.ChannelListViewModelFactory
import io.getstream.chat.android.ui.message.MessageListActivity
import javax.inject.Inject

@AndroidEntryPoint
class ChatListFragment : Fragment() {
    private val chatListViewModel: ChannelListViewModel
            by viewModels { ChannelListViewModelFactory() }

    @Inject
    lateinit var client: ChatClient // 임시
    private var _binding: FragmentChatListBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatListBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()
        initToolbar()

        binding.fabCreateChat.setOnClickListener {
            startActivity(Intent(requireContext(), SearchChatActivity::class.java))
        }
    }

    private fun initRecyclerView() {
        val adapter = ChatListViewAdapter()
        adapter.onClickListener = object : ChatListViewAdapter.OnChatClickListener {
            override fun onChatClick(channel: Channel) {
                startActivity(MessageListActivity.createIntent(requireContext(), channel.cid))
            }
        }

        chatListViewModel.state.observe(viewLifecycleOwner) {
            if (it.channels.isEmpty()) {
                binding.tvChatListEmpty.visibility = View.VISIBLE
                binding.rvChatList.visibility = View.INVISIBLE
            } else {
                binding.tvChatListEmpty.visibility = View.INVISIBLE
                binding.rvChatList.visibility = View.VISIBLE
                adapter.submitList(it.channels)
            }
        }
        binding.rvChatList.apply {
            this.adapter = adapter
        }
    }

    private fun initToolbar() {
        val navigation = Navigation.findNavController(binding.root)
        binding.chatListToolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.item_chatList_logout -> {
                    Toast.makeText(requireContext(), "미구현 기능입니다.", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.item_chatList_actionToProffileSetting -> {
                    navigation.navigate(R.id.action_chatListFragment_to_profileSettingFragment)
                    true
                }
                else -> false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}