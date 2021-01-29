package util;

import sun.misc.BASE64Decoder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class CryUtil {

    private static final Charset UTF_8 = Charset.forName("UTF-8");


    private static MessageDigest md5;

    static {
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    private byte[] cryBytes;

    public CryUtil() {
    }

    public CryUtil(String cryStr) {
        if (cryStr != null && !"".equals(cryStr)) {
            this.cryBytes = getMd5(cryStr).getBytes(UTF_8);

        }
    }

    public String cryStr(String str, boolean isEncode) {
        if (!(str == null || "".equals(str)) && this.cryBytes != null) {
            BASE64Decoder decoder = new BASE64Decoder();
            if (isEncode) {
                str = new sun.misc.BASE64Encoder().encode(str.getBytes(UTF_8));
            }
            byte[] bytes = str.getBytes(UTF_8);
            this.cryBytes(bytes);
            str = new String(bytes, UTF_8);
            if (!isEncode) {
                try {
                    str = new String(decoder.decodeBuffer(str), UTF_8);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return str;
    }

    public String cryFilenameStr(String str, boolean isEncode) {
        String name;
        if (isEncode) {
            name = this.cryStr(str, true);
            name = this.base64EncodeStr(name);
            name = name.replaceAll("/", "@1@").replaceAll("\r", "@2@").replaceAll("\n", "@3@");
            return name;
        } else {
            str = str.replaceAll("@1@", "/").replaceAll("@2@", "\r").replaceAll("@3@", "\n");
            name = this.base64DecodeStr(str);
            return this.cryStr(name, false);
        }
    }

    public String base64EncodeStr(String str) {
        return new sun.misc.BASE64Encoder().encode(str.getBytes(UTF_8));
    }

    public String base64DecodeStr(String str) {
        BASE64Decoder decoder = new BASE64Decoder();
        try {
            return new String(decoder.decodeBuffer(str), UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "23";
    }

    public void cryBytes(byte[] data) {
        this.cryBytes(data, 0, data.length);
    }

    public void cryBytes(byte[] data, int length) {
        this.cryBytes(data, 0, length);
    }

    public void cryBytes(byte[] data, int offset, int length) {
        for (int i = offset; i < length; i++) {
            data[i] ^= this.cryBytes[i / (length > this.cryBytes.length ? length : 1)];
        }
    }

    public static String getMd5(String string) {
        byte[] bs = md5.digest(string.getBytes(UTF_8));
        StringBuilder sb = new StringBuilder(40);
        for (byte x : bs) {
            if ((x & 0xff) >> 4 == 0) {
                sb.append("0").append(Integer.toHexString(x & 0xff));
            } else {
                sb.append(Integer.toHexString(x & 0xff));
            }
        }
        return sb.toString();
    }

    public static String getMD5Three(File f) {
        BigInteger bi = null;
        try {
            byte[] buffer = new byte[8192];
            int len;
            MessageDigest md = MessageDigest.getInstance("MD5");
            FileInputStream fis = new FileInputStream(f);
            while ((len = fis.read(buffer)) != -1) {
                md.update(buffer, 0, len);
            }
            fis.close();
            byte[] b = md.digest();
            bi = new BigInteger(1, b);
        } catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }
        return bi.toString(16);
    }
}
