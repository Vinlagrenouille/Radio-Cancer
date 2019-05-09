import java.io.*;
import java.net.*;

public class ClientChat {
	public static Socket socket = null;
	public static Thread t1;

	public static void main(String[] args) {
		try {
			System.out.println("Demande de connexion");
			socket = new Socket("127.0.0.1",2009);
			System.out.println("Connexion etablie avec le serveur, authentification :");
// Si le message s'affiche c'est que je suis connecté
			t1 = new Thread(new Connexion(socket));
			t1.start();
		} catch (UnknownHostException e) {
			System.err.println("Impossible de se connecter a 'adresse "+socket.getLocalAddress());
		} catch (IOException e) {
			System.err.println("Aucun serveur a l'écoute du port "+socket.getLocalPort());
		}
	}
}
