package com.st.STWINBoard_Gui

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.st.BlueSTSDK.Feature
import com.st.BlueSTSDK.Features.highSpeedDataLog.FeatureHSDataLogConfig
import com.st.BlueSTSDK.Features.highSpeedDataLog.communication.*
import com.st.BlueSTSDK.Features.highSpeedDataLog.communication.DeviceModel.Sensor
import com.st.BlueSTSDK.Features.highSpeedDataLog.communication.DeviceModel.SubSensorDescriptor
import com.st.BlueSTSDK.Features.highSpeedDataLog.communication.DeviceModel.SubSensorStatus
import com.st.BlueSTSDK.Node
import com.st.STWINBoard_Gui.Utils.SaveSettings
import com.st.STWINBoard_Gui.Utils.SensorViewData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileNotFoundException
import java.io.IOError

internal class HSDConfigViewModel : ViewModel(){

    enum class Error{
        InvalidFile,
        FileNotFound,
        ImpossibleReadFile,
        ImpossibleWriteFile,
        ImpossibleCreateFile
    }

    private var mCurrentConfig = mutableListOf<SensorViewData>()
    private val _boardConfiguration = MutableLiveData(mCurrentConfig.toList())
    val sensorsConfiguraiton:LiveData<List<SensorViewData>>
        get() = _boardConfiguration

    private var mHSDConfigFeature:FeatureHSDataLogConfig? = null
    private val mSTWINConfListener = Feature.FeatureListener { f: Feature, sample: Feature.Sample? ->
        if (sample == null)
            return@FeatureListener

        val deviceConf = FeatureHSDataLogConfig.getDeviceConfig(sample) ?: return@FeatureListener
        val newConfiguration = mutableListOf<SensorViewData>()
        val sensors = deviceConf.sensors ?: return@FeatureListener
        newConfiguration.addAll(sensors.map { it.toSensorViewData() })
        if(mCurrentConfig != newConfiguration){
            mCurrentConfig = newConfiguration
            _boardConfiguration.postValue(mCurrentConfig.toList())
        }
    }

    private fun Sensor.toSensorViewData(): SensorViewData {
        return SensorViewData(
                sensor = this,
                isCollapsed = true
                )
    }

    private val _error = MutableLiveData<Error?>(null)
    val error:LiveData<Error?>
        get() = _error

    private val _savedConfiguration = MutableLiveData<List<Sensor>?>(null)
    val savedConfuguration:LiveData<List<Sensor>?>
        get() = _savedConfiguration

    //the UI will flip this flag when it start the view to request the data
    val requestFileLocation = MutableLiveData(false)

