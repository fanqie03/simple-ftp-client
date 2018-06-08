package cn.edu.scau.cmi.chenmengfu.net.ftp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
/**
 * 
 * @author mfchen1996@hotmail.com
 * 能完成FTP链接的打开和关闭操作；
 * 能执行文件的上传和下载；
 * 能完成目录的创建、删除等有关操作。
 * 要求可以传输简单的文本文件。
 */
public class FTPClient {
	//数据连接的连接
	private static ServerSocket serverSocket;
	//控制程序的连接
	private static Socket socket;
	//数据连接的端口
	private static Socket subSocket;
	//数据连接的输入流
	private static BufferedReader subReader;
	//数据连接的输出流
	private static PrintWriter subWriter;
	//控制程序的输入流
	private static BufferedReader reader;
	//控制程序的输出流
	private static PrintWriter writer;
	//服务器地址
	private static String host="192.168.79.130";
//	private static String host="172.26.14.30";
	//用21端口与服务器相连
	private static int port=21;
	//指定的上传路径
	private static String myPath = System.getProperty("user.home");
	//
//	private static String serverPath;
	//处理数据传输的端口
	private static int subPort;
	//本地连接的ip信息
	private static InetAddress localAddress;
	
	//缓存
	private static String buf="I am so looking forward to my college life. I have pictured it thousands of times in my mind. The free lifestyle and lively parties are always appearing my mind. The great expectation of campus life endows me a lot of passion to keep study. After two years’ study, I would find my lifestyle.\r\n" + 
			"The first thing for me is to learn more knowledge. Most students find a way to release themselves and skip or sleep in the class. They think study is just the task, so they give up improving themselves and take part in all kinds of activities, which bring them great joy. The wrong conception about college stage makes most students miss the best time to equip themselves with skills. I realize my job and dare not to loose study.\r\n" + 
			"The second thing for me to learn is to make friends by joining activities. I am a shy girl, but inside my heart, I am so eager to make more friends, so I choose to join many activities in the spared time. I learn dancing and playing tennis. The lucky thing is that I know many nice people and we become good friends, who make my college colorful. ";

	
	public static void main(String[] args) throws Exception {
		Scanner scanner = new Scanner(System.in);
		System.out.println("create server socket");
		subPort=initSubSocket();
		System.out.println("create server success");
		System.out.print("host:");
		 host = scanner.nextLine();
//		System.out.print("port:");
		// port = scanner.nextInt();
		socket = new Socket(host, port);
		
		localAddress=socket.getLocalAddress();
		System.out.println(localAddress.getHostAddress());

		reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		System.out.println("YES");
		writer = new PrintWriter(socket.getOutputStream());
		System.out.println(reader.readLine());
		
//		sendCmd("user upload");
//		getReply();
//		sendCmd("pass upload");
//		getReply();
//		stor("stor test.txt");
		

		while (true) {
			String orders = scanner.nextLine();
			String[] order = orders.split(" ");
			String arg = order[0].toUpperCase();
			if ("QUIT".equals(arg)) {
				sendCmd(orders);
				getReply();
				break;
			} else if ("RETR".equalsIgnoreCase(arg)) {
				retr(orders);
				continue;
			}else if("STOR".equalsIgnoreCase(arg)) {
				stor(orders,order[1]);
				continue;
			}
			sendCmd(orders);
			getReply();
		}
		scanner.close();
	}
	/**
	 * 准备port指令，用来指定客户端的端口，表示“我打开了xxx端口，你用这个端口传数据”
	 * @param localAddress
	 * @param port
	 * @return
	 */
	public static String preparePort(InetAddress localAddress,int port) {
		StringBuilder builder = new StringBuilder();
		String prePort=String.valueOf(port/256);
		String rearPort=String.valueOf(port%256);
		String ip = localAddress.getHostAddress().replaceAll("\\.", ",");
		builder.append("PORT ");
		builder.append(ip);
		builder.append(",");
		builder.append(prePort);
		builder.append(",");
		builder.append(rearPort);
		System.out.println(builder.toString());
		return builder.toString();
	}
	/**
	 * retr指令，表示下载，这里没做失败时的处理
	 * @param orders
	 */
	private static void retr(String orders) {
		sendCmd(preparePort(localAddress,subPort));
		sendCmd(orders);
		receiveData();
		getReply();
	}
	/**
	 * 读取文件，转成字符串
	 * @param fileName
	 * @return
	 */
	private static String prepareFile(String fileName) {
		File file = new File(myPath+File.separator+fileName);
		System.out.println(file);
		StringBuilder builder = new StringBuilder();
		char[] bu=new char[1024];
		int len=0;
		try {
			FileReader reader =new FileReader(file);
			while((len=reader.read(bu))!=-1) {
				builder.append(new String(bu, 0, len));
			}
			reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return builder.toString();
	}
	/**
	 * stor指令，表示上传，这里没做失败时的处理，只支持在user.home目录下的文件上传
	 * @param orders
	 * @param fileName
	 */
	private static void stor(String orders,String fileName) {
		buf=prepareFile(fileName);
		sendCmd(preparePort(localAddress,subPort));
		sendCmd(orders);
		getReply();
		
		sendData();
		getReply();
	}
	/**
	 * 发送数据
	 */
	public static void sendData() {
		new Thread(()->{
			try {
				acceptConnect();
				send(subWriter, buf);
				subSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}).start();
	}
	/**
	 * 接收数据
	 */
	public static void receiveData() {
		new Thread(() -> {
			try {
				acceptConnect();
				while ((buf = subReader.readLine()) != null) {
					System.out.println(buf);
				}
				subSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}).start();
	}
	/**
	 * 开启子连接，准备接收或发送消息
	 */
	private static void acceptConnect() {
		try {
//			System.out.println("prepare connect");
			subSocket = serverSocket.accept();
//			System.out.println("connect success");
			subReader = new BufferedReader(new InputStreamReader(subSocket.getInputStream()));
			subWriter = new PrintWriter(subSocket.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/**
	 * 初始化子程序，，并返回开启的端口值
	 */
	public static int initSubSocket() {
		int port=-1;
		try {
			serverSocket = new ServerSocket(0);
			port=serverSocket.getLocalPort();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return port;
	}
	/**
	 * 获得服务端的回复
	 */
	private static void getReply() {
		try {
			System.out.println(reader.readLine());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/**
	 * 发送指令，指令添加换行符
	 * @param orders 指令
	 */
	private static void sendCmd(String orders) {
		orders+="\r\n";
		send(writer, orders);
	}
	/**
	 * 发送消息，并刷新缓存
	 * @param writer 在writer这里发送
	 * @param orders 消息
	 */
	public static void send(PrintWriter writer, String orders) {
		writer.print(orders);
		writer.flush();
	}
}
