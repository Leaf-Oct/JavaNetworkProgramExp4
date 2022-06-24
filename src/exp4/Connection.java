package exp4;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
public class Connection implements Runnable {
	private int index;
	private Socket s;
	private BufferedWriter write_text;
	private BufferedReader read_text;
	private OutputStream out;
	private InputStream in;
	private BufferedOutputStream write_byte;
	private Logger log;
	public Connection(int index, Socket s) {
		super();
		this.index = index;
		this.s = s;
		try {
			out=s.getOutputStream();
			in=s.getInputStream();
			write_text=new BufferedWriter(new OutputStreamWriter(out));
			write_byte=new BufferedOutputStream(out);
			read_text=new BufferedReader(new InputStreamReader(in));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log=LogManager.getLogger(Connection.class);
	}
	
	@Override
	public void run() {
		log.info("编号为"+index+"的请求开始工作");
//		开始来先读请求头
		String line=null;
		String method=null, res=null, protocal=null;
		try {
//			首行
			line = read_text.readLine();
			if(line==null) {
				log.error("读到一个空请求头。原因不明");
				s.close();
				return;
			}
			String firstline[]=line.split(" ");
//			先读请求方式，资源和协议版本
			method=firstline[0];
			res="./web"+firstline[1];
			protocal=firstline[2];
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		log.info("编号"+index+"连接，方法"+method);
//		只关注个别几个参数，连接状态，接受的语言，接受的文件类型，曲曲曲曲奇(误)
		String accept=null,cookie=null;
//		读请求头
		try {
			while(!(line=read_text.readLine()).equals("")) {
				String key_value_pair[]=line.split(":");
				String key=key_value_pair[0];
				String value=key_value_pair[1];
				switch (key) {
				case "Accept":
					accept=value.trim();
					log.info("支持的文件类型"+accept);
					break;
				case "Cookie":
					cookie=value.trim();
					cookie=cookie.split("=")[1];
					log.info("cookie"+cookie);
					break;
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		query后面其实用不上，但还是读取了
		String query=null;
		//读完请求头，开始分析需求并响应内容
		switch (method) {
		case "HEAD":
		case "GET":
			if(res.indexOf("?")!=-1) {
				//有额外查询符的话，要分开。该程序不考虑query
				String res_and_query[]=res.split("\\?");
				res=res_and_query[0];
				query=res_and_query[1];
			}
			break;
		case "POST":
			try {
				while((line=read_text.readLine())!="") {
					query+=line;
					System.out.println(line);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
		}
//		要先将url的文件名解码一下，不然读取文件名含汉字的文件时会404
		res=URLDecoder.decode(res);
		log.info("请求的资源是"+res);
//		要响应的文件
		File f=new File(res);
//		不存在，或者是文件夹的话，那就好办了，直接返回404
		if(!f.exists()||f.isDirectory()) {
			log.warn("404!");
			try {
				if(cookie==null) {
					cookie=String.valueOf(s.hashCode());
					ArrayList<String> res_recorded=new ArrayList<String>();
					res_recorded.add(res);
					WebServer.cookies.put(cookie, res_recorded);
				}
				write_text.write(protocal+" 404\r\n");
				write_text.write("Content-Type: text/html;charset=utf-8\r\n"
						+ "Content-Length: "+"你要找的资源不见辣～".getBytes().length+"\r\n"
						+ "Server: LeafServer\r\n"
						+ "Set-Cookie: id="+cookie+"\r\n"
						+ "Date: "+new Date().toGMTString()+"\r\n\r\n");
				write_text.write("你要找的资源不见辣～");
				write_text.flush();
				write_text.close();
				write_byte.close();
				read_text.close();
				s.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return;
		}
//		如果有发来的cookie，代表之前连接过，查查看之前有没有请求过相同的资源
		if(cookie!=null&&WebServer.cookies.containsKey(cookie)) {
			ArrayList<String> res_recorded=WebServer.cookies.get(cookie);
//			如果请求过，直接返回403
			if(res_recorded.contains("res")) {
				log.warn("403");
				try {
					write_text.write(protocal+" 403\r\n");
					write_text.write("Content-Type: text/html;charset=utf-8\r\n"
							+ "Content-Length: "+"你不是已经请求过了嘛～".getBytes().length+"\r\n"
							+ "Server: LeafServer\r\n"
							+ "Date: "+new Date().toGMTString()+"\r\n\r\n");
					write_text.write("你不是已经请求过了嘛～");
					write_text.flush();
					write_text.close();
					write_byte.close();
					read_text.close();
					s.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return;
			}
			else {
				res_recorded.add(res);
			}
		}
		else {
			//没有cookie表示是新的连接，记录一下
			cookie=String.valueOf(s.hashCode());
			ArrayList<String> res_recorded=new ArrayList<String>();
			res_recorded.add(res);
			WebServer.cookies.put(cookie, res_recorded);
		}
		log.info("200");
//		发响应头。200表示成功！
		try {
			write_text.write(protocal+" 200\r\n"
					+ "Server: LeafServer\r\n"
					+ "Date: "+new Date().toGMTString()+"\r\n"
					+ "Set-Cookie: id="+cookie+"\r\n");
			
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
//		开始发文件了。
//		这里的if可能可以优化
//		如果请求的资源是三种文本文件，接受的文件类型包含文本，就发
		if((res.endsWith("txt")||res.endsWith("html")||res.endsWith("htm"))&&(accept.contains("text")||accept.contains("*/*"))) {
//			读文本文件
			BufferedReader br=null;
			try {
//				先发响应头剩下的部分
				write_text.write("Content-Type: text/html;charset=utf-8\r\n"
						+ "Content-Length: "+f.length()+"\r\n\r\n");
				write_text.flush();
//				如果是head方法，发完请求头就可以了
				if(method.equals("HEAD")) {
					write_byte.close();
					write_text.close();
					read_text.close();
					s.close();
				}
				br=new BufferedReader(new FileReader(f));
				while((line=br.readLine())!=null){
					write_text.write(line+'\n');
					write_text.flush();
				}
				br.close();
				write_text.close();
				write_byte.close();
				s.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
			
		}//否则就发字节文件
		else {
			try {
				BufferedInputStream bis=new BufferedInputStream(new FileInputStream(f));
				byte[] b=new byte[2048];
				write_text.write("Content-Length: "+f.length()+"\r\n\r\n");
				write_text.flush();
//				如果是head方法，发完请求头就可以了
				if(method.equals("HEAD")) {
					write_byte.close();
					write_text.close();
					read_text.close();
					s.close();
				}
				while(bis.read(b)!=-1) {
					write_byte.write(b);
					write_byte.flush();
				}
				write_byte.close();
				write_text.close();
				bis.close();
				s.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

}
