package serveur;

import java.io.IOException;
import java.io.PrintWriter;

import javax.net.ssl.SSLSocket;

public class Listener {
	
	private SSLSocket s;
	private PrintWriter pw;
	
	public Listener(SSLSocket s) throws IOException {
		this.s = s;
		pw = new PrintWriter(s.getOutputStream());
	}

	public void sendUser(String message) {
		System.out.println("Sent");
		pw.println(message);
		pw.flush();
	}
	
	public SSLSocket getS() {
		return s;
	}
	
}
