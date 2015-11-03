package com.ne1c.developerstalk.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Need to testing
public class MarkdownUtils {
    public static final int SINGLELINE_CODE = 0;
    public static final int MULTILINE_CODE = 1;
    public static final int BOLD = 2;
    public static final int ITALICS = 3;
    public static final int STRIKETHROUGH = 4;
    public static final int QUOTE = 5;
    public static final int ISSUE = 6;
    public static final int LINK = 7;
    public static final int IMAGE_LINK = 8;

    private static final Pattern SINGLELINE_CODE_PATTERN = Pattern.compile("(.*)`(.*?)`(.*)"); // `code`
    private static final Pattern MULTILINE_CODE_PATTERN = Pattern.compile("(.*)'''(.*?)'''(.*)");  // '''code'''
    private static final Pattern BOLD_PATTERN = Pattern.compile("(.*)\\*{2}(.*?)\\*{2}(.*)"); // **bold**
    private static final Pattern ITALICS_PATTERN = Pattern.compile("(.*)[*](.*?)[*](.*)"); // *italics*
    private static final Pattern STRIKETHROUGH_PATTERN = Pattern.compile("(.*)~{2}.*?~{2}(.*)"); // ~~strikethrough~~
    private static final Pattern QUOTE_PATTERN = Pattern.compile("(.*)>\\s.*?\\s*(.*)"); // > blockquote
    private static final Pattern ISSUE_PATTERN = Pattern.compile("(.*)#.*?\\S(.*)"); // #123
    private static final Pattern LINK_PATTERN = Pattern.compile("(.*)\\[\\.*?\\S]\\(\\bhttp\\b://.*?\\)(.*)"); // [title](http://)
    private static final Pattern IMAGE_LINK_PATTERN = Pattern.compile("(.*)!\\[\\balt\\b]\\(\\bhttp\\b://.*?\\)(.*)"); // ![alt](http://)

    private final String mMessage;

    private List<String> mMultilineCode;
    private List<String> mSinglelineCode;
    private List<String> mBold;
    private List<String> mItalics;
    private List<String> mStrikethrough;
    private List<String> mQuote;
    private List<String> mLinks;
    private List<String> mImageLinks;
    private List<String> mIssues;

    public MarkdownUtils(String message) {
        if (message == null) {
            throw new NullPointerException("Message is null");
        } else {
            mMessage = message;
        }
    }

    private List convertWithPatterns(String message) {
        String mess = new String(message); // Copy text

        mess.replaceAll(SINGLELINE_CODE_PATTERN.pattern(), "{" + String.valueOf(SINGLELINE_CODE) + "}");
        mess.replaceAll(MULTILINE_CODE_PATTERN.pattern(), "{" + String.valueOf(MULTILINE_CODE) + "}");
        mess.replaceAll(BOLD_PATTERN.pattern(), "{" + String.valueOf(BOLD) + "}");
        mess.replaceAll(ITALICS_PATTERN.pattern(), "{" + String.valueOf(ITALICS) + "}");
        mess.replaceAll(STRIKETHROUGH_PATTERN.pattern(), "{" + String.valueOf(STRIKETHROUGH) + "}");
        mess.replaceAll(QUOTE_PATTERN.pattern(), "{" + String.valueOf(QUOTE) + "}");
        mess.replaceAll(ISSUE_PATTERN.pattern(), "{" + String.valueOf(ISSUE) + "}");
        mess.replaceAll(LINK_PATTERN.pattern(), "{" + String.valueOf(LINK) + "}");
        mess.replaceAll(IMAGE_LINK_PATTERN.pattern(), "{" + String.valueOf(IMAGE_LINK) + "}");

        List<String> list = new ArrayList<>();

        for (String s : mess.split("(.*)\\{\\d\\}(.*)")) {
            list.add(s);
        }

        return list;
    }

    private List readMultilineCode(String message) {
        Matcher matcher = MULTILINE_CODE_PATTERN.matcher(message);

        if (mMultilineCode != null) {
            return mMultilineCode;
        }

        if (matcher.find()) {
            mMultilineCode = new ArrayList<>();

            for (int i = 0; i < matcher.groupCount(); i++) {
                mMultilineCode.add(matcher.group(i).replace("\'\'\'", ""));
            }
            return mMultilineCode;
        }

        return Collections.EMPTY_LIST;
    }

