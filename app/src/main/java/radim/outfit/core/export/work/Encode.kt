package radim.outfit.core.export.work

import android.util.Log
import com.garmin.fit.*
import locus.api.objects.extra.Track
import java.io.File
import radim.outfit.core.export.logic.ResultPOJO
import java.util.*
import com.garmin.fit.LapMesg
import locus.api.objects.extra.Location
import radim.outfit.core.export.work.locusapiextensions.*
import java.lang.RuntimeException

// we don't want the progressBar just to flick
const val MIN_TIME_TAKEN = 300
const val MILIS_TO_UTC_00_00_Dec_31_1989_FROM_START_OF_UNIX_ERA = 631065600000L

// TODO temporary fix
const val DEF_SPEED_M_PER_S = 5.0F

class Encoder{

    // with great help of:
    // https://github.com/gimportexportdevs/gexporter/blob/master/app/src/main/java/org/surfsite/gexporter/Gpx2Fit.java

    fun encode(track: Track, dir: File, filename: String): ResultPOJO{

        val debug = true

        val start = System.currentTimeMillis()

        val publicMessages = mutableListOf<String>()
        val debugMessages = mutableListOf<String>()
        val errorMessages = mutableListOf<String>()
        publicMessages.add("Dir: $dir, Filename: $filename, Track: $track")
        debugMessages.add("Debug:")
        errorMessages.add("Error:")

        val outputFile = File(dir.absolutePath + File.separatorChar + filename)
        val encoder = FileEncoder(outputFile, Fit.ProtocolVersion.V2_0)
        //
        //
        // Every FIT file MUST contain a 'File ID' message as the first message
        val fileIdMesg = getFileIdMesg(track) 
        encoder.write(fileIdMesg)
        val courseMesg = getCourseMesg(track, filename) 
        encoder.write(courseMesg)

        // expensive calls
        val trackIsFullyTimestamped = track.isTimestamped() && track.stats.isTimestamped()
        if (debug) debugMessages.add("trackIsFullyTimestamped: $trackIsFullyTimestamped")
        val trackHasAltitude = track.hasAltitude()
        if (debug) {
            debugMessages.add("trackHasAltitude: $trackHasAltitude")
            debugMessages.add(TrackStringDump.stringDescriptionDeep(track))
        }
        val distancesNonNullPoints = assignPointDistancesToNonNullPoints(track)
        val timestampsNonNullPoints = if(trackIsFullyTimestamped){
                listOf<Long>()
            } else {
                assignPointTimestampsToNonNullPoints(track, distancesNonNullPoints)
            }
        }
        if(debug){
            debugMessages.add("++++++++++++++++++++++distancesNonNullPoints")
            distancesNonNullPoints.forEach{debugMessages.add(it)}
            debugMessages.add("++++++++++++++++++++++timestampsNonNullPoints")
            timestampsNonNullPoints.forEach{debugMessages.add(it)}
            debugMessages.addAll(Dumps.banner())
        }
        if(distancesNonNullPoints.size != timestampsNonNullPoints.size) {
            errorMessages.add("Sizes!")
            errorMessages.add("distancesNonNullPoints.size: ${distancesNonNullPoints.size}")
            errorMessages.add("timestampsNonNullPoints.size: ${timestampsNonNullPoints.size}")
        }
        val timeBundle = if (trackIsFullyTimestamped) {
            TrackTimestampsBundle(
                    track.stats.startTime,
                    track.stats.totalTime.toFloat(),
                    extractPointTimestampsFromPoints(track)
            )
        } else {
            TrackTimestampsBundle(
                    System.currentTimeMillis(),
                    timestampsNonNullPoints[timestampsNonNullPoints.lastIndex].toFloat(), // here not empty
                    timestampsNonNullPoints // here not empty
            )
        }
        val lapMesg = getLapMesg(track, timeBundle)
        encoder.write(lapMesg)
        if(debug){
            debugMessages.addAll(Dumps.fileIdMessageDump(fileIdMesg))
            debugMessages.addAll(Dumps.courseMessageDump(courseMesg))
            debugMessages.addAll(Dumps.lapMessageDump(lapMesg))
            debugMessages.addAll(Dumps.banner())
        }

