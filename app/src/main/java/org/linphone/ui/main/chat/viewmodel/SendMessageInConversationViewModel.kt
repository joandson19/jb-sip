/*
 * Copyright (c) 2010-2023 Belledonne Communications SARL.
 *
 * This file is part of linphone-android
 * (see https://www.linphone.org).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.linphone.ui.main.chat.viewmodel

import android.text.Spannable
import android.view.inputmethod.EditorInfo
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.MutableLiveData
import org.linphone.LinphoneApplication.Companion.coreContext
import org.linphone.R
import org.linphone.core.ChatMessage
import org.linphone.core.ChatRoom
import org.linphone.core.ChatRoomListenerStub
import org.linphone.core.EventLog
import org.linphone.core.tools.Log
import org.linphone.ui.GenericViewModel
import org.linphone.ui.main.chat.model.MessageModel
import org.linphone.ui.main.chat.model.ParticipantModel
import org.linphone.utils.Event
import org.linphone.utils.LinphoneUtils

class SendMessageInConversationViewModel
    @UiThread
    constructor() : GenericViewModel() {
    companion object {
        private const val TAG = "[Send Message In Conversation ViewModel]"
    }

    val textToSend = MutableLiveData<String>()

    val isEmojiPickerOpen = MutableLiveData<Boolean>()

    val isParticipantsListOpen = MutableLiveData<Boolean>()

    val participants = MutableLiveData<ArrayList<ParticipantModel>>()

    val isEditing = MutableLiveData<Boolean>()

    val isEditingMessage = MutableLiveData<Spannable>()

    val isReplying = MutableLiveData<Boolean>()

    val isReplyingTo = MutableLiveData<String>()

    val isReplyingToMessage = MutableLiveData<Spannable>()

    val isKeyboardOpen = MutableLiveData<Boolean>()

    val isCallConversation = MutableLiveData<Boolean>()

    val isComputingParticipantsList = MutableLiveData<Boolean>()

    val imeFlags = MutableLiveData<Int>()

    val requestKeyboardHidingEvent: MutableLiveData<Event<Boolean>> by lazy {
        MutableLiveData()
    }

    val emojiToAddEvent: MutableLiveData<Event<String>> by lazy {
        MutableLiveData()
    }

    val participantUsernameToAddEvent: MutableLiveData<Event<String>> by lazy {
        MutableLiveData()
    }

    val messageSentEvent: MutableLiveData<Event<ChatMessage>> by lazy {
        MutableLiveData()
    }

    lateinit var chatRoom: ChatRoom

    private var chatMessageToReplyTo: ChatMessage? = null

    private var chatMessageToEdit: ChatMessage? = null

    private var participantsListFilter = ""

    private val chatRoomListener = object : ChatRoomListenerStub() {
        @WorkerThread
        override fun onParticipantAdded(chatRoom: ChatRoom, eventLog: EventLog) {
            computeParticipantsList()
        }

        @WorkerThread
        override fun onParticipantRemoved(chatRoom: ChatRoom, eventLog: EventLog) {
            computeParticipantsList()
        }
    }

    init {
        isKeyboardOpen.value = false
        isEmojiPickerOpen.value = false
        isParticipantsListOpen.value = false
        isCallConversation.value = false
    }

    override fun onCleared() {
        super.onCleared()

        coreContext.postOnCoreThread {
            if (::chatRoom.isInitialized) {
                chatRoom.removeListener(chatRoomListener)
            }
        }
    }

    @UiThread
    fun configureChatRoom(room: ChatRoom) {
        Log.i("$TAG Chat room configured")
        chatRoom = room
        coreContext.postOnCoreThread {
            chatRoom.addListener(chatRoomListener)

            val ime = if (chatRoom.hasCapability(ChatRoom.Capabilities.Encrypted.toInt())) {
                EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
            } else {
                EditorInfo.IME_NULL
            }
            imeFlags.postValue(ime)

            computeParticipantsList()
        }
    }

    @UiThread
    fun toggleEmojiPickerVisibility() {
        isEmojiPickerOpen.value = isEmojiPickerOpen.value == false
        if (isEmojiPickerOpen.value == true) {
            requestKeyboardHidingEvent.value = Event(true)
        }
        closeParticipantsList()
    }

    @UiThread
    fun insertEmoji(emoji: String) {
        emojiToAddEvent.value = Event(emoji)
    }

    @UiThread
    fun editMessage(model: MessageModel) {
        if (isReplying.value == true) {
            cancelReply()
        }

        val newValue = model.text.value?.toString() ?: ""
        textToSend.value = newValue

        coreContext.postOnCoreThread {
            val message = model.chatMessage
            Log.i("$TAG Pending message edit [${message.messageId}]")
            chatMessageToEdit = message
            isEditingMessage.postValue(LinphoneUtils.getFormattedTextDescribingMessage(message))
            isEditing.postValue(true)
        }
    }

    @UiThread
    fun cancelEdit() {
        Log.i("$TAG Cancelling edit")
        isEditing.value = false
        chatMessageToEdit = null
        textToSend.value = ""
    }

    @UiThread
    fun replyToMessage(model: MessageModel) {
        if (isEditing.value == true) {
            cancelEdit()
        }

        coreContext.postOnCoreThread {
            val message = model.chatMessage
            Log.i("$TAG Pending reply to message [${message.messageId}]")
            chatMessageToReplyTo = message
            isReplyingTo.postValue(model.avatarModel.value?.friend?.name)
            isReplyingToMessage.postValue(LinphoneUtils.getFormattedTextDescribingMessage(message))
            isReplying.postValue(true)
        }
    }

    @UiThread
    fun cancelReply() {
        Log.i("$TAG Cancelling reply")
        isReplying.value = false
        chatMessageToReplyTo = null
    }

    @UiThread
    fun sendMessage() {
        coreContext.postOnCoreThread {
            val messageToReplyTo = chatMessageToReplyTo
            val messageToEdit = chatMessageToEdit
            val message = if (messageToReplyTo != null) {
                Log.i("$TAG Sending message as reply to [${messageToReplyTo.messageId}]")
                chatRoom.createReplyMessage(messageToReplyTo)
            } else if (messageToEdit != null) {
                chatRoom.createReplacesMessage(messageToEdit)
            } else {
                chatRoom.createEmptyMessage()
            }

            val toSend = textToSend.value.orEmpty().trim()
            if (toSend.isNotEmpty()) {
                message.addUtf8TextContent(toSend)
            }

            if (message.contents.isNotEmpty()) {
                Log.i("$TAG Sending message")
                message.send()
                messageSentEvent.postValue(Event(message))
            }

            Log.i("$TAG Message sent, re-setting defaults")
            textToSend.postValue("")
            isReplying.postValue(false)
            isEditing.postValue(false)
            isParticipantsListOpen.postValue(false)
            isEmojiPickerOpen.postValue(false)

            chatMessageToReplyTo = null
            chatMessageToEdit = null
        }
    }

    @UiThread
    fun notifyComposing(composing: Boolean) {
        coreContext.postOnCoreThread {
            if (::chatRoom.isInitialized) {
                if (composing) {
                    chatRoom.composeTextMessage()
                } else {
                    chatRoom.stopComposing()
                }
            }
        }
    }

    @UiThread
    fun openParticipantsList() {
        isParticipantsListOpen.value = true
        isEmojiPickerOpen.value = false
    }

    @UiThread
    fun closeParticipantsList() {
        isParticipantsListOpen.value = false
        coreContext.postOnCoreThread {
            participantsListFilter = ""
            computeParticipantsList()
        }
    }

    @UiThread
    fun forwardMessage(toForward: MessageModel) {
        coreContext.postOnCoreThread {
            if (::chatRoom.isInitialized) {
                val messageToForward = toForward.chatMessage
                val forwardedMessage = chatRoom.createForwardMessage(messageToForward)
                Log.i("$TAG Sending forwarded message")
                forwardedMessage.send()
                messageSentEvent.postValue(Event(forwardedMessage))

                showGreenToast(R.string.conversation_message_forwarded_toast, R.drawable.forward)
            }
        }
    }

    @WorkerThread
    fun filterParticipantsList(filter: String) {
        Log.i("$TAG Filtering participants list using user-input [$filter]")
        if (filter.isEmpty() && participantsListFilter.isNotEmpty()) {
            participantsListFilter = ""
            computeParticipantsList()
            return
        }

        if (filter.length >= participantsListFilter.length) {
            isComputingParticipantsList.postValue(true)
            participantsListFilter = filter
            val currentList = participants.value.orEmpty()
            val newList = currentList.filter {
                it.address.username.orEmpty().contains(filter) || it.avatarModel.contactName?.contains(filter) == true
            }
            participants.postValue(newList as ArrayList<ParticipantModel>)
            isComputingParticipantsList.postValue(false)
        } else {
            participantsListFilter = filter
            computeParticipantsList(filter)
        }
    }

    @WorkerThread
    private fun computeParticipantsList(filter: String = "") {
        isComputingParticipantsList.postValue(true)
        val participantsList = arrayListOf<ParticipantModel>()

        if (::chatRoom.isInitialized) {
            for (participant in chatRoom.participants) {
                val model = ParticipantModel(participant.address, onClicked = { clicked ->
                    Log.i("$TAG Clicked on participant [${clicked.sipUri}]")
                    coreContext.postOnCoreThread {
                        val username = clicked.address.username
                        if (!username.isNullOrEmpty()) {
                            participantUsernameToAddEvent.postValue(Event(username.substring(participantsListFilter.length)))
                        }
                    }
                })

                if (
                    filter.isEmpty() ||
                    participant.address.asStringUriOnly().contains(filter) ||
                    model.avatarModel.contactName?.contains(filter) == true
                ) {
                    participantsList.add(model)
                }
            }
        }

        participants.postValue(participantsList)
        isComputingParticipantsList.postValue(false)
    }
}
