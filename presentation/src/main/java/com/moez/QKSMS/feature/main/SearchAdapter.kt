/*
 * Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
 *
 * This file is part of QKSMS.
 *
 * QKSMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QKSMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QKSMS.  If not, see <http://www.gnu.org/licenses/>.
 */
package dev.octoshrimpy.quik.feature.main

import android.content.Context
import android.text.SpannableString
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import dev.octoshrimpy.quik.R
import dev.octoshrimpy.quik.common.Navigator
import dev.octoshrimpy.quik.common.base.QkAdapter
import dev.octoshrimpy.quik.common.base.QkBindingViewHolder
import dev.octoshrimpy.quik.common.util.Colors
import dev.octoshrimpy.quik.common.util.DateFormatter
import dev.octoshrimpy.quik.common.util.extensions.setVisible
import dev.octoshrimpy.quik.extensions.removeAccents
import dev.octoshrimpy.quik.model.SearchResult
import dev.octoshrimpy.quik.databinding.SearchListItemBinding
import javax.inject.Inject

class SearchAdapter @Inject constructor(
    colors: Colors,
    private val context: Context,
    private val dateFormatter: DateFormatter,
    private val navigator: Navigator
) : QkAdapter<SearchResult, QkBindingViewHolder<SearchListItemBinding>>() {

    private val highlightColor: Int by lazy { colors.theme().highlight }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QkBindingViewHolder<SearchListItemBinding> {
        val binding = SearchListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return QkBindingViewHolder(binding).apply {
            itemView.setOnClickListener {
                if (adapterPosition == androidx.recyclerview.widget.RecyclerView.NO_POSITION) return@setOnClickListener
                val result = getItem(adapterPosition)
                if (result.messages == 0) navigator.showConversation(result.conversation.id)
                else dev.texto.privacy.TextoDialogs.builder(itemView.context)
                    .setTitle(result.conversation.getTitle()).setMessage(result.body)
                    .setPositiveButton("Open conversation") { _, _ -> navigator.showConversation(result.conversation.id, result.query) }
                    .setNeutralButton("Select text") { _, _ -> dev.texto.privacy.SelectMessageText.show(itemView.context, result.body) }
                    .setNegativeButton("Close", null).show()
            }
        }
    }

    override fun onBindViewHolder(holder: QkBindingViewHolder<SearchListItemBinding>, position: Int) {
        val previous = data.getOrNull(position - 1)
        val result = getItem(position)

        holder.binding.resultsHeader.setVisible(result.messages > 0 && (previous == null || previous.messages == 0))
        holder.binding.resultsTitle.text = "Matching messages · newest first (up to 500)"
        holder.itemView.background = dev.texto.privacy.TextoAppearance.card(holder.itemView.context)
        holder.binding.snippet.maxLines = 4

        val query = result.query
        val strippedQuery = query.removeAccents()
        val title = SpannableString(result.conversation.getTitle())
        val strippedTitle = title.removeAccents()
        var index = strippedTitle.indexOf(strippedQuery, ignoreCase = true)

        while (strippedQuery.isNotEmpty() && index >= 0) {
            val end = (index + strippedQuery.length).coerceAtMost(title.length)
            if (index < end) {
                title.setSpan(BackgroundColorSpan(highlightColor), index, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            index = strippedTitle.indexOf(strippedQuery, index + strippedQuery.length, true)
        }
        holder.binding.title.text = title

        holder.binding.avatars.recipients = result.conversation.recipients

        when (result.messages == 0) {
            true -> {
                holder.binding.date.setVisible(true)
                holder.binding.date.text = dateFormatter.getConversationTimestamp(result.conversation.date)
                holder.binding.snippet.text = when (result.conversation.me) {
                    true -> context.getString(R.string.main_sender_you, result.conversation.snippet)
                    false -> result.conversation.snippet
                }
            }

            false -> {
                holder.binding.date.setVisible(true)
                holder.binding.date.text = dateFormatter.getConversationTimestamp(result.date)
                val match = result.body.indexOf(query, ignoreCase = true)
                val start = (match - 60).coerceAtLeast(0)
                val excerpt = (if (start > 0) "…" else "") + result.body.substring(start).take(400)
                val highlighted = SpannableString(excerpt)
                var offset = excerpt.indexOf(query, ignoreCase = true)
                while (query.isNotEmpty() && offset >= 0) {
                    highlighted.setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), offset, offset + query.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    highlighted.setSpan(android.text.style.UnderlineSpan(), offset, offset + query.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    offset = excerpt.indexOf(query, offset + query.length, true)
                }
                holder.binding.snippet.text = highlighted
            }
        }
    }

    override fun areItemsTheSame(old: SearchResult, new: SearchResult): Boolean {
        return old.conversation.id == new.conversation.id && old.messageId == new.messageId && (old.messages > 0) == (new.messages > 0)
    }

    override fun areContentsTheSame(old: SearchResult, new: SearchResult): Boolean {
        return old.query == new.query && // Queries are the same
                old.conversation.id == new.conversation.id // Conversation id is the same
                && old.messages == new.messages && old.body == new.body && old.date == new.date
    }
}
