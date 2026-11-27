package ph.studytabai;

import java.nio.file.Path;

import ph.studytabai.model.Activity;
import ph.studytabai.model.Document;
import ph.studytabai.model.HistoryEntry;
import ph.studytabai.model.Standard;
import ph.studytabai.model.Teacher;
import ph.studytabai.service.AuthService;
import ph.studytabai.service.StandardsSeed;
import ph.studytabai.session.Session;
import ph.studytabai.storage.DataStore;

/** Wires together the persistent stores and shared services. */
public final class AppContext {
    public final Path dataDir;
    public final DataStore<Teacher> teachers;
    public final DataStore<Document> documents;
    public final DataStore<Activity> activities;
    public final DataStore<Standard> standards;
    public final DataStore<Session> sessions;
    public final DataStore<HistoryEntry> history;
    public final AuthService auth;

    public AppContext(Path dataDir) {
        this.dataDir = dataDir;
        teachers = new DataStore<>(dataDir.resolve("teachers.ser"), Teacher.class);
        documents = new DataStore<>(dataDir.resolve("documents.ser"), Document.class);
        activities = new DataStore<>(dataDir.resolve("activities.ser"), Activity.class);
        standards = new DataStore<>(dataDir.resolve("standards.ser"), Standard.class);
        sessions = new DataStore<>(dataDir.resolve("sessions.ser"), Session.class);
        history = new DataStore<>(dataDir.resolve("student-history.ser"), HistoryEntry.class);
        auth = new AuthService(teachers);
        if (standards.count() == 0) StandardsSeed.samples().forEach(standards::save);
    }
}