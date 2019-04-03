package radim.outfit

import android.content.Context
import android.support.v7.app.AppCompatActivity

fun initSharedPrefs(ctx: AppCompatActivity){
    val sharedPreferences = ctx.getSharedPreferences(
            ctx.getString(R.string.main_activity_preferences), Context.MODE_PRIVATE)

    with(sharedPreferences.edit()) {
        if (!sharedPreferences.contains(ctx.getString("last_seen_speed_value_m_s")))
            putFloat(ctx.getString("last_seen_speed_value_m_s"), radim.outfit.core.export.work.SPEED_DEFAULT_M_S)
        if (!sharedPreferences.contains(ctx.getString("last_seen_speed_units_string"))) {
            putString(ctx.getString("last_seen_speed_units_string"), radim.outfit.DEFAULT_UNITS_RADIO_BUTTON)
        }
        if (!sharedPreferences.contains(ctx.getString("checkbox_cciq")))
            putBoolean(ctx.getString("checkbox_cciq"), true)
        if (!sharedPreferences.contains("dialog_app_not_installed_disabled"))
            putBoolean("dialog_app_not_installed_disabled", false)
        if (!sharedPreferences.contains("dialog_app_old_version_disabled"))
            putBoolean("dialog_app_old_version_disabled", false)
        if (!sharedPreferences.contains("dialog_use_infit_like_this_disabled"))
            putBoolean("dialog_use_infit_like_this_disabled", false)
        apply()
    }
}