package homework;

import java.util.*;

public class RecordingState {
    private final String actor;
    private final String left;
    private final String right;

    public RecordingState(final String actor, final String left, final String right) {
        this.actor = actor;
        this.left = left;
        this.right = right;
    }

    private final Map<String, Queue<Object>> recordedChannelEvents = new HashMap<>();
    private final Map<String, Status> inboundStatus = new HashMap<>();
    private int tokenXState;
    private int tokenYState;
    private boolean isRecording;

    public void initSnapshot(final int x, final int y) {
        isRecording = true;
        setTokenState(x, y);
        inboundStatus.put(left, Status.OPEN);
        inboundStatus.put(right, Status.OPEN);
        recordedChannelEvents.put(left, new LinkedList<>());
        recordedChannelEvents.put(right, new LinkedList<>());
    }

    // if we get a MARKER and not recording
    public void initSnapshot(final int x, final int y, final String closedChannel) {
        initSnapshot(x,y);
        inboundStatus.put(closedChannel, Status.CLOSED);
    }

    public void handleTokenEvent(final String sender, final Object token) {
        if (inboundStatus.get(sender) == Status.OPEN) {
            recordedChannelEvents.get(sender).add(token);
        }
    }

    public void handleMarkerEvent(final String senderChannel) {
        Status status = inboundStatus.get(senderChannel);
        if (status==Status.OPEN) inboundStatus.put(senderChannel,Status.CLOSED);
        recordingCompleteCheck();
    }

    private void recordingCompleteCheck() {
        if (inboundStatus.get(left) == Status.CLOSED && inboundStatus.get(right) == Status.CLOSED) {
            System.out.printf(
                    "%s SNAPSHOT RESULTS: Token X : %d --- Token Y : %d ---" +
                            "In flight events left : %s --- In flight events right : %s%n ",
                    actor, tokenXState, tokenYState, recordedChannelEvents.get(left), recordedChannelEvents.get(right)
            );
            isRecording = false;
            clearState();
        }
    }

    private void clearState() {
        recordedChannelEvents.clear();
        inboundStatus.clear();
        tokenXState = 0;
        tokenYState = 0;
    }


    public boolean isRecording() {
        return isRecording;
    }

    private void setTokenState(final int x, final int y) {
        this.tokenXState = x;
        this.tokenYState = y;
    }

    enum Status {
        OPEN,
        CLOSED
    }
}
