package com.ne1c.developerstalk.models;

public class MarkdownTextModel {
    private String mText;
    private int mType;

    public MarkdownTextModel(String text, int type) {
        mText = text;
        mType = type;
    }

    public String getText() {
        return mText;
    }

    public int getType() {
        return mType;
    }
}
