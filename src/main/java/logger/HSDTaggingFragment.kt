package logger

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.st.BlueSTSDK.Manager
import com.st.BlueSTSDK.Node
import com.st.STWINBoard_Gui.Control.DeviceManager
import com.st.clab.stwin.gui.R
import logger.HSDAnnotationListAdapter.*

/**
 *
 */
class HSDTaggingFragment : Fragment() {

    private val viewModel by viewModels<HSDTaggingViewModel> ()

    private lateinit var mAnnotationListView: RecyclerView
    private lateinit var mAcquisitionName:EditText
    private lateinit var mAcquisitionDesc:EditText
    private lateinit var mStartStopButton:Button
    private lateinit var mAddTagButton:Button

    private val mAdapter: HSDAnnotationListAdapter = HSDAnnotationListAdapter(object : AnnotationInteractionCallback {
        override fun onLabelChanged(annotation: AnnotationViewData, label: String) {
            viewModel.changeTagLabel(annotation,label)
        }

        override fun onAnnotationSelected(selected: AnnotationViewData) {
            viewModel.selectAnnotation(selected)
        }

        override fun onAnnotationDeselected(deselected: AnnotationViewData) {
            viewModel.deSelectAnnotation(deselected)
        }

    })

    var mDeviceManager: DeviceManager? = null

    private val mSwapToDelete = ItemTouchHelper(
            object : ItemTouchHelper.SimpleCallback(0,
                    ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
                override fun onMove(recyclerView: RecyclerView,
                                    viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                    return true // true if moved, false otherwise
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val position = viewHolder.adapterPosition
                    val selectedAnnotation = mAdapter.currentList[position]
                    viewModel.removeAnnotation(selectedAnnotation)
                }
            })

    override fun onResume() {
        super.onResume()
        val node = arguments?.getString(NODE_TAG_EXTRA)?.let {
            Manager.getSharedInstance().getNodeWithTag(it)
        }
        if(node!=null){
            viewModel.enableNotification(node)
        }

    }

    override fun onPause() {
        super.onPause()
        val node = arguments?.getString(NODE_TAG_EXTRA)?.let {
            Manager.getSharedInstance().getNodeWithTag(it)
        }
        if(node!=null){
            viewModel.disableNotification(node)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_log_annotation, container, false)
        mAnnotationListView = view.findViewById(R.id.tagLog_annotation_list)
        mSwapToDelete.attachToRecyclerView(mAnnotationListView)
        mAnnotationListView.adapter = mAdapter

        mAcquisitionName = view.findViewById(R.id.tagLog_acquisitionName_value)
        mAcquisitionDesc = view.findViewById(R.id.tagLog_acquisitionDescription_value)

        mAddTagButton = view.findViewById(R.id.tagLog_annotation_addLabelButton)
        mStartStopButton = view.findViewById(R.id.tagLog_annotation_startButton)
        mStartStopButton.setOnClickListener {
            viewModel.onStartStopLogPressed()
        }

        mAddTagButton.setOnClickListener {
            viewModel.addNewTag()
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.annotations.observe(viewLifecycleOwner, Observer {
            mAdapter.submitList(it)
        })
        viewModel.isLogging.observe(viewLifecycleOwner, Observer {isLogging ->
            enableEditing(!isLogging)
            setupStartStopButtonView(isLogging)
        })
    }

    private fun setupStartStopButtonView(logging: Boolean) {
        //todo add the icon
        if(logging){
            mStartStopButton.setText(R.string.tagLog_stopLog)
        }else{
            mStartStopButton.setText(R.string.tagLog_startLog)
        }
    }

    private fun enableEditing(enable: Boolean) {
        mAcquisitionName.isEnabled = enable
        mAcquisitionDesc.isEnabled = enable
        mAddTagButton.isEnabled = enable
    }


    private fun hideSoftKeyboard(view: View?) {
        val imm = requireContext().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(requireView().windowToken, 0)
    }

    private fun removeAcqTextFocus() {
        if (mAcquisitionName.hasFocus() || mAcquisitionDesc.hasFocus()) {
            mAcquisitionName.clearFocus()
            mAcquisitionDesc.clearFocus()
            hideSoftKeyboard(view)
            mDeviceManager!!.setAcquisitionName(mAcquisitionName.text.toString())
            mDeviceManager!!.setAcquisitionNotes(mAcquisitionDesc.text.toString())
            val acqInfoCommand = mDeviceManager!!.createSetAcqInfoCommand(mAcquisitionName.text.toString(), mAcquisitionDesc.text.toString())
            mDeviceManager!!.encapsulateAndSend(acqInfoCommand)
        }
    }

    companion object {
        val NODE_TAG_EXTRA = HSDTaggingFragment::class.java.name + ".NodeTag"

        fun newInstance(node: Node): Fragment {
            return HSDTaggingFragment().apply {
                arguments = Bundle().apply {
                    putString(NODE_TAG_EXTRA,node.tag)
                }
            }
        }
    }
}