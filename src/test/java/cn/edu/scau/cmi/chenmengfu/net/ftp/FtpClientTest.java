package cn.edu.scau.cmi.chenmengfu.net.ftp;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.junit.Test;

/**
 * @author mengfu1996@hotmail.com
 * @version create time：2018年6月9日 下午12:20:42
 */

public class FtpClientTest {
	@Test 
	public void testServerSocket() throws IOException {
		ServerSocket serverSocket = new ServerSocket(0);
		System.out.println(serverSocket);
	}

	@Test
	public void testSocket() {
		Socket socket = new Socket();
		System.out.println(socket);
	}

	@Test
	public void testPasv() {
		String reply = "227 Entering Passive Mode (172,26,14,30,118,214).";
		int port = Util.parsePort(reply);
		assertEquals(30422, port);

	}
	@Test
	public void testFlow() {
		
	}
}
