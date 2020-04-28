package logger

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.st.BlueSTSDK.Features.FeatureHSDatalogConfig
import com.st.BlueSTSDK.HSDatalog.Device
import com.st.BlueSTSDK.HSDatalog.TagHW
import com.st.BlueSTSDK.Manager
import com.st.BlueSTSDK.Node
import com.st.STWINBoard_Gui.Control.DeviceManager
import com.st.STWINBoard_Gui.HSDConfigFragment
import com.st.STWINBoard_Gui.HSDConfigFragment.Companion.getPxfromDp
import com.st.STWINBoard_Gui.HSDConfigFragment.Companion.setEditTextMaxLength
import logger.HSDAnnotationListAdapter.HSDAnnotationInteractionCallback
import com.st.clab.stwin.gui.R
import java.util.*

/**
 *
 */
class HSDTaggingFragment : Fragment() {
    interface HSDInteractionCallback {
        fun onBackClicked(device: Device)
        fun onStartLogClicked(device: Device)
        fun onStopLogClicked(device: Device)
    }

    private val viewModel by viewModels<HSDTaggingViewModel> ()

    var mDeviceManager: DeviceManager? = null
    private val mAdapter: HSDAnnotationListAdapter = HSDAnnotationListAdapter()
    private var mAnnotationList: ArrayList<HSDAnnotation>? = null
    private lateinit var mAnnotationListView: RecyclerView
    private var acqNameEditText: EditText? = null
    private var acqDescEditText: EditText? = null
    fun setOnDoneCLickedCallback(callback: HSDInteractionCallback) {
        mHSDInteractionCallback = callback
    }


