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
import com.example.tusome.data.model.ChatMessage
import com.example.tusome.data.model.GradeLevel
import com.example.tusome.data.model.MessageSender
import com.example.tusome.data.service.CbcAiTutorService
import com.example.tusome.ui.adapters.ChatAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AiTutorBottomSheetDialog : BottomSheetDialogFragment() {

    private lateinit var rvChatMessages: RecyclerView
    private lateinit var etMessageInput: EditText
    private lateinit var chatAdapter: ChatAdapter
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

        chatAdapter = ChatAdapter(messageList)
        rvChatMessages.layoutManager = LinearLayoutManager(requireContext())
        rvChatMessages.adapter = chatAdapter

        // Initial welcome message
        if (messageList.isEmpty()) {
            chatAdapter.addMessage(
                ChatMessage(
                    id = "msg_welcome",
                    text = "Jambo! I am your Tusome AI Learning Assistant. Ask me any question about your CBC subjects!",
                    sender = MessageSender.AI_TUTOR
                )
            )
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
        chatAdapter.addMessage(
            ChatMessage(
                id = "usr_${System.currentTimeMillis()}",
                text = queryText,
                sender = MessageSender.USER
            )
        )
        rvChatMessages.smoothScrollToPosition(chatAdapter.itemCount - 1)

        CbcAiTutorService.generateTutorResponse(
            userQuery = queryText,
            grade = GradeLevel.GRADE_4
        ) { reply ->
            if (!isAdded) return@generateTutorResponse
            chatAdapter.addMessage(
                ChatMessage(
                    id = "ai_${System.currentTimeMillis()}",
                    text = reply,
                    sender = MessageSender.AI_TUTOR
                )
            )
            rvChatMessages.smoothScrollToPosition(chatAdapter.itemCount - 1)
        }
    }
}