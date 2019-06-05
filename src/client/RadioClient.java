package client;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.net.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.sound.sampled.*;
import java.util.jar.*;
import javax.net.ssl.*;


public class RadioClient extends JFrame {
	Clip clip1;
	SSLSocket sock;
	SSLSocket sock_2;
	DefaultListModel list = new DefaultListModel();
	BufferedReader reader;
	PrintWriter writer;
	//BufferedReader reader_2;
	//PrintWriter writer_2;

	public RadioClient() {
		connectTo();
	}
	
	private class IncomingReader implements Runnable {
		public void run() {
			String message = null;
			AudioInputStream sound;
			try {
				while((message = reader.readLine()) != null) {
					if (message.startsWith("DONE")) {

					}
					else if(message.startsWith("STREAM")) {
						message = message.replaceFirst("STREAM", "");
						byte[] byteToPlay = message.getBytes();
						InputStream listen = new ByteArrayInputStream(byteToPlay);
						sound = AudioSystem.getAudioInputStream(listen);
						// load the sound into memory (a Clip)
						DataLine.Info info = new DataLine.Info(Clip.class, sound.getFormat());
						clip1 = (Clip) AudioSystem.getLine(info);
						clip1.open(sound);
						clip1.addLineListener(new LineListener() {
							public void update(LineEvent event) {
								if (event.getType() == LineEvent.Type.STOP) {
									event.getLine().close();
								}
							}
						});
						// play the sound clip
						clip1.start();
					}
					else if((!message.equals("STOP"))) {
						list.insertElementAt(message, 0);
						validate();
						while (list.getSize() == 0) {
							writer.println("LIST");
							writer.flush();
						}
					}
				}
			}
			catch (Exception e) {e.printStackTrace();}
		}
	}
	
	private void connectTo() {
		try {
			SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
			sock = (SSLSocket) sslsocketfactory.createSocket("localhost", 5001);
			final String[] enabledCipherSuites = { "SSL_DH_anon_WITH_RC4_128_MD5" };
			sock.setEnabledCipherSuites(enabledCipherSuites);
			InputStreamReader streamReader = new InputStreamReader(sock.getInputStream());
			Thread readerThread = new Thread(new IncomingReader());
			readerThread.start();
			reader = new BufferedReader(streamReader);
			writer = new PrintWriter(sock.getOutputStream());
			writer.println("CHOOSE");
			writer.flush();
			Thread.sleep(3000);
			while (list.getSize() == 0) {
				writer.println("CHOOSE");
				writer.flush();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

}