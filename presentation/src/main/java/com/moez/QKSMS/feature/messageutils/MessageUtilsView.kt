package dev.octoshrimpy.quik.feature.messageutils

import dev.octoshrimpy.quik.common.base.QkViewContract
import io.reactivex.Observable
import io.reactivex.Single

interface MessageUtilsView: QkViewContract<MessageUtilsState> {
    fun protectedTools(): Boolean = false

    val autoDeduplicateClickIntent: Observable<*>
    val deduplicateClickIntent: Observable<Unit>
    val autoDeleteClickIntent: Observable<Unit>

    fun showDeduplicationConfirmationDialog(): Single<Boolean>
    fun handleDeduplicationResult(resIdString: Int)

    fun autoDeleteChanged(): Observable<Int>
    fun showAutoDeleteDialog(days: Int)
    suspend fun showAutoDeleteWarningDialog(messages: Int): Boolean
}
