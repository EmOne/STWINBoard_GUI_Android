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

package com.st.STWINBoard_Gui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.text.InputFilter;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.st.BlueSTSDK.Feature;
import com.st.BlueSTSDK.Features.FeatureHSDatalogConfig;
import com.st.BlueSTSDK.HSDatalog.Device;
import com.st.BlueSTSDK.HSDatalog.Sensor;
import com.st.BlueSTSDK.HSDatalog.SensorStatus;
import com.st.BlueSTSDK.HSDatalog.StatusParam;
import com.st.BlueSTSDK.HSDatalog.SubSensorStatus;
import com.st.BlueSTSDK.Node;
import com.st.STWINBoard_Gui.Control.DeviceManager;
import com.st.STWINBoard_Gui.HSDTaggingFragment.HSDInteractionCallback;
import com.st.STWINBoard_Gui.Utils.SensorViewAdapter;
import com.st.clab.stwin.gui.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;

/**
 *
 */

public class HSDConfigFragment extends Fragment {

    private static String STWIN_CONFIG_FRAGMENT_TAG = HSDConfigFragment.class.getName()+".STWIN_CONFIG_FRAGMENT_TAG";
    private static int PICKFILE_REQUEST_CODE = 7777;

    private DeviceManager deviceManager;

    private RecyclerView recyclerView;
    private TextView mDeviceAlias;
    private TextView mDeviceSerialNumber;
    private SensorViewAdapter mSensorsAdapter;

    private Button mLoadConfigButton;
    private Button mSaveConfigButton;
    private Button mTagButton;

    private LinearLayout mTaggingMaskView;
    private LinearLayout mMaskView;
    private ImageView dataImageView;
    private Animation mDataTransferAnimation;

    MenuItem stopMenuItem;
    MenuItem startMenuItem;

    Dialog wifiConfDialog;
    EditText ssid;
    EditText psswd;


    LoadConfTask mLoadConfTask;

    private FeatureHSDatalogConfig mSTWINConf;

