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
import com.st.BlueSTSDK.Features.highSpeedDataLog.communication.DeviceModel.Sensor
import com.st.BlueSTSDK.Node
import com.st.STWINBoard_Gui.Utils.SensorViewAdapter
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

    private val currentConfig:List<Sensor> = emptyList()
    private val _boardConfiguration = MutableLiveData<List<Sensor>>(emptyList())
    val sensorsConfiguraiton:LiveData<List<Sensor>>
        get() = _boardConfiguration

    private val mSTWINConfListener = Feature.FeatureListener { f: Feature, sample: Feature.Sample? ->
        if (sample == null)
            return@FeatureListener

        val deviceConf = FeatureHSDataLogConfig.getDeviceConfig(sample) ?: return@FeatureListener
        val newConfiguration = deviceConf.sensors ?: return@FeatureListener
        if(currentConfig!=newConfiguration){
            _boardConfiguration.postValue(newConfiguration)
        }
    }

    fun enableNotificationFromNode(node: Node){
        node.getFeature(FeatureHSDataLogConfig::class.java)?.apply {
            addFeatureListener(mSTWINConfListener)
            enableNotification()
            sendGETDevice()
        }
    }

    fun disableNotificationFromNode(node: Node){
        node.getFeature(FeatureHSDataLogConfig::class.java)?.apply {
            removeFeatureListener(mSTWINConfListener)
            disableNotification()
        }
    }

}