    fun loadConfigFromFile(file: Uri?, contentResolver: ContentResolver){
        if(file == null){
            _error.postValue(Error.InvalidFile)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val stream = contentResolver.openInputStream(file)
                if(stream==null){
                    _error.postValue(Error.ImpossibleReadFile)
                    return@launch
                }
                val strData = stream.readBytes().toString(Charsets.UTF_8)
                stream.close()
                val config = DeviceParser.extractSensors(strData)
                if(config == null){
                    _error.postValue(Error.InvalidFile)
                    return@launch
                }
                val newConfig = mutableListOf<SensorViewData>()
                newConfig.addAll(config.map { it.toSensorViewData() })
                applyNewConfig(newConfig)
            }catch (e: FileNotFoundException){
                e.printStackTrace()
                _error.postValue(Error.FileNotFound)
            }catch (e: IOError){
                e.printStackTrace()
                _error.postValue(Error.ImpossibleReadFile)
            }
        }
    }

    private fun List<SensorViewData>.getSensorWithId(id:Int):SensorViewData? = find { it.sensor.id == id }

    private fun applyNewConfig(newConfig: MutableList<SensorViewData>) {
        newConfig.forEach { localSensor ->
            val currentSensor =  mCurrentConfig.getSensorWithId(localSensor.sensor.id) ?: return@forEach

            if(currentSensor.sensor.sensorStatus != localSensor.sensor.sensorStatus){
                //todo CHECK THE DESCRIPTION TO BE COMPATIBLE?
                val updateCommand = buildSensorChangesCommand(localSensor.sensor.id,currentSensor.sensor,localSensor.sensor)
                mHSDConfigFeature?.sendSetCmd(updateCommand)
            }
        }
        mCurrentConfig = newConfig
        _boardConfiguration.postValue(newConfig)
    }

    private fun buildSensorChangesCommand(id: Int, currentSensor: Sensor, newSensor: Sensor): HSDSetSensorCmd{
        val subSensorChanges = mutableListOf<SubSensorStatusParam>()
        newSensor.sensorDescriptor.subSensorDescriptors.forEach { subSensorDesc ->
            val currentStatus = currentSensor.getSubSensorStatusForId(subSensorDesc.id) ?: return@forEach
            val newStatus = newSensor.getSubSensorStatusForId(subSensorDesc.id) ?: return@forEach
            if(currentStatus!=newStatus){
                subSensorChanges.addAll(
                        buildSubSensorStatusParamDiff(subSensorDesc.id,currentStatus,newStatus)
                )
            }
        }
        return HSDSetSensorCmd(id,subSensorChanges)
    }

    private fun buildSubSensorStatusParamDiff(subSensorId: Int,
                                              currentSensor: SubSensorStatus, newSensor: SubSensorStatus):List<SubSensorStatusParam> {
        val diff = mutableListOf<SubSensorStatusParam>()
        if(newSensor.isActive != currentSensor.isActive){
            diff.add(IsActiveParam(subSensorId,newSensor.isActive))
        }
        val odr = newSensor.odr
        if(odr!=null && newSensor.odr != currentSensor.odr){
            diff.add(ODRParam(subSensorId,odr))
        }
        val fs = newSensor.fs
        if(fs!=null && newSensor.fs != currentSensor.fs){
            diff.add(FSParam(subSensorId,fs))
        }
        val ts = newSensor.samplesPerTs
        if(ts!=currentSensor.samplesPerTs){
            diff.add(SamplePerTSParam(subSensorId,ts))
        }
        return diff
    }

    fun storeConfigToFile(file: Uri?, contentResolver: ContentResolver) {
        if(file == null){
            _error.postValue(Error.InvalidFile)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val stream = contentResolver.openOutputStream(file)
                if(stream==null){
                    _error.postValue(Error.ImpossibleWriteFile)
                    return@launch
                }
                val sensors = mCurrentConfig.map { it.sensor }
                val jsonStr = DeviceParser.toJsonStr(sensors)
                stream.write(jsonStr.toByteArray(Charsets.UTF_8))
                stream.close()
                _savedConfiguration.postValue(sensors)
            }catch (e: FileNotFoundException){
                e.printStackTrace()
                _error.postValue(Error.ImpossibleCreateFile)
            }catch (e:IOError){
                e.printStackTrace()
                _error.postValue(Error.ImpossibleWriteFile)
            }
        }
    }

    fun enableNotificationFromNode(node: Node){
        mHSDConfigFeature=node.getFeature(FeatureHSDataLogConfig::class.java)

        mHSDConfigFeature?.apply {
            addFeatureListener(mSTWINConfListener)
            enableNotification()
            sendGetCmd(HSDGetDeviceCmd())
        }
    }

    fun disableNotificationFromNode(node: Node){
        node.getFeature(FeatureHSDataLogConfig::class.java)?.apply {
            removeFeatureListener(mSTWINConfListener)
            disableNotification()
        }
    }

    private fun getSubSensorStatus(sensorId:Int,subSensorId:Int): SubSensorStatus?{
        return mCurrentConfig.getSensorWithId(sensorId)
                ?.sensor?.getSubSensorStatusForId(subSensorId)
    }

    fun changeODRValue(sensor: Sensor, subSensor: SubSensorDescriptor, newOdrValue: Double) {
        Log.d("ConfigVM","onSubSensorODRChange ${sensor.id} -> ${subSensor.id} -> $newOdrValue")
        val paramList = listOf(ODRParam(subSensor.id,newOdrValue))
        val ssODRCmd = HSDSetSensorCmd(sensor.id, paramList)
        mHSDConfigFeature?.sendSetCmd(ssODRCmd)
        getSubSensorStatus(sensor.id,subSensor.id)?.odr = newOdrValue
        _boardConfiguration.postValue(mCurrentConfig.toList())
    }

    fun changeFullScale(sensor: Sensor, subSensor: SubSensorDescriptor, newFSValue: Double) {
        Log.d("ConfigVM","onSubSensorFSChange ${sensor.id} -> ${subSensor.id} -> $newFSValue")
        val paramList = listOf(FSParam(subSensor.id,newFSValue))
        val ssFSCmd = HSDSetSensorCmd(sensor.id, paramList)
        mHSDConfigFeature?.sendSetCmd(ssFSCmd)
        getSubSensorStatus(sensor.id,subSensor.id)?.fs = newFSValue
        _boardConfiguration.postValue(mCurrentConfig.toList())
    }

    fun changeSampleForTimeStamp(sensor: Sensor, subSensor: SubSensorDescriptor, newSampleValue: Int) {
        Log.d("ConfigVM","onSubSensorSampleChange ${sensor.id} -> ${subSensor.id} -> $newSampleValue")
        val paramList = listOf(SamplePerTSParam(subSensor.id,newSampleValue))
        val ssSamplePerTSCmd = HSDSetSensorCmd(sensor.id, paramList)
        mHSDConfigFeature?.sendSetCmd(ssSamplePerTSCmd)
        getSubSensorStatus(sensor.id,subSensor.id)?.samplesPerTs = newSampleValue
        _boardConfiguration.postValue(mCurrentConfig.toList())
    }

    fun changeEnableState(sensor: Sensor, subSensor: SubSensorDescriptor, newState: Boolean) {
        Log.d("ConfigVM","onSubSensorEnableChange ${sensor.id} -> ${subSensor.id} -> $newState")
        val paramList = listOf(IsActiveParam(subSensor.id,newState))
        val ssIsActiveCmd = HSDSetSensorCmd(sensor.id, paramList)
        mHSDConfigFeature?.sendSetCmd(ssIsActiveCmd)
        getSubSensorStatus(sensor.id,subSensor.id)?.isActive = newState
        _boardConfiguration.postValue(mCurrentConfig.toList())
    }

    private fun setCurrentConfAsDefault() {
        Log.d("ConfigVM","set as default")
        mHSDConfigFeature?.sendControlCmd(HSDSaveCmd())
    }

    fun saveConfiguration(saveSettings: SaveSettings){
        if(saveSettings.setAsDefault){
            setCurrentConfAsDefault()
        }
        if(saveSettings.storeLocalCopy){
            requestFileLocation.postValue(true)
        }else{
            _savedConfiguration.postValue(mCurrentConfig.map { it.sensor })
        }
    }

    private fun updateSensorConfig(newSensor: SensorViewData) {
        val sensorIndex = mCurrentConfig.indexOfFirst {
            it.sensor == newSensor.sensor
        }
        mCurrentConfig[sensorIndex] = newSensor
        _boardConfiguration.postValue(mCurrentConfig.toList())
    }

    fun collapseSensor(selected: SensorViewData) {
        Log.d("ConfigViewMode","select to: ${selected.sensor.name}")
        val newSensor = selected.copy(isCollapsed = true)
        updateSensorConfig(newSensor)
    }

    fun expandSensor(selected: SensorViewData) {
        Log.d("ConfigViewMode","select to: ${selected.sensor.name}")
        val newSensor = selected.copy(isCollapsed = false)
        updateSensorConfig(newSensor)
    }

}