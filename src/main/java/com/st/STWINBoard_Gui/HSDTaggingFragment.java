package com.st.STWINBoard_Gui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewManager;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.st.BlueSTSDK.Features.FeatureHSDatalogConfig;
import com.st.BlueSTSDK.HSDatalog.Device;
import com.st.BlueSTSDK.HSDatalog.Tag;
import com.st.STWINBoard_Gui.Control.DeviceManager;
import com.st.STWINBoard_Gui.Utils.HSDAnnotation;
import com.st.STWINBoard_Gui.Utils.HSDAnnotationListAdapter;
import com.st.clab.stwin.gui.R;

import java.util.ArrayList;

/**
 *
 */

public class HSDTaggingFragment extends Fragment {

    public interface HSDInterface {
        void onBackClicked(Device device);
        void onStartLogClicked(Device device);
        void onStopLogClicked(Device device);
    }
    public interface HSDInteractionCallback extends HSDInterface {}

    DeviceManager mDeviceManager;
    private HSDAnnotationListAdapter mAdapter;
    private ArrayList<HSDAnnotation> mAnnotationList;

    private RecyclerView mAnnotationListView;
    private EditText acqNameEditText;
    private EditText acqDescEditText;

    private static HSDInteractionCallback mHSDInteractionCallback;

    public static HSDTaggingFragment newInstance() {

        Bundle args = new Bundle();
        
        HSDTaggingFragment fragment = new HSDTaggingFragment();
        fragment.setArguments(args);

        return fragment;
    }

    public void setOnDoneCLickedCallback(HSDInteractionCallback callback ){
        this.mHSDInteractionCallback = callback;
    }

    @Override
    public void onResume() {
        super.onResume();
        getView().setFocusableInTouchMode(true);
        getView().requestFocus();
        getView().setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
                    mHSDInteractionCallback.onBackClicked(mDeviceManager.getDeviceModel());
                    if (getFragmentManager() != null) {
                        getFragmentManager().popBackStack();
                    }
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        Device device = new Device();
        FeatureHSDatalogConfig hsdFeature = null;
        Boolean isLogging = false;
        Bundle bundle = this.getArguments();
        if (bundle != null) {
            device = bundle.getParcelable("Device");
            hsdFeature = bundle.getParcelable("Feature");
            isLogging = bundle.getBoolean("IsLogging");
        }

        Log.e("TEST","getParcelable------------------------------------------");
        Log.e("TEST","device alias: " + device.getDeviceInfo().getAlias());
        Log.e("TEST","device s/n: " + device.getDeviceInfo().getSerialNumber());

        mAnnotationList = new ArrayList<>();
        for (Tag tag : device.getTags()) {
            //Annotation an = new Annotation(tag.getLabel());
            HSDAnnotation hsdAnnotation;
            if(tag.isSWTag()){
                hsdAnnotation = new HSDAnnotation(tag.getId(), tag.getLabel(),null, HSDAnnotation.TagType.SW);
            } else {
                hsdAnnotation = new HSDAnnotation(tag.getId(), tag.getLabel(),tag.getPinDesc(), HSDAnnotation.TagType.HW);
            }
            hsdAnnotation.setLocked(isLogging);
            mAnnotationList.add(hsdAnnotation);
        }

        mAdapter = new HSDAnnotationListAdapter(getContext());
        mAdapter.setAnnotation(mAnnotationList);
        mAdapter.setOnAnnotationInteractionCallback(new HSDAnnotationListAdapter.HSDAnnotationInteractionCallback() {
            @Override
            public void onLabelChanged(HSDAnnotation selected, String label) {
                removeAcqTextFocus();
                boolean isSW = selected.getTagType() == HSDAnnotation.TagType.SW;
                String jsonConfigTagMessage = mDeviceManager.createConfigTagCommand(
                        selected.getId(),
                        isSW,
                        label
                );
                mDeviceManager.encapsulateAndSend(jsonConfigTagMessage);
                mDeviceManager.setTagLabel(selected.getId(),isSW,selected.getLabel());
                //Log.e("HSDTaggingFragment","onLabelChanged");
            }

            @Override
            public void onLabelInChanging(HSDAnnotation annotation, String label) {
                removeAcqTextFocus();
            }

            @Override
            public void onAnnotationSelected(HSDAnnotation selected) {
                removeAcqTextFocus();
                boolean isSW = selected.getTagType() == HSDAnnotation.TagType.SW;
                String jsonEnableTagMessage = mDeviceManager.createSetTagCommand(
                        selected.getId(),
                        isSW,
                        true
                );
                mDeviceManager.encapsulateAndSend(jsonEnableTagMessage);
                mDeviceManager.setTagEnabled(selected.getId(),isSW,true);
                //Log.e("HSDTaggingFragment","onAnnotationSelected");
            }

            @Override
            public void onAnnotationDeselected(HSDAnnotation deselected) {
                removeAcqTextFocus();
                boolean isSW = deselected.getTagType() == HSDAnnotation.TagType.SW;
                String jsonDisableTagMessage = mDeviceManager.createSetTagCommand(
                        deselected.getId(),
                        isSW,
                        false
                );
                mDeviceManager.encapsulateAndSend(jsonDisableTagMessage);
                mDeviceManager.setTagEnabled(deselected.getId(),isSW,true);
                //Log.e("HSDTaggingFragment","onAnnotationDeselected");
            }

            @Override
            public void onRemoved(HSDAnnotation annotation) {
                //mAnnotationViewModel.remove(annotation);
                Log.e("HSDTaggingFragment","onRemoved");
            }
        });
        mDeviceManager = new DeviceManager();
        mDeviceManager.setDevice(device);
        mDeviceManager.setHSDFeature(hsdFeature);
        mDeviceManager.setIsLogging(isLogging);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_stwin_list_annotation,container,false);
        LinearLayout tagButtonsLayout = view.findViewById(R.id.aiLog_annotation_buttonsLayout);
        ((ViewManager)tagButtonsLayout.getParent()).removeView(tagButtonsLayout);

