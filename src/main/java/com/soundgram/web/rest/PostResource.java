package com.soundgram.web.rest;

import com.soundgram.config.Constants;
import com.soundgram.domain.FollowedUser;
import com.soundgram.domain.Post;
import com.soundgram.domain.User;
import com.soundgram.repository.FollowedUserRepository;
import com.soundgram.repository.PostRepository;
import com.soundgram.repository.UserRepository;
import com.soundgram.repository.search.PostSearchRepository;
import com.soundgram.security.SecurityUtils;
import com.soundgram.web.rest.errors.BadRequestAlertException;

import io.github.jhipster.web.util.HeaderUtil;
import io.github.jhipster.web.util.PaginationUtil;
import io.github.jhipster.web.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.elasticsearch.index.query.QueryBuilders.*;

/**
 * REST controller for managing {@link com.soundgram.domain.Post}.
 */
@RestController
@RequestMapping("/api")
@Transactional
public class PostResource {

    private final Logger log = LoggerFactory.getLogger(PostResource.class);

    private static final String ENTITY_NAME = "post";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final PostRepository postRepository;

    private final PostSearchRepository postSearchRepository;

    private final UserRepository userRepository;

    private final FollowedUserRepository followedUserRepository;

    public PostResource(PostRepository postRepository, PostSearchRepository postSearchRepository, UserRepository userRepository, FollowedUserRepository followedUserRepository) {
        this.postRepository = postRepository;
        this.postSearchRepository = postSearchRepository;
        this.userRepository = userRepository;
        this.followedUserRepository = followedUserRepository;
    }

    /**
     * {@code POST  /posts} : Create a new post.
     *
     * @param post the post to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new post, or with status {@code 400 (Bad Request)} if the post has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/posts")
    public ResponseEntity<Post> createPost(@RequestBody Post post) throws URISyntaxException {
        log.debug("REST request to save Post : {}", post);
        if (post.getId() != null) {
            throw new BadRequestAlertException("A new post cannot already have an ID", ENTITY_NAME, "idexists");
        }
        User currentUser = userRepository.findOneByLogin(SecurityUtils.getCurrentUserLogin().orElse(null)).orElse(null);
        post.setUser(currentUser);
        Post result = postRepository.save(post);
        postSearchRepository.save(result);
        return ResponseEntity.created(new URI("/api/posts/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * {@code PUT  /posts} : Updates an existing post.
     *
     * @param post the post to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated post,
     * or with status {@code 400 (Bad Request)} if the post is not valid,
     * or with status {@code 500 (Internal Server Error)} if the post couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/posts")
    public ResponseEntity<Post> updatePost(@RequestBody Post post) throws URISyntaxException {
        log.debug("REST request to update Post : {}", post);
        if (post.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        Post result = postRepository.save(post);
        postSearchRepository.save(result);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, post.getId().toString()))
            .body(result);
    }

    /**
     * {@code GET  /posts} : get all the posts.
     *
     * @param pageable  the pagination information.
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of posts in body.
     */
    @GetMapping("/posts")
    public ResponseEntity<List<Post>> getAllPosts(Pageable pageable, @RequestParam(required = false, defaultValue = "false") boolean eagerload) {
        log.debug("REST request to get a page of Posts");
        log.debug("eagerLoad" + eagerload);
        Page<Post> page;
//        if (eagerload) {
            page = postRepository.findAllWithEagerRelationships(pageable);
//        } else {
            List<Post> allPosts = postRepository.findAll();
        //}
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(allPosts);
    }

    @GetMapping("/posts-followed")
    public ResponseEntity<List<Post>> getFollowedUsersPosts() {
        log.debug("REST request to get a page of followed users Posts");
        List<FollowedUser> allFU = followedUserRepository.findByUserIsCurrentUser();
        List<Post> allPosts = postRepository.findAll();
        log.debug("Searching for posts for user: {}", allFU.get(0).getUser().getLogin());
        log.debug("Followed users amount: {}", allFU.size());
        log.debug("First post owner: {}", allPosts.size());
        // page = postRepository.findAll(pageable); // if no eagerload
        List<Post> followedPosts = new ArrayList<>();
        for (Post post : allPosts) {
            for (FollowedUser fl : allFU) {
                if ( post.getUser().getId().equals(fl.getFollowedUserId()) ) {
                    followedPosts.add(post);
                    log.debug("Post tags size: {}", post.getTags().size()); // dodanie tego powoduje przesyłanie tagów na front - w przeciwnym razie post.tags is null !(???)
                }
            }
        }
        // HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().body(followedPosts);
    }

    @GetMapping("/posts-user/{login:" + Constants.LOGIN_REGEX + "}")
    public ResponseEntity<List<Post>> getUserPosts(@PathVariable String login) {
        log.debug("REST request to get posts that belong to user: {}", login);
        List<Post> userPosts = postRepository.findPostByUserLogin(login);
        return ResponseEntity.ok().body(userPosts);
    }

    /**
     * {@code GET  /posts/:id} : get the "id" post.
     *
     * @param id the id of the post to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the post, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/posts/{id}")
    public ResponseEntity<Post> getPost(@PathVariable Long id) {
        log.debug("REST request to get Post : {}", id);
        Optional<Post> post = postRepository.findOneWithEagerRelationships(id);
        return ResponseUtil.wrapOrNotFound(post);
    }

    /**
     * {@code DELETE  /posts/:id} : delete the "id" post.
     *
     * @param id the id of the post to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/posts/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        log.debug("REST request to delete Post : {}", id);
        postRepository.deleteById(id);
        postSearchRepository.deleteById(id);
        return ResponseEntity.noContent().headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString())).build();
    }

    /**
     * {@code SEARCH  /_search/posts?query=:query} : search for the post corresponding
     * to the query.
     *
     * @param query    the query of the post search.
     * @param pageable the pagination information.
     * @return the result of the search.
     */
    @GetMapping("/_search/posts")
    public ResponseEntity<List<Post>> searchPosts(@RequestParam String query, Pageable pageable) {
        log.debug("REST request to search for a page of Posts for query {}", query);
        Page<Post> page = postSearchRepository.search(queryStringQuery(query), pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }
}
