package com.st.STWINBoard_Gui.Utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class HSDAnnotation {

    public enum TagType {
        HW,
        SW,
    }//TagType

    @NonNull
    private String label;
    @NonNull
    private int id;
    @Nullable
    private String pinDesc;
    @NonNull
    private TagType tagType;

    private boolean isSelected;
    private boolean isEditable;
    private boolean isLocked;

    public HSDAnnotation(int id,@NonNull String label, @Nullable String pinDesc, @NonNull TagType tagType) {
        this.label = label;
        this.id = id;
        this.pinDesc = pinDesc;
        this.tagType = tagType;
    }

    @NonNull
    public String getLabel() {
        return label;
    }

    public void setLabel(@NonNull String label) {
        this.label = label;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Nullable
    public String getPinDesc() {
        return pinDesc;
    }

    public void setPinDesc(@Nullable String pinDesc) {
        this.pinDesc = pinDesc;
    }

    @NonNull
    public TagType getTagType() {
        return tagType;
    }

    public void setTagType(@NonNull TagType tagType) {
        this.tagType = tagType;
    }

    public void setLocked(boolean locked) {
        isLocked = locked;
    }

    public void setSelected(boolean isSelected){
        this.isSelected=isSelected;
    }

    public void setEditable(boolean editable) {
        isEditable = editable;
    }

    public boolean isLocked() {
        return this.isLocked;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public boolean isEditable() {
        return isEditable;
    }


}
