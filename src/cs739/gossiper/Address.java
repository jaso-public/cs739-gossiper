package cs739.gossiper;

import java.util.Objects;

public class Address {
	public final String ipAddress;
	public final int port;
	
	public Address(String ipAddress, int port) {
		super();
		this.ipAddress = ipAddress;
		this.port = port;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(ipAddress, port);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Address other = (Address) obj;
		return Objects.equals(ipAddress, other.ipAddress) && port == other.port;
	}

	@Override
	public String toString() {
		return "Address [ipAddress=" + ipAddress + ", port=" + port + "]";
	}
}
