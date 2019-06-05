package serveur;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.*;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

public class RadioServer {
	private ArrayList<String> globalListSong = new ArrayList<String>();
	private ArrayList<Radio> listRadio = new ArrayList<>();
	private ArrayList<ClientHandler> listeners;

	public RadioServer() {
		File folder = new File("your/path");
		File[] listOfFiles = folder.listFiles();
		
		for (int i = 0; i < listOfFiles.length; i++) {
			if(listOfFiles[i].isDirectory()) {
				File listSongF = new File("your/path/"+listOfFiles[i].getName());
				File[] listSongRadio = listSongF.listFiles();
				ArrayList<String> list = new ArrayList<>();
				for(int j = 0; j < listOfFiles[i].length(); j++) {
					list.add(listSongRadio[j].getName());
					globalListSong.add(listOfFiles[i].getName());
				}
				Radio r = new Radio(listOfFiles[i].getName(), list);
				listRadio.add(r);
			}
		}
		go();
	}

	private void go() {
		boolean flag = true;
		try {
			SSLServerSocketFactory ssocketFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
			SSLServerSocket serverSock = (SSLServerSocket) ssocketFactory.createServerSocket(5001);
			final String[] enabledCipherSuites = { "SSL_DH_anon_WITH_RC4_128_MD5" };
			serverSock.setEnabledCipherSuites(enabledCipherSuites);

			while (flag) {
				SSLSocket fileSocket = (SSLSocket) serverSock.accept();
				ClientHandler c = new ClientHandler(fileSocket);
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
		SSLSocket sock;
		public ClientHandler(SSLSocket clientSocket) {
			sock = clientSocket;
			try {
				InputStreamReader isReader = new InputStreamReader(sock.getInputStream());
				reader = new BufferedReader(isReader);
				l = new Listener(sock);
			}
			catch(Exception e) {e.printStackTrace();}
			for (Radio x : listRadio) {
				l.sendUser(x.toString());
			}
			l.sendUser("STOP");
			l.sendUser("Please choose a radio by typing: 'CHOOSE <name_of_the_radio>'");
			l.sendUser("To create a new Radio: 'NEW <name_of_your_radio> list_of_songs'");
			l.sendUser("To stop : 'STOP'");
			l.sendUser("To list radios again: 'LIST'");
			l.sendUser("To get help: 'HELP'");
		}

		public void run() {
			String message = null;
			boolean flag = true;
			try {
				while(flag) {
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
							for(Radio r : listRadio) {
								if(r.getName() == message) {
									r.addListener(l);
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
							flag = false;
							sock.close();
						}
					}
				}
			}
			catch (Exception e) {e.printStackTrace();}
		}
	}
}