import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

class Jogo extends UnicastRemoteObject implements JogoInterface {
  public static final int port = 52369;
  public static int numPlayers;
  public static int maxPlays = 10;

  public boolean started = false;
  public int[] ids;
  public int[] plays;
  public String[] hostnames;
  public JogadorInterface[] jogadores;
  public boolean[] finished;
  public int[] points;

  public Jogo() throws RemoteException {
    System.out.println("Created instance of Jogo");
    hostnames = new String[numPlayers];
    ids = new int[numPlayers];
    plays = new int[numPlayers];
    jogadores = new JogadorInterface[numPlayers];
    finished = new boolean[numPlayers];
    points = new int[numPlayers];
    for (int i = 0; i < numPlayers; i++) {
      ids[i] = -1;
      finished[i] = false;
      points[i] = 0;
    }
  }

  public int getDisponibleId() {
    for (int i = 0; i < ids.length; i++) {
      if (ids[i] == -1) {
        return i;
      }
    }
    return -1;
  }

  public static void main(String[] args) {
    if (args.length != 2) {
      System.out.println("Usage: java Jogo <server ip> <num players>");
      System.exit(1);
    }

    numPlayers = Integer.parseInt(args[1]);

    try {
      System.setProperty("java.rmi.server.hostname", args[0]);
      LocateRegistry.createRegistry(port);
      System.out.println("java RMI registry created.");
    } catch (RemoteException e) {
      System.out.println("java RMI registry already exists.");
    }

    try {
      Jogo jogo = new Jogo();
      String server = "rmi://" + args[0] + ":" + port + "/jogo";
      Naming.rebind(server, jogo);
      System.out.println("Server is ready at: " + server);

      long verifiedAt = 0;

      while (true) {
        if (jogo.getDisponibleId() == -1 && !jogo.started) {
          jogo.started = true;
          for (int i = 0; i < jogo.ids.length; i++) {
            if (jogo.ids[i] == -1)
              continue;
            try {
              jogo.jogadores[i].inicia();
              System.out.println("Player init, id=" + i);
            } catch (Exception e) {
              jogo.ids[i] = -1;
              System.out.println("Player disconnected, id=" + i);
            }
          }
        }

        if (jogo.started) {
          boolean allPlayersFinished = true;
          for (int i = 0; i < jogo.ids.length; i++) {
            if (jogo.ids[i] == -1) continue;
            allPlayersFinished = allPlayersFinished && jogo.finished[i];
          }
          if (allPlayersFinished) {
            for (int i = 0; i < jogo.ids.length; i++) {
              if (jogo.ids[i] == -1) continue;
              System.out.println("Player #" + i + " have " + jogo.points[i] + " points");
              jogo.ids[i] = -1;
            }
            System.out.println("Finished game, waiting players to start new game");
            jogo.started = false;
          }
        }

        if (System.currentTimeMillis() - verifiedAt >= 5000) {
          for (int i = 0; i < jogo.ids.length; i++) {
            if (jogo.ids[i] == -1)
              continue;
            try {
              jogo.jogadores[i].verifica();
              System.out.println("Player verified, id=" + i);
            } catch (Exception e) {
              jogo.ids[i] = -1;
              System.out.println("Player disconnected, id=" + i);
            }
          }
          verifiedAt = System.currentTimeMillis();
        }

        Thread.sleep(1);
      }
    } catch (Exception e) {
      System.out.println("Serverfailed: " + e);
    }
  }

  public int registra() throws RemoteException {
    int id = getDisponibleId();
    if (id == -1 || started) {
      return -1;
    }
    try {
      String hostname = getClientHost();
      String connectLocation = "rmi://" + hostname + ":" + port + "/jogador";
      System.out.println("Connecting to player at : " + connectLocation);
      JogadorInterface jogador = (JogadorInterface) Naming.lookup(connectLocation);
      ids[id] = id;
      hostnames[id] = hostname;
      jogadores[id] = jogador;
      plays[id] = 0;
      finished[id] = false;
      points[id] = 0;
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
    if (Math.random() >= 0.07) {
      jogadores[id].bonifica();
      points[id] += 1;
      System.out.println("Player #" + id + " received bonification points=" + points[id]);
    }
    return plays[id];
  }

  public int desiste(int id) throws RemoteException {
    if (!started || ids[id] == -1 || finished[id]) {
      return -1;
    }
    finished[id] = true;
    return 0;
  }

  public int finaliza(int id) throws RemoteException {
    if (!started || ids[id] == -1 || plays[id] < maxPlays) {
      return -1;
    }
    finished[id] = true;
    return 0;
  }
}
