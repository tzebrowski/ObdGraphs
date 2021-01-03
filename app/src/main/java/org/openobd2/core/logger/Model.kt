package org.openobd2.core.logger

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData


class Model {

    companion object {

        @JvmStatic
        private val _text = MutableLiveData<String>().apply {
            value = ""
        }
        @JvmStatic
        val text: LiveData<String> = _text


        @JvmStatic
        private val _dashboardText = MutableLiveData<String>().apply {
            value = ""
        }

        @JvmStatic
        val dashboardText: LiveData<String> = _dashboardText

        @JvmStatic
        private val _notificationText = MutableLiveData<String>().apply {
            value = "This is notifications Fragment"
        }

        @JvmStatic
        val notificationText: LiveData<String> = _notificationText

        @JvmStatic
        fun updateHomeStatus(text: String) {
            _text.postValue(text)
        }
    }
}
