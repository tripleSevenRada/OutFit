package radim.outfit

import android.os.Parcel
import android.os.Parcelable
import android.text.SpannableString
import android.text.TextUtils

class ViewResultsParcel : Parcelable {

    var title: String
    var messages: List<SpannableString>
    var buffer: Array<String> // circular buffer of last buffer.size exports. May contain empty strings or
    // duplicates both in terms of filenames and complete paths
    var fileNameToCourseName: Map<String, String>

    private constructor(inParcel: Parcel) {
        this.title = inParcel.readString()?: ""
        val nmb = inParcel.readInt()
        val messagesLocally = mutableListOf<SpannableString>()
        repeat(nmb) {
            val mSpannableString: SpannableString = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(inParcel) as SpannableString
            messagesLocally.add(mSpannableString)
        }
        messages = messagesLocally
        val bufferSize = inParcel.readInt()
        val buffer = Array(bufferSize){""}
        for(i in buffer.indices){
            buffer[i] = inParcel.readString()?: ""
        }
        this.buffer = buffer
        val fileToCourseNameSize = inParcel.readInt()
        val fileToCourseName = mutableMapOf<String, String>()
        repeat(fileToCourseNameSize){
            fileToCourseName[inParcel.readString()?:""] = inParcel.readString()?:""
        }
        this.fileNameToCourseName = fileToCourseName
    }

    constructor(title: String,
                messages: List<SpannableString>,
                buffer: Array<String>,
                fileToCourseName: Map<String, String>) {
        this.title = title
        this.messages = messages
        this.buffer = buffer
        this.fileNameToCourseName = fileToCourseName
    }

    override fun writeToParcel(p0: Parcel?, p1: Int) {
        p0?.writeString(title)
        p0?.writeInt(messages.size)
        if(p0 != null) {
            messages.forEach { TextUtils.writeToParcel(it, p0, p1) }
        }
        p0?.writeInt(buffer.size)
        for  (i in buffer.indices){
            p0?.writeString(buffer[i])
        }
        p0?.writeInt(fileNameToCourseName.entries.size)
        fileNameToCourseName.entries.forEach {
            p0?.writeString(it.key)
            p0?.writeString(it.value)
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