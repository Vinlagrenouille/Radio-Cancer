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

//Pour envoi/r�ception paquet, regarder MTU !!! Permet de cr�er des paquets de plusieurs bytes.
//Pour l'asynchrone, regarder AIO files !!!

public class RadioClient extends JFrame {
	Clip clip1;
	SSLSocket sock;
	SSLSocket sock_2;
	JList songs;
	DefaultListModel list = new DefaultListModel();
	BufferedReader reader;
	PrintWriter writer;
	BufferedReader reader_2;
	PrintWriter writer_2;

	private class IncomingReader implements Runnable {
		public void run() {
			String message = null;
			boolean done = true;
			boolean started = false;
			StringBuilder tryMe = new StringBuilder("");
			AudioInputStream sound;
			try {
				while((message = reader.readLine()) != null) {
					if (message.startsWith("DONE")) {
						String stringToConvertToBytes = tryMe.toString();
						byte[] newSounds = stringToConvertToBytes.getBytes();
						started = false;
						done = true;
						playSound(newSounds);

					}
					else if(message.startsWith("STREAM")) {
						message = message.replaceFirst("STREAM", "");
						byte byteToPlay = message.getBytes();
						InputStream listen = new ByteArrayInputStream(songsToPlay);
						AudioInputStream sound = AudioSystem.getAudioInputStream(listen);
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
						/*tryMe.delete(0, tryMe.length());
						started = true;
						done = false;
						message = message.replaceFirst("STREAM", "");
						tryMe.append(message);*/
					}
					else if (started && !done) {
						tryMe.append(message);
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

	public static void main(String[] args) {
		new FileHostingClient().init();
	}

	private void init() {
		songs = new JList(list);
		songs.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		songs.addMouseListener(new MouseListener() {
			public void mousePressed(MouseEvent mousePress) {
			}
			public void mouseReleased(MouseEvent mouseRelease) {
			}
			public void mouseEntered(MouseEvent mouseEnter) {
			}
			public void mouseExited(MouseEvent mouseExit) {
			}
			public void mouseClicked(MouseEvent mouseClick) {
				JList theList = (JList) mouseClick.getSource();
				if (mouseClick.getClickCount() == 2) {
					int index = theList.locationToIndex(mouseClick.getPoint());
					if (index >= 0) {
						Object o = theList.getModel().getElementAt(index);
						System.out.println("CHOOSE " + o.toString());
						writer.println("CHOOSE " + o.toString());
						writer.flush();
						try {
							clip1.stop();
						}
						catch (Exception e) {e.printStackTrace();}
					}
				}
			}
		});
		JScrollPane pane = new JScrollPane(songs);
		add(pane);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(300, 300);
		setVisible(true);
		connectTo();
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

	public void playSound(byte[] songsToPlay) {
		try {
			InputStream listen = new ByteArrayInputStream(songsToPlay);
			AudioInputStream sound = AudioSystem.getAudioInputStream(listen);
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
		catch (Exception e) {
			e.printStackTrace();
		}
	}

}