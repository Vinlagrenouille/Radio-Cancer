import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class Serveur {

	public static void main(String[] args) {
	
		ServerSocket srvrSocket;
		Socket lstnSocket;
		BufferedReader in;
		PrintWriter out;

		try {
		
			srvrSocket = new ServerSocket(22);
			System.out.println("Le serveur est a l'ecoute du port "+srvrSocket.getLocalPort());
			lstnSocket = srvrSocket.accept(); 
			System.out.println("Le client "+lstnSocket.getInetAddress()+" s'est connecte !");
			out = new PrintWriter(lstnSocket.getOutputStream());
			out.println("Vous etes connecte "+lstnSocket.getInetAddress());
			out.flush();
			srvrSocket.close();
			lstnSocket.close();

		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
