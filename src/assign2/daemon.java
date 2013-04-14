package assign2;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;


public class daemon {
	

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public void start() {
    	
        final Runnable cleaner = new Runnable() {
        		
                public void run() {
                	Hello.clean();         //call the clean method of Class Hello
                }
        };
        
        final ScheduledFuture<?> beeperHandle = scheduler.scheduleAtFixedRate(cleaner, 5, 5, SECONDS);    //the cleaner will run every 5 seconds

        scheduler.schedule(new Runnable() {						      //the cleaner will run an hour, we can set to one year or more
                public void run() { beeperHandle.cancel(true); }
        }, 60 * 60, SECONDS);
    }
    
    
}