    /**
     * listener for the STWIN Conf feature, it will
     *
     */
    private final Feature.FeatureListener mSTWINConfListener = (f, sample) -> {
        String command = ((FeatureHSDatalogConfig)f).getCommand(sample);
        //Log.e("STWINConfigFragment","command: " + command);
        JSONObject jsonObj = null;
        try {
            jsonObj = new JSONObject(command);
            Iterator<String> keys = jsonObj.keys();
            String firstKey = keys.next();
            //Log.e("STWINConfigFragment","firstKey: " + firstKey);

            switch (firstKey) {
                case "device":
                    mLoadConfTask = new LoadConfTask();
                    mLoadConfTask.execute(jsonObj);
                    break;
                case "deviceInfo":
                    //Empty
                    break;
                case "id":
                    //Empty
                    break;
                case "register":
                    //Empty
                    break;
                case "command":
                    switch (jsonObj.getString(firstKey)) {
                        case "STATUS":
                            String type = jsonObj.getString("type");
                            if(!type.equals("performance"))
                                Log.e("TEST","type: " + type);
                            switch (type){
                                //NOTE unHide to enable WiFi configuration dialog (unHide also the menu item)
                                /*case "network":
                                if(wifiConfDialog!=null){
                                    String ssidValue = jsonObj.getString("ssid");
                                    String psswdValue = jsonObj.getString("password");
                                    String ipValue = jsonObj.getString("ip");
                                    boolean ischecked = !ipValue.equals("unavailable");
                                    updateGui(() -> {
                                        EditText ssid = wifiConfDialog.findViewById(R.id.stwin_wifi_ssid);
                                        EditText psswd = wifiConfDialog.findViewById(R.id.stwin_wifi_password);
                                        Switch wifiSwitch = wifiConfDialog.findViewById(R.id.stwin_wifi_switch);
                                        TextView ip = wifiConfDialog.findViewById(R.id.stwin_wifi_ip_value);
                                        ssid.setText(ssidValue);
                                        psswd.setText(psswdValue);
                                        wifiSwitch.setChecked(ischecked);
                                        ip.setText(ipValue);
                                    });
                                }
                                break;*/
                                case "logstatus":
                                    boolean isLogging = jsonObj.getBoolean("isLogging");
                                    deviceManager.setIsLogging(isLogging);
                                    //NOTE - check this
                                    //updateGui(() -> {
                                        startMenuItem.setVisible(!isLogging);
                                        stopMenuItem.setVisible(isLogging);
                                        if(isLogging) {
                                            //TODO remove this animation
                                            obscureConfig(mMaskView, dataImageView);
                                            //TODO get tagList
                                            //TODO get device and set tagList
                                            //TODO call openTaggingFragment();
                                        }
                                        else
                                            unobscureConfig(mMaskView,dataImageView);
                                    //});
                                    break;
                            }
                            break;
                    }
                    break;
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    };

    //NOTE if for some reason it will be necessary to hide a sensor, update the following lambda
    //private SensorViewAdapter.FilterSensor mFilterSensor = s -> true;
    //NOTE if for some reason it will be necessary to hide a subsensor, update the following lambda
    //private SubSensorViewAdapter.FilterSubSensor mFilterSubSensor = ssc -> true;

    private void manageSensorSpinnerSelection(int sensorId, @NonNull String paramName, @NonNull String value){
        if(deviceManager.getSensorStatusParam(sensorId, paramName) != null &&
                !deviceManager.getSensorStatusParam(sensorId, paramName).equals(value)) {
            String jsonSetMessage = deviceManager.createSetSensorStatusParamCommand(sensorId, paramName, value);
            deviceManager.encapsulateAndSend(jsonSetMessage);
        }
    }

    private void manageSubSensorSpinnerSelection(int sensorId, @NonNull Integer subSensorId, @NonNull String paramName, @NonNull String value){
        if(deviceManager.getSubSensorStatusParam(sensorId,subSensorId, paramName) != null &&
                !deviceManager.getSubSensorStatusParam(sensorId,subSensorId, paramName).equals(value)) {
            String jsonSetMessage = deviceManager.createSetSubSensorStatusParamCommand(sensorId, subSensorId, paramName, value);
            deviceManager.encapsulateAndSend(jsonSetMessage);
        }
    }

    private void manageSensorSwitchClicked(int sensorId){
        deviceManager.updateSensorIsActiveModel(sensorId);
        String jsonSetMessage = deviceManager.createSetSensorIsActiveCommand(sensorId);
        deviceManager.encapsulateAndSend(jsonSetMessage);
    }

    private void manageSubSensorIconClicked(int sensorId, @NonNull Integer subSensorId){
        if(deviceManager.getDeviceModel().getSensor(sensorId).getSensorStatus().isActive()) {
            deviceManager.updateSubSensorIsActiveModel(sensorId,subSensorId);
            String jsonSetMessage = deviceManager.createSetSubSensorIsActiveCommand(sensorId, subSensorId);
            deviceManager.encapsulateAndSend(jsonSetMessage);
        }
    }

    private void manageSensorEditTextChanged(int sensorId, @NonNull String paramName, @NonNull String value){
        if(!deviceManager.getSensorStatusParam(sensorId, paramName).equals(value)) {
            String jsonSetMessage = deviceManager.createSetSensorStatusParamCommand(sensorId, paramName, value);
            deviceManager.encapsulateAndSend(jsonSetMessage);
        }
    }

    private void manageSubSensorEditTextChanged(int sensorId, @NonNull Integer subSensorId, @NonNull String paramName, @NonNull String value){
        if(!deviceManager.getSubSensorStatusParam(sensorId,subSensorId, paramName).equals(value)) {
            String jsonSetMessage = deviceManager.createSetSubSensorStatusParamCommand(sensorId, subSensorId, paramName, value);
            deviceManager.encapsulateAndSend(jsonSetMessage);
        }
    }

    private HSDInteractionCallback mHSDTagFragmentCallbacks = new HSDInteractionCallback() {
        @Override
        public void onBackClicked(Device device) {
            unobscureConfig(mTaggingMaskView,null);
            if(deviceManager.isLogging())
                obscureConfig(mMaskView, dataImageView);
            deviceManager.setDevice(device);
            //Log.e("HSDInteractionCallback","onDoneClicked: " + device.toString());
        }

        @Override
        public void onStartLogClicked(Device device) {
            deviceManager.setIsLogging(true);
            //Log.e("HSDInteractionCallback","onStartLogClicked: " + device.toString());
        }

        @Override
        public void onStopLogClicked(Device device) {
            unobscureConfig(mMaskView,dataImageView);
            deviceManager.setIsLogging(false);
            deviceManager.setDevice(device);
            //Log.e("HSDInteractionCallback","onStopLogClicked: " + device.toString());
        }
    };

    //NOTE /////////////////////////////////////////////////////////////////////////////////////////////

    private String iStreamToString(InputStream is) throws IOException {
        InputStreamReader isReader = new InputStreamReader(is);
        //Creating a BufferedReader object
        BufferedReader reader = new BufferedReader(isReader);
        StringBuffer sb = new StringBuffer();
        String str;
        while((str = reader.readLine())!= null){
            sb.append(str);
        }
        return sb.toString();
    }

    public class LoadJSONTask extends AsyncTask<Uri, Void, JSONObject> {
        @Override
        protected JSONObject doInBackground(Uri... uris) {
            Uri jsonUri = uris[0];
            JSONObject jsonObject;
            InputStream inputStream = null;
            try {
                inputStream = getActivity().getContentResolver().openInputStream(jsonUri);
                jsonObject = new JSONObject(iStreamToString(inputStream));
                inputStream.close();
                Log.e("TAG","jsonObject obtained!!!!");
                return jsonObject;
            } catch (JSONException | IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject jsonObject) {
            super.onPostExecute(jsonObject);
            mLoadConfTask = new LoadConfTask();
            mLoadConfTask.execute(jsonObject);
        }
    }

    public class SendConfTask extends AsyncTask<DeviceManager, Void, DeviceManager>{
        @Override
        protected DeviceManager doInBackground(DeviceManager... deviceManagers) {
            return deviceManagers[0];
        }

        @Override
        protected void onPostExecute(DeviceManager dm) {
            super.onPostExecute(dm);

            Device dev = dm.getDeviceModel();
            String message;
            for (Sensor s: dev.getSensors()) {
                SensorStatus ss = s.getSensorStatus();
                message = dm.createSetSensorIsActiveCommand(s.getId(),ss.isActive());
                deviceManager.encapsulateAndSend(message);
                for (StatusParam sp : s.getSensorStatus().getStatusParams()) {
                    message = dm.createSetSensorStatusParamCommand(s.getId(),sp.getName(),sp.getValue());
                    deviceManager.encapsulateAndSend(message);
                }
                for (SubSensorStatus sss : s.getSensorStatus().getSubSensorStatusList()){
                    message = dm.createSetSubSensorIsActiveCommand(s.getId(),sss.getId(),sss.isActive());
                    deviceManager.encapsulateAndSend(message);
                    for(StatusParam ssp : sss.getParams()){
                        message = dm.createSetSubSensorStatusParamCommand(s.getId(),sss.getId(),ssp.getName(),ssp.getValue());
                        deviceManager.encapsulateAndSend(message);
                    }
                }
            }
        }
    }

    public class LoadConfTask extends AsyncTask<JSONObject, Void, DeviceManager> {
        @Override
        protected DeviceManager doInBackground(JSONObject... jsonObjects) {
            JSONObject loadedJson = jsonObjects[0];
            try {
                deviceManager.setDevice(loadedJson);
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
            return deviceManager;
        }

        @Override
        protected void onPostExecute(DeviceManager dm) {
            super.onPostExecute(dm);

            String checkModelEM = dm.checkModel();
            if(checkModelEM != null){
                //Dialog
                AlertDialog alertDialog = new AlertDialog.Builder(getContext()).create();
                alertDialog.setTitle("Loaded JSON Model Error");
                alertDialog.setMessage(checkModelEM);
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        (dialog, which) -> dialog.dismiss());
                alertDialog.show();
            } else {

                mSensorsAdapter = new SensorViewAdapter(
                        getContext(),
                        R.layout.sensor_item,
                        dm.getDeviceModel().getSensors(),
                        HSDConfigFragment.this::manageSensorSwitchClicked,
                        HSDConfigFragment.this::manageSensorSpinnerSelection,
                        HSDConfigFragment.this::manageSensorEditTextChanged,
                        HSDConfigFragment.this::manageSubSensorIconClicked,
                        HSDConfigFragment.this::manageSubSensorSpinnerSelection,
                        HSDConfigFragment.this::manageSubSensorEditTextChanged
                );
                // Set the adapter
                recyclerView.setAdapter(mSensorsAdapter);

                //NOTE - check this
                //updateGui(() -> {
                    mDeviceAlias.setText(dm.getDeviceModel().getDeviceInfo().getAlias());
                    mDeviceSerialNumber.setText(dm.getDeviceModel().getDeviceInfo().getSerialNumber());
                    mSensorsAdapter.notifyDataSetChanged();
                //});

                SendConfTask mSendConfTask = new SendConfTask();
                mSendConfTask.execute(dm);
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }
    }

    private void openJSONSelector(){
        Intent chooserFile = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        chooserFile.addCategory(Intent.CATEGORY_DEFAULT);
        CharSequence chooserTitle = "Load a configuration JSON";
        chooserFile.setType("application/octet-stream");
        startActivityForResult(Intent.createChooser(chooserFile, chooserTitle), PICKFILE_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Uri jsonUri = null;
        if (requestCode == PICKFILE_REQUEST_CODE
                && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                jsonUri = data.getData();
            }
            //Log.e("TEST", "JSON URI= " + jsonUri);
            LoadJSONTask loadJSONTask = new LoadJSONTask();
            loadJSONTask.execute(jsonUri);
        } else if(requestCode == CREATE_FILE
                && resultCode == Activity.RESULT_OK){
            if (data != null) {
                jsonUri = data.getData();
            }
            ParcelFileDescriptor pfd;
            try {
                if (jsonUri != null) {
                    pfd = getActivity().getContentResolver().openFileDescriptor(jsonUri, "w");
                    FileOutputStream fileOutputStream = new FileOutputStream(pfd.getFileDescriptor());
                    fileOutputStream.write(String.valueOf(deviceManager.getJSONfromDevice()).getBytes());
                    fileOutputStream.close();
                    pfd.close();

                    LoadConfTask loadConfTask = new LoadConfTask();
                    loadConfTask.execute(deviceManager.getJSONfromDevice());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    //NOTE /////////////////////////////////////////////////////////////////////////////////////////////


    @Override
    public void onResume() {
        super.onResume();
        if(deviceManager != null && deviceManager.getDeviceModel() != null) {
            LoadConfTask loadConfTask = new LoadConfTask();
            loadConfTask.execute(deviceManager.getJSONfromDevice());
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        deviceManager = new DeviceManager();
    }

    private void obscureConfig(View maskLayout, ImageView animImage){
        maskLayout.setVisibility(View.VISIBLE);
        maskLayout.requestFocus();
        maskLayout.setClickable(true);
        if(animImage != null)
            animImage.startAnimation(mDataTransferAnimation);
    }

    private void unobscureConfig(View maskLayout, ImageView animImage){
        if(animImage != null)
            animImage.clearAnimation();
        mDataTransferAnimation.cancel();
        mDataTransferAnimation.reset();
        maskLayout.setVisibility(View.INVISIBLE);
        maskLayout.setClickable(false);
    }

    private static final int CREATE_FILE = 1;
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_stwin_config, container, false);

        mLoadConfigButton = root.findViewById(R.id.loadConfButton);
        mLoadConfigButton.setOnClickListener(view -> openJSONSelector());

        mSaveConfigButton = root.findViewById(R.id.saveConfButton);
        mSaveConfigButton.setOnClickListener(view -> {
            showSaveDialog();
        });

        mTagButton = root.findViewById(R.id.tagButton);
        mTagButton.setOnClickListener(view -> {
            obscureConfig(mTaggingMaskView,null);
            openTaggingFragment();
        });

        mDeviceAlias = root.findViewById(R.id.deviceAlias);
        mDeviceAlias.setOnLongClickListener(view -> {
            showChangeAliasDialog(getContext(),mDeviceAlias);
            return true;
        });
        mDeviceSerialNumber = root.findViewById(R.id.deviceSerialNumber);
        recyclerView = root.findViewById(R.id.sensors_list);

        mTaggingMaskView = root.findViewById(R.id.start_log_mask);
        mMaskView = root.findViewById(R.id.animation_mask);
        dataImageView = root.findViewById(R.id.ongoingLogImageView);
        mDataTransferAnimation = AnimationUtils.loadAnimation(getActivity().getApplicationContext(), R.anim.move_right_full);
        unobscureConfig(mTaggingMaskView,dataImageView);

        return root;
    }

    static int getPxfromDp(Resources res, int yourdp){
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                yourdp,
                res.getDisplayMetrics()
        );
    }

    static void setEditTextMaxLength(EditText editText, int length) {
        InputFilter[] FilterArray = new InputFilter[1];
        FilterArray[0] = new InputFilter.LengthFilter(length);
        editText.setFilters(FilterArray);
    }

    private void showChangeAliasDialog(Context context, TextView mDeviceAlias) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("change your device alias");
        EditText editText = new EditText(context);
        LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        editText.setText(mDeviceAlias.getText());
        editText.setLayoutParams(elp);
        setEditTextMaxLength(editText,10);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        LinearLayout layout = new LinearLayout(context);
        layout.setLayoutParams(lp);
        int px = getPxfromDp(getResources(),22);
        layout.setPadding(px,px,px,0);
        layout.addView(editText);
        builder.setView(layout);
        builder.setNegativeButton("CANCEL",null);
        builder.setPositiveButton("OK", (dialogInterface, i) -> {
            String newAlias = editText.getText().toString();
            mDeviceAlias.setText(newAlias);
            deviceManager.setDeviceAlias(newAlias);
            String changeAliasMessage = deviceManager.createSetDeviceAliasCommand(newAlias);
            deviceManager.encapsulateAndSend(changeAliasMessage);
        });
        builder.show();
    }


    protected void enableNeededNotification(@NonNull Node node) {
        mSTWINConf = node.getFeature(FeatureHSDatalogConfig.class);
        //NOTE new STWINConf char
        if(mSTWINConf != null){
            mSTWINConf.addFeatureListener(mSTWINConfListener);
            boolean test = node.enableNotification(mSTWINConf);
            Log.e("TEST","notifEnabled: " + test);

            deviceManager.setHSDFeature(mSTWINConf);
            String jsonGetDeviceMessage = deviceManager.createGetDeviceCommand();
            deviceManager.encapsulateAndSend(jsonGetDeviceMessage);
        }
    }


    protected void disableNeedNotification(@NonNull Node node) {
        if(mSTWINConf!=null) {
            mSTWINConf.removeFeatureListener(mSTWINConfListener);
            mSTWINConf.disableNotification();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.findItem(R.id.startLog).setVisible(false);
        inflater.inflate(R.menu.menu_stwin_hs_datalog, menu);

        stopMenuItem = menu.findItem(R.id.menu_stopSTWIN_HS_Datalog);
        startMenuItem = menu.findItem(R.id.menu_startSTWIN_HS_Datalog);

        startMenuItem.getActionView().setOnClickListener(view -> {
            startMenuItem.setVisible(false);
            stopMenuItem.setVisible(true);
            String jsonStartMessage = deviceManager.createStartCommand();
            deviceManager.encapsulateAndSend(jsonStartMessage);
            deviceManager.setIsLogging(true);
            obscureConfig(mTaggingMaskView,null);

            Log.e("STWINConfigFragment","START TAG PRESSED!!!!");
            openTaggingFragment();

        });

        stopMenuItem.getActionView().setOnClickListener(view -> {
            startMenuItem.setVisible(true);
            stopMenuItem.setVisible(false);
            deviceManager.setIsLogging(false);
            unobscureConfig(mMaskView,dataImageView);
            String jsonStopMessage = deviceManager.createStopCommand();
            deviceManager.encapsulateAndSend(jsonStopMessage);
        });

        startMenuItem.setVisible(!deviceManager.isLogging());
        stopMenuItem.setVisible(deviceManager.isLogging());

        super.onCreateOptionsMenu(menu, inflater);
    }

    private void openTaggingFragment(){
        FragmentManager fm = getChildFragmentManager();
        HSDTaggingFragment frag = HSDTaggingFragment.newInstance();
        frag.setOnDoneCLickedCallback(mHSDTagFragmentCallbacks);
        Bundle bundle = new Bundle();
        bundle.putParcelable("Device", deviceManager.getDeviceModel());
        bundle.putParcelable("Feature", mSTWINConf);
        bundle.putBoolean("IsLogging", deviceManager.isLogging());
        frag.setArguments(bundle);
        fm.beginTransaction()
                .add(R.id.start_log_mask,frag,STWIN_CONFIG_FRAGMENT_TAG)
                .addToBackStack(null)
                .commit();
    }

    //NOTE unHide to enable WiFi configuration dialog (unHide also the menu item)
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.menu_wifiConfSTWIN_HS_Datalog){
            wifiConfDialog = new Dialog(getContext());
            wifiConfDialog.setContentView(R.layout.stwin_dialog_wifi_conf);

            // set the custom dialog components - text, image and button
            EditText ssid = wifiConfDialog.findViewById(R.id.stwin_wifi_ssid);
            EditText psswd = wifiConfDialog.findViewById(R.id.stwin_wifi_password);
            Switch wifiSwitch = wifiConfDialog.findViewById(R.id.stwin_wifi_switch);

            Button sendConfigButton = wifiConfDialog.findViewById(R.id.stwin_wifi_sendConfButton);
            // if button is clicked, send currently written Wi-Fi credentials
            /*sendConfigButton.setOnClickListener(v -> {
                String jsonWiFiConfMessage =  deviceManager.createConfigWifiCredentialsCommand(ssid.getText().toString(),
                        psswd.getText().toString(),
                        wifiSwitch.isChecked());
                encapsulateAndSend(jsonWiFiConfMessage);
                wifiConfDialog.dismiss();
            });*/

            Button closeButton = wifiConfDialog.findViewById(R.id.stwin_wifi_cancelButton);
            // if button is clicked, close the custom dialog
            closeButton.setOnClickListener(v -> wifiConfDialog.dismiss());

            /* Switch wifiSwitch = wifiConfDialog.findViewById(R.id.stwin_wifi_switch);
            wifiSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
                String jsonWiFiConfMessage =  deviceManager.createWifiOnOffCommand(b);
                encapsulateAndSend(jsonWiFiConfMessage);
            });*/

            wifiConfDialog.show();
            /*DisplayMetrics metrics = getResources().getDisplayMetrics();
            int width = metrics.widthPixels;
            wifiConfDialog.getWindow().setLayout((6 * width)/7, ViewGroup.LayoutParams.WRAP_CONTENT);

            String message = deviceManager.createGETWiFiConfCommand();
            encapsulateAndSend(message);*/
        }
        return super.onOptionsItemSelected(item);
    }

    private void showSaveDialog(){
        final Dialog dialog = new Dialog(getContext());
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.hsd_save_dialog);

        Switch localSwitch = dialog.findViewById(R.id.hsd_save_local_switch);
        Switch boardSwitch = dialog.findViewById(R.id.hsd_save_board_switch);
        Button saveButton = dialog.findViewById(R.id.hsd_save_button);

        localSwitch.setOnClickListener(view -> {
            if (localSwitch.isChecked()){
                saveButton.setEnabled(true);
            } else {
                if (!boardSwitch.isChecked()){
                    saveButton.setEnabled(false);
                }
            }
        });

        boardSwitch.setOnClickListener(view -> {
            if (boardSwitch.isChecked()){
                saveButton.setEnabled(true);
            } else {
                if (!localSwitch.isChecked()){
                    saveButton.setEnabled(false);
                }
            }
        });

        saveButton.setOnClickListener(view -> {
            dialog.dismiss();
            if (localSwitch.isChecked()){
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/octet-stream");
                intent.putExtra(Intent.EXTRA_TITLE, "STWIN_conf.json");
                startActivityForResult(intent, CREATE_FILE);
            }
            if (boardSwitch.isChecked()){
                //TODO send save config on board command (ToBeDefined)
                Log.e("STWINConfigFragment","Save Config on the board!");
            }
        });
        Button closeButton = dialog.findViewById(R.id.hsd_close_button);
        closeButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}
