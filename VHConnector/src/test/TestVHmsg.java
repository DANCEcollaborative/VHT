package test;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import smartlab.communication.CommunicationManager;
import smartlab.vhcommunication.PSISubscriber;
import smartlab.vhcommunication.RendererController;
import smartlab.vhcommunication.VHReceiver;
import smartlab.vhcommunication.VHSender;
import smartlab.vhmsgprocessor.NVBMsgProcessor;
import smartlab.vhmsgprocessor.TextMsgProcessor;
import smartlab.vhmsgprocessor.VHMsgSpliter;

public class TestVHmsg {
		public static void main(String[] args) throws UnsupportedEncodingException {
			VHSender sender = new VHSender();
			//VHReceiver reciver = new VHReceiver();
			// vhNvbgReceiver vhNvbgReceiver = new vhNvbgReceiver();
			//RendererController controller = RendererController.getInstance();
			//sender.setChar(controller.getCharacter());
			//CommunicationManager manager = new CommunicationManager();
			//psiNvbSubscriber textmsg = new psiNvbSubscriber("PSI_NVBG_Location");
			// "sbm  bml char Rachel speech \"" + utfStrraw + "\"";
			VHMsgSpliter vhp = VHMsgSpliter.getInstance();
		    NVBMsgProcessor nvbMsg = new NVBMsgProcessor();
		    TextMsgProcessor textMsg = new TextMsgProcessor();	
		    String content =  "明月几时有，把酒问青天";
		    String utfStr = URLEncoder.encode(content,"UTF8");
		    String utfStr123 = new String(content.getBytes("GBK"), "GBK");
		    String name=new String(content.getBytes("UTF8"), "UTF8");
		    //String ufts= URLEncoder.encode( content, "UTF-8");
		    String ufts = content;
		    String suft= "sbm  bml char Rachel speech \""+ufts+"\"";
		    //String suft = new String(sgkb.getBytes(),"UTF-8");
		    System.out.println("utfStr is"+utfStr);
		    System.out.println("utfStr123 is"+utfStr123);
		    System.out.println("name is"+name);
		    System.out.println("suft is"+ufts);
			String s = "send message to : multimodal:true;%;identity:someone;%;speech:"+ufts;
			String type = vhp.typeGetter(s);
			String identity = vhp.identityGetter(s);
			//String[] coordinate = vhp.coordinateGetter(s);
			String text1 = vhp.textGetter(s);
			//String location = nvbMsg.angleGetter(s);
			System.out.println(type + identity + text1+ "11111111");
			sender.sendMessage(s, type);
			//for(String out: coordinate) { System.out.println(out); }
			/*
			 * double[] coordinate1 = vhp.angleCalculate(s); System.out.println(type);
			 * System.out.println(identity); for(double out: coordinate1) {
			 * System.out.println(out); }
			 */
			
			 // manager.subscribe(textmsg, "PSI_NVBG_Location");

			//sender.sendMessage(message);
			System.exit(0);
	}

}