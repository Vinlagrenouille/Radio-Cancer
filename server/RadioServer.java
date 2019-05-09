import java.io.*;
import java.net.*;
import java.util.*;
import javax.net.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.text.*;
import javax.sound.sampled.*;
import java.util.jar.*;
import javax.net.ssl.*;

public class RadioServer {
	private ArrayList<String> globalListSong = new ArrayList<String>();
	private ArrayList<Radio> listRadio = new ArrayList<>();
	private ArrayList<PrintWriter> userList;

	private class ClientHandler implements Runnable {
		BufferedReader reader;
		SSLSocket sock;
		public ClientHandler(SSLSocket clientSocket, PrintWriter writer) {
			try {
				userList = new ArrayList<PrintWriter>();
				userList.add(writer);
				sock = clientSocket;
				InputStreamReader isReader = new InputStreamReader(sock.getInputStream());
				reader = new BufferedReader(isReader);
			}
			catch(Exception e) {e.printStackTrace();}

		}
		public void run() {
			String message = null;
			try {
				while((message = reader.readLine()) != null) {
					if (message.equals("LIST")) {
						System.out.println("Sending...");
						System.out.println("listSong.size() = " + listRadio.size());
						for (Radio x : listRadio) {
							System.out.println("Sent : " + x);
							sendUser(x, userList);
						}
						System.out.println("Stopped.");
						sendUser("STOP", userList);
					}
					else if (message.startsWith("CHOOSE ")) {
						message = message.replaceFirst("CHOOSE ", "");

						File file = new File(message);
						InputStream fileStream = new FileInputStream(file);
						System.out.println("CHOOSE : " + message);
						long length = file.length();
						if (length > Integer.MAX_VALUE) {
							System.out.println("TOO LARGE - ERROR");
						}
						byte[] bytes = new byte[(int)length];
						int offset = 0;
						int numRead = 0;
						while (offset < bytes.length && (numRead=fileStream.read(bytes, offset, bytes.length-offset)) >= 0) {
							offset += numRead;
						}
						if (offset < bytes.length) {
							throw new IOException("FAILED READING "+ file.getName());
						}
						fileStream.close();
						String value = new String(bytes);
						sendUser("STREAM", userList);
						sendUser(value, userList);
						sendUser("DONE", userList);
					}
				}
			}
			catch (Exception e) {e.printStackTrace();}
		}
	}
	public static void main(String args[]) {
		new FileHostingServer().go();
	}

	private void go() {
		try {
			//TODO
			File songList = new File("SongList.txt");
			String song = null;
			FileReader songReader = new FileReader(songList);
			BufferedReader songStream = new BufferedReader(songReader);
			while ((song = songStream.readLine()) != null) {
				System.out.println(song);
				listSong.add(song);
			}
			//TODO
		}
		catch (Exception uin) {uin.printStackTrace();}

		try {
			SSLServerSocketFactory ssocketFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
			SSLServerSocket serverSock = (SSLServerSocket) ssocketFactory.createServerSocket(5001);
			final String[] enabledCipherSuites = { "SSL_DH_anon_WITH_RC4_128_MD5" };
			serverSock.setEnabledCipherSuites(enabledCipherSuites);
			while (true) {
				SSLSocket fileSocket = (SSLSocket) serverSock.accept();
				PrintWriter writer = new PrintWriter(fileSocket.getOutputStream());
				Thread t = new Thread(new ClientHandler(fileSocket, writer));
				t.start();
			}
		}
		catch (Exception ef) {
			ef.printStackTrace();
		}
	}

	private void sendUser(String message, ArrayList array) {
		Iterator it = array.iterator();
		try {
			PrintWriter writer = (PrintWriter) it.next();
			writer.println(message);
			writer.flush();
		}
		catch (Exception exaf) {
			exaf.printStackTrace();
		}
	}

}