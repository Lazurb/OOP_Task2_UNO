package uno.model;

public enum GameDirection {
    По_часовой(1),
    Против_часовой(-1);

    private final int step;

    GameDirection(int step) {
        this.step = step;
    }

    public int getStep() {
        return step;
    }

    public GameDirection reverse() {
        return this == По_часовой ? Против_часовой : По_часовой;
    }
}