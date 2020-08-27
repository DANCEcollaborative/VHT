package smartlab.vhcommunication;
import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;

import edu.usc.ict.vhmsg.*;
import smartlab.vhmsgprocessor.*;

/*
 * Send the VHmsg as needed,both text and NVB message.
 */
public class VHSender {

    public static VHMsg vhmsg;
    public String name = "Rachel";
    public String msgtype = "speech";

    public int numMessagesReceived = 0;
    public int m_testSpecialCases = 0;
    VHMsgSpliter vhmsgspliter = VHMsgSpliter.getInstance();
    NVBMsgProcessor nvbMsg = new NVBMsgProcessor();
    TextMsgProcessor textMsg = new TextMsgProcessor();
    
	private static VHSender instance = new VHSender();
	public static VHSender getInstance() {
		return instance;
	}

    /*
     * get the name of current Char in the VHT
     */
    public void setChar(String name) {
        this.name = name;
    }

    private boolean kbhit()
    {
        try
        {
            return ( System.in.available() != 0 );
        }
        catch (IOException ignored)
        {
        }
        return false;
    }

    public VHSender()
    {
        System.out.println( "VHMSG_SERVER: " + System.getenv( "VHMSG_SERVER" ) );
        System.out.println( "VHMSG_SCOPE: " + System.getenv( "VHMSG_SCOPE" ) );

        vhmsg = new VHMsg();

        boolean ret = vhmsg.openConnection();
        if ( !ret )
        {
            System.out.println( "Connection error!" );
            return;
        }
        System.out.println( "VHSender Created" );
    }

    /*
     * Send the text or Non-verbal behavior(NVB) message basing 
     * @param name : String
     * name of the Char
     * @param content : String
     * the content received from PSI
     * @param msgtype : String
     * the message type of the received message(text/nvb)
     */
    public void sendMessage(String name, String content, String msgtype) throws UnsupportedEncodingException {
        if (msgtype.equals("speech")) {
        	System.out.println("!!!!Messages to!!!!"+name+content+msgtype);
        	String output = textMsg.constructTextMsg(name, content);
        	System.out.println("!!!!Messages to tts!!!!"+name+output);
        	String utfStrraw = URLEncoder.encode(output,"UTF8");
        	String utfstr = this.replacer(utfStrraw);
        	String utfStr1 = new String(output.getBytes("GBK"), "UTF8");
        	System.out.println("!!!!UTF8123111 Messages to tts!!!!"+utfstr);
        	System.out.println("!!!!UTF8111 Messages to tts!!!!"+utfStr1);
        	vhmsg.sendMessage(utfstr);
        }
        else if (msgtype.equals("location")) {
        	vhmsg.sendMessage(nvbMsg.constructNVBMsg(name, content));
        }    	
    }
    
    /*
     * Send the text or Non-verbal behavior(NVB) message basing 
     * @param content : String
     * the content received from PSI
     * @param msgtype : String
     * the message type of the received message(text/nvb)
     */
    public void sendMessage(String content, String msgtype) throws UnsupportedEncodingException {
        if (msgtype.equals("speech")) {
        	System.out.println("!!!!Messages to!!!!"+name+content+msgtype);
        	String output = textMsg.constructTextMsg(name, content);
			/*        	System.out.println("!!!!Messages to tts!!!!"+name+output);
			        	String utfStrraw = URLEncoder.encode(output,"UTF8");
			        	String utfstr = this.replacer(utfStrraw);
			        	String utfStr1 = new String(output.getBytes("GBK"), "UTF8");
			        	System.out.println("!!!!UTF8123113433 Messages to tts!!!!"+utfstr);
			        	System.out.println("!!!!UTF8111 Messages to tts!!!!"+utfStr1);*/
        	vhmsg.sendMessage(output);
        }
        else if (msgtype.equals("location")) {
        	vhmsg.sendMessage(nvbMsg.constructNVBMsg(this.name, content));
        } 	
    }
    
    public static String replacer(String data) {
        try {
           data = data.replaceAll("%(?![0-9a-fA-F]{2})", "%25");
           data = data.replaceAll("\\+", "%2B");
        } catch (Exception e) {
           e.printStackTrace();
        }
        return data;
     }
}
