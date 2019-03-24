package radim.outfit.core.persistence

import android.arch.persistence.room.TypeConverter
import android.text.SpannableString
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


class Converters {
    companion object {

        // to reify: make (something abstract) more concrete or real.
        private inline fun <reified T> genericType() = object: TypeToken<T>() {}.type

        @TypeConverter
        @JvmStatic
        fun fromSpannableStringList(list: List<SpannableString>): String {
            return Gson().toJson(list)
        }

        @TypeConverter
        @JvmStatic
        fun toSpannableStringList(value: String): List<SpannableString> {
            val listType = genericType<List<SpannableString>>()
            return Gson().fromJson(value, listType)
        }

        @TypeConverter
        @JvmStatic
        fun fromStringList(list: List<String>): String {
            return Gson().toJson(list)
        }

        @TypeConverter
        @JvmStatic
        fun toStringList(value: String): List<String> {
            val listType = genericType<List<String>>()
            return Gson().fromJson(value, listType)
        }

    }
}