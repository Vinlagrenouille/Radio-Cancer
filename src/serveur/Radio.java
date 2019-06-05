package serveur;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Objects;

public class Radio implements Runnable {

	private String name;
	private ArrayList<String> listSong = new ArrayList<>();
	private ArrayList<Listener> listeners = new ArrayList<>();

	public Radio(String n, ArrayList<String> list) {
		Objects.requireNonNull(n);
		name = n;
		listSong = list;
	}
	
	public void addListener(Listener l) {
		listeners.add(l);
	}
	
	public boolean removeListener(Listener l) {
		return listeners.remove(l);
	}

	@Override
	public void run() {
		int MTU = 100;
		int tmp;
		try {
			for(Listener l : listeners) {
				tmp = NetworkInterface.getByInetAddress(l.getS().getInetAddress()).getMTU();
				if(tmp - 500 < MTU)
					MTU = tmp;
				if(MTU <= 0) {
					MTU = 100;
				}
			}
			for(String s : listSong) {
				File file = new File(s);
				InputStream fileStream = new FileInputStream(file);

				long length = file.length();
				if (length > Integer.MAX_VALUE) {
					System.out.println("TOO LARGE - ERROR");
				}

				byte[] pack = new byte[MTU];
				for(int i=0; i<length; i+=MTU) {
					fileStream.read(pack);
					sendUsers("STREAM"+pack);
				}

				fileStream.close();
				sendUsers("DONE");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendUsers(String message) {
		for(Listener l : listeners) {
			l.sendUser(message);
		}
	}
	
	

	public String getName() {
		return name;
	}
}
