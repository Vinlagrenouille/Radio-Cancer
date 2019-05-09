import java.net.*; 
import java.io.*; 

public class JavaServer extends Thread { 
	final static int port = 9632; 
	private Socket socket; 
	public static void main(String[] args) { 
		try { 
			ServerSocket socketServeur = new ServerSocket(port); 
			System.out.println("Lancement du serveur"); 
			while (true) { 
				Socket socketClient = socketServeur.accept(); 
				JavaServer t = new JavaServer(socketClient); 
				t.start(); 
			} 
		} catch (Exception e) { 
				e.printStackTrace(); 
		} 
	} 

	public JavaServer(Socket socket) { 
		this.socket = socket; 
	} 
	public void run() { 
		traitements(); 
	} 
	public void traitements() { 
		try { 
			String message = ""; 
			System.out.println("Connexion avec le client : " + socket.getInetAddress()); 
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream())); 
			PrintStream out = new PrintStream(socket.getOutputStream()); 
			message = in.readLine(); 
			System.out.println(message); 
			out.println("bonjour c'est le serveur"); 
			socket.close(); 
		} catch (Exception e) { 
			e.printStackTrace(); 
		} 
	} 
} 
