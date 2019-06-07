package serveur;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
		System.out.println("Radio created");
		System.out.println(listSong);
		run();
	}

	public void addListener(Listener l) {
		listeners.add(l);
	}

	public boolean removeListener(Listener l) {
		return listeners.remove(l);
	}

	@Override
	public void run() {
		System.out.println("Starting the radio..");
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
				System.out.println("Playing "+s);
				File file = new File(System.getProperty("user.dir")+"\\Musique\\"+name+"\\"+s);
				InputStream fileStream = new FileInputStream(file);

				long length = file.length();
				if (length > Integer.MAX_VALUE) {
					System.out.println("TOO LARGE - ERROR");
				}

				byte[] pack = new byte[MTU+1];
				int offset = 0;
				int numRead = 0;
				while (offset < pack.length && (numRead=fileStream.read(pack, offset, pack.length-offset)) >= 0) {
					offset += numRead;
				}
				if (offset < pack.length) {
					throw new IOException("Could not completely read file "+ file.getName());
				}
				String value = new String(pack);
				/*for(int i=0; i<length; i+=MTU) {
					if(i+MTU >= length) {
						fileStream.read(pack, i, (int) (length-i));
						System.out.println(pack.toString());
					}
					else {
						fileStream.read(pack, i, MTU);
						System.out.println(pack.toString());
					}
					sendUsers("STREAM"+pack);
				}*/
				sendUsers("STREAM"+value);
				fileStream.close();
				sendUsers("DONE");
			}
			System.out.println("End of the radio");
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
