package com.ecommerce.rag.rag.retriever;

import java.util.ArrayList;
import java.util.List;

import com.ecommerce.rag.models.dto.ChatCandidate;

public class NoMatchRecoveryResult {

    private boolean recovered;
    private String recoveryType;
    private List<ChatCandidate> relaxedCandidates = new ArrayList<>();
    private String userMessage;
    private List<String> relaxedConstraints = new ArrayList<>();

    public static NoMatchRecoveryResult notRecovered(String reason) {
        NoMatchRecoveryResult r = new NoMatchRecoveryResult();
        r.recovered = false;
        r.recoveryType = "NONE";
        r.userMessage = reason;
        return r;
    }

    public static NoMatchRecoveryResult recovered(String recoveryType,
                                                   List<ChatCandidate> relaxedCandidates,
                                                   String userMessage,
                                                   List<String> relaxedConstraints) {
        NoMatchRecoveryResult r = new NoMatchRecoveryResult();
        r.recovered = true;
        r.recoveryType = recoveryType;
        r.relaxedCandidates = relaxedCandidates != null ? relaxedCandidates : new ArrayList<>();
        r.userMessage = userMessage;
        r.relaxedConstraints = relaxedConstraints != null ? relaxedConstraints : new ArrayList<>();
        return r;
    }

    public boolean isRecovered() { return recovered; }
    public void setRecovered(boolean recovered) { this.recovered = recovered; }

    public String getRecoveryType() { return recoveryType; }
    public void setRecoveryType(String recoveryType) { this.recoveryType = recoveryType; }

    public List<ChatCandidate> getRelaxedCandidates() { return relaxedCandidates; }
    public void setRelaxedCandidates(List<ChatCandidate> relaxedCandidates) { this.relaxedCandidates = relaxedCandidates; }

    public String getUserMessage() { return userMessage; }
    public void setUserMessage(String userMessage) { this.userMessage = userMessage; }

    public List<String> getRelaxedConstraints() { return relaxedConstraints; }
    public void setRelaxedConstraints(List<String> relaxedConstraints) { this.relaxedConstraints = relaxedConstraints; }
}
