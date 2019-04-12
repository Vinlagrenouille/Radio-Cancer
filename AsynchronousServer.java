package server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AsynchronousServer {
	public static void main(String[] args) throws Exception {
		// Creation of the server socket channel
		AsynchronousServerSocketChannel server = AsynchronousServerSocketChannel.open();
		String host = "localhost";
		int port = 8989;
		// Defining the host and the port of the server
		InetSocketAddress sAddr = new InetSocketAddress(host, port);
		try {
			server.bind(sAddr);
		} catch (BindException e) {
			System.out.println("Port already used.");
			System.exit(0);
		}
		System.out.println("Server is listening at " + sAddr);

		// We create a list of client connections
		ClientConnectionInformation.connectedClients = new ArrayList<>();

		// We create the actual connection
		ClientConnectionInformation clientCoInfo = new ClientConnectionInformation();
		clientCoInfo.server = server;

		// Accepting a connection with the Connection Handler !
		server.accept(clientCoInfo, new ConnectionHandler());
		Thread.currentThread().join();
	}
}

// CLient connection informations !!
class ClientConnectionInformation {
	// The connected clients to the server
	static List<ClientConnectionInformation> connectedClients = new ArrayList<>();

	// The list of messages not received yet
	// Map of <LOGIN, LIST_OF_MESSAGES_TO_THIS_LOGIN> and each message is
	// structured like this : sender-message
	static Map<String, List<String>> messages = new HashMap<>();

	// The server
	AsynchronousServerSocketChannel server;

	// The socket of the client
	AsynchronousSocketChannel client;

	// The buffer
	ByteBuffer buffer;

	// The client address
	SocketAddress clientAddr;

	// To know if the server is waiting for reading the client (isRead=true) or
	// for writing to the client (isRread=false)
	boolean isRead;

	// login of this client
	String login;

	// the login of the receiver of the message sent by this client
	String dest;
}

// COnnection handler used when a client is connecting to the server
class ConnectionHandler implements CompletionHandler<AsynchronousSocketChannel, ClientConnectionInformation> {