    private List readSinglelineCode(String message) {
        Matcher matcher = SINGLELINE_CODE_PATTERN.matcher(message);

        if (mSinglelineCode != null) {
            return mSinglelineCode;
        }

        if (matcher.find()) {
            mSinglelineCode = new ArrayList<>();

            for (int i = 0; i < matcher.groupCount(); i++) {
                mSinglelineCode.add(matcher.group(i).replace("`", ""));
            }

            return mSinglelineCode;
        }

        return Collections.EMPTY_LIST;
    }

    private List readBold(String message) {
        Matcher matcher = BOLD_PATTERN.matcher(message);

        if (mBold != null) {
            return mBold;
        }

        if (matcher.find()) {
            mBold = new ArrayList<>();

            for (int i = 0; i < matcher.groupCount(); i++) {
                mBold.add(matcher.group(i).replace("**", ""));
            }

            return mBold;
        }

        return Collections.EMPTY_LIST;
    }

    private List readStrikethrough(String message) {
        Matcher matcher = STRIKETHROUGH_PATTERN.matcher(message);

        if (mStrikethrough != null) {
            return mStrikethrough;
        }

        if (matcher.find()) {
            mStrikethrough = new ArrayList<>();

            for (int i = 0; i < matcher.groupCount(); i++) {
                mStrikethrough.add(matcher.group(i).replace("~~", ""));
            }

            return mStrikethrough;
        }

        return Collections.EMPTY_LIST;
    }

    private List readQuote(String message) {
        Matcher matcher = QUOTE_PATTERN.matcher(message);

        if (mQuote != null) {
            return mQuote;
        }

        if (matcher.find()) {
            mQuote = new ArrayList<>();

            for (int i = 0; i < matcher.groupCount(); i++) {
                mQuote.add(matcher.group(i).replace(">", ""));
            }

            return mQuote;
        }

        return Collections.EMPTY_LIST;
    }

    private List readItalics(String message) {
        Matcher matcher = ITALICS_PATTERN.matcher(message);

        if (mItalics != null) {
            return mItalics;
        }

        if (matcher.find()) {
            mItalics = new ArrayList<>();

            for (int i = 0; i < matcher.groupCount(); i++) {
                mItalics.add(matcher.group(i).replace("*", ""));
            }

            return mBold;
        }

        return Collections.EMPTY_LIST;
    }

    private List readLinks(String message) {
        Matcher matcher = LINK_PATTERN.matcher(message);

        if (mLinks != null) {
            return mLinks;
        }

        if (matcher.find()) {
            mLinks = new ArrayList<>();

            for (int i = 0; i < matcher.groupCount(); i++) {
                mLinks.add(matcher.group(i).replaceFirst("\\[]\\(\\bhttp\\b://\\)", ""));
            }

            return mLinks;
        }

        return Collections.EMPTY_LIST;
    }

    private List readImageLinks(String message) {
        Matcher matcher = IMAGE_LINK_PATTERN.matcher(message);

        if (mImageLinks != null) {
            return mImageLinks;
        }

        if (matcher.find()) {
            mImageLinks = new ArrayList<>();

            for (int i = 0; i < matcher.groupCount(); i++) {
                mImageLinks.add(matcher.group(i).replaceFirst("!\\[\\balt\\b]\\(\\bhttp\\b://.*?\\)", ""));
            }

            return mLinks;
        }

        return Collections.EMPTY_LIST;
    }

    private List readIssues(String message) {
        Matcher matcher = ISSUE_PATTERN.matcher(message);

        if (mIssues != null) {
            return mIssues;
        }

        if (matcher.find()) {
            mIssues = new ArrayList<>();

            for (int i = 0; i < matcher.groupCount(); i++) {
                mIssues.add(matcher.group(i).replace("#", ""));
            }

            return mIssues;
        }

        return Collections.EMPTY_LIST;
    }

    public List getSinglelineCode() {
        return readSinglelineCode(mMessage);
    }

    public List getMultilineCode() {
        return readMultilineCode(mMessage);
    }

    public List getBold() {
        return readBold(mMessage);
    }

    public List getStrikethrough() {
        return readStrikethrough(mMessage);
    }

    public List getQuote() {
        return readQuote(mMessage);
    }

    public List getItalics() {
        return readItalics(mMessage);
    }

    public List getLinks() {
        return readLinks(mMessage);
    }

    public List getImageLinks() {
        return readImageLinks(mMessage);
    }

    public List getIssues() {
        return readIssues(mMessage);
    }
}