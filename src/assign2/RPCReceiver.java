package assign2;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class RPCReceiver extends Thread{
	
	private static final int RPCTIMEOUT = 1000; //1 seconds
	private DatagramSocket rpcSocket;
	private boolean finished;
	
	public int port;
	
	public RPCReceiver(){
		finished = false;                       //crash simulation variable
		try {
			rpcSocket = new DatagramSocket();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		port = rpcSocket.getLocalPort();       //server port
		Hello.callID = 10000 * rpcSocket.getLocalPort();
	}
	
	public void run(){
		
			try {
				rpcReceive();
			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("receiver has been successfully terminated");
	}
	private void rpcReceive() throws SocketException, IOException {
				
		while(true) {
			if(finished)	return;
			
		    byte[] inBuf = new byte[Hello.MAXPACKETSIZE];
		    DatagramPacket recvPkt = new DatagramPacket(inBuf, inBuf.length);
		    
		    rpcSocket.receive(recvPkt);	    

			if(finished)	return;
		    InetAddress returnAddr = recvPkt.getAddress();
		    int returnPort = recvPkt.getPort();

		    String receivedMessage = new String(recvPkt.getData(),"UTF-8");
		    

			int receivedCallID = Integer.parseInt(receivedMessage.substring(0,receivedMessage.indexOf(','))); //parse callID
			
		    int operationCode = Parse.parseOpCode(receivedMessage); // get requested operationCode
		    
			byte[] outBuf = null;
		    switch( operationCode ) {
		    	
		    	case Hello.SESSIONREAD:
		    		outBuf = SessionRead(recvPkt.getData(), recvPkt.getLength(),receivedCallID);
		    		break;
		    	case Hello.SESSIONWRITE:
		    		outBuf = SessionWrite(recvPkt.getData(), recvPkt.getLength(),receivedCallID);
		    		break;
		    	case Hello.SESSIONDELETE:
		    		outBuf = SessionDelete(recvPkt.getData(), recvPkt.getLength(),receivedCallID);
		    		break;
		    	case Hello.GETMEMBERS:
		    		outBuf = GetMembership(recvPkt.getData(), recvPkt.getLength(),receivedCallID,Hello.MAXPACKETSIZE);
		    		break;

		    }
		    // here outBuf should contain the callID and results of the call
		    DatagramPacket sendPkt = new DatagramPacket(outBuf, outBuf.length, returnAddr, returnPort);
		    rpcSocket.send(sendPkt);
		  }
	}
	private byte[] GetMembership(byte[] data, int length, int receivedCallID,int size) {
		String result="";
		for(String s:Hello.membership){
			result = result + "," + s;
		}
		String reply = receivedCallID + result;
		return reply.getBytes();
	}

	private byte[] SessionDelete(byte[] data, int length, int receivedCallID) throws UnsupportedEncodingException {
		String sessionID = Parse.parseSessionID(data);		
		Hello.map.remove(sessionID);
		String reply = receivedCallID +",OK";
		return reply.getBytes();
	}

	private byte[] SessionWrite(byte[] data, int length, int receivedCallID) throws UnsupportedEncodingException {
		String sessionID = Parse.parseSessionID(data);
		long   version   = Parse.parseVersion(data);
		String message   = Parse.parseMessage(data);
		long   discardTime = Parse.parseDiscardTime(data);
		Session s = new Session(sessionID, version, message, discardTime);
		Hello.map.put(sessionID, s);
		String reply = receivedCallID +",ACK";
		return reply.getBytes();
	}
	private byte[] SessionRead(byte[] data, int length, int receivedCallID) throws UnsupportedEncodingException {
		String sessionID = Parse.parseSessionID(data);		
		String reply = receivedCallID +","+ Hello.map.get(sessionID).toString();
		return reply.getBytes();
	}

	public void crash() {
		finished = true;
		System.out.println("crash");
	}
	
}
