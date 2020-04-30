package logger

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.st.BlueSTSDK.Feature
import com.st.BlueSTSDK.Features.FeatureHSDatalogConfig
import com.st.BlueSTSDK.HSDatalog.Tag
import com.st.BlueSTSDK.HSDatalog.TagHW
import com.st.BlueSTSDK.Node
import com.st.clab.stwin.gui.R

internal class HSDTaggingViewModel :ViewModel(){

    private val tagListener = Feature.FeatureListener { f, sample ->
        /*
        val isLogging = FeatureHSDatalogConfig.isLogging(sample)
        if(isLogging != _isLogging.value){
            _isLogging.postValue(isLogging)
        }

        val annotation = FeatureHSDatalogConfig.getDeviceTags(sample)
        if(annotation.isNullOrEmpty())
            return@FeatureListener

        mAnnotationViewDataList.clear()
        mAnnotationViewDataList.addAll(annotation.map { it.toAnnotationViewData() })
        _annotation.postValue(mAnnotationViewDataList)
*/
    }

    private fun Tag.toAnnotationViewData():AnnotationViewData{
        return if (this is TagHW) {
            AnnotationViewData(id = this.id,
                    label = this.label,
                    pinDesc = this.pinDesc,
                    tagType = R.string.annotationView_hwType,
                    isSelected = this.isEnabled,
                    userCanEditLabel = !(this@HSDTaggingViewModel.isLogging.value ?: false),
                    userCanSelect = false)
        } else {
            AnnotationViewData(id = this.id,
                    label = this.label,
                    pinDesc = null,
                    tagType = R.string.annotationView_swType,
                    isSelected = this.isEnabled,
                    userCanEditLabel = !(this@HSDTaggingViewModel.isLogging.value ?: false),
                    userCanSelect = true)
        }
    }

    private var mConfigFeature: FeatureHSDatalogConfig? = null
    private val mAnnotationViewDataList = mutableListOf<AnnotationViewData>()
    private val _annotation = MutableLiveData<List<AnnotationViewData>>(mAnnotationViewDataList)
    val annotations:LiveData<List<AnnotationViewData>>
        get() = _annotation

    private val _isLogging = MutableLiveData<Boolean>(false)
    val isLogging:LiveData<Boolean>
        get() = _isLogging

    fun enableNotification(node: Node){
        mConfigFeature = node.getFeature(FeatureHSDatalogConfig::class.java)
        mConfigFeature?.apply {
            addFeatureListener(tagListener)
            enableNotification()
        }
        val lastSample = mConfigFeature?.sample
        if(lastSample!=null){
            tagListener.onUpdate(mConfigFeature!!,lastSample)
        }
    }

    fun disableNotification(node:Node){
        mConfigFeature?.apply {
            removeFeatureListener(tagListener)
            disableNotification()

        }
    }

    private fun startLog(){
        disableLabelEditing()
        _isLogging.postValue(true)
    }

    private fun stopLog(){
        enableLabelEditing()
        _isLogging.postValue(false)
    }

    private fun enableLabelEditing() {
        val currentStatus = _annotation.value
        val nextStatus = currentStatus?.map { it.copy(userCanEditLabel = true) }
        _annotation.postValue(nextStatus)
    }

    private fun disableLabelEditing() {
        val currentStatus = _annotation.value
        val nextStatus = currentStatus?.map { it.copy(userCanEditLabel = false) }
        _annotation.postValue(nextStatus)
    }

    fun onStartStopLogPressed() {
        if(_isLogging.value==true){
            stopLog()
        }else{
            startLog()
        }
    }

    fun removeAnnotation(annotation: AnnotationViewData) {
        val annotationIndex = mAnnotationViewDataList.indexOfFirst { it.id == annotation.id }
        if(annotationIndex == -1)
            return
        mAnnotationViewDataList.removeAt(annotationIndex)
        _annotation.postValue(mAnnotationViewDataList.toList())
    }

    fun addNewTag() {
        val newId = mAnnotationViewDataList.maxBy { it.id }?.id?.inc() ?: 0
        val newTag = AnnotationViewData(newId,"Tag $newId",null,R.string.annotationView_swType,false,true,userCanSelect = true)
        mAnnotationViewDataList.add(0,newTag)
        _annotation.postValue(mAnnotationViewDataList.toList())
    }

    fun changeTagLabel(selected: AnnotationViewData, label: String) {
        val newAnnotation = selected.copy(label = label)
        updateAnnotation(newAnnotation)
    }

    private fun updateAnnotation(newAnnotation: AnnotationViewData) {
        val annotationIndex = mAnnotationViewDataList.indexOfFirst { it.id == newAnnotation.id }
        mAnnotationViewDataList[annotationIndex] = newAnnotation
        _annotation.postValue(mAnnotationViewDataList.toList())
    }


    fun selectAnnotation(selected: AnnotationViewData) {
        Log.d("TagViewMode","select to: ${selected.label}")
        val newAnnotation = selected.copy(isSelected = true)
        updateAnnotation(newAnnotation)
    }

    fun deSelectAnnotation(selected: AnnotationViewData) {
        Log.d("TagViewMode","deSelect to: ${selected.label}")
        val newAnnotation = selected.copy(isSelected = false)
        updateAnnotation(newAnnotation)
    }

}