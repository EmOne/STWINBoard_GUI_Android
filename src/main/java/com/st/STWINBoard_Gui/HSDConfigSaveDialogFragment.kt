package com.st.STWINBoard_Gui

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.st.BlueSTSDK.Features.highSpeedDataLog.communication.DeviceParser
import com.st.clab.stwin.gui.R
import kotlinx.android.parcel.Parcelize

internal class HSDConfigSaveDialogFragment : DialogFragment(){

    private val viewModel by viewModels<HSDConfigViewModel> ({requireParentFragment()})

    @Parcelize
    private data class SelectionStatus(
            var storeLocalCopy:Boolean = false,
            var setAsDefault:Boolean = false
    ):Parcelable

    private lateinit var currentStatus:SelectionStatus

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        currentStatus = savedInstanceState?.getParcelable(SELECTION_STATUS) ?: SelectionStatus()
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

    private fun onSaveClicked(){
        if(currentStatus.setAsDefault){
            viewModel.setCurrentConfAsDefault()
        }
        if(currentStatus.storeLocalCopy){
            requestFileCreation()
        }else{
            sendConfigCompleteEvent()
            dismiss()
        }
    }

    private fun sendConfigCompleteEvent(){
        val newConfig = viewModel.sensorsConfiguraiton.value?.let { DeviceParser.toJsonStr(it) }
        LocalBroadcastManager.getInstance(requireContext())
                .sendBroadcast(HSDConfigFragment.buildConfigCompletedEvent(newConfig))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(SELECTION_STATUS,currentStatus)
    }

    private fun requestFileCreation(){
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = PICKFILE_REQUEST_TYPE
            putExtra(Intent.EXTRA_TITLE, DEFAULT_CONFI_NAME)
        }
        startActivityForResult(intent, CREATE_FILE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            CREATE_FILE_REQUEST_CODE -> {
                val fileUri = data?.data
                if (resultCode == Activity.RESULT_OK) {
                    viewModel.storeConfigToFile(fileUri,requireContext().contentResolver)
                    sendConfigCompleteEvent()
                    dismiss()
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    companion object{
        private val SELECTION_STATUS = HSDConfigSaveDialogFragment::class.java.name+".SELECTION_STATUS"
        private const val CREATE_FILE_REQUEST_CODE = 1
        private const val PICKFILE_REQUEST_TYPE = "application/json"
        private const val DEFAULT_CONFI_NAME = "STWIN_conf.json"
    }

}