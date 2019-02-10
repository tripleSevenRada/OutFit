package radim.outfit

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment

class IQAppIsInvalidDialogFragment : DialogFragment() {

    // https://drive.google.com/open?id=1FKkw-kk3oXjaHCp9Z4muFLhNSxb_wAXc

    // Use this instance of the interface to deliver action events
    private lateinit var mListener: IQAppIsInvalidDialogListener

    /* The activity that creates an instance of this dialog fragment must
    implement this interface in order to receive event callbacks.
    Each method passes the DialogFragment in case the host needs to query it.
    */
    interface IQAppIsInvalidDialogListener {
        fun onDialogPositiveClick(dialog: DialogFragment)
        fun onDialogNegativeClick(dialog: DialogFragment)
        fun onDialogNeutralClick(dialog: DialogFragment)
        fun setDialogVisible(visible: Boolean)
        fun getDialogVisible(): Boolean
        fun setDialogType(type: DialogType)
        fun getDialogType(): DialogType?
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the IQAppIsInvalidDialogListener so we can send events to the host
            mListener = context as IQAppIsInvalidDialogListener
        } catch (e: ClassCastException) {
            // The activity doesn't implement the interface, throw exception
            throw ClassCastException((context.toString() +
                    " must implement IQAppIsInvalidDialogListener"))
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreate(savedInstanceState)
        return activity?.let {
            val message = arguments?.getString("message")?: "message"
            val positive = arguments?.getString("positive")?: ""
            val negative = arguments?.getString("negative")?: ""
            val neutral = arguments?.getString("neutral")?: ""
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(it)
            builder.setMessage(message)
            if(positive.isNotEmpty())
                builder.setPositiveButton(positive)
                    { _, _ ->
                        mListener.onDialogPositiveClick(this)
                        mListener.setDialogVisible(false)
                    }
            if(negative.isNotEmpty())
                builder.setNegativeButton(negative)
                    { _, _ ->
                        mListener.onDialogNegativeClick(this)
                        mListener.setDialogVisible(false)
                    }
            if(neutral.isNotEmpty())
                builder.setNeutralButton(neutral)
                    { _, _ ->
                        mListener.onDialogNeutralClick(this)
                        mListener.setDialogVisible(false)
                    }
            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onCancel(dialog: DialogInterface?) {
        super.onCancel(dialog)
        mListener.setDialogVisible(false)
    }
}

sealed class DialogType {
    data class NotInstalled(val description: String) : DialogType()
    data class OldVersion(val description: String) : DialogType()
}