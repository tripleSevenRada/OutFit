package radim.outfit.core.export.work

import android.util.Log
import com.garmin.fit.*
import locus.api.objects.extra.Location
import locus.api.objects.extra.Track
import radim.outfit.core.export.logic.Result
import radim.outfit.core.export.work.locusapiextensions.*
import radim.outfit.core.export.work.locusapiextensions.stringdumps.TrackStringDump
import radim.outfit.debugdumps.FitSDKDebugDumps.Dumps
import java.io.File
import com.garmin.fit.DateTime
import java.util.*

// we don't want the progressBar just to flick
const val MIN_TIME_TAKEN = 300
const val MILIS_FROM_START_UNIX_ERA_TO_UTC_00_00_Dec_31_1989 = 631065600000L

// TODO temporary fix
const val DEF_SPEED_M_PER_S = 5.0F

class Encoder {

    // with great help of:
    // https://github.com/gimportexportdevs/gexporter/blob/master/app/src/main/java/org/surfsite/gexporter/Gpx2Fit.java
    // https://github.com/mrihtar/Garmin-FIT

    fun encode(track: Track, dir: File, filename: String): Result {

        val debug = true

        val start = System.currentTimeMillis()

        val publicMessages = mutableListOf<String>()
        val debugMessages = mutableListOf<String>()
        val errorMessages = mutableListOf<String>()
        publicMessages.add("Dir: $dir, Filename: $filename, Track: $track")
        debugMessages.add("Debug:")
        errorMessages.add("Error:")

        val outputFile = File(dir.absolutePath + File.separatorChar + filename)
        lateinit var encoder: FileEncoder
        try {
            encoder = FileEncoder(outputFile, Fit.ProtocolVersion.V1_0)

            // The course file should, at a minimum, contain the file_id, lap, record, and course FIT messages.
            // It may optionally contain the course_point messages.
            // The file_id, course, lap, and optional course_point messages shall be defined and recorded sequentially, using only local
            // message type (i.e. 0). The file_id, and course messages need only be recorded once, at the start of the course file. At least
            // one lap message will be recorded in each course file; however multiple lap messages may be recorded if desired. Redefining
            // local message type 0 for all of these messages will ensure simple processors can handle all course data. The rest of the
            // course file will consist of multiple record messages detailing the course.

            // I do not recommend following garmin instructions for courses bundled in
            // FitSDKRelease_20.76.00.
            // These seem to be outdated to me.
            // Helpful tool: https://github.com/mrihtar/Garmin-FIT
            // Dump course exported directly from Garmin Connect and follow what you see in there

            // Every FIT file MUST contain a 'File ID' message as the first message
            val fileIdMesg = getFileIdMesg(track)
            encoder.write(fileIdMesg)

            // 'Course message'
            val courseMesg = getCourseMesg(track, filename)
            encoder.write(courseMesg)

            // expensive calls

            //================================================================================
            val trackIsFullyTimestamped = track.isTimestamped() && track.stats.isTimestamped()
            //================================================================================

            if (debug) debugMessages.add("trackIsFullyTimestamped: $trackIsFullyTimestamped")

            //================================================================================
            val trackHasAltitude = track.hasAltitude()
            //================================================================================

            if (debug) {
                debugMessages.add("trackHasAltitude: $trackHasAltitude")
                debugMessages.addAll(TrackStringDump.stringDescriptionDeep(track))
            }

            val distancesNonNullPoints = assignPointDistancesToNonNullPoints(track)
            val timestampsNonNullPoints = if (trackIsFullyTimestamped) {
                listOf()
            } else {
                assignPointTimestampsToNonNullPoints(track, distancesNonNullPoints)
            }

            if (debug) {
                debugMessages.addAll(Dumps.banner())
                debugMessages.add("++++++++++++++++++++++distancesNonNullPoints")
                distancesNonNullPoints.forEach { debugMessages.add(it.toString()) }
                debugMessages.addAll(Dumps.banner())
                debugMessages.add("++++++++++++++++++++++timestampsNonNullPoints")
                timestampsNonNullPoints.forEach { debugMessages.add(it.toString()) }
                debugMessages.addAll(Dumps.banner())
                debugMessages.add("++++++++++++++++++++++speedsNonNullPoints")
            }
            if (!trackIsFullyTimestamped &&
                    distancesNonNullPoints.size != timestampsNonNullPoints.size) {
                errorMessages.add("Sizes!")
                errorMessages.add("distancesNonNullPoints.size: ${distancesNonNullPoints.size}")
                errorMessages.add("timestampsNonNullPoints.size: ${timestampsNonNullPoints.size}")
                return Result.Fail(debugMessages, errorMessages, dir, filename)
            }
            val timeBundle = if (trackIsFullyTimestamped) {
                //track has NO null elements
                TrackTimestampsBundle(
                        track.stats.startTime,
                        track.stats.totalTime.toFloat(),
                        extractPointTimestampsFromPoints(track)
                )
            } else {
                //track may contain null elements and is considered not timestamped
                TrackTimestampsBundle(
                        System.currentTimeMillis(),
                        timestampsNonNullPoints[timestampsNonNullPoints.lastIndex].toFloat() -
                                timestampsNonNullPoints[0].toFloat(), // here not empty
                        timestampsNonNullPoints // here not empty
                )
            }

            //================================================================================
            val trackHasSpeed = track.hasSpeed()
            //================================================================================

            if (debug) debugMessages.add("trackHasSpeed: $trackHasSpeed")
            val speedsNonNullPoints = if (trackHasSpeed) {
                listOf()
            } else {
                assignSpeedsToNonNullPoints(track, timeBundle, distancesNonNullPoints)
            }

            if (debug) {
                debugMessages.addAll(Dumps.banner())
                speedsNonNullPoints.forEach { debugMessages.add(it.toString()) }
            }

            // 'Lap message'
            val lapMesg = getLapMesg(track,
                    timeBundle,
                    distancesNonNullPoints,
                    errorMessages,
                    debugMessages)

            encoder.write(lapMesg)

            if (debug) {
                debugMessages.addAll(Dumps.banner())
                debugMessages.addAll(Dumps.fileIdMessageDump(fileIdMesg))
                debugMessages.addAll(Dumps.banner())
                debugMessages.addAll(Dumps.courseMessageDump(courseMesg))
                debugMessages.addAll(Dumps.banner())
                debugMessages.addAll(Dumps.lapMessageDump(lapMesg))
            }

            /*
timestamp (253-1-UINT32): 2018-11-03T07:42:28 (910161748)
event (0-1-ENUM): timer (0)
event_group (4-1-UINT8): 0
event_type (1-1-ENUM): start (0)
             */

            val eventMesgStart = EventMesg()
            eventMesgStart.localNum = 3
            eventMesgStart.timestamp = (DateTime((timeBundle.startTime -
                    MILIS_FROM_START_UNIX_ERA_TO_UTC_00_00_Dec_31_1989) / 1000L))
            eventMesgStart.event = Event.TIMER
            eventMesgStart.eventGroup = 0.toShort()
            eventMesgStart.eventType = EventType.START
            encoder.write(eventMesgStart)

            if (debug) {
                debugMessages.addAll(Dumps.banner())
                debugMessages.addAll(Dumps.eventMessageDump(eventMesgStart))
                debugMessages.addAll(Dumps.banner())
            }

            //RECORDS START
            var index = 0
            var timestamp: DateTime? = null
            for (i in 0 until track.points.size) {
                if (track.points[i] == null) {
                    if (debug) debugMessages.add("----------------------------------------------------------------------------NULL PRESENT!")
                    continue
                }
                val recordMesg = getRecordMesg(
                        track.points[i],
                        distancesNonNullPoints,
                        timestampsNonNullPoints,
                        speedsNonNullPoints,
                        trackIsFullyTimestamped,
                        trackHasAltitude,
                        trackHasSpeed,
                        index
                )
                timestamp = recordMesg.timestamp
                if (debug) debugMessages.addAll(Dumps.recordMessageDumpLine(recordMesg))
                encoder.write(recordMesg)
                index++
            }
            // RECORDS END

            // sanity check
            if (index != distancesNonNullPoints.size ||
                    (!trackIsFullyTimestamped && index != timestampsNonNullPoints.size) ||
                    (!trackHasSpeed && index != speedsNonNullPoints.size)) {
                errorMessages.add("Sizes mismatch: Encode")
                return Result.Fail(debugMessages, errorMessages, dir, filename)
            }

            /*
timestamp (253-1-UINT32): 2018-11-03T09:27:37 (910168057)
event (0-1-ENUM): timer (0)
event_group (4-1-UINT8): 0
event_type (1-1-ENUM): stop_disable_all (9)
             */

            val eventMesgStop = EventMesg()
            eventMesgStop.localNum = 3
            eventMesgStop.timestamp = timestamp
            eventMesgStop.event = Event.TIMER
            eventMesgStop.eventGroup = 0.toShort()
            eventMesgStop.eventType = EventType.STOP_DISABLE_ALL

            encoder.write(eventMesgStop)

            if (debug) {
                debugMessages.addAll(Dumps.banner())
                debugMessages.addAll(Dumps.eventMessageDump(eventMesgStop))
            }

        } catch (e: Exception) {
            errorMessages.add(e.localizedMessage)
            return Result.Fail(debugMessages, errorMessages, dir, filename, e)
        } finally {
            try {
                encoder.close()
            } catch (e: FitRuntimeException) {
                errorMessages.add(e.localizedMessage)
                return Result.Fail(debugMessages, errorMessages, dir, filename, e)
            }
        }

        val timeTaken = System.currentTimeMillis() - start
        Log.i("Encode", "time taken: $timeTaken")
        if (timeTaken < MIN_TIME_TAKEN) {
            // TODO interupted?
            Thread.sleep(MIN_TIME_TAKEN - timeTaken)
        }
        return Result.Success(publicMessages, debugMessages, dir, filename)
    }

