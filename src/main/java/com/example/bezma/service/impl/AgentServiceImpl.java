package com.example.bezma.service.impl;

import com.example.bezma.dto.req.agent.AgentExecuteRequest;
import com.example.bezma.dto.res.agent.AgentExecuteResponse;
import com.example.bezma.entity.user.User;
import com.example.bezma.exception.AppException;
import com.example.bezma.common.enumCom.ErrorCode;
import com.example.bezma.service.iService.IAgentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentServiceImpl implements IAgentService {

    private final RestTemplate restTemplate;
    
    @Value("${agent.service.url:http://localhost:8001}")
    private String agentServiceUrl;
    
    @Override
    public AgentExecuteResponse executePipeline(AgentExecuteRequest request, User user) {
        String url = agentServiceUrl + "/api/v1/agent/execute";
        
        Map<String, Object> body = Map.of(
            "prompt", request.getPrompt(),
            "tenant_id", (user != null && user.getTenant() != null) ? user.getTenant().getId() : 0,
            "user_id", (user != null && user.getId() != null) ? user.getId() : 0,
            "tenant_name", (user != null && user.getTenant() != null) ? user.getTenant().getName() : "Doanh nghiệp",
            "tenant_description", (user != null && user.getTenant() != null && user.getTenant().getDescription() != null) ? user.getTenant().getDescription() : ""
        );
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<>() {}
            );
            
            Map<String, Object> data = response.getBody();
            if (data == null) {
                throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
            }
            
            // Check agents_used which is a List
            List<String> agentsUsed = null;
            if (data.get("agents_used") instanceof List<?> rawList) {
                agentsUsed = rawList.stream().map(Object::toString).toList();
            }
            
            return AgentExecuteResponse.builder()
                .agentsUsed(agentsUsed)
                .fullResponse((String) data.get("full_response"))
                .strategy((String) data.get("strategy_agent"))
                .content((String) data.get("content_agent"))
                .imagePrompt((String) data.get("design_agent"))
                .imageUrl((String) data.get("generated_image_url"))
                .generatedImageUrl((String) data.get("generated_image_url"))
                .report((String) data.get("report_agent"))
                .reportUrl((String) data.get("report_url"))
                .build();
                
        } catch (Exception e) {
            log.error("Agent Service Error: {}", e.getMessage());
            // You may need to create an ErrorCode.AI_SERVICE_ERROR if it doesn't exist
            // Using a generic exception for now
            throw new RuntimeException("Lỗi kết nối tới AI Agent Service: " + e.getMessage());
        }
    }
    
    @Override
    public List<String> previewAgents(String prompt, User user) {
        String url = agentServiceUrl + "/api/v1/agent/orchestrate";
        
        Map<String, Object> body = Map.of(
            "prompt", prompt,
            "tenant_id", (user != null && user.getTenant() != null) ? user.getTenant().getId() : 0,
            "user_id", (user != null && user.getId() != null) ? user.getId() : 0,
            "tenant_name", (user != null && user.getTenant() != null) ? user.getTenant().getName() : "Doanh nghiệp",
            "tenant_description", (user != null && user.getTenant() != null && user.getTenant().getDescription() != null) ? user.getTenant().getDescription() : ""
        );
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<>() {}
            );
            
            Map<String, Object> data = response.getBody();
            if (data != null && data.get("agents") instanceof List<?> rawList) {
                return rawList.stream().map(Object::toString).toList();
            }
            return List.of();
                
        } catch (Exception e) {
            log.error("Agent Service Preview Error: {}", e.getMessage());
            throw new RuntimeException("Lỗi kết nối tới AI Agent Service: " + e.getMessage());
        }
    }
}
