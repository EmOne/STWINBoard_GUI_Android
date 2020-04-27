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
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.view.View.OnLongClickListener
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.st.BlueSTSDK.Feature
import com.st.BlueSTSDK.Feature.FeatureListener
import com.st.BlueSTSDK.Features.FeatureHSDatalogConfig
import com.st.BlueSTSDK.HSDatalog.Device
import com.st.BlueSTSDK.Node
import com.st.BlueSTSDK.gui.demos.DemoFragment
import com.st.STWINBoard_Gui.Control.DeviceManager
import com.st.STWINBoard_Gui.HSDTaggingFragment.HSDInteractionCallback
import com.st.STWINBoard_Gui.Utils.SensorViewAdapter
import com.st.STWINBoard_Gui.Utils.SensorViewAdapter.*
import com.st.clab.stwin.gui.R
import org.json.JSONException
import org.json.JSONObject
import java.io.*

/**
 *
 */
open class HSDConfigFragment : DemoFragment() {
    private var deviceManager: DeviceManager? = null
    private var recyclerView: RecyclerView? = null
    private var mDeviceAlias: TextView? = null
    private var mDeviceSerialNumber: TextView? = null
    private var mSensorsAdapter: SensorViewAdapter? = null
    private var mLoadConfigButton: Button? = null
    private var mSaveConfigButton: Button? = null
    private var mTagButton: Button? = null
    private var mTaggingMaskView: LinearLayout? = null
    private var mMaskView: LinearLayout? = null
    private var dataImageView: ImageView? = null
    private var mDataTransferAnimation: Animation? = null
    var stopMenuItem: MenuItem? = null
    var startMenuItem: MenuItem? = null
    var wifiConfDialog: Dialog? = null
    var ssid: EditText? = null
    var psswd: EditText? = null
    var mLoadConfTask: LoadConfTask? = null
    private var mSTWINConf: FeatureHSDatalogConfig? = null

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

