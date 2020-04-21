package test;

import smartlab.communication.CommunicationManager;
import smartlab.test.TestByteSubscriber;
import smartlab.test.TestHybridSubscriber;
import smartlab.test.TestTextSubscriber;
import vhCommunication.psiNvbSubscriber;

public class nvbgcommunicationtest {

	static public void main(String[] args) {
        CommunicationManager manager = new CommunicationManager();
        psiNvbSubscriber textmsg = new psiNvbSubscriber("PSI_NVBG_Location");
        manager.subscribe(textmsg, "PSI_NVBG_Location");
    }
}
