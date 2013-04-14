package assign2;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.servlet.http.Cookie;

public class Session {
	private static final long sessionDuration = 300000; //duration of a session in session state table in millisecond
	
	private String sessionID;							
	private long   version;
	private String message;
	private long   timeout;                             //in millisecond
	
	public Session(String sesID,long ver, String msg,  long time){
		sessionID = sesID;
		message = msg;
		version = ver;
		timeout = time;
	}
	
	public void updateSession(String msg){    //update the session state with new message
		message = msg;
		version++;
		timeout = new java.util.Date().getTime() + sessionDuration;
	}
	
	public void updateSessionWithoutUpdatingVersion(String msg){
		message = msg;
	}
	
	public void updateSessionTimeout(){       
		timeout = new java.util.Date().getTime() + sessionDuration;
		version++;
	}
	
	public boolean checkTimeout(){            //check if a session state has timed out
		boolean delete = false;
		long currentTime = new java.util.Date().getTime();
		if(timeout < currentTime){
			delete = true;
		}
		return delete;
	}
		
	public String getMessage(){
		return message;
	}
	
	public long getVersion(){
		return version;
	}
	
	public String getSessionID(){
		return sessionID;
	}
	
	public long getTimeout(){
		return timeout;
	}
	
	public @Override String toString(){
		return sessionID + "," + version + "," + message + "," + timeout;		
	}
}
