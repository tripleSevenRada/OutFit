package radim.outfit

import android.os.Parcel
import android.os.Parcelable

class ViewResultsParcel: Parcelable{

    var title: String?
    var messages: List<String?>
    var fitFileAbsPath: String?
    var parentDir: String?

    private constructor(inParcel: Parcel){
        this.title = inParcel.readString()
        val nmb = inParcel.readInt()
        val messagesLocally = mutableListOf<String?>()
        repeat(nmb){
            messagesLocally.add(inParcel.readString())
        }
        messages = messagesLocally
        fitFileAbsPath = inParcel.readString()
        parentDir = inParcel.readString()
    }

    constructor(title: String,
                messages: List<String>,
                fitFilePath: String,
                parentDir: String){
        this.title = title
        this.messages = messages
        this.fitFileAbsPath = fitFilePath
        this.parentDir = parentDir
    }

    override fun writeToParcel(p0: Parcel?, p1: Int) {
        p0?.writeString(title)
        p0?.writeInt(messages.size)
        messages.forEach { p0?.writeString(it) }
        p0?.writeString(fitFileAbsPath)
        p0?.writeString(parentDir)
    }

    override fun describeContents(): Int  = messages.size

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<ViewResultsParcel> = object : Parcelable.Creator<ViewResultsParcel> {
            override fun createFromParcel(inParcel: Parcel): ViewResultsParcel {
                return ViewResultsParcel(inParcel)
            }
            override fun newArray(size: Int): Array<ViewResultsParcel?> {
                return arrayOfNulls(size)
            }
        }
    }
}