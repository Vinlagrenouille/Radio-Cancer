package serveur;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

public class RadioServer {
	private ArrayList<String> globalListSong = new ArrayList<String>();
	private ArrayList<Radio> listRadio = new ArrayList<>();
	private ArrayList<ClientHandler> listeners = new ArrayList<>();

	//The constructor gets the radios and the songs, to list them 
	public RadioServer() {
		System.out.println("Working Directory = " +
				System.getProperty("user.dir"));
		File folder = new File(System.getProperty("user.dir")+"\\Musique");
		File[] listOfFiles = folder.listFiles();
		System.out.println(listOfFiles.length);

		for (int i = 0; i < listOfFiles.length; i++) {
			if(listOfFiles[i].isDirectory()) {
				File listSongF = new File(System.getProperty("user.dir")+"\\Musique\\"+listOfFiles[i].getName());
				File[] listSongRadio = listSongF.listFiles();
				ArrayList<String> list = new ArrayList<>();

				for(int j = 0; j < listSongRadio.length; j++) {

					list.add(listSongRadio[j].getName());
					globalListSong.add(listSongRadio[j].toString());
					System.out.println(listSongRadio[j].toString());
				}
				Radio r = new Radio(listOfFiles[i].getName(), list);
				listRadio.add(r);
				Thread t = new Thread(r);
				t.start();
			}
		}
		go();
	}

	private void go() {
		System.out.println("Server online");
		boolean flag = true;
		try {
			ServerSocket serverSock = new ServerSocket(5001);

			while (flag) {
				Socket fileSocket = serverSock.accept();
				ClientHandler c = new ClientHandler(fileSocket);
				Thread t = new Thread(c);
				t.start();
				listeners.add(c);
			}
		}
		catch (Exception ef) {
			ef.printStackTrace();
		}
	}

	private class ClientHandler implements Runnable {
		Listener l;
		BufferedReader reader;
		Socket sock;
		public ClientHandler(Socket clientSocket) throws IOException {
			sock = clientSocket;
			InputStreamReader isReader = new InputStreamReader(sock.getInputStream());
			reader = new BufferedReader(isReader);
			
			l = new Listener(clientSocket);
			
			for (Radio x : listRadio) {
				l.sendUser(x.getName());
			}
			l.sendUser("INFO");
			l.sendUser("INFO Please choose a radio by typing: 'CHOOSE <name_of_the_radio>'");
			l.sendUser("INFO To create a new Radio: 'NEW <name_of_your_radio> list_of_songs'");
			l.sendUser("INFO To stop : 'STOP'");
			l.sendUser("INFO To list radios again: 'LIST'");
			l.sendUser("INFO To get help: 'HELP'");
			l.sendUser("CHOOSE");
		}

		public void run() {
			String message = null;
			boolean flag = true;
			try {
				while(flag) {
					System.out.println("Thinking..");
					if((message = reader.readLine()) != null) {
						if(message.startsWith("NEW ")) {
							message = message.replaceFirst("NEW ", "");
							Radio r = new Radio(message, new ArrayList<>());
							//TODO get list of musics
							listRadio.add(r);
							for(Radio tmp : listRadio) {
								tmp.removeListener(l);
							}
							r.addListener(l);
						}
						else if (message.equals("LIST")) {
							for (Radio x : listRadio) {
								l.sendUser(x.toString());
							}
							l.sendUser("STOP");
						}
						else if (message.startsWith("CHOOSE ")) {
							message = message.replaceFirst("CHOOSE ", "");
							System.out.println("Choosed "+message);
							for(Radio r : listRadio) {
								System.out.println(r.getName());
								if(message.startsWith(r.getName())) {
									r.addListener(l);
									r.run();
								}
							}
						}
						else if (message.equals("HELP")) {
							l.sendUser("Please choose a radio by typing: 'CHOOSE <name_of_the_radio>'");
							l.sendUser("To create a new Radio: 'NEW <name_of_your_radio> list_of_songs'");
							l.sendUser("To stop : 'STOP'");
							l.sendUser("To list radios again: 'LIST'");
							l.sendUser("To get help: 'HELP'");
						}
						else if (message.equals("STOP")) {
							sock.close();
						}
					}
				}
			}
			catch (Exception e) {e.printStackTrace();}
		}
	}
	public static void main(String[] args) {
		new RadioServer();
	}
}