package smartlab.vhcommunication;
import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;

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
        	String temp = textMsg.constructTextMsg(this.name, content);
        	String s = new String(temp.getBytes("UTF-8"),"UTF-8");
        	vhmsg.sendMessage(new String(s.getBytes("GKB"),"GKB"));
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
        	System.out.println("!!!!Messages to!!!!"+content+msgtype);
        	String temp = textMsg.constructTextMsg(this.name, content);
        	System.out.println(temp);
		    //String s=new String(temp.getBytes("GKB"),"utf-8");
			//System.out.println(s);*/
            //String ufts= URLDecoder.decode(temp,"utf-8");
            //String ufts= new String(temp.getBytes("utf-8"),"utf-8");
        	String ufts = temp;
            System.out.println(ufts);
        	vhmsg.sendMessage(ufts);
        }
        else if (msgtype.equals("location")) {
        	vhmsg.sendMessage(nvbMsg.constructNVBMsg(this.name, content));
        } 	
    }
}
