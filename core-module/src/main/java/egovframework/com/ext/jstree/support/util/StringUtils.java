package egovframework.com.ext.jstree.support.util;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;


public class StringUtils extends org.apache.commons.lang.StringUtils {
    public static String getString(String text) {
        if (null == text) {
            return "";
        }
        
        return text;
    }
    
    public static String getString(Date text) {
        if (null == text) {
            return "";
        }
        
        return text.toString();
    }
    
    public static boolean getBoolean(String text) {
        if (equalsIgnoreCase(text, "true") || equalsIgnoreCase(text, "1")) {
            return true;
        }
        
        return false;
    }
    
    public static String getString(int value) {
        return ((Integer) value).toString();
    }
    
    public static String getString(long value) {
        return ((Long) value).toString();
    }
    
    public static String getString(boolean flag) {
        if (flag) {
            return "1";
        } else {
            return "0";
        }
    }
    
    public static String makeIpv4Cidr(String ip) {
        if (!StringUtils.contains(ip, "/")) {
            return ip + "/32";
        }
        
        return ip;
    }
    
    public static String[] makeIpv4(String ipMask) {
        if (!StringUtils.contains(ipMask, "/")) {
            String[] ips = { ipMask, "32" };
            return ips;
        } else {
            return StringUtils.split(ipMask, "/");
        }
    }
    
    public static String[] getTypeForIp(String value) {
        String type = "0";
        String mask = "32";
        String ip = value;
        if (contains(value, "-")) {
            type = "2";
            String[] temp = split(value, "/");
            ip = temp[0];
        } else if (contains(value, "/")) {
            type = "1";
            String[] temp = split(value, "/");
            ip = temp[0];
            mask = temp[1];
            
            if (StringUtils.equals(mask, "32")) {
                type = "0";
            }
        }
        
        String[] result = { ip, type, mask };
        return result;
    }
    
    public static List<List<String>> diffMembers(List<String> oldMember, List<String> newMember) {
        return diffMembers(oldMember.toArray(new String[oldMember.size()]), newMember.toArray(new String[newMember.size()]));
    }
    
    public static List<List<String>> diffMembersLong(List<Long> oldMember, List<Long> newMember) {
        List<String> newMembers = new ArrayList<>();
        List<String> oldMembers = new ArrayList<>();
        for (Long tempLong : oldMember) {
            oldMembers.add(StringUtils.getString(tempLong));
        }
        for (Long tempLong : newMember) {
            newMembers.add(StringUtils.getString(tempLong));
        }
        
        return diffMembers(oldMembers, newMembers);
    }
    
    public static List<List<String>> diffMembers(String[] oldMember, String[] newMember) {
        Map<String, Integer> oldMap = new LinkedHashMap<>();
        List<String> newMembers = new ArrayList<>();
        if (null != oldMember) {
            for (String string : oldMember) {
                oldMap.put(string, 0);
            }
        }
        
        if (null != newMember && newMember.length > 0) {
            for (String string : newMember) {
                if (oldMap.containsKey(string)) {
                    oldMap.put(string, 1);
                } else {
                    newMembers.add(string);
                }
            }
        }
        
        List<String> updateMembers = new ArrayList<>();
        List<String> deleteMembers = new ArrayList<>();
        
        for (Map.Entry<String, Integer> entry : oldMap.entrySet()) {
            if (entry.getValue() > 0) {
                updateMembers.add(entry.getKey());
            } else {
                deleteMembers.add(entry.getKey());
            }
        }
        
        List<List<String>> result = new ArrayList<>();
        result.add(deleteMembers);
        result.add(newMembers);
        result.add(updateMembers);
        
        return result;
    }
    
    
    public static String removeMember(String members, String removeMember) {
        String members2 = ";" + members + ";";
        String result = StringUtils.replace(members2, removeMember + ";", "");
        
        return StringUtils.substring(result, 1, result.length() - 1);
    }
    
    public static String getFullURL(HttpServletRequest request) {
        String requestURL = request.getRequestURI();
        String queryString = request.getQueryString();
        
        if (queryString == null) {
            return requestURL.toString();
        } else {
            return requestURL + "?" + queryString;
        }
    }
    
    public static String changeApplyIcon(String applyFlag) {
        if (StringUtils.equals(applyFlag, "1")) {
            return "<img src=\"/files/image/icon/apply.png\" class=\"imgTop\" />";
        }
        return "";
    }
    
    public static String substringBeforeLastByte(String str, String encoding, int cutByte) throws UnsupportedEncodingException {
        int subStrByte = str.getBytes(encoding).length - cutByte;
        
        if(subStrByte <= 0) {
            return "";
        }
        
        String temp = str;
        while (subStrByte < temp.getBytes(encoding).length) {
            temp = substring(temp, 0, temp.length() - 1);
        }
        
        return temp;
    }
    
    public static String[] splitStringByNewLineOrTab(String str) {
        
        return str.split("(\r\n)|\r|\n|\t");
    }
}
