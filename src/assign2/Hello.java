package assign2;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class Hello
 */
@WebServlet("/Hello")
public class Hello extends HttpServlet {
	
	public static int callID;          
	private static long sess_num = 0;
	public static String dataLocation;                  //location metadata

	public static final int SESSIONREAD = 0;             //rpc operation
  	public static final int SESSIONWRITE = 1;            //rpc operation
	public static final int SESSIONDELETE = 2; 			 //rpc operation
	public static final int GETMEMBERS = 3;				 //rpc operation
	
	public static final int MAXPACKETSIZE = 512;         //max packet size	
    private static final int K = 2;                      //K-resilient
    
    private static boolean isCrash = false;
    private static boolean isNewServer;
    private static boolean isInitiated = false;
	
    static RPCReceiver rpcReceiver;                               //rpc server
	static RPCCaller   rpcCaller; 								  //rpc client
	static HashSet<String> membership;							  //hashset used to store membership
	
    static ConcurrentHashMap<String,Session> map;                 //HashTable used to store session states,
    															  //key = sessionID
    															  //value = sessionID,version,message,expiration_timestamp
    
	private static final long   SESSIONDURATION   = 300000;	 	  //cookie duration in millisecond, which is 5 minutes
	private static final int    DELTA             = 10;           //constant that used to calculate discard time, 10 sec
	private static final int    ALPHA             = 10000;		  //constant that used to calculate discard time, 10 sec
	private static final int    COOKIEMAXAGE      = 300;          //cookie duration in second, which is also 5 minutes
	
   
	public void init(ServletConfig config) throws ServletException {
		
		if(!isInitiated){
			
			isNewServer = true;
			
			membership =new HashSet<String>();
			map = new ConcurrentHashMap<String,Session>(); //when initiate, a new hashtable and a new hashset is created
			
			daemon dae = new daemon();					   //daemon is a separate thread that clean the hashtable periodically
			dae.start();						           //initiate the cleaner
			
			rpcReceiver = new RPCReceiver();               //rpcReceiver is a separate thread that listen to the rpc calls
			rpcReceiver.start();
			
			rpcCaller = new RPCCaller();                   //rpcCaller is a class that is responsible for issuing rpc calls
			
			isInitiated =true;
		}
	}


	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		if(isCrash) return;                      //crash simulation, if crashes, give no response
		
   		if(request.getMethod().equals("POST")){  //check if client has clicked a button
   			doPost(request, response);
   			return;
   		}
   		                                         //for just enter of the website without clicking any button
   		updateSession(request, response, null);
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
			
