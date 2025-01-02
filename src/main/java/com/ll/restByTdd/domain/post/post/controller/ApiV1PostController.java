package com.ll.restByTdd.domain.post.post.controller;

import com.ll.restByTdd.domain.post.post.dto.PostDto;
import com.ll.restByTdd.domain.post.post.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class ApiV1PostController {
    private final PostService postService;

    @GetMapping("/{id}")
    public PostDto item(@PathVariable long id) {
        return new PostDto(postService.findById(id).get());
    }
}
