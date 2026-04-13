package com.example.bezma.service.iService;

import com.example.bezma.dto.req.agent.AgentExecuteRequest;
import com.example.bezma.dto.res.agent.AgentExecuteResponse;
import com.example.bezma.entity.user.User;

import java.util.List;

public interface IAgentService {
    AgentExecuteResponse executePipeline(AgentExecuteRequest request, User user);
    List<String> previewAgents(String prompt, User user);
}
