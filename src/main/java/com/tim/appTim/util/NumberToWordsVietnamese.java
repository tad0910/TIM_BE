package com.tim.appTim.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

public class NumberToWordsVietnamese {

    private static final String[] unitNumbers = {
            "không", "một", "hai", "ba", "bốn",
            "năm", "sáu", "bảy", "tám", "chín"
    };

    private static final String[] placeValues = {
            "", "nghìn", "triệu", "tỷ"
    };

    public static String convert(BigDecimal amount) {
        if (amount == null) return "";
        long number = amount.longValue();
        if (number == 0) return "Không đồng";

        String sNumber = String.valueOf(number);
        String sReturn = "";
        int length = sNumber.length();

        int i = 0;
        while (i * 3 < length) {
            String group;
            if (length > (i + 1) * 3) {
                group = sNumber.substring(length - (i + 1) * 3, length - i * 3);
            } else {
                group = sNumber.substring(0, length - i * 3);
            }

            String read = readGroup(group);
            if (!read.isEmpty()) {
                sReturn = read + " " + placeValues[i] + " " + sReturn;
            }
            i++;
        }

        sReturn = sReturn.trim();
        sReturn = sReturn.replaceAll("\\s+", " ");
        sReturn = sReturn.substring(0, 1).toUpperCase() + sReturn.substring(1) + " đồng chẵn";

        return sReturn;
    }

    private static String readGroup(String sNumber) {
        String result = "";
        int len = sNumber.length();

        if (len == 1) sNumber = "00" + sNumber;
        if (len == 2) sNumber = "0" + sNumber;

        String s1 = sNumber.substring(0, 1);
        String s2 = sNumber.substring(1, 2);
        String s3 = sNumber.substring(2, 3);

        if (!s1.equals("0")) {
            result = unitNumbers[Integer.parseInt(s1)] + " trăm";
        } else if (!s2.equals("0") || !s3.equals("0")) {
            if (sNumber.length() > 3) result = "không trăm";
        }

        if (!s2.equals("0")) {
            if (s2.equals("1")) {
                result += " mười";
            } else {
                result += " " + unitNumbers[Integer.parseInt(s2)] + " mươi";
            }
        } else if (!s3.equals("0") && !s1.equals("0")) {
            result += " linh";
        }

        if (!s3.equals("0")) {
            if (s3.equals("1") && !s2.equals("0") && !s2.equals("1")) {
                result += " mốt";
            } else if (s3.equals("5") && !s2.equals("0")) {
                result += " lăm";
            } else {
                result += " " + unitNumbers[Integer.parseInt(s3)];
            }
        }

        return result;
    }

    public static String formatMoney(BigDecimal amount) {
        if (amount == null) return "0";

        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator(',');

        DecimalFormat formatter = new DecimalFormat("#,###", symbols);
        return formatter.format(amount);
    }
}
