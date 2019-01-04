package radim.outfit

import android.os.Parcel
import android.os.Parcelable

class ViewResultsParcel : Parcelable {

    var title: String
    var messages: List<String>
    var parentDir: String
    var buffer: Array<String>

    private constructor(inParcel: Parcel) {
        this.title = inParcel.readString()?: ""
        val nmb = inParcel.readInt()
        val messagesLocally = mutableListOf<String>()
        repeat(nmb) {
            messagesLocally.add(inParcel.readString()?:"")
        }
        messages = messagesLocally
        parentDir = inParcel.readString()?:""
        val bufferSize = inParcel.readInt()
        val buffer = Array(bufferSize){""}
        for(i in buffer.indices){
            buffer[i] = inParcel.readString()?: ""
        }
        this.buffer = buffer
    }

    constructor(title: String,
                messages: List<String>,
                parentDir: String,
                buffer: Array<String>) {
        this.title = title
        this.messages = messages
        this.parentDir = parentDir
        this.buffer = buffer
    }

    override fun writeToParcel(p0: Parcel?, p1: Int) {
        p0?.writeString(title)
        p0?.writeInt(messages.size)
        messages.forEach { p0?.writeString(it) }
        p0?.writeString(parentDir)
        p0?.writeInt(buffer.size)
        for  (i in buffer.indices){
            p0?.writeString(buffer[i])
        }
    }

    override fun describeContents(): Int = messages.size

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