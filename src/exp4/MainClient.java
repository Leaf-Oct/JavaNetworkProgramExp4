package exp4;

import java.awt.Desktop;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.WindowConstants;

public class MainClient {

	public static void main(String[] args) {
		System.out.println("如果输入到一半突然停了，一定是非法输入(懒得设置非法处理机制)");
		Scanner in=new Scanner(System.in);
		System.out.println("输入数字表示请求方法\t1.GET 2.POST 3.HEAD");
		int operation=in.nextInt();
		String method=null;
		switch (operation) {
		case 1:		
			method="GET";
			break;
		case 2:
			method="POST";
			break;
		case 3:
			method="HEAD";
			break;
		default:
			System.exit(0);
		}
		System.out.println("输入目标ip，空格，和端口");
		String ip=in.next();
		int port=in.nextInt();
		try {
			InetAddress ia=InetAddress.getByName(ip);
		} catch (UnknownHostException e) {
			System.exit(0);
		}
		System.out.println("输入请求资源(需加斜杠，如请求index需要输入/index)");
		String res=in.next();
//		不是get，head方法却有query符号(问号)的，绝对有问题
		if(res.contains("?")&&method.equals("POST")) {
			System.exit(0);
		}
		if(!res.startsWith("/")) {
			System.exit(0);
		}
		System.out.println("输入接收的文件类型(可以输入*/*表示接收所有文件类型)");
		String accept=in.next();
		String post_body=null;
		if(method.equals("POST")) {
			System.out.println("检测到用的是post方法，可以在方法体里写点什么(反正服务端也不会处理，随便敲个空格就行了)");
			post_body=in.nextLine();
		}
//		记录cookie的文件，先预备着
		File cookieee=new File("./cookie");
//		不存在的话要先创建，哪怕是空的。不然Scanner会报空指针异常
		if(!cookieee.exists()) {
			try {
				cookieee.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
//		cookie的id
		String cookie=null;
		try {
//			从里面读(之前的)cookie出来，请求时发过去
			Scanner read_cookie=new Scanner(cookieee);
			if(read_cookie.hasNext()) {
				cookie=read_cookie.nextLine();
			}
			read_cookie.close();
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
//			根据前面输入的信息构造url
			URL u=new URL("http://"+ip+":"+port+res);
			HttpURLConnection huc=(HttpURLConnection) u.openConnection();
//			设置请求方式
			huc.setRequestMethod(method);
//			接收的文件类型和cookie
			huc.setRequestProperty("Accept", accept);
			huc.setRequestProperty("Cookie", "id="+cookie);
//			如果是post请求，还可以再写个请求体
			if(method.equals("POST")) {
				huc.setDoOutput(true);
				BufferedWriter bw=new BufferedWriter(new OutputStreamWriter(huc.getOutputStream()));
				bw.write(post_body);
				bw.flush();
			}
//			请求的资源文件如果带有路径的话，将前面的路径全部抹掉，只取文件名，保存在自己的根目录下
			String temp[]=res.split("/");
			String file_name=temp[temp.length-1];
			File download_file=new File("./"+file_name);
//			请求过了就不用再请求了
			if(download_file.exists()) {
				System.out.println("文件有啦");
				huc.disconnect();
				System.exit(0);
			}
//			删除原cookie文件，更新cookie
			if(cookieee.exists()) {
				cookieee.delete();
			}
			String set_cookie=huc.getHeaderField("Set-Cookie");
			Writer w=new FileWriter(cookieee);
			w.write(set_cookie.split("=")[1]);
			w.flush();
			w.close();
//			如果只是head方法，就没必要读响应内容，读响应头就行
			if(method.equals("HEAD")) {
				Map<String, List<String>> map=huc.getHeaderFields();
				System.out.println("响应头是");
				for(Map.Entry<String, List<String>> entry:map.entrySet()) {
					System.out.println(entry.getKey()+": "+entry.getValue());
				}
				
				System.exit(0);
			}
			if(huc.getResponseCode()!=200) {
				System.out.println(huc.getResponseCode()+"木大木大");
				System.exit(0);
			}
//			读的是文本类型的文件，用字符流保存
			if(res.endsWith(".txt")||res.endsWith(".html")||res.endsWith(".htm")) {
				BufferedReader br=new BufferedReader(new InputStreamReader(huc.getInputStream()));
				BufferedWriter bw=new BufferedWriter(new FileWriter(download_file));
				String line=null;
				while((line=br.readLine())!=null) {
					bw.write(line+'\n');
					bw.flush();
				}
				br.close();
				bw.close();
				huc.disconnect();
//				完了之后调用系统浏览器打开这个url
				openBrowser("http://"+ip+":"+port+res);
			}
			else {
//				不是文本类型的一律字节流
				BufferedInputStream bis=new BufferedInputStream(huc.getInputStream());
				byte b[]=new byte[2048];
				BufferedOutputStream bos=new BufferedOutputStream(new FileOutputStream(download_file));
				while(bis.read(b)!=-1) {
					bos.write(b);
				}
				bis.close();
				bos.close();
				huc.disconnect();
//				如果是图片，用swing打开
				if(file_name.endsWith(".png")||file_name.endsWith(".jpg")) {
					openImage(file_name);
				}
			}
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static void openBrowser(String url) {
//		检查系统的桌面环境。(命令行就别玩了，还不如字符画)
		if(Desktop.isDesktopSupported()) {
			try {
//				获得资源的url
				URI uri=URI.create(url);
//				获取桌面环境
				Desktop desk=Desktop.getDesktop();
//				可以打开浏览器的话，就在浏览器打开这个标签
				if(desk.isSupported(Desktop.Action.BROWSE)) {
					desk.browse(uri);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	public static void openImage(String f) {
//		用了个JFrame打开图片
		JFrame window=new JFrame("图像预览");
		Icon i=new ImageIcon(f);
		window.setSize(i.getIconWidth(), i.getIconWidth());
		JLabel l=new JLabel(i);
		window.add(l);
		window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		window.setVisible(true);
	}
}
