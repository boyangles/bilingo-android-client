package info.bilingo.bilingoclientapp;

import android.util.Log;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

public class LanguageUtils {
    public static String getDecoratedPhrase(String phrase, String lang) {
        String result = new String(phrase);

        if (lang == "zh" || lang == "zh-CN") {
            String pinyin = getPinYin(phrase);

            if (!pinyin.isEmpty()) {
                result += " (" + pinyin + ")";
            }
        }

        return result;
    }

    private static String getPinYin(String word) {
        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
        format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        format.setToneType(HanyuPinyinToneType.WITH_TONE_MARK);
        format.setVCharType(HanyuPinyinVCharType.WITH_U_UNICODE);

        char[] ch = word.trim().toCharArray();
        StringBuilder rs = new StringBuilder();
        try {
            if (ch.length > 40) {
                return PinyinHelper.toHanYuPinyinString(word, format, " ", true);
            }
            // 解析
            String s_ch;
            String[] temp;
            for (int i = 0; i < ch.length; i++) {
                s_ch = Character.toString(ch[i]);
                if (s_ch.matches("[\u4e00-\u9fa5]+")) {
                    // 汉字
                    temp = PinyinHelper.toHanyuPinyinStringArray(ch[i], format);
                    if (null != temp && temp.length > 0) {
                        rs.append(temp[0]);
                    }
                } else if (s_ch.matches("[\u0030-\u0039]+")) {
                    // 0-9
                    rs.append(s_ch);
                } else if (s_ch.matches("[\u0041-\u005a]+") || s_ch.matches("[\u0061-\u007a]+")) {
                    // a-zA-Z
                    rs.append(s_ch);
                }
            }
        } catch (BadHanyuPinyinOutputFormatCombination e) {
            Log.d("getPinYin", "failed to make get PinYin: " +
                    e.getMessage());
        }
        return rs.toString();
    }
}
