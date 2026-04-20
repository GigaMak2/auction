package com.example.auction.domain.ai.exception;

public class ToolEmptyResultException extends RuntimeException {

    public ToolEmptyResultException(String llmMessage) {
        super(llmMessage);
    }
}