        mAnnotationListView = view.findViewById(R.id.aiLog_annotation_list);
        mAnnotationListView.setAdapter(mAdapter);
        //mAnnotationListView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));

        LinearLayout acquisitionDescLayout = new LinearLayout(getContext());
        acquisitionDescLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        acquisitionDescLayout.setLayoutParams(lp);
        int px = HSDConfigFragment.getPxfromDp(getResources(),8);
        acquisitionDescLayout.setPadding(px,0,px,0);

        acqNameEditText = new EditText(getContext());
        acqNameEditText.setHint("Acquisition name...");
        HSDConfigFragment.setEditTextMaxLength(acqNameEditText,15);
        acqNameEditText.setText(mDeviceManager.getDeviceModel().getAcquisitionName());
        acqDescEditText = new EditText(getContext());
        acqDescEditText.setHint("Notes...");
        HSDConfigFragment.setEditTextMaxLength(acqDescEditText,100);
        acqDescEditText.setText(mDeviceManager.getDeviceModel().getAcquisitionNotes());
        acqDescEditText.setTextSize(12);

        acquisitionDescLayout.addView(acqNameEditText);
        acquisitionDescLayout.addView(acqDescEditText);

        ConstraintLayout layout = view.findViewById(R.id.mainConstraint);
        ConstraintSet set = new ConstraintSet();

