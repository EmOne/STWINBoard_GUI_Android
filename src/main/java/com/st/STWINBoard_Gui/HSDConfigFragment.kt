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
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.st.BlueSTSDK.Feature
import com.st.BlueSTSDK.Feature.FeatureListener
import com.st.BlueSTSDK.Features.FeatureHSDatalogConfig
import com.st.BlueSTSDK.HSDatalog.Device
import com.st.BlueSTSDK.Manager
import com.st.BlueSTSDK.Node
import com.st.STWINBoard_Gui.Control.DeviceManager
import logger.HSDTaggingFragment.HSDInteractionCallback
import com.st.STWINBoard_Gui.Utils.SensorViewAdapter
import com.st.STWINBoard_Gui.Utils.SensorViewAdapter.*
import com.st.clab.stwin.gui.R
import org.json.JSONException
import org.json.JSONObject
import java.io.*

/**
 *
 */
open class HSDConfigFragment : Fragment() {
    private var deviceManager: DeviceManager? = null
    private var recyclerView: RecyclerView? = null
    private var mSensorsAdapter: SensorViewAdapter? = null
    private var mTaggingMaskView: LinearLayout? = null
    private var mMaskView: LinearLayout? = null
    private var dataImageView: ImageView? = null
    private var mDataTransferAnimation: Animation? = null
    var stopMenuItem: MenuItem? = null
    var startMenuItem: MenuItem? = null
    var mLoadConfTask: LoadConfTask? = null
    private var mSTWINConf: FeatureHSDatalogConfig? = null

    private val viewModel by viewModels<HSDConfigViewModel>()

