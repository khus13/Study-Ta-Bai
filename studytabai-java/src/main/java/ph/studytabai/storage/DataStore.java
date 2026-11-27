package ph.studytabai.storage;

import ph.studytabai.model.Identifiable;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Predicate;

/**
 * Generic file-backed repository using Java serialization.
 * Writes go to a temp file first, then replace the real file, so a crash can't corrupt data.
 * Tip: if you mutate an object you got from the store, call save(obj) again to persist the change.
 */
public final class DataStore<T extends Identifiable & Serializable> {
    private final Path file;
    private final Class<T> type;
    private final Map<String, T> items = new LinkedHashMap<>();

    public DataStore(Path file, Class<T> type) {
        this.file = file;
        this.type = type;
        load();
    }

    public synchronized void save(T item) {
        items.put(item.getId(), item);
        flush();
    }

    public synchronized Optional<T> findById(String id) { return Optional.ofNullable(items.get(id)); }

    public synchronized List<T> findAll() { return new ArrayList<>(items.values()); }

    public synchronized List<T> find(Predicate<T> filter) { return items.values().stream().filter(filter).toList(); }

    public synchronized boolean delete(String id) {
        boolean removed = items.remove(id) != null;
        if (removed) flush();
        return removed;
    }

    public synchronized int count() { return items.size(); }

    private void load() {
        if (!Files.exists(file)) return;
        try (InputStream in = new BufferedInputStream(Files.newInputStream(file))) {
            Object o = SafeDeserializer.read(in);
            if (!(o instanceof Map<?, ?> map)) throw new StorageException("Unexpected data format in " + file);
            for (Object v : map.values()) {
                if (!type.isInstance(v)) throw new StorageException("Unexpected object type in " + file);
                T t = type.cast(v);
                items.put(t.getId(), t);
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new StorageException("Could not read " + file + ": " + e.getMessage(), e);
        }
    }

    private void flush() {
        try {
            Path dir = file.toAbsolutePath().getParent();
            if (dir != null) Files.createDirectories(dir);
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            try (ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(tmp)))) {
                out.writeObject(new LinkedHashMap<>(items));
            }
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new StorageException("Could not write " + file + ": " + e.getMessage(), e);
        }
    }
}
