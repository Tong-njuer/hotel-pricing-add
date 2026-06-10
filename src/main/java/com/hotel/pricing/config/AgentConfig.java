package com.hotel.pricing.config;

import com.alibaba.cloud.ai.graph.agent.AgentTool;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the multi-agent system for the ADD 3.0 method.
 * <p>
 * Architecture: Supervisor pattern where the ArchitectAgent acts as
 * the supervisor and calls three specialized sub-agents as tools:
 * <ul>
 *   <li>AnalystAgent — driver decomposition and design alternatives</li>
 *   <li>QualityAgent — quality attribute verification</li>
 *   <li>ReviewerAgent — consistency and completeness review</li>
 * </ul>
 * Sub-agents are registered as tools on the supervisor via
 * {@link AgentTool#getFunctionToolCallback(ReactAgent)}.
 * Each sub-agent uses {@code inputType(String.class)} so the supervisor
 * passes a natural-language instruction string.
 */
@Configuration
public class AgentConfig {

    // ========================================================================
    // Shared infrastructure
    // ========================================================================

    @Bean
    public MemorySaver memorySaver() {
        return new MemorySaver();
    }

    // ========================================================================
    // Sub-Agent: Analyst — driver decomposition and design alternatives
    // ========================================================================

    @Bean
    public ReactAgent analystAgent(@Qualifier("openAiChatModel") ChatModel chatModel) {
        return ReactAgent.builder()
                .name("call_analyst")
                .description("""
                        Call the Analyst Agent when you need deeper analysis of
                        architecture drivers, decomposition of system elements
                        impacted by drivers, or generation of design alternatives.
                        Provide a specific question about which drivers to analyze
                        and what the current iteration goal is.
                        Input: a detailed analysis request with iteration context.
                        """)
                .systemPrompt(PromptConstants.PRIOR_KNOWLEDGE_ADD30
                        + "\n" + PromptConstants.PRIOR_KNOWLEDGE_CASE
                        + "\n" + PromptConstants.ANALYST_SYSTEM_PROMPT)
                .model(chatModel)
                .inputType(String.class)
                .build();
    }

    // ========================================================================
    // Sub-Agent: Quality — quality attribute verification
    // ========================================================================

    @Bean
    public ReactAgent qualityAgent(@Qualifier("openAiChatModel") ChatModel chatModel) {
        return ReactAgent.builder()
                .name("call_quality")
                .description("""
                        Call the Quality Agent to verify that the current design
                        satisfies all Quality Attributes (QA-1 through QA-9).
                        Provide the design output to be verified along with the
                        iteration goal.
                        Input: the design description and diagrams to verify.
                        """)
                .systemPrompt(PromptConstants.PRIOR_KNOWLEDGE_ADD30
                        + "\n" + PromptConstants.PRIOR_KNOWLEDGE_CASE
                        + "\n" + PromptConstants.QUALITY_SYSTEM_PROMPT)
                .model(chatModel)
                .inputType(String.class)
                .build();
    }

    // ========================================================================
    // Sub-Agent: Reviewer — consistency and completeness review
    // ========================================================================

    @Bean
    public ReactAgent reviewerAgent(@Qualifier("openAiChatModel") ChatModel chatModel) {
        return ReactAgent.builder()
                .name("call_reviewer")
                .description("""
                        Call the Reviewer Agent to cross-check the design for
                        consistency, completeness, constraint compliance, and
                        technical debt risk. Also validates Mermaid syntax.
                        Provide the full design output including all diagrams.
                        Input: the complete design output to review.
                        """)
                .systemPrompt(PromptConstants.PRIOR_KNOWLEDGE_ADD30
                        + "\n" + PromptConstants.PRIOR_KNOWLEDGE_CASE
                        + "\n" + PromptConstants.REVIEWER_SYSTEM_PROMPT)
                .model(chatModel)
                .inputType(String.class)
                .build();
    }

    // ========================================================================
    // Supervisor: Architect Agent — orchestrates the ADD 3.0 process
    // ========================================================================

    @Bean
    public ReactAgent architectAgent(@Qualifier("openAiChatModel") ChatModel chatModel,
                                      ReactAgent analystAgent,
                                      ReactAgent qualityAgent,
                                      ReactAgent reviewerAgent,
                                      MemorySaver memorySaver) {
        return ReactAgent.builder()
                .name("architect_agent")
                .systemPrompt(PromptConstants.PRIOR_KNOWLEDGE_ADD30
                        + "\n" + PromptConstants.PRIOR_KNOWLEDGE_CASE
                        + "\n" + PromptConstants.ITERATION_PLAN
                        + "\n" + PromptConstants.ARCHITECT_SYSTEM_PROMPT
                        + "\n" + PromptConstants.ARCHITECT_DIALOGUE_RULES)
                .model(chatModel)
                .saver(memorySaver)
                .tools(
                        AgentTool.getFunctionToolCallback(analystAgent),
                        AgentTool.getFunctionToolCallback(qualityAgent),
                        AgentTool.getFunctionToolCallback(reviewerAgent))
                .build();
    }
}
