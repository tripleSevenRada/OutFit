package radim.outfit

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class SpeedPickerFragment : DialogFragment() {

    companion object {
        fun newInstance(): SpeedPickerFragment {
            return SpeedPickerFragment()
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.speed_picker_fragment, container, false)
    }
}