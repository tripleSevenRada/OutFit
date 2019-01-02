package radim.outfit.core.export.work

import android.util.Log
import com.garmin.fit.*
import locus.api.objects.enums.PointRteAction
import locus.api.objects.extra.Location
import locus.api.objects.extra.Track
import radim.outfit.core.FilenameCharsFilter
import radim.outfit.core.export.work.locusapiextensions.*
import java.util.*

const val COURSENAME_MAX_LENGTH = 12

internal fun getFileIdMesg(): FileIdMesg {
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

internal fun getCourseMesg(track: Track, filename: String, filterBundle: MessagesStringFilterBundle): CourseMesg {
    /*
    name (5-13-STRING): "kostelecRoad"
    sport (4-1-ENUM): cycling (2)
     */
    val courseMesg = CourseMesg()
    courseMesg.localNum = 1
    courseMesg.name = if (!track.name.isNullOrEmpty()) {
        val lengthAsserted = assertStringLength(track.name, COURSENAME_MAX_LENGTH)
        replaceReserved(lengthAsserted, filterBundle)
    } else {
        val lengthAsserted = assertStringLength(
                filename.substring(0, filename.lastIndexOf(".")), COURSENAME_MAX_LENGTH)
        replaceReserved(lengthAsserted, filterBundle)
    }
    courseMesg.sport = Sport.GENERIC
    // courseMesg.capabilities = CourseCapabilities.NAVIGATION // Not required
    return courseMesg
}

internal fun getLapMesg(track: Track,
                        trackTimestampsBundle: TrackTimestampsBundle,
                        dst: List<Float>
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
internal fun getRecordMesg(point: Location,
                           dst: List<Float>,
                           time: List<Long>,
                           speed: List<Float>,
                           fullyTimestamped: Boolean,
                           hasAltitude: Boolean,
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
        record.distance = dst[index]
        DateTime((time[index] - MILIS_FROM_START_UNIX_ERA_TO_UTC_00_00_Dec_31_1989) / 1000L)
    }
    record.speed = speed[index]
    if (hasAltitude) record.altitude = point.altitude.toFloat()
    return record
}

/*
course_point (32, type: 5, length: 32 bytes):
timestamp (1-1-UINT32): 2018-12-14T19:50:04 (913747804)
type (5-1-ENUM): generic (0)
position_lat (2-1-SINT32): 50.0414826 deg (597018142)
position_long (3-1-SINT32): 14.4738929 deg (172680268)
distance (4-1-UINT32): 0.00 m (0)
name (6-14-STRING): "Generic Point"
*/

internal fun getCoursepointMesg(wp: WaypointSimplified,
                                mapNonNullIndicesToTmstmp: Map<Int, Long>,
                                mapNonNullIndicesToDist: Map<Int, Float>,
                                track: Track,
                                filterBundle: MessagesStringFilterBundle): CoursePointMesg? {
    val tag = "getCPMesg"
    val typeInLocus: PointRteAction? = wp.rteAction
    typeInLocus ?: return null
    // unsupported should be already filtered out by caller!
    if (!routePointActionsToCoursePoints.keys.contains(typeInLocus)) return null
    val cp = CoursePointMesg()
    cp.localNum = 5
    val indexOfTrackpoint = wp.rteIndex
    if (indexOfTrackpoint == -1) {
        Log.e(tag, "unexpected wp.rteIndex == -1")
        return null
    }
    val tmstmp: Long? = mapNonNullIndicesToTmstmp[indexOfTrackpoint]
    val dst: Float? = mapNonNullIndicesToDist[indexOfTrackpoint]
    tmstmp ?: return null
    dst ?: return null
    cp.timestamp = DateTime((tmstmp - MILIS_FROM_START_UNIX_ERA_TO_UTC_00_00_Dec_31_1989) / 1000L)
    val p: Location? = track.getPoint(indexOfTrackpoint)
    p ?: return null
    cp.positionLat = p.latitude.toSemiCircles()
    cp.positionLong = p.longitude.toSemiCircles()
    cp.distance = dst
    cp.type = if (wp.coursepointEnumForced == null) routePointActionsToCoursePoints[typeInLocus]
    else wp.coursepointEnumForced
    val lengthAsserted = assertStringLength(wp.name, COURSEPOINTS_NAME_MAX_LENGTH)
    cp.name = replaceReserved(lengthAsserted, filterBundle)
    return cp
}

internal fun assertStringLength(value: String, max: Int): String {
    return if (value.length > max) {
        value.substring(0, max)
    } else {
        value
    }
}

internal fun replaceReserved(value: String, filterBundle: MessagesStringFilterBundle): String {
    return filterBundle.filter.replaceReservedChars(
            value, filterBundle.reserved, filterBundle.replacementChar).toString()
}

data class MessagesStringFilterBundle(val filter: FilenameCharsFilter,
                                      val reserved: Set<Char>,
                                      val replacementChar: Char)