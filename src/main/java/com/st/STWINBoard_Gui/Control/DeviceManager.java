package com.st.STWINBoard_Gui.Control;

import android.os.AsyncTask;
import android.util.Log;

import com.st.BlueSTSDK.Feature;
import com.st.BlueSTSDK.Features.FeatureHSDatalogConfig;
import com.st.BlueSTSDK.HSDatalog.Device;
import com.st.BlueSTSDK.HSDatalog.DescriptorParam;
import com.st.BlueSTSDK.HSDatalog.DeviceInfo;
import com.st.BlueSTSDK.HSDatalog.DeviceInfoKt;
import com.st.BlueSTSDK.HSDatalog.DeviceStats;
import com.st.BlueSTSDK.HSDatalog.Sensor;
import com.st.BlueSTSDK.HSDatalog.SensorDescriptor;
import com.st.BlueSTSDK.HSDatalog.SensorStatus;
import com.st.BlueSTSDK.HSDatalog.StatusParam;
import com.st.BlueSTSDK.HSDatalog.SubSensorDescriptor;
import com.st.BlueSTSDK.HSDatalog.SubSensorStatus;
import com.st.BlueSTSDK.HSDatalog.Tag;
import com.st.BlueSTSDK.HSDatalog.TagKt;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static com.st.BlueSTSDK.Debug.byteToString;
import static com.st.BlueSTSDK.Debug.stringToByte;

public class DeviceManager implements DeviceManagerInterface {

    private Device mDevice;
    private DeviceStats deviceStats;
    private Feature mHSDFeature;
    private Boolean isLogging;

    private static final String CURRENT_CONFIG_FILENAME = "currentConfig.json";

    private ByteArrayOutputStream currentMessage;

    /** Transport Protocol */
    private static final int MTU_SIZE = 20;
    private static final byte TP_START_PACKET = ((byte) (0x00));
    private static final byte TP_START_END_PACKET = ((byte) (0x20));
    private static final byte TP_MIDDLE_PACKET = ((byte) (0x40));
    private static final byte TP_END_PACKET = ((byte) (0x80));

    //NOTE constructor
    public DeviceManager(){
        this.mDevice = null;//new Device();
        this.deviceStats = new DeviceStats();
        this.mHSDFeature = null;
        this.isLogging = false;
    }

