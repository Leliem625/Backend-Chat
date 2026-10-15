package com.example.demo.dto;

public record FriendSearchResponse(
        Long id,
        String username,
        String avatarUrl,
        FriendRelation relation) {

}
