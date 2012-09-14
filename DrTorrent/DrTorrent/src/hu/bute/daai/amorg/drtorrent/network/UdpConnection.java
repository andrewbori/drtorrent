package hu.bute.daai.amorg.drtorrent.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import android.util.Log;

public class UdpConnection {
	private final static String LOG_TAG = "UdpConnection";
	private String host_;
	private int port_;
	private byte[] message_;
	private int lengthOfResponse_;
	
	private String errorMessage_ = null;
	
	/** Constructor with the host that has to be connecting to. */
	public UdpConnection(String host, int port, byte[] message, int lengthOfResponse) {
		host_ = host;
		port_ = port;
		message_ = message;
		lengthOfResponse_ = lengthOfResponse;
	}
	
	/** Sends the request and returns the response as a byte array. */
	public byte[] execute() {
		DatagramSocket socket = null;
		DatagramPacket packet = null;
		
		try {
			socket = new DatagramSocket();
			socket.setSoTimeout(30000);		// 30 sec
			InetAddress address = InetAddress.getByName(host_);
			
			packet = new DatagramPacket(message_, message_.length, address, port_);
			Log.v(LOG_TAG, "Sending request to: udp://" + host_);
			socket.send(packet);
			
			// For Received message
			packet = new DatagramPacket(new byte[lengthOfResponse_], lengthOfResponse_);
			Log.v(LOG_TAG, "Reading respone from: udp://" + host_);
			socket.receive(packet);				

            return packet.getData();
		} catch (IOException e) {
			errorMessage_ = e.getMessage();
			Log.v(LOG_TAG, "Exception: " + errorMessage_);
			return null;
		} finally {
			try {
				if (socket != null) socket.close();
			} catch (Exception e) {}
		}
	}
	
	/** Returns the error message. If the connection was successful it equals NULL. */
	public String getMessage() {
		return errorMessage_;
	}	
}
