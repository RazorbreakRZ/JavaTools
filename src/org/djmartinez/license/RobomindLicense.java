package org.djmartinez.license;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

public class RobomindLicense {

    static final char[] hexChar = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
    static String alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    static String sleutel = "FNvHIf2cpk1DS79YGgL5PBqOUnT3h6eV0CuyZKEWA8XzQsRoaJwbdrxMmit4jl";

    static int getSalt(){
        return ((int)(Math.random() * 10000.0D));
    }

    static int getChecksum(String s) {
        int sum = 0;
        for (char c : s.toCharArray()) {
            sum += c;
        }
        return sum ^ 0x55555555;
    }

    static String getLicenseDetails(){
        String SEP = "|";
        String type = "I" + getSalt();
        String owner = System.getenv("USERNAME");
        int quantity = 1;
        String duration = "inf";
        String details = type + SEP + owner + SEP + quantity + SEP + duration;
        String order = "" + getChecksum(details);
        String checksum = "" + getChecksum(details + SEP + order);
        return details + SEP + order + SEP + checksum;
    }

    static String encode(String normal) {
        normal = unicodeEscape(normal.trim());
        char[] normalChars = normal.toCharArray();
        for (int i = 0; i < normalChars.length; i++) {
            char c = normalChars[i];
            int ix = alphabet.indexOf(c);
            normalChars[i] = (ix >= 0 ? sleutel.charAt(ix) : c);
        }
        return new String(normalChars);
    }

    static String decode(String chinese) {
        chinese = chinese.trim();
        if (!chinese.matches("[a-zA-Z0-9\\-]+")) {
            return null;
        }
        char[] chineseChars = chinese.toCharArray();
        for (int i = 0; i < chineseChars.length; i++) {
            char c = chineseChars[i];
            int ix = sleutel.indexOf(c);
            chineseChars[i] = (ix >= 0 ? alphabet.charAt(ix) : c);
            }
        return unicodeUnescape(new String(chineseChars));
    }

    static String unicodeEscape(String s) {
        try {
            StringBuilder sb = new StringBuilder();
            Pattern normal = Pattern.compile("[a-zA-Z0-9]");
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (!normal.matcher("" + c).matches()) {
                    sb.append("-");
                    sb.append(hexChar[(c >> '\f' & 0xF)]);
                    sb.append(hexChar[(c >> '\b' & 0xF)]);
                    sb.append(hexChar[(c >> '\004' & 0xF)]);
                    sb.append(hexChar[(c & 0xF)]);
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        } catch (Exception e) {
            System.err.println(e);
        }
        return null;
    }

    static String unicodeUnescape(String s) {
        try {
            StringBuilder sb = new StringBuilder();
            char[] chars = s.toCharArray();
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c == '-') {
                    int val = Integer.parseInt("" + chars[(++i)] + chars[(++i)] + chars[(++i)] + chars[(++i)], 16);
                    sb.append((char)val);
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        } catch (NumberFormatException e) {
            System.err.println(e);
        }
        return null;
    }

    static String getPart(String licenseDetails, int i){
        try {
            return licenseDetails.split("\\|")[i];
        } catch (Exception e) {
            System.err.println(e);
        }
        return null;
    }

    static String getExpirationDateShortString(String licenseDetails) {
        return DateFormat.getDateInstance(3, Locale.US).format(getExpirationDate(licenseDetails));
    }

    static Date getExpirationDate(String licenseDetails) {
        DateFormat dateFormatter = DateFormat.getDateInstance(3, Locale.US);
        try {
            return dateFormatter.parse(getPart(licenseDetails,3));
        } catch (Exception e) {
            System.err.println(e);
        }
        return null;
    }

    static boolean isDateValid(String licenseDetails) {
        String datePart = getPart(licenseDetails,3);
        if (datePart == null)
           return false;
        if (datePart.toLowerCase().equals("inf"))
            return true;
        Date ed = getExpirationDate(licenseDetails);
        return (ed != null) && (ed.after(new Date()));
    }

    static boolean isCheckSumValid(String licenseDetails) {
        String sumPart = getPart(licenseDetails,4);
        if (sumPart == null)
            return false;
        try {
            int s = Integer.parseInt(sumPart);
            if (s == 0)
                return true;
            String toSign = getPart(licenseDetails,0) + "|" + getPart(licenseDetails,1) + "|" + getPart(licenseDetails,2) + "|" + getPart(licenseDetails,3);
            return getChecksum(toSign) == s;
        }
        catch (NumberFormatException e) {
            System.err.println(e);
        }
        return false;
    }

    static public boolean isValid(String license)
    {
        return (license != null) && (isCheckSumValid(license)) && (isDateValid(license));
    }





    public static void main(String[] args){
        System.out.println("Generating license key...");
        String licenseDetails = getLicenseDetails();
        String license = encode(licenseDetails);
        System.out.println("DETAILS: [" + licenseDetails + "]");
        System.out.println("LICENSE: [" + license + "][" + isValid(decode(license)) + "]");

    }
}
