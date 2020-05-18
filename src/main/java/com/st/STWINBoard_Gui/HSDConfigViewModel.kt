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

    private val _error = MutableLiveData<Error?>(null)
    val error:LiveData<Error?>
        get() = _error

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
                Log.d("ViewModel",strData)
                stream.close()
            }catch (e: FileNotFoundException){
                e.printStackTrace()
                _error.postValue(Error.FileNotFound)
            }catch (e: IOError){
                e.printStackTrace()
                _error.postValue(Error.ImpossibleReadFile)
            }
        }


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
                stream.write("ciao".toByteArray(Charsets.UTF_8))
                stream.close()
            }catch (e: FileNotFoundException){
                e.printStackTrace()
                _error.postValue(Error.ImpossibleCreateFile)
            }catch (e:IOError){
                e.printStackTrace()
                _error.postValue(Error.ImpossibleWriteFile)
            }
        }
    }

    private var currentConfig:List<Sensor> = emptyList()
    private val _boardConfiguration = MutableLiveData<List<Sensor>>(emptyList())
    val sensorsConfiguraiton:LiveData<List<Sensor>>
        get() = _boardConfiguration

    private var mHSDConfigFeature:FeatureHSDataLogConfig? = null
    private val mSTWINConfListener = Feature.FeatureListener { f: Feature, sample: Feature.Sample? ->
        if (sample == null)
            return@FeatureListener

        val deviceConf = FeatureHSDataLogConfig.getDeviceConfig(sample) ?: return@FeatureListener
        val newConfiguration = deviceConf.sensors ?: return@FeatureListener
        if(currentConfig!=newConfiguration){
            currentConfig = newConfiguration
            _boardConfiguration.postValue(currentConfig)
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
        return currentConfig.getOrNull(sensorId)
                ?.sensorStatus
                ?.subSensorStatusList
                ?.getOrNull(subSensorId)
    }

    fun changeODRValue(sensor: Sensor, subSensor: SubSensorDescriptor, newOdrValue: Double) {
        Log.d("ConfigVM","onSubSensorODRChange ${sensor.id} -> ${subSensor.id} -> $newOdrValue")
        val paramList = listOf(ODRParam(subSensor.id,newOdrValue))
        val ssODRCmd = HSDSetSensorCmd(sensor.id, paramList)
        mHSDConfigFeature?.sendSetCmd(ssODRCmd)
        getSubSensorStatus(sensor.id,subSensor.id)?.odr = newOdrValue
        _boardConfiguration.postValue(currentConfig)
    }

    fun changeFullScale(sensor: Sensor, subSensor: SubSensorDescriptor, newFSValue: Double) {
        Log.d("ConfigVM","onSubSensorFSChange ${sensor.id} -> ${subSensor.id} -> $newFSValue")
        val paramList = listOf(FSParam(subSensor.id,newFSValue))
        val ssFSCmd = HSDSetSensorCmd(sensor.id, paramList)
        mHSDConfigFeature?.sendSetCmd(ssFSCmd)
        getSubSensorStatus(sensor.id,subSensor.id)?.fs = newFSValue
        _boardConfiguration.postValue(currentConfig)
    }

    fun changeSampleForTimeStamp(sensor: Sensor, subSensor: SubSensorDescriptor, newSampleValue: Int) {
        Log.d("ConfigVM","onSubSensorSampleChange ${sensor.id} -> ${subSensor.id} -> $newSampleValue")
        val paramList = listOf(SamplePerTSParam(subSensor.id,newSampleValue))
        val ssSamplePerTSCmd = HSDSetSensorCmd(sensor.id, paramList)
        mHSDConfigFeature?.sendSetCmd(ssSamplePerTSCmd)
        getSubSensorStatus(sensor.id,subSensor.id)?.samplesPerTs = newSampleValue
        _boardConfiguration.postValue(currentConfig)
    }

    fun changeEnableState(sensor: Sensor, subSensor: SubSensorDescriptor, newState: Boolean) {
        Log.d("ConfigVM","onSubSensorEnableChange ${sensor.id} -> ${subSensor.id} -> $newState")
        val paramList = listOf(IsActiveParam(subSensor.id,newState))
        val ssIsActiveCmd = HSDSetSensorCmd(sensor.id, paramList)
        mHSDConfigFeature?.sendSetCmd(ssIsActiveCmd)
        getSubSensorStatus(sensor.id,subSensor.id)?.isActive = newState
        _boardConfiguration.postValue(currentConfig)
    }

}