package com.example.bezma.controller;

import com.example.bezma.dto.req.agent.AgentExecuteRequest;
import com.example.bezma.dto.res.agent.AgentExecuteResponse;
import com.example.bezma.common.res.ApiResponse;
import com.example.bezma.entity.user.User;
import com.example.bezma.service.iService.IAgentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/agent")
@RequiredArgsConstructor
public class AgentController {

    private final IAgentService agentService;

    @PostMapping("/execute")
    public ApiResponse<AgentExecuteResponse> execute(
            @RequestBody @Valid AgentExecuteRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.<AgentExecuteResponse>builder()
                .data(agentService.executePipeline(request, currentUser))
                .message("AI Agent đã xử lý xong!")
                .build();
    }

    @PostMapping("/preview")
    public ApiResponse<List<String>> previewAgents(
            @RequestBody @Valid AgentExecuteRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.<List<String>>builder()
                .data(agentService.previewAgents(request.getPrompt(), currentUser))
                .message("Preview: Các agents sẽ được sử dụng")
                .build();
    }
}
