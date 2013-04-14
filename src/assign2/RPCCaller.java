package assign2;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;



public class RPCCaller{

	private static final int RPCTIMEOUT = 1000; //duration before timeout, 1 seconds	
	
	public DatagramPacket rpcSessionRead(String sessionID, long sessionVersionNum, InetAddress[] destAddr, int[] destPort) throws IOException{
		
		DatagramSocket rpcSocket = new DatagramSocket();
		int callID = Hello.callID++;
		
		byte[] outBuf = new byte[Hello.MAXPACKETSIZE];
		
		//fill outBuf with [ callID, operationSESSIONREAD, sessionID, sessionVersionNum ]
		String message = callID + "," + Hello.SESSIONREAD + "," + sessionID + "," + sessionVersionNum;
		outBuf = message.getBytes();
		
		for( int i=0; i<destAddr.length; i++ ) {  //for each ipp, send message and wait for reply
			if(destAddr[i]==null) break;

			DatagramPacket sendPkt = new DatagramPacket(outBuf,outBuf.length, destAddr[i], destPort[i]);
			rpcSocket.send(sendPkt);
		
			byte [] inBuf = new byte[Hello.MAXPACKETSIZE];
			DatagramPacket recvPkt = new DatagramPacket(inBuf, inBuf.length);
			try {
				long receivedCallID;
				do {
					rpcSocket.setSoTimeout(RPCTIMEOUT);
					recvPkt.setLength(inBuf.length);	
					rpcSocket.receive(recvPkt);					
					String receivedMessage = new String(recvPkt.getData(), "UTF-8");
					receivedCallID = Integer.parseInt(receivedMessage.substring(0,receivedMessage.indexOf(',')));
				}while( receivedCallID != callID );       //while( the callID in inBuf is not the expected one, repeat listening );
				
				Hello.membership.add(destAddr[i]+"_"+destPort[i]); //got reply, add it in membership

			}catch(InterruptedIOException iioe) {
					    // timeout, delete it from membership
				if(destAddr[i] != null) Hello.membership.remove(destAddr[i]+"_"+ destPort[i]);
				recvPkt = null;
			} catch(IOException ioe) {
					    // other error 
				recvPkt = null;
			}

			if(recvPkt != null){ 
				//Hello.dataLocation = destAddr[i]+":"+destPort[i];
				rpcSocket.close();
				return recvPkt;
			}
		}
		return null;
	}
	
	public DatagramPacket rpcSessionWrite(Session s, long discardTime, InetAddress destAddr, int destPort) throws IOException{
		DatagramSocket rpcSocket = new DatagramSocket();
		int callID = Hello.callID++;
		
		byte[] outBuf = new byte[Hello.MAXPACKETSIZE];
		
		//fill outBuf with [ callID, operationSESSIONWRITE, sessionID, sessionVersionNum ,data and discard time]
		
		String message = callID + "," + Hello.SESSIONWRITE + "," + s.getSessionID() + "," + s.getVersion() + "," + s.getMessage() + "," + discardTime;
		outBuf = message.getBytes();
		
		DatagramPacket sendPkt = new DatagramPacket(outBuf,outBuf.length, destAddr, destPort);

		rpcSocket.send(sendPkt);
		
		byte [] inBuf = new byte[Hello.MAXPACKETSIZE];
		DatagramPacket recvPkt = new DatagramPacket(inBuf, inBuf.length);
		try {
			long receivedCallID;
			do {
				rpcSocket.setSoTimeout(RPCTIMEOUT);
				recvPkt.setLength(inBuf.length);
				rpcSocket.receive(recvPkt);
				String receivedMessage = new String(recvPkt.getData(),"UTF-8");
				receivedCallID = Integer.parseInt(receivedMessage.substring(0,receivedMessage.indexOf(',')));
			}while( receivedCallID != callID ); //while( the callID in inBuf is not the expected one );
			
			Hello.membership.add(destAddr+"_"+destPort);
			
		}catch(InterruptedIOException iioe) {
				    // timeout 
			if(destAddr != null) Hello.membership.remove(destAddr+"_"+ destPort);		
			recvPkt = null;
		} catch(IOException ioe) {
				    // other error 
			recvPkt = null;
		}
		rpcSocket.close();
		return recvPkt;
	}
	
	public DatagramPacket getMembers(int size, InetAddress destAddr, int destPort) throws IOException{

		DatagramSocket rpcSocket = new DatagramSocket();
		int callID = Hello.callID++;
		
		byte[] outBuf = new byte[Hello.MAXPACKETSIZE];
		

		String message = callID + "," + Hello.GETMEMBERS + ",";
		outBuf = message.getBytes();
		
		DatagramPacket sendPkt = new DatagramPacket(outBuf,outBuf.length, destAddr, destPort);

		rpcSocket.send(sendPkt);
		
		byte [] inBuf = new byte[Hello.MAXPACKETSIZE];
		DatagramPacket recvPkt = new DatagramPacket(inBuf, inBuf.length);
		try {
			long receivedCallID;
			do {
				rpcSocket.setSoTimeout(RPCTIMEOUT);
				recvPkt.setLength(inBuf.length);
				rpcSocket.receive(recvPkt);
				String receivedMessage = new String(recvPkt.getData(),"UTF-8");
				if(receivedMessage.contains(",")){
					receivedCallID = Integer.parseInt(receivedMessage.substring(0,receivedMessage.indexOf(',')));
				}else{
					receivedCallID = Integer.parseInt(receivedMessage.substring(0,9));
				}
			}while( receivedCallID != callID ); //while( the callID in inBuf is not the expected one );
			
			Hello.membership.add(destAddr+"_"+destPort);
			
		}catch(InterruptedIOException iioe) {
			// timeout 				
			Hello.membership.remove(destAddr+"_"+ destPort);
			recvPkt = null;
		} catch(IOException ioe) {
				    // other error 
			recvPkt = null;
		}
		rpcSocket.close();
		return recvPkt;
	}


	public void rpcSessionDelete(String sessionID, InetAddress[] destAddr,int[] destPort) throws IOException {
		DatagramSocket rpcSocket = new DatagramSocket();
		int callID = Hello.callID++;
		
		byte[] outBuf = new byte[Hello.MAXPACKETSIZE];
		
		String message = callID + "," + Hello.SESSIONDELETE + "," + sessionID;
		outBuf = message.getBytes();
		
		for( int i=0; i<destAddr.length; i++ ) {
			if(destAddr[i]==null) break;
			if(destAddr[i]!=rpcSocket.getLocalAddress()){
				DatagramPacket sendPkt = new DatagramPacket(outBuf,outBuf.length, destAddr[i],destPort[i] );
				rpcSocket.send(sendPkt);
			}
		}	
		rpcSocket.close();
	}
	
}