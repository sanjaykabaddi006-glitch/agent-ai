package com.knowledgehub.rag;

import java.util.ArrayList;
import java.util.List;

public class RagResponse {
    private String answer;
    private List<Citation> citations = new ArrayList<>();
    private boolean grounded;
    private long latencyMs;

    public RagResponse() {}

    public RagResponse(String answer, List<Citation> citations, boolean grounded, long latencyMs) {
        this.answer = answer;
        this.citations = citations != null ? citations : new ArrayList<>();
        this.grounded = grounded;
        this.latencyMs = latencyMs;
    }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public List<Citation> getCitations() { return citations; }
    public void setCitations(List<Citation> citations) { this.citations = citations; }

    public boolean isGrounded() { return grounded; }
    public void setGrounded(boolean grounded) { this.grounded = grounded; }

    public long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(long latencyMs) { this.latencyMs = latencyMs; }
}
