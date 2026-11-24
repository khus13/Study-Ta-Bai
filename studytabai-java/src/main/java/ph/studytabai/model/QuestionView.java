package ph.studytabai.model;

import java.io.Serializable;
import java.util.List;

/**
 * What a student is allowed to see. Deliberately has NO field for correct answers,
 * so it is safe to send over the network before the question closes.
 */
@SuppressWarnings("serial")
public final class QuestionView implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String type;
    private final String prompt;
    private final List<String> options;
    private final int points;
    private final int timeLimitSec;
    private final String imagePath;
    private final byte[] image;   // optional picture, filled in by the server (never a teacher's file path on the student's disk)

    public QuestionView(String id, String type, String prompt, List<String> options,
                        int points, int timeLimitSec, String imagePath) {
        this(id, type, prompt, options, points, timeLimitSec, imagePath, null);
    }

    private QuestionView(String id, String type, String prompt, List<String> options,
                         int points, int timeLimitSec, String imagePath, byte[] image) {
        this.image = image;
        this.id = id;
        this.type = type;
        this.prompt = prompt;
        this.options = List.copyOf(options);
        this.points = points;
        this.timeLimitSec = timeLimitSec;
        this.imagePath = imagePath;
    }

    public String getId() { return id; }
    public String getType() { return type; }
    public String getPrompt() { return prompt; }
    public List<String> getOptions() { return options; }
    public int getPoints() { return points; }
    public int getTimeLimitSec() { return timeLimitSec; }
    public String getImagePath() { return imagePath; }
    public byte[] getImage() { return image == null ? null : image.clone(); }

    public QuestionView withImage(byte[] bytes) {
        return new QuestionView(id, type, prompt, options, points, timeLimitSec, imagePath, bytes == null ? null : bytes.clone());
    }
}
