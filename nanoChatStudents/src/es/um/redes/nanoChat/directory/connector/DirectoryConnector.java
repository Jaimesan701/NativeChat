package es.um.redes.nanoChat.directory.connector;


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

/**
 * Cliente con métodos de consulta y actualización específicos del directorio
 */

public class DirectoryConnector {
	// CONSTANTES
	private static final int PACKET_MAX_SIZE = 128;	// tamaño máximo del paquete UDP (los mensajes intercambiados son muy cortos)
	private static final int DEFAULT_PORT = 6868; // puerto en el que atienden los servidores de directorio
	private static final int TIMEOUT = 1000; // valor del TIMEOUT

	// ATRIBUTOS
	private DatagramSocket socket; // socket UDP
	private InetSocketAddress directoryAddress; // dirección del servidor de directorio

	// CONSTRUCTOR
	public DirectoryConnector(String agentAddress) throws IOException {
		// Creamos un InetSocketAddress (dirección y puerto)
		directoryAddress = new InetSocketAddress(InetAddress.getByName(agentAddress), DEFAULT_PORT);
		// Creamos un socket UPD a través de la dirección y puerto
		socket = new DatagramSocket();
	}

	//------------------------- REGISTRAR UN SERVIDOR DE CHAT A UN PROTOCOLO -------------------------------------------
	/**
	 * Envía una solicitud para registrar el servidor de chat asociado a un determinado protocolo
	 *
	 */
	public boolean registerServerForProtocol(int protocol, int port) throws IOException {

		// Construimos la solicitud de registro (buildRegistration)
		byte[] solicitudDeRegistro = buildRegistration(protocol, port);
		byte[] mensajeRecibido = new byte[PACKET_MAX_SIZE];
		DatagramPacket dataToSend = new DatagramPacket(solicitudDeRegistro,solicitudDeRegistro.length,directoryAddress);
		DatagramPacket dataReceive = new DatagramPacket(mensajeRecibido, mensajeRecibido.length);
		// Enviamos la solicitud
		socket.send(dataToSend);
		// Recibimos la respuesta, si salta timeout de 1 seg sin recibir respuesta, volvemos a enviar los datos
		boolean respuesta_recibida = false;
		int num_reintentos_restantes = 5;
		while(!respuesta_recibida && num_reintentos_restantes > 0) {
			socket.setSoTimeout(TIMEOUT);   // set the timeout in millisecounds.
			try{
				socket.receive(dataReceive);
				respuesta_recibida = true;	// hemos recibido la respuesta
			}
			catch (SocketTimeoutException s) {	// no hemos recibido la respuesta
				socket.send(dataToSend);
				num_reintentos_restantes--;
			}
		}
		if(!respuesta_recibida)	// si no se ha recibido respuesta del servidor de directorio
			return false;
		// Procesamos la respuesta para ver si se ha podido registrar correctamente
		ByteBuffer respuesta  = ByteBuffer.wrap(dataReceive.getData());
		int opcode = respuesta.get();
		if(opcode == 2)
			return true;
		else return false;
	}

	// Método para construir una solicitud de registro de servidor según el formato acordado
	// La dirección desde la que se envía el mensaje viene implícita en este, por eso no la indicamos
	private byte[] buildRegistration(int protocol, int port) {
		ByteBuffer registroBinario = ByteBuffer.allocate(9);
		byte op_code = 1;	// código de registro
		registroBinario.put(op_code);
		registroBinario.putInt(protocol);
		registroBinario.putInt(port);
		return registroBinario.array();
	}

	//------------------------------- CONSULTAR DIRECCIÓN DE UN SERVIDOR DE CHAT ---------------------------------
	/**
	 * Envía una solicitud para obtener el servidor de chat asociado a un determinado protocolo
	 * 
	 */
	public InetSocketAddress getServerForProtocol(int protocol) throws IOException {

		// Generamos el mensaje de consulta llamando a buildQuery()
		byte[] consulta = buildQuery(protocol);
		// Construimos el datagrama con la consulta
		DatagramPacket dpSend = new DatagramPacket(consulta, consulta.length, directoryAddress);
		// Enviamos datagrama por el socket
		socket.send(dpSend);
		// Preparamos el buffer para la respuesta
		byte[] respuesta = new byte[PACKET_MAX_SIZE];
		DatagramPacket dpRec = new DatagramPacket(respuesta, respuesta.length);
		// Establecemos el temporizador para el caso en que no haya respuesta
		boolean respuesta_recibida = false;
		int num_reintentos_restantes = 5;
		while(!respuesta_recibida && num_reintentos_restantes > 0) {
			socket.setSoTimeout(TIMEOUT);   // set the timeout in millisecounds.
			try{
				socket.receive(dpRec);
				respuesta_recibida = true;	// hemos recibido la respuesta
			}
			catch (SocketTimeoutException s) {	// no hemos recibido la respuesta
				socket.send(dpSend);
				num_reintentos_restantes--;
			}
		}
		// Procesamos la respuesta para devolver la dirección que hay en ella
		InetSocketAddress direccionFinal = getAddressFromResponse(dpRec);
		return direccionFinal;
	}

	// Método para generar el mensaje de consulta (para obtener el servidor asociado a un protocolo)
	private byte[] buildQuery(int protocol) {
		// Devolvemos el mensaje codificado en binario según el formato acordado
		ByteBuffer buf = ByteBuffer.allocate(2);
		byte opcode = 3;
		buf.put(opcode);
		buf.put((byte)protocol);
		byte[] message = buf.array();
		return message;
	}

	// Método para obtener la dirección a partir del mensaje UDP de respuesta
	private InetSocketAddress getAddressFromResponse(DatagramPacket packet) throws UnknownHostException {
		// Analizamos si la respuesta no contiene dirección (devolver null)
		ByteBuffer ret = ByteBuffer.wrap(packet.getData());
		int opcode = ret.get();
		if(opcode == 5) return null;
		if(opcode == 4) {
			int portRecibido = ret.getInt();
			String IP = new String();
			for (int i = 0; i < 3; i++) {
				IP = IP.concat((((Integer)(int)ret.get()).toString()));
				IP = IP.concat(".");
			}
			IP = IP.concat((((Integer)(int)ret.get()).toString()));
			System.out.println("Puerto del servidor que consulto :"+ portRecibido);
			System.out.println("Direccion IP del servidor que consulto : "+IP.toString());
			InetSocketAddress direccionFinal = new InetSocketAddress(IP,portRecibido); 
			return direccionFinal;
		}else return null;
	}

	public void close() {
		socket.close();
	}
}
