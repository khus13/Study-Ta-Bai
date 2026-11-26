package ph.studytabai.net;

import ph.studytabai.model.*;
import ph.studytabai.net.Messages.*;
import ph.studytabai.session.*;
import ph.studytabai.storage.SafeDeserializer;

import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

/**
 * Hosts one session on the local network. One thread per student; all state lives in the Session
 * (the server validates every message). Correct answers are only revealed after a student answers.
 */
public final class GameServer implements Closeable {
    public interface Listener {
        void changed();
        void reaction(String nickname, String emoji);
    }

    private static final Set<String> EMOJI = Set.of("👏", "😂", "🔥", "🎉", "❤️", "😮");
    private static final int MAX_PLAYERS = 200;

    private final Session session;
    private final ServerSocket server;
    private final Set<Conn> conns = ConcurrentHashMap.newKeySet();
    private final Map<String, Deque<Long>> joinAttempts = new ConcurrentHashMap<>();
    private final ExecutorService pool = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "stb-net");
        t.setDaemon(true);
        return t;
    });
    private volatile Listener listener = new Listener() {
        @Override public void changed() { }
        @Override public void reaction(String n, String e) { }
    };
    private final Map<String, byte[]> imageCache = new ConcurrentHashMap<>();
    private volatile boolean closed;

    public GameServer(Session session, int port) throws IOException {
        this.session = session;
        this.server = new ServerSocket(port);
        pool.execute(this::acceptLoop);
    }

    public void setListener(Listener l) { this.listener = l; }
    public Session getSession() { return session; }
    public int getPort() { return server.getLocalPort(); }

    /** Best guess at the LAN address students should type. */
    public static String guessHostAddress() {
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;
                for (InetAddress a : Collections.list(ni.getInetAddresses()))
                    if (a instanceof Inet4Address && !a.isLoopbackAddress()) return a.getHostAddress();
            }
        } catch (SocketException ignored) { }
        return "127.0.0.1";
    }

    // ---- host controls (live mode) ----
    private LiveSession live() {
        if (session instanceof LiveSession l) return l;
        throw new IllegalStateException("Only live sessions have these controls.");
    }

    public void startGame() { live().start(); refresh(); }
    public void pauseGame() { live().pause(); refresh(); }
    public void resumeGame() { live().resume(); refresh(); }
    public void nextQuestion() { live().next(); refresh(); }

    public void endGame() { session.end(); refresh(); }

    /** Push fresh state to everyone and tell the host UI. */
    public void refresh() {
        for (Conn c : conns) if (c.pid != null) c.send(stateFor(c.pid));
        listener.changed();
    }

    @Override
    public void close() {
        closed = true;
        try { server.close(); } catch (IOException ignored) { }
        for (Conn c : conns) c.close();
        pool.shutdownNow();
    }

    // ---- networking ----
    private void acceptLoop() {
        while (!closed) {
            try {
                Socket s = server.accept();
                if (conns.size() >= MAX_PLAYERS + 20) { s.close(); continue; }
                pool.execute(() -> new Conn(s).run());
            } catch (IOException e) {
                if (closed) return;
            }
        }
    }

    private boolean allowJoin(String ip) {
        Deque<Long> d = joinAttempts.computeIfAbsent(ip, k -> new ArrayDeque<>());
        long now = System.currentTimeMillis();
        synchronized (d) {
            while (!d.isEmpty() && now - d.peekFirst() > 60_000) d.pollFirst();
            if (d.size() >= 20) return false;
            d.addLast(now);
            return true;
        }
    }

    private StateUpdate stateFor(String pid) {
        List<LeaderboardEntry> lb = session.leaderboard();
        if (lb.size() > 10) lb = new ArrayList<>(lb.subList(0, 10));
        Accommodation acc = session.accommodationFor(pid);
        int total = session.getQuestions().size();
        if (session instanceof LiveSession ls) {
            Question q = ls.currentQuestion();
            return new StateUpdate(ls.getStatus().name(), ls.getCurrentIndex(), total, ls.participantCount(),
                    q == null ? null : view(q), ls.remainingMs(pid),
                    q != null && ls.hasAnswered(pid, q.getId()), lb, acc, session.isMusicEnabled());
        }
        return new StateUpdate(session.getStatus().name(), -1, total, session.participantCount(), null, 0, false, lb, acc, false);
    }

    /** Student view plus the question's picture (read once from the teacher's disk, small PNG/JPG/GIF only). */
    private QuestionView view(Question q) {
        QuestionView v = q.toStudentView();
        String path = q.getImagePath();
        if (path == null || path.isBlank()) return v;
        byte[] img = imageCache.computeIfAbsent(path, GameServer::readImage);
        return img.length == 0 ? v : v.withImage(img);
    }

    private static byte[] readImage(String path) {
        try {
            String lower = path.toLowerCase(Locale.ROOT);
            if (!(lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".gif"))) return new byte[0];
            java.nio.file.Path p = java.nio.file.Path.of(path);
            if (!java.nio.file.Files.isRegularFile(p) || java.nio.file.Files.size(p) > 1_000_000) return new byte[0];
            return java.nio.file.Files.readAllBytes(p);
        } catch (IOException | RuntimeException e) {
            return new byte[0];
        }
    }

    private JoinResponse joinResponse(Participant p, String token) {
        List<QuestionView> views = null;
        List<Flashcard> cards = null;
        if (session instanceof SelfPacedSession) {
            if (session.getActivity() instanceof FlashcardSet fs) cards = fs.getCards();
            else views = session.getQuestions().stream().map(this::view).toList();
        }
        return new JoinResponse(true, null, p.getId(), token, session.getMode(), session.getTitle(), views, cards,
                session.accommodationFor(p.getId()));
    }

    private final class Conn {
        private final Socket socket;
        private ObjectOutputStream out;
        private volatile String pid;
        private String nickname;
        private long lastReaction;

        Conn(Socket s) { this.socket = s; }

        synchronized void send(Msg m) {
            try {
                out.writeObject(m);
                out.reset();          // do not cache object references across messages
                out.flush();
            } catch (IOException e) {
                close();
            }
        }

        void close() {
            conns.remove(this);
            try { socket.close(); } catch (IOException ignored) { }
        }

        void run() {
            try {
                out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                out.flush();
                ObjectInputStream in = SafeDeserializer.open(new BufferedInputStream(socket.getInputStream()));
                socket.setSoTimeout(15_000);   // must join quickly
                conns.add(this);
                while (!closed) {
                    Object o = in.readObject();
                    if (o instanceof JoinRequest j) handleJoin(j);
                    else if (pid == null) break;
                    else if (o instanceof AnswerSubmit a) handleAnswer(a);
                    else if (o instanceof ReactionMsg r) handleReaction(r);
                    else if (o instanceof Leave) break;
                    else break;
                }
            } catch (IOException | ClassNotFoundException e) {
                // disconnect or rejected data: just drop the connection
            } finally {
                close();
                listener.changed();
            }
        }

        private void handleJoin(JoinRequest j) throws IOException {
            String ip = socket.getInetAddress().getHostAddress();
            if (!allowJoin(ip)) { send(JoinResponse.fail("Too many attempts. Wait a minute and try again.")); return; }
            String given = j.code == null ? "" : j.code.trim().toUpperCase(Locale.ROOT);
            boolean codeOk = MessageDigest.isEqual(given.getBytes(), session.getJoinCode().getBytes());
            if (!codeOk) { send(JoinResponse.fail("Wrong join code.")); return; }
            if (session.getStatus() == SessionStatus.ENDED) { send(JoinResponse.fail("This session has ended.")); return; }
            try {
                Participant p;
                String token;
                if (j.participantId != null && j.token != null && (p = session.rejoin(j.participantId, j.token)) != null) {
                    token = j.token;
                } else {
                    if (session.participantCount() >= MAX_PLAYERS) { send(JoinResponse.fail("This session is full.")); return; }
                    Session.Joined joined = session.join(j.nickname);
                    p = joined.participant;
                    token = joined.token;
                }
                pid = p.getId();
                nickname = p.getNickname();
                socket.setSoTimeout(0);
                send(joinResponse(p, token));
                refresh();
            } catch (IllegalArgumentException | IllegalStateException e) {
                send(JoinResponse.fail(e.getMessage()));
            }
        }

        private void handleAnswer(AnswerSubmit a) {
            Question q = session.findQuestion(a.questionId);
            try {
                if (a.answer == null) throw new IllegalArgumentException("Missing answer.");
                AnswerRecord r = session.submit(pid, a.questionId, a.answer);
                send(new AnswerResult(a.questionId, true, r.isCorrect(), r.getPoints(), session.totalPoints(pid),
                        q.getCorrectAnswerText(), q.getExplanation(), null));
                if (session instanceof LiveSession) refresh();
                else { send(stateFor(pid)); listener.changed(); }
            } catch (IllegalArgumentException | IllegalStateException e) {
                send(new AnswerResult(a.questionId, false, false, 0, session.totalPoints(pid), null, null, e.getMessage()));
            }
        }

        private void handleReaction(ReactionMsg r) {
            long now = System.currentTimeMillis();
            if (!session.isReactionsEnabled() || !EMOJI.contains(r.emoji) || now - lastReaction < 1000) return;
            lastReaction = now;
            ReactionBroadcast b = new ReactionBroadcast(nickname, r.emoji);
            for (Conn c : conns) if (c.pid != null) c.send(b);
            listener.reaction(nickname, r.emoji);
        }
    }
}
