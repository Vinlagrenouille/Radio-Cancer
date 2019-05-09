import java.net.*;
 
class MyHostName {
	public static void main (String args[]) {
		try {
			InetAddress ia = InetAddress.getLocalHost();
			InetAddress[] myHost = InetAddress.getAllByName(ia.getHostName());
			for(InetAddress host:myHost) {
				NetworkInterface ney = NetworkInterface.getByInetAddress(host);
				byte [] mac = ney.getHardwareAddress();
				StringBuilder sb = new StringBuilder();
				for(int i = 0; i < mac.length; i++) {
					sb.append(String.format("%02X%s",mac[i], (i < mac.length -1) ? "-":""));
				}
				System.out.println(ia.getHostName() + "\t" + host.getHostAddress() + "\t" + sb.toString());
			}
		} catch (Exception e) {
		}
	}
}
