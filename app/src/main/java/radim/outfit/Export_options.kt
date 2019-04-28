package radim.outfit

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
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
    fun getBundleDist(): String
    fun setBundleDist(value: String)
    fun onExportOptionsDismiss()
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
    private var bundleDistPick: NumberPicker? = null
    private var moveCheckBox: CheckBox? = null
    private var bundleCheckBox: CheckBox? = null

    // BUNDLE
    private val bundlePickMinValue = 0
    private val bundlePickMaxValue = 16
    private val bundlePickRange = IntRange(bundlePickMinValue, bundlePickMaxValue)

    private val bundlePickerPositionToValue = mapIntString(bundlePickRange, 4)
    private val bundlePickerValueToPosition = mapStringInt(bundlePickRange, 4)
    private val bundlePickerDisplayedValues = stringArray(bundlePickRange, 4)

    // MOVE
    private val movePickMinValue = 0
    private val movePickMaxValue = 7
    private val movePickRange = IntRange(movePickMinValue, movePickMaxValue)

    private val movePickerPositionToValue = mapIntString(movePickRange, 1)
    private val movePickerValueToPosition = mapStringInt(movePickRange, 1)
    private val movePickerDisplayedValues = stringArray(movePickRange, 1)

    private fun mapStringInt(range: IntRange, offset: Int): Map<String, Int>{
        // "10" to 0, "20" to 1
        val data = mutableMapOf<String, Int>()
        for (i in range){ data["${i+offset}0"] = i }
        return data
    }
    private fun mapIntString(range: IntRange, offset: Int): Map<Int, String>{
        // 0 to "10", 1 to "20"
        val data = mutableMapOf<Int, String>()
        for (i in range){ data[i] = "${i+offset}0" }
        return data
    }
    private fun stringArray(range: IntRange, offset: Int): Array<String>{
        // "10", "20"
        val data = mutableListOf<String>()
        for (i in range) data.add("${i+offset}0")
        return data.toTypedArray()
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val mView = inflater.inflate(R.layout.fragment_export_options, container, false)

        moveDistPick = mView?.findViewById(R.id.export_options_fragmentNPMove)
        moveDistPick?.wrapSelectorWheel = false
        moveDistPick?.minValue = movePickMinValue
        moveDistPick?.maxValue = movePickMaxValue
        moveDistPick?.displayedValues = movePickerDisplayedValues
        moveDistPick?.value = movePickerValueToPosition[exportOptionsDataProvider.getMoveDist()]?: 0
        moveDistPick?.setOnValueChangedListener { _,_, newVal ->
            exportOptionsDataProvider.setMoveDist(movePickerPositionToValue[newVal]?: "10")
        }
        moveDistPick?.isEnabled = exportOptionsDataProvider.getMove()

        bundleDistPick = mView?.findViewById(R.id.export_options_fragmentNPBundle)
        bundleDistPick?.wrapSelectorWheel = false
        bundleDistPick?.minValue = bundlePickMinValue
        bundleDistPick?.maxValue = bundlePickMaxValue
        bundleDistPick?.displayedValues = bundlePickerDisplayedValues
        bundleDistPick?.value = bundlePickerValueToPosition[exportOptionsDataProvider.getBundleDist()]?: 0
        bundleDistPick?.setOnValueChangedListener { _,_, newVal ->
            exportOptionsDataProvider.setBundleDist(bundlePickerPositionToValue[newVal]?: "80")
        }
        bundleDistPick?.isEnabled = exportOptionsDataProvider.getBundle()

        moveCheckBox = mView?.findViewById(R.id.export_options_fragmentCHCKBMove)
        moveCheckBox?.setOnClickListener{
            moveDistPick?.isEnabled = moveCheckBox?.isChecked?: false
            exportOptionsDataProvider.setMove(moveCheckBox?.isChecked?: false)
        }
        moveCheckBox?.isChecked = exportOptionsDataProvider.getMove()

        bundleCheckBox = mView?.findViewById(R.id.export_options_fragmentCHCKBBundle)
        bundleCheckBox?.setOnClickListener{
            bundleDistPick?.isEnabled = bundleCheckBox?.isChecked?: false
            exportOptionsDataProvider.setBundle(bundleCheckBox?.isChecked?: true)
        }
        bundleCheckBox?.isChecked = exportOptionsDataProvider.getBundle()

        return mView
    }

    override fun onDismiss(dialog: DialogInterface?) {
        super.onDismiss(dialog)
        exportOptionsDataProvider.onExportOptionsDismiss()
    }
}