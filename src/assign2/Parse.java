package assign2;

import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Parse {
	
	public static String parseSessionID(String value){         //given cookie value, return sessionID of the cookie
		String result = value.substring(0,value.indexOf("_")+1);
		value = value.substring(value.indexOf("_")+1);
		result += value.substring(0,value.indexOf("_")+1);
		value = value.substring(value.indexOf("_")+1);
		result += value.substring(0,value.indexOf("_"));
		return result;
	}
	public static String parseSessionID(byte[] data) throws UnsupportedEncodingException {
														//parse sessionID from received data
		String result = new String(data, "UTF-8");
		result = result.substring(result.indexOf(",")+1);
		result = result.substring(result.indexOf(",")+1);
		if(result.contains(","))	return result.substring(0,result.indexOf(","));
		else                        return result;
	}
	public static long parseVersionNum(String value){  //given cookie value, return version number of the cookie
		value = value.substring(value.indexOf("_")+1);
		value = value.substring(value.indexOf("_")+1);
		value = value.substring(value.indexOf("_")+1);		
		return Long.parseLong(value.substring(0,value.indexOf("_")));
	}
	public static void parseIPP(String value, String[] IPP) {   //parse IPP from location metadata
		
		String result = value.substring(value.indexOf("_")+1);  //after sess num
		result = result.substring(result.indexOf("_")+1);       //after ip
		result = result.substring(result.indexOf("_")+1);       //after port
		result = result.substring(result.indexOf("_")+1);       //after version
		
		for (int i = 0; i < IPP.length; i++) {
			
			String ip = result.substring(0,result.indexOf("_"));
			String port;
			result = result.substring(result.indexOf("_")+1);
			if(result.indexOf("_") != -1){
				port = result.substring(0,result.indexOf("_"));
				result = result.substring(result.indexOf("_")+1);
			}else{
				port = result;
				IPP[i] = ip+"_"+port;
				break;
			}
			IPP[i] = ip+"_"+port;
		}
	}
	public static void parseIPAndPort(String[] IPP, InetAddress[] destAddr,int[] destPort) throws UnknownHostException {
		for(int i=0; i<IPP.length;i++){
			if(IPP[i] == null) break;
			if(IPP[i].contains("/")){
				destAddr[i] = InetAddress.getByName( IPP[i].substring(1, IPP[i].indexOf('_')));
			}else{
				destAddr[i] = InetAddress.getByName( IPP[i].substring(0, IPP[i].indexOf('_')));
			}
			destPort[i] = Integer.parseInt(IPP[i].substring(IPP[i].indexOf('_')+1));
		}
	}

	public static int parseOpCode(String data) {                //parse operation code
		String result = data.substring(data.indexOf(',')+1);
		return Integer.parseInt(result.substring(0,result.indexOf(',')));
	}
	public static long parseVersion(byte[] data) throws UnsupportedEncodingException {

		String result = new String(data, "UTF-8");
		result = result.substring(result.indexOf(",")+1);
		result = result.substring(result.indexOf(",")+1);
		result = result.substring(result.indexOf(",")+1);
		return Long.parseLong(result.substring(0,result.indexOf(",")));
	}
	public static String parseMessage(byte[] data) throws UnsupportedEncodingException {

		String result = new String(data, "UTF-8");
		result = result.substring(result.indexOf(",")+1);
		result = result.substring(result.indexOf(",")+1);
		result = result.substring(result.indexOf(",")+1);
		result = result.substring(result.indexOf(",")+1);
		return result.substring(0,result.indexOf(","));
	}
	public static long parseDiscardTime(byte[] data) throws UnsupportedEncodingException {
		String result = new String(data, "UTF-8");
		result = result.substring(result.indexOf(",")+1);
		result = result.substring(result.indexOf(",")+1);
		result = result.substring(result.indexOf(",")+1);
		result = result.substring(result.indexOf(",")+1);
		result = result.substring(result.indexOf(",")+1);
		return Long.parseLong(result.substring(0,13));
	}
	public static Session parseSession(DatagramPacket recPacket) throws UnsupportedEncodingException {
		String receivedMessage = new String(recPacket.getData(),"UTF-8");
		String session = receivedMessage.substring(receivedMessage.indexOf(",")+1);
		String sessionID = session.substring(0,session.indexOf(","));
		session = session.substring(session.indexOf(",")+1);
		long version =Long.parseLong(session.substring(0,session.indexOf(",")));
		session = session.substring(session.indexOf(",")+1);
		String message = session.substring(0,session.indexOf(","));
		session = session.substring(session.indexOf(",")+1);
		long expireTime = Long.parseLong(session.substring(0,13));
		return new Session(sessionID,version,message,expireTime);
	}
	public static void parseMembership(String reply) {
		String result = reply.substring(reply.indexOf(",")+1);
		while(result.contains(",")){
			Hello.membership.add(result.substring(0,result.indexOf(',')));
			result = result.substring(result.indexOf(',')+1);
		}
		Hello.membership.add(result);
	}	
}