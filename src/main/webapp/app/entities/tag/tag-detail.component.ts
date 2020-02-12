import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { ITag } from 'app/shared/model/tag.model';
import { PostService } from 'app/entities/post/post.service';
import { IPost } from 'app/shared/model/post.model';

@Component({
  selector: 'jhi-tag-detail',
  templateUrl: './tag-detail.component.html'
})
export class TagDetailComponent implements OnInit {
  tag: ITag;
  posts: IPost[] = [];

  constructor(protected activatedRoute: ActivatedRoute, protected postService: PostService) {}

  ngOnInit() {
    this.activatedRoute.data.subscribe(({ tag }) => {
      this.tag = tag;
      this.loadTagPosts();
    });
  }

  loadTagPosts() {
    this.postService.query().subscribe(res => {
      const allPosts = res.body;
      for (const post of allPosts) {
        if (post.tags) {
          for (const postTag of post.tags) {
            if (this.tag.name === postTag.name) {
              this.posts.push(post);
              break;
            }
          }
        }
      }
    });
  }

  trackId(index: number, item: IPost) {
    return item.id;
  }

  previousState() {
    window.history.back();
  }
}
