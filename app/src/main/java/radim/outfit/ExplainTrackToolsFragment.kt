package radim.outfit

import android.support.v4.app.DialogFragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class ExplainTrackToolsFragment: DialogFragment(){

    //override fun onAttach(context: Context) {
    //    super.onAttach(context)
    //}

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.explain_track_tools, container, false)
    }

    //override fun onStart() {
    //    super.onStart()
    //}
}