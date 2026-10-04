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
package dev.octoshrimpy.quik.feature.changelog

import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import dev.octoshrimpy.quik.BuildConfig
import dev.octoshrimpy.quik.R
import dev.octoshrimpy.quik.databinding.ChangelogDialogBinding
import dev.octoshrimpy.quik.feature.main.MainActivity
import dev.octoshrimpy.quik.manager.ChangelogManager
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject

class ChangelogDialog(private val activity: MainActivity) {
    val moreClicks: Subject<Unit> = PublishSubject.create()

    fun show(changelog: ChangelogManager.CumulativeChangelog) {
        val d = activity.resources.displayMetrics.density
        val content = android.widget.LinearLayout(activity).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding((24*d).toInt(), (8*d).toInt(), (24*d).toInt(), (16*d).toInt())
        }
        val foreground = if (activity.resources.configuration.uiMode and 0x30 == 0x20) android.graphics.Color.WHITE else 0xFF20232B.toInt()
        listOf("Added" to changelog.added, "Improved" to changelog.improved, "Fixed" to changelog.fixed, "Removed" to changelog.removed).forEach { (title, entries) ->
            if (entries.isNotEmpty()) {
                content.addView(android.widget.TextView(activity).apply {
                    text=title; textSize=18f; setTypeface(null, android.graphics.Typeface.BOLD)
                    setTextColor(foreground); setPadding(0,(16*d).toInt(),0,(8*d).toInt())
                })
                entries.forEach { entry -> content.addView(android.widget.TextView(activity).apply {
                    text="•  $entry"; textSize=15f; setTextColor(foreground)
                    setLineSpacing(3*d,1f); setPadding(0,0,0,(10*d).toInt())
                }) }
            }
        }
        dev.texto.privacy.TextoDialogs.builder(activity)
            .setTitle("What’s new in Texto ${BuildConfig.VERSION_NAME.removeSuffix("-debug")}")
            .setView(android.widget.ScrollView(activity).apply { addView(content) })
            .setPositiveButton("Got it", null)
            .setNeutralButton("All releases") { _, _ -> moreClicks.onNext(Unit) }
            .show()
    }
}
