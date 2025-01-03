package com.ll.restByTdd.com.ll.restByTdd.domain.post.post.controller;

import com.ll.restByTdd.domain.member.member.entity.Member;
import com.ll.restByTdd.domain.member.member.service.MemberService;
import com.ll.restByTdd.domain.post.post.controller.ApiV1PostController;
import com.ll.restByTdd.domain.post.post.entity.Post;
import com.ll.restByTdd.domain.post.post.service.PostService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
public class ApiV1PostControllerTest {
    @Autowired
    private PostService postService;
    @Autowired
    private MemberService memberService;
    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("1번글 조회")
    void t1() throws Exception {
        ResultActions resultActions = mvc
                .perform(
                        get("/api/v1/posts/1")
                )
                .andDo(print());

        Post post = postService.findById(1).get();

        resultActions
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("item"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(post.getId()))
                .andExpect(jsonPath("$.createDate").value(Matchers.startsWith(post.getCreateDate().toString().substring(0, 25))))
                .andExpect(jsonPath("$.modifyDate").value(Matchers.startsWith(post.getModifyDate().toString().substring(0, 25))))
                .andExpect(jsonPath("$.authorId").value(post.getAuthor().getId()))
                .andExpect(jsonPath("$.authorName").value(post.getAuthor().getName()))
                .andExpect(jsonPath("$.title").value(post.getTitle()))
                .andExpect(jsonPath("$.content").value(post.getContent()))
                .andExpect(jsonPath("$.published").value(post.isPublished()))
                .andExpect(jsonPath("$.listed").value(post.isListed()));
    }

