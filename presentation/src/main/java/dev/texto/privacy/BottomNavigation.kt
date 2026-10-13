package dev.texto.privacy

import android.app.Activity
import android.content.Context
import android.content.Intent
import dev.octoshrimpy.quik.R

object BottomNavigation {
    val labels = arrayOf("Messages", "Themes", "Contacts", "Scheduled", "Settings", "Backup & restore")
    val icons = intArrayOf(R.drawable.ic_message_black_24dp, R.drawable.ic_invert_colors_black_24dp,
        R.drawable.ic_person_black_24dp, R.drawable.ic_schedule_send_black_24dp, R.drawable.ic_settings_black_24dp, R.drawable.ic_backup_black_24dp)
    fun enabled(context: Context) = TextoAppearance.prefs(context).getBoolean("bottom_bar_visible", true)
    fun items(context: Context): List<Int> = TextoAppearance.prefs(context).getString("bottom_bar_items", "0,1").orEmpty()
        .split(',').mapNotNull { it.toIntOrNull() }.filter { it in labels.indices }.distinct().take(5)
        .ifEmpty { listOf(0, 1) }
    fun open(activity: Activity, id: Int) {
        val destination = when(id) {
            0 -> dev.octoshrimpy.quik.feature.main.MainActivity::class.java
            1 -> ThemesActivity::class.java
            2 -> ContactDirectoryActivity::class.java
            3 -> dev.octoshrimpy.quik.feature.scheduled.ScheduledActivity::class.java
            4 -> dev.octoshrimpy.quik.feature.settings.SettingsActivity::class.java
            else -> dev.octoshrimpy.quik.feature.backup.BackupActivity::class.java
        }
        activity.startActivity(Intent(activity, destination).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
    }
    fun configure(context: Context, changed: () -> Unit) {
        val selected = items(context).toMutableSet()
        val checked = BooleanArray(labels.size) { it in selected }
        TextoDialogs.builder(context).setTitle("Bottom bar · choose up to 5")
            .setMultiChoiceItems(labels, checked) { dialog, index, value ->
                if (value && selected.size >= 5) {
                    (dialog as androidx.appcompat.app.AlertDialog).listView.setItemChecked(index, false)
                    android.widget.Toast.makeText(context, "Choose up to five shortcuts", android.widget.Toast.LENGTH_SHORT).show()
                } else if(value) selected.add(index) else selected.remove(index)
            }.setPositiveButton("Save") { _, _ ->
                TextoAppearance.prefs(context).edit().putString("bottom_bar_items", selected.sorted().joinToString(","))
                    .putBoolean("bottom_bar_visible", selected.isNotEmpty()).apply()
                changed()
            }.setNegativeButton("Cancel", null).show()
    }
}
