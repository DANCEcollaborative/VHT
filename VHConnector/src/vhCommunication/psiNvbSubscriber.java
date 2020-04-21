package vhCommunication;

import smartlab.communication.ISLTextSubscriber;
import vhMsgProcessor.*;

public class psiNvbSubscriber implements ISLTextSubscriber{
	String name;
	VHSender sender = new VHSender();
	RendererController controller = new RendererController();
	VHMsgSpliter vhp = new VHMsgSpliter();
    NVBMsgProcessor nvbMsg = new NVBMsgProcessor();
    TextMsgProcessor textMsg = new TextMsgProcessor();
	

    public psiNvbSubscriber(String name) {
        this.name = name;
    }

    @Override
    public void onReceive(String topic, String content) {

    	sender.setChar(controller.getCharacter());    
		String type = vhp.typeGetter(content);
		String identity = vhp.identityGetter(content);
		//String angle = nvbMsg.angleGetter(content);
		//String nvbmsg = nvbMsg.constructNVBMsg(angle, content);
        System.out.println("Received string message. Subscriber:" + this.name + "\tTopic: " + topic + "\tContent:" + content);
        sender.sendMessage(content, type);
    }

	
}