		String[] s = detectButtonPressed(request, response); //determine which button is clicked
   		updateSession(request, response, s);
   		

	}
	
	private String[] detectButtonPressed(HttpServletRequest request, HttpServletResponse response)throws ServletException, IOException{
		Enumeration<String> paramNames = request.getParameterNames();
	    while(paramNames.hasMoreElements()) {
        	String paramName = paramNames.nextElement();
            String[] paramValues =	request.getParameterValues(paramName);
	        if (paramName.equals("NewText")){					
	           	if (paramValues.length == 1) {					//if replace button is clicked
	           													//return a string array with corresponding instructions
	           		String input = paramValues[0];				
	           		String[] operation = new String[2];
	           		
	           		operation[0] = "NewText";
	           		operation[1] = input;
	           		return operation;
	           	}
	        }
	        if (paramName.equals("Refresh")){
	        													//if refresh is clicked, return a string array with corresponding instructions
	        	String[] operation = new String[1];           		
           		operation[0] = "Refresh";
           		return operation; 		
	        }
	        											//clicked "LogOut" Button
	        if (paramName.equals("LogOut")){            //return a string array with corresponding instructions

	        	String[] operation = new String[1];           		
           		operation[0] = "LogOut";
           		return operation; 	
	        }
	        if (paramName.equals("CrashSimulation")){
	        											//if "crashsimulation" is clicked,return a string array with corresponding instructions
	        	String[] operation = new String[1];           		
           		operation[0] = "CrashSimulation";
           		return operation; 		
	        }
	    }
		return null;
	}
	
	private void generateLogOutResponse(HttpServletResponse response)throws ServletException, IOException {
		response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        													//Generate Response HTML Body for logout
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
	    out.println("<meta http-equiv=\"Content-type\" content=\"text/html; charset=utf-8\">");
        out.println("<title>" + "You have successfully log out!" + "</title>");
	    out.println("<link rel=\"stylesheet\" href=\"styles/styles.css\" type=\"text/css\" media=\"screen\">");
        out.println("</head>");
        
        out.println("<body>");
        out.println("<div id=\"content\" class=\"container\">");
        out.println("<div class=\"section grid grid5 s3\">");
        out.println("   <h2>" + "Bye!" +"</h2>");
        out.println("</div>");
        out.println("</div>");
        out.println("</body>"); 
        out.println("</html>");
	}
	private void generateCrashResponse(HttpServletResponse response)throws ServletException, IOException {
		response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        													//Generate Response HTML Body for crash
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
	    out.println("<meta http-equiv=\"Content-type\" content=\"text/html; charset=utf-8\">");
        out.println("<title>" + "Server crashs!" + "</title>");
	    out.println("<link rel=\"stylesheet\" href=\"styles/styles.css\" type=\"text/css\" media=\"screen\">");
        out.println("</head>");
        
        out.println("<body>");
        out.println("<div id=\"content\" class=\"container\">");
        out.println("<div class=\"section grid grid5 s3\">");
        out.println("   <h2>" + "Server crashs!!" +"</h2>");
        out.println("</div>");
        out.println("</div>");
        out.println("</body>"); 
        out.println("</html>");
	}
	private void sessionTimedOut(HttpServletResponse response)throws ServletException, IOException {
		response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        													//Generate Response HTML Body for session timeout
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
	    out.println("<meta http-equiv=\"Content-type\" content=\"text/html; charset=utf-8\">");
        out.println("<title>" + "Session Timed Out" + "</title>");
	    out.println("<link rel=\"stylesheet\" href=\"styles/styles.css\" type=\"text/css\" media=\"screen\">");
        out.println("</head>");
        
        out.println("<body>");
        out.println("<div id=\"content\" class=\"container\">");
        out.println("<div class=\"section grid grid5 s3\">");
        out.println("   <h2>" + "Session Timed Out!" +"</h2>");
        out.println("</div>");
        out.println("</div>");
        out.println("</body>"); 
        out.println("</html>");
	}
	
	private void generateResponse(HttpServletRequest request, HttpServletResponse response, Session session,String[] iPPs)throws ServletException, IOException {
		response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        												//Generate Response HTML Body for normal response
	    out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
	    out.println("<meta http-equiv=\"Content-type\" content=\"text/html; charset=utf-8\">");
        out.println("<title>" + session.getMessage() + "</title>");
	    out.println("<link rel=\"stylesheet\" href=\"styles/styles.css\" type=\"text/css\" media=\"screen\">");
        out.println("</head>");
        
        out.println("<body>");
        //out.println("<h1>" + title + "</h1>");
        out.println("<div id=\"content\" class=\"container\">");
        out.println("<div class=\"section grid grid5 s3\">");
        out.println("   <h2>" + session.getMessage() + " Version:"+session.getVersion() +"</h2>");
        out.println("   <form method=\"POST\" action=\"Hello\"><input type=submit name=Replace value=Replace>&nbsp;&nbsp;<input type=text name=NewText size=40 maxlength=512>&nbsp;&nbsp;");
        out.println("	</form>");
        out.println("	<form method=\"POST\" action=\"Hello\">");
        out.println("	<input type=submit name=Refresh value=Refresh>");
        out.println("	</form>");
        out.println("	<form method=\"POST\" action=\"Hello\">");
        out.println("	<input type=submit name=LogOut value=LogOut>");
        out.println("	</form>");
        out.println("	<form method=\"POST\" action=\"Hello\">");
        out.println("	<input type=submit name=CrashSimulation value=CrashSimulation>");
        out.println("	</form>");
        out.println("	<p>");        
        out.println("	ServerID: "+request.getRemoteAddr()+ "_" + request.getRemotePort());
        out.println("	</p>");
        out.println("	<p>");        
        out.println("	Session data was found in: "+dataLocation);
        out.println("	</p>");
        out.println("	<p>");   
        out.println("	Primary IPP:" + iPPs[0]);
        out.println("	</p>");
        out.println("	<p>");   
        out.print("	Backup IPPs:");
        for(int i=1; i<iPPs.length;i++){
        	out.print(iPPs[i]+",");
        }
        out.println();
        out.println("	</p>");   
        out.println("	<p>");   
        out.println("	Expiration Time:" + new java.util.Date(session.getTimeout()));
        out.println("	</p>");   
        out.println("	<p>");   
        out.println("	Discard Time:" + new java.util.Date(session.getTimeout() + 1000*DELTA + ALPHA));
        out.println("	</p>");   
        out.println("	<p>");        
        out.print("	Membership Set: ");
        for(String s:membership){
        	out.print(s+",");
        }
        out.println();
        out.println("	</p>");
        out.println("    </ul>");
        out.println("</div>");
        out.println("</div>");
        out.println("</body>"); 
        out.println("</html>");
	}

	private Session updateSession(HttpServletRequest request, HttpServletResponse response, String[] operation) throws ServletException, IOException{
		
		boolean newUser = true;
   		Cookie currentUser = null;
   		Cookie[] cookies = request.getCookies();
   		if (cookies != null) {
   			for(Cookie c: cookies) {
   				if ((c.getName().equals("CS5300PROJ1SESSION"))) {    //check all the cookies of the request
   																	 // determine if the client is a new user
   						newUser = false;
   						currentUser = c;
   						break;
   				}
   			}
   		} 
   		
   		boolean needSessionRead = true;
   		String[] IPP = new String[K];              
   		InetAddress[] destAddr = new InetAddress[K];
		int[] destPort = new int[K];	
			
   		if(!newUser){
   			Parse.parseIPP(currentUser.getValue(), IPP);             //if a return user, parse location metadata
   			
   			for(String s:IPP){                                       //determine if the session is located on the server
   				if(s != null){
   					if(s.contains("/")) s=s.substring(1);
   					String local = request.getRemoteAddr()+"_"+rpcReceiver.port;
   					if(s.equals(local)){
   					needSessionRead = false;
   					}else{
   					 membership.add("/"+s);                          //if location metadata contains other ipps, add them to the server membership
   					}
   				}
   			}
   			Parse.parseIPAndPort(IPP, destAddr, destPort);           //separate ipp into ip and port
   		}
   		if(newUser){
   			                                                         //if a new user, compute a new session state
			dataLocation = null;									 //since it is a new user, the session is found in nowhere
   			
   			sess_num++;
			String sessionID = sess_num +"_" +request.getRemoteAddr()+ "_" + rpcReceiver.port;
   			long version = 1;                                        
   			String message ="New user";
   			long expiration_timestamp = new java.util.Date().getTime() + SESSIONDURATION;
   			
   			Session session = new Session(sessionID.toString(),version,message,expiration_timestamp);
   			map.put(sessionID, session);							 //compute a new session state, and put it in the table
   			
   			memberUpdate(request, response, session);                //send the session to other server to maintain k-resilient
			
   			return session;

   		}else if(needSessionRead){          //if not a new user, and session is not on local server
   			dataLocation ="";
   			for(String s:IPP){
   				if(s != null) dataLocation = dataLocation + s + ","; 
   			}
   			
   			if(isNewServer){				//if this is a just boosted server, use getMember method to obtain membership of other servers
   				for( int i=0; i<destAddr.length; i++ ) {
   					if(destAddr[i]==null) break;
   					DatagramPacket recvpkt = rpcCaller.getMembers(MAXPACKETSIZE,destAddr[i],destPort[i]);
   					if(recvpkt != null){
   						String reply = new String(recvpkt.getData(),"UTF-8");
   						if(reply.contains(",")) Parse.parseMembership(reply);
   					}
   				}
   				
   				if(membership != null){
   					isNewServer = false;
   				}
   			}
	
   			String sessionID       = Parse.parseSessionID(currentUser.getValue());
   			long sessionVersionNum = Parse.parseVersionNum(currentUser.getValue());
   			 			
   			if(operation != null){                           //if user clicked logout
   				if(operation[0].equals("LogOut")){
   					
   					rpcCaller.rpcSessionDelete(sessionID,destAddr,destPort);  //delete session in corresponding servers
   					Cookie newCookie = new Cookie("CS5300PROJ1SESSION","");   //delete user cookie
   					newCookie.setMaxAge(0);
   					response.addCookie(newCookie);
   					generateLogOutResponse(response);
   					return null;
   				}
   			}
   															//use session read to obtain session states located on other servers
   			DatagramPacket recPacket = rpcCaller.rpcSessionRead(sessionID,sessionVersionNum, destAddr,destPort);
   			
   			if(recPacket == null){                          //if session on other server and no one reply, the session is timeout
   				sessionTimedOut(response);
   															// the cookie for the timed-out-or-lost session is deleted from the browser.
   				Cookie newCookie = new Cookie("CS5300PROJ1SESSION","");
   	   			newCookie.setMaxAge(0);
   	   			response.addCookie(newCookie);
   				return null;
   			}else{											//if got the session from other servers, update it, put it in local session table
   															//inform other servers to maintain k-resilient
   				Session session = Parse.parseSession(recPacket);
   				session = whichButtonPressed(operation, session, response);
   				
   				if(isCrash){
   					generateCrashResponse(response);
   					return null;
   				}
   				
   				map.put(session.getSessionID(), session);
   				
   	  		   	memberUpdate(request, response, session);
   				return session;
   			}
   			
   			
   		}else{   									// if session is on local server
   			dataLocation = "cache";
   			String sessionID = Parse.parseSessionID(currentUser.getValue()); 
   			
   			if(operation != null){					//if user clicked logout
   				if(operation[0].equals("LogOut")){
   					map.remove(sessionID);          //delete session in local session table and corresponding servers' session tables
   					rpcCaller.rpcSessionDelete(sessionID,destAddr,destPort);
   					Cookie newCookie = new Cookie("CS5300PROJ1SESSION","");  //delete user cookie
   					newCookie.setMaxAge(0);
   					response.addCookie(newCookie);
   					generateLogOutResponse(response);
   					return null;
   				}
   			}
		
   			Session session = map.get(sessionID);                       //if session is not in local session table, because of recover after crash
   			if(session == null){                        
   				Cookie newCookie = new Cookie("CS5300PROJ1SESSION",""); //new booted server doesnot have session, delete user cookie, and reply session timeout
				newCookie.setMaxAge(0);
				response.addCookie(newCookie);
				sessionTimedOut(response);
				return null;
   			}
   			session = whichButtonPressed(operation, session, response); //determine what user clicked, and update session
   			
   			if(isCrash){
   				generateCrashResponse(response);
   				return null;
   			}
   			
   			map.put(sessionID, session);
   		   																
   		   	memberUpdate(request, response, session);                  //send new session to several other servers

   			return session;
   		}
	}

	private Session whichButtonPressed(String[] operation, Session session, HttpServletResponse response) throws ServletException, IOException {
		if(operation != null){
			if(operation[0].equals("NewText")){         //if clicked replace
				session.updateSession(operation[1]);
				
			}else if(operation[0].equals("Refresh")){   //if clicked refresh
				session.updateSessionTimeout();
				
			}else if(operation[0].equals("CrashSimulation")){   //if clicked crash simulation
				
				rpcReceiver.crash();   //stop answering RPC request
				isCrash = true;        //stop answering HTTP request
			}
		}else{
			session.updateSessionTimeout();
		}
		return session;
	}

	private void memberUpdate(HttpServletRequest request, HttpServletResponse response, Session session)throws IOException, ServletException {
		
		String locationMetadata = request.getRemoteAddr() + "_" + rpcReceiver.port;
		DatagramPacket ack = null;
		int count = 0;
		for(String s:membership){
			
			long discardTime = new java.util.Date().getTime() + SESSIONDURATION + 2*1000*DELTA + ALPHA;      //calculate discard time for remote servers
			InetAddress ip = InetAddress.getByName( s.substring(0, s.indexOf('_')).replace('/', ' ').trim());//parse IP and Port from membership
			String portnum = s.substring(s.indexOf('_')+1);
			int port = Integer.parseInt(portnum.trim());
			
			ack = rpcCaller.rpcSessionWrite(session, discardTime, ip, port );     //send session write to each server
			if(ack != null){													  //if got ack
				count++;
				locationMetadata += "_" + ip + "_" + port;
			}
			if(count == K-1) break;												  //if K-1 servers reply, k-resilient is realized
		}
		
		String value = session.getSessionID()+"_"+session.getVersion()+"_"+locationMetadata;
		String[] IPPs = new String[K];
		Parse.parseIPP(value, IPPs);
		generateResponse(request, response, session,IPPs);                        //merely for display primary ipp and backup ipp in website
			
		Cookie newCookie = new Cookie("CS5300PROJ1SESSION",value);				  //calculate new cookie for user
		newCookie.setMaxAge(COOKIEMAXAGE+DELTA);
		response.addCookie(newCookie);
	}
	
	public static void clean(){  //this method is called by daemon thread, which is to delete timeout session states
		deleteTimeoutSession();
    }
	
	private static void deleteTimeoutSession(){
		Set<String> keys = map.keySet();             //first obtain a set of keys in the session state table
		for(String s : keys){                        //then check each session to find out if it has timed out
			Session session = map.get(s);            //if it is, then remove it from the session state table
			if(session.checkTimeout()){
				System.out.println("Deleting timeout Session!!!");
				map.remove(s);
			}
		}
	}

}

