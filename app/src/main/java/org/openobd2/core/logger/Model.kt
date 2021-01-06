package org.openobd2.core.logger

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.openobd2.core.command.CommandReply


class Model {

    companion object {

        @JvmStatic
        private val _text = MutableLiveData<String>().apply {
            value = ""
        }

        @JvmStatic
        val text: LiveData<String> = _text

        @JvmStatic
        private val _notificationText = MutableLiveData<String>().apply {
            value = "This is notifications Fragment"
        }

        @JvmStatic
        val notificationText: LiveData<String> = _notificationText

        @JvmStatic
        fun updateDebugScreen(text: String) {
            _text.postValue(text)
        }

        @JvmStatic
        private val _lveData = MutableLiveData<MutableCollection<CommandReply<*>>>().apply {
        }

        @JvmStatic
        val lveData: LiveData<MutableCollection<CommandReply<*>>> = _lveData

        @JvmStatic
        fun updateLiveData(text: MutableCollection<CommandReply<*>>) {
            _lveData.postValue(text)
        }
    }
}
