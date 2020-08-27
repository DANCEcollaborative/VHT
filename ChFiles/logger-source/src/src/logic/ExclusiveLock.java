/*
 * This class is created for using exclusive locks in Java
 * instead of using reentrant locks which allow the same thread to keep locking
 */

package src.logic;

/**
 *
 * @author suri
 */
public class ExclusiveLock {

    private boolean isLocked = false;

    //to lock
    //if locked, wait
    //when unlocked, acquire lock
  public synchronized void lock()
  throws InterruptedException{
    while(isLocked){
      wait();
    }
    isLocked = true;
  }

  //unlock
  //Then notify all the waiting threads
  public synchronized void unlock(){
    isLocked = false;
    notify();
  }

}
