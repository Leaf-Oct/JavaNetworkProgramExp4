package exp4;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.Socket;
import java.net.SocketException;

public class WebServer {
	private ServerSocket ss;
//	每个连接的socket有不同的编号
	private Map<Integer,Socket> sockets;
//	记录日志的
	private Logger log;
//	线程池
	private ExecutorService pool;
//	记录cookie信息，哪个幺儿请求过什么资源。请求过的再请求就不给了。
	public static Map<String, ArrayList<String>> cookies;
//	默认使用23333端口
	public WebServer() {
		this(23333);
	}
	public WebServer(int port) {
		try {
			ss=new ServerSocket(port);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		sockets=new HashMap<Integer, Socket>();
		log=LogManager.getLogger(WebServer.class);
//		最多100个线程
		pool=Executors.newFixedThreadPool(100);
		cookies=new HashMap<String, ArrayList<String>>();
	}
	public void init() {
		//监听23334号端口的UDP信息，销毁已创建的连接
		new Thread() {
			@Override
			public void run() {
//				给这个线程命个名，方便日志查阅
				this.setName("ReceiveStopCommandThread");
				DatagramSocket ds=null;
				try {
					ds=new DatagramSocket(23334);
				} catch (SocketException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				byte b[]=new byte[10];
				DatagramPacket dp=new DatagramPacket(b, b.length);
				while(true) {
					try {
						ds.receive(dp);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					String command=new String(dp.getData(),0,dp.getLength());
//					接收到结束指令exit
					if(command.equals("exit")) {
						try {
							ss.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						sockets.clear();
						return;
					}
					else {
//						不然的话接收到一个数字,把那个数字编号对应的socket除掉
						try {
							int num = Integer.valueOf(command);
							sockets.remove(num);
//							接收到的东西不是数字，而是意料之外的东西
						} catch (NumberFormatException e) {
							log.fatal("23334号端口接收到不明指令！");
							log.fatal("有内鬼！终止交易！");
							try {
								ss.close();
							} catch (IOException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
							sockets.clear();
							return;
						}
					}
				}
			}
		}.start();
		
		Integer order=0;
		while(true) {
			Socket s=null;
			try {
				s=ss.accept();
				sockets.put(order, s);
				order++;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				log.warn("接收到退出指令，任务结束,over");
				return;
			}
			log.info(s.getInetAddress().getHostAddress()+" 加入连接, 编号为 "+order);
			pool.execute(new Connection(order, s));
			
		}
	}
}