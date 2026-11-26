package ph.studytabai.net;

import ph.studytabai.model.Answer;
import ph.studytabai.net.Messages.*;
import ph.studytabai.storage.SafeDeserializer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;

/** Student-side connection. Callbacks run on the network thread; the UI must hop to the Swing thread. */
public final class GameClient implements Closeable {
    public interface Listener {
        void joined(JoinResponse r);
        void state(StateUpdate s);
        void answerResult(AnswerResult r);
        void reaction(ReactionBroadcast r);
        void error(String message);
        void disconnected();
    }

    private final String host;
    private final int port;
    private final Listener listener;
    private Socket socket;
    private ObjectOutputStream out;
    private volatile boolean closed;

    public GameClient(String host, int port, Listener listener) {
        this.host = host;
        this.port = port;
        this.listener = listener;
    }

    public void connect(String code, String nickname, String participantId, String token) throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), 5000);
        out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        out.flush();
        ObjectInputStream in = SafeDeserializer.open(new BufferedInputStream(socket.getInputStream()));
        Thread t = new Thread(() -> readLoop(in), "stb-client");
        t.setDaemon(true);
        t.start();
        send(new JoinRequest(code, nickname, participantId, token));
    }

    private void readLoop(ObjectInputStream in) {
        try {
            while (!closed) {
                Object o = in.readObject();
                if (o instanceof JoinResponse r) listener.joined(r);
                else if (o instanceof StateUpdate s) listener.state(s);
                else if (o instanceof AnswerResult r) listener.answerResult(r);
                else if (o instanceof ReactionBroadcast r) listener.reaction(r);
                else if (o instanceof ErrorMsg e) listener.error(e.message);
            }
        } catch (IOException | ClassNotFoundException e) {
            // connection closed
        } finally {
            if (!closed) listener.disconnected();
        }
    }

    public synchronized void send(Msg m) throws IOException {
        out.writeObject(m);
        out.reset();
        out.flush();
    }

    public void submit(String questionId, Answer a) throws IOException { send(new AnswerSubmit(questionId, a)); }
    public void react(String emoji) throws IOException { send(new ReactionMsg(emoji)); }

    @Override
    public void close() {
        closed = true;
        try { if (out != null) send(new Leave()); } catch (IOException ignored) { }
        try { if (socket != null) socket.close(); } catch (IOException ignored) { }
    }
}
