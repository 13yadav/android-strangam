package com.strangecoder.socialmedia.presentation.ui.main.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strangecoder.socialmedia.R
import com.strangecoder.socialmedia.data.entities.ProfileUpdate
import com.strangecoder.socialmedia.data.entities.User
import com.strangecoder.socialmedia.commons.Constants.MIN_USERNAME_LENGTH
import com.strangecoder.socialmedia.commons.Event
import com.strangecoder.socialmedia.commons.Resource
import com.strangecoder.socialmedia.domain.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val repository: MainRepository,
    private val applicationContext: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) : ViewModel() {

    private val _updateProfileStatus = MutableLiveData<Event<Resource<Any>>>()
    val updateProfileStatus: LiveData<Event<Resource<Any>>> = _updateProfileStatus

    private val _getUserStatus = MutableLiveData<Event<Resource<User>>>()
    val getUserStatus: LiveData<Event<Resource<User>>> = _getUserStatus

    private val _curImageUri = MutableLiveData<Uri>()
    val curImageUri: LiveData<Uri> = _curImageUri

    fun updateProfile(profileUpdate: ProfileUpdate) {
        if (profileUpdate.username.isEmpty() || profileUpdate.bio.isEmpty()) {
            val error = applicationContext.getString(R.string.error_input_empty)
            _updateProfileStatus.postValue(Event(Resource.Error(error)))
        } else if (profileUpdate.username.length < MIN_USERNAME_LENGTH) {
            val error = applicationContext.getString(R.string.error_username_too_short)
            _updateProfileStatus.postValue(Event(Resource.Error(error)))
        } else {
            _updateProfileStatus.postValue(Event(Resource.Loading()))
            viewModelScope.launch(dispatcher) {
                val result = repository.updateProfile(profileUpdate)
                _updateProfileStatus.postValue(Event(result))
            }
        }
    }

    fun getUser(uid: String) {
        _getUserStatus.postValue(Event(Resource.Loading()))
        viewModelScope.launch(dispatcher) {
            val result = repository.getUser(uid)
            _getUserStatus.postValue(Event(result))
        }
    }

    fun setCurImageUri(uri: Uri) {
        _curImageUri.postValue(uri)
    }
}