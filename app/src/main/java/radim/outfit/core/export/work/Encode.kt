package radim.outfit.core.export.work

import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.garmin.fit.*
import locus.api.objects.extra.Track
import radim.outfit.core.export.logic.Result
import radim.outfit.core.export.work.locusapiextensions.*
import radim.outfit.core.export.work.locusapiextensions.stringdumps.TrackStringDump
import radim.outfit.debugdumps.FitSDKDebugDumps.Dumps
import java.io.File
import com.garmin.fit.DateTime
import radim.outfit.DEBUG_MODE
import radim.outfit.debugasserts.*
import radim.outfit.getString
import java.lang.RuntimeException

const val MIN_TIME_TAKEN = 8
const val MILIS_FROM_START_UNIX_ERA_TO_UTC_00_00_Dec_31_1989 = 631065600000L

// Error messages 1 - 2
class Encoder {

    // with great help of:
    // https://github.com/gimportexportdevs/gexporter/blob/master/app/src/main/java/org/surfsite/gexporter/Gpx2Fit.java
    // https://github.com/mrihtar/Garmin-FIT

    fun encode(track: Track,
               dir: File,
               filename: String,
               speedIfNotInTrack: Float,
               ctx: AppCompatActivity
    ): Result {

        val start = System.currentTimeMillis()

        val publicMessages = mutableListOf<String>()
        val debugMessages = mutableListOf<String>()
        val errorMessages = mutableListOf<String>()
        publicMessages.add("${ctx.getString("filename")} $filename")
        debugMessages.add("Debug:")
        errorMessages.add("Error:")

        val outputFile = File(dir.absolutePath + File.separatorChar + filename)
        lateinit var encoder: FileEncoder
        lateinit var filterBundle: MessagesStringFilterBundle
        var courseName: String = "default"
        try {
            encoder = FileEncoder(outputFile, Fit.ProtocolVersion.V1_0)
            filterBundle = MessagesStringFilterBundle(FilenameCharsFilter(),
                    FILENAME_RESERVED_CHARS,
                    COURSE_AND_COURSEPOINTS_REPLACEMENT_CHAR)

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
            val fileIdMesg = getFileIdMesg()
            encoder.write(fileIdMesg)

            // 'Course message'
            val courseMesg = getCourseMesg(track, filename, filterBundle)
            courseName = courseMesg.name
            encoder.write(courseMesg)
            with(publicMessages) {
                add("${ctx.getString("course_name")} ${courseMesg.name}")
                add(ctx.getString("exported"))
            }
            /*

            // https://drive.google.com/file/d/1IgF-khr7c0fF5zZzU8fORJqGpF5DXliz/view?usp=sharing

@startuml
title Track.isFullyTimestamped()
state "isFullyTimestamped" as stamped {
  state true
  state false
  true --> distancesNonNullPoints: Assign
  false --> distancesNonNullPoints: Assign
  true --> timestampsNonNullPoints: Assign EMPTY list
  false --> timestampsNonNullPoints: Assign
  true --> trackTimeStampsBundle: ExtractFromPoints
  false --> trackTimeStampsBundle: extract from\ntimestampsNonNullPoints \nwhich is NOT EMPTY
  note right of speedsNonNullPoints
  Depends on:
  trackTimeStampsBundle
  distancesNonNullPoints
  end note
}
@enduml
            */

            //================================================================================
            val trackIsFullyTimestamped = track.isTimestamped() && track.stats.isTimestamped()
            //================================================================================

            if (DEBUG_MODE) debugMessages.add("trackIsFullyTimestamped: $trackIsFullyTimestamped")

            //================================================================================
            val trackHasAltitude = track.hasAltitude()
            //================================================================================

            if (DEBUG_MODE) {
                debugMessages.add("trackHasAltitude: $trackHasAltitude")
                debugMessages.addAll(TrackStringDump.stringDescriptionDeep(track))
            }

            val distancesNonNullPoints = assignPointDistancesToNonNullPoints(track)

            if (DEBUG_MODE) {
                if (!assertValuesIncreasingOrEqual(distancesNonNullPoints)) {
                    throw RuntimeException("assertValuesIncreasingOrEqual - distancesNonNullPoints")
                }
            }

            val timestampsNonNullPoints = if (trackIsFullyTimestamped) {
                if (DEBUG_MODE) {
                    if (!assertTimestampsIncreasingOrEqualFullyTimestampedTrack(track))
                        throw RuntimeException("assertTimestampsIncreasingOrEqualFullyTimestampedTrack")
                }
                listOf()
            } else {
                val timestamps = assignPointTimestampsToNonNullPoints(track, distancesNonNullPoints, speedIfNotInTrack)
                if (DEBUG_MODE) {
                    if (!assertValuesIncreasingOrEqual(timestamps)) {
                        throw RuntimeException("assertValuesIncreasingOrEqual - timestamps")
                    }
                }
                timestamps
            }

            if (DEBUG_MODE) {
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

            val speedsNonNullPoints = assignSpeedsToNonNullPoints(track, timeBundle, distancesNonNullPoints)

            if (DEBUG_MODE) {
                debugMessages.addAll(Dumps.banner())
                speedsNonNullPoints.forEach { debugMessages.add(it.toString()) }
            }

            // 'Lap message'
            val lapMesg = getLapMesg(track,
                    timeBundle,
                    distancesNonNullPoints
            )

            encoder.write(lapMesg)

            if (DEBUG_MODE) {
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

            if (DEBUG_MODE) {
                debugMessages.addAll(Dumps.banner())
                debugMessages.addAll(Dumps.eventMessageDump(eventMesgStart))
                debugMessages.addAll(Dumps.banner())
            }

            // WAYPOINTS START

            val waypointsRebuilder = AttachWaypointsToTrack(track)
            val waypointsRebuilt = waypointsRebuilder.rebuild(track.waypoints, DEBUG_MODE)
            if (DEBUG_MODE) {
                debugMessages.addAll(Dumps.banner())
                debugMessages.add("++++++++++++++++++++++AttachWaypointsToTrack")
                debugMessages.addAll(waypointsRebuilder.debugMessages)
            }
            val unsupportedRid = ridUnsupportedRtePtActions(waypointsRebuilt)
            val reducedToLimit = reduceWayPointsSizeTo(unsupportedRid, COURSEPOINTS_LIMIT)
            val mapNonNullIndicesToTmstmp = mapNonNullPointsIndicesToTimestamps(track, timeBundle)
            val mapNonNullIndicesToDist = mapNonNullPointsIndicesToDistances(track, distancesNonNullPoints)

            if (DEBUG_MODE) {
                debugMessages.addAll(Dumps.banner())
                debugMessages.add("++++++++++++++++++++++mapNonNullIndicesToTmstmp")
                debugMessages.add(mapNonNullIndicesToTmstmp.toString())
                debugMessages.addAll(Dumps.banner())
            }

            val mapCoursePointsTypesToFrequencies = mutableMapOf<CoursePoint, Int>()
            var countCP = 0

            if (DEBUG_MODE) {
                // SANITY CHECKS
                reducedToLimit.forEach {
                    if (!it.hasProperName() ||
                            !it.hasValidRteIndex(track))
                        throw RuntimeException("proper name or rteIndex: name: >${it.name}< rteIndex: ${it.rteIndex}")
                }
                if (!assertWaypointsAreLinkedToTrackpointsOneToOneIncreasing(reducedToLimit))
                    throw RuntimeException("assertWaypointsAreLinkedToTrackpointsOneToOneIncreasing")
            }

            reducedToLimit.forEach {
                val coursePointMesg = getCoursepointMesg(
                        it,
                        mapNonNullIndicesToTmstmp,
                        mapNonNullIndicesToDist,
                        track,
                        filterBundle)
                if (coursePointMesg != null) {
                    encoder.write(coursePointMesg)
                    countCP++
                    countFrequencies(coursePointMesg.type, mapCoursePointsTypesToFrequencies)
                    if (DEBUG_MODE) debugMessages.addAll(Dumps.coursePointMessageDumpLine(coursePointMesg))
                }
            }

            val indexToInsertTrackpointsInfo = publicMessages.size
            publicMessages.add("${ctx.getString("nmb_coursepoints")} ${reducedToLimit.size}")

            coursePointsDisplayOrder.forEach {
                if (mapCoursePointsTypesToFrequencies.keys.contains(it)) {
                    publicMessages.add("$it : ${mapCoursePointsTypesToFrequencies[it]}")
                }
            }
            publicMessages.add(if (trackHasAltitude) ctx.getString("course_has_elevation_yes")
            else ctx.getString("course_has_elevation_no"))

            if (DEBUG_MODE) {
                debugMessages.addAll(Dumps.banner())
                debugMessages.add("CP count: $countCP")
                debugMessages.addAll(Dumps.banner())
            }
            // WAYPOINTS END

            // RECORDS START
            var index = 0
            var timestamp: DateTime? = null
            for (i in track.points.indices) {
                if (track.points[i] == null && DEBUG_MODE) {
                    debugMessages.add("-----------------------------------NULL PRESENT!")
                    continue
                }
                val recordMesg = getRecordMesg(
                        track.points[i],
                        distancesNonNullPoints,
                        timestampsNonNullPoints,
                        speedsNonNullPoints,
                        trackIsFullyTimestamped,
                        trackHasAltitude,
                        index
                )
                timestamp = recordMesg.timestamp
                if (DEBUG_MODE) debugMessages.addAll(Dumps.recordMessageDumpLine(recordMesg))
                encoder.write(recordMesg)
                index++
            }
            publicMessages.add(indexToInsertTrackpointsInfo, "${ctx.getString("nmb_trackpoints")} ${track.points.size}")
            // RECORDS END

            // sanity check
            if (index != distancesNonNullPoints.size ||
                    (!trackIsFullyTimestamped && index != timestampsNonNullPoints.size)) {
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

            if (DEBUG_MODE) {
                debugMessages.addAll(Dumps.banner())
                debugMessages.addAll(Dumps.eventMessageDump(eventMesgStop))
            }

        } catch (e: Exception) { //FitRuntimeException
            errorMessages.add("${e.localizedMessage} - 1")
            e.printStackTrace()
            return Result.Fail(debugMessages, errorMessages, dir, filename, courseName, e)
        } finally {
            try {
                encoder.close()
            } catch (e: Exception) { //FitRuntimeException
                errorMessages.add("${e.localizedMessage} - 2")
                e.printStackTrace()
                return Result.Fail(debugMessages, errorMessages, dir, filename, courseName, e)
            }
        }

        val timeTaken = System.currentTimeMillis() - start
        Log.i("Encode", "time taken: $timeTaken")
        if (timeTaken < MIN_TIME_TAKEN) {
            // TODO interrupted?
            Thread.sleep(MIN_TIME_TAKEN - timeTaken)
        }
        return Result.Success(publicMessages, debugMessages, dir, filename, courseName)
    }

    private fun <K> countFrequencies(word: K, map: MutableMap<K, Int>) {
        if (!map.containsKey(word))
            map[word] = 1
        else {
            var count = map[word]
            if (count != null) {
                count++
                map[word] = count
            }
        }
    }
}