    override fun onResume() {
        super.onResume()
        requireView().apply {
            isFocusableInTouchMode = true
            requestFocus()
            setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
                if (event.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
                    mHSDInteractionCallback!!.onBackClicked(mDeviceManager!!.deviceModel)
                    parentFragmentManager.popBackStack()
                    return@OnKeyListener true
                }
                false
            })
        }

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        var device = Device()
        var hsdFeature: FeatureHSDatalogConfig? = null
        var isLogging = false
        mAdapter.setOnAnnotationInteractionCallback(object : HSDAnnotationInteractionCallback {
            override fun onLabelChanged(selected: HSDAnnotation, label: String) {
                removeAcqTextFocus()
                val isSW = selected.tagType == HSDAnnotation.TagType.SW
                val jsonConfigTagMessage = mDeviceManager!!.createConfigTagCommand(
                        selected.id,
                        isSW,
                        label
                )
                mDeviceManager!!.encapsulateAndSend(jsonConfigTagMessage)
                mDeviceManager!!.setTagLabel(selected.id, isSW, selected.label)
                //Log.e("HSDTaggingFragment","onLabelChanged");
            }

            override fun onLabelInChanging(annotation: HSDAnnotation, label: String) {
                removeAcqTextFocus()
            }

            override fun onAnnotationSelected(selected: HSDAnnotation) {
                removeAcqTextFocus()
                val isSW = selected.tagType == HSDAnnotation.TagType.SW
                val jsonEnableTagMessage = mDeviceManager!!.createSetTagCommand(
                        selected.id,
                        isSW,
                        true
                )
                mDeviceManager!!.encapsulateAndSend(jsonEnableTagMessage)
                mDeviceManager!!.setTagEnabled(selected.id, isSW, true)
                //Log.e("HSDTaggingFragment","onAnnotationSelected");
            }

            override fun onAnnotationDeselected(deselected: HSDAnnotation) {
                removeAcqTextFocus()
                val isSW = deselected.tagType == HSDAnnotation.TagType.SW
                val jsonDisableTagMessage = mDeviceManager!!.createSetTagCommand(
                        deselected.id,
                        isSW,
                        false
                )
                mDeviceManager!!.encapsulateAndSend(jsonDisableTagMessage)
                mDeviceManager!!.setTagEnabled(deselected.id, isSW, true)
                //Log.e("HSDTaggingFragment","onAnnotationDeselected");
            }

            override fun onRemoved(annotation: HSDAnnotation) {
                //mAnnotationViewModel.remove(annotation);
                Log.e("HSDTaggingFragment", "onRemoved")
            }
        })
        mDeviceManager = DeviceManager()
        mDeviceManager!!.setDevice(device)
        mDeviceManager!!.setHSDFeature(hsdFeature)
        mDeviceManager!!.setIsLogging(isLogging)
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_stwin_list_annotation, container, false)
        val tagButtonsLayout = view.findViewById<LinearLayout>(R.id.aiLog_annotation_buttonsLayout)
        (tagButtonsLayout.parent as ViewManager).removeView(tagButtonsLayout)
        mAnnotationListView = view.findViewById(R.id.aiLog_annotation_list)
        mAnnotationListView.adapter = mAdapter
        //mAnnotationListView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
        val acquisitionDescLayout = LinearLayout(context)
        acquisitionDescLayout.orientation = LinearLayout.VERTICAL
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        acquisitionDescLayout.layoutParams = lp
        val px = getPxfromDp(resources, 8)
        acquisitionDescLayout.setPadding(px, 0, px, 0)
        acqNameEditText = EditText(context)
        acqNameEditText!!.hint = "Acquisition name..."
        setEditTextMaxLength(acqNameEditText!!, 15)
        acqNameEditText!!.setText(mDeviceManager!!.deviceModel.acquisitionName)
        acqDescEditText = EditText(context)
        acqDescEditText!!.hint = "Notes..."
        setEditTextMaxLength(acqDescEditText!!, 100)
        acqDescEditText!!.setText(mDeviceManager!!.deviceModel.acquisitionNotes)
        acqDescEditText!!.textSize = 12f
        acquisitionDescLayout.addView(acqNameEditText)
        acquisitionDescLayout.addView(acqDescEditText)
        val layout: ConstraintLayout = view.findViewById(R.id.mainConstraint)
        val set = ConstraintSet()
        acquisitionDescLayout.id = View.generateViewId()
        layout.addView(acquisitionDescLayout, 0)
        set.clone(layout)
        set.connect(acquisitionDescLayout.id, ConstraintSet.TOP, layout.id, ConstraintSet.TOP, 8)
        set.connect(acquisitionDescLayout.id, ConstraintSet.BOTTOM, mAnnotationListView.getId(), ConstraintSet.TOP, 8)
        set.connect(mAnnotationListView.getId(), ConstraintSet.TOP, acquisitionDescLayout.id, ConstraintSet.BOTTOM, 8)
        set.connect(mAnnotationListView.getId(), ConstraintSet.BOTTOM, layout.id, ConstraintSet.BOTTOM, 8)
        set.applyTo(layout)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.annotations.observe(viewLifecycleOwner, Observer {
            mAdapter.submitList(it)
        })
    }

    private fun lockAcquisitionGUI(lock: Boolean) {
        acqNameEditText!!.isEnabled = !lock
        acqDescEditText!!.isEnabled = !lock
    }

    private fun hideSoftKeyboard(view: View?) {
        val imm = requireContext().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm?.hideSoftInputFromWindow(requireView().windowToken, 0)
    }

    private fun removeAcqTextFocus() {
        if (acqNameEditText!!.hasFocus() || acqDescEditText!!.hasFocus()) {
            acqNameEditText!!.clearFocus()
            acqDescEditText!!.clearFocus()
            hideSoftKeyboard(view)
            mDeviceManager!!.setAcquisitionName(acqNameEditText!!.text.toString())
            mDeviceManager!!.setAcquisitionNotes(acqDescEditText!!.text.toString())
            val acqInfoCommand = mDeviceManager!!.createSetAcqInfoCommand(acqNameEditText!!.text.toString(), acqDescEditText!!.text.toString())
            mDeviceManager!!.encapsulateAndSend(acqInfoCommand)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_stwin_hs_datalog,menu)
        menu.findItem(R.id.startLog).isVisible = false
        val stopMenuItem = menu.findItem(R.id.menu_stopSTWIN_HS_Datalog)
        val startMenuItem = menu.findItem(R.id.menu_startSTWIN_HS_Datalog)
        startMenuItem.actionView.setOnClickListener { view: View? ->
            startMenuItem.isVisible = false
            stopMenuItem.isVisible = true
            removeAcqTextFocus()
            lockAcquisitionGUI(true)
            for (tag in mAnnotationList!!) {
                tag.isLocked = true
                if (tag.isEditable) {
                    //Log.e("HSDTaggingFragment","tag " + hsdAnnotation.getLabel() + "IS IN EDIT MODE!!!");
                    val jsonSetLabelMessage = mDeviceManager!!.createConfigTagCommand(
                            tag.id,
                            tag.tagType == HSDAnnotation.TagType.SW,
                            tag.label
                    )
                    mDeviceManager!!.encapsulateAndSend(jsonSetLabelMessage)
                    tag.isEditable = false
                }
            }
            mAdapter!!.notifyDataSetChanged()
            val jsonStartMessage = mDeviceManager!!.createStartCommand()
            mDeviceManager!!.encapsulateAndSend(jsonStartMessage)
            mDeviceManager!!.setIsLogging(true)
            mHSDInteractionCallback!!.onStartLogClicked(mDeviceManager!!.deviceModel)
        }
        stopMenuItem.actionView.setOnClickListener { view: View? ->
            startMenuItem.isVisible = true
            stopMenuItem.isVisible = false
            removeAcqTextFocus()
            lockAcquisitionGUI(false)
            for (tag in mAnnotationList!!) {
                tag.isLocked = false
                tag.isSelected = false
            }
            mAdapter!!.notifyDataSetChanged()
            val jsonStopMessage = mDeviceManager!!.createStopCommand()
            mDeviceManager!!.encapsulateAndSend(jsonStopMessage)
            mDeviceManager!!.setIsLogging(false)
            mHSDInteractionCallback!!.onStopLogClicked(mDeviceManager!!.deviceModel)
        }
        startMenuItem.isVisible = !mDeviceManager!!.isLogging
        stopMenuItem.isVisible = mDeviceManager!!.isLogging
        super.onCreateOptionsMenu(menu, inflater)
    }

    /**Navigation bar back button management */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            mHSDInteractionCallback!!.onBackClicked(mDeviceManager!!.deviceModel)
            if (fragmentManager != null) {
                fragmentManager?.popBackStack()
            }
            return true
        }
        return false
    }

    companion object {
        val NODE_TAG_EXTRA = HSDTaggingFragment::class.java.name + ".NodeTag"

        private var mHSDInteractionCallback: HSDInteractionCallback? = null
        fun newInstance(node: Node): Fragment {
            return HSDTaggingFragment().apply {
                arguments = Bundle().apply {
                    putString(NODE_TAG_EXTRA,node.tag)
                }
            }
        }
    }
}