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

interface OkActionProvider {
    fun getOkAction(): (Float) -> Unit
}

interface LastSelectedValuesProvider {
    fun getSpeedMperS(): Float
    fun setSpeedMperS(value: Float)
    fun getUnitsButtonId(): Int
    fun setUnitsButtonId(id: Int)
}

interface TrackDetailProvider {
    fun getLengthInM(): Int
}

// https://drive.google.com/file/d/1wwYzoPQts1HreDpS614oMAVyafU07ZYF/view?usp=sharing

class SpeedPickerFragment : DialogFragment() {

    private lateinit var actionProvider: OkActionProvider
    private lateinit var lastSelectedProvider: LastSelectedValuesProvider
    private lateinit var trackDetailProvider: TrackDetailProvider

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is Activity) {
            actionProvider = context as OkActionProvider
            lastSelectedProvider = context as LastSelectedValuesProvider
            trackDetailProvider = context as TrackDetailProvider
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

        if (::actionProvider.isInitialized &&
                ::lastSelectedProvider.isInitialized &&
                ::trackDetailProvider.isInitialized) {
            val buttKmh: RadioButton? = mView?.findViewById(R.id.content_speed_picker_speedBTNKmh)
            val buttMph: RadioButton? = mView?.findViewById(R.id.content_speed_picker_speedBTNMph)
            val npSpeed: NumberPicker? = mView?.findViewById(R.id.content_speed_picker_speedNPSpeed)
            val npHours: NumberPicker? = mView?.findViewById(R.id.content_speed_picker_durationNPHours)
            val npMinutes: NumberPicker? = mView?.findViewById(R.id.content_speed_picker_durationNPMinutes)

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
                    Log.e("speedPickFrag", "connectTimeOnChaneListeners $e.")
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

            val lastSelectedButton: RadioButton? = mView?.findViewById(lastSelectedProvider.getUnitsButtonId())
            lastSelectedButton?.isChecked = true

            buttKmh?.setOnClickListener {
                lastSelectedProvider.setUnitsButtonId(R.id.content_speed_picker_speedBTNKmh)
                disconnectTimeOnChangeListeners()
                setCurrentSelectedSpeed(SpeedInPickerPOJO.SpeedWithinBounds(true,
                        false, npSpeed?.value ?: 10), npHours, npMinutes)
                connectTimeOnChangeListeners()
            }

            buttMph?.setOnClickListener {
                lastSelectedProvider.setUnitsButtonId(R.id.content_speed_picker_speedBTNMph)
                disconnectTimeOnChangeListeners()
                setCurrentSelectedSpeed(SpeedInPickerPOJO.SpeedWithinBounds(false,
                        true, npSpeed?.value ?: 10), npHours, npMinutes)
                connectTimeOnChangeListeners()
            }

            updateSpeedCluster(readStoredSpeed(buttKmh?.isChecked, buttMph?.isChecked), npSpeed)

            val initialTrackTimesPOJO = getTrackTimesPOJO(buttKmh?.isChecked,
                    buttMph?.isChecked, npSpeed?.value
                    ?: 10, trackDetailProvider.getLengthInM().toFloat())
            disconnectTimeOnChangeListeners()
            updateDurationCluster(initialTrackTimesPOJO, npHours, npMinutes)
            connectTimeOnChangeListeners()

            val buttonOKSpeed: Button? = mView?.findViewById(R.id.content_speed_picker_speedBTNOk)
            buttonOKSpeed?.setOnClickListener {
                val value: Float = npSpeed?.value?.toFloat() ?: SPEED_DEFAULT_M_S
                val valueMperS: Float = when {
                    (buttKmh != null && buttKmh.isChecked) -> value.kmhToMS()
                    (buttMph != null && buttMph.isChecked) -> value.mphToMS()
                    else -> value.kmhToMS()
                }
                val action = actionProvider.getOkAction()
                action.invoke(valueMperS)
                dialog.dismiss()
            }

        } else {
            Log.e("SpeedPickerFragment", "init error")
        }
        return mView
    }

    private fun readStoredSpeed(kmhChecked: Boolean?, mphChecked: Boolean?): SpeedInPickerPOJO =
            getTrackSpeedPOJO(kmhChecked, mphChecked, lastSelectedProvider.getSpeedMperS())

    //
    private fun setCurrentSelectedSpeed(speedPojo: SpeedInPickerPOJO.SpeedWithinBounds,
                                        npHours: NumberPicker?, npMinutes: NumberPicker?) {
        val inMperS = when {
            (speedPojo.kmh != null && speedPojo.kmh) -> speedPojo.speedUnitAgnostic.toFloat().kmhToMS()
            (speedPojo.mph != null && speedPojo.mph) -> speedPojo.speedUnitAgnostic.toFloat().mphToMS()
            else -> speedPojo.speedUnitAgnostic.toFloat().kmhToMS()
        }
        val extraData: TrackTimesInPickerPOJO = getTrackTimesPOJO(
                speedPojo.kmh,
                speedPojo.mph,
                speedPojo.speedUnitAgnostic,
                trackDetailProvider.getLengthInM().toFloat())
        updateDurationCluster(extraData, npHours, npMinutes)
        lastSelectedProvider.setSpeedMperS(inMperS) // accessor clamps slowest1KMH fastest 130MPH
    }

    //
    private fun setCurrentSelectedTime(timePojo: TrackTimesInPickerPOJO.TimeWithinBounds,
                                       kmh: Boolean?,
                                       mph: Boolean?,
                                       npSpeed: NumberPicker?) {
        val seconds = (timePojo.hours * 60 * 60) + (timePojo.minutes * 60)
        val speedUnitAgnostic = clampSpeed(
                if (seconds > 0) {
                    val mPerS = trackDetailProvider.getLengthInM() / seconds.toFloat()
                    if (kmh != null && kmh) speedMperSToKmh(mPerS)
                    else if (mph != null && mph) speedMperSToMph(mPerS)
                    else speedMperSToKmh(mPerS)
                } else SPEED_MAX_UNIT_AGNOSTIC
        )
        lastSelectedProvider.setSpeedMperS(
                if (kmh != null && kmh) speedUnitAgnostic.toFloat().kmhToMS()
                else if (mph != null && mph) speedUnitAgnostic.toFloat().mphToMS()
                else speedUnitAgnostic.toFloat().kmhToMS()
        )
        npSpeed?.value = speedUnitAgnostic
    }

    //towards hours functions
    //
    private fun updateDurationCluster(extraData: TrackTimesInPickerPOJO, npHours: NumberPicker?, npMinutes: NumberPicker?) {
        fun handleNumberPickersTime(simpleTimePOJO: SimpleTimePOJO, backgroundColor: Int) {
            npHours?.value = simpleTimePOJO.hours
            npMinutes?.value = simpleTimePOJO.minutes
            npHours?.setBackgroundColor(backgroundColor)
            npMinutes?.setBackgroundColor(backgroundColor)
        }
        when {
            (extraData is TrackTimesInPickerPOJO.TimeWithinBounds) -> {
                handleNumberPickersTime(SimpleTimePOJO(extraData.hours, extraData.minutes),
                        ContextCompat.getColor(actionProvider as AppCompatActivity, R.color.blueBackground))
            }
            (extraData is TrackTimesInPickerPOJO.TimeOutOfBounds) -> {
                handleNumberPickersTime(SimpleTimePOJO(extraData.hours, extraData.minutes), extraData.backgroundColor)
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