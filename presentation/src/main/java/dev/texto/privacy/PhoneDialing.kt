package dev.texto.privacy

import android.content.Context
import android.content.Intent
import android.net.Uri

/** Dial the visible number, without guessing the user's country from their SIM or locale. */
object PhoneDialing {
    fun number(raw: String, countryCode: String = ""): String {
        val number = raw.filter { it.isDigit() || it in "+*#," }
        if (number.startsWith("+") || number.startsWith("00") || number.any { it in "*#," } ||
            number.count { it.isDigit() } < 7 || !countryCode.matches(Regex("\\+[1-9][0-9]{0,2}"))) return number
        return countryCode + number.trimStart('0')
    }
    fun intent(context: Context, raw: String): Intent = Intent(Intent.ACTION_DIAL,
        Uri.fromParts("tel", number(raw, TextoAppearance.prefs(context).getString("dial_country_code", "").orEmpty()), null))

    fun choose(context: Context, raw: String) {
        TextoDialogs.builder(context).setTitle(raw)
            .setItems(arrayOf("Open in dialer", "Copy number")) { _, which ->
                if (which == 0) open(context, raw)
                else (context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager)
                    .setPrimaryClip(android.content.ClipData.newPlainText("Phone number", raw))
            }.setNegativeButton("Cancel", null).show()
    }

    fun open(context: Context, raw: String) {
        try { context.startActivity(intent(context, raw)) }
        catch (_: android.content.ActivityNotFoundException) {
            android.widget.Toast.makeText(context, "No dialer is available", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun summary(context: Context): String {
        val code = TextoAppearance.prefs(context).getString("dial_country_code", "").orEmpty()
        return "${context.getString(dev.octoshrimpy.quik.R.string.texto_settings_dialing)}  ·  ${code.ifEmpty { "As written" }}"
    }

    fun settings(context: Context, onSaved: () -> Unit = {}) {
        val field = com.google.android.material.textfield.TextInputEditText(context).apply {
            setSingleLine(); hint = "As written, or a code such as +250"
            inputType = android.text.InputType.TYPE_CLASS_PHONE
            setText(TextoAppearance.prefs(context).getString("dial_country_code", ""))
            val padding = (24 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
        }
        val dialog = TextoDialogs.builder(context).setTitle("Dialing country code")
            .setMessage("Leave empty to keep numbers as written. An optional code replaces leading local zeros. International numbers and short service codes are kept.")
            .setView(field).setNegativeButton("Cancel", null).setPositiveButton("Save", null).create()
        dialog.setOnShowListener {
            dialog.getButton(-1).setOnClickListener {
                val value = field.text.toString().trim()
                if (value.isNotEmpty() && !value.matches(Regex("\\+[1-9][0-9]{0,2}"))) {
                    field.error = "Enter + followed by 1–3 digits, or leave empty"
                } else {
                    TextoAppearance.prefs(context).edit().putString("dial_country_code", value).apply()
                    onSaved()
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }
}
