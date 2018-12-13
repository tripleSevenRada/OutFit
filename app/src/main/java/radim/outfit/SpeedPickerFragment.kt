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

const val SPEED_MIN = 1
const val SPEED_MAX = 130
const val SPEED_DEFAULT = 14
const val DEFAULT_UNITS_RADIO_BUTTON_ID = R.id.speedFragButtonKmh

interface OkActionProvider {
    fun getOkAction(): (Float) -> Unit
}

interface LastSelectedValuesProvider {
    fun getSpeed(): Int
    fun setSpeed(value: Int)
    fun getUnitsButtonId(): Int
    fun setUnitsButtonId(id: Int)
}

// https://drive.google.com/file/d/1wwYzoPQts1HreDpS614oMAVyafU07ZYF/view?usp=sharing

class SpeedPickerFragment : DialogFragment() {

    private lateinit var actionProvider: OkActionProvider
    private lateinit var lastSelectedProvider: LastSelectedValuesProvider

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.speed_picker_fragment, container, false)
        val np: NumberPicker = view.findViewById(R.id.speedFragNP)
        np.minValue = SPEED_MIN
        np.maxValue = SPEED_MAX
        np.wrapSelectorWheel = false
        val buttKmh: RadioButton = view.findViewById(R.id.speedFragButtonKmh)
        val buttMph: RadioButton = view.findViewById(R.id.speedFragButtonMph)
        if (::lastSelectedProvider.isInitialized) {
            np.value = lastSelectedProvider.getSpeed()
            np.setOnValueChangedListener { _, _, newVal -> lastSelectedProvider.setSpeed(newVal) }
            val lastSelectedButton: RadioButton = view.findViewById(lastSelectedProvider.getUnitsButtonId())
            lastSelectedButton.isChecked = true
            buttKmh.setOnClickListener { lastSelectedProvider.setUnitsButtonId(R.id.speedFragButtonKmh) }
            buttMph.setOnClickListener { lastSelectedProvider.setUnitsButtonId(R.id.speedFragButtonMph) }
        } else Log.e("SpeedFrag","lateinit error 1")
        val buttonOK: Button = view.findViewById(R.id.speedFragButtonOk)
        buttonOK.setOnClickListener {
            val value = np.value.toFloat()
            val valueMperS = if(buttKmh.isChecked) value / 3.6F
            else value / 2.237F
            if (::actionProvider.isInitialized) {
                val action = actionProvider.getOkAction()
                action.invoke(valueMperS)
            } else Log.e("SpeedFrag","lateinit error 2")
            dialog.dismiss()
        }
        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is Activity) {
            actionProvider = context as OkActionProvider
            lastSelectedProvider = context as LastSelectedValuesProvider
        } else {
            Log.e("SpeedFrag", "Activity expected, called by $context instead")
        }
    }
}