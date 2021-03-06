package radim.outfit.core.export.work.locusapiextensions

import com.garmin.fit.CoursePoint
import locus.api.objects.enums.PointRteAction
import locus.api.objects.extra.Point
import radim.outfit.core.export.work.styleNameORIconStyleIconUrlToCoursePoints

const val PARAMETER_STYLE_NAME_FALLBACK = "poi"

fun Point.getWaypointName(): String {
    return if (parameterRteAction != null &&
            parameterRteAction != PointRteAction.PASS_PLACE &&
            parameterRteAction != PointRteAction.UNDEFINED
    ) {
        if (name.isNullOrEmpty()) {
            parameterRteAction.textId ?: PARAMETER_STYLE_NAME_FALLBACK
        } else {
            name
        }
    } else if (
            parameterRteAction != null) {
        if (name.isNullOrEmpty()) {
            if (parameterStyleName.isNullOrEmpty()) PARAMETER_STYLE_NAME_FALLBACK
            else parameterStyleName
        } else name
    } else PARAMETER_STYLE_NAME_FALLBACK
}

/*
String styleName = pt.getParameterStyleName();
if (Validator.isValid(styleName)) {
    return styleName;
}

if (pt.styleNormal != null && pt.styleNormal.getIconStyleIconUrl() != null) {
    return pt.styleNormal.getIconStyleIconUrl();
}
return "";
*/

fun Point.getCoursepointEnumForced(): CoursePoint? {
    val styleName: String = parameterStyleName
            ?: if (styleNormal != null && styleNormal.iconStyleIconUrl != null)
                styleNormal.iconStyleIconUrl else ""
    var enumForced = styleNameORIconStyleIconUrlToCoursePoints[styleName]
    if (enumForced == null &&
            (parameterRteAction == PointRteAction.PASS_PLACE ||
                    parameterRteAction == PointRteAction.UNDEFINED))
        enumForced = CoursePoint.GENERIC
    return enumForced
}

data class WaypointSimplified(val rteIndex: Int,
                              val name: String,
                              val rteAction: PointRteAction,
                              val coursepointEnumForced: CoursePoint?) : Comparable<WaypointSimplified> {
    constructor(point: Point) : this(point.paramRteIndex, point.getWaypointName(),
            point.parameterRteAction, point.getCoursepointEnumForced())

    constructor(waypointSimplified: WaypointSimplified, newIndex: Int) : this(
            newIndex,
            waypointSimplified.name,
            waypointSimplified.rteAction,
            waypointSimplified.coursepointEnumForced
    )

    override fun compareTo(other: WaypointSimplified): Int = this.rteIndex.compareTo(other.rteIndex)
}

val allLeft: Set<PointRteAction> = setOf(
        PointRteAction.EXIT_LEFT,
        PointRteAction.LEFT,
        PointRteAction.LEFT_SHARP,
        PointRteAction.LEFT_SLIGHT,
        PointRteAction.MERGE_LEFT,
        PointRteAction.STAY_LEFT,
        PointRteAction.RAMP_ON_LEFT,
        PointRteAction.U_TURN_LEFT
)
val allRight: Set<PointRteAction> = setOf(
        PointRteAction.EXIT_RIGHT,
        PointRteAction.RIGHT,
        PointRteAction.RIGHT_SHARP,
        PointRteAction.RIGHT_SLIGHT,
        PointRteAction.MERGE_RIGHT,
        PointRteAction.STAY_RIGHT,
        PointRteAction.RAMP_ON_RIGHT,
        PointRteAction.U_TURN_RIGHT
)
val allStraight: Set<PointRteAction> = setOf(
        PointRteAction.RAMP_STRAIGHT,
        PointRteAction.CONTINUE_STRAIGHT,
        PointRteAction.STAY_STRAIGHT,
        PointRteAction.MERGE
)
// NO_MANEUVER and CONTINUE_STRAIGHT are not dismissible, used
// for clustered rte actions,
// NO_MANEUVER later mapped to Coursepoint.GENERIC
// CONTINUE_STRAIGHT to Coursepoint.STRAIGHT
val dismissibleRteActions: Set<PointRteAction> = setOf(
        PointRteAction.MERGE,
        PointRteAction.ENTER_STATE,
        PointRteAction.STAY_STRAIGHT,
        PointRteAction.RAMP_STRAIGHT,
        PointRteAction.STAY_RIGHT,
        PointRteAction.STAY_LEFT,
        PointRteAction.RAMP_ON_RIGHT,
        PointRteAction.RAMP_ON_LEFT
)
// always filter out these at the very beginning, before Kruskal possibly adds
// NO_MANEUVER used as a placeholder fallback in clustering
val alwaysDismissibleRteActions: Set<PointRteAction> = setOf(
        PointRteAction.NO_MANEUVER,
        PointRteAction.NO_MANEUVER_NAME_CHANGE
)