    //NOTE utils
    private ArrayList<Double> arrayListFromJSONArray(JSONArray jsonArray){
        ArrayList<Double> list = new ArrayList<>();
        if (jsonArray != null) {
            int len = jsonArray.length();
            for (int i=0;i<len;i++){
                try {
                    list.add(jsonArray.getDouble(i));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return list;
        }
        return null;
    }

    private <T extends Comparable<T>> boolean isEquals(List<T> list1, List<T> list2){
        if (list1 == null && list2 == null) {
            return true;
        }
        //Only one of them is null
        else if(list1 == null || list2 == null) {
            return false;
        }
        else if(list1.size() != list2.size()) {
            return false;
        }
        //copying to avoid rearranging original lists
        list1 = new ArrayList<T>(list1);
        list2 = new ArrayList<T>(list2);
        Collections.sort(list1);
        Collections.sort(list2);

        return list1.equals(list2);
    }

    //NOTE parsing
    private Device parseDevice(JSONObject jsonObj) throws JSONException {
        JSONObject jsonDevice = jsonObj.getJSONObject(DEVICE_JSON_KEY);
        Iterator<String> keys = jsonDevice.keys();
        ArrayList<Sensor> mSensors = new ArrayList<>();
        ArrayList<Tag> mTags = new ArrayList<>();
        //build a fake device info to put the error inside..
        DeviceInfo deviceInfo = new DeviceInfo("","",null,null,null,null,0,"Error in DeviceInfo parsing!");
        while(keys.hasNext()) {
            String key = keys.next();
            switch (key){
                case DEVICE_INFO_JSON_KEY:
                    deviceInfo = parseDeviceInfo(jsonDevice);
                    if (deviceInfo == null){
                        //TODO menage the erro in a better way..
                        Log.e("ERROR","Error in DeviceInfo parsing!");
                    }
                break;
                case SENSOR_JSON_KEY:
                    JSONArray jsonSensors = jsonDevice.getJSONArray(SENSOR_JSON_KEY);
                    mSensors = new ArrayList<>();
                    for (int i = 0; i < jsonSensors.length(); i++) {
                        JSONObject jsonSensor = jsonSensors.getJSONObject(i);
                        int mId = jsonSensor.getInt(ID_JSON_KEY);
                        //NOTE - Sensor Descriptor ---------------------------------------------------------
                        SensorDescriptor mSensorDescriptor = parseSensorDescriptor(jsonSensor);
                        //NOTE - Sensor Status -------------------------------------------------------------
                        SensorStatus mSensorStatus = parseSensorStatus(jsonSensor);
                        Sensor mSensor = new Sensor(mId, mSensorDescriptor,mSensorStatus,null);
                        mSensors.add(mSensor);
                    }
                break;
                case TAG_LIST_JSON_KEY:
                    JSONObject jsonTagList = jsonDevice.getJSONObject(TAG_LIST_JSON_KEY);
                    mTags = new ArrayList<>();
                    mTags.addAll(parseTagList(jsonTagList));
                break;
            }
        }

        Set<Sensor> dupSensors = Device.areThereDuplicateSensorIDs(mSensors);
        if(!dupSensors.isEmpty()){
           Log.e("ERROR","Error in parsing Sensor List. Duplicate id!");
            return new Device(deviceInfo,mSensors,mTags,"-> Duplicate Sensor id!");
        }
        else{
            return new Device(deviceInfo,mSensors,mTags,null);
        }
    }

    private DeviceInfo parseDeviceInfo(JSONObject jsonDevice) {
        JSONObject jsonDeviceInfo;
        try {
            jsonDeviceInfo = jsonDevice.getJSONObject(DEVICE_INFO_JSON_KEY);
            return DeviceInfoKt.extractDeviceInfo(jsonDeviceInfo.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private SensorDescriptor parseSensorDescriptor(JSONObject jsonSensor) throws JSONException{
        JSONObject jsonSensorDescriptor = jsonSensor.getJSONObject(SENSOR_DESCRIPTOR_JSON_KEY);
        List<DescriptorParam> descriptorParamList = parseDescriptorParams(jsonSensorDescriptor);
        JSONArray jsonSubSensorDescriptors = jsonSensorDescriptor.getJSONArray(SUB_SENSOR_DESCRIPTOR_KEY);
        ArrayList<SubSensorDescriptor> subSensorDescriptorsList = new ArrayList<>();
        for (int j = 0; j < jsonSubSensorDescriptors.length(); j++) {
            JSONObject jsonSubSensorDescriptor = jsonSubSensorDescriptors.getJSONObject(j);
            List<DescriptorParam> subDescriptorParamList = parseDescriptorParams(jsonSubSensorDescriptor);
            SubSensorDescriptor mSubSensorDescriptor = new SubSensorDescriptor(
                    jsonSubSensorDescriptor.getInt(ID_JSON_KEY),
                    jsonSubSensorDescriptor.getString(SENSOR_TYPE_JSON_KEY),
                    jsonSubSensorDescriptor.getDouble(DATA_PER_SAMPLE_JSON_KEY),
                    jsonSubSensorDescriptor.getString(UNIT_JSON_KEY),
                    subDescriptorParamList,
                    null
            );
            subSensorDescriptorsList.add(mSubSensorDescriptor);
        }
        Set<SubSensorDescriptor> dupList = SensorDescriptor.areThereSSDDuplicateIDs(subSensorDescriptorsList);
        if (!dupList.isEmpty()){
            Log.e("ERROR","-> Duplicate SubSensor id!. Duplicate id (removed non-first occurences)!");

            for (SubSensorDescriptor ssd: dupList
                 ) {
                Log.e("ERROR","Duplicate ids: " + ssd.getId() + ", type: " + ssd.getSensorType());
            }
            //subSensorDescriptorsList.removeAll(dupList);
            return new SensorDescriptor(
                    jsonSensorDescriptor.getString(DATA_TYPE_JSON_KEY),
                    jsonSensorDescriptor.getString(NAME_JSON_KEY),
                    descriptorParamList,
                    subSensorDescriptorsList,
                    "Duplicate ID!"
            );
        }
        return new SensorDescriptor(
                jsonSensorDescriptor.getString(DATA_TYPE_JSON_KEY),
                jsonSensorDescriptor.getString(NAME_JSON_KEY),
                descriptorParamList,
                subSensorDescriptorsList,
                null
        );
    }

    private SensorStatus parseSensorStatus(JSONObject jsonSensor) throws  JSONException{
        JSONObject jsonSensorStatus = jsonSensor.getJSONObject(SENSOR_STATUS_JSON_KEY);
        Iterator<String> keys = jsonSensorStatus.keys();
        List<StatusParam> statusParamList = new ArrayList<>();
        List<SubSensorStatus> subSensorStatusList = new ArrayList<>();
        while(keys.hasNext()) {
            String key = keys.next();
            if(!(key.equals(SUB_SENSOR_STATUS_JSON_KEY) || key.equals(IS_ACTIVE_JSON_KEY))) {
                StatusParam ssp = new StatusParam(key, jsonSensorStatus.getString(key));
                statusParamList.add(ssp);
            } else if (!(key.equals(IS_ACTIVE_JSON_KEY))){
                JSONArray jsonSubSensorStatus = jsonSensorStatus.getJSONArray(SUB_SENSOR_STATUS_JSON_KEY);
                for (int j = 0; j < jsonSubSensorStatus.length(); j++) {
                    JSONObject jsonSubSensorParam = jsonSubSensorStatus.getJSONObject(j);
                    Iterator<String> subKeys = jsonSubSensorParam.keys();
                    List<StatusParam> subStatusParamList = new ArrayList<>();
                    while(subKeys.hasNext()) {
                        String subKey = subKeys.next();
                        if(!(subKey.equals(SENSITIVITY_JSON_KEY) || subKey.equals(IS_ACTIVE_JSON_KEY))) {
                            StatusParam ssp = new StatusParam(subKey, jsonSubSensorParam.getString(subKey));
                            subStatusParamList.add(ssp);
                        }
                    }
                    SubSensorStatus sss = new SubSensorStatus(
                            j,
                            jsonSubSensorParam.getDouble(SENSITIVITY_JSON_KEY),
                            jsonSubSensorParam.getBoolean(IS_ACTIVE_JSON_KEY),
                            subStatusParamList,
                            null
                    );
                    subSensorStatusList.add(sss);
                }
            }
        }
        return new SensorStatus(
                jsonSensorStatus.getBoolean(IS_ACTIVE_JSON_KEY),
                statusParamList,
                subSensorStatusList,
                null
        );
    }

    private List<DescriptorParam> parseDescriptorParams(JSONObject jsonSensorDescriptor) throws JSONException{
        Iterator<String> keys = jsonSensorDescriptor.keys();
        List<DescriptorParam> descriptorParamList = new ArrayList<>();
        while(keys.hasNext()) {
            String key = keys.next();
            if(!(key.equals(DATA_TYPE_JSON_KEY) ||
                    key.equals(SENSOR_TYPE_JSON_KEY) ||
                    key.equals(DATA_PER_SAMPLE_JSON_KEY) ||
                    key.equals(NAME_JSON_KEY) ||
                    key.equals(UNIT_JSON_KEY) ||
                    key.equals(ID_JSON_KEY) ||
                    key.equals(SUB_SENSOR_DESCRIPTOR_KEY))) {
                JSONObject jsonSensorDescriptorParam = jsonSensorDescriptor.getJSONObject(key);
                ArrayList<Double> mValues;
                try {
                    JSONArray jsonValues = jsonSensorDescriptorParam.getJSONArray(VALUES_JSON_KEY);
                    mValues = arrayListFromJSONArray(jsonValues);
                } catch (JSONException e){
                    mValues = null;
                }

                Double min;
                try {
                    min = jsonSensorDescriptorParam.getDouble(MIN_JSON_KEY);
                } catch (JSONException e){
                    min = null;
                }

                Double max;
                try {
                    max = jsonSensorDescriptorParam.getDouble(MAX_JSON_KEY);
                } catch (JSONException e){
                    max = null;
                }

                String dataType;
                try {
                    dataType = jsonSensorDescriptorParam.getString(DATA_TYPE_JSON_KEY);
                } catch (JSONException e){
                    dataType = null;
                }
                DescriptorParam dp = new DescriptorParam(
                        key,
                        mValues,
                        min,
                        max,
                        dataType,
                        null
                );
                descriptorParamList.add(dp);
            }
        }
        return descriptorParamList;
    }

    public DeviceStats getDeviceStats() {
        return deviceStats;
    }

    private List<Tag> parseTagList(JSONObject jsonTagList) {
        return TagKt.extractTagList(jsonTagList.toString());
    }

    @Override
    public void parseFWStats(JSONObject jsonObj) {
        //Log.e("TAG","FW Stats Parsing");
        Iterator<String> keys = jsonObj.keys();
        while(keys.hasNext()) {
            String key = keys.next();
            try {
                switch (key){
                    case CPU_USAGE_JSON_KEY:
                        double cpuUsage = jsonObj.getDouble(CPU_USAGE_JSON_KEY);
                        //Log.e("TESTFWStats","cpu usage: " + cpuUsage);
                        this.deviceStats.setCpuUsage(cpuUsage);
                        break;
                    case BATTERY_VOLTAGE_JSON_KEY:
                        double battVoltage = jsonObj.getDouble(BATTERY_VOLTAGE_JSON_KEY);
                        //Log.e("TESTFWStats","battery voltage: " + battVoltage);
                        this.deviceStats.setBatteryVoltage(battVoltage);
                        break;
                    case BATTERY_LEVEL_JSON_KEY:
                        double battLevel = jsonObj.getDouble(BATTERY_LEVEL_JSON_KEY);
                        //Log.e("TESTFWStats","battery level: " + battLevel);
                        this.deviceStats.setBatteryLevel(battLevel);
                        break;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void setDevice(Device device){
        this.mDevice = device;
    }

    @Override
    public void setDeviceAlias(String alias) {
        this.mDevice.setDeviceAlias(alias);
    }

    @Override
    public void setHSDFeature(Feature feature) {
        this.mHSDFeature = feature;
    }

    @Override
    public void setTagLabel(int tagId, boolean isSW, String label) {
        this.mDevice.updateTagLabel(tagId, isSW, label);
    }

    @Override
    public void setTagEnabled(int tagId, boolean isSW, boolean isEnabled) {
        this.mDevice.enableTag(tagId, isSW, isEnabled);
    }

    public void setAcquisitionName(String name){
        this.mDevice.setAcquisitionName(name);
    }

    @Override
    public void setAcquisitionNotes(String notes) {
        this.mDevice.setAcquisitionNotes(notes);
    }

    @Override
    public void setDevice(JSONObject deviceJSON) throws JSONException {
        Device loadedDevice = parseDevice(deviceJSON);
        if (this.mDevice == null)
            this.mDevice =  loadedDevice;//NOTE this is executed always @connection or  when the stopLog button is pressed
        else {
            //TODO check if the new device loaded from "deviceJSON" is compatible with the one read from the STWIN
            Device checkedDevice = new Device();
            //TODO check DeviceInfo
            checkedDevice.setDeviceInfo(checkDeviceInfo(loadedDevice.getDeviceInfo()));

            if (loadedDevice.getDeviceInfo().getNSensor() != loadedDevice.getSensors().size())
                checkedDevice.getDeviceInfo().addErrorMessage("-> Inconsistent number of sensors");

            //checkedDevice.setSensors(this.mDevice.getSensors());

            ArrayList<Sensor> checkedSensorList = new ArrayList<>();
                for (Sensor s : this.mDevice.getSensors()) {
                    Sensor loadedSensor = loadedDevice.getSensor(s.getId());
                    Sensor checkedSensor = new Sensor();
                    checkedSensor.setId(s.getId());
                    if (loadedSensor != null) {
                        checkedSensor.setSensorDescriptor(checkSensorDescriptor(loadedSensor));
                        checkedSensor.setSensorStatus(loadedSensor.getSensorStatus());
                    } else {
                        checkedSensor.setSensorDescriptor(s.getSensorDescriptor());
                        checkedSensor.setSensorStatus(s.getSensorStatus());
                        checkedSensor.addErrorMessage("-> sID: " + s.getId() + " missing Sensor");
                    }
                    checkedSensorList.add(checkedSensor);
                }
            checkedDevice.setSensors(checkedSensorList);

            this.mDevice = checkedDevice;
        }
    }

    @Override
    public void setDeviceInfo(JSONObject jsonObj) throws JSONException {
        this.mDevice.setDeviceInfo(parseDeviceInfo(jsonObj));
    }

    private DeviceInfo checkDeviceInfo(DeviceInfo di) {
        // TODO build it only at the end when we have all the data
        // set the serial number, alias nSensor as constant?
        DeviceInfo checkedDeviceInfo = new DeviceInfo("","",null,null,null,null,0,"");

        checkedDeviceInfo.setSerialNumber(this.mDevice.getDeviceInfo().getSerialNumber());
        if (!this.mDevice.getDeviceInfo().getSerialNumber().equals(di.getSerialNumber())) {
            checkedDeviceInfo.addErrorMessage("-> Invalid serialNumber");
            Log.e("ModelCheckError","Loaded serial number != connected board serial number");
        }

        checkedDeviceInfo.setAlias(this.mDevice.getDeviceInfo().getAlias());
        if (!this.mDevice.getDeviceInfo().getAlias().equals(di.getAlias())) {
            checkedDeviceInfo.addErrorMessage("-> Invalid alias");
            Log.e("ModelCheckError","Loaded alias != connected board alias");
        }

        checkedDeviceInfo.setNSensor(this.mDevice.getDeviceInfo().getNSensor());
        if (!(this.mDevice.getDeviceInfo().getNSensor() == (di.getNSensor()))) {
            checkedDeviceInfo.addErrorMessage("-> Invalid number of sensors");
            checkedDeviceInfo.addErrorMessage("----> expected: " + mDevice.getDeviceInfo().getNSensor());
            checkedDeviceInfo.addErrorMessage("----> found: " + di.getNSensor());
            Log.e("ModelCheckError","Loaded sensor number != connected board sensor number");
        }
        return checkedDeviceInfo;
    }

    @Override
    public void setSensorDescriptor(JSONObject jsonObj) throws JSONException {
        int id = jsonObj.getInt(ID_JSON_KEY);
        this.mDevice.getSensor(id).setSensorDescriptor(parseSensorDescriptor(jsonObj));
    }

    //todo check the name of this funciton
    private DescriptorParam checkValidity(DescriptorParam currentValues,DescriptorParam  validValues){
        String errors = "";

        List<Double> values = currentValues.getValues();
        if(!isEquals(values,validValues.getValues())){
            errors += "-> " + validValues.getName() + " Invalid Values\n";
        }

        String dataType = currentValues.getDataType();
        if(!(currentValues.getDataType() == null || currentValues.getDataType().equals(validValues.getDataType()))){
            errors += "-> " + validValues.getName() + " Invalid Data Type\n";
        }

        Double min = currentValues.getMin();
        if(!(min == null || min.equals(validValues.getMin()))){
            errors += "-> " + validValues.getName() + " Invalid Min\n";
        }

        Double max = currentValues.getMax();
        if(!(max == null || max.equals(validValues.getMax()))){
            errors += "-> " + validValues.getName() + " Invalid Max\n";
        }

        return new DescriptorParam(
                validValues.getName(),
                values,
                min,
                max,
                dataType,
                errors.isEmpty() ? null : errors
        );

    }

    private SensorDescriptor checkSensorDescriptor(Sensor s){
        int sid = s.getId();
        SensorDescriptor sd = s.getSensorDescriptor();
        SensorDescriptor modelSensorDescriptor = this.mDevice.getSensor(sid).getSensorDescriptor();
        SensorDescriptor checkedSensorDescriptor = new SensorDescriptor();

        checkedSensorDescriptor.setName(modelSensorDescriptor.getName());
        if (!modelSensorDescriptor.getName().equals(sd.getName())) {
            checkedSensorDescriptor.addErrorMessage("-> sID: " + sid + " Invalid Sensor Name");
        }

        checkedSensorDescriptor.setDataType(modelSensorDescriptor.getDataType());
        if (!modelSensorDescriptor.getDataType().equals(sd.getDataType())) {
            checkedSensorDescriptor.addErrorMessage("-> sID: " + sid + " Invalid Data Type");
        }

        ArrayList<DescriptorParam> checkedDescriptorParamList = new ArrayList<>();
        for (DescriptorParam descriptorParam : modelSensorDescriptor.getDescriptorParams()) {
            DescriptorParam dp = sd.getDescriptorParam(descriptorParam.getName());
            DescriptorParam checkedDescriptorParam = checkValidity(descriptorParam,dp);
            checkedDescriptorParamList.add(checkedDescriptorParam);
        }
        checkedSensorDescriptor.setDescriptorParams(checkedDescriptorParamList);

        ArrayList<SubSensorDescriptor> checkedSubSensorDescriptorList = new ArrayList<>();
        for (SubSensorDescriptor subSensorDescriptor : modelSensorDescriptor.getSubSensorDescriptors()) {
            SubSensorDescriptor checkedSubSensorDescriptor = new SubSensorDescriptor();
            SubSensorDescriptor ssd = sd.getSubSensorDescriptor(subSensorDescriptor.getId());

            checkedSubSensorDescriptor.setId(subSensorDescriptor.getId());
            if(ssd.getErrorMessage() == null) {
                checkedSubSensorDescriptor.setSensorType(subSensorDescriptor.getSensorType());
                if (!subSensorDescriptor.getSensorType().equals(ssd.getSensorType())) {
                    checkedSubSensorDescriptor.addErrorMessage("-> sID:" + sid + ", ssID: " + ssd.getId() + " Invalid sensorType");
                }

                checkedSubSensorDescriptor.setDataPerSample(subSensorDescriptor.getDataPerSample());
                if (!subSensorDescriptor.getDataPerSample().equals(ssd.getDataPerSample())) {
                    checkedSubSensorDescriptor.addErrorMessage("-> sID:" + sid + ", ssID: " + ssd.getId() + " Invalid dataPerSample");
                }

                checkedSubSensorDescriptor.setUnit(subSensorDescriptor.getUnit());
                if (!subSensorDescriptor.getUnit().equals(ssd.getUnit())) {
                    checkedSubSensorDescriptor.addErrorMessage("-> sID:" + sid + ", ssID: " + ssd.getId() + " Invalid unit");
                }
                //NOTE DescriptorParam function enclosure future........................................
                ArrayList<DescriptorParam> checkedSubDescriptorParamList = new ArrayList<>();

                for (DescriptorParam subDescriptorParam : subSensorDescriptor.getSubDescriptorParams()) {

                    if (ssd.getErrorMessage() == null) {
                        DescriptorParam sdp = ssd.getDescriptorParam(subDescriptorParam.getName());
                        //TODO check if name isEquals???
                        //checkedDescriptorSubParam.setName(sdp.getName());

                        DescriptorParam checkedDescriptorSubParam = checkValidity(subDescriptorParam,sdp);
                        checkedSubDescriptorParamList.add(checkedDescriptorSubParam);
                    } else {
                        DescriptorParam checkedDescriptorSubParam = subDescriptorParam;
                        checkedDescriptorSubParam.addErrorMessage("-> Invalid " + subDescriptorParam.getName());
                        checkedSubDescriptorParamList.add(checkedDescriptorSubParam);
                    }
                }
                checkedSubSensorDescriptor.setSubDescriptorParams(checkedSubDescriptorParamList);
                checkedSubSensorDescriptorList.add(checkedSubSensorDescriptor);
            }else{
                checkedSubSensorDescriptor = subSensorDescriptor;
                checkedSubSensorDescriptor.addErrorMessage("-> Sensor: " + sid);
                checkedSubSensorDescriptor.addErrorMessage("----> SubSensorID: " + subSensorDescriptor.getId() + " not found");
                checkedSubSensorDescriptorList.add(checkedSubSensorDescriptor);
            }
        }
        checkedSensorDescriptor.setSubSensorDescriptors(checkedSubSensorDescriptorList);
        return checkedSensorDescriptor;
    }

    @Override
    public void setSensorStatus(JSONObject jsonObj) throws JSONException {
        int id = jsonObj.getInt(ID_JSON_KEY);
        this.mDevice.getSensor(id).setSensorStatus(parseSensorStatus(jsonObj));
    }

    //NOTE Transport Protocol
    public byte[] encapsulate(String string){
        byte[] byteCommand = stringToByte(string);
        byte head = TP_START_PACKET;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int cnt = 0, size = 0;
        int codedDataLength = byteCommand.length;
        int mtuSize = MTU_SIZE;
        while (cnt < codedDataLength)
        {
            size = Math.min((mtuSize-1),(codedDataLength-cnt));
            if (codedDataLength - cnt <= (mtuSize-1)){
                if (cnt == 0){
                    head = TP_START_END_PACKET;
                }
                else{
                    head = TP_END_PACKET;
                }
            }
            switch (head)
            {
                case TP_START_PACKET:
                {
                    /*First part of a packet*/
                    baos.write(head);
                    baos.write(byteCommand,0,mtuSize-1);
                    head = TP_MIDDLE_PACKET;
                }
                break;
                case TP_START_END_PACKET:
                {
                    /*First and last part of a packet*/
                    baos.write(head);
                    baos.write(byteCommand,0,codedDataLength);
                    head = TP_START_PACKET;
                }
                break;
                case TP_MIDDLE_PACKET:
                {
                    /*Central part of a packet*/
                    baos.write(head);
                    baos.write(byteCommand,cnt,mtuSize-1);
                }
                break;
                case TP_END_PACKET:
                {
                    /*Last part of a packet*/
                    baos.write(head);
                    baos.write(byteCommand,cnt,codedDataLength-cnt);
                    head = TP_START_PACKET;
                }
                break;
            }
            /*length variables update*/
            cnt += size;
        }
        return baos.toByteArray();
    }

    public String decapsulate(String string){
        byte[] byteCommand = stringToByte(string);
        if (byteCommand[0] == 0) {
            currentMessage = new ByteArrayOutputStream();
            currentMessage.write(byteCommand,1,byteCommand.length - 1);
        }
        else if (byteCommand[0] == 32) {
            currentMessage = new ByteArrayOutputStream();
            return byteToString(currentMessage.toByteArray());
        }
        else if (byteCommand[0] == 64) {
            if (currentMessage != null)
                currentMessage.write(byteCommand,1,byteCommand.length - 1);
        }
        else if (byteCommand[0] == -128) {
            if (currentMessage != null){
                currentMessage.write(byteCommand,1,byteCommand.length - 1);
                return byteToString(currentMessage.toByteArray());
            }
        }
        return null;
    }

    //NOTE Getters (data from device model)
    @Override
    public Device getDeviceModel(){
        return this.mDevice;
    }

    @Override
    public String getSensorStatusParam(int sensorId, String paramName) {
        Sensor s = this.mDevice.getSensor(sensorId);
        if (s != null)
            return s.getSensorStatus().getParamValue(paramName);
        return null;
    }

    @Override
    public String getSubSensorStatusParam(int sensorId, int subSensorId, String paramName) {
        Sensor s = this.mDevice.getSensor(sensorId);
        if (s != null)
            return s.getSensorStatus().getSubSensorStatus(subSensorId).getParamValue(paramName);
        return null;
    }

    //NOTE Getters JSON from device model
    @Override
    public JSONObject getJSONfromDevice(){
        JSONObject mainDeviceJSON = new JSONObject();
        JSONObject deviceJSON = new JSONObject();
        JSONObject deviceInfoJSON = new JSONObject();
        try {
            deviceInfoJSON.put(SERIAL_NUMBER_JSON_KEY,mDevice.getDeviceInfo().getSerialNumber());
            deviceInfoJSON.put(ALIAS_JSON_KEY,mDevice.getDeviceInfo().getAlias());
            deviceInfoJSON.put(N_SENSORS_JSON_KEY,mDevice.getDeviceInfo().getNSensor());
            deviceJSON.put(DEVICE_INFO_JSON_KEY, deviceInfoJSON);
            JSONArray sensorArrayParamsJSON = new JSONArray();
            for (Sensor sensor : mDevice.getSensors()) {
                JSONObject sensorJSON = new JSONObject();
                sensorJSON.put(ID_JSON_KEY,sensor.getId());
                JSONObject sensorDescriptorJSON = new JSONObject();
                sensorDescriptorJSON.put(NAME_JSON_KEY,sensor.getSensorDescriptor().getName());
                sensorDescriptorJSON.put(DATA_TYPE_JSON_KEY,sensor.getSensorDescriptor().getDataType());
                for (DescriptorParam param : sensor.getSensorDescriptor().getDescriptorParams()) {
                    JSONObject paramParamsJSON = new JSONObject();
                    if (param.getMin() != null)
                        paramParamsJSON.put(MIN_JSON_KEY,param.getMin());
                    if (param.getMax() != null)
                        paramParamsJSON.put(MAX_JSON_KEY,param.getMax());
                    if (param.getDataType() != null)
                        paramParamsJSON.put(DATA_TYPE_JSON_KEY, param.getDataType());
                    if (param.getValues() != null) {
                        JSONArray valueArrayJSON = new JSONArray();
                        for (Double value: param.getValues()) {
                            valueArrayJSON.put(value);
                        }
                        paramParamsJSON.put(VALUES_JSON_KEY, valueArrayJSON);
                    }
                    sensorDescriptorJSON.put(param.getName(),paramParamsJSON);
                }

                JSONArray subSensorDescriptorArrayJSON = new JSONArray();
                for (SubSensorDescriptor ssd : sensor.getSensorDescriptor().getSubSensorDescriptors()) {
                    JSONObject subSensorDescriptorJSON = new JSONObject();
                    subSensorDescriptorJSON.put(ID_JSON_KEY,ssd.getId());
                    subSensorDescriptorJSON.put(SENSOR_TYPE_JSON_KEY,ssd.getSensorType());
                    subSensorDescriptorJSON.put(DATA_PER_SAMPLE_JSON_KEY,ssd.getDataPerSample());
                    subSensorDescriptorJSON.put(UNIT_JSON_KEY,ssd.getUnit());
                    for (DescriptorParam subDescParam : ssd.getSubDescriptorParams()) {
                        JSONObject subParamParamsJSON = new JSONObject();
                        if (subDescParam.getMin() != null)
                            subParamParamsJSON.put(MIN_JSON_KEY,subDescParam.getMin());
                        if (subDescParam.getMax() != null)
                            subParamParamsJSON.put(MAX_JSON_KEY,subDescParam.getMax());
                        if (subDescParam.getDataType() != null)
                            subParamParamsJSON.put(DATA_TYPE_JSON_KEY, subDescParam.getDataType());
                        if (subDescParam.getValues() != null) {
                            JSONArray valueArrayJSON = new JSONArray();
                            for (Double value: subDescParam.getValues()) {
                                valueArrayJSON.put(value);
                            }
                            subParamParamsJSON.put(VALUES_JSON_KEY, valueArrayJSON);
                        }
                        subSensorDescriptorJSON.put(subDescParam.getName(),subParamParamsJSON);
                    }
                    subSensorDescriptorArrayJSON.put(subSensorDescriptorJSON);
                    sensorDescriptorJSON.put(SUB_SENSOR_DESCRIPTOR_KEY,subSensorDescriptorArrayJSON);
                }
                sensorJSON.put(SENSOR_DESCRIPTOR_JSON_KEY,sensorDescriptorJSON);

                JSONObject sensorStatusJSON = new JSONObject();
                sensorStatusJSON.put(IS_ACTIVE_JSON_KEY,sensor.getSensorStatus().isActive());
                for (StatusParam statusParam : sensor.getSensorStatus().getStatusParams()) {
                    sensorStatusJSON.put(statusParam.getName(),statusParam.getValue());
                }
                JSONArray subSensorStatusArrayJSON = new JSONArray();
                for (SubSensorStatus sss : sensor.getSensorStatus().getSubSensorStatusList()){
                    JSONObject subSensorStatusJSON = new JSONObject();
                    for (StatusParam subStatusParam : sss.getParams()) {
                        subSensorStatusJSON.put(subStatusParam.getName(),subStatusParam.getValue());
                    }
                    subSensorStatusJSON.put(SENSITIVITY_JSON_KEY,sss.getSensitivity());
                    subSensorStatusJSON.put(IS_ACTIVE_JSON_KEY,sss.isActive());

                    subSensorStatusArrayJSON.put(subSensorStatusJSON);
                }
                sensorStatusJSON.put(SUB_SENSOR_STATUS_JSON_KEY,subSensorStatusArrayJSON);
                sensorJSON.put(SENSOR_STATUS_JSON_KEY,sensorStatusJSON);

                sensorArrayParamsJSON.put(sensorJSON);
            }
            deviceJSON.put("sensor",sensorArrayParamsJSON);
            mainDeviceJSON.put("device", deviceJSON);
            return mainDeviceJSON;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    //NOTE commands


    @Override
    public String createSetDeviceAliasCommand(String alias) {
        JSONObject jsonCommandStartTag = new JSONObject();
        try {
            jsonCommandStartTag.put("command","SET");
            jsonCommandStartTag.put("request", "deviceInfo");
            jsonCommandStartTag.put("alias", alias);
            //Log.e("JSONTest", "Tag SET: " + jsonCommandStartTag.toString());
            return jsonCommandStartTag.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String createSetSensorStatusParamCommand(int sensorId, String paramName, String value) {
        Sensor s = this.mDevice.getSensor(sensorId);
        if (s != null) {
            SensorStatus sStatus = s.getSensorStatus();
            if (sStatus != null) {
                sStatus.setParamValue(paramName, value);
                JSONObject jsonCommandSET = new JSONObject();
                try {
                    jsonCommandSET.put("command", "SET");
                    jsonCommandSET.put("sensorId", sensorId);
                    DescriptorParam param = s.getSensorDescriptor().getDescriptorParam(paramName);
                    if (param != null) {
                        String dataType = param.getDataType();
                        if (dataType != null) {
                            switch (dataType) {
                                case "int8_t":
                                case "int16_t":
                                    jsonCommandSET.put(paramName, Integer.valueOf(value));
                                    break;
                                case "float":
                                    jsonCommandSET.put(paramName, Double.valueOf(value));
                                    break;
                            }
                        } else {
                            jsonCommandSET.put(paramName, Double.valueOf(value));
                        }
                    } else {
                        jsonCommandSET.put(paramName, Double.valueOf(value));
                    }
                    //Log.e("JSONTest", "Sensor SET: " + jsonCommandSET.toString());
                    return jsonCommandSET.toString();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    @Override
    public String createSetSubSensorStatusParamCommand(int sensorId, int subSensorId, String paramName, String value){
        Sensor s = this.mDevice.getSensor(sensorId);
        if (s != null) {
            SubSensorStatus ssStatus = s.getSensorStatus().getSubSensorStatus(subSensorId);
            if (ssStatus != null) {
                ssStatus.setParamValue(paramName, value);
                JSONObject jsonCommandSET = new JSONObject();
                JSONArray jsonCommandSETParams = new JSONArray();
                try {
                    jsonCommandSET.put("command", "SET");
                    jsonCommandSET.put("sensorId", sensorId);
                    JSONObject jsonParam = new JSONObject();//NOTE here
                    jsonParam.put("id", subSensorId);
                    DescriptorParam param = s.getSensorDescriptor().getSubSensorDescriptors().get(subSensorId).getDescriptorParam(paramName);
                    if (param != null) {
                        String dataType = param.getDataType();
                        if(dataType != null){
                            switch (dataType){
                                case "int8_t": case "int16_t":
                                    jsonParam.put(paramName, Integer.valueOf(value));
                                    break;
                                case "float":
                                    jsonParam.put(paramName, Double.valueOf(value));
                                    break;
                            }
                        } else {
                            jsonParam.put(paramName, Double.valueOf(value));
                        }
                    }
                    jsonCommandSETParams.put(jsonParam);
                    jsonCommandSET.put("subSensorStatus",jsonCommandSETParams);
                    //Log.e("JSONTest", "SubSensor SET: " + jsonCommandSET.toString());
                    return jsonCommandSET.toString();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }


    public String createSetSensorIsActiveCommand(int sensorId, boolean value) {
        Sensor s = this.mDevice.getSensor(sensorId);
        if (s != null) {
            JSONObject jsonCommandSET = new JSONObject();
            try {
                jsonCommandSET.put("command", "SET");
                jsonCommandSET.put("sensorId", sensorId);
                jsonCommandSET.put("isActive", value);
                //Log.e("JSONTest", "Sensor SET: " + jsonCommandSET.toString());
                return jsonCommandSET.toString();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    //NOTE model update
    public void updateSensorIsActiveModel(int sensorId) {
        Sensor s = this.mDevice.getSensor(sensorId);
        SensorStatus sStatus = s.getSensorStatus();
        boolean value = !sStatus.isActive();
        sStatus.setActive(value);
    }

    @Override
    public String createSetSensorIsActiveCommand(int sensorId) {
        Sensor s = this.mDevice.getSensor(sensorId);
        if (s != null) {
            JSONObject jsonCommandSET = new JSONObject();
            try {
                jsonCommandSET.put("command", "SET");
                jsonCommandSET.put("sensorId", sensorId);
                jsonCommandSET.put("isActive", s.getSensorStatus().isActive());
                //Log.e("JSONTest", "Sensor SET: " + jsonCommandSET.toString());
                return jsonCommandSET.toString();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public String createSetSubSensorIsActiveCommand(int sensorId, Integer subSensorId) {
        Sensor s = this.mDevice.getSensor(sensorId);
        if (s != null) {
            JSONObject jsonCommandSET = new JSONObject();
            JSONArray jsonCommandSETParams = new JSONArray();
            try {
                jsonCommandSET.put("command", "SET");
                jsonCommandSET.put("sensorId", sensorId);
                JSONObject jsonParam = new JSONObject();
                jsonParam.put("id", subSensorId);
                jsonParam.put("isActive", s.getSensorStatus().getSubSensorStatus(subSensorId).isActive());
                jsonCommandSETParams.put(jsonParam);
                jsonCommandSET.put("subSensorStatus",jsonCommandSETParams);
                //Log.e("JSONTest", "SubSensor SET: " + jsonCommandSET.toString());
                return jsonCommandSET.toString();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    //NOTE model update
    public void updateSubSensorIsActiveModel(int sensorId, int subSensorId) {
        Sensor s = this.mDevice.getSensor(sensorId);
        SubSensorStatus ssStatus = s.getSensorStatus().getSubSensorStatus(subSensorId);
        boolean value = !ssStatus.isActive();
        ssStatus.setActive(value);
    }

    public String createSetSubSensorIsActiveCommand(int sensorId, Integer subSensorId, boolean value) {
        Sensor s = this.mDevice.getSensor(sensorId);
        if (s != null) {
            JSONObject jsonCommandSET = new JSONObject();
            JSONArray jsonCommandSETParams = new JSONArray();
            try {
                jsonCommandSET.put("command", "SET");
                jsonCommandSET.put("sensorId", sensorId);
                JSONObject jsonParam = new JSONObject();//NOTE here
                jsonParam.put("id", subSensorId);
                jsonParam.put("isActive", value);
                jsonCommandSETParams.put(jsonParam);
                jsonCommandSET.put("subSensorStatus",jsonCommandSETParams);
                //Log.e("JSONTest", "SubSensor SET: " + jsonCommandSET.toString());
                return jsonCommandSET.toString();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public String createSetTagCommand(int tagId, boolean isSW, boolean enable) {
        JSONObject jsonCommandStartTag = new JSONObject();
        try {
            jsonCommandStartTag.put("command","SET");
            jsonCommandStartTag.put("request",isSW ? "sw_tag" : "hw_tag");
            jsonCommandStartTag.put("ID",tagId);
            jsonCommandStartTag.put("enable",enable);
            //Log.e("JSONTest", "Tag SET: " + jsonCommandStartTag.toString());
            return jsonCommandStartTag.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String createConfigTagCommand(int tagId, boolean isSW, String label) {
        JSONObject jsonCommandConfigTag = new JSONObject();
        try {
            jsonCommandConfigTag.put("command","SET");
            jsonCommandConfigTag.put("request",isSW ? "sw_tag_label" : "hw_tag_label");
            jsonCommandConfigTag.put("ID",tagId);
            jsonCommandConfigTag.put("label",label);
            //Log.e("JSONTest", "Tag Config SET: " + jsonCommandConfigTag.toString());
            return jsonCommandConfigTag.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String createSetAcqInfoCommand(String name, String notes) {
        JSONObject jsonCommandAcqInfo = new JSONObject();
        try {
            jsonCommandAcqInfo.put("command","SET");
            jsonCommandAcqInfo.put("request","acq_info");
            jsonCommandAcqInfo.put("name",name);
            jsonCommandAcqInfo.put("notes",notes);
            //Log.e("JSONTest", "Acquisition Info SET: " + jsonCommandAcqInfo.toString());
            return jsonCommandAcqInfo.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String createGetRegisterCommand(int sensorId, String address) {
        JSONObject jsonCommandGET = new JSONObject();
        try {
            jsonCommandGET.put("command","GET");
            jsonCommandGET.put("request","register");
            jsonCommandGET.put("sensorId",sensorId);
            jsonCommandGET.put("address",address);
            //Log.e("JSONTest","GET command: " + jsonCommandGET.toString());
            return jsonCommandGET.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String createGetDeviceCommand() {
        JSONObject jsonCommandGET = new JSONObject();
        try {
            jsonCommandGET.put("command","GET");
            jsonCommandGET.put("request","device");
            //Log.e("JSONTest","GET device command: " + jsonCommandGET.toString());
            return jsonCommandGET.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String createGetDeviceInfoCommand() {
        JSONObject jsonCommandGET = new JSONObject();
        try {
            jsonCommandGET.put("command","GET");
            jsonCommandGET.put("request","deviceInfo");
            //Log.e("JSONTest","GET command: " + jsonCommandGET.toString());
            return jsonCommandGET.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String createGetSensorDescriptorCommand(int sensorId) {
        JSONObject jsonCommandGET = new JSONObject();
        try {
            jsonCommandGET.put("command","GET");
            jsonCommandGET.put("request","descriptor");
            jsonCommandGET.put("sensorId",sensorId);
            //Log.e("JSONTest","GET command: " + jsonCommandGET.toString());
            return jsonCommandGET.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String createGetSensorStatusCommand(int sensorId) {
        JSONObject jsonCommandGET = new JSONObject();
        try {
            jsonCommandGET.put("command","GET");
            jsonCommandGET.put("request","status");
            jsonCommandGET.put("sensorId",sensorId);
            //Log.e("JSONTest","GET command: " + jsonCommandGET.toString());
            return jsonCommandGET.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }


    public String createGETWiFiConfCommand() {
        JSONObject jsonCommandGET = new JSONObject();
        try {
            jsonCommandGET.put("command","GET");
            jsonCommandGET.put("request","network");
            //Log.e("JSONTest","GET command: " + jsonCommandGET.toString());
            return jsonCommandGET.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String createGetFWStatsCommand() {
        JSONObject jsonCommandGET = new JSONObject();
        try {
            jsonCommandGET.put("command","GET");
            jsonCommandGET.put("request","fwStats");
            //Log.e("JSONTest","GET command: " + jsonCommandGET.toString());
            return jsonCommandGET.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String createGetTagListCommand() {
        JSONObject jsonCommandGET = new JSONObject();
        try {
            jsonCommandGET.put("command","GET");
            jsonCommandGET.put("request","tagList");
            //Log.e("JSONTest","GET command: " + jsonCommandGET.toString());
            return jsonCommandGET.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String createStartCommand() {
        JSONObject jsonCommandSTART = new JSONObject();
        try {
            jsonCommandSTART.put("command","START");
            //Log.e("JSONTest","START command: " + jsonCommandSTART.toString());
            return jsonCommandSTART.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String createStopCommand() {
        JSONObject jsonCommandSTOP = new JSONObject();
        try {
            jsonCommandSTOP.put("command","STOP");
            //Log.e("JSONTest","STOP command: " + jsonCommandSTOP.toString());
            return jsonCommandSTOP.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String createConfigWifiCredentialsCommand(String ssid, String psswd, Boolean enable) {
        JSONObject jsonCommandWiFiCred = new JSONObject();
        try {
            jsonCommandWiFiCred.put("command","SET");
            jsonCommandWiFiCred.put("request","network");
            jsonCommandWiFiCred.put("ssid",ssid);
            //TODO psswd must be encrypted
            jsonCommandWiFiCred.put("password",psswd);
            jsonCommandWiFiCred.put("enable",enable);
            //Log.e("JSONTest","SET command: " + jsonCommandWiFiCred.toString());
            return jsonCommandWiFiCred.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String checkModel(){
        StringBuilder errorMessageBuilder = new StringBuilder();
        if (mDevice.getErrorMessage() != null)
            errorMessageBuilder.append(mDevice.getErrorMessage());
        if (mDevice.getDeviceInfo().getErrorMessage() != null)
            errorMessageBuilder.append(mDevice.getDeviceInfo().getErrorMessage());
        for (Sensor s: mDevice.getSensors()) {
            if(s.getErrorMessage() != null)
                errorMessageBuilder.append(s.getErrorMessage());
            SensorDescriptor sd = s.getSensorDescriptor();
            for (DescriptorParam dp : sd.getDescriptorParams()) {
                if(dp.getErrorMessage() != null)
                    errorMessageBuilder.append(dp.getErrorMessage());
            }
            for (SubSensorDescriptor ssd : sd.getSubSensorDescriptors()){
                if(ssd.getErrorMessage() != null)
                    errorMessageBuilder.append(ssd.getErrorMessage());
                for (DescriptorParam ssdp : ssd.getSubDescriptorParams()) {
                    if(ssdp.getErrorMessage() != null)
                        errorMessageBuilder.append(ssdp.getErrorMessage());
                }
            }

            SensorStatus ss = s.getSensorStatus();
            for (StatusParam sp : ss.getStatusParams()){
                if(sp.getErrorMessage() != null)
                    errorMessageBuilder.append(sp.getErrorMessage());
            }
            for (SubSensorStatus sss : ss.getSubSensorStatusList()){
                if(sss.getErrorMessage() != null)
                    errorMessageBuilder.append(sss.getErrorMessage());
                for (StatusParam sssp : sss.getParams()) {
                    if(sssp.getErrorMessage() != null)
                        errorMessageBuilder.append(sssp.getErrorMessage());
                }
            }
        }
        return errorMessageBuilder.toString().equals("") ? null : errorMessageBuilder.toString();
    }

    @Override
    public void encapsulateAndSend(String message) {
        byte[] bytesToSend = encapsulate(message);
        //mConsole.write(bytesToSend,0,bytesToSend.length);
        BLESendTask bleSendTask = new BLESendTask();
        bleSendTask.execute(bytesToSend);
        //mSTWINConf.sendWrite(bytesToSend);
        //getNode().writeCharRawTest(mSTWINConf,bytesToSend);

        //int nByteSent = mConsole.write(bytesToSend,0,bytesToSend.length);
        //Log.e("TEST_writeBLE","nByteSent: " + nByteSent);
    }

    public void setIsLogging(Boolean isLogging){
        this.isLogging = isLogging;
    }

    @Override
    public Boolean isLogging() {
        return this.isLogging;
    }

    class BLESendTask extends AsyncTask<byte[], Void, Void> {
        @Override
        protected Void doInBackground(byte[]... bytesToSend)
        {
            byte[] data = bytesToSend[0];
            ((FeatureHSDatalogConfig)mHSDFeature).sendWrite(data);
            return null;
        }
    }
}
