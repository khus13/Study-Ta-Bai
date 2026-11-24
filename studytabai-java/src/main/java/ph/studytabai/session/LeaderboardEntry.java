package ph.studytabai.session;

import java.io.Serializable;

public final class LeaderboardEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String nickname;
    private final int points;
    private final int answered;

    public LeaderboardEntry(String nickname, int points, int answered) {
        this.nickname = nickname;
        this.points = points;
        this.answered = answered;
    }

    public String getNickname() { return nickname; }
    public int getPoints() { return points; }
    public int getAnswered() { return answered; }
}