        acquisitionDescLayout.setId(View.generateViewId());
        layout.addView(acquisitionDescLayout,0);
        set.clone(layout);
        set.connect(acquisitionDescLayout.getId(), ConstraintSet.TOP, layout.getId(), ConstraintSet.TOP, 8);
        set.connect(acquisitionDescLayout.getId(), ConstraintSet.BOTTOM, mAnnotationListView.getId(), ConstraintSet.TOP, 8);
        set.connect(mAnnotationListView.getId(), ConstraintSet.TOP, acquisitionDescLayout.getId(), ConstraintSet.BOTTOM, 8);
        set.connect(mAnnotationListView.getId(), ConstraintSet.BOTTOM, layout.getId(), ConstraintSet.BOTTOM, 8);
        set.applyTo(layout);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Log.e("HSDTaggingFragment","onViewCreated");
    }

    private void lockAcquisitionGUI(boolean lock){
        acqNameEditText.setEnabled(!lock);
        acqDescEditText.setEnabled(!lock);
    }

    private void hideSoftKeyboard(View view){
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void removeAcqTextFocus(){
        if (acqNameEditText.hasFocus() || acqDescEditText.hasFocus()) {
            acqNameEditText.clearFocus();
            acqDescEditText.clearFocus();
            hideSoftKeyboard(getView());
            mDeviceManager.setAcquisitionName(acqNameEditText.getText().toString());
            mDeviceManager.setAcquisitionNotes(acqDescEditText.getText().toString());
            String acqInfoCommand = mDeviceManager.createSetAcqInfoCommand(acqNameEditText.getText().toString(),acqDescEditText.getText().toString());
            mDeviceManager.encapsulateAndSend(acqInfoCommand);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.findItem(R.id.startLog).setVisible(false);

        MenuItem stopMenuItem = menu.findItem(R.id.menu_stopSTWIN_HS_Datalog);
        MenuItem startMenuItem = menu.findItem(R.id.menu_startSTWIN_HS_Datalog);

        startMenuItem.getActionView().setOnClickListener(view -> {
            startMenuItem.setVisible(false);
            stopMenuItem.setVisible(true);

            removeAcqTextFocus();
            lockAcquisitionGUI(true);

            for (HSDAnnotation tag : mAnnotationList) {
                tag.setLocked(true);
                if(tag.isEditable()){
                    //Log.e("HSDTaggingFragment","tag " + hsdAnnotation.getLabel() + "IS IN EDIT MODE!!!");
                    String jsonSetLabelMessage = mDeviceManager.createConfigTagCommand(
                            tag.getId(),
                            tag.getTagType() == HSDAnnotation.TagType.SW,
                            tag.getLabel()
                    );
                    mDeviceManager.encapsulateAndSend(jsonSetLabelMessage);
                    tag.setEditable(false);
                }
            }
            mAdapter.notifyDataSetChanged();

            String jsonStartMessage = mDeviceManager.createStartCommand();
            mDeviceManager.encapsulateAndSend(jsonStartMessage);

            mDeviceManager.setIsLogging(true);
            mHSDInteractionCallback.onStartLogClicked(mDeviceManager.getDeviceModel());
        });

        stopMenuItem.getActionView().setOnClickListener(view -> {
            startMenuItem.setVisible(true);
            stopMenuItem.setVisible(false);

            removeAcqTextFocus();
            lockAcquisitionGUI(false);

            for (HSDAnnotation tag : mAnnotationList) {
                tag.setLocked(false);
                tag.setSelected(false);
            }
            mAdapter.notifyDataSetChanged();

            String jsonStopMessage = mDeviceManager.createStopCommand();
            mDeviceManager.encapsulateAndSend(jsonStopMessage);

            mDeviceManager.setIsLogging(false);
            mHSDInteractionCallback.onStopLogClicked(mDeviceManager.getDeviceModel());
        });

        startMenuItem.setVisible(!mDeviceManager.isLogging());
        stopMenuItem.setVisible(mDeviceManager.isLogging());

        super.onCreateOptionsMenu(menu, inflater);
    }

    /**Navigation bar back button management*/
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            mHSDInteractionCallback.onBackClicked(mDeviceManager.getDeviceModel());
            if (getFragmentManager() != null) {
                getFragmentManager().popBackStack();
            }
            return true;
        }
        return false;
    }
}


