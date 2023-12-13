<script setup lang="ts">
import {ref} from "vue";
import axios from "axios";
import {useRouter} from "vue-router";

const posts = ref([])

const router = useRouter()

axios.get('/api/posts?page=1&size=5').then(response => {
  response.data.forEach((r: any) => {
    posts.value.push(r)
  })
})
</script>

<template>
  <ul>
    <li v-for="post in posts" :key="post.id">
      <div class="title">
        <router-link :to="{name: 'read', params: {postId:post.id}}">
          {{ post.title }}
        </router-link>
      </div>

      <div class="content">
        {{ post.content }}
      </div>
    </li>
  </ul>
</template>

<style scoped lang="scss">
ul {
  list-style: none;
  padding: 0;
}

li {
  margin-bottom: 1.3rem;
}

li .title a {
  font-size: 1.2rem;
  color: #383838;
  text-decoration: none;
}

li .content {
  font-size: 0.95rem;
  color: #5d5d5d;
}

li:last-child {
  margin-bottom: 0;
}
</style>