    private fun getFileIdMesg(track: Track): FileIdMesg {
        /*
exported course from garmin connect printed by https://github.com/mrihtar/Garmin-FIT
type (0-1-ENUM): course (6)
manufacturer (1-1-UINT16): garmin (1)
garmin_product (2-1-UINT16, original name: product): connect (65534)
time_created (4-1-UINT32): 2018-11-03T07:42:28 (910161748)
serial_number (3-1-UINT32Z): 21431572
number (5-1-UINT16): 1
        */
        val fileIdMesg = FileIdMesg()
        fileIdMesg.localNum = 0
        fileIdMesg.type = com.garmin.fit.File.COURSE
        fileIdMesg.manufacturer = Manufacturer.GARMIN
        fileIdMesg.product = 65534
        //Seconds since UTC 00:00 Dec 31 1989 If <0x10000000 = system time
        fileIdMesg.timeCreated = DateTime(Date())
        //(System.currentTimeMillis() - MILIS_FROM_START_UNIX_ERA_TO_UTC_00_00_Dec_31_1989) / 1000L

        fileIdMesg.serialNumber = 21431572
        fileIdMesg.number = 1

        return fileIdMesg
    }

    private fun getCourseMesg(track: Track, filename: String): CourseMesg {
        /*
        name (5-13-STRING): "kostelecRoad"
        sport (4-1-ENUM): cycling (2)
         */
        val courseMesg = CourseMesg()
        courseMesg.localNum = 1
        //TODO lenght?
        courseMesg.name = if (track.name != null && track.name.isNotEmpty()) {
            track.name
        } else {
            filename.substring(0, filename.lastIndexOf("."))
        }
        courseMesg.sport = Sport.GENERIC
        // courseMesg.capabilities = CourseCapabilities.NAVIGATION // Not required
        return courseMesg
    }

