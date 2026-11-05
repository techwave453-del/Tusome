package com.example.tusome.ui.adapters

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tusome.R
import com.example.tusome.data.model.ChatMessage
import com.example.tusome.data.model.MessageSender
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors

class ChatAdapter(
    private val messages: MutableList<ChatMessage>
) : RecyclerView.Adapter<ChatAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val layoutContainer: LinearLayout = view.findViewById(R.id.layoutMessageContainer)
        val cardMessage: MaterialCardView = view.findViewById(R.id.cardMessage)
        val tvMessageBody: TextView = view.findViewById(R.id.tvMessageBody)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_message, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val msg = messages[position]
        holder.tvMessageBody.text = msg.text

        val isUser = msg.sender == MessageSender.USER
        holder.layoutContainer.gravity = if (isUser) Gravity.END else Gravity.START

        val userBgColor = MaterialColors.getColor(holder.itemView, com.google.android.material.R.attr.colorPrimaryContainer)
        val userTextColor = MaterialColors.getColor(holder.itemView, com.google.android.material.R.attr.colorOnPrimaryContainer)

        val aiBgColor = MaterialColors.getColor(holder.itemView, com.google.android.material.R.attr.colorSurfaceVariant)
        val aiTextColor = MaterialColors.getColor(holder.itemView, com.google.android.material.R.attr.colorOnSurfaceVariant)

        if (isUser) {
            holder.cardMessage.setCardBackgroundColor(userBgColor)
            holder.tvMessageBody.setTextColor(userTextColor)
        } else {
            holder.cardMessage.setCardBackgroundColor(aiBgColor)
            holder.tvMessageBody.setTextColor(aiTextColor)
        }
    }

    override fun getItemCount(): Int = messages.size

    fun addMessage(msg: ChatMessage) {
        messages.add(msg)
        notifyItemInserted(messages.size - 1)
    }
}