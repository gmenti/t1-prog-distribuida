import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ThreadLocalRandom;

class Jogador extends UnicastRemoteObject implements JogadorInterface {
	private static final int port = 52369;
	public boolean started = false;
	public int points = 0;
	public long verifiedAt;

	public Jogador() throws RemoteException {
	}

	public static void main(String[] args) {
		if (args.length != 2) {
			System.out.println("Usage: java Jogador <server ip> <client ip>");
			System.exit(1);
		}

		String serverIp = args[0];
		String clientIp = args[1];

		try {
			System.setProperty("java.rmi.server.hostname", serverIp);
			LocateRegistry.createRegistry(port);
			System.out.println("Java RMI registry created.");
		} catch (RemoteException e) {
			System.out.println("Java RMI registry already exists.");
		}

		String connectLocation = "rmi://" + serverIp + ":" + port + "/jogo";

		JogoInterface jogo = null;
		try {
			System.out.println("Connecting to server at: " + connectLocation);
			jogo = (JogoInterface) Naming.lookup(connectLocation);
		} catch (Exception e) {
			System.out.println("Connection failed: ");
			e.printStackTrace();
		}

		System.out.println("Connected to server");

		try {
			Jogador jogador = new Jogador();
			String client = "rmi://" + clientIp + ":52369/jogador";
			Naming.rebind(client, jogador);
			System.out.println("Player server is ready on " + client);

			int id = jogo.registra(clientIp);
			if (id == -1) {
				throw new Exception("Server is full, try connect later");
			}

			System.out.println("Registered to server, received id= " + id);

			while (true) {
			
				// executa jogadas caso a partida tenha iniciado
				if (jogador.started) {

					// trava execução durante 250ms à 950ms
					int randomTime = ThreadLocalRandom.current().nextInt(250, 950);
					Thread.sleep(randomTime);
				
					int played = jogo.joga(id);
					System.out.println("'Joga' executed id=" + id);

					// caso tenha atingido limite de jogadas, então finaliza a partida
					if (played == -1) {
						jogo.finaliza(id);
						jogador.started = false;
						System.out.println("'Finaliza' executed id=" + id);
					}
				}

				// sleep para liberar thread
				Thread.sleep(1);
			}

		} catch (Exception e) {
			System.out.println("Unexpected error ocurred: " + e);
			System.exit(-1);
		}
	}

	@Override
	public void inicia() throws RemoteException {
		if (started)
			return;
		started = true;
	}

	@Override
	public void bonifica() throws RemoteException {
		if (!started)
			return;
		points += 1;
	}

	@Override
	public void verifica() throws RemoteException {
		verifiedAt = System.currentTimeMillis();
	}
}
