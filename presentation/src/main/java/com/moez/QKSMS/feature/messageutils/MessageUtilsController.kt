package dev.octoshrimpy.quik.feature.messageutils

import android.animation.ObjectAnimator
import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.jakewharton.rxbinding2.view.clicks
import dev.octoshrimpy.quik.R
import dev.octoshrimpy.quik.common.base.QkController
import dev.octoshrimpy.quik.common.util.extensions.animateLayoutChanges
import dev.octoshrimpy.quik.databinding.MessageUtilsControllerBinding
import dev.octoshrimpy.quik.feature.settings.autodelete.AutoDeleteDialog
import dev.octoshrimpy.quik.injection.appComponent
import dev.octoshrimpy.quik.repository.MessageRepository
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.resume

class MessageUtilsController : QkController<MessageUtilsControllerBinding, MessageUtilsView, MessageUtilsState, MessageUtilsPresenter>(), MessageUtilsView {

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup): MessageUtilsControllerBinding =
        MessageUtilsControllerBinding.inflate(inflater, container, false)

    override fun protectedTools() = dev.texto.privacy.PrivacyGate.protectedTools(activity)
    @Inject lateinit var context: Context
    @Inject override lateinit var presenter: MessageUtilsPresenter
    private val autoDeleteDialog: AutoDeleteDialog by lazy {
        AutoDeleteDialog(activity!!, autoDeleteSubject::onNext)
    }
    private val autoDeleteSubject: Subject<Int> = PublishSubject.create()
    override val autoDeduplicateClickIntent by lazy { binding.autoDeduplicate.clicks() }
    override val deduplicateClickIntent by lazy { binding.deduplicateMessages.clicks() }
    override val autoDeleteClickIntent by lazy { binding.autoDelete.clicks() }

    init {
        appComponent.inject(this)
        retainViewMode = RetainViewMode.RETAIN_DETACH
    }

    override fun onViewCreated() {
        super.onViewCreated()
        if(activity?.intent?.getBooleanExtra("protected_tools",false)==true) {
            binding.parent.addView(com.google.android.material.button.MaterialButton(binding.root.context).apply {
                text="Move old protected messages to bin"; isAllCaps=false
                dev.texto.privacy.TextoAppearance.styleSettingsCard(this)
                setOnClickListener {
                    if(!protectedTools()) return@setOnClickListener
                    com.google.android.material.dialog.MaterialAlertDialogBuilder(context).setTitle("Move protected messages to bin")
                        .setItems(arrayOf("Older than 30 days","Older than 60 days","Older than 90 days")) { _, index ->
                            if(!protectedTools()) return@setItems
                            val days=listOf(30,60,90)[index]
                            com.google.android.material.dialog.MaterialAlertDialogBuilder(context).setTitle("Move messages older than $days days?")
                                .setMessage("Only protected and archived numbers are included. Messages can be restored from the recycle bin until its retention period expires.")
                                .setNegativeButton("Cancel",null).setPositiveButton("Move to bin") { _,_ ->
                                    if(protectedTools()) Thread {
                                        val result=runCatching { io.realm.Realm.getDefaultInstance().use { realm ->
                                            val scope=dev.texto.privacy.ToolScope(context,realm,true)
                                            val ids=realm.where(dev.octoshrimpy.quik.model.Message::class.java).equalTo("trashedAt",0L)
                                                .lessThan("date",System.currentTimeMillis()-java.util.concurrent.TimeUnit.DAYS.toMillis(days.toLong()))
                                                .findAll().filter(scope::accepts).map { it.id }
                                            dev.texto.privacy.TrashStore(context).move(ids)
                                        } }
                                        activity?.runOnUiThread { android.widget.Toast.makeText(context,if(result.isSuccess) "Moved to recycle bin" else "Could not move messages",android.widget.Toast.LENGTH_LONG).show() }
                                    }.start()
                                }.show()
                        }.show()
                }
            },android.widget.LinearLayout.LayoutParams(-1,-2))
        }
        binding.root.postDelayed({
            binding.parent.animateLayoutChanges = true
        }, 100)
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        setTitle(if(activity?.intent?.getBooleanExtra("protected_tools",false)==true) "Protected message management" else "Message management")
        binding.autoDelete.isVisible = activity?.intent?.getBooleanExtra("protected_tools",false)!=true
        binding.autoDeduplicate.isVisible = activity?.intent?.getBooleanExtra("protected_tools",false)!=true
        showBackButton(true)
        presenter.bindIntents(this)
    }

    override fun render(state: MessageUtilsState) {
        binding.autoDeduplicate.checkbox?.isChecked = state.autoDeduplicateMessages

        val deduplicationProgress = binding.deduplicationProgress
        val progressAnimator = ObjectAnimator.ofInt(deduplicationProgress, "progress", 0, 0)

        when (state.deduplicationProgress) {
            is MessageRepository.DeduplicationProgress.Idle -> {
                deduplicationProgress.isVisible = false
            }

            is MessageRepository.DeduplicationProgress.Running -> {
                deduplicationProgress.isVisible = true
                deduplicationProgress.max = state.deduplicationProgress.max
                progressAnimator
                    .apply {
                        setIntValues(
                            deduplicationProgress.progress,
                            state.deduplicationProgress.progress
                        )
                    }
                    .start()
                deduplicationProgress.isIndeterminate =
                    state.deduplicationProgress.indeterminate
            }
        }

        binding!!.autoDelete.summary = when (state.autoDelete) {
            0 -> context.getString(R.string.settings_auto_delete_never)
            else -> context.resources.getQuantityString(
                R.plurals.settings_auto_delete_summary, state.autoDelete, state.autoDelete)
        }
    }

    override fun showDeduplicationConfirmationDialog(): Single<Boolean> = Single.create { emitter ->
        AlertDialog.Builder(activity)
            .setTitle(R.string.deduplicate_messages_title)
            .setMessage(R.string.deduplicate_message_confirmation_dialog_message)
            .setPositiveButton(R.string.button_continue) { _, _ -> emitter.onSuccess(true) }
            .setNegativeButton(R.string.button_cancel) { _, _ -> emitter.onSuccess(false) }
            .setCancelable(false)
            .show()
    }

    override fun handleDeduplicationResult(resIdString: Int) {
        binding.deduplicationProgressText.isVisible = true
        binding.deduplicationProgressText.setText(resIdString)
    }

    override fun autoDeleteChanged(): Observable<Int> = autoDeleteSubject

    override fun showAutoDeleteDialog(days: Int) = autoDeleteDialog.setExpiry(days).show()

    override suspend fun showAutoDeleteWarningDialog(messages: Int): Boolean = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { cont ->
            androidx.appcompat.app.AlertDialog.Builder(activity!!)
                .setTitle(R.string.settings_auto_delete_warning)
                .setMessage(context.resources.getString(R.string.settings_auto_delete_warning_message, messages))
                .setOnCancelListener { cont.resume(false) }
                .setNegativeButton(R.string.button_cancel) { _, _ -> cont.resume(false) }
                .setPositiveButton(R.string.button_yes) { _, _ -> cont.resume(true) }
                .show()
        }
    }
}
