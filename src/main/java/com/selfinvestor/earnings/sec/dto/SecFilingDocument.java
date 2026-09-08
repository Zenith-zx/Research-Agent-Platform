package com.selfinvestor.earnings.sec.dto;

public record SecFilingDocument(String type, String filename, String description, String url, String content) {}
