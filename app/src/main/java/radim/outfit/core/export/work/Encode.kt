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

// TODO temporary fix
const val DEF_SPEED_M_PER_S = 5.0F

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
        //
        //
        // Every FIT file MUST contain a 'File ID' message as the first message
        encoder.write(getFileIdMesg(track))
        encoder.write(getCourseMesg(track, filename))
        val trackIsFullyTimestamped = track.isTimestamped() && track.stats.isTimestamped()
        val distancesNonNullPoints = assignPointDistancesToNonNullPoints(track)
        val timestampsNonNullPoints = assignPointTimestampsToNonNullPoints(track, distancesNonNullPoints)
        if(distancesNonNullPoints.size != timestampsNonNullPoints.size) throw RuntimeException("sizes!")
        val timeBundle = if (trackIsFullyTimestamped) {
            TrackTimestampsBundle(
                    track.stats.startTime,
                    track.stats.totalTime.toFloat(),
                    extractPointTimestampsFromPoints(track)
            )
        }else {
            TrackTimestampsBundle(
                    System.currentTimeMillis(),
                    timestampsNonNullPoints[timestampsNonNullPoints.lastIndex].toFloat(),
                    timestampsNonNullPoints
            )
        }
        encoder.write(getLapMesg(track, timeBundle))

        if(trackIsFullyTimestamped) {
            var count = 0
            //does not contain null elements
            track.points.forEach {
                encoder.write(getRecordMesg(it,
                            distancesNonNullPoints[count],
                            timestampsNonNullPoints[count],
                        true)
                        )
                count ++
            }
        } else {
            var count = 0
            for(i in 0 until track.points.size){
                if(track.points[i] == null)continue
                encoder.write(getRecordMesg(track.points[i],
                        distancesNonNullPoints[count],
                        timestampsNonNullPoints[count],
                        false)
                )
                count ++
            }
        }
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
        val exposedPublicMessages: List<String> = publicMessages
        val exposedDebugMessages: List<String> = debugMessages
        val exposedErrorMessages: List<String> = errorMessages
        return ResultPOJO(exposedPublicMessages, exposedDebugMessages, exposedErrorMessages)
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

        lapMesg.timestamp = DateTime(trackTimestampsBundle.startTime)
        lapMesg.startTime = DateTime(trackTimestampsBundle.startTime)
        lapMesg.totalTimerTime = trackTimestampsBundle.totalTime
        lapMesg.totalElapsedTime = trackTimestampsBundle.totalTime


        lapMesg.totalDistance = track.stats.totalLength

        return lapMesg
    }

    private fun getRecordMesg(point: Location,
                              dst: Float,
                              time: Long,
                              timestamped: Boolean): RecordMesg{
        val record = RecordMesg()
        record.positionLat = point.latitude.toSemiCircles()
        record.positionLong = point.longitude.toSemiCircles()
        if(point.hasAltitude()) record.altitude = point.altitude.toFloat()
        record.distance = dst
        record.timestamp = DateTime(point.time)
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