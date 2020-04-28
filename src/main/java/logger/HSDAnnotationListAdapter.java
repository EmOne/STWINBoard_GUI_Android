/*
 * Copyright (c) 2019  STMicroelectronics – All rights reserved
 * The STMicroelectronics corporate logo is a trademark of STMicroelectronics
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions
 *   and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of
 *   conditions and the following disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * - Neither the name nor trademarks of STMicroelectronics International N.V. nor any other
 *   STMicroelectronics company nor the names of its contributors may be used to endorse or
 *   promote products derived from this software without specific prior written permission.
 *
 * - All of the icons, pictures, logos and other images that are provided with the source code
 *   in a directory whose title begins with st_images may only be used for internal purposes and
 *   shall not be redistributed to any third party or modified in any way.
 *
 * - Any redistributions in binary form shall not include the capability to display any of the
 *   icons, pictures, logos and other images that are provided with the source code in a directory
 *   whose title begins with st_images.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGE.
 */
package logger;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.st.clab.stwin.gui.R;

import java.util.List;

public class HSDAnnotationListAdapter extends
        ListAdapter<HSDAnnotation,RecyclerView.ViewHolder>{

    HSDAnnotationListAdapter(){
        super(new HSDAnnotationDiffCallback());
    }

    public interface AnnotationInteractionCallback {
        void onAnnotationSelected(HSDAnnotation selected);
        void onAnnotationDeselected(HSDAnnotation deselected);
        void onRemoved(HSDAnnotation annotation);
    }

    public interface HSDAnnotationInteractionCallback extends AnnotationInteractionCallback {
        void onLabelChanged(HSDAnnotation annotation, String label);
        void onLabelInChanging(HSDAnnotation annotation, String label);
    }

    private final static int TYPE_HSD_SW = 2;
    private final static int TYPE_HSD_HW = 3;

    private AnnotationInteractionCallback mCallback;

    //public HSDAnnotationListAdapter(Context context) { mInflater = LayoutInflater.from(context); }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if(viewType == TYPE_HSD_SW) {
            View view = inflater.inflate(R.layout.hsd_log_sw_annotation_item, parent, false);
            return new HSD_SWViewHolder(view);
        }else{
            View view = inflater.inflate(R.layout.hsd_log_hw_annotation_item, parent, false);
            return new HSD_HWViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        switch (getItemViewType(position)){
            case TYPE_HSD_SW:
                ((HSD_SWViewHolder)holder).setAnnotation(getItem(position));
                ((HSD_SWViewHolder)holder).tagSelector.setChecked(getItem(position).isSelected());
                break;
            case TYPE_HSD_HW:
                ((HSD_HWViewHolder)holder).setAnnotation(getItem(position));
                break;
        }
    }

    @Override
    public int getItemViewType(int position) {
        if((getItem(position)).getTagType().equals(HSDAnnotation.TagType.SW))
            return TYPE_HSD_SW;
        else
            return TYPE_HSD_HW;
    }


    public void setOnAnnotationInteractionCallback(AnnotationInteractionCallback callback){
        mCallback = callback;
    }


    class HSD_SWViewHolder extends HSD_HWViewHolder {
        final CompoundButton tagSelector;

        public HSD_SWViewHolder(@NonNull View itemView) {
            super(itemView);
            tagSelector = itemView.findViewById(R.id.hsd_tag_selector);
            tagSelector.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if(mCallback!=null){
                    if(isChecked){
                        mCallback.onAnnotationSelected(currentData);
                        super.tagImageView.setEnabled(false);
                        super.tagImageView.setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
                        super.currentData.setSelected(true);
                        itemView.getBackground().setColorFilter(Color.parseColor("#dff1a6"),PorterDuff.Mode.SRC_IN);
                    }else{
                        mCallback.onAnnotationDeselected(currentData);
                        super.tagImageView.setEnabled(true);
                        super.tagImageView.clearColorFilter();
                        super.currentData.setSelected(false);
                        itemView.getBackground().clearColorFilter();
                    }
                }
            });
        }

        @Override
        void setAnnotation(HSDAnnotation annotation) {
            super.setAnnotation(annotation);
            if(currentData.isLocked()){
                tagSelector.setEnabled(true);
            } else {
                tagSelector.setEnabled(false);
            }
        }

    }


    class HSD_HWViewHolder extends RecyclerView.ViewHolder {

        HSDAnnotation currentData;

        private EditText tagLabel;
        private TextView pinDesc;
        private TextView tagType;
        private ImageView tagImageView;

        public HSD_HWViewHolder(@NonNull View itemView) {
            super(itemView);
            itemView.setBackground(ContextCompat.getDrawable(itemView.getContext(), R.drawable.tag_list_item_shape));
            tagLabel = itemView.findViewById(R.id.hsd_tag_label);
            pinDesc = itemView.findViewById(R.id.hsd_tag_desc);
            tagType = itemView.findViewById(R.id.hsd_tag_type);
            tagImageView = itemView.findViewById(R.id.hsd_tag_lock);

            tagImageView.setOnClickListener(view -> {
                if(!currentData.isEditable()) {
                    tagImageView.setImageResource(R.drawable.ic_done);
                    currentData.setEditable(true);
                    lockSelectedTag(true,false);
                    itemView.getBackground().setColorFilter(Color.TRANSPARENT,PorterDuff.Mode.SRC_IN);
                    ((HSDAnnotationInteractionCallback)mCallback).onLabelInChanging(currentData,tagLabel.getText().toString());
                }
                else {
                    tagImageView.setImageResource(R.drawable.ic_edit);
                    currentData.setEditable(false);
                    lockSelectedTag(false,false);
                    itemView.getBackground().clearColorFilter();
                    currentData.setLabel(tagLabel.getText().toString());
                    ((HSDAnnotationInteractionCallback)mCallback).onLabelChanged(currentData,tagLabel.getText().toString());
                }
            });
        }

        void setAnnotation(HSDAnnotation annotation){
            currentData = annotation;
            tagLabel.setText(annotation.getLabel());
            pinDesc.setText(annotation.getPinDesc());
            tagType.setText(annotation.getTagType() == HSDAnnotation.TagType.SW ? "SW" : "HW");

            if(currentData.isLocked()){
                tagImageView.setVisibility(View.INVISIBLE);
            } else {
                tagImageView.setVisibility(View.VISIBLE);
            }

            lockSelectedTag(currentData.isEditable(),currentData.isSelected());
        }

        /**
         * lock/unlock tag label EditText
         * @param lock true: edit label, false: lock label
         */
        void lockSelectedTag(boolean lock, boolean isSelected){
            if(lock) {
                tagLabel.getBackground().clearColorFilter();
                tagLabel.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
                tagLabel.setTextColor(Color.GRAY);
                tagImageView.setImageResource(R.drawable.ic_done);
            } else {
                tagLabel.getBackground().setColorFilter(Color.TRANSPARENT, PorterDuff.Mode.SRC_IN);
                tagLabel.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
                tagLabel.setTextColor(Color.BLACK);
                tagImageView.setImageResource(R.drawable.ic_edit);
                if(isSelected)
                    itemView.getBackground().setColorFilter(Color.parseColor("#dff1a6"),PorterDuff.Mode.SRC_IN);
                else
                    itemView.getBackground().clearColorFilter();
            }
            tagLabel.setEnabled(lock);
        }
    }

}
