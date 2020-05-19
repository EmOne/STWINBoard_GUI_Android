package com.st.STWINBoard_Gui

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.st.STWINBoard_Gui.Utils.SaveSettings
import com.st.clab.stwin.gui.R

internal class HSDConfigSaveDialogFragment : DialogFragment(){

    private lateinit var currentStatus: SaveSettings

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        currentStatus = savedInstanceState?.getParcelable(SELECTION_STATUS) ?: SaveSettings()
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(R.string.saveConf_title)

        val booleanStatus = booleanArrayOf(currentStatus.storeLocalCopy,currentStatus.setAsDefault)
        builder.setMultiChoiceItems(R.array.saveConf_choose,booleanStatus ){ dialog, which, isChecked ->
            when(which){
                0 -> currentStatus.storeLocalCopy = isChecked
                1 -> currentStatus.setAsDefault = isChecked
            }
        }

        builder.setPositiveButton(R.string.saveConf_save){_,_ -> onSaveClicked()}
        builder.setNegativeButton(R.string.saveConf_cancel){ _, _ -> dismiss()}

        return builder.create()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(SELECTION_STATUS,currentStatus)
    }


    private fun onSaveClicked() {
        parentFragment?.onActivityResult(targetRequestCode,Activity.RESULT_OK,
                encapsulateSettings(currentStatus)
        )
    }

    companion object{
        private val SELECTION_STATUS = HSDConfigSaveDialogFragment::class.java.name+".SELECTION_STATUS"

        fun extractSaveSettings(intent:Intent?): SaveSettings?{
            return if(intent?.hasExtra(SELECTION_STATUS) == true){
                intent.getParcelableExtra(SELECTION_STATUS)
            }else{
                null
            }
        }

        private fun encapsulateSettings(settings: SaveSettings):Intent{
            return Intent().apply {
                putExtra(SELECTION_STATUS,settings)
            }
        }
    }
}