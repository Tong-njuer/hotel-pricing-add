package com.hotel.pricing.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Captures a full conversation log of the multi-agent ADD 3.0 interaction,
 * including timestamps, agent inputs/outputs, and extracted Mermaid diagrams.
 * <p>
 * Output is written to {@code src/main/resources/output/} with a timestamped
 * session ID so each run produces a distinct log.
 */
public class ConversationLogger implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(ConversationLogger.class);
    private static final DateTimeFormatter ISO_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
                    .withZone(ZoneId.of("Asia/Shanghai"));

    private final Path outputDir;
    private final String sessionId;
    private final long sessionStartEpochMs;
    private final StringBuilder conversationLog;
    private int humanTurnCount;

    public ConversationLogger(String baseDir) throws IOException {
        this.sessionStartEpochMs = System.currentTimeMillis();
        this.sessionId = "session_" + sessionStartEpochMs;
        this.outputDir = Paths.get(baseDir, sessionId);
        Files.createDirectories(outputDir);
        Files.createDirectories(outputDir.resolve("diagrams"));
        this.conversationLog = new StringBuilder();
        this.humanTurnCount = 0;

        writeHeader();
    }

    // ---------- header / footer ----------

    private void writeHeader() {
        String header = """
                ================================================================================
                ADD 3.0 Multi-Agent Conversation Log
                Session ID : %s
                Started    : %s
                Model      : GPT-5.4
                Paradigm   : Multi-Agent (Option 3 — Supervisor + Sub-Agent collaborative verification)
                Framework  : Spring AI Alibaba Agent Framework
                ================================================================================
                """.formatted(sessionId, now());
        conversationLog.append(header).append("\n");
    }

    private void writeFooter(long elapsedMs, double totalTokensK, int humanTurns) {
        String footer = """

                ================================================================================
                SESSION SUMMARY
                Ended           : %s
                Duration        : %d min %d sec
                Human Turns     : %d
                Token Consumed  : %.1f K tokens
                ================================================================================
                """.formatted(now(),
                elapsedMs / 60000, (elapsedMs % 60000) / 1000,
                humanTurns, totalTokensK);
        conversationLog.append(footer);
    }

    // ---------- iteration ----------

    public void beginIteration(int num, String goal) {
        humanTurnCount++;
        String block = """

                %s
                >>> ITERATION %d BEGIN <<<
                Goal: %s
                %s
                """.formatted("#".repeat(70), num, goal, "#".repeat(70));
        conversationLog.append(block);
        log.info("=== Iteration {} BEGIN ===", num);
    }

    public void endIteration(int num) {
        String block = """
                <<< ITERATION %d END >>>
                """.formatted(num);
        conversationLog.append(block);
        log.info("=== Iteration {} END ===", num);
    }

    // ---------- agent call ----------

    public void recordHumanInput(String input) {
        humanTurnCount++;
        conversationLog.append("\n--- [").append(now()).append("] HUMAN_INPUT ---\n");
        conversationLog.append(input).append("\n");
        conversationLog.append("--- END HUMAN_INPUT ---\n");
    }

    public void recordAgentCall(String agentName, String input, AssistantMessage output) {
        conversationLog.append("\n--- [").append(now()).append("] AGENT: ")
                .append(agentName).append(" ---\n");
        conversationLog.append("INPUT:\n").append(truncate(input, 4000)).append("\n\n");
        String outputText = output != null ? output.getText() : "<null>";
        conversationLog.append("OUTPUT:\n").append(truncate(outputText, 12000)).append("\n");
        conversationLog.append("--- END ").append(agentName).append(" ---\n");

        // Extract Mermaid diagrams from the output
        if (outputText != null) {
            extractAndSaveMermaid(agentName, outputText);
        }
    }

    public void recordSubAgentCall(String callerName, String subAgentName,
                                    String instruction, AssistantMessage response) {
        conversationLog.append("\n  [").append(now()).append("] SUB-AGENT CALL: ")
                .append(callerName).append(" → ").append(subAgentName).append("\n");
        conversationLog.append("  INSTRUCTION: ").append(truncate(instruction, 3000)).append("\n");
        String respText = response != null ? response.getText() : "<null>";
        conversationLog.append("  RESPONSE: ").append(truncate(respText, 8000)).append("\n");
    }

    // ---------- Mermaid extraction ----------

    private static final Pattern MERMAID_BLOCK =
            Pattern.compile("```mermaid\\s*\\n([\\s\\S]*?)```", Pattern.MULTILINE);

    private void extractAndSaveMermaid(String agentName, String text) {
        Matcher m = MERMAID_BLOCK.matcher(text);
        int count = 0;
        while (m.find()) {
            count++;
            String diagram = m.group(1).strip();
            String filename = sanitize(agentName) + "_" + System.currentTimeMillis()
                    + "_" + count + ".mermaid";
            try {
                Files.writeString(outputDir.resolve("diagrams").resolve(filename),
                        diagram, StandardOpenOption.CREATE_NEW);
                log.info("Saved Mermaid diagram: {}", filename);
            } catch (IOException e) {
                log.warn("Failed to save Mermaid diagram {}: {}", filename, e.getMessage());
            }
        }
        if (count > 0) {
            conversationLog.append("  [Extracted ").append(count)
                    .append(" Mermaid diagram(s)]\n");
        }
    }

    // ---------- cost tracking ----------

    private long totalPromptTokens = 0;
    private long totalCompletionTokens = 0;

    public void recordTokenUsage(long promptTokens, long completionTokens) {
        this.totalPromptTokens += promptTokens;
        this.totalCompletionTokens += completionTokens;
    }

    public double getTotalTokensK() {
        return (totalPromptTokens + totalCompletionTokens) / 1000.0;
    }

    public int getHumanTurnCount() {
        return humanTurnCount;
    }

    // ---------- finalize ----------

    @Override
    public void close() {
        long elapsed = System.currentTimeMillis() - sessionStartEpochMs;
        writeFooter(elapsed, getTotalTokensK(), humanTurnCount);

        Path logFile = outputDir.resolve("conversation.log");
        try {
            Files.writeString(logFile, conversationLog.toString(),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            log.info("Conversation log written to: {}", logFile.toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to write conversation log: {}", e.getMessage());
        }
    }

    public Path getOutputDir() {
        return outputDir;
    }

    // ---------- helpers ----------

    private String now() {
        return ISO_FMT.format(Instant.now());
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "<null>";
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen) + "\n... [truncated, original length=" + s.length() + "]";
    }

    private static String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9_-]", "_").replaceAll("_+", "_");
    }
}
