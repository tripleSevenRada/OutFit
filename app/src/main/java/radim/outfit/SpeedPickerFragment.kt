package radim.outfit

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.NumberPicker
import android.widget.RadioButton
import radim.outfit.core.export.work.*
import java.lang.Exception

const val DEFAULT_UNITS_RADIO_BUTTON_ID = R.id.content_speed_picker_speedBTNKmh

interface TriggerActionProvider {
    fun getTriggerAction(): (Float) -> Unit
}

interface LastSelectedValuesProvider {
    fun getSpeedMperS(): Float
    fun setSpeedMperS(value: Float)
    fun getUnitsButtonId(): Int
    fun setUnitsButtonId(id: Int)
}

interface TrackDetailsProvider {
    fun getLengthInM(): Int
}

// https://drive.google.com/file/d/1wwYzoPQts1HreDpS614oMAVyafU07ZYF/view?usp=sharing

class SpeedPickerFragment : DialogFragment() {

    private lateinit var providerOfTriggerAction: TriggerActionProvider
    private lateinit var providerOfLastSelectedValues: LastSelectedValuesProvider
    private lateinit var providerOfTrackDetails: TrackDetailsProvider

    lateinit var speed_toMperS: Int.() -> Float

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is Activity) {
            providerOfTriggerAction = context as TriggerActionProvider
            providerOfLastSelectedValues = context as LastSelectedValuesProvider
            providerOfTrackDetails = context as TrackDetailsProvider
        } else {
            Log.e("SpeedFrag", "Activity expected, called by $context instead")
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val mView = inflater.inflate(R.layout.speed_picker_fragment, container, false)

        if (::providerOfTriggerAction.isInitialized &&
                ::providerOfLastSelectedValues.isInitialized &&
                ::providerOfTrackDetails.isInitialized) {

            val buttKmh: RadioButton? = mView?.findViewById(R.id.content_speed_picker_speedBTNKmh)
            val buttMph: RadioButton? = mView?.findViewById(R.id.content_speed_picker_speedBTNMph)
            val npSpeed: NumberPicker? = mView?.findViewById(R.id.content_speed_picker_speedNPSpeed)
            val npHours: NumberPicker? = mView?.findViewById(R.id.content_speed_picker_durationNPHours)
            val npMinutes: NumberPicker? = mView?.findViewById(R.id.content_speed_picker_durationNPMinutes)

            val speedIsInKmh_toMperS: Int.() -> Float = {this.kmhToMS()}
            val speedIsInMph_toMperS: Int.() -> Float = {this.mphToMS()}

            fun disconnectSpeedOnChangeListener() {
                npSpeed?.setOnValueChangedListener(null)
            }

            fun disconnectTimeOnChangeListeners() {
                npHours?.setOnValueChangedListener(null)
                npMinutes?.setOnValueChangedListener(null)
            }

            val numberPickers =
                    mutableListOf<NumberPicker?>(npHours, npMinutes)
            val numberPickersToCheckNonNull =
                    mutableListOf<NumberPicker?>(npMinutes, npHours)

            fun connectTimeOnChangeListeners() {
                try {
                    for (i in 0..1) {
                        numberPickers[i]?.setOnValueChangedListener { _, _, newVal ->
                            run {
                                disconnectSpeedOnChangeListener()
                                if (numberPickersToCheckNonNull[i] != null) {
                                    if (i == 0) {
                                        setCurrentSelectedTime(
                                                TrackTimesInPickerPOJO.TimeWithinBounds
                                                (newVal, numberPickersToCheckNonNull[i]!!.value),
                                                buttKmh?.isChecked, buttMph?.isChecked, npSpeed)
                                    } else if (i == 1) {
                                        setCurrentSelectedTime(
                                                TrackTimesInPickerPOJO.TimeWithinBounds
                                                (numberPickersToCheckNonNull[i]!!.value, newVal),
                                                buttKmh?.isChecked, buttMph?.isChecked, npSpeed)
                                    }
                                }
                                // TODO duplicity, this is connectSpeedOnChangeListener()
                                npSpeed?.setOnValueChangedListener { _, _, newVal ->
                                    run {
                                        disconnectTimeOnChangeListeners()
                                        setCurrentSelectedSpeed(SpeedInPickerPOJO.SpeedWithinBounds
                                        (buttKmh?.isChecked, buttMph?.isChecked, newVal), npHours, npMinutes)
                                        connectTimeOnChangeListeners()
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SPF", " $e ")
                }
            }

            fun connectSpeedOnChangeListener() {
                npSpeed?.setOnValueChangedListener { _, _, newVal ->
                    run {
                        disconnectTimeOnChangeListeners()
                        setCurrentSelectedSpeed(SpeedInPickerPOJO.SpeedWithinBounds
                        (buttKmh?.isChecked, buttMph?.isChecked, newVal), npHours, npMinutes)
                        connectTimeOnChangeListeners()
                    }
                }
            }

            connectSpeedOnChangeListener()
            connectTimeOnChangeListeners()

            npSpeed?.minValue = SPEED_MIN_UNIT_AGNOSTIC
            npSpeed?.maxValue = SPEED_MAX_UNIT_AGNOSTIC
            npSpeed?.wrapSelectorWheel = false
            npHours?.wrapSelectorWheel = false
            npHours?.minValue = 0
            npHours?.maxValue = MAX_HOURS_PICKER
            npMinutes?.wrapSelectorWheel = false
            npMinutes?.minValue = 0
            npMinutes?.maxValue = 59

            val lastSelectedButton: RadioButton? =
                    mView?.findViewById(providerOfLastSelectedValues.getUnitsButtonId())
            lastSelectedButton?.isChecked = true

            val unitButtons = mutableListOf<Button?>(buttKmh, buttMph)
            val boolParamKmh = mutableListOf(true, false)
            val boolParamMph = mutableListOf(false, true)
            val buttonIds = mutableListOf<Int>(R.id.content_speed_picker_speedBTNKmh,
                    R.id.content_speed_picker_speedBTNMph)
            val speedUnitsToMperS = mutableListOf<Int.() -> Float>(speedIsInKmh_toMperS, speedIsInMph_toMperS)
            var count = 0
            unitButtons.forEach{
                it?.setOnClickListener {
                    providerOfLastSelectedValues.setUnitsButtonId(buttonIds[count])
                    speed_toMperS = speedUnitsToMperS[count]
                    disconnectTimeOnChangeListeners()
                    setCurrentSelectedSpeed(SpeedInPickerPOJO.SpeedWithinBounds(boolParamKmh[count],
                            boolParamMph[count], npSpeed?.value ?: 10), npHours, npMinutes)
                    connectTimeOnChangeListeners()
                }
                count++
            }

            updateSpeedCluster(readStoredSpeed(buttKmh?.isChecked, buttMph?.isChecked), npSpeed)

            val initialTrackTimesPOJO = getTrackTimesPOJO(
                    providerOfLastSelectedValues.getSpeedMperS(),
                    providerOfTrackDetails.getLengthInM().toFloat())
            disconnectTimeOnChangeListeners()
            updateDurationCluster(initialTrackTimesPOJO, npHours, npMinutes)
            connectTimeOnChangeListeners()

            val buttonOKSpeed: Button? = mView?.findViewById(R.id.content_speed_picker_speedBTNOk)
            buttonOKSpeed?.setOnClickListener {
                val valueToBeInCourseMperS: Float = (npSpeed?.value?: 10).speed_toMperS()
                providerOfTriggerAction.getTriggerAction().invoke(valueToBeInCourseMperS)
                dialog.dismiss()
            }
        } else {
            Log.e("SPF", "Init error...")
        }
        return mView
    }

    private fun readStoredSpeed(kmhChecked: Boolean?, mphChecked: Boolean?): SpeedInPickerPOJO =
            getTrackSpeedPOJO(kmhChecked, mphChecked, providerOfLastSelectedValues.getSpeedMperS())

    //
    private fun setCurrentSelectedSpeed(speedPojo: SpeedInPickerPOJO.SpeedWithinBounds,
                                        npHours: NumberPicker?, npMinutes: NumberPicker?) {
        val inMperS = when {
            (speedPojo.kmh != null && speedPojo.kmh) -> speedPojo.speedUnitAgnostic.toFloat().kmhToMS()
            (speedPojo.mph != null && speedPojo.mph) -> speedPojo.speedUnitAgnostic.toFloat().mphToMS()
            else -> speedPojo.speedUnitAgnostic.toFloat().kmhToMS()
        }
        val extraData: TrackTimesInPickerPOJO = getTrackTimesPOJO(
                inMperS,
                providerOfTrackDetails.getLengthInM().toFloat())
        updateDurationCluster(extraData, npHours, npMinutes)
        providerOfLastSelectedValues.setSpeedMperS(inMperS) // accessor clamps slowest1KMH fastest 130MPH
    }

    //
    private fun setCurrentSelectedTime(timePojo: TrackTimesInPickerPOJO.TimeWithinBounds,
                                       kmh: Boolean?,
                                       mph: Boolean?,
                                       npSpeed: NumberPicker?) {
        val seconds = timePojo.toSeconds()
        val speedUnitAgnostic = clampSpeed(
                if (seconds > 0) {
                    val mPerS = providerOfTrackDetails.getLengthInM() / seconds.toFloat()
                    if (kmh != null && kmh) speedMperSToKmh(mPerS)
                    else if (mph != null && mph) speedMperSToMph(mPerS)
                    else speedMperSToKmh(mPerS)
                } else SPEED_MAX_UNIT_AGNOSTIC
        )
        npSpeed?.value = speedUnitAgnostic
        providerOfLastSelectedValues.setSpeedMperS(
                if (seconds == 0) {
                    SPEED_MAX_UNIT_AGNOSTIC.speed_toMperS()
                } else (providerOfTrackDetails.getLengthInM() / seconds.toFloat())
        )
    }

    //towards hours functions
    //
    private fun updateDurationCluster(extraData: TrackTimesInPickerPOJO, npHours: NumberPicker?,
                                      npMinutes: NumberPicker?) {
        fun handleNumberPickersTime(simpleTimePOJO: SimpleTimePOJO, backgroundColor: Int) {
            npHours?.value = simpleTimePOJO.hours
            npMinutes?.value = simpleTimePOJO.minutes
            npHours?.setBackgroundColor(backgroundColor)
            npMinutes?.setBackgroundColor(backgroundColor)
        }
        when {
            (extraData is TrackTimesInPickerPOJO.TimeWithinBounds) -> {
                handleNumberPickersTime(SimpleTimePOJO(extraData.hours, extraData.minutes),
                        ContextCompat.getColor(providerOfTriggerAction as AppCompatActivity,
                                R.color.blueBackground))
            }
            (extraData is TrackTimesInPickerPOJO.TimeOutOfBounds) -> {
                handleNumberPickersTime(SimpleTimePOJO(extraData.hours, extraData.minutes),
                        extraData.backgroundColor)
            }
        }
    }

    // towards speed functions
    //
    private fun updateSpeedCluster(extraData: SpeedInPickerPOJO, npSpeed: NumberPicker?) {
        fun handleSpeedPicker(speed: Int, backgroundColor: Int) {
            npSpeed?.value = speed
            npSpeed?.setBackgroundColor(backgroundColor)
        }
        when {
            (extraData is SpeedInPickerPOJO.SpeedWithinBounds) -> {
                handleSpeedPicker(extraData.speedUnitAgnostic, Color.WHITE)
            }
            (extraData is SpeedInPickerPOJO.SpeedOutOfBounds) -> {
                handleSpeedPicker(extraData.speedUnitAgnostic, extraData.backgroundColor)
            }
        }
    }
}