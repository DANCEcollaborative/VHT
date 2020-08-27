package test;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class testcoding {
	public static void main(String[] args) throws UnsupportedEncodingException {
		String content =  "%E4%BD%A0%E5%A5%BD";
	    String utfStr = URLEncoder.encode(content,"UTF8");
	    String gbkStr = URLDecoder.decode(content, "UTF8");
	    System.out.println("utfStr" +utfStr);
	    System.out.println("gbkStr is"+gbkStr);
	    System.out.println();
	}

}