    @Test
    @DisplayName("존재하지 않는 1000000번글 조회, 404")
    void t2() throws Exception {
        ResultActions resultActions = mvc
                .perform(
                        get("/api/v1/posts/1000000")
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("item"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"))
                .andExpect(jsonPath("$.msg").value("해당 데이터가 존재하지 않습니다."));
    }

    @Test
    @DisplayName("글 작성")
    void t3() throws Exception {
        Member actor = memberService.findByUsername("user1").get();

        ResultActions resultActions = mvc
                .perform(
                        post("/api/v1/posts")
                                .header("Authorization", "Bearer " + actor.getApiKey())
                                .content("""
                                        {
                                            "title": "제목 new",
                                            "content": "내용 new",
                                            "published": true,
                                            "listed": false
                                        }
                                        """)
                                .contentType(
                                        new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8)
                                )
                )
                .andDo(print());

        Post post = postService.findLatest().get();

        assertThat(post.getAuthor()).isEqualTo(actor);

        resultActions
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("write"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201-1"))
                .andExpect(jsonPath("$.msg").value("%d번 글이 작성되었습니다.".formatted(post.getId())))
                .andExpect(jsonPath("$.data.id").value(post.getId()))
                .andExpect(jsonPath("$.data.createDate").value(Matchers.startsWith(post.getCreateDate().toString().substring(0, 25))))
                .andExpect(jsonPath("$.data.modifyDate").value(Matchers.startsWith(post.getModifyDate().toString().substring(0, 25))))
                .andExpect(jsonPath("$.data.authorId").value(post.getAuthor().getId()))
                .andExpect(jsonPath("$.data.authorName").value(post.getAuthor().getName()))
                .andExpect(jsonPath("$.data.title").value(post.getTitle()))
                .andExpect(jsonPath("$.data.content").value(post.getContent()))
                .andExpect(jsonPath("$.data.published").value(post.isPublished()))
                .andExpect(jsonPath("$.data.listed").value(post.isListed()));
    }

    @Test
    @DisplayName("글 작성, with no input")
    void t4() throws Exception {
        Member actor = memberService.findByUsername("user1").get();

        ResultActions resultActions = mvc
                .perform(
                        post("/api/v1/posts")
                                .header("Authorization", "Bearer " + actor.getApiKey())
                                .content("""
                                        {
                                            "title": "",
                                            "content": ""
                                        }
                                        """)
                                .contentType(
                                        new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8)
                                )
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("write"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"))
                .andExpect(jsonPath("$.msg").value("""
                        content-Length-length must be between 2 and 10000000
                        content-NotBlank-may not be empty
                        title-Length-length must be between 2 and 100
                        title-NotBlank-may not be empty
                        """.stripIndent().trim()));
    }

    @Test
    @DisplayName("글 작성, with no actor")
    void t5() throws Exception {
        ResultActions resultActions = mvc
                .perform(
                        post("/api/v1/posts")
                                .content("""
                                        {
                                            "title": "제목 new",
                                            "content": "내용 new"
                                        }
                                        """)
                                .contentType(
                                        new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8)
                                )
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("write"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-1"))
                .andExpect(jsonPath("$.msg").value("apiKey를 입력해주세요."));
    }

    @Test
    @DisplayName("글 수정")
    void t6() throws Exception {
        Member actor = memberService.findByUsername("user1").get();
        Post post = postService.findById(1).get();

        LocalDateTime oldModifyDate = post.getModifyDate();

        ResultActions resultActions = mvc
                .perform(
                        put("/api/v1/posts/1")
                                .header("Authorization", "Bearer " + actor.getApiKey())
                                .content("""
                                        {
                                            "title": "축구 하실 분 계신가요?",
                                            "content": "14시 까지 22명을 모아야 진행이 됩니다.",
                                            "published": true,
                                            "listed": false
                                        }
                                        """)
                                .contentType(
                                        new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8)
                                )
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("modify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("1번 글이 수정되었습니다."))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.createDate").value(Matchers.startsWith(post.getCreateDate().toString().substring(0, 25))))
                .andExpect(jsonPath("$.data.modifyDate").value(Matchers.not(Matchers.startsWith(oldModifyDate.toString().substring(0, 25)))))
                .andExpect(jsonPath("$.data.authorId").value(post.getAuthor().getId()))
                .andExpect(jsonPath("$.data.authorName").value(post.getAuthor().getName()))
                .andExpect(jsonPath("$.data.title").value("축구 하실 분 계신가요?"))
                .andExpect(jsonPath("$.data.content").value("14시 까지 22명을 모아야 진행이 됩니다."))
                .andExpect(jsonPath("$.data.published").value(true))
                .andExpect(jsonPath("$.data.listed").value(false));
    }

    @Test
    @DisplayName("글 수정, with no input")
    void t7() throws Exception {
        Member actor = memberService.findByUsername("user1").get();

        ResultActions resultActions = mvc
                .perform(
                        put("/api/v1/posts/1")
                                .header("Authorization", "Bearer " + actor.getApiKey())
                                .content("""
                                        {
                                            "title": "",
                                            "content": ""
                                        }
                                        """)
                                .contentType(
                                        new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8)
                                )
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("modify"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"))
                .andExpect(jsonPath("$.msg").value("""
                        content-Length-length must be between 2 and 10000000
                        content-NotBlank-may not be empty
                        title-Length-length must be between 2 and 100
                        title-NotBlank-may not be empty
                        """.stripIndent().trim()));
    }

    @Test
    @DisplayName("글 수정, with no actor")
    void t8() throws Exception {
        ResultActions resultActions = mvc
                .perform(
                        put("/api/v1/posts/1")
                                .content("""
                                        {
                                            "title": "축구 하실 분 계신가요?",
                                            "content": "14시 까지 22명을 모아야 진행이 됩니다."
                                        }
                                        """)
                                .contentType(
                                        new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8)
                                )
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("modify"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-1"))
                .andExpect(jsonPath("$.msg").value("apiKey를 입력해주세요."));
    }

    @Test
    @DisplayName("글 수정, with no permission")
    void t9() throws Exception {
        Member actor = memberService.findByUsername("user2").get();

        ResultActions resultActions = mvc
                .perform(
                        put("/api/v1/posts/1")
                                .header("Authorization", "Bearer " + actor.getApiKey())
                                .content("""
                                        {
                                            "title": "축구 하실 분 계신가요?",
                                            "content": "14시 까지 22명을 모아야 진행이 됩니다."
                                        }
                                        """)
                                .contentType(
                                        new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8)
                                )
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("modify"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("403-1"))
                .andExpect(jsonPath("$.msg").value("작성자만 글을 수정할 수 있습니다."));
    }

    @Test
    @DisplayName("글 삭제")
    void t10() throws Exception {
        Member actor = memberService.findByUsername("user1").get();

        ResultActions resultActions = mvc
                .perform(
                        delete("/api/v1/posts/1")
                                .header("Authorization", "Bearer " + actor.getApiKey())
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("delete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("1번 글이 삭제되었습니다."));

        assertThat(postService.findById(1)).isEmpty();
    }

    @Test
    @DisplayName("글 삭제, with not exist post id")
    void t11() throws Exception {
        Member actor = memberService.findByUsername("user1").get();

        ResultActions resultActions = mvc
                .perform(
                        delete("/api/v1/posts/1000000")
                                .header("Authorization", "Bearer " + actor.getApiKey())
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("delete"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"))
                .andExpect(jsonPath("$.msg").value("해당 데이터가 존재하지 않습니다."));
    }

    @Test
    @DisplayName("글 삭제, with no actor")
    void t12() throws Exception {
        ResultActions resultActions = mvc
                .perform(
                        delete("/api/v1/posts/1")
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("delete"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-1"))
                .andExpect(jsonPath("$.msg").value("apiKey를 입력해주세요."));
    }

    @Test
    @DisplayName("글 삭제, with no permission")
    void t13() throws Exception {
        Member actor = memberService.findByUsername("user2").get();

        ResultActions resultActions = mvc
                .perform(
                        delete("/api/v1/posts/1")
                                .header("Authorization", "Bearer " + actor.getApiKey())
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("delete"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("403-1"))
                .andExpect(jsonPath("$.msg").value("작성자만 글을 삭제할 수 있습니다."));
    }

    @Test
    @DisplayName("비공개글 6번글 조회, with 작성자")
    void t14() throws Exception {
        Member actor = memberService.findByUsername("user4").get();

        ResultActions resultActions = mvc
                .perform(
                        get("/api/v1/posts/6")
                                .header("Authorization", "Bearer " + actor.getApiKey())
                )
                .andDo(print());

        Post post = postService.findById(6).get();

        resultActions
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("item"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(post.getId()))
                .andExpect(jsonPath("$.createDate").value(Matchers.startsWith(post.getCreateDate().toString().substring(0, 25))))
                .andExpect(jsonPath("$.modifyDate").value(Matchers.startsWith(post.getModifyDate().toString().substring(0, 25))))
                .andExpect(jsonPath("$.authorId").value(post.getAuthor().getId()))
                .andExpect(jsonPath("$.authorName").value(post.getAuthor().getName()))
                .andExpect(jsonPath("$.title").value(post.getTitle()))
                .andExpect(jsonPath("$.content").value(post.getContent()));
    }

    @Test
    @DisplayName("비공개글 6번글 조회, with no actor")
    void t15() throws Exception {
        ResultActions resultActions = mvc
                .perform(
                        get("/api/v1/posts/6")
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("item"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-1"))
                .andExpect(jsonPath("$.msg").value("apiKey를 입력해주세요."));
    }

    @Test
    @DisplayName("비공개글 6번글 조회, with no permission")
    void t16() throws Exception {
        Member actor = memberService.findByUsername("user1").get();

        ResultActions resultActions = mvc
                .perform(
                        get("/api/v1/posts/6")
                                .header("Authorization", "Bearer " + actor.getApiKey())
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("item"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("403-1"))
                .andExpect(jsonPath("$.msg").value("비공개글은 작성자만 볼 수 있습니다."));
    }

    @Test
    @DisplayName("다건 조회")
    void t17() throws Exception {
        ResultActions resultActions = mvc
                .perform(
                        get("/api/v1/posts?page=1&pageSize=3")
                )
                .andDo(print());

        Page<Post> postPage = postService
                .findByListedPaged(true, 1, 3);

        resultActions
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(postPage.getTotalElements()));

        List<Post> posts = postPage.getContent();

        for (int i = 0; i < posts.size(); i++) {
            Post post = posts.get(i);
            resultActions
                    .andExpect(jsonPath("$.items[%d].id".formatted(i)).value(post.getId()))
                    .andExpect(jsonPath("$.items[%d].createDate".formatted(i)).value(Matchers.startsWith(post.getCreateDate().toString().substring(0, 25))))
                    .andExpect(jsonPath("$.items[%d].modifyDate".formatted(i)).value(Matchers.startsWith(post.getModifyDate().toString().substring(0, 25))))
                    .andExpect(jsonPath("$.items[%d].authorId".formatted(i)).value(post.getAuthor().getId()))
                    .andExpect(jsonPath("$.items[%d].authorName".formatted(i)).value(post.getAuthor().getName()))
                    .andExpect(jsonPath("$.items[%d].title".formatted(i)).value(post.getTitle()))
                    .andExpect(jsonPath("$.items[%d].content".formatted(i)).doesNotExist())
                    .andExpect(jsonPath("$.items[%d].published".formatted(i)).value(post.isPublished()))
                    .andExpect(jsonPath("$.items[%d].listed".formatted(i)).value(post.isListed()));
        }
    }
}
