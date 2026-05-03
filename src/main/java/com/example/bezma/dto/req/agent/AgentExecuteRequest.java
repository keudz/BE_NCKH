package com.example.bezma.dto.req.agent;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentExecuteRequest {

    @NotBlank(message = "Prompt cannot be blank")
    private String prompt;
    
    private java.util.List<java.util.Map<String, String>> history;
    
    // Optional session ID if conversation context is required later
    private String sessionId;
}
