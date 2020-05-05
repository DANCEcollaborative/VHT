package vhCommunication;

public class VHMsgProducer {
private Object lock;
    
    public VHMsgProducer(Object lock)
    {
        this.lock = lock;
    }
    
    public void setValue(String content)
    {
        try
        {
            synchronized (lock)
            {
                if (!VHMsgStorage.value.equals(""))
                    lock.wait();
                String value = content;
                System.out.println("SetµÄÖµÊÇ£º" + value);
                VHMsgStorage.value = value;
                lock.notify();
            }
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }
}
