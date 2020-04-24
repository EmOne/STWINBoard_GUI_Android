package com.st.STWINBoard_Gui.Control;

import com.st.BlueSTSDK.HSDatalog.Device;
import com.st.BlueSTSDK.Feature;

import org.json.JSONException;
import org.json.JSONObject;

public interface DeviceManagerInterface {

    String DEVICE_JSON_KEY = "device";
    String DEVICE_INFO_JSON_KEY = "deviceInfo";
    String SERIAL_NUMBER_JSON_KEY = "serialNumber";
    String ALIAS_JSON_KEY = "alias";
    String PART_NUMBER_JSON_KEY = "partNumber";
    String URL_JSON_KEY = "URL";
    String FW_NAME_JSON_KEY = "fwName";
    String FW_VERSION_JSON_KEY = "fwVersion";
    String N_SENSORS_JSON_KEY = "nSensor";
    String SENSOR_JSON_KEY = "sensor";
    String ID_JSON_KEY = "id";
    String SENSOR_DESCRIPTOR_JSON_KEY = "sensorDescriptor";
    String DATA_TYPE_JSON_KEY = "dataType";
    String NAME_JSON_KEY = "name";
    String VALUES_JSON_KEY = "values";
    String MIN_JSON_KEY = "min";
    String MAX_JSON_KEY = "max";
    String SUB_SENSOR_DESCRIPTOR_KEY = "subSensorDescriptor";
    String SENSOR_TYPE_JSON_KEY = "sensorType";
    String DATA_PER_SAMPLE_JSON_KEY = "dataPerSample";
    String UNIT_JSON_KEY = "unit";
    String SENSOR_STATUS_JSON_KEY = "sensorStatus";
    String SUB_SENSOR_STATUS_JSON_KEY = "subSensorStatus";
    String SENSITIVITY_JSON_KEY = "sensitivity";
    String IS_ACTIVE_JSON_KEY = "isActive";
    String TAG_LIST_JSON_KEY = "tagConfig";

    String CPU_USAGE_JSON_KEY = "cpuUsage";
    String BATTERY_VOLTAGE_JSON_KEY = "batteryVoltage";
    String BATTERY_LEVEL_JSON_KEY = "batteryLevel";

    Device getDeviceModel();
    void setDevice(Device device);
    void setDeviceAlias(String alias);
    void setHSDFeature(Feature feature);
    void setTagLabel(int tagId, boolean isSW, String label);
    void setTagEnabled(int tagId, boolean isSW, boolean isEnabled);
    void setAcquisitionName(String name);
    void setAcquisitionNotes(String notes);

    //NOTE privatized
    //Device parseDevice(JSONObject jsonObj)throws JSONException;
    //DeviceInfo parseDeviceInfo(JSONObject jsonDevice) throws JSONException;
    //SensorDescriptor parseSensorDescriptor(JSONObject jsonSensor) throws JSONException;
    //SensorStatus parseSensorStatus(JSONObject jsonSensor) throws  JSONException;
    void parseFWStats(JSONObject jsonObj);

    //NOTE Getters JSON from device model
    JSONObject getJSONfromDevice();

    void setDevice(JSONObject deviceJSON) throws JSONException;
    void setDeviceInfo(JSONObject jsonObj) throws JSONException;
    void setSensorDescriptor(JSONObject jsonObj) throws JSONException;
    void setSensorStatus(JSONObject jsonObj) throws JSONException;

    String getSensorStatusParam(int sensorId, String paramName);
    String getSubSensorStatusParam(int sensorId, int subSensorId, String paramName);

    String createSetDeviceAliasCommand(String alias);
    String createSetSensorStatusParamCommand(int sensorId, String paramName, String value);
    String createSetSubSensorStatusParamCommand(int sensorId, int subSensorId, String paramName, String value);
    String createSetSensorIsActiveCommand(int sensorId);
    String createSetSubSensorIsActiveCommand(int sensorId, Integer subSensorId);
    String createSetTagCommand(int tagId, boolean isSW, boolean enable);
    String createConfigTagCommand(int tagId, boolean isSW, String label);
    String createSetAcqInfoCommand(String name, String notes);

    String createGetRegisterCommand(int sensorId, String address);
    String createGetDeviceCommand();
    String createGetDeviceInfoCommand();
    String createGetSensorDescriptorCommand(int sensorId);
    String createGetSensorStatusCommand(int sensorId);
    String createGetFWStatsCommand();
    String createGetTagListCommand();

    String createStartCommand();
    String createStopCommand();

    String createConfigWifiCredentialsCommand(String ssid, String psswd, Boolean enable);

    String checkModel();

    //NOTE enc and send
    void encapsulateAndSend(String message);

    Boolean isLogging();
}
