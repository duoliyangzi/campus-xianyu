<script setup>
import { computed, onMounted, reactive, ref } from 'vue'

const API_BASE = 'http://localhost:8080/api'
const TOKEN_KEY = 'campus_xianyu_token'

const page = ref('auth')
const mode = ref('login')
const message = ref('')
const errorMessage = ref('')
const loading = ref(false)
const activeStudentTab = ref('profile')
const activeAdminTab = ref('authReview')
const authApplications = ref([])
const adminLoading = ref(false)
const categories = ref([])
const campuses = ref([])
const myProducts = ref([])
const editingProductId = ref(null)
const confirmDialog = reactive({
  visible: false,
  title: '',
  content: '',
  confirmText: '',
  action: null
})

const currentUser = reactive({
  id: null,
  username: '',
  nickname: '',
  role: 'STUDENT',
  authStatus: 'UNAUTH',
  studentNo: '',
  college: '',
  authRemark: '',
  status: ''
})

const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', nickname: '', phone: '', password: '', confirmPassword: '' })
const authForm = reactive({ studentNo: '', college: '' })
const productForm = reactive({
  title: '',
  price: '',
  categoryId: '',
  conditionLevel: '',
  campusId: '',
  tradeMethod: '',
  coverUrl: '',
  description: ''
})

const studentTabs = [
  { key: 'home', label: '首页' },
  { key: 'wanted', label: '求购' },
  { key: 'publish', label: '发布' },
  { key: 'messages', label: '消息' },
  { key: 'profile', label: '我的' }
]

const adminTabs = [
  { key: 'authReview', label: '认证审核', owner: 'A' },
  { key: 'productReview', label: '商品审核', owner: 'C' },
  { key: 'reports', label: '举报处理', owner: 'C' },
  { key: 'users', label: '用户管理', owner: 'C' },
  { key: 'categories', label: '分类管理', owner: 'C' }
]

const conditionOptions = [
  { value: 'NEW', label: '全新' },
  { value: 'LIKE_NEW', label: '几乎全新' },
  { value: 'GOOD', label: '成色较好' },
  { value: 'FAIR', label: '有使用痕迹' },
  { value: 'POOR', label: '旧一些' }
]

const tradeMethodOptions = [
  { value: 'FACE', label: '线下面交' },
  { value: 'MAIL', label: '快递/邮寄' },
  { value: 'BOTH', label: '都可以' }
]

const pageTitle = computed(() => (mode.value === 'login' ? '欢迎回来' : '创建学生账号'))
const isAdmin = computed(() => currentUser.role === 'ADMIN')
const isApprovedStudent = computed(() => currentUser.role === 'STUDENT' && currentUser.authStatus === 'APPROVED')

const authStatusText = computed(() => {
  const statusMap = {
    UNAUTH: '未认证',
    PENDING: '待审核',
    APPROVED: '已通过',
    REJECTED: '已拒绝'
  }
  return statusMap[currentUser.authStatus] || '未认证'
})

const authStatusClass = computed(() => `status-${String(currentUser.authStatus || 'UNAUTH').toLowerCase()}`)

const studentHeaderTitle = computed(() => studentTabs.find((item) => item.key === activeStudentTab.value)?.label || '校园二手')
const adminHeaderTitle = computed(() => adminTabs.find((item) => item.key === activeAdminTab.value)?.label || '平台管理')

function clearNotice() {
  message.value = ''
  errorMessage.value = ''
}

function updateCurrentUser(user) {
  currentUser.id = user?.id || null
  currentUser.username = user?.username || ''
  currentUser.nickname = user?.nickname || user?.username || ''
  currentUser.role = user?.role || 'STUDENT'
  currentUser.authStatus = user?.role === 'ADMIN' ? 'APPROVED' : (user?.authStatus || 'UNAUTH')
  currentUser.studentNo = user?.studentNo || ''
  currentUser.college = user?.college || ''
  currentUser.authRemark = user?.authRemark || ''
  currentUser.status = user?.status || ''
}

