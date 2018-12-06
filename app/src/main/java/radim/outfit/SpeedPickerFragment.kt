package radim.outfit

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.NumberPicker
import android.widget.RadioButton

const val SPEED_MIN = 1
const val SPEED_MAX = 130
const val SPEED_DEFAULT = 14

class SpeedPickerFragment : DialogFragment() {

    companion object {
        fun newInstance(okAction: (Float) -> Unit,
                        dismissedAction: () -> Unit,
                        whenCreatedAction: () -> Unit): SpeedPickerFragment {
            val instance = SpeedPickerFragment()
            //instance.okAction = okAction
            //instance.dismissedAction = dismissedAction
            //instance.whenCreatedAction = whenCreatedAction
            return instance
        }
    }

    //private lateinit var okAction: (Float) -> Unit
    //private lateinit var dismissedAction: () -> Unit
    //private lateinit var whenCreatedAction: () -> Unit

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
        //TODO remember position
        np.value = SPEED_DEFAULT
        np.wrapSelectorWheel = false
        val buttonKmh: RadioButton = view.findViewById(R.id.speedFragButtonKmh)
        buttonKmh.isChecked = true
        val buttonOK: Button = view.findViewById(R.id.speedFragButtonOK)
        dialog.setOnDismissListener{
            //dismissedAction
        }
        buttonOK.setOnClickListener {
            val value = np.value.toFloat()
            //TODO mph vs km/h to m/s
            //okAction(value)
            dialog.dismiss()
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //whenCreatedAction
    }
}