package vhCommunication;
import java.io.*;
import edu.usc.ict.vhmsg.*;
import vhMsgProcessor.*;

// Send the VHmsg as needed,both text and NVB message.
public class VHSender {

    public static VHMsg vhmsg;
    public String name = "Brad";
    public String msgtype = "text";

    public int numMessagesReceived = 0;
    public int m_testSpecialCases = 0;
    VHMsgSpliter vhmsgspliter = new VHMsgSpliter();
    NVBMsgProcessor nvbMsg = new NVBMsgProcessor();
    TextMsgProcessor textMsg = new TextMsgProcessor();

    public void setChar(String name) {
        this.name = name;
    }
    
    public void setMsgType(String msgtype) {
        this.msgtype = msgtype;
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

    public void sendMessage(String name, String content, String msgtype) {
        if (msgtype.equals("text")) {
        	vhmsg.sendMessage(textMsg.constructTextMsg(name, content));
        }
        else if (msgtype.equals("NVB")) {
        	vhmsg.sendMessage(nvbMsg.constructNVBMsg(name, content));
        }    	
    }
    
    public void sendMessage(String content, String msgtype) {
        if (msgtype.equals("text")) {
        	vhmsg.sendMessage(textMsg.constructTextMsg(this.name, content));
        }
        else if (msgtype.equals("NVB")) {
        	vhmsg.sendMessage(nvbMsg.constructNVBMsg(this.name, content));
        } 	
    }
}
