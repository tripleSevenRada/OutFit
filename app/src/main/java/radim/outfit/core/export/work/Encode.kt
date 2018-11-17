package radim.outfit.core.export.work

import android.util.Log
import com.garmin.fit.*
import locus.api.objects.extra.Track
import java.io.File
import radim.outfit.core.export.logic.ResultPOJO
import java.util.*


class Encoder{

    // with help of:
    // https://github.com/gimportexportdevs/gexporter/blob/master/app/src/main/java/org/surfsite/gexporter/Gpx2Fit.java

    fun encode(track: Track, dir: File, filename: String): ResultPOJO{
        val start = System.currentTimeMillis()

        val publicMessages = mutableListOf<String>()
        val debugMessages = mutableListOf<String>()
        val errorMessages = mutableListOf<String>()

        publicMessages.add("Dir: $dir, Filename: $filename, Track: $track")
        debugMessages.add("Debug:")
        errorMessages.add("Error:")

        val outputFile = File(dir.absolutePath + File.separatorChar + filename)
        val encoder = FileEncoder(outputFile, Fit.ProtocolVersion.V2_0)
        // Every FIT file MUST contain a 'File ID' message as the first message
        encoder.write(getFileIdMesg(track))
        encoder.write(getCourseMesg(track, filename))
        encoder.close()

        Log.i("Encode","time taken: ${System.currentTimeMillis() - start}")
        val exposedPublicMessages: List<String> = publicMessages
        val exposedDebugMessages: List<String> = debugMessages
        val exposedErrorMessages: List<String> = errorMessages
        return ResultPOJO(exposedPublicMessages, exposedDebugMessages, exposedErrorMessages)
    }

    private fun getFileIdMesg(track: Track): FileIdMesg{
        val fileIdMesg = FileIdMesg()
        fileIdMesg.type = com.garmin.fit.File.COURSE
        fileIdMesg.manufacturer = Manufacturer.DYNASTREAM
        fileIdMesg.product = 12345
        fileIdMesg.serialNumber = 12345L
        fileIdMesg.number = track.points.hashCode() // Not required
        fileIdMesg.timeCreated = DateTime(Date())
        return fileIdMesg
    }

    private fun getCourseMesg(track: Track, filename: String): CourseMesg{
        val courseMesg = CourseMesg()
        courseMesg.localNum = 0
        courseMesg.name = if(track.name != null && track.name.isNotEmpty()){track.name}
            else {filename.substring(0, filename.lastIndexOf("."))}
        courseMesg.sport = Sport.GENERIC // Not required
        courseMesg.capabilities = CourseCapabilities.NAVIGATION // Not required
        return courseMesg
    }

}