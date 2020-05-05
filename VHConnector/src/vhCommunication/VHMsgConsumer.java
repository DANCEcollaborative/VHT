package vhCommunication;
import vhMsgProcessor.*;

public class VHMsgConsumer extends Thread {
	 private VHMsgStorage storage;
	 private Object lock;
	 VHSender sender = new VHSender();
	 RendererController controller = new RendererController();
	 VHMsgSpliter vhp = new VHMsgSpliter();
	 NVBMsgProcessor nvbMsg = new NVBMsgProcessor();
	 TextMsgProcessor textMsg = new TextMsgProcessor();   
	    
	    public VHMsgConsumer(Object lock)
	    {
	        this.lock = lock;
	    }
	    
	    public void getValue()
	    {
	        try
	        {
	            synchronized (lock)
	            {
	                if (VHMsgStorage.value.equals(""))
	                    lock.wait();
	                sender.setChar(controller.getCharacter());    	
	    			String type = vhp.typeGetter(VHMsgStorage.value);
	    			String identity = vhp.identityGetter(VHMsgStorage.value);
	    			//String angle = nvbMsg.angleGetter(content);
	    			//String nvbmsg = nvbMsg.constructNVBMsg(angle, content);	        
	    	        sender.sendMessage(VHMsgStorage.value, type);
	                System.out.println("GetµÄÖµÊÇ£º" + VHMsgStorage.value);
	                VHMsgStorage.value = "";
	                lock.notify();
	            }
	        } 
	        catch (InterruptedException e)
	        {
	            e.printStackTrace();
	        }
	    }
	 
}
