package radim.outfit.core.export.logic

import android.text.SpannableString
import java.io.File

sealed class Result{
    data class Success(
            val publicMessage: List<SpannableString>,
            val debugMessage: List<String>,
            val fileDir: File,
            val filename: String,
            val coursename: String
    ): Result()
    data class Fail(
            val debugMessage: List<String>,
            val errorMessage: List<String>,
            val fileDir: File? = null,
            val filename: String? = null,
            val coursename: String? = null,
            val exception: Throwable = RuntimeException("default")
    ): Result()
}

