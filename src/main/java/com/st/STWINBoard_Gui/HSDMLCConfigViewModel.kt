package com.st.STWINBoard_Gui

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.st.BlueSTSDK.Features.highSpeedDataLog.FeatureHSDataLogConfig
import com.st.BlueSTSDK.Features.highSpeedDataLog.communication.DeviceModel.Sensor
import com.st.BlueSTSDK.Features.highSpeedDataLog.communication.DeviceModel.SubSensorDescriptor
import com.st.BlueSTSDK.Features.highSpeedDataLog.communication.HSDSetMLCSensorCmd
import com.st.BlueSTSDK.Features.highSpeedDataLog.communication.HSDSetSensorCmd
import com.st.BlueSTSDK.Features.highSpeedDataLog.communication.MLCConfigParam
import com.st.BlueSTSDK.Node
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileNotFoundException
import java.io.IOError

internal class HSDMLCConfigViewModel : ViewModel(){

    private val _error = MutableLiveData<IOConfError>(null)
    val error: LiveData<IOConfError?>
        get() = _error

    private val _isLoading = MutableLiveData(false)
    val isLoading:LiveData<Boolean>
        get() = _isLoading

    private var mHSDConfigFeature: FeatureHSDataLogConfig? =null
    private var mMLCsId = -1
    private var mMLCssId = -1

    fun openLoadMLCConf(sensor: Sensor, subSensor: SubSensorDescriptor){
        Log.d("ConfigVM","onSubSensorEnableChange ${sensor.id} -> ${subSensor.id} -> openMLCConf")
        mMLCsId = sensor.id
        mMLCssId = subSensor.id
    }

    fun attachTo(node: Node){
        mHSDConfigFeature=node.getFeature(FeatureHSDataLogConfig::class.java)
    }

    fun loadUCFFromFile(file: Uri?, contentResolver: ContentResolver){
        if(file == null){
            _error.postValue(IOConfError.InvalidFile)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoading.postValue(true)
                val stream = contentResolver.openInputStream(file)
                if(stream==null){
                    _isLoading.postValue(false)
                    _error.postValue(IOConfError.ImpossibleReadFile)
                    return@launch
                }
                val lineList = mutableListOf<String>()
                //the stream is close by useLine
                stream.bufferedReader(Charsets.UTF_8).useLines { lines ->
                    lines.forEach { if(!isCommentLine(it)) lineList.add(it) }
                }
                val strData = compactLines(lineList)
                val paramList = listOf(MLCConfigParam(mMLCssId,strData.length/2,strData))
                mHSDConfigFeature?.sendSetCmd(
                        HSDSetMLCSensorCmd(mMLCsId,paramList),
                        Runnable {
                            Log.d("MLC","LoadComplete")
                            _isLoading.postValue(false) }
                )
            }catch (e: FileNotFoundException){
                e.printStackTrace()
                _isLoading.postValue(false)
                _error.postValue(IOConfError.FileNotFound)
            }catch (e: IOError){
                e.printStackTrace()
                _isLoading.postValue(false)
                _error.postValue(IOConfError.ImpossibleReadFile)
            }
        }
    }

    private fun isCommentLine(line:String):Boolean{
        return line.startsWith("--")
    }

    private fun compactLines(lines:List<String>):String{
        val isSpace = "\\s+".toRegex()
        return lines.joinToString("") { it.replace(isSpace,"").drop(2)}
    }
}