    private val mHSDTagFragmentCallbacks: HSDInteractionCallback = object : HSDInteractionCallback {
        override fun onBackClicked(device: Device) {
            unobscureConfig(mTaggingMaskView, null)
            if (deviceManager!!.isLogging) obscureConfig(mMaskView, dataImageView)
            deviceManager!!.setDevice(device)
            //Log.e("HSDInteractionCallback","onDoneClicked: " + device.toString());
        }

        override fun onStartLogClicked(device: Device) {
            deviceManager!!.setIsLogging(true)
            //Log.e("HSDInteractionCallback","onStartLogClicked: " + device.toString());
        }

        override fun onStopLogClicked(device: Device) {
            unobscureConfig(mMaskView, dataImageView)
            deviceManager!!.setIsLogging(false)
            deviceManager!!.setDevice(device)
            //Log.e("HSDInteractionCallback","onStopLogClicked: " + device.toString());
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
                        dm.deviceModel.sensors, OnSensorSwitchClickedListener { sensorId: Int -> manageSensorSwitchClicked(sensorId) }, OnSensorSpinnerValueSelectedListener { sensorId: Int, paramName: String, value: String -> manageSensorSpinnerSelection(sensorId, paramName, value) }, OnSensorEditTextChangedListener { sensorId: Int, paramName: String, value: String -> manageSensorEditTextChanged(sensorId, paramName, value) }, OnSubSensorIconClickedListener { sensorId: Int, subSensorId: Int -> manageSubSensorIconClicked(sensorId, subSensorId) }, OnSubSensorSpinnerValueSelectedListener { sensorId: Int, subSensorId: Int, paramName: String, value: String -> manageSubSensorSpinnerSelection(sensorId, subSensorId, paramName, value) }, OnSubSensorEditTextChangedListener { sensorId: Int, subSensorId: Int, paramName: String, value: String -> manageSubSensorEditTextChanged(sensorId, subSensorId, paramName, value) })
                // Set the adapter
                recyclerView!!.adapter = mSensorsAdapter

                //NOTE - check this
                //updateGui(() -> {
                mDeviceAlias!!.text = dm.deviceModel.deviceInfo.alias
                mDeviceSerialNumber!!.text = dm.deviceModel.deviceInfo.serialNumber
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

    private fun openJSONSelector() {
        val chooserFile = Intent(Intent.ACTION_OPEN_DOCUMENT)
        chooserFile.addCategory(Intent.CATEGORY_DEFAULT)
        val chooserTitle: CharSequence = "Load a configuration JSON"
        chooserFile.type = "application/octet-stream"
        startActivityForResult(Intent.createChooser(chooserFile, chooserTitle), PICKFILE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        var jsonUri: Uri? = null
        if (requestCode == PICKFILE_REQUEST_CODE
                && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                jsonUri = data.data
            }
            //Log.e("TEST", "JSON URI= " + jsonUri);
            val loadJSONTask = LoadJSONTask()
            loadJSONTask.execute(jsonUri)
        } else if (requestCode == CREATE_FILE
                && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                jsonUri = data.data
            }
            val pfd: ParcelFileDescriptor?
            try {
                if (jsonUri != null) {
                    pfd = requireContext().contentResolver.openFileDescriptor(jsonUri, "w")
                    val fileOutputStream = FileOutputStream(pfd!!.fileDescriptor)
                    fileOutputStream.write(deviceManager!!.jsoNfromDevice.toString().toByteArray())
                    fileOutputStream.close()
                    pfd.close()
                    val loadConfTask = LoadConfTask()
                    loadConfTask.execute(deviceManager!!.jsoNfromDevice)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
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
        mLoadConfigButton = root.findViewById(R.id.loadConfButton)
        mLoadConfigButton?.setOnClickListener(View.OnClickListener { view: View? -> openJSONSelector() })
        mSaveConfigButton = root.findViewById(R.id.saveConfButton)
        mSaveConfigButton?.setOnClickListener(View.OnClickListener { view: View? -> showSaveDialog() })
        mTagButton = root.findViewById(R.id.tagButton)
        mTagButton?.setOnClickListener(View.OnClickListener { view: View? ->
            obscureConfig(mTaggingMaskView, null)
            openTaggingFragment()
        })
        mDeviceAlias = root.findViewById(R.id.deviceAlias)
        mDeviceAlias?.setOnLongClickListener(OnLongClickListener { view: View? ->
            showChangeAliasDialog(context, mDeviceAlias)
            true
        })
        mDeviceSerialNumber = root.findViewById(R.id.deviceSerialNumber)
        recyclerView = root.findViewById(R.id.sensors_list)
        mTaggingMaskView = root.findViewById(R.id.start_log_mask)
        mMaskView = root.findViewById(R.id.animation_mask)
        dataImageView = root.findViewById(R.id.ongoingLogImageView)
        mDataTransferAnimation = AnimationUtils.loadAnimation(activity!!.applicationContext, R.anim.move_right_full)
        unobscureConfig(mTaggingMaskView, dataImageView)
        return root
    }

    private fun showChangeAliasDialog(context: Context?, mDeviceAlias: TextView?) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("change your device alias")
        val editText = EditText(context)
        val elp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        editText.setText(mDeviceAlias!!.text)
        editText.layoutParams = elp
        setEditTextMaxLength(editText, 10)
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        val layout = LinearLayout(context)
        layout.layoutParams = lp
        val px = getPxfromDp(resources, 22)
        layout.setPadding(px, px, px, 0)
        layout.addView(editText)
        builder.setView(layout)
        builder.setNegativeButton("CANCEL", null)
        builder.setPositiveButton("OK") { dialogInterface: DialogInterface?, i: Int ->
            val newAlias = editText.text.toString()
            mDeviceAlias.text = newAlias
            deviceManager!!.setDeviceAlias(newAlias)
            val changeAliasMessage = deviceManager!!.createSetDeviceAliasCommand(newAlias)
            deviceManager!!.encapsulateAndSend(changeAliasMessage)
        }
        builder.show()
    }

    override fun enableNeededNotification(node: Node) {
        mSTWINConf = node.getFeature(FeatureHSDatalogConfig::class.java)
        //NOTE new STWINConf char
        if (mSTWINConf != null) {
            mSTWINConf!!.addFeatureListener(mSTWINConfListener)
            val test = node.enableNotification(mSTWINConf)
            Log.e("TEST", "notifEnabled: $test")
            deviceManager!!.setHSDFeature(mSTWINConf)
            val jsonGetDeviceMessage = deviceManager!!.createGetDeviceCommand()
            deviceManager!!.encapsulateAndSend(jsonGetDeviceMessage)
        }
    }

    override fun disableNeedNotification(node: Node) {
        if (mSTWINConf != null) {
            mSTWINConf!!.removeFeatureListener(mSTWINConfListener)
            mSTWINConf!!.disableNotification()
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
            openTaggingFragment()
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

    private fun openTaggingFragment() {
        val fm = childFragmentManager
        val frag = HSDTaggingFragment.newInstance()
        frag.setOnDoneCLickedCallback(mHSDTagFragmentCallbacks)
        val bundle = Bundle()
        bundle.putParcelable("Device", deviceManager!!.deviceModel)
        bundle.putParcelable("Feature", mSTWINConf)
        bundle.putBoolean("IsLogging", deviceManager!!.isLogging)
        frag.arguments = bundle
        fm.beginTransaction()
                .add(R.id.start_log_mask, frag, STWIN_CONFIG_FRAGMENT_TAG)
                .addToBackStack(null)
                .commit()
    }

    //NOTE unHide to enable WiFi configuration dialog (unHide also the menu item)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.menu_wifiConfSTWIN_HS_Datalog) {
            wifiConfDialog = Dialog(requireContext())
            wifiConfDialog!!.setContentView(R.layout.stwin_dialog_wifi_conf)

            // set the custom dialog components - text, image and button
            val ssid = wifiConfDialog!!.findViewById<EditText>(R.id.stwin_wifi_ssid)
            val psswd = wifiConfDialog!!.findViewById<EditText>(R.id.stwin_wifi_password)
            val wifiSwitch = wifiConfDialog!!.findViewById<Switch>(R.id.stwin_wifi_switch)
            val sendConfigButton = wifiConfDialog!!.findViewById<Button>(R.id.stwin_wifi_sendConfButton)
            // if button is clicked, send currently written Wi-Fi credentials
            /*sendConfigButton.setOnClickListener(v -> {
                String jsonWiFiConfMessage =  deviceManager.createConfigWifiCredentialsCommand(ssid.getText().toString(),
                        psswd.getText().toString(),
                        wifiSwitch.isChecked());
                encapsulateAndSend(jsonWiFiConfMessage);
                wifiConfDialog.dismiss();
            });*/
            val closeButton = wifiConfDialog!!.findViewById<Button>(R.id.stwin_wifi_cancelButton)
            // if button is clicked, close the custom dialog
            closeButton.setOnClickListener { v: View? -> wifiConfDialog!!.dismiss() }

            /* Switch wifiSwitch = wifiConfDialog.findViewById(R.id.stwin_wifi_switch);
            wifiSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
                String jsonWiFiConfMessage =  deviceManager.createWifiOnOffCommand(b);
                encapsulateAndSend(jsonWiFiConfMessage);
            });*/wifiConfDialog!!.show()
            /*DisplayMetrics metrics = getResources().getDisplayMetrics();
            int width = metrics.widthPixels;
            wifiConfDialog.getWindow().setLayout((6 * width)/7, ViewGroup.LayoutParams.WRAP_CONTENT);

            String message = deviceManager.createGETWiFiConfCommand();
            encapsulateAndSend(message);*/
        }
        return super.onOptionsItemSelected(item)
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
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.type = "application/octet-stream"
                intent.putExtra(Intent.EXTRA_TITLE, "STWIN_conf.json")
                startActivityForResult(intent, CREATE_FILE)
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
        private val STWIN_CONFIG_FRAGMENT_TAG = HSDConfigFragment::class.java.name + ".STWIN_CONFIG_FRAGMENT_TAG"
        private const val PICKFILE_REQUEST_CODE = 7777

        fun newInstance(node: Node): Fragment {
            return HSDConfigFragment()
        }

        private const val CREATE_FILE = 1
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
}