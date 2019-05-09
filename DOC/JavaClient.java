import java.net.*; 
import java.io.*; 

public class JavaClient { 
	final static int port = 9632; 

	public static void main(String[] args) { 
		Socket socket; 
		DataInputStream userInput; 
		PrintStream theOutputStream; 
		try {
			InetAddress monAdresse = InetAddress.getLocalHost();
			System.out.println(monAdresse.getHostName());
			System.out.println(monAdresse.getCanonicalHostName());
			System.out.println(monAdresse.getLocalHost());
			System.out.println(monAdresse.getHostAddress());
			System.out.println(monAdresse.getLoopbackAddress());
		} catch (UnknownHostException uhe) {
			System.out.println("Inconnu !");
		}
		try { 
			InetAddress serveur = InetAddress.getByName("192.168.1.4"); 
			socket = new Socket(serveur, port); 
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream())); 
			PrintStream out = new PrintStream(socket.getOutputStream()); 
			out.println("bonjour c'est le client " + args[0]); 
			System.out.println(in.readLine()); 
		} catch (Exception e) { 
			e.printStackTrace(); 
		} 
	} 
} 
