package com.ne1c.developerstalk.Models;

public class MarkdownTextModel {
    public static final int SINGLELINE_CODE = 0;
    public static final int MULTILINE_CODE = 1;
    public static final int BOLD = 2;
    public static final int ITALICS = 3;
    public static final int STRIKETHROUGH = 4;
    public static final int QUOTE = 5;
    public static final int ISSUE = 6;
    public static final int LINK = 7;
    public static final int IMAGE_LINK = 8;

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
