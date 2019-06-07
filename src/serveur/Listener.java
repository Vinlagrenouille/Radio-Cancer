package serveur;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class Listener {
	
	private Socket s;
	private PrintWriter pw;
	
	public Listener(Socket s) throws IOException {
		this.s = s;
		pw = new PrintWriter(s.getOutputStream());
	}

	public void sendUser(String message) {
		System.out.println("Sent: "+message);
		pw.println(message);
		pw.flush();
	}
	
	public Socket getS() {
		return s;
	}
	
}
