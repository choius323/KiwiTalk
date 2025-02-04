package com.kiwi.kiwitalk.ui.newchat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.kiwi.domain.model.NewChatInfo
import com.kiwi.domain.repository.NewChatRepository
import com.kiwi.data.AppPreference
import com.kiwi.kiwitalk.util.Const.EMPTY_STRING
import com.kiwi.kiwitalk.util.Const.LOGIN_ID_KEY
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewChatViewModel @Inject constructor(
    private val repository: NewChatRepository,
    private val pref: AppPreference
) : ViewModel() {

    private val _chatImage = MutableLiveData<String>()
    val chatImage: LiveData<String> = _chatImage

    private val _chatId = MutableLiveData<String>()
    val chatId: LiveData<String> = _chatId

    private val _newChatInfo = MutableLiveData<NewChatInfo>()
    val newChatInfo: LiveData<NewChatInfo> = _newChatInfo

    private val _address = MutableLiveData<String>()
    val address: LiveData<String> = _address

    private val _latLng = MutableLiveData<LatLng>()
    val latLng: LiveData<LatLng> = _latLng


    fun setChatImage(uri: String) {
        _chatImage.value = uri
    }

    fun setAddress(address: String) {
        _address.value = address
    }

    fun setLatLng(latLng: LatLng) {
        _latLng.value = latLng
    }

    fun setNewChat(newChat: NewChatInfo) {
        _newChatInfo.value = newChat
    }

    fun setChatId() {
        _chatId.value = pref.getString(LOGIN_ID_KEY, EMPTY_STRING)
    }

    fun addNewChat(userid: String, currentTime: String, newChat: NewChatInfo) {
        viewModelScope.launch {
            repository.addChatUpload(userid,currentTime,newChat)
        }
    }
}

