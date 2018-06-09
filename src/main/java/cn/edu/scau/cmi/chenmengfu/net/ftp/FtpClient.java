package cn.edu.scau.cmi.chenmengfu.net.ftp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 
 * @author mfchen1996@hotmail.com
 *
 */
public class FtpClient {
	private static final Logger logger = LoggerFactory.getLogger(FtpClient.class);

	private static final String PASV = "PASV";

	/**
	 * 控制程序的连接
	 */
	private static Socket socket;

	/**
	 * 控制程序的输入流
	 */
	private static BufferedReader reader;
	/**
	 * 控制程序的输出流
	 */
	private static PrintWriter writer;
	/**
	 * 服务器地址
	 */
	private static String host = "192.168.79.130";
	/**
	 * 用21端口与服务器相连
	 */
	private static int port = 21;
	/**
	 * 指定的上传路径
	 */
	private static String myPath = System.getProperty("user.home");

	/**
	 * 本地连接的ip信息
	 */
	private static InetAddress localAddress;

	/**
	 * 缓存
	 */
	private static String buf = "";

	public static void main(String[] args) throws Exception {
		Scanner scanner = new Scanner(System.in);

		System.out.print("host:");
		host = scanner.nextLine();
		socket = new Socket(host, port);

		localAddress = socket.getLocalAddress();
		logger.info("my ip addr : {}", localAddress.getHostAddress());

		reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		writer = new PrintWriter(socket.getOutputStream());
		getReply();

		while (true) {
			String orders = scanner.nextLine();
			String[] order = orders.split(" ");
			String arg = order[0].toUpperCase();
			if ("QUIT".equalsIgnoreCase(arg)) {
				sendCmd(orders);
				getReply();
				break;
			} else if ("RETR".equalsIgnoreCase(arg)) {
				retrWithPasv(orders);
				continue;
			} else if ("STOR".equalsIgnoreCase(arg)) {
				// stor(orders,order[1]);
				storWithPasv(orders, order[1]);
				continue;
			}
			sendCmd(orders);
			getReply();
		}
		scanner.close();
	}

	/**
	 * 准备port指令，用来指定客户端的端口，表示“我打开了xxx端口，你用这个端口传数据”
	 * 
	 * @param localAddress
	 * @param port
	 * @return
	 */
	public static String preparePort(InetAddress localAddress, int port) {
		StringBuilder builder = new StringBuilder();
		String prePort = String.valueOf(port / 256);
		String rearPort = String.valueOf(port % 256);
		String ip = localAddress.getHostAddress().replaceAll("\\.", ",");
		builder.append("PORT ");
		builder.append(ip);
		builder.append(",");
		builder.append(prePort);
		builder.append(",");
		builder.append(rearPort);
		return builder.toString();
	}

	/**
	 * PASV模式接收文件
	 * 
	 * @param orders
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	private static void retrWithPasv(String orders) throws UnknownHostException, IOException {
		sendCmd(PASV);
		String reply = getReply();
		int port = Util.parsePort(reply);
		Socket socket = new Socket(host, port);
		sendCmd(orders);
		StringBuilder builder = new StringBuilder();
		String temp = null;
		BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		while ((temp = reader.readLine()) != null) {
			builder.append(temp + "\r\n");
		}
		buf = builder.toString();
		logger.debug(buf);
		saveFile(orders);
		socket.close();
	}

	/**
	 * 保存数据到文件，保存到user.home目录下
	 * 
	 * @param orders
	 *            包含文件名的指令
	 * @throws IOException
	 */
	private static void saveFile(String orders) throws IOException {
		String[] o = orders.split(" ");
		File file = new File(myPath + File.separator + o[1]);
		if (!file.exists()) {
			file.createNewFile();
		}
		PrintWriter pWriter = new PrintWriter(file);
		pWriter.print(buf);
		pWriter.flush();
		pWriter.close();
	}

	/**
	 * 读取文件，转成字符串
	 * 
	 * @param fileName
	 * @return
	 * @throws FileNotFoundException
	 */
	private static String prepareFile(String fileName) throws FileNotFoundException {
		File file = new File(myPath + File.separator + fileName);
		logger.debug(file.toString());
		if (!file.exists()) {
			throw new FileNotFoundException(fileName.toString());
		}
		StringBuilder builder = new StringBuilder();
		char[] bu = new char[1024];
		int len = 0;
		try {
			FileReader reader = new FileReader(file);
			while ((len = reader.read(bu)) != -1) {
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

	private static void storWithPasv(String orders, String fileName) throws UnknownHostException, IOException {
		buf = prepareFile(fileName);
		sendCmd(PASV);
		String reply = getReply();
		int port = Util.parsePort(reply);
		sendCmd(orders);
		Socket socket = new Socket(host, port);
		getReply();
		PrintWriter writer = new PrintWriter(socket.getOutputStream());
		writer.print(buf);
		writer.flush();
		writer.close();
		socket.close();
		getReply();
	}

	/**
	 * 获得服务端的回复
	 */
	private static String getReply() {
		return getReply(reader);
	}

	private static String getReply(BufferedReader bufferedReader) {
		try {
			String reply = bufferedReader.readLine();
			logger.debug("<< {} : {}", Thread.currentThread().getName(), reply);
			System.out.println(reply);
			return reply;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 发送指令
	 * 
	 * @param orders
	 *            指令
	 */
	private static void sendCmd(String orders) {
		send(writer, orders);
	}

	/**
	 * 发送消息，并刷新缓存
	 * 
	 * @param writer
	 *            在writer这里发送
	 * @param orders
	 *            消息
	 */
	public static void send(PrintWriter writer, String orders) {
		logger.debug(">> {} : {}", Thread.currentThread().getName(), orders);
		writer.println(orders);
		writer.flush();
	}
}
