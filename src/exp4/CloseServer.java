package exp4;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Map;
//执行这个正常关闭服务器
public class CloseServer {
	public static void main(String[] args) {
		String e="exit232";
		DatagramSocket ds=null;
		try {
			ds=new DatagramSocket();
		} catch (SocketException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		byte exit[]=e.getBytes();
		DatagramPacket dp=new DatagramPacket(exit, exit.length);
		try {
			ds.connect(InetAddress.getLocalHost(), 23334);
			ds.send(dp);
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

	}

}
