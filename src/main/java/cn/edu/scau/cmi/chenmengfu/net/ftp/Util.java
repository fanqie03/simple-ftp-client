package cn.edu.scau.cmi.chenmengfu.net.ftp;
/**
* @author mengfu1996@hotmail.com
* @version create time：2018年6月9日 下午1:05:18
*/
public class Util {
	/** 
	 * "227 Entering Passive Mode (172,26,14,30,118,214)."-->30422
	 * @param orders
	 * @return
	 */
	public static int parsePort(String orders) {
		String inet = orders.substring(orders.indexOf('(')+1, orders.indexOf(')'));
		String[] split = inet.split(",");
		int preffix=Integer.parseInt(split[4]);
		int suffix=Integer.parseInt(split[5]);
		int port=preffix*256+suffix;
		return port;
	}
}