function enterAppByRole() {
  page.value = isAdmin.value ? 'adminApp' : 'studentApp'
  activeStudentTab.value = 'profile'
  activeAdminTab.value = 'authReview'
  if (isAdmin.value) {
    loadAuthApplications()
  } else {
    loadDictionaries()
    loadMyProducts()
  }
}

async function apiRequest(path, options = {}) {
  const token = localStorage.getItem(TOKEN_KEY)
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {})
  }

  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers
  })

  const result = await response.json().catch(() => null)
  if (!response.ok || !result || result.code !== 0) {
    throw new Error(result?.message || `请求失败：${response.status}`)
  }
  return result.data
}

async function loadMe() {
  const token = localStorage.getItem(TOKEN_KEY)
  if (!token) return

  try {
    const user = await apiRequest('/users/me')
    updateCurrentUser(user)
    enterAppByRole()
  } catch (error) {
    localStorage.removeItem(TOKEN_KEY)
  }
}

async function loadDictionaries() {
  try {
    const [categoryData, campusData] = await Promise.all([
      apiRequest('/categories'),
      apiRequest('/campuses')
    ])
    categories.value = categoryData
    campuses.value = campusData
  } catch (error) {
    errorMessage.value = error.message
  }
}

async function loadMyProducts() {
  const token = localStorage.getItem(TOKEN_KEY)
  if (!token || isAdmin.value) return
  try {
    myProducts.value = await apiRequest('/products/mine')
  } catch (error) {
    errorMessage.value = error.message
  }
}

function switchMode(nextMode) {
  mode.value = nextMode
  clearNotice()
}

async function handleLogin() {
  clearNotice()
  if (!loginForm.username || !loginForm.password) {
    errorMessage.value = '请输入用户名和密码'
    return
  }

  loading.value = true
  try {
    const data = await apiRequest('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username: loginForm.username, password: loginForm.password })
    })
    localStorage.setItem(TOKEN_KEY, data.token)
    updateCurrentUser(data.user)
    enterAppByRole()
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  clearNotice()
  if (!registerForm.username || !registerForm.password || !registerForm.confirmPassword) {
    errorMessage.value = '请填写用户名、密码和确认密码'
    return
  }
  if (registerForm.password !== registerForm.confirmPassword) {
    errorMessage.value = '两次输入的密码不一致'
    return
  }

  loading.value = true
  try {
    await apiRequest('/auth/register', {
      method: 'POST',
      body: JSON.stringify({
        username: registerForm.username,
        password: registerForm.password,
        nickname: registerForm.nickname || registerForm.username,
        phone: registerForm.phone || null
      })
    })
    loginForm.username = registerForm.username
    loginForm.password = ''
    registerForm.password = ''
    registerForm.confirmPassword = ''
    mode.value = 'login'
    message.value = '注册成功，请用刚才的账号登录。'
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    loading.value = false
  }
}

function openAuthPage() {
  authForm.studentNo = currentUser.studentNo
  authForm.college = currentUser.college
  clearNotice()
  page.value = 'studentAuth'
}

async function submitStudentAuth() {
  clearNotice()
  if (!authForm.studentNo || !authForm.college) {
    errorMessage.value = '请填写学号和学院'
    return
  }

  loading.value = true
  try {
    const user = await apiRequest('/users/auth', {
      method: 'POST',
      body: JSON.stringify({ studentNo: authForm.studentNo, college: authForm.college })
    })
    updateCurrentUser(user)
    message.value = '实名认证已提交，等待管理员审核。'
    page.value = 'studentApp'
    activeStudentTab.value = 'profile'
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    loading.value = false
  }
}

async function loadAuthApplications() {
  if (!isAdmin.value) return
  adminLoading.value = true
  try {
    authApplications.value = await apiRequest('/admin/auth-applications?status=PENDING')
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    adminLoading.value = false
  }
}

function selectAdminTab(tabKey) {
  activeAdminTab.value = tabKey
  if (tabKey === 'authReview') {
    loadAuthApplications()
  }
}