        //RECORDS START
        var index = 0
        for(i in 0 until track.points.size){
            if(track.points[i] == null) {
                if(debug)debugMessages.add("NULL PRESENT! NULL PRESENT! NULL PRESENT! NULL PRESENT! NULL PRESENT!")
                errorMessages.add("NULL PRESENT!")
                continue
            }
            val recordMesg = getRecordMesg(
                    track.points[i],
                    distancesNonNullPoints,
                    timestampsNonNullPoints,
                    trackIsFullyTimestamped,
                    trackHasAltitude,
                    index
                )
            if(debug) debugMessages.addAll(Dumps.recordMessageDump(recordMesg))
            encoder.write(recordMesg)
            index ++
        }
        // RECORDS END

        encoder.close()
        //
        //
        //
        val timeTaken = System.currentTimeMillis() - start
        Log.i("Encode","time taken: $timeTaken")
        if(timeTaken < MIN_TIME_TAKEN){
            // TODO interupted?
            Thread.sleep(MIN_TIME_TAKEN - timeTaken)
        }
        return ResultPOJO(publicMessages, debugMessages, errorMessages)
    }

    private fun getFileIdMesg(track: Track): FileIdMesg{
        val fileIdMesg = FileIdMesg()
        fileIdMesg.localNum = 0
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

    private fun getLapMesg(track: Track, trackTimestampsBundle: TrackTimestampsBundle): LapMesg{
        val lapMesg = LapMesg()
        lapMesg.localNum = 0
        val firstPoint = track.points[0]
        val lastPoint = track.points[track.points.lastIndex]

        lapMesg.startPositionLat = firstPoint.getLatitude().toSemiCircles()
        lapMesg.startPositionLong = firstPoint.getLongitude().toSemiCircles()
        lapMesg.endPositionLat = lastPoint.getLatitude().toSemiCircles()
        lapMesg.endPositionLong = lastPoint.getLongitude().toSemiCircles()

        lapMesg.timestamp = DateTime(trackTimestampsBundle.startTime - MILIS_TO_UTC_00_00_Dec_31_1989_FROM_START_OF_UNIX_ERA)
        lapMesg.startTime = DateTime(trackTimestampsBundle.startTime - MILIS_TO_UTC_00_00_Dec_31_1989_FROM_START_OF_UNIX_ERA)
        lapMesg.totalTimerTime = trackTimestampsBundle.totalTime
        lapMesg.totalElapsedTime = trackTimestampsBundle.totalTime

        lapMesg.totalDistance = track.stats.totalLength * 100 // TODO in cm too?

        return lapMesg
    }

    // waypoint is "Point" trackpoint is "Location"
    private fun getRecordMesg(point: Location,
                              dst: List<Float>,
                              time: List<Long>,
                              fullyTimestamped: Boolean,
                              hasAltitude: Boolean,
                              index: Int): RecordMesg{
        val record = RecordMesg()
        record.positionLat = point.latitude.toSemiCircles()
        record.positionLong = point.longitude.toSemiCircles()
        if(hasAltitude) record.altitude = point.altitude.toFloat()
        record.distance = dst[index] * 100 // in cm!
        record.timestamp = if (fullyTimestamped) {
            // time (List) is empty
            DateTime(point.time - MILIS_TO_UTC_00_00_Dec_31_1989_FROM_START_OF_UNIX_ERA)
        } else {
            // time (List) is non empty
            DateTime(time[index] - MILIS_TO_UTC_00_00_Dec_31_1989_FROM_START_OF_UNIX_ERA)
        } 
        return record
    }

    /*
    private fun getCoursePointMesg(track: Track): CoursePointMesg{
        val cpMesg = CoursePointMesg()
        cpMesg.localNum = 0
        return cpMesg
    }
    */

}