	// If the connection has succeeded, we enter in the completed method!
	@Override
	public void completed(AsynchronousSocketChannel client, ClientConnectionInformation attach) {
		try {
			// We accepted this connection
			SocketAddress clientAddr = client.getRemoteAddress();
			System.out.println("Accepted a connection from " + clientAddr);

			// We keep listening to another client to connect
			attach.server.accept(attach, this);

			// Creation of the completion handler for read write operations
			ReadWriteHandler rwHandler = new ReadWriteHandler();

			// Saving the client informations
			ClientConnectionInformation newAttach = new ClientConnectionInformation();
			newAttach.server = attach.server;
			newAttach.client = client;
			newAttach.buffer = ByteBuffer.allocate(2048);
			newAttach.isRead = false;
			newAttach.clientAddr = clientAddr;

			// Adding the new client to the list of connected clients
			ClientConnectionInformation.connectedClients.add(newAttach);

			// We print the connected clients
			for (ClientConnectionInformation a : ClientConnectionInformation.connectedClients) {
				System.out.println("connected : " + a.clientAddr);
			}

			Charset cs = Charset.forName("UTF-8");
			String msg = ":" + newAttach.server.getLocalAddress() + " CNX";
			byte[] data = msg.getBytes(cs);
			newAttach.buffer.put(data);
			newAttach.buffer.flip();

			// We write to the client the command CNX to ask him if he already
			// has an account, and the dialog begins
			client.write(newAttach.buffer, newAttach, rwHandler);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// If not we enter in the failed method
	@Override
	public void failed(Throwable e, ClientConnectionInformation attach) {
		System.out.println("Failed to accept a  connection.");
		e.printStackTrace();
	}
}

// Completion handler for the read or write operations
class ReadWriteHandler implements CompletionHandler<Integer, ClientConnectionInformation> {

	// If the read/write operation was a succeed, we enter in the completed
	// method
	@Override
	public void completed(Integer result, ClientConnectionInformation attach) {
		// If the result=-1, the client has disconnect, so we disconnect it and
		// stop listening to it
		if (result == -1) {
			try {
				attach.client.close();
				System.out.println("Stopped listening to the client " + attach.clientAddr);

				// we remove this client of the list of the connected clients
				ClientConnectionInformation.connectedClients.remove(attach);
			} catch (IOException ex) {
				ex.printStackTrace();
			}
			return;
		}

		if (attach.isRead) {
			// Read succeeded
			// Getting the message we just read
			attach.buffer.flip();
			int limits = attach.buffer.limit();
			byte bytes[] = new byte[limits];
			attach.buffer.get(bytes, 0, limits);
			Charset cs = Charset.forName("UTF-8");
			String msg = new String(bytes, cs);
			msg = msg.split("\r")[0];

			// treatment of the message
			treatmentForMessage(msg, attach);

			attach.isRead = false; // Now we will write to the client
			attach.buffer.rewind();

			attach.client.write(attach.buffer, attach, this);
		} else {
			// Write succeeded
			attach.isRead = true; // Now we will read from the client
			attach.buffer.clear();
			attach.client.read(attach.buffer, attach, this);
		}
	}

	// Treatment of a message
	private void treatmentForMessage(String msg, ClientConnectionInformation clientCoInfo) {
		// Syso to know what the client sends
		// System.out.println("Client at " + clientCoInfo.clientAddr + " says :
		// " + msg);

		// We separate the different elements of the message : the prefix, the
		// command and the params (msg)
		int prefixEndIndex = msg.indexOf(" ");
		String prefix = msg.substring(1, prefixEndIndex - 1);
		msg = msg.substring(prefixEndIndex + 1);
		int commandEndIndex = msg.indexOf(" ");
		String command = "";
		if (commandEndIndex < 0) {
			command = msg;
			msg = "";
		} else {
			command = msg.substring(0, commandEndIndex);
			msg = msg.substring(commandEndIndex + 1);
		}

		Charset cs = Charset.forName("UTF-8");
		SocketAddress address = null;
		try {
			address = clientCoInfo.server.getLocalAddress();
		} catch (IOException e2) {
			e2.printStackTrace();
		}

		clientCoInfo.buffer.clear();
		String response = "";

		// Now we switch on the different possible commands
		switch (command) {
		case "CNX":
			if (msg.equalsIgnoreCase("y")) {
				// Connection to as existing account
				response = ":" + address + " NICK";
			} else {
				// Creating a new account
				response = ":" + address + " NEW";
			}
			break;
		case "NICK":
			// Login
			clientCoInfo.login = msg;
			if (loginExists(clientCoInfo.login) && !alreadyConnected(clientCoInfo.login)) {
				response = ":" + address + " PASS";
			} else if (alreadyConnected(clientCoInfo.login)) {
				System.out.println("login already connected");
				response = ":" + address + " ERR_NICKCOLLISION";
			} else {
				System.out.println("error nick");
				response = ":" + address + " NICK e";
			}
			break;
		case "ERR_NICKCOLLISION": // A second user try to connect with a login
			// already connected -- disconnect both
			for (ClientConnectionInformation c : ClientConnectionInformation.connectedClients) {
				if (c.login.equals(clientCoInfo.login)) {
					response = ":" + address + " QUIT" + " collision";
				}
			}
			break;
		case "ERR_NONICKNAMEGIVEN": // login is empty
			System.out.println("LOGIN EMPTY");
			if (msg.contains("newErr"))
				response = ":" + address + " NEW";
			else
				response = ":" + address + " NICK";
			break;
		case "PASS":
			// Enter the password of the existing account
			if (loginAssociatedToPassword(clientCoInfo.login, msg)) {
				System.out.println(clientCoInfo.login + " just signed in.");
				response = ":" + address + " MENU";

				// If the user does not exist in the map of message not received
				// yet
				if (!ClientConnectionInformation.messages.containsKey(clientCoInfo.login)) {
					// We inser it
					ClientConnectionInformation.messages.put(clientCoInfo.login, new ArrayList<String>());
				}
				// We check if the user has message not received yet, if yes we
				// tell the client with "new"
				if (!ClientConnectionInformation.messages.get(clientCoInfo.login).isEmpty()) {
					response += " new";
				}
			} else {
				System.out.println("error pass");
				response = ":" + address + " PASS e";
			}
			break;

		// Login of the new account
		case "NEW":
			if (!loginExists(msg)) {
				clientCoInfo.login = msg;
				response = ":" + address + " NEWPASS";
			} else {
				System.out.println("login already used");
				response = ":" + address + " NEW" + " used";
			}
			break;

		// The password, fullname and mail of the enw account
		case "NEWPASS":
			String[] tab = msg.split(" ");
			try {
				createNewAccount(clientCoInfo.login, tab[0], tab[1], tab[2]);
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println(clientCoInfo.login + " just signed up.");
			response = ":" + address + " MENU";
			if (!ClientConnectionInformation.messages.containsKey(clientCoInfo.login)) {
				ClientConnectionInformation.messages.put(clientCoInfo.login, new ArrayList<String>());
			}
			if (!ClientConnectionInformation.messages.get(clientCoInfo.login).isEmpty()) {
				response += " new";
			}
			break;

		// Menu
		case "MENU":
			switch (msg) {
			case "1":
				try {
					response = ":" + address + " CONTACTS_OPTIONS " + listOfContacts(clientCoInfo.login);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
				break;
			case "2":
				try {
					response = ":" + address + " CHAT " + listOfContacts(clientCoInfo.login);
				} catch (FileNotFoundException e2) {
					e2.printStackTrace();
				}
				if (!ClientConnectionInformation.messages.get(clientCoInfo.login).isEmpty()) {
					response += " new";
					String sender = ClientConnectionInformation.messages.get(clientCoInfo.login).get(0).split(";@;")[0];
					response += "-" + sender;
				}
				break;
			case "3":
				try {
					response = ":" + address + " CHATN " + listOfContacts(clientCoInfo.login);
				} catch (FileNotFoundException e2) {
					e2.printStackTrace();
				}
				;
				break;
			case "4":
				response = ":" + address + " OPTIONS";
				break;
			}
			break;

		// Contact options menu
		case "CONTACTS_OPTIONS":
			switch (msg) {
			case "1":
				response = ":" + address + " ADD_CONTACT " + listOfUsers(clientCoInfo.login);
				break;
			case "2":
				try {
					response = ":" + address + " DEL_CONTACT " + listOfContacts(clientCoInfo.login);
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				}
				break;
			case "3":
				response = ":" + address + " MENU";
				if (!ClientConnectionInformation.messages.get(clientCoInfo.login).isEmpty()) {
					response += " new";
				}
				break;
			}
			break;

		// Add a contact
		case "ADD_CONTACT":
			addInContacts(clientCoInfo.login, msg);
			try {
				response = ":" + address + " CONTACTS_OPTIONS " + listOfContacts(clientCoInfo.login) + " added";
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			break;

		// Delete a contact
		case "DEL_CONTACT":
			try {
				String[] contacts = listOfContacts(clientCoInfo.login).split("-");
				delFromContacts(clientCoInfo.login, contacts[Integer.parseInt(msg) - 1]);
				response = ":" + address + " CONTACTS_OPTIONS " + listOfContacts(clientCoInfo.login) + " deleted";
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}
			break;

		// Options menu
		case "OPTIONS":
			switch (msg) {
			case "1":
				response = ":" + address + " CHANGE login";
				break;
			case "2":
				response = ":" + address + " CHANGE pass";
				break;
			case "3":
				response = ":" + address + " CHANGE fullName";
				break;
			case "4":
				response = ":" + address + " CHANGE mail";
				break;
			case "6":
				response = ":" + address + " MENU";
				if (!ClientConnectionInformation.messages.get(clientCoInfo.login).isEmpty()) {
					response += " new";
				}
				break;
			}
			break;

		// Change info of the client
		case "CHANGE":
			String type = msg.split("-")[0];
			String change = msg.split("-")[1];
			if (!loginExists(change)) {
				changeUserInfo(clientCoInfo, type, change);
				response = ":" + address + " OPTIONS " + type;
			} else {
				System.out.println("login already used");
				response = ":" + address + " ERR_NICKNAMEINUSE";
			}
			break;

		// CHat 1 to 1
		case "CHAT":
			clientCoInfo.dest = msg;
			if (ClientConnectionInformation.messages.get(clientCoInfo.login).isEmpty()) {
				response = ":" + address + " PRIVMSG " + clientCoInfo.dest;
			} else {
				for (String s : ClientConnectionInformation.messages.get(clientCoInfo.login)) {
					if (s.contains(clientCoInfo.dest)) {
						response = ":" + address + " PRIVMSG " + s;
						ClientConnectionInformation.messages.get(clientCoInfo.login).remove(s);
						break;
					}
				}
			}
			break;

		// Chat 1 to List
		case "CHATN":
			String[] msgSplit = msg.split(";@;");
			if (msgSplit[1].equalsIgnoreCase("/quit")) {
				response = ":" + address + " MENU";
				if (!ClientConnectionInformation.messages.get(clientCoInfo.login).isEmpty()) {
					response += " new";
				}
			} else {
				String[] dests = msgSplit[0].split("-");
				for (int i = 0; i < dests.length; i++) {
					List<String> msgs = new ArrayList<>();
					if (ClientConnectionInformation.messages.containsKey(dests[i])) {
						msgs = ClientConnectionInformation.messages.get(dests[i]);
					}
					msgs.add(clientCoInfo.login + "-" + msgSplit[1]);
					ClientConnectionInformation.messages.put(dests[i], msgs);
				}
			}
			response = ":" + address + " MENU";
			if (!ClientConnectionInformation.messages.get(clientCoInfo.login).isEmpty()) {
				response += " new";
			}
			break;

		// Private message
		case "PRIVMSG":

			// If the user wants to return to the menu
			if (msg.split("-")[1].equalsIgnoreCase("/quit")) {
				response = ":" + address + " MENU";

				// Check if he has new messages
				if (!ClientConnectionInformation.messages.get(clientCoInfo.login).isEmpty()) {
					response += " new";
				}
				clientCoInfo.dest = null;
			} else {
				// Obtaining the receiver login
				clientCoInfo.dest = msg.split("-")[0];

				// We check if he has a List of message in the list of messages
				// not received yet
				List<String> list = ClientConnectionInformation.messages.get(clientCoInfo.dest);
				if (Objects.isNull(list)) {
					ClientConnectionInformation.messages.put(clientCoInfo.dest, new ArrayList<String>());
					list = ClientConnectionInformation.messages.get(clientCoInfo.dest);
				}
				System.out.println(msg);
				// We had the message sent in this list
				list.add(clientCoInfo.login + ";@;" + msg.split("-")[1]);
				ClientConnectionInformation.messages.put(clientCoInfo.dest, list);

				System.out.println("list " + ClientConnectionInformation.messages.get(clientCoInfo.login));

				if (ClientConnectionInformation.messages.get(clientCoInfo.login).isEmpty()) {
					response = ":" + address + " PRIVMSG " + clientCoInfo.dest;
					if (!alreadyConnected(clientCoInfo.dest))
						response += "-rpl_away";
				} else {
					for (String s : ClientConnectionInformation.messages.get(clientCoInfo.login)) {
						if (s.contains(clientCoInfo.dest)) {
							response = ":" + address + " PRIVMSG " + s;
							ClientConnectionInformation.messages.get(clientCoInfo.login).remove(s);
							break;
						}
					}
				}
			}
			break;

		// Actualization of the chat 1 to 1
		case "ACTUALIZE":
			if (ClientConnectionInformation.messages.get(clientCoInfo.login).isEmpty()) {
				response = ":" + address + " PRIVMSG " + clientCoInfo.dest;
			} else {
				for (String s : ClientConnectionInformation.messages.get(clientCoInfo.login)) {
					if (s.contains(clientCoInfo.dest)) {
						response = ":" + address + " PRIVMSG " + s;
						ClientConnectionInformation.messages.get(clientCoInfo.login).remove(s);
						break;
					}
				}
			}
			break;

		// Proper deconnexion
		case "QUIT":
			if (msg.contains("delete")) {
				deleteAccount(clientCoInfo.login);
				response = ":" + address + " QUIT deleted";
			} else if (msg.contains("tooMuchTentative")) {
				response = ":" + address + " QUIT tooMuchTentative";
			} else
				response = ":" + address + " QUIT";
			break;
		default:
			System.out.println("Command not handled");
			break;
		}
		response = response + "\r\n";
		byte[] data = response.getBytes(cs);
		clientCoInfo.buffer.put(data);
		clientCoInfo.buffer.flip();
	}

	// Check if a connected clients uses this login
	private boolean alreadyConnected(String login) {
		int score = 0;
		for (ClientConnectionInformation a : ClientConnectionInformation.connectedClients) {
			if (!Objects.isNull(a.login) && a.login.equals(login))
				score++;
		}
		if (score > 1)
			return true;
		return false;
	}

	// Change a user informations in the directory file
	private void changeUserInfo(ClientConnectionInformation attach, String type, String change) {
		File dir = new File("directory.txt");
		File temp = new File("temp.txt");
		try {
			BufferedReader br = new BufferedReader(new FileReader(dir));
			BufferedWriter bw = new BufferedWriter(new FileWriter(temp));
			String line = "";

			while ((line = br.readLine()) != null) {
				if (line.startsWith("1;@;" + attach.login + ";@;")) {
					String[] tab = line.split(";@;");
					switch (type) {
					case "login":
						tab[1] = change;
						attach.login = change;
						break;
					case "pass":
						tab[2] = change;
						break;
					case "fullName":
						tab[3] = change;
						break;
					case "mail":
						tab[4] = change;
						break;
					}
					String newLine = "1";
					for (int i = 1; i < tab.length; i++) {
						newLine += ";@;" + tab[i];
					}
					if (tab.length <= 5)
						newLine += ";@;";
					bw.write(newLine + "\n");
				} else {
					bw.write(line + "\n");
				}
				bw.flush();
			}
			bw.close();
			br.close();

			dir.delete();
			temp.renameTo(new File("directory.txt"));

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Delete an account (it will make it UNACTIVE)
	private void deleteAccount(String login) {
		File dir = new File("directory.txt");
		File temp = new File("temp.txt");
		try {
			BufferedReader br = new BufferedReader(new FileReader(dir));
			BufferedWriter bw = new BufferedWriter(new FileWriter(temp));
			String line = "";

			while ((line = br.readLine()) != null) {
				if (line.startsWith("1;@;" + login + ";@;")) {
					bw.write("0" + line.substring(1) + "\n");
				} else {
					bw.write(line + "\n");
				}
				bw.flush();
			}
			bw.close();
			br.close();

			dir.delete();
			temp.renameTo(new File("directory.txt"));

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Delete a contact from the list of contacts of a login
	private void delFromContacts(String login, String contact) {
		File dir = new File("directory.txt");
		File temp = new File("temp.txt");
		try {
			BufferedReader br = new BufferedReader(new FileReader(dir));
			BufferedWriter bw = new BufferedWriter(new FileWriter(temp));
			String line = "";

			while ((line = br.readLine()) != null) {
				if (line.startsWith("1;@;" + login + ";@;") && line.contains(contact)) {
					line = line.substring(0, line.lastIndexOf(";") + 1);
					String tab = listOfContacts(login);
					if (tab.equals(contact)) {
						bw.write(line + "\n");
					} else if (tab.startsWith(contact)) {
						bw.write(line + tab.substring(tab.indexOf("-") + 1) + "\n");
					} else if (tab.endsWith(contact)) {
						bw.write(line + tab.substring(0, tab.indexOf(contact) - 1) + "\n");
					} else if (tab.contains(contact)) {
						String first = tab.substring(0, tab.indexOf(contact));
						String second = tab.substring(tab.indexOf(contact));
						second = second.substring(second.indexOf("-") + 1);
						bw.write(line + first + second + "\n");
					}
				} else {
					bw.write(line + "\n");
				}
				bw.flush();
			}
			bw.close();
			br.close();

			dir.delete();
			temp.renameTo(new File("directory.txt"));

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Returns the list of active users of this server
	private String listOfUsers(String login) {
		File dir = new File("directory.txt");
		String user = "";
		try {
			BufferedReader br = new BufferedReader(new FileReader(dir));
			String line = "";
			while ((line = br.readLine()) != null) {
				if (line.startsWith("1")) {
					line = line.substring(4);
					if (!line.startsWith(login + ";@;")) {
						int index = line.indexOf(";@;");
						String loginContact = line.substring(0, index);
						if (!isAlreadyAContact(login, loginContact)) {
							if (user.equals("")) {
								user = loginContact;
							} else {
								user = user + "-" + loginContact;
							}
						}
					}
				}
			}
			br.close();
			if (user.length() == 0)
				return "";
		} catch (IOException e) {
			e.printStackTrace();
		}

		return user;
	}

	// Check if the contact is already a contact of this login
	private boolean isAlreadyAContact(String login, String loginContact) {
		try {
			File dir = new File("directory.txt");
			BufferedReader br = new BufferedReader(new FileReader(dir));
			String line = "";
			String user = "";
			while ((line = br.readLine()) != null) {
				if (line.startsWith("1;@;" + login + ";@;")) {
					user = line;
				}
			}
			br.close();
			String[] tab = user.split(";@;");
			if (5 >= tab.length)
				return false;
			if (tab[5].contains(loginContact))
				return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	// Check if the login and the password are correct
	private boolean loginAssociatedToPassword(String login2, String pass2) {
		try {
			File dir = new File("directory.txt");
			BufferedReader br = new BufferedReader(new FileReader(dir));
			String line = "";

			while ((line = br.readLine()) != null) {
				if (line.startsWith("1;@;" + login2 + ";@;" + pass2 + ";@;")) {
					br.close();
					return true;
				}
			}
			br.close();
		} catch (FileNotFoundException e) {
			System.err.println("L'annuaire n'existe pas !");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	// Check if the login already exists
	private boolean loginExists(String msg) {
		try {
			File dir = new File("directory.txt");
			BufferedReader br = new BufferedReader(new FileReader(dir));
			String line = "";

			while ((line = br.readLine()) != null) {
				if (line.startsWith("1;@;" + msg + ";@;")) {
					br.close();
					return true;
				}
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	// Create a new account
	private void createNewAccount(String login, String pass, String fullName, String mail) throws IOException {
		String s = "1;@;" + login + ";@;" + pass + ";@;" + fullName + ";@;" + mail + ";@;\n";
		byte data[] = s.getBytes();
		Path p = Paths.get("directory.txt");
		Files.write(p, data, StandardOpenOption.APPEND);
	}

	// return the list of contacts of this login
	private String listOfContacts(String login) throws FileNotFoundException {
		File dir = new File("directory.txt");
		BufferedReader br = new BufferedReader(new FileReader(dir));
		String line = "";

		try {
			while ((line = br.readLine()) != null) {
				if (line.startsWith("1;@;" + login + ";@;")) {
					break;
				}
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		String[] tab = line.split(";@;");
		if (5 >= tab.length)
			return "";
		return tab[5];
	}

	// Add a new contact in the list of contacts of this login
	private void addInContacts(String login, String contact) {
		File dir = new File("directory.txt");
		File temp = new File("temp.txt");
		try {
			BufferedReader br = new BufferedReader(new FileReader(dir));
			BufferedWriter bw = new BufferedWriter(new FileWriter(temp));
			String line = "";

			while ((line = br.readLine()) != null) {
				if (line.startsWith("1;@;" + login)) {
					if (line.endsWith(";"))
						bw.write(line + contact + "\n");
					else
						bw.write(line + "-" + contact + "\n");
				} else {
					bw.write(line + "\n");
				}
				bw.flush();
			}
			bw.close();
			br.close();

			dir.delete();
			temp.renameTo(new File("directory.txt"));

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Read write not completed
	@Override
	public void failed(Throwable e, ClientConnectionInformation attach) {
		e.printStackTrace();
	}
}