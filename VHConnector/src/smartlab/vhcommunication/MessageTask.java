package smartlab.vhcommunication;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import javax.jms.JMSException;

import smartlab.vhmsgprocessor.*;

public class MessageTask implements Runnable{
	String content;
	String type;
	private final Map<String,String> identityInfo = new ConcurrentHashMap<>();
	VHSender sender = VHSender.getInstance();
	
	public MessageTask(String content, String type) {
		this.content = content;
		this.type = type;
	}
	@Override
	public void run() {
		try {
			sender.sendMessage(this.content, this.type);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Present Thread:"+Thread.currentThread().getName()+"Content is :"+this.content+"Type is :" +this.type);		
	}

}
