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
package dev.octoshrimpy.quik.feature.settings.about

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jakewharton.rxbinding2.view.clicks
import dev.octoshrimpy.quik.BuildConfig
import dev.octoshrimpy.quik.R
import dev.octoshrimpy.quik.common.base.QkController
import dev.octoshrimpy.quik.common.widget.PreferenceView
import dev.octoshrimpy.quik.databinding.AboutControllerBinding
import dev.octoshrimpy.quik.injection.appComponent
import io.reactivex.Observable
import javax.inject.Inject
import kotlinx.coroutines.*

class AboutController : QkController<AboutControllerBinding, AboutView, Unit, AboutPresenter>(), AboutView {

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup): AboutControllerBinding =
        AboutControllerBinding.inflate(inflater, container, false)

    @Inject override lateinit var presenter: AboutPresenter

    @Inject lateinit var changelogManager: dev.octoshrimpy.quik.manager.ChangelogManager
    @Inject lateinit var externalNavigator: dev.octoshrimpy.quik.common.ExternalNavigator
    private var changelogJob: Job? = null
    private var changelogDialog: dev.octoshrimpy.quik.feature.changelog.ChangelogDialog? = null
    private var moreClick: io.reactivex.disposables.Disposable? = null

    override fun showCurrentChangelog() {
        val host=activity ?: return
        changelogJob?.cancel()
        changelogJob=CoroutineScope(Dispatchers.Main).launch {
            val changes=changelogManager.getChangelog(currentVersionOnly = true)
            if(activity !== host || !isAttached) return@launch
            changelogDialog?.dismiss(); moreClick?.dispose()
            changelogDialog=dev.octoshrimpy.quik.feature.changelog.ChangelogDialog(host).also {
                moreClick=it.moreClicks.subscribe { externalNavigator.showChangelog() }
                it.show(changes)
            }
        }
    }

    override fun onDetach(view: View) {
        changelogJob?.cancel(); changelogDialog?.dismiss(); changelogDialog=null
        moreClick?.dispose(); moreClick=null
        super.onDetach(view)
    }

    init {
        appComponent.inject(this)
    }

    override fun onViewCreated() {
        binding.version.summary = BuildConfig.VERSION_NAME
        binding.guidance.setOnClickListener { showGuidanceDialog() }
        val host = activity ?: return
        val d = host.resources.displayMetrics.density
        val ink = dev.texto.privacy.TextoAppearance.readableText(host)
        val headingInk = dev.texto.privacy.TextoAppearance.controlColor(host)
        val sectionTitle = android.widget.TextView(host).apply {
            text = "Guidance & features"
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(headingInk)
            setPadding((16 * d).toInt(), (20 * d).toInt(), (16 * d).toInt(), (8 * d).toInt())
        }
        val guideCard = android.widget.TextView(host).apply {
            text = buildGuidanceText()
            textSize = 14f
            setLineSpacing(0f, 1.2f)
            setTextColor(ink)
            dev.texto.privacy.TextoAppearance.styleSettingsCard(this)
            setPadding((20 * d).toInt(), (16 * d).toInt(), (20 * d).toInt(), (16 * d).toInt())
            setOnClickListener { showGuidanceDialog() }
        }
        binding.preferences.addView(sectionTitle)
        binding.preferences.addView(
            guideCard,
            android.widget.LinearLayout.LayoutParams(-1, -2).apply {
                setMargins((16 * d).toInt(), (4 * d).toInt(), (16 * d).toInt(), (12 * d).toInt())
            }
        )
    }

    private fun buildGuidanceText(): String =
        "• Private Messages (Pull & hold 3s):\n" +
        "At the top of your normal inbox, pull down with one finger and hold continuously for 3 seconds. No tip is shown on the header so your private space stays discreet. If no lock or locked SMS exists yet, Texto will propose setting up Fingerprint, PIN, or Pattern.\n\n" +
        "• Archived Messages & Sticky Spam Section:\n" +
        "Long-press the top inbox title to open Archived messages. The Spam section sticks to the bottom of Archive with intelligent auto-filtering (Auto: ON/OFF) and manual sender/phrase filtering (+ Filter). Spammer threads stay archived even when new messages arrive.\n\n" +
        "• Return to Normal Messages on Leave:\n" +
        "Leaving Texto from Archive, Private Messages, or settings automatically locks your private session and returns to normal messages (Inbox) next time.\n\n" +
        "• Xiaomi Messages 100% Exclusive Theme:\n" +
        "Open Settings → Themes, cards & navigation to apply the one-tap Xiaomi Messages (HyperOS) exclusive look and launcher icon.\n\n" +
        "• One-Tap OTP Copy & Word Selection:\n" +
        "Tap the verification-code banner in a conversation header to copy 2FA codes instantly, or long-press any word inside a message bubble to copy specific text."

    private fun showGuidanceDialog() {
        val host = activity ?: return
        dev.texto.privacy.TextoDialogs.builder(host)
            .setTitle("Guidance & hidden gestures")
            .setMessage(buildGuidanceText())
            .setPositiveButton("Got it", null)
            .show()
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        presenter.bindIntents(this)
        setTitle(R.string.about_title)
        showBackButton(true)
    }

    override fun preferenceClicks(): Observable<PreferenceView> = (0 until binding.preferences.childCount)
            .map { index -> binding.preferences.getChildAt(index) }
            .mapNotNull { view -> view as? PreferenceView }
            .map { preference -> preference.clicks().map { preference } }
            .let { preferences -> Observable.merge(preferences) }

    override fun render(state: Unit) {
        // No special rendering required
    }

}