async function reviewAuth(application, status) {
  clearNotice()
  adminLoading.value = true
  try {
    await apiRequest(`/admin/auth-applications/${application.id}/review`, {
      method: 'POST',
      body: JSON.stringify({
        authStatus: status,
        authRemark: status === 'APPROVED' ? '实名认证审核通过' : '实名认证审核未通过'
      })
    })
    message.value = status === 'APPROVED' ? '已通过该学生认证。' : '已拒绝该学生认证。'
    await loadAuthApplications()
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    adminLoading.value = false
  }
}

function validateProductForm() {
  if (!productForm.title || !productForm.price || !productForm.categoryId || !productForm.conditionLevel || !productForm.campusId || !productForm.tradeMethod || !productForm.description) {
    return '请补全商品标题、价格、分类、新旧程度、校区、交易方式和图文描述'
  }
  if (Number(productForm.price) <= 0) {
    return '商品价格必须大于 0'
  }
  return ''
}

function resetProductForm() {
  editingProductId.value = null
  productForm.title = ''
  productForm.price = ''
  productForm.categoryId = ''
  productForm.conditionLevel = ''
  productForm.campusId = ''
  productForm.tradeMethod = ''
  productForm.coverUrl = ''
  productForm.description = ''
}

async function submitProduct() {
  clearNotice()
  const validationError = validateProductForm()
  if (validationError) {
    errorMessage.value = validationError
    return
  }

  loading.value = true
  try {
    const payload = {
      title: productForm.title,
      price: Number(productForm.price),
      categoryId: Number(productForm.categoryId),
      conditionLevel: productForm.conditionLevel,
      campusId: Number(productForm.campusId),
      tradeMethod: productForm.tradeMethod,
      coverUrl: productForm.coverUrl || null,
      description: productForm.description
    }

    if (editingProductId.value) {
      await apiRequest(`/products/${editingProductId.value}`, {
        method: 'PUT',
        body: JSON.stringify(payload)
      })
      message.value = '商品已更新，并重新提交审核。'
    } else {
      await apiRequest('/products', {
        method: 'POST',
        body: JSON.stringify(payload)
      })
      message.value = '商品已发布，等待管理员审核。'
    }

    resetProductForm()
    await loadMyProducts()
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    loading.value = false
  }
}

function editProduct(product) {
  clearNotice()
  editingProductId.value = product.id
  productForm.title = product.title
  productForm.price = product.price
  productForm.categoryId = String(product.categoryId)
  productForm.conditionLevel = product.conditionLevel
  productForm.campusId = String(product.campusId)
  productForm.tradeMethod = product.tradeMethod
  productForm.coverUrl = product.coverUrl || ''
  productForm.description = product.description
  activeStudentTab.value = 'publish'
}

function openConfirmDialog({ title, content, confirmText, action }) {
  confirmDialog.visible = true
  confirmDialog.title = title
  confirmDialog.content = content
  confirmDialog.confirmText = confirmText
  confirmDialog.action = action
}

function closeConfirmDialog() {
  confirmDialog.visible = false
  confirmDialog.title = ''
  confirmDialog.content = ''
  confirmDialog.confirmText = ''
  confirmDialog.action = null
}

async function runConfirmAction() {
  const action = confirmDialog.action
  closeConfirmDialog()
  if (action) {
    await action()
  }
}

function askOffShelfProduct(product) {
  openConfirmDialog({
    title: '确认下架商品？',
    content: `下架后，其他同学将暂时看不到“${product.title}”。你之后可以在我的商品里恢复上架。`,
    confirmText: '确认下架',
    action: () => offShelfProduct(product)
  })
}

async function offShelfProduct(product) {
  clearNotice()
  loading.value = true
  try {
    await apiRequest(`/products/${product.id}/off-shelf`, { method: 'PUT' })
    message.value = '商品已下架。'
    await loadMyProducts()
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    loading.value = false
  }
}

function askRestoreProduct(product) {
  openConfirmDialog({
    title: '恢复上架商品？',
    content: `恢复后，“${product.title}”会重新提交审核，审核通过后才能正式展示。`,
    confirmText: '恢复上架',
    action: () => restoreProduct(product)
  })
}