    private fun getLapMesg(track: Track,
                           trackTimestampsBundle: TrackTimestampsBundle,
                           dst: List<Float>,
                           errorMsg: MutableList<String>,
                           debugMsg: MutableList<String>
    ): LapMesg {
        /*
  start_time (2-1-UINT32): 2018-11-03T07:42:28 (910161748)
  timestamp (253-1-UINT32): 2018-11-03T07:42:28 (910161748)
  start_position_lat (3-1-SINT32): 49.9937300 deg (596448431)
  start_position_long (4-1-SINT32): 14.8579999 deg (177262844)
  end_position_lat (5-1-SINT32): 49.9890330 deg (596392394)
  end_position_long (6-1-SINT32): 14.8511969 deg (177181681)
  total_ascent (21-1-UINT16): 785 m (785)
  total_descent (22-1-UINT16): 766 m (766)
  swc_lat (29-1-SINT32): 49.8724400 deg (595001385)
  swc_long (30-1-SINT32): 14.7864799 deg (176409577)
  nec_lat (27-1-SINT32): 49.9937300 deg (596448431)
  nec_long (28-1-SINT32): 14.9034800 deg (177805442)
  avg_speed (13-1-UINT16): 0.000 km/h (0)
  total_elapsed_time (7-1-UINT32): 6309.142 s (6309142)
  total_timer_time (8-1-UINT32): 6309.142 s (6309142)
  total_distance (9-1-UINT32): 43805.72 m (4380572)
  message_index (254-1-UINT16): selected=0,reserved=0,mask=0 (0)
         */
        val lapMesg = LapMesg()
        lapMesg.localNum = 2

        val firstPoint = track.getFirstNonNullPoint()
        val lastPoint = track.getLastNonNullPoint()

        if (firstPoint == null || lastPoint == null) throw RuntimeException("Track has null elements only.")

        lapMesg.startTime = DateTime((trackTimestampsBundle.startTime -
                MILIS_FROM_START_UNIX_ERA_TO_UTC_00_00_Dec_31_1989) / 1000L)
        lapMesg.timestamp = DateTime((trackTimestampsBundle.startTime -
                MILIS_FROM_START_UNIX_ERA_TO_UTC_00_00_Dec_31_1989) / 1000L)

        lapMesg.startPositionLat = firstPoint.getLatitude().toSemiCircles()
        lapMesg.startPositionLong = firstPoint.getLongitude().toSemiCircles()
        lapMesg.endPositionLat = lastPoint.getLatitude().toSemiCircles()
        lapMesg.endPositionLong = lastPoint.getLongitude().toSemiCircles()

        if (track.hasAltitudeTotals()) {
            lapMesg.totalAscent = track.stats.elePositiveHeight.toInt()
            lapMesg.totalDescent = track.stats.eleNegativeHeight.toInt()
        }

        lapMesg.totalTimerTime = trackTimestampsBundle.totalTime / 1000F
        lapMesg.totalElapsedTime = trackTimestampsBundle.totalTime / 1000F
        lapMesg.totalDistance = dst[dst.lastIndex]

        if (track.hasAltitudeBounds()) {
            lapMesg.minAltitude = track.stats.altitudeMin
            lapMesg.maxAltitude = track.stats.altitudeMax
        }

        return lapMesg
    }

