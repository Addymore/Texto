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