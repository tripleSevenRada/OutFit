package radim.outfit.core.export.logic

import java.io.File

data class ResultPOJO(val publicMessage: List<String>,
                      val debugMessage: List<String>,
                      val errorMessage: List<String>,
                      val logFileDir: File = File("/"),
                      val filename: String = ""
)
