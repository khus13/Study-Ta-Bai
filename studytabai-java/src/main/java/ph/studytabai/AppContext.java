package ph.studytabai;

import ph.studytabai.ai.AiClient;
import ph.studytabai.ai.AiException;
import ph.studytabai.ai.AiRateLimiter;
import ph.studytabai.ai.AnthropicClient;
import ph.studytabai.model.*;
import ph.studytabai.service.AuthService;
import ph.studytabai.service.StandardsSeed;
import ph.studytabai.session.Session;
import ph.studytabai.storage.DataStore;

import java.nio.file.Path;

/** Wires together the persistent stores and shared services. */
public final class AppContext {
    public final Path dataDir;
    public final DataStore<Teacher> teachers;
    public final DataStore<Document> documents;
    public final DataStore<Activity> activities;
    public final DataStore<Standard> standards;
    public final DataStore<AiResource> resources;
    public final DataStore<Session> sessions;
    public final DataStore<HistoryEntry> history;
    public final AuthService auth;
    private final AiRateLimiter aiLimiter = new AiRateLimiter(10, 60_000);
    private volatile AiClient aiOverride;

    public AppContext(Path dataDir) {
        this.dataDir = dataDir;
        teachers = new DataStore<>(dataDir.resolve("teachers.ser"), Teacher.class);
        documents = new DataStore<>(dataDir.resolve("documents.ser"), Document.class);
        activities = new DataStore<>(dataDir.resolve("activities.ser"), Activity.class);
        standards = new DataStore<>(dataDir.resolve("standards.ser"), Standard.class);
        resources = new DataStore<>(dataDir.resolve("ai_resources.ser"), AiResource.class);
        sessions = new DataStore<>(dataDir.resolve("sessions.ser"), Session.class);
        history = new DataStore<>(dataDir.resolve("student-history.ser"), HistoryEntry.class);
        auth = new AuthService(teachers);
        if (standards.count() == 0) StandardsSeed.samples().forEach(standards::save);
    }

    /** Rate-limited AI client. Tests can inject a fake with setAiClient. */
    public AiClient aiClient() throws AiException {
        aiLimiter.check();
        AiClient c = aiOverride;
        return c != null ? c : AnthropicClient.fromEnv();
    }

    public void setAiClient(AiClient c) { this.aiOverride = c; }
}
