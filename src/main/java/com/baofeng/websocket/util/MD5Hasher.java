package com.baofeng.websocket.util;


import java.security.MessageDigest;

public class MD5Hasher {


	public  String getMd5Str(final String src){
		try{
            byte[] byteArray =  MessageDigest.getInstance("MD5").digest(src.getBytes("utf-8"));
            StringBuffer md5StrBuff = new StringBuffer();
            for (int i = 0; i < byteArray.length; i++) {
                if (Integer.toHexString(0xFF & byteArray[i]).length() == 1) {
					md5StrBuff.append("0").append(Integer.toHexString(0xFF & byteArray[i]));
				} else {
					md5StrBuff.append(Integer.toHexString(0xFF & byteArray[i]));
				}
            }
            return md5StrBuff.toString();
		}   catch   (Exception   e)   {
			return null;
		}
	}
	public  String getMd5Str(final byte[] src){
		try{
            byte[] byteArray =  MessageDigest.getInstance("MD5").digest(src);
            StringBuffer md5StrBuff = new StringBuffer();
            for (int i = 0; i < byteArray.length; i++) {
                if (Integer.toHexString(0xFF & byteArray[i]).length() == 1) {
					md5StrBuff.append("0").append(Integer.toHexString(0xFF & byteArray[i]));
				} else {
					md5StrBuff.append(Integer.toHexString(0xFF & byteArray[i]));
				}
            }
            return md5StrBuff.toString();
		}   catch   (Exception   e)   {
			return null;
		}
	}
}
