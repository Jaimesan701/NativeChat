package testDirectory;

import java.io.IOException;
import java.net.InetSocketAddress;

import es.um.redes.nanoChat.directory.connector.DirectoryConnector;

public class TestDirectory {
	public static void main(String[] args) throws IOException {
		String strToConvert = new String("fdgsdfg");
		//me pone en contacto con un servidor cliente que tiene ip que se pasa como parametro
		DirectoryConnector dc = new DirectoryConnector("192.168.1.67");
		
		InetSocketAddress resp = dc.getServerForProtocol(50);
		System.out.println(resp);
	}
}
