package radim.outfit.core.export.logic

data class ResultPOJO(val publicMessage: MutableList<String>, val debugMessage: MutableList<String>, val errorMessage: MutableList<String>){

    fun addToErrorMessage(line: String) = errorMessage.add(line)
    fun addToDebugMessage(line: String) = debugMessage.add(line)
    fun addToPublicMessage(line: String) = publicMessage.add(line)

}