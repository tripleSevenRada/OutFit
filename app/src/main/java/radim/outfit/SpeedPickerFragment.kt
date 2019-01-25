package radim.outfit

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.NumberPicker
import android.widget.RadioButton
import android.widget.TextView
import radim.outfit.core.export.work.*
import java.lang.Exception

const val DEFAULT_UNITS_RADIO_BUTTON_ID = R.id.content_speed_picker_speedBTNKmh

// https://drive.google.com/file/d/1wwYzoPQts1HreDpS614oMAVyafU07ZYF/view?usp=sharing

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
    fun getActivityType(): Int
}

class SpeedPickerFragment : DialogFragment() {

    private lateinit var providerOfTriggerAction: TriggerActionProvider
    private lateinit var providerOfLastSelectedValues: LastSelectedValuesProvider
    private lateinit var providerOfTrackDetails: TrackDetailsProvider

    private lateinit var speedInCheckedUnitToMperS: Int.() -> Float
    private lateinit var speedInMperSToCheckedUnit: Float.() -> Int

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is Activity) {
            providerOfTriggerAction = context as TriggerActionProvider
            providerOfLastSelectedValues = context as LastSelectedValuesProvider
            providerOfTrackDetails = context as TrackDetailsProvider
        } else {
            Log.e("SPF", "Activity expected, called by $context instead")
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

            val tvActivityType: TextView? = mView?.findViewById(R.id.content_speed_picker_speedTVActivityType)
            val activityMessage = context?.getString(R.string.frag_label_activity_type) + providerOfTrackDetails.getActivityType()
            tvActivityType?.text = activityMessage

            val buttKmh: RadioButton? = mView?.findViewById(R.id.content_speed_picker_speedBTNKmh)
            val buttMph: RadioButton? = mView?.findViewById(R.id.content_speed_picker_speedBTNMph)
            val npSpeed: NumberPicker? = mView?.findViewById(R.id.content_speed_picker_speedNPSpeed)
            val npHours: NumberPicker? = mView?.findViewById(R.id.content_speed_picker_durationNPHours)
            val npMinutes: NumberPicker? = mView?.findViewById(R.id.content_speed_picker_durationNPMinutes)

            val speedIsInKmhToMperS: Int.() -> Float = { this.kmhToMs() }
            val speedIsInMphToMperS: Int.() -> Float = { this.mphToMs() }

            val speedInMperStoKmh: Float.() -> Int = { this.msToKmh() }
            val speedInMperStoMph: Float.() -> Int = { this.msToMph() }

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
                                                TrackTimesInPickerPOJO
                                                (newVal, numberPickersToCheckNonNull[i]!!.value),
                                                npSpeed)
                                    } else if (i == 1) {
                                        setCurrentSelectedTime(
                                                TrackTimesInPickerPOJO
                                                (numberPickersToCheckNonNull[i]!!.value, newVal),
                                                npSpeed)
                                    }
                                }
                                npSpeed?.setOnValueChangedListener { _, _, newVal ->
                                    run {
                                        disconnectTimeOnChangeListeners()
                                        setCurrentSelectedSpeed(newVal.speedInCheckedUnitToMperS(),
                                                npHours, npMinutes)
                                        connectTimeOnChangeListeners()
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SPF", "{$e.localizedMessage()}")
                }
            }

            fun connectSpeedOnChangeListener() {
                npSpeed?.setOnValueChangedListener { _, _, newVal ->
                    run {
                        disconnectTimeOnChangeListeners()
                        setCurrentSelectedSpeed(newVal.speedInCheckedUnitToMperS(),
                                npHours, npMinutes)
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

            val unitButtons = mutableListOf<Button?>(buttKmh, buttMph)
            val buttonIds = mutableListOf<Int>(R.id.content_speed_picker_speedBTNKmh,
                    R.id.content_speed_picker_speedBTNMph)
            val speedInUnitToMperS = mutableListOf<Int.() -> Float>(speedIsInKmhToMperS, speedIsInMphToMperS)
            val speedInMperSToUnit = mutableListOf<Float.() -> Int>(speedInMperStoKmh, speedInMperStoMph)

            for (i in unitButtons.indices) {
                unitButtons[i]?.setOnClickListener {
                    providerOfLastSelectedValues.setUnitsButtonId(buttonIds[i])
                    speedInCheckedUnitToMperS = speedInUnitToMperS[i]
                    speedInMperSToCheckedUnit = speedInMperSToUnit[i]
                    disconnectTimeOnChangeListeners()
                    setCurrentSelectedSpeed((npSpeed?.value
                            ?: 10).speedInCheckedUnitToMperS(), npHours, npMinutes)
                    connectTimeOnChangeListeners()
                }
            }

            val buttonId = providerOfLastSelectedValues.getUnitsButtonId()
            val lastSelectedButton: RadioButton? = mView?.findViewById(buttonId)
            lastSelectedButton?.isChecked = true
            for (i in buttonIds.indices) {
                if (buttonId == buttonIds[i]) {
                    speedInCheckedUnitToMperS = speedInUnitToMperS[i]
                    speedInMperSToCheckedUnit = speedInMperSToUnit[i]
                }
            }

            npSpeed?.value = readStoredSpeed()

            val initialTrackTimesPOJO = getTrackTimesPOJO(
                    providerOfLastSelectedValues.getSpeedMperS(),
                    providerOfTrackDetails.getLengthInM().toFloat())
            disconnectTimeOnChangeListeners()
            updateDurationCluster(initialTrackTimesPOJO, npHours, npMinutes)
            connectTimeOnChangeListeners()

            val buttonOKSpeed: Button? = mView?.findViewById(R.id.content_speed_picker_speedBTNOk)
            buttonOKSpeed?.setOnClickListener {
                val valueToBeInCourseMperS: Float = (npSpeed?.value
                        ?: 10).speedInCheckedUnitToMperS()
                providerOfTriggerAction.getTriggerAction().invoke(valueToBeInCourseMperS)
                dialog.dismiss()
            }
        } else {
            Log.e("SPF", "Init error...")
        }
        return mView
    }

    private fun readStoredSpeed(): Int =
            getTrackSpeed(providerOfLastSelectedValues.getSpeedMperS(), speedInMperSToCheckedUnit)

    //
    private fun setCurrentSelectedSpeed(inMperS: Float, npHours: NumberPicker?, npMinutes: NumberPicker?) {
        val extraDataTimes: TrackTimesInPickerPOJO = getTrackTimesPOJO(inMperS,
                providerOfTrackDetails.getLengthInM().toFloat()
        )
        updateDurationCluster(extraDataTimes, npHours, npMinutes)
        providerOfLastSelectedValues.setSpeedMperS(inMperS)
    }

    //
    private fun setCurrentSelectedTime(timePojo: TrackTimesInPickerPOJO,
                                       npSpeed: NumberPicker?) {
        val seconds = timePojo.toSeconds()
        val speed = clampSpeedForSpeedPicker(
                if (seconds > 0) {
                    val mPerS = providerOfTrackDetails.getLengthInM() / seconds.toFloat()
                    mPerS.speedInMperSToCheckedUnit()
                } else SPEED_MAX_UNIT_AGNOSTIC + 10 // so clamp is needed
                )
        npSpeed?.value = speed
        providerOfLastSelectedValues.setSpeedMperS(
                if (seconds == 0) {
                    (SPEED_MAX_UNIT_AGNOSTIC + 10).speedInCheckedUnitToMperS()// so clamp is needed
                } else (providerOfTrackDetails.getLengthInM() / seconds.toFloat())
        )
    }

    //towards hours functions
    //
    private fun updateDurationCluster(extraData: TrackTimesInPickerPOJO, npHours: NumberPicker?,
                                      npMinutes: NumberPicker?) {
        with(clampTimeForTimePicker(extraData)) {
            npHours?.value = this.hours
            npMinutes?.value = this.minutes
        }
    }
}