package radim.outfit

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.NumberPicker

interface ExportOptionsDataProvider{
    fun getBundle(): Boolean
    fun setBundle(value: Boolean)
    fun getMove(): Boolean
    fun setMove(value: Boolean)
    fun getMoveDist(): String
    fun setMoveDist(value: String)
}

class ExportOptionsFragment: DialogFragment(){

    //TODO isInitialized
    private lateinit var exportOptionsDataProvider: ExportOptionsDataProvider

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is Activity) {
            exportOptionsDataProvider = context as ExportOptionsDataProvider
        } else {
            Log.e("EOF", "Activity expected, called by $context instead")
        }
    }

    private var moveDistPick: NumberPicker? = null
    private var moveCheckBox: CheckBox? = null
    private var bundleCheckBox: CheckBox? = null

    private val movePickerPositionToValue = mapOf( 0 to "10", 1 to "20", 2 to "30", 3 to "40", 4 to "50", 5 to "60", 6 to "70", 7 to "80" )
    private val movePickerValueToPosition = mapOf( "10" to 0, "20" to 1, "30" to 2, "40" to 3, "50" to 4, "60" to 5, "70" to 6, "80" to 7 )
    private val displayedValues = arrayOf("10","20","30","40","50","60","70","80")

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val mView = inflater.inflate(R.layout.fragment_export_options, container, false)

        moveDistPick = mView?.findViewById(R.id.export_options_fragmentNPMove)
        moveDistPick?.wrapSelectorWheel = false
        moveDistPick?.minValue = 0
        moveDistPick?.maxValue = 7
        moveDistPick?.displayedValues = displayedValues
        moveDistPick?.value = movePickerValueToPosition[exportOptionsDataProvider.getMoveDist()]?: 0
        moveDistPick?.setOnValueChangedListener { _,_, newVal ->
            exportOptionsDataProvider.setMoveDist(movePickerPositionToValue[newVal]?: "10")
        }
        moveDistPick?.isEnabled = exportOptionsDataProvider.getMove()

        moveCheckBox = mView?.findViewById(R.id.export_options_fragmentCHCKBMove)
        moveCheckBox?.setOnClickListener{
            moveDistPick?.isEnabled = moveCheckBox?.isChecked?: false
            exportOptionsDataProvider.setMove(moveCheckBox?.isChecked?: false)
        }
        moveCheckBox?.isChecked = exportOptionsDataProvider.getMove()

        bundleCheckBox = mView?.findViewById(R.id.export_options_fragmentCHCKBBundle)
        bundleCheckBox?.setOnClickListener{
            exportOptionsDataProvider.setBundle(bundleCheckBox?.isChecked?: true)
        }
        bundleCheckBox?.isChecked = exportOptionsDataProvider.getBundle()

        return mView
    }
}