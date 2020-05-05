package vhCommunication;

import smartlab.communication.ISLTextSubscriber;
import vhMsgProcessor.*;
import java.util.LinkedList;

/*
 * Subscriber the message from PSI.
 */

public class PSISubscriber implements ISLTextSubscriber{
	String name;
	Object lock = new Object();
	final VHMsgProducer producer = new VHMsgProducer(lock);
    final VHMsgConsumer customer = new VHMsgConsumer(lock);
	VHSender sender = new VHSender();
	RendererController controller = new RendererController();
	VHMsgSpliter vhp = new VHMsgSpliter();
    NVBMsgProcessor nvbMsg = new NVBMsgProcessor();
    TextMsgProcessor textMsg = new TextMsgProcessor();
    VHMsgStorage storage = new VHMsgStorage() ;
    
	

    public PSISubscriber(String name) {
        this.name = name;
    }


    @Override    
    public void onReceive(String topic, String content){
    	Runnable producerRunnable = new Runnable()
        {
            public void run()
            {
                while (true)
                {
                    producer.setValue(content);
                }
            }
        };
        Runnable customerRunnable = new Runnable()
        {
            public void run()
            {
                while (true)
                {
                    customer.getValue();
                }
            }
        };
        Thread producerThread = new Thread(producerRunnable);
        Thread CustomerThread = new Thread(customerRunnable);
        producerThread.start();
        CustomerThread.start();
    }
    	
    }
