package ph.studytabai.ai;

public class AiException extends Exception {
    private static final long serialVersionUID = 1L;
    public AiException(String message) { super(message); }
    public AiException(String message, Throwable cause) { super(message, cause); }
}
