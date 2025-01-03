package com.ll.restByTdd.domain.post.post.repository;

import com.ll.restByTdd.domain.post.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findAllByOrderByIdDesc();

    Optional<Post> findFirstByOrderByIdDesc();

    Page<Post> findByListed(boolean listed, PageRequest pageRequest);
}