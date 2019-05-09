import java.util.ArrayList;
import java.util.Objects;

public class Radio {

	String final name;
	ArrayList<String> listSong = new ArrayList<String>();

	public Radio(String n) {
		Objects.requireNonNull(n);
		name = n;		
	}

	public Radio(String n, ArrayList<String> list) {
		Objects.requireNonNull(n, list);
		name = n;
		listSong = list;
	}

	public void add(String song) {
		Objects.requireNonNull(song);
		listSong.add(song);
	}

	public void addList(ArrayList<String> list) {
		Objects.requireNonNull(list);
		listSong.addAll(list);
	}

	public void remove(String song) {
		Objects.requireNonNull(song);
		listSong.remove(song);
	}

}
