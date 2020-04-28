package logger

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.st.BlueSTSDK.Feature
import com.st.BlueSTSDK.Features.FeatureHSDatalogConfig
import com.st.BlueSTSDK.HSDatalog.TagHW
import com.st.BlueSTSDK.Node

class HSDTaggingViewModel :ViewModel(){

    private val tagListener = Feature.FeatureListener { f, sample ->
        val tags = FeatureHSDatalogConfig.getDeviceTags(sample)
        if(tags.isNullOrEmpty())
            return@FeatureListener
        val isLogging = FeatureHSDatalogConfig.isLogging(sample)
        val annotations = tags.map {
            val uiAnnotation = if (it is TagHW) {
                HSDAnnotation(it.id, it.label, it.pinDesc, HSDAnnotation.TagType.HW)
            } else {
                HSDAnnotation(it.id, it.label, null, HSDAnnotation.TagType.SW)
            }
            uiAnnotation.isLocked = isLogging
            uiAnnotation
        }

        _annotation.postValue(annotations)

    }

    private var mConfigFeature: FeatureHSDatalogConfig? = null

    private val _annotation = MutableLiveData<List<HSDAnnotation>>(emptyList())
    val annotations:LiveData<List<HSDAnnotation>>
        get() = _annotation


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


}