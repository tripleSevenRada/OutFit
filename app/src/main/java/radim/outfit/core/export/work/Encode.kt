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
import java.lang.reflect.InvocationTargetException
import com.garmin.fit.DateTime

// we don't want the progressBar just to flick
const val MIN_TIME_TAKEN = 300
const val MILIS_FROM_START_UNIX_ERA_TO_UTC_00_00_Dec_31_1989 = 631065600000L

// TODO temporary fix
const val DEF_SPEED_M_PER_S = 5.0F

class Encoder {

    // with great help of:
    // https://github.com/gimportexportdevs/gexporter/blob/master/app/src/main/java/org/surfsite/gexporter/Gpx2Fit.java

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
            encoder = FileEncoder(outputFile, Fit.ProtocolVersion.V2_0)

            //
            // The course file should, at a minimum, contain the file_id, lap, record, and course FIT messages.
            // It may optionally contain the course_point messages.
            // The file_id, course, lap, and optional course_point messages shall be defined and recorded sequentially, using only local
            // message type (i.e. 0). The file_id, and course messages need only be recorded once, at the start of the course file. At least
            // one lap message will be recorded in each course file; however multiple lap messages may be recorded if desired. Redefining
            // local message type 0 for all of these messages will ensure simple processors can handle all course data. The rest of the
            // course file will consist of multiple record messages detailing the course

            // Every FIT file MUST contain a 'File ID' message as the first message
            val fileIdMesg = getFileIdMesg(track)
            encoder.write(fileIdMesg)

            // 'Course message'
            val courseMesg = getCourseMesg(track, filename)
            encoder.write(courseMesg)

            // expensive calls
            val trackIsFullyTimestamped = track.isTimestamped() && track.stats.isTimestamped()
            if (debug) debugMessages.add("trackIsFullyTimestamped: $trackIsFullyTimestamped")
            val trackHasAltitude = track.hasAltitude()
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
                debugMessages.add("++++++++++++++++++++++distancesNonNullPoints")
                distancesNonNullPoints.forEach { debugMessages.add(it.toString()) }
                debugMessages.add("++++++++++++++++++++++timestampsNonNullPoints")
                timestampsNonNullPoints.forEach { debugMessages.add(it.toString()) }
                debugMessages.addAll(Dumps.banner())
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

            // 'Lap message'
            val lapMesg = getLapMesg(track,
                    timeBundle,
                    distancesNonNullPoints,
                    errorMessages,
                    debugMessages)

            encoder.write(lapMesg)

            if (debug) {
                debugMessages.addAll(Dumps.fileIdMessageDump(fileIdMesg))
                debugMessages.addAll(Dumps.courseMessageDump(courseMesg))
                debugMessages.addAll(Dumps.lapMessageDump(lapMesg))
                debugMessages.addAll(Dumps.banner())
            }

            val eventMesg = EventMesg()
            eventMesg.localNum = 0
            eventMesg.event = Event.TIMER
            eventMesg.eventType = EventType.START
            eventMesg.eventGroup = 0.toShort()
            eventMesg.timestamp = (DateTime((timeBundle.startTime -
                    MILIS_FROM_START_UNIX_ERA_TO_UTC_00_00_Dec_31_1989) / 1000L))
            encoder.write(eventMesg)

            if (debug) {
                debugMessages.addAll(Dumps.banner())
                debugMessages.addAll(Dumps.eventMessageDump(eventMesg))
            }

