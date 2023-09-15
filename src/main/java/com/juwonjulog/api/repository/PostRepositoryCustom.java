package com.juwonjulog.api.repository;

import com.juwonjulog.api.domain.Post;
import com.juwonjulog.api.request.PostSearch;

import java.util.List;

public interface PostRepositoryCustom {

    List<Post> getList(PostSearch postSearch);
}