async function restoreProduct(product) {
  clearNotice()
  loading.value = true
  try {
    await apiRequest(`/products/${product.id}/restore`, { method: 'PUT' })
    message.value = '商品已恢复，并重新提交审核。'
    await loadMyProducts()
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    loading.value = false
  }
}

function getCategoryName(id) {
  return categories.value.find((item) => item.id === id)?.name || `分类 ${id}`
}

function getCampusName(id) {
  return campuses.value.find((item) => item.id === id)?.name || `校区 ${id}`
}

function getConditionLabel(value) {
  return conditionOptions.find((item) => item.value === value)?.label || value
}

function getTradeMethodLabel(value) {
  return tradeMethodOptions.find((item) => item.value === value)?.label || value
}

function logout() {
  localStorage.removeItem(TOKEN_KEY)
  page.value = 'auth'
  mode.value = 'login'
  clearNotice()
  loginForm.password = ''
  myProducts.value = []
  resetProductForm()
  updateCurrentUser({ role: 'STUDENT', authStatus: 'UNAUTH' })
}

onMounted(loadMe)
</script>

<template>
  <main class="page">
    <section v-if="page === 'auth'" class="auth-card">
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
        <button class="primary-button" type="submit" :disabled="loading">{{ loading ? '登录中...' : '登录' }}</button>
        <p class="hint">管理员测试账号：admin / password。学生账号可先注册再登录。</p>
      </form>

      <form v-else class="form" @submit.prevent="handleRegister">
        <h2>{{ pageTitle }}</h2>
        <label>用户名<input v-model.trim="registerForm.username" type="text" autocomplete="username" placeholder="设置登录用户名" /></label>
        <label>昵称<input v-model.trim="registerForm.nickname" type="text" placeholder="例如：小鱼同学" /></label>
        <label>手机号<input v-model.trim="registerForm.phone" type="tel" autocomplete="tel" placeholder="选填" /></label>
        <label>密码<input v-model="registerForm.password" type="password" autocomplete="new-password" placeholder="设置密码" /></label>
        <label>确认密码<input v-model="registerForm.confirmPassword" type="password" autocomplete="new-password" placeholder="再次输入密码" /></label>
        <button class="primary-button" type="submit" :disabled="loading">{{ loading ? '注册中...' : '注册学生账号' }}</button>
      </form>

      <p v-if="message" class="message">{{ message }}</p>
      <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>
    </section>

    <section v-else-if="page === 'studentApp'" class="app-shell">
      <header class="app-header">
        <div>
          <p class="eyebrow">学生端</p>
          <h1>{{ studentHeaderTitle }}</h1>
        </div>
        <button class="icon-button" type="button" @click="logout">退出</button>
      </header>

      <div class="app-content">
        <section v-if="activeStudentTab === 'home'" class="panel">
          <h2>商品列表</h2>
          <p class="intro">这里预留给 B 同学实现商品列表、关键词搜索、分类/价格/校区/新旧程度筛选和分页。</p>
          <div class="placeholder-list">
            <article class="placeholder-card"><strong>教材</strong><span>商品卡片占位</span></article>
            <article class="placeholder-card"><strong>数码</strong><span>筛选结果占位</span></article>
          </div>
        </section>

        <section v-else-if="activeStudentTab === 'wanted'" class="panel">
          <h2>求购管理</h2>
          <p class="intro">这里预留给 B 同学实现求购发布、求购列表、求购详情和卖家接单沟通。</p>
          <button class="secondary-button" type="button">发布求购</button>
        </section>

        <section v-else-if="activeStudentTab === 'publish'" class="panel">
          <h2>{{ editingProductId ? '修改商品' : '发布商品' }}</h2>
          <p v-if="!isApprovedStudent" class="intro">发布商品需要先通过学生实名认证。当前状态：{{ authStatusText }}</p>
          <button v-if="!isApprovedStudent" class="primary-button" type="button" @click="openAuthPage">去实名认证</button>

          <form v-else class="form" @submit.prevent="submitProduct">
            <label>标题<input v-model.trim="productForm.title" type="text" placeholder="例如：高等数学教材" /></label>
            <label>价格<input v-model.trim="productForm.price" type="number" min="0" step="0.01" placeholder="例如：25" /></label>
            <label>分类
              <select v-model="productForm.categoryId">
                <option value="">请选择分类</option>
                <option v-for="item in categories" :key="item.id" :value="item.id">{{ item.name }}</option>
              </select>
            </label>
            <label>新旧程度
              <select v-model="productForm.conditionLevel">
                <option value="">请选择新旧程度</option>
                <option v-for="item in conditionOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
              </select>
            </label>
            <label>校区
              <select v-model="productForm.campusId">
                <option value="">请选择校区</option>
                <option v-for="item in campuses" :key="item.id" :value="item.id">{{ item.name }}</option>
              </select>
            </label>
            <label>交易方式
              <select v-model="productForm.tradeMethod">
                <option value="">请选择交易方式</option>
                <option v-for="item in tradeMethodOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
              </select>
            </label>
            <label>封面图片地址<input v-model.trim="productForm.coverUrl" type="url" placeholder="选填，可先粘贴图片链接" /></label>
            <label>图文描述<textarea v-model.trim="productForm.description" rows="4" placeholder="描述物品情况、交易地点、购买时间等"></textarea></label>
            <button class="primary-button" type="submit" :disabled="loading">{{ loading ? '提交中...' : (editingProductId ? '保存修改' : '提交商品') }}</button>
            <button v-if="editingProductId" class="secondary-button" type="button" @click="resetProductForm">取消修改</button>
          </form>
          <p v-if="message" class="message">{{ message }}</p>
          <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>
        </section>

        <section v-else-if="activeStudentTab === 'messages'" class="panel">
          <h2>消息中心</h2>
          <p class="intro">这里预留给 C 同学实现商品留言、买卖双方私聊、线下交易约定和订单状态跟踪。</p>
          <div class="empty-state">暂无消息</div>
        </section>

        <section v-else class="panel">
          <div class="profile-head compact">
            <span class="avatar">{{ currentUser.nickname.slice(0, 1) || '我' }}</span>
            <div>
              <p class="eyebrow">我的</p>
              <h1>{{ currentUser.nickname }}</h1>
              <p class="intro">{{ currentUser.username }} · 学生</p>
            </div>
          </div>

          <div class="info-list">
            <div class="info-row"><span>角色</span><strong>普通学生</strong></div>
            <div class="info-row"><span>认证状态</span><strong :class="['status-pill', authStatusClass]">{{ authStatusText }}</strong></div>
            <div v-if="currentUser.studentNo" class="info-row"><span>学号</span><strong>{{ currentUser.studentNo }}</strong></div>
            <div v-if="currentUser.college" class="info-row"><span>学院</span><strong>{{ currentUser.college }}</strong></div>
          </div>

          <div class="action-list">
            <button class="primary-button" type="button" @click="openAuthPage">{{ currentUser.authStatus === 'UNAUTH' ? '去实名认证' : '查看/更新认证信息' }}</button>
            <button class="secondary-button" type="button" @click="activeStudentTab = 'publish'">发布商品</button>
            <p class="hint">A 模块负责用户认证和商品发布管理；其他页面已留好入口。</p>
          </div>

          <section class="section-block">
            <div class="section-title">
              <h2>我的商品</h2>
              <button class="text-button" type="button" @click="loadMyProducts">刷新</button>
            </div>
            <div v-if="myProducts.length === 0" class="empty-state">还没有发布商品</div>
            <article v-for="product in myProducts" :key="product.id" class="product-card">
              <div>
                <strong>{{ product.title }}</strong>
                <p>￥{{ product.price }} · {{ getCategoryName(product.categoryId) }} · {{ getCampusName(product.campusId) }}</p>
                <p>{{ getConditionLabel(product.conditionLevel) }} · {{ getTradeMethodLabel(product.tradeMethod) }}</p>
              </div>
              <span :class="['status-pill', `status-${product.status.toLowerCase().replace('_', '-')}`]">{{ product.status }}</span>
              <p class="product-desc">{{ product.description }}</p>
              <div class="review-actions">
                <button type="button" @click="editProduct(product)">修改</button>
                <button v-if="product.status !== 'OFF_SHELF'" type="button" @click="askOffShelfProduct(product)">下架</button>
                <button v-else type="button" @click="askRestoreProduct(product)">恢复上架</button>
              </div>
            </article>
          </section>
        </section>
      </div>

      <nav class="bottom-nav" aria-label="学生端底部导航">
        <button v-for="item in studentTabs" :key="item.key" type="button" :class="{ active: activeStudentTab === item.key }" @click="activeStudentTab = item.key">
          <span>{{ item.label }}</span>
        </button>
      </nav>
    </section>

    <section v-else-if="page === 'adminApp'" class="app-shell admin-shell">
      <header class="app-header">
        <div>
          <p class="eyebrow">管理员端</p>
          <h1>{{ adminHeaderTitle }}</h1>
        </div>
        <button class="icon-button" type="button" @click="logout">退出</button>
      </header>

      <div class="admin-menu" aria-label="管理员功能导航">
        <button v-for="item in adminTabs" :key="item.key" type="button" :class="{ active: activeAdminTab === item.key }" @click="selectAdminTab(item.key)">
          {{ item.label }}
        </button>
      </div>

      <div class="app-content">
        <section v-if="activeAdminTab === 'authReview'" class="panel">
          <h2>学生认证审核</h2>
          <p class="intro">这里显示数据库中已提交实名认证、状态为待审核的学生。</p>
          <p v-if="message" class="message">{{ message }}</p>
          <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>
          <div v-if="adminLoading" class="empty-state">正在加载...</div>
          <div v-else-if="authApplications.length === 0" class="empty-state">暂无待审核认证</div>
          <article v-else v-for="item in authApplications" :key="item.id" class="review-card">
            <div>
              <strong>{{ item.nickname }}</strong>
              <p>{{ item.username }} · {{ item.studentNo }} · {{ item.college }}</p>
            </div>
            <div class="review-actions">
              <button type="button" @click="reviewAuth(item, 'APPROVED')">通过</button>
              <button type="button" @click="reviewAuth(item, 'REJECTED')">拒绝</button>
            </div>
            <span :class="['status-pill', `status-${item.authStatus.toLowerCase()}`]">{{ item.authStatus }}</span>
          </article>
        </section>

        <section v-else class="panel">
          <h2>{{ adminHeaderTitle }}</h2>
          <p class="intro">该管理功能由 {{ adminTabs.find((item) => item.key === activeAdminTab)?.owner }} 同学后续开发。</p>
          <div class="empty-state">功能入口已预留</div>
        </section>
      </div>
    </section>

    <section v-else class="auth-card">
      <div class="topbar">
        <button class="icon-button" type="button" @click="page = 'studentApp'; activeStudentTab = 'profile'">返回</button>
      </div>

      <p class="eyebrow">学生实名认证</p>
      <h1>提交学号与学院</h1>
      <p class="intro">提交后状态会变为待审核，管理员通过后即可发布商品。</p>

      <form class="form" @submit.prevent="submitStudentAuth">
        <label>学号<input v-model.trim="authForm.studentNo" type="text" placeholder="例如：20260001" /></label>
        <label>学院<input v-model.trim="authForm.college" type="text" placeholder="例如：计算机学院" /></label>
        <button class="primary-button" type="submit" :disabled="loading">{{ loading ? '提交中...' : '提交认证' }}</button>
      </form>

      <p v-if="message" class="message">{{ message }}</p>
      <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>
    </section>

    <div v-if="confirmDialog.visible" class="confirm-overlay" role="dialog" aria-modal="true">
      <div class="confirm-dialog">
        <h2>{{ confirmDialog.title }}</h2>
        <p>{{ confirmDialog.content }}</p>
        <div class="confirm-actions">
          <button class="secondary-button" type="button" @click="closeConfirmDialog">取消</button>
          <button class="danger-button" type="button" @click="runConfirmAction">{{ confirmDialog.confirmText }}</button>
        </div>
      </div>
    </div>
  </main>
</template>