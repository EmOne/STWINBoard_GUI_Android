/*
 * Copyright (c) 2017  STMicroelectronics – All rights reserved
 * The STMicroelectronics corporate logo is a trademark of STMicroelectronics
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions
 *   and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of
 *   conditions and the following disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * - Neither the name nor trademarks of STMicroelectronics International N.V. nor any other
 *   STMicroelectronics company nor the names of its contributors may be used to endorse or
 *   promote products derived from this software without specific prior written permission.
 *
 * - All of the icons, pictures, logos and other images that are provided with the source code
 *   in a directory whose title begins with st_images may only be used for internal purposes and
 *   shall not be redistributed to any third party or modified in any way.
 *
 * - Any redistributions in binary form shall not include the capability to display any of the
 *   icons, pictures, logos and other images that are provided with the source code in a directory
 *   whose title begins with st_images.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGE.
 */
package com.st.STWINBoard_Gui

import android.app.Activity
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.st.BlueSTSDK.Features.highSpeedDataLog.communication.DeviceParser
import com.st.BlueSTSDK.Manager
import com.st.BlueSTSDK.Node
import com.st.STWINBoard_Gui.Utils.SensorViewAdapter
import com.st.clab.stwin.gui.R

/**
 *
 */
open class HSDConfigFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView

    private var mTaggingMaskView: LinearLayout? = null
    private var mMaskView: LinearLayout? = null
    private var dataImageView: ImageView? = null
    private var mDataTransferAnimation: Animation? = null

    private val viewModel by viewModels<HSDConfigViewModel>()

    private val mSensorsAdapter = SensorViewAdapter(
            onSubSensorODRChange = {sensor, subSensor, newOdrValue ->
                viewModel.changeODRValue(sensor,subSensor,newOdrValue)
            },
            onSubSensorFullScaleChange = {sensor, subSensor, newFSValue ->
                viewModel.changeFullScale(sensor,subSensor,newFSValue)
            },
            onSubSensorSampleChange = {sensor, subSensor, newSampleValue ->
                viewModel.changeSampleForTimeStamp(sensor,subSensor,newSampleValue)
            },
            onSubSubSensorEnableStatusChange = {sensor, subSensor, newState ->
                viewModel.changeEnableState(sensor,subSensor,newState)
            })


    private fun requestConfigurationFile() {
        val chooserFile = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/json"))
            type = PICKFILE_REQUEST_TYPE
        }
        val chooserTitle = getString(R.string.hsdl_configFileChooserTitle)
        startActivityForResult(Intent.createChooser(chooserFile, chooserTitle), PICKFILE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            PICKFILE_REQUEST_CODE -> {
                val fileUri = data?.data
                if (resultCode == Activity.RESULT_OK) {
                    viewModel.loadConfigFromFile(fileUri,requireContext().contentResolver)
                }
            }
            CREATE_FILE_REQUEST_CODE -> {
                val fileUri = data?.data
                if (resultCode == Activity.RESULT_OK) {
                    viewModel.storeConfigToFile(fileUri,requireContext().contentResolver)
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    private fun unobscureConfig(maskLayout: View?, animImage: ImageView?) {
        animImage?.clearAnimation()
        mDataTransferAnimation!!.cancel()
        mDataTransferAnimation!!.reset()
        maskLayout!!.visibility = View.INVISIBLE
        maskLayout.isClickable = false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_stwin_config, container, false)

        root.findViewById<View>(R.id.loadConfButton).setOnClickListener {
            requestConfigurationFile()
        }

        root.findViewById<View>(R.id.saveConfButton).setOnClickListener {
            showSaveDialog()
        }

        recyclerView = root.findViewById(R.id.sensors_list)
        recyclerView.adapter = mSensorsAdapter

        mTaggingMaskView = root.findViewById(R.id.start_log_mask)
        mMaskView = root.findViewById(R.id.animation_mask)
        dataImageView = root.findViewById(R.id.ongoingLogImageView)
        mDataTransferAnimation = AnimationUtils.loadAnimation(requireContext().applicationContext, R.anim.move_right_full)
        unobscureConfig(mTaggingMaskView, dataImageView)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.error.observe(viewLifecycleOwner, Observer { error ->
            if(error!=null)
                displayErrorMessage(error)
        })
        viewModel.sensorsConfiguraiton.observe(viewLifecycleOwner, Observer {
            mSensorsAdapter.submitList(it)
        })
    }

    private fun displayErrorMessage(error: HSDConfigViewModel.Error) {
        Snackbar.make(requireView(),error.toStringRes,Snackbar.LENGTH_SHORT)
                .show()
    }

    private fun getNode():Node?{
        return arguments?.getString(NODE_TAG_EXTRA)?.let {
            Manager.getSharedInstance().getNodeWithTag(it)
        }
    }

    override fun onStart() {
        super.onStart()
        val node = getNode()
        if(node!=null){
            enableNeededNotification(node)
        }
    }

    override fun onStop() {
        super.onStop()
        Log.d("HSDConf","STOP")
        val node = getNode()
        if(node!=null){
            disableNeedNotification(node)
        }
    }

    fun enableNeededNotification(node: Node) {
        viewModel.enableNotificationFromNode(node)
    }

     fun disableNeedNotification(node: Node) {
        viewModel.disableNotificationFromNode(node)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_stwin_hs_datalog,menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.menu_hsdl_wifiConf -> {
                val node = getNode()
                if(node!=null) {
                    val wifSettings = WiFiConfigureDialogFragment.newInstance(node)
                    wifSettings.show(childFragmentManager, WIFI_CONFIG_FRAGMENT_TAG)
                }
                true
            }
            R.id.menu_hsdl_changeAlias -> {
                val node = getNode()
                if(node!=null) {
                    val aliasSettings = BoardAliasConfDialogFragment.newInstance(node)
                    aliasSettings.show(childFragmentManager, ALIAS_CONFIG_FRAGMENT_TAG)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showSaveDialog() {
        HSDConfigSaveDialogFragment().show(childFragmentManager,"saveDialog")
    }

    companion object {
        private val WIFI_CONFIG_FRAGMENT_TAG = HSDConfigFragment::class.java.name + ".WIFI_CONFIG_FRAGMENT"
        private val ALIAS_CONFIG_FRAGMENT_TAG = HSDConfigFragment::class.java.name + ".ALIAS_CONFIG_FRAGMENT"
        private const val PICKFILE_REQUEST_CODE = 7777
        private const val PICKFILE_REQUEST_TYPE = "application/octet-stream"
        private const val DEFAULT_CONFI_NAME = "STWIN_conf.json"
        private val NODE_TAG_EXTRA = HSDConfigFragment::class.java.name + ".NODE_TAG_EXTRA"

        fun newInstance(node: Node): Fragment {
            return HSDConfigFragment().apply {
                arguments = Bundle().apply {
                    putString(NODE_TAG_EXTRA,node.tag)
                }
            }
        }

        private const val CREATE_FILE_REQUEST_CODE = 1

        private val ACTION_CONFIG_COMPLETE = HSDConfigFragment::class.java.name + ".ACTION_CONFIG_COMPLETE"
        private val ACTION_CONFIG_EXTRA = HSDConfigFragment::class.java.name + ".ACTION_CONFIG_EXTRA"
        fun buildConfigCompleteIntentFilter(): IntentFilter {
            return IntentFilter().apply {
                addAction(ACTION_CONFIG_COMPLETE)
            }
        }

        fun extractSavedConfig(intent: Intent?):String?{
            return if(intent?.action == ACTION_CONFIG_COMPLETE){
                intent.getStringExtra(ACTION_CONFIG_EXTRA)
            }else{
                null
            }
        }

        internal fun buildConfigCompletedEvent(newConfig: String?):Intent{
            return Intent(ACTION_CONFIG_COMPLETE).apply {
                if(newConfig!=null)
                    putExtra(ACTION_CONFIG_EXTRA, newConfig)
            }
        }

    }

    private val HSDConfigViewModel.Error.toStringRes:Int
        get() = when(this){
            HSDConfigViewModel.Error.InvalidFile -> R.string.hsdl_error_invalidFile
            HSDConfigViewModel.Error.FileNotFound -> R.string.hsdl_error_fileNotFound
            HSDConfigViewModel.Error.ImpossibleReadFile -> R.string.hsdl_error_readError
            HSDConfigViewModel.Error.ImpossibleWriteFile -> R.string.hsdl_error_writeError
            HSDConfigViewModel.Error.ImpossibleCreateFile -> R.string.hsdl_error_createError
        }

}

