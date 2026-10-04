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
package dev.octoshrimpy.quik.common.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import dev.octoshrimpy.quik.R
import dev.octoshrimpy.quik.common.Navigator
import dev.octoshrimpy.quik.common.util.Colors
import dev.octoshrimpy.quik.common.util.extensions.setBackgroundTint
import dev.octoshrimpy.quik.common.util.extensions.setTint
import dev.octoshrimpy.quik.databinding.AvatarViewBinding
import dev.octoshrimpy.quik.injection.appComponent
import dev.octoshrimpy.quik.model.Recipient
import dev.octoshrimpy.quik.util.GlideApp
import javax.inject.Inject

class AvatarView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    @Inject lateinit var colors: Colors
    @Inject lateinit var navigator: Navigator

    private var renderedKey: List<Any?>? = null
    private var address: String = ""
    private var lookupKey: String? = null
    private var fullName: String? = null
    private var photoUri: String? = null
    private var lastUpdated: Long? = null
    private var theme: Colors.Theme
    private var layout: AvatarViewBinding

    init {
        if (!isInEditMode) {
            appComponent.inject(this)
        }

        theme = colors.theme()

        layout = AvatarViewBinding.inflate(LayoutInflater.from(context), this)
        setBackgroundResource(R.drawable.circle)
        clipToOutline = true
    }

    /**
     * Use the contact information to display the avatar.
     */
    fun setRecipient(recipient: Recipient?) {
        address = recipient?.address.orEmpty()
        lookupKey = recipient?.contact?.lookupKey
        fullName = recipient?.contact?.name
        photoUri = recipient?.contact?.photoUri
        lastUpdated = recipient?.contact?.lastUpdate
        theme = colors.theme(recipient)
        updateView()
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        if (!isInEditMode) {
            updateView()
        }
    }

    private fun updateView() {
        val key = listOf(address, lookupKey, fullName, photoUri, lastUpdated, theme.theme, dev.texto.privacy.TextoAppearance.accent(context))
        if (renderedKey == key) return
        renderedKey = key
        // Apply theme
        val accent = dev.texto.privacy.TextoAppearance.accent(context)
        val fill = androidx.core.graphics.ColorUtils.blendARGB(accent, android.graphics.Color.WHITE, .12f)
        backgroundTintList = null
        background = android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TL_BR, intArrayOf(fill, accent)).apply { shape=android.graphics.drawable.GradientDrawable.OVAL }
        val ink=dev.texto.privacy.TextoTheme.onColor(accent)
        layout.initial.setTextColor(ink)
        layout.initial.typeface=android.graphics.Typeface.create("sans-serif-medium",android.graphics.Typeface.NORMAL)
        layout.icon.setImageDrawable(dev.texto.privacy.ModernAvatar(address,ink))
        layout.icon.setTint(ink)

        val initials = fullName
                ?.substringBefore(',')
                ?.split(" ").orEmpty()
                .filter { name -> name.isNotEmpty() }
                .map { name -> name[0] }
                .filter { initial -> initial.isLetterOrDigit() }
                .map { initial -> initial.toString() }

        if (initials.isNotEmpty()) {
            layout.initial.text = if (initials.size > 1) initials.first() + initials.last() else initials.first()
            layout.icon.visibility = GONE
        } else {
            layout.initial.text = null
            layout.icon.visibility = VISIBLE
        }

        GlideApp.with(layout.photo).clear(layout.photo)
        layout.photo.setImageDrawable(null)
        photoUri?.let { photoUri ->
            GlideApp.with(layout.photo)
                    .load(photoUri)
                    .into(layout.photo)
        }
    }
}
