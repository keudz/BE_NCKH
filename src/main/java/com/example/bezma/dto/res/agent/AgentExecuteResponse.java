package com.example.bezma.dto.res.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentExecuteResponse {
    private List<String> agentsUsed;
    private String fullResponse;
    private String strategy;
    private String content;
    private String imagePrompt;
    private String imageUrl;
    private String generatedImageUrl;
    private String report;
    private String reportUrl;
}