    /**
     * listener for the STWIN Conf feature, it will
     *
     */
    private val mSTWINConfListener = FeatureListener { f: Feature, sample: Feature.Sample? ->
        val command = (f as FeatureHSDatalogConfig).getCommand(sample)
        //Log.e("STWINConfigFragment","command: " + command);
        var jsonObj: JSONObject? = null
        try {
            jsonObj = JSONObject(command)
            val keys = jsonObj.keys()
            val firstKey = keys.next()
            when (firstKey) {
                "device" -> {
                    mLoadConfTask = LoadConfTask()
                    mLoadConfTask!!.execute(jsonObj)
                }
                "deviceInfo" -> {
                }
                "id" -> {
                }
                "register" -> {
                }
                "command" -> when (jsonObj.getString(firstKey)) {
                    "STATUS" -> {
                        val type = jsonObj.getString("type")
                        if (type != "performance") Log.e("TEST", "type: $type")
                        when (type) {
                            "logstatus" -> {
                                val isLogging = jsonObj.getBoolean("isLogging")
                                deviceManager!!.setIsLogging(isLogging)
                                //NOTE - check this
                                //updateGui(() -> {
                                startMenuItem!!.isVisible = !isLogging
                                stopMenuItem!!.isVisible = isLogging
                                if (isLogging) {
                                    //TODO remove this animation
                                    obscureConfig(mMaskView, dataImageView)
                                    //TODO get tagList
                                    //TODO get device and set tagList
                                    //TODO call openTaggingFragment();
                                } else unobscureConfig(mMaskView, dataImageView)
                            }
                        }
                    }
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    //NOTE if for some reason it will be necessary to hide a sensor, update the following lambda
    //private SensorViewAdapter.FilterSensor mFilterSensor = s -> true;
    //NOTE if for some reason it will be necessary to hide a subsensor, update the following lambda
    //private SubSensorViewAdapter.FilterSubSensor mFilterSubSensor = ssc -> true;
    private fun manageSensorSpinnerSelection(sensorId: Int, paramName: String, value: String) {
        if (deviceManager!!.getSensorStatusParam(sensorId, paramName) != null &&
                deviceManager!!.getSensorStatusParam(sensorId, paramName) != value) {
            val jsonSetMessage = deviceManager!!.createSetSensorStatusParamCommand(sensorId, paramName, value)
            deviceManager!!.encapsulateAndSend(jsonSetMessage)
        }
    }

    private fun manageSubSensorSpinnerSelection(sensorId: Int, subSensorId: Int, paramName: String, value: String) {
        if (deviceManager!!.getSubSensorStatusParam(sensorId, subSensorId, paramName) != null &&
                deviceManager!!.getSubSensorStatusParam(sensorId, subSensorId, paramName) != value) {
            val jsonSetMessage = deviceManager!!.createSetSubSensorStatusParamCommand(sensorId, subSensorId, paramName, value)
            deviceManager!!.encapsulateAndSend(jsonSetMessage)
        }
    }

    private fun manageSensorSwitchClicked(sensorId: Int) {
        deviceManager!!.updateSensorIsActiveModel(sensorId)
        val jsonSetMessage = deviceManager!!.createSetSensorIsActiveCommand(sensorId)
        deviceManager!!.encapsulateAndSend(jsonSetMessage)
    }

    private fun manageSubSensorIconClicked(sensorId: Int, subSensorId: Int) {
        if (deviceManager!!.deviceModel.getSensor(sensorId)!!.sensorStatus.isActive) {
            deviceManager!!.updateSubSensorIsActiveModel(sensorId, subSensorId)
            val jsonSetMessage = deviceManager!!.createSetSubSensorIsActiveCommand(sensorId, subSensorId)
            deviceManager!!.encapsulateAndSend(jsonSetMessage)
        }
    }

    private fun manageSensorEditTextChanged(sensorId: Int, paramName: String, value: String) {
        if (deviceManager!!.getSensorStatusParam(sensorId, paramName) != value) {
            val jsonSetMessage = deviceManager!!.createSetSensorStatusParamCommand(sensorId, paramName, value)
            deviceManager!!.encapsulateAndSend(jsonSetMessage)
        }
    }

    private fun manageSubSensorEditTextChanged(sensorId: Int, subSensorId: Int, paramName: String, value: String) {
        if (deviceManager!!.getSubSensorStatusParam(sensorId, subSensorId, paramName) != value) {
            val jsonSetMessage = deviceManager!!.createSetSubSensorStatusParamCommand(sensorId, subSensorId, paramName, value)
            deviceManager!!.encapsulateAndSend(jsonSetMessage)
        }
    }

    //NOTE /////////////////////////////////////////////////////////////////////////////////////////////
    @Throws(IOException::class)
    private fun iStreamToString(`is`: InputStream?): String {
        val isReader = InputStreamReader(`is`)
        //Creating a BufferedReader object
        val reader = BufferedReader(isReader)
        val sb = StringBuffer()
        var str: String?
        while (reader.readLine().also { str = it } != null) {
            sb.append(str)
        }
        return sb.toString()
    }

    inner class LoadJSONTask : AsyncTask<Uri, Void?, JSONObject?>() {
        override fun doInBackground(vararg uris: Uri): JSONObject? {
            val jsonUri = uris[0]
            val jsonObject: JSONObject
            var inputStream: InputStream? = null
            try {
                inputStream = activity!!.contentResolver.openInputStream(jsonUri)
                jsonObject = JSONObject(iStreamToString(inputStream))
                inputStream!!.close()
                Log.e("TAG", "jsonObject obtained!!!!")
                return jsonObject
            } catch (e: JSONException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return null
        }

        override fun onPostExecute(jsonObject: JSONObject?) {
            super.onPostExecute(jsonObject)
            mLoadConfTask = LoadConfTask()
            mLoadConfTask!!.execute(jsonObject)
        }
    }

    inner class SendConfTask : AsyncTask<DeviceManager, Void, DeviceManager>() {
        override fun doInBackground(vararg deviceManagers: DeviceManager): DeviceManager {
            return deviceManagers[0]
        }

        override fun onPostExecute(dm: DeviceManager) {
            super.onPostExecute(dm)
            val dev = dm.deviceModel
            var message: String?
            for (s in dev.sensors) {
                val ss = s.sensorStatus
                message = dm.createSetSensorIsActiveCommand(s.id, ss.isActive)
                deviceManager!!.encapsulateAndSend(message)
                for ((name, value) in s.sensorStatus.statusParams) {
                    message = dm.createSetSensorStatusParamCommand(s.id, name, value)
                    deviceManager!!.encapsulateAndSend(message)
                }
                for (sss in s.sensorStatus.subSensorStatusList) {
                    message = dm.createSetSubSensorIsActiveCommand(s.id, sss.id, sss.isActive)
                    deviceManager!!.encapsulateAndSend(message)
                    for ((name, value) in sss.params) {
                        message = dm.createSetSubSensorStatusParamCommand(s.id, sss.id, name, value)
                        deviceManager!!.encapsulateAndSend(message)
                    }
                }
            }
        }

    }

    inner class LoadConfTask : AsyncTask<JSONObject, Void?, DeviceManager?>() {
        override fun doInBackground(vararg jsonObjects: JSONObject): DeviceManager? {
            val loadedJson = jsonObjects[0]
            try {
                deviceManager!!.setDevice(loadedJson)
            } catch (e: JSONException) {
                e.printStackTrace()
                return null
            }
            return deviceManager
        }

        override fun onPostExecute(dm: DeviceManager?) {
            super.onPostExecute(dm)
            val checkModelEM = dm!!.checkModel()
            if (checkModelEM != null) {
                //Dialog
                val alertDialog = AlertDialog.Builder(context).create()
                alertDialog.setTitle("Loaded JSON Model Error")
                alertDialog.setMessage(checkModelEM)
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK"
                ) { dialog: DialogInterface, which: Int -> dialog.dismiss() }
                alertDialog.show()
            } else {
                mSensorsAdapter = SensorViewAdapter(
                        context!!,
                        R.layout.sensor_item,
                        dm.deviceModel.sensors,
                        object : OnSensorSwitchClickedListener{
                            override fun onSensorSwitchClicked(sensorId: Int) {
                                Log.d(TAG,"onSensorSwitchClicked $sensorId")
                                manageSensorSwitchClicked(sensorId)
                            }
                        } ,
                        object : OnSensorSpinnerValueSelectedListener {
                            override fun onSpinnerValueSelected(sensorId: Int, paramName: String, value: String) {
                                Log.d(TAG,"onSpinnerValueSelected $sensorId : $paramName:$value")
                                manageSensorSpinnerSelection(sensorId, paramName, value)
                            }
                        },
                        object : OnSensorEditTextChangedListener {
                            override fun onEditTextValueChanged(sensorId: Int, paramName: String, value: String) {
                                Log.d(TAG,"onEditTextValueChanged $sensorId -> $paramName:$value")
                                manageSensorEditTextChanged(sensorId, paramName, value)
                            }
                        },
                        object : OnSubSensorIconClickedListener {
                            override fun onSubSensorIconClicked(sensorId: Int, subSensorId: Int) {
                                Log.d(TAG,"onSubSensorIconClicked $sensorId - $subSensorId")
                                manageSubSensorIconClicked(sensorId, subSensorId)
                            }
                        },
                        object : OnSubSensorSpinnerValueSelectedListener {
                            override fun onSpinnerValueSelected(sensorId: Int, subSensorId: Int?, paramName: String, value: String) {
                                Log.d(TAG,"onSpinnerValueSelected $sensorId - $subSensorId $paramName:$value")
                                manageSubSensorSpinnerSelection(sensorId, subSensorId!!, paramName, value)
                            }
                        },
                        object : OnSubSensorEditTextChangedListener {
                            override fun onSubSensorEditTextValueChanged(sensorId: Int, subSensorId: Int?, paramName: String, value: String) {
                                Log.d(TAG,"onSubSensorEditTextValueChanged $sensorId - $subSensorId $paramName:$value")
                                manageSubSensorEditTextChanged(sensorId, subSensorId!!, paramName, value)
                            }
                        }
                )
                // Set the adapter
                recyclerView!!.adapter = mSensorsAdapter

                //NOTE - check this
                //updateGui(() -> {
                mSensorsAdapter!!.notifyDataSetChanged()
                //});
                val mSendConfTask = SendConfTask()
                mSendConfTask.execute(dm)
            }
        }

        override fun onCancelled() {
            super.onCancelled()
        }
    }

    private fun requestConfigurationFile() {
        val chooserFile = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
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

    //NOTE /////////////////////////////////////////////////////////////////////////////////////////////
    override fun onResume() {
        super.onResume()
        if (deviceManager != null && deviceManager!!.deviceModel != null) {
            val loadConfTask = LoadConfTask()
            loadConfTask.execute(deviceManager!!.jsoNfromDevice)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        deviceManager = DeviceManager()
    }

    private fun obscureConfig(maskLayout: View?, animImage: ImageView?) {
        maskLayout!!.visibility = View.VISIBLE
        maskLayout.requestFocus()
        maskLayout.isClickable = true
        animImage?.startAnimation(mDataTransferAnimation)
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

    private lateinit var mNode: Node

     fun enableNeededNotification(node: Node) {
         mNode = node;
        mSTWINConf = node.getFeature(FeatureHSDatalogConfig::class.java)
        //NOTE new STWINConf char
        mSTWINConf?.apply {
            addFeatureListener(mSTWINConfListener)
            enableNotification()
            Log.e("TEST", "notifEnabled")
            deviceManager!!.setHSDFeature(this)
            val jsonGetDeviceMessage = deviceManager!!.createGetDeviceCommand()
            deviceManager!!.encapsulateAndSend(jsonGetDeviceMessage)
        }
    }

     fun disableNeedNotification(node: Node) {
        mSTWINConf?.apply {
            removeFeatureListener(mSTWINConfListener)
            disableNotification()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.findItem(R.id.startLog).isVisible = false
        inflater.inflate(R.menu.menu_stwin_hs_datalog, menu)
        stopMenuItem = menu.findItem(R.id.menu_stopSTWIN_HS_Datalog)
        startMenuItem = menu.findItem(R.id.menu_startSTWIN_HS_Datalog)
        startMenuItem?.getActionView()?.setOnClickListener { view: View? ->
            startMenuItem?.setVisible(false)
            stopMenuItem?.setVisible(true)
            val jsonStartMessage = deviceManager!!.createStartCommand()
            deviceManager!!.encapsulateAndSend(jsonStartMessage)
            deviceManager!!.setIsLogging(true)
            obscureConfig(mTaggingMaskView, null)
            Log.e("STWINConfigFragment", "START TAG PRESSED!!!!")
        }
        stopMenuItem?.getActionView()?.setOnClickListener { view: View? ->
            startMenuItem?.setVisible(true)
            stopMenuItem?.setVisible(false)
            deviceManager!!.setIsLogging(false)
            unobscureConfig(mMaskView, dataImageView)
            val jsonStopMessage = deviceManager!!.createStopCommand()
            deviceManager!!.encapsulateAndSend(jsonStopMessage)
        }
        startMenuItem?.setVisible(!deviceManager!!.isLogging)
        stopMenuItem?.setVisible(deviceManager!!.isLogging)
        super.onCreateOptionsMenu(menu, inflater)
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
        val dialog = Dialog(requireContext())
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.hsd_save_dialog)
        val localSwitch = dialog.findViewById<Switch>(R.id.hsd_save_local_switch)
        val boardSwitch = dialog.findViewById<Switch>(R.id.hsd_save_board_switch)
        val saveButton = dialog.findViewById<Button>(R.id.hsd_save_button)
        localSwitch.setOnClickListener { view: View? ->
            if (localSwitch.isChecked) {
                saveButton.isEnabled = true
            } else {
                if (!boardSwitch.isChecked) {
                    saveButton.isEnabled = false
                }
            }
        }
        boardSwitch.setOnClickListener { view: View? ->
            if (boardSwitch.isChecked) {
                saveButton.isEnabled = true
            } else {
                if (!localSwitch.isChecked) {
                    saveButton.isEnabled = false
                }
            }
        }
        saveButton.setOnClickListener { view: View? ->
            dialog.dismiss()
            if (localSwitch.isChecked) {
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = PICKFILE_REQUEST_TYPE
                    putExtra(Intent.EXTRA_TITLE, DEFAULT_CONFI_NAME)
                }
                startActivityForResult(intent, CREATE_FILE_REQUEST_CODE)
            }
            if (boardSwitch.isChecked) {
                //TODO send save config on board command (ToBeDefined)
                Log.e("STWINConfigFragment", "Save Config on the board!")
            }
        }
        val closeButton = dialog.findViewById<Button>(R.id.hsd_close_button)
        closeButton.setOnClickListener { v: View? -> dialog.dismiss() }
        dialog.show()
    }

    companion object {
        private val TAG = "HSDConfigFragment"
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
        @JvmStatic
        fun getPxfromDp(res: Resources, yourdp: Int): Int {
            return TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    yourdp.toFloat(),
                    res.displayMetrics
            ).toInt()
        }

        @JvmStatic
        fun setEditTextMaxLength(editText: EditText, length: Int) {
            val FilterArray = arrayOfNulls<InputFilter>(1)
            FilterArray[0] = LengthFilter(length)
            editText.filters = FilterArray
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