            //RECORDS START
            var index = 0
            var timestamp: DateTime? = null
            for (i in 0 until track.points.size) {
                if (track.points[i] == null) {
                    if (debug) debugMessages.add("NULL PRESENT!")
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
                timestamp = recordMesg.timestamp
                if (debug) debugMessages.addAll(Dumps.recordMessageDump(recordMesg))
                encoder.write(recordMesg)
                index++
            }
            // RECORDS END

            eventMesg.event = Event.TIMER
            eventMesg.eventType = EventType.STOP_DISABLE_ALL
            eventMesg.eventGroup = 0.toShort()
            eventMesg.timestamp = timestamp

            encoder.write(eventMesg)

            if (debug) {
                debugMessages.addAll(Dumps.banner())
                debugMessages.addAll(Dumps.eventMessageDump(eventMesg))
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
        val fileIdMesg = FileIdMesg()
        fileIdMesg.localNum = 0
        //file:///home/radim/fit/FitSDKRelease_20.76.00/java/doc/com/garmin/fit/File.html
        fileIdMesg.type = com.garmin.fit.File.COURSE
        //Set manufacturer field
        fileIdMesg.manufacturer = Manufacturer.DYNASTREAM
        //Set product field
        fileIdMesg.product = 12345
        //Set serial_number field
        fileIdMesg.serialNumber = 12345L
        //Set time_created field Comment: Only set for files that are can be created/erased.
        //file:///home/radim/fit/FitSDKRelease_20.76.00/java/doc/com/garmin/fit/DateTime.html
        //Seconds since UTC 00:00 Dec 31 1989 If <0x10000000 = system time
        fileIdMesg.timeCreated = DateTime((System.currentTimeMillis() -
                MILIS_FROM_START_UNIX_ERA_TO_UTC_00_00_Dec_31_1989) / 1000L)
        //Set number field Comment: Only set for files that are not created/erased.
        fileIdMesg.number = track.points.hashCode()
        return fileIdMesg
    }

    private fun getCourseMesg(track: Track, filename: String): CourseMesg {
        val courseMesg = CourseMesg()
        courseMesg.localNum = 0
        courseMesg.name = if (track.name != null && track.name.isNotEmpty()) {
            track.name
        } else {
            filename.substring(0, filename.lastIndexOf("."))
        }
        courseMesg.sport = Sport.GENERIC
        courseMesg.capabilities = CourseCapabilities.NAVIGATION // Not required
        return courseMesg
    }

    private fun getLapMesg(track: Track,
                           trackTimestampsBundle: TrackTimestampsBundle,
                           dst: List<Float>,
                           errorMsg: MutableList<String>,
                           debugMsg: MutableList<String>
    ): LapMesg {
        val lapMesg = LapMesg()
        lapMesg.localNum = 0
        val firstPoint = track.getFirstNonNullPoint()
        val lastPoint = track.getLastNonNullPoint()

        if (firstPoint == null || lastPoint == null) throw RuntimeException("Track has null elements only.")

        lapMesg.startPositionLat = firstPoint.getLatitude().toSemiCircles()
        lapMesg.startPositionLong = firstPoint.getLongitude().toSemiCircles()
        lapMesg.endPositionLat = lastPoint.getLatitude().toSemiCircles()
        lapMesg.endPositionLong = lastPoint.getLongitude().toSemiCircles()

        // Set timestamp field Units: s Comment: Lap end time.
        lapMesg.timestamp = DateTime(
                (trackTimestampsBundle.pointStamps[trackTimestampsBundle.pointStamps.lastIndex] -
                        MILIS_FROM_START_UNIX_ERA_TO_UTC_00_00_Dec_31_1989) / 1000L)
        // Set start_time field
        lapMesg.startTime = DateTime((trackTimestampsBundle.startTime -
                MILIS_FROM_START_UNIX_ERA_TO_UTC_00_00_Dec_31_1989) / 1000L)
        // Set total_timer_time field Units: s Comment: Timer Time (excludes pauses)
        lapMesg.totalTimerTime = trackTimestampsBundle.totalTime / 1000F
        // Set total_elapsed_time field Units: s Comment: Time (includes pauses)
        lapMesg.totalElapsedTime = trackTimestampsBundle.totalTime / 1000F
        //Set total_distance field Units: m
        lapMesg.totalDistance = dst[dst.lastIndex]

        // elevation
        if (track.hasAltitudeTotals()) {
            lapMesg.totalAscent = track.stats.elePositiveHeight.toInt()
            lapMesg.totalDescent = track.stats.eleNegativeHeight.toInt()
        }
        if (track.hasAltitudeBounds()) {
            lapMesg.minAltitude = track.stats.altitudeMin
            lapMesg.maxAltitude = track.stats.altitudeMax
        }

        // Add the bounding box of the course in the undocumented fields
        try {
            val c = Field::class.java.getDeclaredConstructor(String::class.java, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType,
                    Double::class.javaPrimitiveType, Double::class.javaPrimitiveType, String::class.java,
                    Boolean::class.javaPrimitiveType, Profile.Type::class.java)
            c.isAccessible = true
            lapMesg.addField(c.newInstance("bound_max_position_lat", 27, 133, 1.0, 0.0, "semicircles", false, Profile.Type.SINT32) as Field)
            lapMesg.addField(c.newInstance("bound_max_position_long", 28, 133, 1.0, 0.0, "semicircles", false, Profile.Type.SINT32) as Field)
            lapMesg.addField(c.newInstance("bound_min_position_lat", 29, 133, 1.0, 0.0, "semicircles", false, Profile.Type.SINT32) as Field)
            lapMesg.addField(c.newInstance("bound_min_position_long", 30, 133, 1.0, 0.0, "semicircles", false, Profile.Type.SINT32) as Field)
            lapMesg.setFieldValue(27, 0, track.maxLat()?.toSemiCircles() as Int, "\uffff")
            lapMesg.setFieldValue(28, 0, track.maxLon()?.toSemiCircles() as Int, "\uffff")
            lapMesg.setFieldValue(29, 0, track.minLat()?.toSemiCircles() as Int, "\uffff")
            lapMesg.setFieldValue(30, 0, track.minLon()?.toSemiCircles() as Int, "\uffff")
        } catch (e: NoSuchMethodException) {
            errorMsg.add(e.localizedMessage)
            debugMsg.add(e.localizedMessage)
        } catch (e: IllegalAccessException) {
            errorMsg.add(e.localizedMessage)
            debugMsg.add(e.localizedMessage)
        } catch (e: InstantiationException) {
            errorMsg.add(e.localizedMessage)
            debugMsg.add(e.localizedMessage)
        } catch (e: InvocationTargetException) {
            errorMsg.add(e.localizedMessage)
            debugMsg.add(e.localizedMessage)
        }

        return lapMesg
    }

    // waypoint is "Point" trackpoint is "Location"
    private fun getRecordMesg(point: Location,
                              dst: List<Float>,
                              time: List<Long>,
                              fullyTimestamped: Boolean,
                              hasAltitude: Boolean,
                              index: Int): RecordMesg {
        val record = RecordMesg()
        record.positionLat = point.latitude.toSemiCircles()
        record.positionLong = point.longitude.toSemiCircles()
        if (hasAltitude) record.altitude = point.altitude.toFloat()
        record.distance = dst[index]
        record.timestamp = if (fullyTimestamped) {
            // time (List) is empty
            DateTime((point.time - MILIS_FROM_START_UNIX_ERA_TO_UTC_00_00_Dec_31_1989) / 1000L)
        } else {
            // time (List) is non empty
            DateTime((time[index] - MILIS_FROM_START_UNIX_ERA_TO_UTC_00_00_Dec_31_1989) / 1000L)
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