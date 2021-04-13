import java.net.MalformedURLException;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

class Jogo extends UnicastRemoteObject implements JogoInterface {
  public static final int port = 52369;
  public static String serverIp;
  public static int numPlayers;
  public static int maxPlays;
  public static Jogo jogo;
  public static long verifiedAt = 0;

  public boolean started = false;

  public int[] ids;
  public int[] plays;
  public String[] hostnames;
  public JogadorInterface[] jogadores;
  public boolean[] finished;
  public int[] points;
  public boolean[] connected;

  public static void main(String[] args) {
    if (args.length != 3) {
      System.out.println("Usage: java Jogo <server ip> <num players> <play amount>");
      System.exit(1);
    }

    serverIp = args[0];
    numPlayers = Integer.parseInt(args[1]);
    maxPlays = Integer.parseInt(args[2]);

    try {
      System.setProperty("java.rmi.server.hostname", serverIp);
      LocateRegistry.createRegistry(port);
      System.out.println("java RMI registry created.");
    } catch (RemoteException e) {
      System.out.println("java RMI registry already exists.");
    }

    try {
      jogo = new Jogo();
      String serverHost = "rmi://" + serverIp + ":" + port + "/jogo";
      Naming.rebind(serverHost, jogo);
      System.out.println("Server is ready at: " + serverHost);

      while (true) {

        // inicia jogo automaticamente quando as vagas estiverem preenchidas
        if (!jogo.canJoin() && !jogo.started) {
          jogo.startGame();
        }

        // finaliza o jogo caso todos os jogadores ja tenham finalizado
        if (jogo.started && jogo.isAllPlayersFinished()) {
          jogo.endGame();
        }

        // verifica os jogadores a cada 5 segundos
        if (System.currentTimeMillis() - verifiedAt >= 5000) {
          jogo.verifyAllPlayers();
          verifiedAt = System.currentTimeMillis();
        }

        // sleep para liberar a thread
        Thread.sleep(1);
      }

    } catch (Exception e) {
      System.out.println("Serverfailed: " + e);
      System.exit(-1);
    }
  }

  public Jogo() throws RemoteException {
    removeAllPlayers();
  }

  private int getDisponibleId() {
    for (int i = 0; i < ids.length; i++) {
      if (ids[i] == -1) {
        return i;
      }
    }
    return -1;
  }

  private boolean canJoin() {
    return !started && getDisponibleId() >= 0;
  }

  private void addPlayer(int id, String hostname) {
    ids[id] = id;
    hostnames[id] = hostname;
    plays[id] = 0;
    finished[id] = false;
    points[id] = 0;
  }

  private void removePlayer(int id) {
    ids[id] = -1;
    finished[id] = false;
    points[id] = 0;
    jogadores[id] = null;
    hostnames[id] = null;
  }

  private void removeAllPlayers() {
    hostnames = new String[numPlayers];
    ids = new int[numPlayers];
    plays = new int[numPlayers];
    jogadores = new JogadorInterface[numPlayers];
    finished = new boolean[numPlayers];
    points = new int[numPlayers];
    for (int id = 0; id < numPlayers; id++) {
      removePlayer(id);
    }
  }

  private void startGame() {
    started = true;
    for (int i = 0; i < ids.length; i++) {
      if (ids[i] == -1)
        continue;
      try {
        makeConnection(i);
        jogadores[i].inicia();
        System.out.println("Player init, id=" + i);
      } catch (Exception e) {
        ids[i] = -1;
        System.out.println("Player disconnected, id=" + i);
      }
    }
  }

  private boolean isAllPlayersFinished() {
    boolean allPlayersFinished = true;
    for (int i = 0; i < ids.length; i++) {
      if (ids[i] == -1)
        continue;
      allPlayersFinished = allPlayersFinished && finished[i];
    }
    return allPlayersFinished;
  }

  private void endGame() {
    if (isAllPlayersFinished()) {
      System.out.println("");
      for (int i = 0; i < ids.length; i++) {
        if (ids[i] == -1)
          continue;
        System.out.println("Player #" + i + " have " + points[i] + " points");
        ids[i] = -1;
      }
      System.out.println("\n###############################################################");
      System.out.println("###### Finished game, waiting players to start new game #######");
      System.out.println("###############################################################\n");
      started = false;
    }
  }

  private void verifyAllPlayers() {
    for (int i = 0; i < ids.length; i++) {
      if (ids[i] == -1)
        continue;
      try {
        makeConnection(i);
        jogadores[i].verifica();
        System.out.println("Player verified, id=" + i);
      } catch (Exception e) {
        ids[i] = -1;
        System.out.println("Player disconnected, id=" + i + " reason=" + e);
      }
    }
  }

  private void makeConnection(int id) throws RemoteException, MalformedURLException, NotBoundException {
    String connectLocation = "rmi://" + hostnames[id] + ":" + port + "/jogador";
    System.out.println("Connecting to player at : " + connectLocation);
    JogadorInterface jogador = (JogadorInterface) Naming.lookup(connectLocation);
    jogadores[id] = jogador;
  }

  public int registra(String hostname) throws RemoteException {
    if (!canJoin()) {
      return -1;
    }
    int id = getDisponibleId();
    try {
      addPlayer(id, hostname);
      makeConnection(id);
      System.out.println("Receive register from " + hostname + ", id=" + id);
      return id;
    } catch (Exception e) {
      System.out.println("Failed to register player: " + e);
      return -1;
    }
  }

  public int joga(int id) throws RemoteException {
    System.out.println("Receive (joga) from player #" + id);
    if (!started || ids[id] == -1 || plays[id] >= maxPlays || finished[id])
      return -1;
    plays[id] += 1;
    System.out.println("Player #" + id + " played " + plays[id] + " times");
    if (Math.random() > 0.5) {
      try {
        makeConnection(id);
        jogadores[id].bonifica();
        points[id] += 1;
        System.out.println("Player #" + id + " received bonification points=" + points[id]);
      } catch (Exception e) {
        System.out.println("Failed to give bonification to player #" + id + ", reason: " + e);
      }
    }
    return plays[id];
  }

  public int desiste(int id) throws RemoteException {
    if (!started || ids[id] == -1 || finished[id]) {
      return -1;
    }
    removePlayer(id);
    return 0;
  }

  public int finaliza(int id) throws RemoteException {
    if (!started || ids[id] == -1 || plays[id] < maxPlays || finished[id]) {
      return -1;
    }
    finished[id] = true;
    return 0;
  }
}
