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
import radim.outfit.core.export.work.*

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
        if (::actionProvider.isInitialized && ::lastSelectedProvider.isInitialized && ::trackDetailProvider.isInitialized) {

            val npSpeed: NumberPicker? = mView?.findViewById(R.id.content_speed_picker_speedNPSpeed)
            val npHours: NumberPicker? = mView?.findViewById(R.id.content_speed_picker_durationNPHours)
            val npMinutes: NumberPicker? = mView?.findViewById(R.id.content_speed_picker_durationNPMinutes)
            npSpeed?.minValue = SPEED_MIN_UNIT_AGNOSTIC
            npSpeed?.maxValue = SPEED_MAX_UNIT_AGNOSTIC
            npSpeed?.wrapSelectorWheel = false
            npHours?.wrapSelectorWheel = false
            npHours?.minValue = 0
            npHours?.maxValue = MAX_HOURS_PICKER
            npMinutes?.minValue = 0
            npMinutes?.maxValue = 59
            npMinutes?.wrapSelectorWheel = false
            val buttKmh: RadioButton? = mView?.findViewById(R.id.content_speed_picker_speedBTNKmh)
            val buttMph: RadioButton? = mView?.findViewById(R.id.content_speed_picker_speedBTNMph)

            val lastSelectedButton: RadioButton? = mView?.findViewById(lastSelectedProvider.getUnitsButtonId())
            lastSelectedButton?.isChecked = true
            buttKmh?.setOnClickListener {
                lastSelectedProvider.setUnitsButtonId(R.id.content_speed_picker_speedBTNKmh)
                setCurrentSelectedSpeedInMperS(true, false, npSpeed?.value ?: 10, npHours, npMinutes)
            }
            buttMph?.setOnClickListener {
                lastSelectedProvider.setUnitsButtonId(R.id.content_speed_picker_speedBTNMph)
                setCurrentSelectedSpeedInMperS(false, true, npSpeed?.value ?: 10, npHours, npMinutes)
            }
            npSpeed?.value = readStoredSpeedInUnit(buttKmh?.isChecked, buttMph?.isChecked)
            npSpeed?.setOnValueChangedListener { _, _, newVal ->
                run {
                    setCurrentSelectedSpeedInMperS(buttKmh?.isChecked, buttMph?.isChecked, newVal, npHours, npMinutes)
                }
            }
            val initialTrackTimesPOJO = getTrackTimesPOJO(buttKmh?.isChecked,
                    buttMph?.isChecked, npSpeed?.value
                    ?: 10, trackDetailProvider.getLengthInM().toFloat())
            updateDuration(initialTrackTimesPOJO, npHours, npMinutes)

            val buttonOKSpeed: Button? = mView?.findViewById(R.id.content_speed_picker_speedBTNOk)
            buttonOKSpeed?.setOnClickListener {
                val value: Float = npSpeed?.value?.toFloat() ?: SPEED_DEFAULT_M_S
                val valueMperS: Float = if (buttKmh != null && buttKmh.isChecked) value.kmhToMS()
                else if (buttMph != null && buttMph.isChecked) value.mphToMS()
                else value.kmhToMS()

                val action = actionProvider.getOkAction()
                action.invoke(valueMperS)

                dialog.dismiss()
            }
        } else {
            Log.e("SpeedPickerFragment", "init error")
        }
        return mView
    }

    private fun setCurrentSelectedSpeedInMperS(kmhChecked: Boolean?, mphChecked: Boolean?, pickerVal: Int,
                                               npHours: NumberPicker?, npMinutes: NumberPicker?) {
        val inMperS = if (kmhChecked != null && kmhChecked) pickerVal.toFloat().kmhToMS()
        else if (mphChecked != null && mphChecked) pickerVal.toFloat().mphToMS()
        else pickerVal.toFloat().kmhToMS()
        val extraData: TrackTimesPOJO = getTrackTimesPOJO(kmhChecked, mphChecked, pickerVal, trackDetailProvider.getLengthInM().toFloat())
        updateDuration(extraData, npHours, npMinutes)
        lastSelectedProvider.setSpeedMperS(inMperS) // accessor clamps
    }

    private fun readStoredSpeedInUnit(kmhChecked: Boolean?, mphChecked: Boolean?): Int = if (kmhChecked != null && kmhChecked)
        clampSpeed(speedMperSToKmh(lastSelectedProvider.getSpeedMperS()))
    else if (mphChecked != null && mphChecked)
        clampSpeed(speedMperSToMph(lastSelectedProvider.getSpeedMperS()))
    else clampSpeed(speedMperSToKmh(lastSelectedProvider.getSpeedMperS()))

    private fun updateDuration(extraData: TrackTimesPOJO, npHours: NumberPicker?, npMinutes: NumberPicker?) {
        if (extraData.hours in 0..MAX_HOURS_PICKER) npHours?.value = extraData.hours
        if (extraData.minutes in 0..59)npMinutes?.value = extraData.minutes
    }

}