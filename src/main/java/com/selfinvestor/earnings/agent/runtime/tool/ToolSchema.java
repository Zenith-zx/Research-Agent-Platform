package com.selfinvestor.earnings.agent.runtime.tool;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 暴露给模型的轻量 Tool 参数 Schema。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolSchema {
    private Map<String, String> properties = new LinkedHashMap<>();
    private List<String> required = List.of();
}
