package com.example.tusome.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tusome.R
import com.example.tusome.data.local.OfflineCurriculumCache
import com.example.tusome.data.model.ChatMessage
import com.example.tusome.data.model.GradeLevel
import com.example.tusome.data.model.MessageSender
import com.example.tusome.data.service.CbcAiTutorService
import com.example.tusome.ui.adapters.ChatAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.json.JSONArray
import org.json.JSONObject

class AiTutorBottomSheetDialog : BottomSheetDialogFragment() {

    private lateinit var rvChatMessages: RecyclerView
    private lateinit var etMessageInput: EditText
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var offlineCache: OfflineCurriculumCache
    private val messageList = mutableListOf<ChatMessage>()

    companion object {
        fun newInstance(initialPrompt: String? = null): AiTutorBottomSheetDialog {
            return AiTutorBottomSheetDialog().apply {
                arguments = Bundle().apply {
                    putString("INITIAL_PROMPT", initialPrompt)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        offlineCache = OfflineCurriculumCache(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.dialog_ai_tutor, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvChatMessages = view.findViewById(R.id.rvChatMessages)
        etMessageInput = view.findViewById(R.id.etMessageInput)
        val btnSendMessage = view.findViewById<View>(R.id.btnSendMessage)

        loadCachedChatHistory()

        chatAdapter = ChatAdapter(messageList)
        rvChatMessages.layoutManager = LinearLayoutManager(requireContext())
        rvChatMessages.adapter = chatAdapter

        // Initial welcome message if history is empty
        if (messageList.isEmpty()) {
            val welcomeMsg = ChatMessage(
                id = "msg_welcome",
                text = "Jambo! I am your Tusome AI Learning Assistant. Ask me any question about your CBC subjects!",
                sender = MessageSender.AI_TUTOR
            )
            chatAdapter.addMessage(welcomeMsg)
            saveChatHistoryToCache()
        }

        btnSendMessage.setOnClickListener {
            val text = etMessageInput.text.toString().trim()
            if (text.isNotBlank()) {
                sendMessage(text)
                etMessageInput.setText("")
            }
        }

        // Setup prompt suggestion chips
        setupPromptChip(view.findViewById(R.id.chipPromptCompost), "🌱 How do I build a compost heap?")
        setupPromptChip(view.findViewById(R.id.chipPromptDrip), "💧 Explain drip irrigation methods")
        setupPromptChip(view.findViewById(R.id.chipPromptPlaceValue), "🔢 Explain place value in Grade 4 Math")
        setupPromptChip(view.findViewById(R.id.chipPromptCompetencies), "🏆 What are the 7 CBC Core Competencies?")

        // Handle initial prompt if passed
        arguments?.getString("INITIAL_PROMPT")?.let { prompt ->
            if (prompt.isNotBlank()) {
                sendMessage(prompt)
            }
        }
    }

    private fun setupPromptChip(chipView: TextView, textPrompt: String) {
        chipView.setOnClickListener {
            sendMessage(textPrompt)
        }
    }

    private fun sendMessage(queryText: String) {
        val userMsg = ChatMessage(
            id = "usr_${System.currentTimeMillis()}",
            text = queryText,
            sender = MessageSender.USER
        )
        chatAdapter.addMessage(userMsg)
        saveChatHistoryToCache()
        rvChatMessages.smoothScrollToPosition(chatAdapter.itemCount - 1)

        CbcAiTutorService.generateTutorResponse(
            userQuery = queryText,
            grade = GradeLevel.GRADE_4
        ) { reply ->
            if (!isAdded) return@generateTutorResponse
            val aiMsg = ChatMessage(
                id = "ai_${System.currentTimeMillis()}",
                text = reply,
                sender = MessageSender.AI_TUTOR
            )
            chatAdapter.addMessage(aiMsg)
            saveChatHistoryToCache()
            rvChatMessages.smoothScrollToPosition(chatAdapter.itemCount - 1)
        }
    }

    private fun loadCachedChatHistory() {
        val cachedJson = offlineCache.getCachedAiTutorChatHistory() ?: return
        try {
            val array = JSONArray(cachedJson)
            messageList.clear()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val sender = if (obj.getString("sender") == MessageSender.USER.name) MessageSender.USER else MessageSender.AI_TUTOR
                messageList.add(
                    ChatMessage(
                        id = obj.getString("id"),
                        text = obj.getString("text"),
                        sender = sender,
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        } catch (_: Exception) {
        }
    }

    private fun saveChatHistoryToCache() {
        try {
            val array = JSONArray()
            messageList.forEach { msg ->
                val obj = JSONObject().apply {
                    put("id", msg.id)
                    put("text", msg.text)
                    put("sender", msg.sender.name)
                    put("timestamp", msg.timestamp)
                }
                array.put(obj)
            }
            offlineCache.cacheAiTutorChatHistory(array.toString())
        } catch (_: Exception) {
        }
    }
}