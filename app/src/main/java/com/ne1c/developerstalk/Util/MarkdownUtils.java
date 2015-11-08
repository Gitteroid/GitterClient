package com.ne1c.developerstalk.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static final Pattern SINGLELINE_CODE_PATTERN = Pattern.compile("(?!``)`(.*?)(?!``)`"); // `code`
    private static final Pattern MULTILINE_CODE_PATTERN = Pattern.compile("```(.*?|\\n)+```");  // ```code```
    private static final Pattern BOLD_PATTERN = Pattern.compile("[*]{2}(.*?)[*]{2}"); // **bold**
    private static final Pattern ITALICS_PATTERN = Pattern.compile("[*](.*?)[*]"); // *italics*
    private static final Pattern STRIKETHROUGH_PATTERN = Pattern.compile("~{2}.*?~{2}"); // ~~strikethrough~~
    private static final Pattern QUOTE_PATTERN = Pattern.compile(">\\s(.*[\\n]?)+"); // > blockquote
    private static final Pattern ISSUE_PATTERN = Pattern.compile("#.*?\\S"); // #123
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[.*?\\S]\\((\\bhttp\\b|\\bhttps\\b):\\/\\/.*?\\)"); // [title](http://)
    private static final Pattern IMAGE_LINK_PATTERN = Pattern.compile("!\\[\\balt\\b]\\((\\bhttp\\b|\\bhttps\\b):\\/\\/.*?\\)"); // ![alt](http://)

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

    private List<String> convertWithPatterns(String message) {
        message = message.replaceAll(SINGLELINE_CODE_PATTERN.pattern(), "{" + String.valueOf(SINGLELINE_CODE) + "}");
        message = message.replaceAll(MULTILINE_CODE_PATTERN.pattern(), "{" + String.valueOf(MULTILINE_CODE) + "}");
        message = message.replaceAll(BOLD_PATTERN.pattern(), "{" + String.valueOf(BOLD) + "}");
        message = message.replaceAll(ITALICS_PATTERN.pattern(), "{" + String.valueOf(ITALICS) + "}");
        message = message.replaceAll(STRIKETHROUGH_PATTERN.pattern(), "{" + String.valueOf(STRIKETHROUGH) + "}");
        message = message.replaceAll(QUOTE_PATTERN.pattern(), "{" + String.valueOf(QUOTE) + "}");
        message = message.replaceAll(ISSUE_PATTERN.pattern(), "{" + String.valueOf(ISSUE) + "}");
        message = message.replaceAll(IMAGE_LINK_PATTERN.pattern(), "{" + String.valueOf(IMAGE_LINK) + "}");
        message = message.replaceAll(LINK_PATTERN.pattern(), "{" + String.valueOf(LINK) + "}");

        List<String> list = new ArrayList<>();
        Matcher matcher = Pattern.compile("\\{\\d\\}").matcher(message);
        String[] splitted = message.split("\\{\\d\\}");

        if (matcher.find()) {
            int i = 0;
            do {
                if (splitted.length > 0 && !splitted[i].equals("\\n") && !splitted[i].isEmpty()) {
                    list.add(splitted[i].replaceAll("\\n", ""));
                }

                try {
                    list.add(matcher.group(0));
                } catch (IndexOutOfBoundsException | IllegalStateException e) {
                    System.out.print(e);
                }

                i++;
                matcher.find();
            } while (i < splitted.length);

        } else {
            list.addAll(Arrays.asList(splitted));
        }

        return list;
    }

    private List<String> readMultilineCode(String message) {
        Matcher matcher = MULTILINE_CODE_PATTERN.matcher(message);

        if (mMultilineCode != null) {
            return mMultilineCode;
        }

        if (matcher.find()) {
            mMultilineCode = new ArrayList<>();

            do {
                String code = matcher.group(0).replace("```", "");
                code = code.substring(1, code.length() - 1); // remove \n
                mMultilineCode.add(code);
            } while (matcher.find());

            // remove new line


            return mMultilineCode;
        }

        return Collections.emptyList();
    }

    private List<String> readSinglelineCode(String message) {
        Matcher matcher = SINGLELINE_CODE_PATTERN.matcher(message);

        if (mSinglelineCode != null) {
            return mSinglelineCode;
        }

        if (matcher.find()) {
            mSinglelineCode = new ArrayList<>();

            do {
                mSinglelineCode.add(matcher.group(0).replace("`", ""));
            } while (matcher.find());

            return mSinglelineCode;
        }

        return Collections.emptyList();
    }

    private List<String> readBold(String message) {
        Matcher matcher = BOLD_PATTERN.matcher(message);

        if (mBold != null) {
            return mBold;
        }

        if (matcher.find()) {
            mBold = new ArrayList<>();

            do {
                mBold.add(matcher.group(0).replace("**", ""));
            } while (matcher.find());

            return mBold;
        }

        return Collections.emptyList();
    }

    private List<String> readStrikethrough(String message) {
        Matcher matcher = STRIKETHROUGH_PATTERN.matcher(message);

        if (mStrikethrough != null) {
            return mStrikethrough;
        }

        if (matcher.find()) {
            mStrikethrough = new ArrayList<>();

            do {
                mStrikethrough.add(matcher.group(0).replace("~~", ""));
            } while (matcher.find());

            return mStrikethrough;
        }

        return Collections.emptyList();
    }

    private List<String> readQuote(String message) {
        Matcher matcher = QUOTE_PATTERN.matcher(message);

        if (mQuote != null) {
            return mQuote;
        }

        if (matcher.find()) {
            mQuote = new ArrayList<>();

            do {
                mQuote.add(matcher.group(0).replace("> ", "").replaceAll("\\n", ""));
            } while (matcher.find());

            return mQuote;
        }

        return Collections.emptyList();
    }

    private List<String> readItalics(String message) {
        Matcher matcher = ITALICS_PATTERN.matcher(message);

        if (mItalics != null) {
            return mItalics;
        }

        if (matcher.find()) {
            mItalics = new ArrayList<>();

            do {
                mItalics.add(matcher.group(0).replace("*", ""));
            } while (matcher.find());

            return mBold;
        }

        return Collections.emptyList();
    }

    private List<String> readLinks(String message) {
        Matcher matcher = LINK_PATTERN.matcher(message);

        if (mLinks != null) {
            return mLinks;
        }

        if (matcher.find()) {
            mLinks = new ArrayList<>();

            do {
                mLinks.add(matcher.group(0).replaceFirst("\\[]\\(\\bhttp\\b://\\)", ""));
            } while (matcher.find());

            return mLinks;
        }

        return Collections.emptyList();
    }

    private List<String> readImageLinks(String message) {
        Matcher matcher = IMAGE_LINK_PATTERN.matcher(message);

        if (mImageLinks != null) {
            return mImageLinks;
        }

        if (matcher.find()) {
            mImageLinks = new ArrayList<>();

            do {
                mImageLinks.add(matcher.group(0).replaceFirst("!\\[\\balt\\b]\\(", "").replace(")", ""));
            } while (matcher.find());

            return mImageLinks;
        }

        return Collections.emptyList();
    }

    private List<String> readIssues(String message) {
        Matcher matcher = ISSUE_PATTERN.matcher(message);

        if (mIssues != null) {
            return mIssues;
        }

        if (matcher.find()) {
            mIssues = new ArrayList<>();

            do {
                mIssues.add(matcher.group(0).replace("#", ""));
            } while (matcher.find());

            return mIssues;
        }

        return Collections.emptyList();
    }

    public List<String> getSinglelineCode() {
        return readSinglelineCode(mMessage);
    }

    public List<String> getMultilineCode() {
        return readMultilineCode(mMessage);
    }

    public List<String> getBold() {
        return readBold(mMessage);
    }

    public List<String> getStrikethrough() {
        return readStrikethrough(mMessage);
    }

    public List<String> getQuote() {
        return readQuote(mMessage);
    }

    public List<String> getItalics() {
        return readItalics(mMessage);
    }

    public List<String> getLinks() {
        return readLinks(mMessage);
    }

    public List<String> getImageLinks() {
        return readImageLinks(mMessage);
    }

    public List<String> getIssues() {
        return readIssues(mMessage);
    }

    public List<String> getParsedString() {
        return convertWithPatterns(mMessage);
    }

    public boolean existMarkdown() {
        return getSinglelineCode().size() > 0 ||
                getMultilineCode().size() > 0 ||
                getBold().size() > 0 ||
                getStrikethrough().size() > 0 ||
                getQuote().size() > 0 ||
                getItalics().size() > 0 ||
                getImageLinks().size() > 0 ||
                getLinks().size() > 0 ||
                getIssues().size() > 0;
    }
}