    // waypoint is "Point" trackpoint is "Location"
    private fun getRecordMesg(point: Location,
                              dst: List<Float>,
                              time: List<Long>,
                              speed: List<Float>,
                              fullyTimestamped: Boolean,
                              hasAltitude: Boolean,
                              hasSpeed: Boolean,
                              index: Int): RecordMesg {
        /*
  position_lat (0-1-SINT32): 49.9937300 deg (596448431)
  position_long (1-1-SINT32): 14.8579999 deg (177262844)
  timestamp (253-1-UINT32): 2018-11-03T07:42:28 (910161748)
  speed (6-1-UINT16): 0.000 km/h (0)
  distance (5-1-UINT32): 0.00 m (0)
  altitude (2-1-UINT16): 399.0 m (4495)
         */
        val record = RecordMesg()
        record.localNum = 4
        record.positionLat = point.latitude.toSemiCircles()
        record.positionLong = point.longitude.toSemiCircles()
        record.timestamp = if (fullyTimestamped) {
            // time (List) is empty
            DateTime((point.time - MILIS_FROM_START_UNIX_ERA_TO_UTC_00_00_Dec_31_1989) / 1000L)
        } else {
            // time (List) is non empty
            DateTime((time[index] - MILIS_FROM_START_UNIX_ERA_TO_UTC_00_00_Dec_31_1989) / 1000L)
        }
        record.speed = if (hasSpeed) {
            point.speed
        } else {
            speed[index]
        }
        record.distance = dst[index]
        if (hasAltitude) record.altitude = point.altitude.toFloat()
        return record
    }
}