package logger

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.st.BlueSTSDK.Feature
import com.st.BlueSTSDK.Features.FeatureHSDatalogConfig
import com.st.BlueSTSDK.HSDatalog.TagHW
import com.st.BlueSTSDK.Node
import com.st.clab.stwin.gui.R

internal class HSDTaggingViewModel :ViewModel(){

    private val tagListener = Feature.FeatureListener { f, sample ->
        val tags = FeatureHSDatalogConfig.getDeviceTags(sample)
        if(tags.isNullOrEmpty())
            return@FeatureListener
        val isLogging = FeatureHSDatalogConfig.isLogging(sample)
        val annotations = tags.map {
            val uiAnnotation = if (it is TagHW) {
                AnnotationViewData(it.id, it.label, it.pinDesc, R.string.annotationView_hwType,it.isEnabled,!isLogging,true)
            } else {
                AnnotationViewData(it.id, it.label, null, R.string.annotationView_swType,it.isEnabled,false,false)
            }
            uiAnnotation
        }

        _annotation.postValue(annotations)

    }

    private var mConfigFeature: FeatureHSDatalogConfig? = null

    private val _annotation = MutableLiveData<List<AnnotationViewData>>(emptyList())
    val annotations:LiveData<List<AnnotationViewData>>
        get() = _annotation

    private val _isEditing = MutableLiveData<Boolean>(false)
    val isLogging:LiveData<Boolean>
        get() = _isEditing

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

    fun onStartStopLogPressed() {
        _isEditing.postValue(!_isEditing.value!!)
    }

    fun removeAnnotation(annotation: AnnotationViewData) {
        val newList = _annotation.value?.filterNot { it.id==annotation.id }
        newList?.let { _annotation.postValue(it) }
    }

    fun addNewTag() {
        val newId = _annotation.value?.maxBy { it.id }?.id?.inc() ?: 0
        val newTag = AnnotationViewData(newId,"Tag $newId",null,R.string.annotationView_swType,false,true)
        val newList = mutableListOf(newTag).apply {
            val oldList = _annotation.value
            if(oldList!=null) {
                addAll(oldList)
            }
        }
        _annotation.postValue(newList)
    }

    fun changeTagLabel(selected: AnnotationViewData, label: String) {
        Log.d("TagViewMode","change to: $label")
    }


    fun selectAnnotation(selected: AnnotationViewData) {
        Log.d("TagViewMode","select to: ${selected.label}")
    }

    fun deSelectAnnotation(selected: AnnotationViewData) {
        Log.d("TagViewMode","deSelect to: ${selected.label}")
    }

}