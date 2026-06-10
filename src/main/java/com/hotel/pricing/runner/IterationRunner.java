package com.hotel.pricing.runner;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.hotel.pricing.config.PromptConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Drives the four ADD 3.0 iterations by sending structured prompts to the
 * Architect Agent (Supervisor). Each iteration's output is logged via
 * {@link ConversationLogger} for the required deliverable.
 * <p>
 * Enabled when {@code hotelpricing.run-iterations=true}.
 * <p>
 * Each iteration runs sequentially because later iterations depend on
 * design decisions from earlier ones. The Architect Agent maintains
 * state via {@code MemorySaver} across calls within the same JVM session.
 */
@Component
@Order(1)
@ConditionalOnProperty(name = "hotelpricing.run-iterations", havingValue = "true")
public class IterationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(IterationRunner.class);

    private final ReactAgent architectAgent;

    public IterationRunner(@Qualifier("architectAgent") ReactAgent architectAgent) {
        this.architectAgent = architectAgent;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String outputBase = "src/main/resources/output";

        try (ConversationLogger logger = new ConversationLogger(outputBase)) {

            log.info("══════ Starting ADD 3.0 — 4 Iterations ══════");

            // ================================================================
            // ITERATION 1: Establishing an Overall System Structure
            // ================================================================
            runSingleIteration(logger, 1,
                    PromptConstants.ITERATION_1_INPUT,
                    "Establishing an Overall System Structure");

            // ================================================================
            // ITERATION 2: Identifying Structures to Support Primary Functionality
            // ================================================================
            runSingleIteration(logger, 2,
                    PromptConstants.ITERATION_2_INPUT,
                    "Identifying Structures to Support Primary Functionality");

            // ================================================================
            // ITERATION 3: Addressing Reliability and Availability
            // ================================================================
            runSingleIteration(logger, 3,
                    PromptConstants.ITERATION_3_INPUT,
                    "Addressing Reliability and Availability Quality Attributes");

            // ================================================================
            // ITERATION 4: Addressing Development and Operations
            // ================================================================
            runSingleIteration(logger, 4,
                    PromptConstants.ITERATION_4_INPUT,
                    "Addressing Development and Operations");

            log.info("══════ All 4 iterations completed ══════");
            log.info("Output written to: {}", logger.getOutputDir().toAbsolutePath());
            log.info("Total token consumption: {} K tokens", String.format("%.1f", logger.getTotalTokensK()));
            log.info("Human interaction turns: {}", logger.getHumanTurnCount());
        }
    }

    /**
     * Runs one ADD iteration by sending the iteration prompt to the Architect Agent.
     * The Architect Agent may internally call sub-agents (Analyst, Quality, Reviewer)
     * multiple times. All interactions are captured in the conversation log.
     */
    private void runSingleIteration(ConversationLogger logger,
                                     int iterationNum,
                                     String iterationPrompt,
                                     String goal) {
        logger.beginIteration(iterationNum, goal);
        logger.recordHumanInput(iterationPrompt);

        log.info("Iteration {}: sending prompt to ArchitectAgent...", iterationNum);
        long startMs = System.currentTimeMillis();

        try {
            AssistantMessage response = architectAgent.call(new UserMessage(iterationPrompt));
            long elapsed = System.currentTimeMillis() - startMs;

            logger.recordAgentCall("architect_agent", iterationPrompt, response);
            logger.endIteration(iterationNum);

            log.info("Iteration {} completed in {} ms. Response length: {} chars",
                    iterationNum, elapsed,
                    response != null ? response.getText().length() : 0);
        } catch (Exception e) {
            log.error("Iteration {} FAILED: {}", iterationNum, e.getMessage(), e);
            logger.endIteration(iterationNum);
            // Continue with next iteration even if this one fails
        }
    }
}
