<script setup>
import { computed, reactive, ref } from 'vue'

const mode = ref('login')
const message = ref('')

const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', nickname: '', phone: '', password: '', confirmPassword: '' })

const pageTitle = computed(() => (mode.value === 'login' ? '欢迎回来' : '创建学生账号'))

function switchMode(nextMode) {
  mode.value = nextMode
  message.value = ''
}

function handleLogin() {
  if (!loginForm.username || !loginForm.password) {
    message.value = '请输入用户名和密码'
    return
  }
  const roleName = loginForm.username === 'admin' ? '管理员' : '学生'
  message.value = `登录信息已填写，接入后端后将进入${roleName}页面`
}

function handleRegister() {
  if (!registerForm.username || !registerForm.password || !registerForm.confirmPassword) {
    message.value = '请填写用户名、密码和确认密码'
    return
  }
  if (registerForm.password !== registerForm.confirmPassword) {
    message.value = '两次输入的密码不一致'
    return
  }
  message.value = '注册信息已填写，接入后端后将创建学生账号'
}
</script>

<template>
  <main class="page">
    <section class="auth-card">
      <div class="brand">
        <span class="brand-mark">闲</span>
        <div>
          <p class="eyebrow">Campus Xianyu</p>
          <h1>校园二手交易平台</h1>
        </div>
      </div>

      <p class="intro">本校学生专属的二手发布、认证与线下交易 H5。</p>

      <div class="tabs" aria-label="登录注册切换">
        <button type="button" :class="{ active: mode === 'login' }" @click="switchMode('login')">登录</button>
        <button type="button" :class="{ active: mode === 'register' }" @click="switchMode('register')">注册</button>
      </div>

      <form v-if="mode === 'login'" class="form" @submit.prevent="handleLogin">
        <h2>{{ pageTitle }}</h2>
        <label>用户名<input v-model.trim="loginForm.username" type="text" autocomplete="username" placeholder="请输入用户名" /></label>
        <label>密码<input v-model="loginForm.password" type="password" autocomplete="current-password" placeholder="请输入密码" /></label>
        <button class="primary-button" type="submit">登录</button>
        <p class="hint">管理员账号由系统初始化，学生注册后再提交学号与学院认证。</p>
      </form>

      <form v-else class="form" @submit.prevent="handleRegister">
        <h2>{{ pageTitle }}</h2>
        <label>用户名<input v-model.trim="registerForm.username" type="text" autocomplete="username" placeholder="设置登录用户名" /></label>
        <label>昵称<input v-model.trim="registerForm.nickname" type="text" placeholder="例如：小鱼同学" /></label>
        <label>手机号<input v-model.trim="registerForm.phone" type="tel" autocomplete="tel" placeholder="选填" /></label>
        <label>密码<input v-model="registerForm.password" type="password" autocomplete="new-password" placeholder="设置密码" /></label>
        <label>确认密码<input v-model="registerForm.confirmPassword" type="password" autocomplete="new-password" placeholder="再次输入密码" /></label>
        <button class="primary-button" type="submit">注册学生账号</button>
      </form>

      <p v-if="message" class="message">{{ message }}</p>
    </section>
  </main>
</template>
