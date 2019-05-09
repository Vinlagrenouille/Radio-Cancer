import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.*;
import java.lang.*;

public class NicInform{

public static void main(String[] args) throws SocketException {
    Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
    while (networkInterfaces.hasMoreElements()) {
        NetworkInterface networkInterface = networkInterfaces.nextElement();
        System.out.printf("%s with %s%n", networkInterface.getDisplayName(),
                macString(networkInterface).orElse("no hardware address"));
    }
}

/**
 * Gets the hardware address of a {@link NetworkInterface} in the format
 * {@code AA-BB-CC-DD-EE-FF}.
 *
 * @param iface The interface to get the MAC address string of.
 * @return A optional containing the string representation. This optional will
 * be empty if the network interface does not have a hardware address (virtual adapters like
 * loopback devices).
 * @throws SocketException If an I/O error occurs when getting the hardware address from the interface.
 */
private static Optional<String> macString(NetworkInterface iface) throws SocketException {
    byte[] mac = iface.getHardwareAddress();
    if (mac == null) {
        return Optional.empty();
    }
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < mac.length; i++) {
        sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
    }
    return Optional.of(sb.toString());
}

}
