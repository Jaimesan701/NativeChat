package es.um.redes.nanoChat.directory.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class DirectoryThread extends Thread {

	// CONSTANTE
	private static final int PACKET_MAX_SIZE = 128;	// tamaño máximo del paquete UDP

	// ATRIBUTOS
	protected Map<Integer, InetSocketAddress> servers; // estructura para guardar las asociaciones ID_PROTOCOLO -> Dirección del servidor
	protected DatagramSocket socket = null;	// Socket de comunicación UDP
	protected double messageDiscardProbability; // probabilidad de descarte del mensaje

	// CONSTRUCTOR
	public DirectoryThread(String name, int directoryPort, double corruptionProbability) throws SocketException {

		super(name);

		// Asociamos una direccion IP y puerto al servidor de directorio,
		// si no se indica la direccion entonces es localhost
		InetSocketAddress serverAddress = new InetSocketAddress(directoryPort);

		// Creamos un socket UDP
		// El servidor de directorio se queda escuchando en la ip y puerto de la variable serverAddress
		socket = new DatagramSocket(serverAddress);
		messageDiscardProbability = corruptionProbability;

		// Inicialización del mapa que asocia identificadores de protocolo con ip,puerto
		servers = new HashMap<Integer, InetSocketAddress>();

	}

	// MÉTODO QUE EJECUTA UN HILO
	@Override
	public void run() {

		byte[] buf = new byte[PACKET_MAX_SIZE];

		System.out.println("Directory starting...");
		boolean running = true;
		while (running) {

			// Recibimos la solicitud en el DatagramPacket a través del DatagramSocket
			DatagramPacket dpRec = new DatagramPacket(buf, buf.length);
			try {
				socket.receive(dpRec);
			} catch (IOException e) {
				e.printStackTrace();
			}
			// Extraemos quién es el cliente (el emisor del paquete recibido)
			InetSocketAddress clientAddress = (InetSocketAddress) dpRec.getSocketAddress();

			// Vemos si el mensaje debe ser descartado por la probabilidad de descarte
			double rand = Math.random();
			if (rand < messageDiscardProbability) {
				System.err.println("Directory DISCARDED corrupt request from... ");
				continue;
			}
			// Analizamos y procesamos la solicitud (llamando a processRequestFromCLient)
			try {
				processRequestFromClient(dpRec.getData(), clientAddress);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// TODO 5) Tratar las excepciones que puedan producirse
		}
		socket.close();

	}

	// Método para procesar la solicitud enviada por clientAddr
	public void processRequestFromClient(byte[] data, InetSocketAddress clientAddr) throws IOException {
		// Extraemos el tipo de mensaje recibido
		ByteBuffer rec = ByteBuffer.wrap(data);
		int opcode = rec.get();
		// Si se trata de un registro (código 1), lo formalizamos y respondemos con un sendOK (código 2)
		if (opcode == 1) {
			Integer protocol = rec.getInt();
			Integer port = rec.getInt();
			InetSocketAddress IpPort = new InetSocketAddress(clientAddr.getAddress(), port);
			System.out.println("Indentificador de protocolo " + protocol);
			System.out.println("Puerto recibido " + port);
			System.out.println("Puerto del proceso que envia " + clientAddr.getPort());
			servers.put(protocol, IpPort);	// registramos el par [identificador de protocolo | IP,puerto]
			System.out.println("Servidores registrados : ");
			for(Integer p : servers.keySet())
				System.out.println("1 : protocolo: "+p+" IP/puerto : "+ servers.get(p).toString());
			sendOK(clientAddr);	// enviamos confirmación
		}
		// Si se trata de una consulta (código 3), la procesamos y respondemos (código 4)
		// si lo que se consulta no existe, respondemos con sendEmpty (código 5)
		if (opcode == 3) {
			int idProtocol = rec.get();
			if (servers.containsKey(idProtocol))
				sendServerInfo(servers.get(idProtocol), clientAddr);
			else {
				System.out.println("No existe");
				sendEmpty(clientAddr);
			}
		}

	}

	// Método para enviar la confirmación del registro (código 2)
	private void sendOK(InetSocketAddress clientAddr) throws IOException {
		// Construimos la respuesta
		ByteBuffer bb = ByteBuffer.allocate(1);
		byte opcode = 2;
		bb.put(opcode);
		byte[] sendBuf = bb.array();
		DatagramPacket dataToSend = new DatagramPacket(sendBuf, sendBuf.length, clientAddr);
		// Enviamos la respuesta
		socket.send(dataToSend);
	}

	// Método para enviar la información de un servidor de chat (código 4)
	private void sendServerInfo(InetSocketAddress serverAddress, InetSocketAddress clientAddr) throws IOException {
		// Obtenemos la representación binaria de la dirección
		InetAddress direccionIP = serverAddress.getAddress();
		String direccionIPString = direccionIP.getHostAddress().toString();
		String[] direccionIPSplit = direccionIPString.split("[.]");
		// Construimos la respuesta
		ByteBuffer bb = ByteBuffer.allocate(9); // 9 bytes CAMBIAR
		byte opcode = 4;
		bb.put(opcode);
		bb.putInt(serverAddress.getPort());
		for (String s : direccionIPSplit) {
			bb.put((byte) Integer.parseInt(s));
		}
		byte[] envio = bb.array();
		DatagramPacket paqueteEnvio = new DatagramPacket(envio, envio.length, clientAddr);
		// Enviamos respuesta
		socket.send(paqueteEnvio);

	}

	// Método para enviar una respuesta vacía en el caso de que no exista el servidor que se consulta (código 5)
	private void sendEmpty(InetSocketAddress clientAddr) throws IOException {
		ByteBuffer bb = ByteBuffer.allocate(1);
		byte opcode = 5;
		bb.put(opcode);
		// Enviamos la respuesta
		byte[] envio = bb.array();
		DatagramPacket pckt = new DatagramPacket(envio, envio.length, clientAddr);
		socket.send(pckt);
	}


}
