package radim.outfit.core.viewmodels

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import android.content.Intent
import android.util.Log
import android.widget.Toast
import facade.SegmentsMatchAPI
import locus.api.android.utils.LocusUtils
import locus.api.android.utils.exceptions.RequiredVersionMissingException
import locus.api.objects.extra.Point
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import radim.outfit.DEBUG_MODE
import radim.outfit.LOG_TAG_MAIN
import radim.outfit.R
import radim.outfit.core.export.work.locusapiextensions.hasUndefinedWaypoints
import radim.outfit.core.export.work.locusapiextensions.track_preprocessing.TrackContainer
import radim.outfit.core.export.work.locusapiextensions.track_preprocessing.WaypointsRelatedTrackPreprocessing
import resources.ActivityType
import resources.LatLonPair
import resources.MatchingResult
import resources.MatchingScenario
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivityViewModel (application: Application): AndroidViewModel (application){

    // Repository pattern? No need, IMO

    val app = application

    var exportInProgress = false
    var trackContainerFinished = false
    var preprocessInProgress = false
    var speedPickerFragmentShown = false

    var exportOptionsDisplayed = false

    var btWarningShownCounter = 0

    var trackContainer = MutableLiveData<TrackContainer>()

    // Encapsulation?
    var trackTotalLength: Double? = null
    var activityType: Int? = null

    fun buildTrackContainer(intent: Intent, debugMessages: MutableList<String>) {
        preprocessInProgress = true
        var failMessage = ""

        // TODO
        val callbacks = object : Callback<MatchingResult>{
            override fun onFailure(call: Call<MatchingResult>, t: Throwable) {
                Toast.makeText(app, "Segments: onFailure -> ${t.localizedMessage}",Toast.LENGTH_LONG).show()
            }
            override fun onResponse(call: Call<MatchingResult>, response: Response<MatchingResult>) {
                Toast.makeText(app, "Segments: onResponse -> ${response.body()?.segmentsDetected?.size}",Toast.LENGTH_LONG).show()
            }
        }

        // TODO async
        val tokenValid = "b0d77cdd6000365506e7149b77283eb064f36982"
        val trackUp = LocusUtils.handleIntentTrackTools(app, intent)
        val locations = mutableListOf<LatLonPair>()
        trackUp.points.forEach { locations.add(LatLonPair(it.latitude, it.longitude)) }
        SegmentsMatchAPI().asynchronousCall(locations, ActivityType.RIDE, MatchingScenario.LOOSE, tokenValid, callbacks)


        doAsync {
            var trackContainerBuilt: TrackContainer? = null
            try {
                val track = LocusUtils.handleIntentTrackTools(app, intent)

                trackContainerBuilt = if (track.hasUndefinedWaypoints()) {
                    if (DEBUG_MODE) {
                        val message = "Track has undefined waypoints"
                        Log.w(LOG_TAG_MAIN, message)
                        debugMessages.add(message)
                    }
                    WaypointsRelatedTrackPreprocessing(
                            track,
                            debugMessages
                    ).preprocess() // returns TrackContainer
                } else {
                    val definedRteActionsToIndices = mutableMapOf<Point, Int>()
                    track.waypoints.forEach { wpt ->
                        if (wpt.paramRteIndex != -1)
                            definedRteActionsToIndices[wpt] = wpt.paramRteIndex
                        else if (DEBUG_MODE) {
                            failMessage = "Unexpected paramRteIndex = -1 Error 8"
                        }
                    }
                    TrackContainer(track, definedRteActionsToIndices)
                }

                // or inject a mock
                //trackContainer = getTrackOkNoCP()
                //trackContainer = getTrackNullEndNoCP()
                //trackContainer = getTrackNullStartNoCP()
                //trackContainer = getTrackRandomNullsNoCP()

            } catch (e: RequiredVersionMissingException) {
                failMessage = "${app.getString(R.string.required_version_missing)} ${e.localizedMessage} Error 4" //"required_version_missing"
            } catch (e: Exception) {
                failMessage = "${e.localizedMessage} Error 5"
            }
            trackContainerBuilt?.failedMessage = failMessage
            uiThread {
                preprocessInProgress = false
                trackContainerFinished = true
                val validPoints: Boolean = trackContainerBuilt?.track?.points?.size.let { it != null && it > 0 }
                val validStats: Boolean = trackContainerBuilt?.track?.stats != null
                if (validPoints && validStats) {
                    trackContainer.value = trackContainerBuilt
                    trackTotalLength = trackContainerBuilt?.track?.stats?.totalLength?.toDouble()
                    activityType = trackContainerBuilt?.track?.activityType
                } else {
                    trackContainerBuilt?.failedMessage = " null or empty - Error 6"
                    trackContainer.value = trackContainerBuilt
                }
            }
        }
    }

    //override fun onCleared() {
    //super.onCleared()
    //}

}