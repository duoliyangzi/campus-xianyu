<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { match as matchPinyin } from 'pinyin-pro'

const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api'
const TOKEN_KEY = 'campus_xianyu_token'

const page = ref('auth')
const mode = ref('login')
const message = ref('')
const errorMessage = ref('')
const loading = ref(false)
const activeStudentTab = ref('profile')
const activeAdminTab = ref('authReview')
const authReviewStatus = ref('PENDING')
const authApplications = ref([])
const adminLoading = ref(false)
const imageUploading = ref(false)
const previewImageUrl = ref('')
const categories = ref([])
const campuses = ref([])
const myProducts = ref([])
const editingProductId = ref(null)
const products = ref([])
const productPage = reactive({ page: 0, totalPages: 0, totalElements: 0, first: true, last: true })
const productFilters = reactive({ keyword: '', categoryId: '', campusId: '', conditionLevel: '', minPrice: '', maxPrice: '' })
const selectedProduct = ref(null)
const wantedItems = ref([])
const myWanted = ref([])
const wantedPage = reactive({ page: 0, totalPages: 0, totalElements: 0, first: true, last: true })
const wantedFilters = reactive({ keyword: '', campusId: '', expectCondition: '', minBudget: '', maxBudget: '' })
const activeMyTab = ref('products')
const showWantedForm = ref(false)
const editingWantedId = ref(null)
const selectedWanted = ref(null)
const publicProfile = ref(null)
const profileProducts = ref([])
const profileWanted = ref([])
const profileLoading = ref(false)
const productComments = ref([])
const commentForm = reactive({ content: '' })
const conversations = ref([])
const selectedConversation = ref(null)
const pendingChat = ref(null)
const chatMessages = ref([])
const chatForm = reactive({ content: '' })
const myOrders = ref([])
const reportReasons = ref([])
const reportDialog = reactive({ visible: false, targetType: '', targetId: null, reasonId: '', description: '' })
const orderForm = reactive({ meetTime: '', meetLocation: '', remark: '' })
const pendingProducts = ref([])
const productReviewStatus = ref('PENDING')
const adminReports = ref([])
const reportReviewStatus = ref('PENDING')
const adminUsers = ref([])
const adminUserKeyword = ref('')
const adminCategories = ref([])
const categoryForm = reactive({ id: null, name: '' })
const aiAuditLogs = ref([])
const aiAuditMap = ref({})
const confirmDialog = reactive({
  visible: false,
  title: '',
  content: '',
  confirmText: '',
  action: null
})

const noticeDialog = reactive({
  visible: false,
  type: 'info',
  title: '',
  content: ''
})
const authRemarkDialog = reactive({
  visible: false,
  application: null,
  status: '',
  remark: ''
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
  imageUrls: [],
  description: ''
})
const wantedForm = reactive({ itemName: '', budget: '', expectCondition: '', campusId: '', description: '' })

const studentTabs = [
  { key: 'home', label: '首页' },
  { key: 'wanted', label: '求购' },
  { key: 'publish', label: '发布' },
  { key: 'messages', label: '消息' },
  { key: 'profile', label: '我的' }
]
const myTabs = [
  { key: 'products', label: '我的商品' },
  { key: 'wanted', label: '我的求购' },
  { key: 'orders', label: '我的订单' }
]

const adminTabs = [
  { key: 'authReview', label: '认证审核', owner: 'A' },
  { key: 'productReview', label: '商品审核', owner: 'C' },
  { key: 'reports', label: '举报处理', owner: 'C' },
  { key: 'users', label: '用户管理', owner: 'C' },
  { key: 'categories', label: '分类管理', owner: 'C' }
]

const authReviewTabs = [
  { key: 'PENDING', label: '待审核' },
  { key: 'APPROVED', label: '已通过' },
  { key: 'REJECTED', label: '未通过' }
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
const passwordRequirements = computed(() => [
  { key: 'length', label: '不少于 8 个字符', passed: registerForm.password.length >= 8 },
  { key: 'lower', label: '包含小写字母', passed: /[a-z]/.test(registerForm.password) },
  { key: 'upper', label: '包含大写字母', passed: /[A-Z]/.test(registerForm.password) },
  { key: 'number', label: '包含数字', passed: /\d/.test(registerForm.password) }
])
const isRegisterPasswordValid = computed(() => passwordRequirements.value.every((item) => item.passed))
const confirmPasswordError = computed(() => {
  if (!registerForm.confirmPassword) return ''
  return registerForm.password === registerForm.confirmPassword ? '' : '两次输入的密码不一致'
})

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
const totalUnreadCount = computed(() => conversations.value.reduce((sum, item) => sum + (item.unreadCount || 0), 0))

function showNotice({ type = 'info', title = '提示', content = '' }) {
  noticeDialog.visible = true
  noticeDialog.type = type
  noticeDialog.title = title
  noticeDialog.content = content
}

function closeNoticeDialog() {
  noticeDialog.visible = false
  noticeDialog.type = 'info'
  noticeDialog.title = ''
  noticeDialog.content = ''
}
function clearNotice() {
  message.value = ''
  errorMessage.value = ''
}

function resetLoginForm() {
  loginForm.username = ''
  loginForm.password = ''
}

function resetOrderForm() {
  orderForm.meetTime = ''
  orderForm.meetLocation = ''
  orderForm.remark = ''
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
  resetOrderForm()
}

function clearCurrentSession() {
  sessionStorage.removeItem(TOKEN_KEY)
  updateCurrentUser(null)
  page.value = 'auth'
  mode.value = 'login'
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
    loadProducts()
    loadWanted()
    loadMyWanted()
    loadReportReasons()
    loadConversations()
  }
}

async function apiRequest(path, options = {}) {
  const token = sessionStorage.getItem(TOKEN_KEY)
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
    const requestError = new Error(result?.message || `请求失败：${response.status}`)
    if (result?.message?.includes('请先登录')) {
      clearCurrentSession()
    }
    throw requestError
  }
  return result.data
}

async function loadMe() {
  const token = sessionStorage.getItem(TOKEN_KEY)
  if (!token) return

  try {
    const user = await apiRequest('/users/me')
    updateCurrentUser(user)
    enterAppByRole()
  } catch (error) {
    clearCurrentSession()
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
    showNotice({ type: 'error', title: '操作失败', content: error.message })
  }
}

async function loadMyProducts() {
  const token = sessionStorage.getItem(TOKEN_KEY)
  if (!token || isAdmin.value) return
  try {
    myProducts.value = await apiRequest('/products/mine')
  } catch (error) {
    showNotice({ type: 'error', title: '操作失败', content: error.message })
  }
}

function buildQuery(values, page) {
  const params = new URLSearchParams({ page: String(page), size: '6' })
  Object.entries(values).forEach(([key, value]) => {
    if (value !== '' && value !== null && value !== undefined) params.set(key, value)
  })
  return params.toString()
}

async function loadProducts(page = 0) {
  try {
    const data = await apiRequest(`/products?${buildQuery(productFilters, page)}`)
    products.value = data.content
    Object.assign(productPage, data)
  } catch (error) {
    products.value = []
    Object.assign(productPage, { page: 0, totalPages: 0, totalElements: 0, first: true, last: true })
    showNotice({ type: 'error', title: '操作失败', content: error.message })
  }
}

function resetProductFilters() {
  Object.assign(productFilters, { keyword: '', categoryId: '', campusId: '', conditionLevel: '', minPrice: '', maxPrice: '' })
  loadProducts(0)
}

async function openProductDetail(product) {
  clearNotice()
  resetOrderForm()
  try {
    selectedProduct.value = await apiRequest(`/products/${product.id}`)
    commentForm.content = ''
    await loadProductComments(product.id)
  } catch (error) {
    showNotice({ type: 'error', title: '操作失败', content: error.message })
  }
}

async function loadProductComments(productId) {
  try {
    const data = await apiRequest(`/comments?productId=${productId}&page=0&size=20`)
    productComments.value = data.content
  } catch (error) {
    productComments.value = []
  }
}

async function submitComment() {
  if (!selectedProduct.value) return
  if (!commentForm.content.trim()) {
    showNotice({ type: 'error', title: '提示', content: '请输入留言内容' })
    return
  }
  loading.value = true
  try {
    await apiRequest('/comments', {
      method: 'POST',
      body: JSON.stringify({ productId: selectedProduct.value.id, content: commentForm.content.trim() })
    })
    commentForm.content = ''
    await loadProductComments(selectedProduct.value.id)
    message.value = '留言成功'
  } catch (error) {
    showNotice({ type: 'error', title: '操作失败', content: error.message })
  } finally {
    loading.value = false
  }
}

async function loadConversations() {
  try {
    conversations.value = await apiRequest('/messages/conversations')
  } catch (error) {
    conversations.value = []
    showNotice({ type: 'error', title: '操作失败', content: error.message })
  }
}

async function openConversation(conversation) {
  selectedConversation.value = conversation
  pendingChat.value = null
  chatForm.content = ''
  await loadChatMessages(conversation.id)
  await loadConversations()
}

async function loadChatMessages(conversationId) {
  try {
    chatMessages.value = await apiRequest(`/messages/conversations/${conversationId}`)
  } catch (error) {
    chatMessages.value = []
    showNotice({ type: 'error', title: '操作失败', content: error.message })
  }
}

async function sendChatMessage() {
  if (!chatForm.content.trim()) return
  if (!selectedConversation.value && !pendingChat.value) return
  loading.value = true
  try {
    let conversationId = selectedConversation.value?.id
    if (pendingChat.value) {
      const conversation = await apiRequest('/messages/conversations', {
        method: 'POST',
        body: JSON.stringify({
          peerUserId: pendingChat.value.peerUserId,
          productId: pendingChat.value.productId,
          wantedId: pendingChat.value.wantedId
        })
      })
      pendingChat.value = null
      selectedConversation.value = conversation
      conversationId = conversation.id
    }
    await apiRequest(`/messages/conversations/${conversationId}/messages`, {
      method: 'POST',
      body: JSON.stringify({ content: chatForm.content.trim() })
    })
    chatForm.content = ''
    await Promise.all([loadChatMessages(conversationId), loadConversations()])
  } catch (error) {
    showNotice({ type: 'error', title: '操作失败', content: error.message })
  } finally {
    loading.value = false
  }
}

function openPendingChat({ peerUser, productId = null, wantedId = null }) {
  if (!peerUser || peerUser.id === currentUser.id) {
    showNotice({ type: 'error', title: '提示', content: '不能和自己私聊' })
    return
  }
  pendingChat.value = { peerUserId: peerUser.id, peerUser, productId, wantedId }
  selectedConversation.value = null
  chatMessages.value = []
  chatForm.content = ''
  activeStudentTab.value = 'messages'
}

function startChatWithSeller() {
  if (!selectedProduct.value?.seller) return
  openPendingChat({
    peerUser: selectedProduct.value.seller,
    productId: selectedProduct.value.id
  })
  selectedProduct.value = null
}

function startChatWithPublisher() {
  if (!selectedWanted.value?.publisher) return
  openPendingChat({
    peerUser: selectedWanted.value.publisher,
    wantedId: selectedWanted.value.id
  })
  selectedWanted.value = null
}

async function loadMyOrders() {
  try {
    myOrders.value = await apiRequest('/orders')
  } catch (error) {
    myOrders.value = []
    showNotice({ type: 'error', title: '操作失败', content: error.message })
  }
}

async function createOrderFromProduct() {
  if (!selectedProduct.value) return
  loading.value = true
  try {
    const payload = {
      productId: selectedProduct.value.id,
      meetTime: orderForm.meetTime || null,
      meetLocation: orderForm.meetLocation || null,
      remark: orderForm.remark || null,
      conversationId: selectedConversation.value?.id || null
    }
    await apiRequest('/orders', { method: 'POST', body: JSON.stringify(payload) })
    message.value = '订单创建成功，可在“我的订单”查看'
    resetOrderForm()
    selectedProduct.value = null
    activeStudentTab.value = 'profile'
    activeMyTab.value = 'orders'
    await loadMyOrders()
  } catch (error) {
    showNotice({ type: 'error', title: '操作失败', content: error.message })
  } finally {
    loading.value = false
  }
}

async function updateOrderStatus(order, status) {
  loading.value = true
  try {
    await apiRequest(`/orders/${order.id}/status`, {
      method: 'PUT',
      body: JSON.stringify({ status })
    })
    message.value = '订单状态已更新'
    await Promise.all([loadMyOrders(), loadProducts(productPage.page), loadMyProducts()])
  } catch (error) {
    showNotice({ type: 'error', title: '操作失败', content: error.message })
  } finally {
    loading.value = false
  }
}

async function loadReportReasons() {
  try {
    reportReasons.value = await apiRequest('/reports/reasons')
  } catch (error) {
    reportReasons.value = []
  }
}

function openReportDialog(targetType, targetId) {
  reportDialog.visible = true
  reportDialog.targetType = targetType
  reportDialog.targetId = targetId
  reportDialog.reasonId = reportReasons.value[0]?.id || ''
  reportDialog.description = ''
}

function closeReportDialog() {
  reportDialog.visible = false
  reportDialog.targetType = ''
  reportDialog.targetId = null
  reportDialog.reasonId = ''
  reportDialog.description = ''
}

async function submitReport() {
  if (!reportDialog.reasonId) {
    showNotice({ type: 'error', title: '提示', content: '请选择举报原因' })
    return
  }
  loading.value = true
  try {
    await apiRequest('/reports', {
      method: 'POST',
      body: JSON.stringify({
        reasonId: Number(reportDialog.reasonId),
        targetType: reportDialog.targetType,
        targetId: reportDialog.targetId,
        description: reportDialog.description || null
      })
    })
    closeReportDialog()
    message.value = '举报已提交，等待管理员处理'
  } catch (error) {
    showNotice({ type: 'error', title: '操作失败', content: error.message })
  } finally {
    loading.value = false
  }
}

async function loadPendingProducts() {
  adminLoading.value = true
  try {
    const data = await apiRequest(`/admin/products?status=${productReviewStatus.value}&page=0&size=20`)
    pendingProducts.value = data.content
    if (productReviewStatus.value === 'PENDING') {
      const entries = await Promise.all(pendingProducts.value.map(async (product) => {
        try {
          const logs = await apiRequest(`/admin/ai-audit/products/${product.id}`)
          return [product.id, logs[0] || null]
        } catch (error) {
          return [product.id, null]
        }
      }))
      aiAuditMap.value = Object.fromEntries(entries)
    } else {
      aiAuditMap.value = {}
    }
  } catch (error) {
    pendingProducts.value = []
    aiAuditMap.value = {}
    showNotice({ type: 'error', title: '操作失败', content: error.message })
  } finally {
    adminLoading.value = false
  }
}

async function auditProduct(product, status) {
  adminLoading.value = true
  try {
    await apiRequest(`/admin/products/${product.id}/audit`, {
      method: 'PUT',
      body: JSON.stringify({ status, auditRemark: status === 'PUBLISHED' ? '审核通过' : '审核未通过' })
    })
    message.value = status === 'PUBLISHED' ? '商品审核通过' : '商品已拒绝'
    await loadPendingProducts()
  } catch (error) {
    showNotice({ type: 'error', title: '操作失败', content: error.message })
  } finally {
    adminLoading.value = false
  }
}

async function loadAiAuditLogs(productId) {
  try {
    aiAuditLogs.value = await apiRequest(`/admin/ai-audit/products/${productId}`)
    if (aiAuditLogs.value[0]) {
      aiAuditMap.value[productId] = aiAuditLogs.value[0]
    }
  } catch (error) {
    aiAuditLogs.value = []
  }
}

async function runAiAudit(product) {
  adminLoading.value = true
  try {
    const result = await apiRequest(`/ai-audit/products/${product.id}`, { method: 'POST' })
    aiAuditMap.value[product.id] = result
    aiAuditLogs.value = [result]
    message.value = `关键词审核完成：${getAiSuggestionLabel(result.suggestion)}`
  } catch (error) {
    showNotice({ type: 'error', title: '操作失败', content: error.message })
  } finally {
    adminLoading.value = false
  }
}

function getAiSuggestionLabel(value) {
  return {
    PASS: '建议通过',
    REVIEW: '建议人工复核',
    REJECT: '建议拒绝'
  }[value] || value
}

function getAiRiskClass(value) {
  return `ai-risk-${String(value || 'none').toLowerCase()}`
}

async function loadAdminReports() {
  adminLoading.value = true
  try {
    const data = await apiRequest(`/admin/reports?status=${reportReviewStatus.value}&page=0&size=20`)
    adminReports.value = data.content
  } catch (error) {
    adminReports.value = []
    showNotice({ type: 'error', title: '操作失败', content: error.message })
  } finally {
    adminLoading.value = false
  }
}

async function handleReport(report, status) {
  adminLoading.value = true
  try {
    await apiRequest(`/admin/reports/${report.id}/handle`, {
      method: 'PUT',
      body: JSON.stringify({
        status,
        handleResult: status === 'RESOLVED' ? '举报成立，商品已下架' : '举报不成立，商品继续展示'
      })
    })
    message.value = status === 'RESOLVED' ? '举报已处理，相关商品已下架' : '举报已驳回，商品不受影响'
    await loadAdminReports()
  } catch (error) {
    showNotice({ type: 'error', title: '操作失败', content: error.message })
  } finally {
    adminLoading.value = false
  }
}

async function loadAdminUsers() {
  adminLoading.value = true
  try {
    const keyword = adminUserKeyword.value.trim()
    const query = keyword ? `?keyword=${encodeURIComponent(keyword)}&page=0&size=20` : '?page=0&size=20'
    const data = await apiRequest(`/admin/users${query}`)
    adminUsers.value = data.content
  } catch (error) {
    adminUsers.value = []
    showNotice({ type: 'error', title: '操作失败', content: error.message })
  } finally {
    adminLoading.value = false
  }
}

async function updateUserStatus(user, status) {
  adminLoading.value = true
  try {
    await apiRequest(`/admin/users/${user.id}/status`, {
      method: 'PUT',
      body: JSON.stringify({ status })
    })
    message.value = status === 'BANNED' ? '用户已封禁' : '用户已解封'
    await loadAdminUsers()
  } catch (error) {
    showNotice({ type: 'error', title: '操作失败', content: error.message })
  } finally {
    adminLoading.value = false
  }
}

async function loadAdminCategories() {
  adminLoading.value = true
  try {
    adminCategories.value = await apiRequest('/admin/categories')
  } catch (error) {
    adminCategories.value = []
    showNotice({ type: 'error', title: '操作失败', content: error.message })
  } finally {
    adminLoading.value = false
  }
}

function resetCategoryForm() {
  categoryForm.id = null
  categoryForm.name = ''
}

function editCategory(category) {
  categoryForm.id = category.id
  categoryForm.name = category.name
}

async function submitCategoryForm() {
  if (!categoryForm.name.trim()) {
    showNotice({ type: 'error', title: '提示', content: '请输入分类名称' })
    return
  }
  adminLoading.value = true
  try {
    const payload = { name: categoryForm.name.trim() }
    if (categoryForm.id) {
      await apiRequest(`/admin/categories/${categoryForm.id}`, { method: 'PUT', body: JSON.stringify(payload) })
      message.value = '分类已更新'
    } else {
      await apiRequest('/admin/categories', { method: 'POST', body: JSON.stringify(payload) })
      message.value = '分类已创建'
    }
    resetCategoryForm()
    await Promise.all([loadAdminCategories(), loadDictionaries()])
  } catch (error) {
    showNotice({ type: 'error', title: '操作失败', content: error.message })
  } finally {
    adminLoading.value = false
  }
}

function askDeleteCategory(category) {
  confirmDialog.visible = true
  confirmDialog.title = '删除分类'
  confirmDialog.content = `确定删除分类「${category.name}」吗？若分类下仍有商品将无法删除。`
  confirmDialog.confirmText = '删除'
  confirmDialog.action = () => deleteCategory(category)
}

async function deleteCategory(category) {
  adminLoading.value = true
  try {
    await apiRequest(`/admin/categories/${category.id}`, { method: 'DELETE' })
    message.value = '分类已删除'
    if (categoryForm.id === category.id) resetCategoryForm()
    await Promise.all([loadAdminCategories(), loadDictionaries()])
  } catch (error) {
    showNotice({ type: 'error', title: '操作失败', content: error.message })
  } finally {
    adminLoading.value = false
  }
}

async function loadWanted(page = 0) {
  clearNotice()
  const minBudget = wantedFilters.minBudget === '' ? null : Number(wantedFilters.minBudget)
  const maxBudget = wantedFilters.maxBudget === '' ? null : Number(wantedFilters.maxBudget)
  if ((minBudget !== null && minBudget < 0) || (maxBudget !== null && maxBudget < 0)) {
    wantedItems.value = []
    Object.assign(wantedPage, { page: 0, totalPages: 0, totalElements: 0, first: true, last: true })
    errorMessage.value = '预算金额不能小于 0'
    return
  }
  if (minBudget !== null && maxBudget !== null && minBudget > maxBudget) {
    wantedItems.value = []
    Object.assign(wantedPage, { page: 0, totalPages: 0, totalElements: 0, first: true, last: true })
    errorMessage.value = '最低预算不能高于最高预算'
    return
  }
  try {
    const data = await apiRequest(`/wanted?${buildQuery(wantedFilters, page)}`)
    wantedItems.value = data.content
    Object.assign(wantedPage, data)
  } catch (error) {
    wantedItems.value = []
    Object.assign(wantedPage, { page: 0, totalPages: 0, totalElements: 0, first: true, last: true })
    showNotice({ type: 'error', title: '操作失败', content: error.message })
  }
}

async function loadMyWanted() {
  if (!sessionStorage.getItem(TOKEN_KEY) || isAdmin.value) return
  try {
    myWanted.value = await apiRequest('/wanted/mine')
  } catch (error) {
    showNotice({ type: 'error', title: '操作失败', content: error.message })
  }
}

function resetWantedFilters() {
  Object.assign(wantedFilters, { keyword: '', campusId: '', expectCondition: '', minBudget: '', maxBudget: '' })
  loadWanted(0)
}

function resetWantedForm() {
  editingWantedId.value = null
  showWantedForm.value = false
  Object.assign(wantedForm, { itemName: '', budget: '', expectCondition: '', campusId: '', description: '' })
}

function startWantedCreate() {
  clearNotice()
  if (!isApprovedStudent.value) {
    openAuthPage()
    return
  }
  resetWantedForm()
  showWantedForm.value = true
}

function editWanted(item) {
  clearNotice()
  editingWantedId.value = item.id
  showWantedForm.value = true
  Object.assign(wantedForm, {
    itemName: item.itemName,
    budget: item.budget,
    expectCondition: item.expectCondition,
    campusId: item.campusId ? String(item.campusId) : '',
    description: item.description || ''
  })
  activeStudentTab.value = 'wanted'
}

async function submitWanted() {
  clearNotice()
  if (!wantedForm.itemName || !wantedForm.budget || !wantedForm.expectCondition) {
    errorMessage.value = '请填写物品名称、预算和期望成色'
    return
  }
  if (Number(wantedForm.budget) <= 0) {
    errorMessage.value = '预算必须大于 0'
    return
  }
  loading.value = true
  try {
    const payload = {
      itemName: wantedForm.itemName,
      budget: Number(wantedForm.budget),
      expectCondition: wantedForm.expectCondition,
      campusId: wantedForm.campusId ? Number(wantedForm.campusId) : null,
      description: wantedForm.description || null
    }
    await apiRequest(editingWantedId.value ? `/wanted/${editingWantedId.value}` : '/wanted', {
      method: editingWantedId.value ? 'PUT' : 'POST',
      body: JSON.stringify(payload)
    })
    message.value = editingWantedId.value ? '求购信息已更新。' : '求购发布成功。'
    resetWantedForm()
    activeStudentTab.value = 'profile'
    activeMyTab.value = 'wanted'
    await Promise.all([loadWanted(0), loadMyWanted()])
  } catch (error) {
    showNotice({ type: 'error', title: '操作失败', content: error.message })
  } finally {
    loading.value = false
  }
}

async function changeWantedStatus(item, action) {
  clearNotice()
  loading.value = true
  try {
    await apiRequest(`/wanted/${item.id}/${action}`, { method: 'PUT' })
    message.value = action === 'match' ? '已标记为找到卖家。' : '求购已关闭。'
    await Promise.all([loadWanted(wantedPage.page), loadMyWanted()])
  } catch (error) {
    showNotice({ type: 'error', title: '操作失败', content: error.message })
  } finally {
    loading.value = false
  }
}

async function openWantedDetail(item) {
  clearNotice()
  try {
    selectedWanted.value = await apiRequest(`/wanted/${item.id}`)
  } catch (error) {
    showNotice({ type: 'error', title: '操作失败', content: error.message })
  }
}

function avatarText(user) {
  return (user?.nickname || '校').slice(0, 1)
}

function highlightedSegments(value, keyword) {
  const text = String(value || '')
  const query = String(keyword || '').trim()
  if (!text || !query) return [{ text, highlighted: false }]

  const matchedIndexes = matchPinyin(text, query)
  if (!Array.isArray(matchedIndexes) || matchedIndexes.length === 0) {
    return [{ text, highlighted: false }]
  }

  const highlighted = new Set(matchedIndexes)
  const segments = []
  for (let index = 0; index < text.length; index += 1) {
    const isHighlighted = highlighted.has(index)
    const previous = segments[segments.length - 1]
    if (previous && previous.highlighted === isHighlighted) {
      previous.text += text[index]
    } else {
      segments.push({ text: text[index], highlighted: isHighlighted })
    }
  }
  return segments
}

async function openPublicProfile(userId) {
  if (!userId) return
  selectedProduct.value = null
  selectedWanted.value = null
  profileLoading.value = true
  publicProfile.value = { id: userId, nickname: '正在加载...' }
  try {
    const [profile, productData, wantedData] = await Promise.all([
      apiRequest(`/users/${userId}/public`),
      apiRequest(`/products?sellerId=${userId}&page=0&size=50`),
      apiRequest(`/wanted?buyerId=${userId}&page=0&size=50`)
    ])
    publicProfile.value = profile
    profileProducts.value = productData.content
    profileWanted.value = wantedData.content
  } catch (error) {
    publicProfile.value = null
    showNotice({ type: 'error', title: '操作失败', content: error.message })
  } finally {
    profileLoading.value = false
  }
}

function closePublicProfile() {
  publicProfile.value = null
  profileProducts.value = []
  profileWanted.value = []
}

function selectStudentTab(tabKey) {
  activeStudentTab.value = tabKey
  clearNotice()
  if (tabKey === 'home') loadProducts(productPage.page)
  if (tabKey === 'wanted') Promise.all([loadWanted(wantedPage.page), loadMyWanted()])
  if (tabKey === 'messages') {
    loadConversations()
  } else if (!isAdmin.value && currentUser.id) {
    loadConversations()
  }
}

function switchMode(nextMode) {
  mode.value = nextMode
  clearNotice()
}

function validatePasswordStrength(password) {
  if (password.length < 8) return '密码不少于 8 个字符'
  if (!/[a-z]/.test(password) || !/[A-Z]/.test(password) || !/\d/.test(password)) {
    return '密码至少包含大写字母、小写字母和数字'
  }
  return ''
}

function friendlyLoginError(messageText) {
  if (messageText.includes('用户名或密码')) {
    return '用户名或密码错误，请检查账号密码；如果还没有账号，请先注册。'
  }
  return messageText
}

async function handleLogin() {
  clearNotice()
  if (!loginForm.username || !loginForm.password) {
    showNotice({ type: 'error', title: '登录失败', content: '请输入用户名和密码。' })
    return
  }

  loading.value = true
  try {
    const data = await apiRequest('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username: loginForm.username, password: loginForm.password })
    })
    sessionStorage.setItem(TOKEN_KEY, data.token)
    updateCurrentUser(data.user)
    enterAppByRole()
  } catch (error) {
    showNotice({ type: 'error', title: '登录失败', content: friendlyLoginError(error.message) })
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  clearNotice()
  if (!registerForm.username || !registerForm.password || !registerForm.confirmPassword) {
    showNotice({ type: 'error', title: '注册失败', content: '请填写用户名、密码和确认密码。' })
    return
  }
  const passwordError = validatePasswordStrength(registerForm.password)
  if (passwordError || registerForm.password !== registerForm.confirmPassword) {
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
    showNotice({ type: 'success', title: '注册成功', content: '请用刚才的账号登录。' })
  } catch (error) {
    showNotice({ type: 'error', title: '注册失败', content: error.message })
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
    showNotice({ type: 'error', title: '操作失败', content: error.message })
  } finally {
    loading.value = false
  }
}

async function loadAuthApplications() {
  if (!isAdmin.value) return
  adminLoading.value = true
  try {
    authApplications.value = await apiRequest(`/admin/auth-applications?status=${authReviewStatus.value}`)
  } catch (error) {
    showNotice({ type: 'error', title: '操作失败', content: error.message })
  } finally {
    adminLoading.value = false
  }
}

function switchAuthReviewStatus(status) {
  authReviewStatus.value = status
  clearNotice()
  loadAuthApplications()
}

function selectAdminTab(tabKey) {
  activeAdminTab.value = tabKey
  if (tabKey === 'authReview') {
    loadAuthApplications()
  } else if (tabKey === 'productReview') {
    loadPendingProducts()
  } else if (tabKey === 'reports') {
    loadAdminReports()
  } else if (tabKey === 'users') {
    loadAdminUsers()
  } else if (tabKey === 'categories') {
    loadAdminCategories()
  }
}

function openAuthRemarkDialog(application, status = application.authStatus) {
  clearNotice()
  authRemarkDialog.visible = true
  authRemarkDialog.application = application
  authRemarkDialog.status = status
  authRemarkDialog.remark = application.authRemark || ''
}

function closeAuthRemarkDialog() {
  authRemarkDialog.visible = false
  authRemarkDialog.application = null
  authRemarkDialog.status = ''
  authRemarkDialog.remark = ''
}

async function submitAuthRemarkReview() {
  if (!authRemarkDialog.application || !authRemarkDialog.status) return
  await reviewAuth(authRemarkDialog.application, authRemarkDialog.status, authRemarkDialog.remark)
  closeAuthRemarkDialog()
}
async function reviewAuth(application, status, remark = null) {
  clearNotice()
  adminLoading.value = true
  try {
    await apiRequest(`/admin/auth-applications/${application.id}/review`, {
      method: 'POST',
      body: JSON.stringify({
        authStatus: status,
        authRemark: remark && remark.trim() ? remark.trim() : (status === 'APPROVED' ? '实名认证审核通过' : '实名认证审核未通过')
      })
    })
    message.value = status === 'APPROVED' ? '已将该学生认证改为通过。' : '已将该学生认证改为不通过。'
    await loadAuthApplications()
  } catch (error) {
    showNotice({ type: 'error', title: '操作失败', content: error.message })
  } finally {
    adminLoading.value = false
  }
}

function validateProductForm() {
  if (!productForm.title) return '请填写商品标题'
  if (!productForm.price) return '请填写商品价格'
  if (Number(productForm.price) <= 0) return '商品价格必须大于 0'
  if (!productForm.categoryId) return '请选择商品分类'
  if (!productForm.conditionLevel) return '请选择新旧程度'
  if (!productForm.campusId) return '请选择校区'
  if (!productForm.tradeMethod) return '请选择交易方式'
  if (productForm.imageUrls.length === 0) return '请至少上传一张商品图片'
  if (!productForm.description) return '请填写商品描述'
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
  productForm.imageUrls.splice(0)
  productForm.description = ''
}

async function uploadProductImage(event) {
  const files = Array.from(event.target.files || [])
  await addProductImages(files)
  event.target.value = ''
}

async function uploadImageFile(file) {
  if (!file) return ''
  if (!file.type.startsWith('image/')) {
    showNotice({ type: 'error', title: '图片格式不支持', content: '只能上传图片文件。' })
    return ''
  }
  if (file.size > 5 * 1024 * 1024) {
    showNotice({ type: 'error', title: '图片过大', content: '图片大小不能超过 5MB。' })
    return ''
  }

  const formData = new FormData()
  formData.append('file', file)
  const token = sessionStorage.getItem(TOKEN_KEY)
  const response = await fetch(`${API_BASE}/uploads/images`, {
    method: 'POST',
    headers: token ? { Authorization: `Bearer ${token}` } : {},
    body: formData
  })
  const result = await response.json().catch(() => null)
  if (!response.ok || !result || result.code !== 0) {
    throw new Error(result?.message || `上传失败：${response.status}`)
  }
  return result.data.url
}

async function addProductImages(files) {
  clearNotice()
  if (!files.length) return
  const remainingCount = 9 - productForm.imageUrls.length
  if (remainingCount <= 0) {
    showNotice({ type: 'error', title: '图片数量已满', content: '商品图片最多上传 9 张。' })
    return
  }

  imageUploading.value = true
  try {
    const selectedFiles = files.slice(0, remainingCount)
    const uploadedUrls = []
    for (const file of selectedFiles) {
      const url = await uploadImageFile(file)
      if (url) uploadedUrls.push(url)
    }
    if (uploadedUrls.length) {
      productForm.imageUrls.push(...uploadedUrls)
      productForm.coverUrl = productForm.imageUrls[0] || ''
      showNotice({
        type: 'success',
        title: '图片上传成功',
        content: `已添加 ${uploadedUrls.length} 张图片，可以点击缩略图查看。`
      })
    }
    if (files.length > selectedFiles.length) {
      showNotice({ type: 'error', title: '图片数量已满', content: '商品图片最多上传 9 张，多出的图片没有上传。' })
    }
  } catch (error) {
    showNotice({ type: 'error', title: '操作失败', content: error.message })
  } finally {
    imageUploading.value = false
  }
}

async function handleImagePaste(event) {
  const imageFiles = Array.from(event.clipboardData?.files || []).filter((file) => file.type.startsWith('image/'))
  if (imageFiles.length) {
    event.preventDefault()
    await addProductImages(imageFiles)
  }
}
function removeProductImage(index) {
  productForm.imageUrls.splice(index, 1)
  productForm.coverUrl = productForm.imageUrls[0] || ''
}
function clearProductImages() {
  productForm.imageUrls.splice(0)
  productForm.coverUrl = ''
}

async function submitProduct() {
  clearNotice()
  const validationError = validateProductForm()
  if (validationError) {
    showNotice({ type: 'error', title: '请补全商品信息', content: validationError })
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
      coverUrl: productForm.imageUrls[0] || null,
      imageUrls: [...productForm.imageUrls],
      description: productForm.description
    }

    if (editingProductId.value) {
      await apiRequest(`/products/${editingProductId.value}`, {
        method: 'PUT',
        body: JSON.stringify(payload)
      })
      showNotice({ type: 'success', title: '保存成功', content: '商品已更新，并重新提交审核。' })
    } else {
      await apiRequest('/products', {
        method: 'POST',
        body: JSON.stringify(payload)
      })
      showNotice({ type: 'success', title: '发布成功', content: '商品已发布，等待管理员审核。' })
    }

    resetProductForm()
    await loadMyProducts()
  } catch (error) {
    showNotice({ type: 'error', title: '操作失败', content: error.message })
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
  const imageUrls = productImageUrls(product)
  productForm.coverUrl = imageUrls[0] || ''
  productForm.imageUrls.splice(0, productForm.imageUrls.length, ...imageUrls)
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
    showNotice({ type: 'error', title: '操作失败', content: error.message })
  } finally {
    loading.value = false
  }
}

function askDeleteProduct(product) {
  openConfirmDialog({
    title: '确认删除商品？',
    content: `删除后，“${product.title}”将不再展示，也不能恢复。相关订单和记录仍会保留。`,
    confirmText: '确认删除',
    action: () => deleteProduct(product)
  })
}

async function deleteProduct(product) {
  clearNotice()
  loading.value = true
  try {
    await apiRequest(`/products/${product.id}`, { method: 'DELETE' })
    message.value = '商品已删除。'
    await Promise.all([loadMyProducts(), loadProducts(productPage.page)])
  } catch (error) {
    showNotice({ type: 'error', title: '操作失败', content: error.message })
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
    showNotice({ type: 'error', title: '操作失败', content: error.message })
  } finally {
    loading.value = false
  }
}

function productImageUrls(product) {
  if (Array.isArray(product?.imageUrls) && product.imageUrls.length > 0) return product.imageUrls
  return product?.coverUrl ? [product.coverUrl] : []
}

function firstProductImage(product) {
  return productImageUrls(product)[0] || ''
}

function extraProductImageCount(product) {
  return Math.max(productImageUrls(product).length - 4, 0)
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

function getAuthStatusLabel(value) {
  return authReviewTabs.find((item) => item.key === value)?.label || value
}
function getProductStatusLabel(value) {
  return {
    PENDING: '待审核',
    PUBLISHED: '已发布',
    OFF_SHELF: '已下架',
    REJECTED: '审核未通过',
    DELETED: '已删除'
  }[value] || value
}

function getWantedStatusLabel(value) {
  return {
    OPEN: '求购中',
    MATCHED: '已找到卖家',
    CLOSED: '已关闭'
  }[value] || value
}

function getOrderStatusLabel(value) {
  return {
    PENDING_CHAT: '待沟通',
    PENDING_TRADE: '待交易',
    COMPLETED: '已完成'
  }[value] || value
}

function logout() {
  clearCurrentSession()
  page.value = 'auth'
  mode.value = 'login'
  clearNotice()
  resetLoginForm()
  resetOrderForm()
  myProducts.value = []
  products.value = []
  wantedItems.value = []
  myWanted.value = []
  conversations.value = []
  selectedConversation.value = null
  pendingChat.value = null
  chatMessages.value = []
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

      <form v-if="mode === 'login'" class="form" autocomplete="off" @submit.prevent="handleLogin">
        <h2>{{ pageTitle }}</h2>
        <label>用户名<input v-model.trim="loginForm.username" type="text" autocomplete="off" placeholder="请输入用户名" /></label>
        <label>密码<input v-model="loginForm.password" type="password" autocomplete="off" placeholder="请输入密码" /></label>
        <button class="primary-button" type="submit" :disabled="loading">{{ loading ? '登录中...' : '登录' }}</button>
        <p class="hint">管理员测试账号：admin / password。学生账号可先注册再登录。</p>
      </form>

      <form v-else class="form" @submit.prevent="handleRegister">
        <h2>{{ pageTitle }}</h2>
        <label>用户名<input v-model.trim="registerForm.username" type="text" autocomplete="username" placeholder="设置登录用户名" /></label>
        <label>昵称<input v-model.trim="registerForm.nickname" type="text" placeholder="例如：小鱼同学" /></label>
        <label>手机号<input v-model.trim="registerForm.phone" type="tel" autocomplete="tel" placeholder="选填" /></label>
        <label class="field-group">密码<input v-model="registerForm.password" type="password" autocomplete="new-password" placeholder="设置密码" /></label>
        <div class="password-rule-panel" aria-live="polite">
          <p>密码要求</p>
          <span v-for="item in passwordRequirements" :key="item.key" :class="['password-rule', { passed: item.passed }]">{{ item.passed ? '✓' : '·' }} {{ item.label }}</span>
        </div>
        <label class="field-group">确认密码<input v-model="registerForm.confirmPassword" type="password" autocomplete="new-password" placeholder="再次输入密码" /></label>
        <p v-if="confirmPasswordError" class="field-error">{{ confirmPasswordError }}</p>
        <button class="primary-button" type="submit" :disabled="loading || !isRegisterPasswordValid || !!confirmPasswordError">{{ loading ? '注册中...' : '注册学生账号' }}</button>      </form>

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
          <p class="intro">搜索校内已审核发布的闲置商品。</p>
          <form class="filter-card" @submit.prevent="loadProducts(0)">
            <input v-model.trim="productFilters.keyword" type="search" placeholder="搜索标题或描述" />
            <div class="filter-grid">
              <select v-model="productFilters.categoryId"><option value="">全部分类</option><option v-for="item in categories" :key="item.id" :value="item.id">{{ item.name }}</option></select>
              <select v-model="productFilters.campusId"><option value="">全部校区</option><option v-for="item in campuses" :key="item.id" :value="item.id">{{ item.name }}</option></select>
              <select v-model="productFilters.conditionLevel"><option value="">全部成色</option><option v-for="item in conditionOptions" :key="item.value" :value="item.value">{{ item.label }}</option></select>
              <div class="price-range"><input v-model="productFilters.minPrice" type="number" inputmode="decimal" min="0" step="0.01" placeholder="最低价" /><span>—</span><input v-model="productFilters.maxPrice" type="number" inputmode="decimal" min="0" step="0.01" placeholder="最高价" /></div>
            </div>
            <div class="inline-actions"><button class="primary-button" type="submit">搜索</button><button class="secondary-button" type="button" @click="resetProductFilters">重置</button></div>
          </form>
          <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>
          <div v-if="products.length === 0" class="empty-state">暂无符合条件的已发布商品</div>
          <div class="card-grid">
            <article v-for="product in products" :key="product.id" class="market-card" @click="openProductDetail(product)">
              <img v-if="firstProductImage(product)" :src="firstProductImage(product)" :alt="product.title" />
              <div v-else class="image-placeholder">闲</div>
              <div class="market-info"><strong><template v-for="(segment, index) in highlightedSegments(product.title, productFilters.keyword)" :key="index"><mark v-if="segment.highlighted" class="search-highlight">{{ segment.text }}</mark><template v-else>{{ segment.text }}</template></template></strong><b>￥{{ product.price }}</b><p v-if="productFilters.keyword" class="search-summary"><template v-for="(segment, index) in highlightedSegments(product.description, productFilters.keyword)" :key="index"><mark v-if="segment.highlighted" class="search-highlight">{{ segment.text }}</mark><template v-else>{{ segment.text }}</template></template></p><p>{{ getCategoryName(product.categoryId) }} · {{ getCampusName(product.campusId) }}</p><span>{{ getConditionLabel(product.conditionLevel) }} · {{ getTradeMethodLabel(product.tradeMethod) }}</span><button v-if="product.seller" class="publisher-mini" type="button" @click.stop="openPublicProfile(product.seller.id)"><img v-if="product.seller.avatarUrl" :src="product.seller.avatarUrl" alt="" /><span v-else>{{ avatarText(product.seller) }}</span><em><template v-for="(segment, index) in highlightedSegments(product.seller.nickname, productFilters.keyword)" :key="index"><mark v-if="segment.highlighted" class="search-highlight">{{ segment.text }}</mark><template v-else>{{ segment.text }}</template></template></em></button></div>
            </article>
          </div>
          <div class="pagination"><button :disabled="productPage.first || productPage.totalPages === 0" @click="loadProducts(productPage.page - 1)">上一页</button><span>共 {{ productPage.totalElements }} 条 · {{ productPage.totalPages === 0 ? 0 : productPage.page + 1 }} / {{ productPage.totalPages }} 页</span><button :disabled="productPage.last || productPage.totalPages === 0" @click="loadProducts(productPage.page + 1)">下一页</button></div>
        </section>

        <section v-else-if="activeStudentTab === 'wanted'" class="panel">
          <h2>求购管理</h2>
          <p class="intro">发布想买的物品，也可以查看其他同学的求购。</p>
          <p v-if="!isApprovedStudent" class="intro">发布求购需要先通过学生实名认证。当前状态：{{ authStatusText }}</p>
          <button v-if="!isApprovedStudent" class="primary-button wanted-create" type="button" @click="openAuthPage">去实名认证</button>
          <button v-else class="primary-button wanted-create" type="button" @click="startWantedCreate">发布求购</button>

          <form v-if="showWantedForm && isApprovedStudent" class="form form-card" @submit.prevent="submitWanted">
            <h2>{{ editingWantedId ? '修改求购' : '发布求购' }}</h2>
            <label>物品名称<input v-model.trim="wantedForm.itemName" maxlength="100" placeholder="例如：二手自行车" /></label>
            <label>预算<input v-model="wantedForm.budget" type="number" inputmode="numeric" min="1" step="1" placeholder="例如：300" /></label>
            <label>期望成色<select v-model="wantedForm.expectCondition"><option value="">请选择</option><option v-for="item in conditionOptions" :key="item.value" :value="item.value">{{ item.label }}</option></select></label>
            <label>校区（选填）<select v-model="wantedForm.campusId"><option value="">不限校区</option><option v-for="item in campuses" :key="item.id" :value="item.id">{{ item.name }}</option></select></label>
            <label>补充描述<textarea v-model.trim="wantedForm.description" maxlength="500" placeholder="型号、用途或可接受的情况"></textarea></label>
            <div class="inline-actions"><button class="primary-button" :disabled="loading" type="submit">{{ editingWantedId ? '保存' : '发布' }}</button><button class="secondary-button" type="button" @click="resetWantedForm">取消</button></div>
          </form>

          <template v-if="!showWantedForm">
            <form class="filter-card" @submit.prevent="loadWanted(0)">
              <input v-model.trim="wantedFilters.keyword" type="search" placeholder="搜索求购物品" />
              <div class="filter-grid"><select v-model="wantedFilters.campusId"><option value="">全部校区</option><option v-for="item in campuses" :key="item.id" :value="item.id">{{ item.name }}</option></select><select v-model="wantedFilters.expectCondition"><option value="">全部成色</option><option v-for="item in conditionOptions" :key="item.value" :value="item.value">{{ item.label }}</option></select></div>
              <div class="price-range"><input v-model="wantedFilters.minBudget" type="number" inputmode="numeric" min="0" step="1" placeholder="最低预算（元）" /><span>—</span><input v-model="wantedFilters.maxBudget" type="number" inputmode="numeric" min="0" step="1" placeholder="最高预算（元）" /></div>
              <div class="inline-actions"><button class="primary-button" type="submit">搜索</button><button class="secondary-button" type="button" @click="resetWantedFilters">重置</button></div>
            </form>
            <div v-if="wantedItems.length === 0" class="empty-state">暂无求购信息</div>
            <div class="card-grid">
              <article v-for="item in wantedItems" :key="item.id" class="wanted-card" @click="openWantedDetail(item)"><strong><template v-for="(segment, index) in highlightedSegments(item.itemName, wantedFilters.keyword)" :key="index"><mark v-if="segment.highlighted" class="search-highlight">{{ segment.text }}</mark><template v-else>{{ segment.text }}</template></template></strong><b class="wanted-budget">预算 ￥{{ item.budget }}</b><p><template v-for="(segment, index) in highlightedSegments(item.description || '暂无补充描述', wantedFilters.keyword)" :key="index"><mark v-if="segment.highlighted" class="search-highlight">{{ segment.text }}</mark><template v-else>{{ segment.text }}</template></template></p><span>{{ getConditionLabel(item.expectCondition) }} · {{ item.campusId ? getCampusName(item.campusId) : '不限校区' }}</span><button v-if="item.publisher" class="publisher-mini" type="button" @click.stop="openPublicProfile(item.publisher.id)"><img v-if="item.publisher.avatarUrl" :src="item.publisher.avatarUrl" alt="" /><span v-else>{{ avatarText(item.publisher) }}</span><em><template v-for="(segment, index) in highlightedSegments(item.publisher.nickname, wantedFilters.keyword)" :key="index"><mark v-if="segment.highlighted" class="search-highlight">{{ segment.text }}</mark><template v-else>{{ segment.text }}</template></template></em></button><em :class="['status-pill', `status-${item.status.toLowerCase()}`]">{{ getWantedStatusLabel(item.status) }}</em></article>
            </div>
            <div class="pagination"><button :disabled="wantedPage.first || wantedPage.totalPages === 0" @click="loadWanted(wantedPage.page - 1)">上一页</button><span>共 {{ wantedPage.totalElements }} 条 · {{ wantedPage.totalPages === 0 ? 0 : wantedPage.page + 1 }} / {{ wantedPage.totalPages }} 页</span><button :disabled="wantedPage.last || wantedPage.totalPages === 0" @click="loadWanted(wantedPage.page + 1)">下一页</button></div>
          </template>
          <p v-if="message" class="message">{{ message }}</p><p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>
        </section>

        <section v-else-if="activeStudentTab === 'publish'" class="panel">
          <h2>{{ editingProductId ? '修改商品' : '发布商品' }}</h2>
          <p v-if="!isApprovedStudent" class="intro">发布商品需要先通过学生实名认证。当前状态：{{ authStatusText }}</p>
          <button v-if="!isApprovedStudent" class="primary-button" type="button" @click="openAuthPage">去实名认证</button>

          <p v-if="message" class="message form-alert">{{ message }}</p>
          <p v-if="errorMessage" class="error-message form-alert">{{ errorMessage }}</p>

          <form v-if="isApprovedStudent" class="form" @submit.prevent="submitProduct">
            <label>标题<input v-model.trim="productForm.title" type="text" placeholder="例如：高等数学教材" /></label>
            <label>价格<input v-model.trim="productForm.price" type="number" inputmode="decimal" min="0" step="0.01" placeholder="例如：25" /></label>
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
            <label>商品图片</label>
            <div class="image-upload-box" tabindex="0" @paste="handleImagePaste">
              <input id="product-image-file" class="visually-hidden" type="file" accept="image/*" multiple @change="uploadProductImage" />
              <label class="upload-button" for="product-image-file">{{ imageUploading ? '上传中...' : '选择本地图片' }}</label>
              <span>{{ productForm.imageUrls.length ? '已上传 ' + productForm.imageUrls.length + ' 张图片，可继续添加或点击下方查看' : '支持一次选择多张图片，或先复制图片后在这里 Ctrl+V 粘贴' }}</span>
            </div>

            <div v-if="productForm.imageUrls.length" class="upload-preview-grid">
              <article v-for="(imageUrl, index) in productForm.imageUrls" :key="imageUrl" class="upload-preview-item">
                <button class="preview-image-button" type="button" @click="previewImageUrl = imageUrl">
                  <img :src="imageUrl" :alt="`商品图片 ${index + 1}`" />
                </button>
                <button class="remove-image-button" type="button" @click="removeProductImage(index)">删除</button>
              </article>
              <button class="clear-images-button" type="button" @click="clearProductImages">清空全部图片</button>
            </div>
            <p class="hint upload-hint">支持 jpg、png、webp、gif，单张不超过 5MB。</p>
            <label>商品描述<textarea v-model.trim="productForm.description" rows="4" placeholder="描述物品情况、交易地点、购买时间等"></textarea></label>
            <button class="primary-button" type="submit" :disabled="loading || imageUploading">{{ imageUploading ? '图片上传中...' : (loading ? '提交中...' : (editingProductId ? '保存修改' : '提交商品')) }}</button>
            <button v-if="editingProductId" class="secondary-button" type="button" @click="resetProductForm">取消修改</button>
          </form>
        </section>

        <section v-else-if="activeStudentTab === 'messages'" class="panel">
          <h2>消息中心</h2>
          <p class="intro">查看私聊会话，与卖家沟通并约定线下交易。</p>
          <div v-if="selectedConversation || pendingChat" class="chat-panel">
            <button class="text-button" type="button" @click="selectedConversation = null; pendingChat = null; chatMessages = []">返回会话列表</button>
            <h3>与 {{ (selectedConversation?.peerUser || pendingChat?.peerUser)?.nickname || '用户' }} 的对话</h3>
            <div class="chat-messages">
              <div v-if="chatMessages.length === 0" class="empty-state">还没有消息，先发一句吧</div>
              <article v-for="item in chatMessages" :key="item.id" :class="['chat-bubble', item.senderId === currentUser.id ? 'mine' : 'peer']">
                <strong>{{ item.sender?.nickname || '用户' }}</strong>
                <p>{{ item.content }}</p>
                <small>{{ item.createdAt }}</small>
              </article>
            </div>
            <form class="chat-form" @submit.prevent="sendChatMessage">
              <textarea v-model.trim="chatForm.content" rows="2" placeholder="输入消息内容"></textarea>
              <button class="primary-button" type="submit" :disabled="loading">发送</button>
            </form>
          </div>
          <template v-else>
            <div v-if="conversations.length === 0" class="empty-state">暂无会话，可在商品详情页联系卖家</div>
            <article v-for="item in conversations" :key="item.peerUserId || item.id" class="conversation-item" @click="openConversation(item)">
              <span class="conversation-avatar">{{ avatarText(item.peerUser || { nickname: '用户' }) }}</span>
              <div class="conversation-body">
                <div class="conversation-top">
                  <strong>{{ item.peerUser?.nickname || '用户' }}</strong>
                  <small>{{ item.lastMsgAt || item.createdAt }}</small>
                </div>
                <div class="conversation-bottom">
                  <p>{{ item.lastMessage }}</p>
                  <span v-if="item.unreadCount > 0" class="unread-badge">{{ item.unreadCount > 99 ? '99+' : item.unreadCount }}</span>
                </div>
              </div>
            </article>
          </template>
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
            <p class="hint">留言、私聊、订单与举报功能已接入。</p>
          </div>

          <section class="section-block">
            <div class="section-title">
              <h2>我的交易</h2>
              <button v-if="activeMyTab === 'products'" class="text-button" type="button" @click="loadMyProducts">刷新</button>
              <button v-else-if="activeMyTab === 'wanted'" class="text-button" type="button" @click="loadMyWanted">刷新</button>
              <button v-else-if="activeMyTab === 'orders'" class="text-button" type="button" @click="loadMyOrders">刷新</button>
            </div>
            <div class="tabs my-tabs"><button v-for="item in myTabs" :key="item.key" :class="{ active: activeMyTab === item.key }" type="button" @click="activeMyTab = item.key; if (item.key === 'orders') loadMyOrders()">{{ item.label }}</button></div>

            <template v-if="activeMyTab === 'products'">
              <div v-if="myProducts.length === 0" class="empty-state">还没有发布商品</div>
              <article v-for="product in myProducts" :key="product.id" class="product-card my-product-card">
                <div v-if="productImageUrls(product).length" class="my-product-thumbs">
                  <button v-for="imageUrl in productImageUrls(product).slice(0, 4)" :key="imageUrl" class="my-product-thumb" type="button" @click="previewImageUrl = imageUrl"><img :src="imageUrl" :alt="product.title" /></button>
                  <span v-if="extraProductImageCount(product)" class="image-count-badge">+{{ extraProductImageCount(product) }}</span>
                </div>
                <div v-else class="my-product-thumbs"><div class="image-placeholder small">闲</div></div>
                <div class="my-product-info"><strong>{{ product.title }}</strong><p>￥{{ product.price }} · {{ getCategoryName(product.categoryId) }} · {{ getCampusName(product.campusId) }}</p><p>{{ getConditionLabel(product.conditionLevel) }} · {{ getTradeMethodLabel(product.tradeMethod) }}</p></div>
                <span :class="['status-pill', `status-${product.status.toLowerCase().replace('_', '-')}`]">{{ getProductStatusLabel(product.status) }}</span>
                <p v-if="product.status === 'REJECTED' && product.auditRemark" class="audit-reason"><strong>未通过原因：</strong>{{ product.auditRemark }}</p><p class="product-desc">{{ product.description }}</p>
                <div class="review-actions"><button type="button" @click="openProductDetail(product)">查看详情与留言</button><button type="button" @click="editProduct(product)">修改</button><button v-if="product.status !== 'OFF_SHELF'" type="button" @click="askOffShelfProduct(product)">下架</button><button v-else type="button" @click="askRestoreProduct(product)">恢复上架</button><button type="button" @click="askDeleteProduct(product)">删除</button></div>
              </article>
            </template>

            <template v-else-if="activeMyTab === 'wanted'">
              <div v-if="myWanted.length === 0" class="empty-state">你还没有发布求购</div>
              <article v-for="item in myWanted" :key="item.id" class="wanted-card my-wanted-card"><strong>{{ item.itemName }}</strong><b class="wanted-budget">预算 ￥{{ item.budget }}</b><p>{{ item.description || '暂无补充描述' }}</p><span>{{ getConditionLabel(item.expectCondition) }} · {{ item.campusId ? getCampusName(item.campusId) : '不限校区' }}</span><em :class="['status-pill', `status-${item.status.toLowerCase()}`]">{{ getWantedStatusLabel(item.status) }}</em><div class="review-actions"><button type="button" @click="openWantedDetail(item)">查看详情</button><button v-if="item.status === 'OPEN'" @click="editWanted(item)">修改</button><button v-if="item.status === 'OPEN'" @click="changeWantedStatus(item, 'match')">已找到卖家</button><button v-if="item.status !== 'CLOSED'" @click="changeWantedStatus(item, 'close')">关闭求购</button></div></article>
            </template>

            <template v-else>
              <div v-if="myOrders.length === 0" class="empty-state"><strong>暂无订单</strong><p>可在商品详情页发起交易。</p></div>
              <article v-for="order in myOrders" :key="order.id" class="product-card my-product-card">
                <div class="my-product-info">
                  <strong>{{ order.product?.title || '商品' }}</strong>
                  <p>订单号：{{ order.orderNo }}</p>
                  <p>￥{{ order.product?.price }} · {{ getOrderStatusLabel(order.status) }}</p>
                  <p v-if="order.meetLocation">约定地点：{{ order.meetLocation }}</p>
                  <p v-if="order.meetTime">约定时间：{{ order.meetTime }}</p>
                </div>
                <div class="review-actions">
                  <button v-if="order.status === 'PENDING_CHAT'" type="button" @click="updateOrderStatus(order, 'PENDING_TRADE')">进入待交易</button>
                  <button v-if="order.status === 'PENDING_TRADE'" type="button" @click="updateOrderStatus(order, 'COMPLETED')">标记完成</button>
                </div>
              </article>
            </template>
          </section>
        </section>
      </div>

      <nav class="bottom-nav" aria-label="学生端底部导航">
        <button v-for="item in studentTabs" :key="item.key" type="button" :class="{ active: activeStudentTab === item.key }" @click="selectStudentTab(item.key)">
          <span>{{ item.label }}</span>
          <em v-if="item.key === 'messages' && totalUnreadCount > 0" class="nav-badge">{{ totalUnreadCount > 99 ? '99+' : totalUnreadCount }}</em>
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
          <p class="intro">按状态查看学生实名认证申请；已审核记录也可以再次调整为通过或不通过。</p>
          <div class="tabs auth-review-tabs" aria-label="认证审核状态筛选">
            <button v-for="item in authReviewTabs" :key="item.key" type="button" :class="{ active: authReviewStatus === item.key }" @click="switchAuthReviewStatus(item.key)">{{ item.label }}</button>
          </div>
          <p v-if="message" class="message">{{ message }}</p>
          <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>
          <div v-if="adminLoading" class="empty-state">正在加载...</div>
          <div v-else-if="authApplications.length === 0" class="empty-state">暂无{{ getAuthStatusLabel(authReviewStatus) }}认证</div>
          <article v-else v-for="item in authApplications" :key="item.id" class="review-card">
            <div>
              <strong>{{ item.nickname }}</strong>
              <p>{{ item.username }} · {{ item.studentNo }} · {{ item.college }}</p>
              <p v-if="item.authRemark">审核备注：{{ item.authRemark }}</p>
            </div>
            <div class="review-actions auth-actions">
              <button v-if="item.authStatus !== 'APPROVED'" type="button" @click="reviewAuth(item, 'APPROVED')">通过</button>
              <button v-if="item.authStatus !== 'REJECTED'" type="button" @click="reviewAuth(item, 'REJECTED')">不通过</button>
              <button type="button" @click="openAuthRemarkDialog(item)">备注</button>
            </div>
            <span :class="['status-pill', `status-${item.authStatus.toLowerCase()}`]">{{ getAuthStatusLabel(item.authStatus) }}</span>
          </article>
        </section>
        <section v-else-if="activeAdminTab === 'productReview'" class="panel">
          <h2>商品审核</h2>
          <p class="intro">提交商品后会自动进行关键词辅助审核；也可手动执行审核。</p>
          <div class="tabs auth-review-tabs">
            <button type="button" :class="{ active: productReviewStatus === 'PENDING' }" @click="productReviewStatus = 'PENDING'; loadPendingProducts()">待审核</button>
            <button type="button" :class="{ active: productReviewStatus === 'PUBLISHED' }" @click="productReviewStatus = 'PUBLISHED'; loadPendingProducts()">已通过</button>
            <button type="button" :class="{ active: productReviewStatus === 'REJECTED' }" @click="productReviewStatus = 'REJECTED'; loadPendingProducts()">已拒绝</button>
          </div>
          <div v-if="adminLoading" class="empty-state">正在加载...</div>
          <div v-else-if="pendingProducts.length === 0" class="empty-state">暂无商品</div>
          <article v-for="product in pendingProducts" :key="product.id" class="review-card">
            <div>
              <strong>{{ product.title }}</strong>
              <p>￥{{ product.price }} · {{ product.seller?.nickname || '卖家' }}</p>
              <p>{{ product.description }}</p>
              <div v-if="aiAuditMap[product.id]" :class="['ai-audit-box', getAiRiskClass(aiAuditMap[product.id].riskLevel)]">
                <p><strong>关键词辅助审核</strong> · {{ getAiSuggestionLabel(aiAuditMap[product.id].suggestion) }} · {{ aiAuditMap[product.id].riskLevel }}</p>
                <p>{{ aiAuditMap[product.id].reason }}</p>
              </div>
            </div>
            <div class="review-actions" v-if="productReviewStatus === 'PENDING'">
              <button type="button" @click="auditProduct(product, 'PUBLISHED')">通过</button>
              <button type="button" @click="auditProduct(product, 'REJECTED')">拒绝</button>
              <button type="button" @click="runAiAudit(product)">执行关键词审核</button>
              <button type="button" @click="loadAiAuditLogs(product.id)">查看记录</button>
            </div>
            <div v-if="aiAuditLogs.length && aiAuditLogs[0]?.productId === product.id" class="ai-audit-box">
              <p v-for="log in aiAuditLogs" :key="log.id"><strong>{{ getAiSuggestionLabel(log.suggestion) }}</strong> · {{ log.riskLevel }} · {{ log.reason }} · {{ log.createdAt }}</p>
            </div>
          </article>
        </section>
        <section v-else-if="activeAdminTab === 'reports'" class="panel">
          <h2>举报处理</h2>
          <div class="tabs auth-review-tabs">
            <button type="button" :class="{ active: reportReviewStatus === 'PENDING' }" @click="reportReviewStatus = 'PENDING'; loadAdminReports()">待处理</button>
            <button type="button" :class="{ active: reportReviewStatus === 'RESOLVED' }" @click="reportReviewStatus = 'RESOLVED'; loadAdminReports()">已处理</button>
            <button type="button" :class="{ active: reportReviewStatus === 'REJECTED' }" @click="reportReviewStatus = 'REJECTED'; loadAdminReports()">已驳回</button>
          </div>
          <div v-if="adminLoading" class="empty-state">正在加载...</div>
          <div v-else-if="adminReports.length === 0" class="empty-state">暂无举报</div>
          <article v-for="item in adminReports" :key="item.id" class="review-card">
            <div>
              <strong>{{ item.reasonName }} · {{ item.targetType }} #{{ item.targetId }}</strong>
              <p>{{ item.description || '无补充说明' }}</p>
              <p>举报人：{{ item.reporter?.nickname }}</p>
            </div>
            <div v-if="reportReviewStatus === 'PENDING'" class="review-actions">
              <button type="button" @click="handleReport(item, 'RESOLVED')">处理</button>
              <button type="button" @click="handleReport(item, 'REJECTED')">驳回</button>
            </div>
          </article>
        </section>
        <section v-else-if="activeAdminTab === 'users'" class="panel">
          <h2>用户管理</h2>
          <form class="search-row" @submit.prevent="loadAdminUsers">
            <input v-model.trim="adminUserKeyword" type="text" placeholder="搜索用户名/昵称/学号" />
            <button class="secondary-button" type="submit" :disabled="adminLoading">搜索</button>
          </form>
          <div v-if="adminLoading" class="empty-state">正在加载...</div>
          <div v-else-if="adminUsers.length === 0" class="empty-state">暂无学生用户</div>
          <article v-for="user in adminUsers" :key="user.id" class="review-card">
            <div>
              <strong>{{ user.nickname }}</strong>
              <p>{{ user.username }} · {{ user.studentNo || '未填学号' }} · {{ user.college || '未填学院' }}</p>
            </div>
            <div class="review-actions">
              <button v-if="user.status !== 'BANNED'" type="button" @click="updateUserStatus(user, 'BANNED')">封禁</button>
              <button v-else type="button" @click="updateUserStatus(user, 'ACTIVE')">解封</button>
            </div>
            <span :class="['status-pill', `status-${user.status.toLowerCase()}`]">{{ user.status === 'BANNED' ? '已封禁' : '正常' }}</span>
          </article>
        </section>
        <section v-else-if="activeAdminTab === 'categories'" class="panel">
          <h2>分类管理</h2>
          <p class="intro">维护商品分类名称，学生发布商品时可选择这些分类。</p>
          <form class="form compact-form" @submit.prevent="submitCategoryForm">
            <label>分类名称<input v-model.trim="categoryForm.name" type="text" placeholder="例如：教材资料" /></label>
            <div class="confirm-actions">
              <button class="primary-button" type="submit" :disabled="adminLoading">{{ categoryForm.id ? '保存修改' : '新增分类' }}</button>
              <button v-if="categoryForm.id" class="secondary-button" type="button" @click="resetCategoryForm">取消编辑</button>
            </div>
          </form>
          <div v-if="adminLoading" class="empty-state">正在加载...</div>
          <article v-for="item in adminCategories" :key="item.id" class="review-card">
            <div>
              <strong>{{ item.name }}</strong>
              <p>序号 {{ item.sortOrder }}</p>
            </div>
            <div class="review-actions">
              <button type="button" @click="editCategory(item)">编辑</button>
              <button type="button" @click="askDeleteCategory(item)">删除</button>
            </div>
          </article>
        </section>
        <section v-else class="panel">
          <h2>{{ adminHeaderTitle }}</h2>
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

    <div v-if="noticeDialog.visible" class="confirm-overlay notice-overlay" role="dialog" aria-modal="true" @click.self="closeNoticeDialog">
      <article :class="['notice-dialog', noticeDialog.type]">
        <h2>{{ noticeDialog.title }}</h2>
        <p>{{ noticeDialog.content }}</p>
        <div class="confirm-actions">
          <button class="primary-button" type="button" @click="closeNoticeDialog">知道了</button>
        </div>
      </article>
    </div>
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

    <div v-if="authRemarkDialog.visible" class="confirm-overlay" role="dialog" aria-modal="true" @click.self="closeAuthRemarkDialog">
      <form class="remark-dialog" @submit.prevent="submitAuthRemarkReview">
        <h2>审核备注</h2>
        <p>为该学生补充审核说明，也可以在这里调整认证状态。</p>
        <label>认证状态
          <select v-model="authRemarkDialog.status">
            <option value="APPROVED">已通过</option>
            <option value="REJECTED">未通过</option>
          </select>
        </label>
        <label>备注内容<textarea v-model.trim="authRemarkDialog.remark" rows="4" maxlength="255" placeholder="选填，例如：材料清晰，审核通过"></textarea></label>
        <div class="confirm-actions">
          <button class="secondary-button" type="button" @click="closeAuthRemarkDialog">取消</button>
          <button class="primary-button" type="submit" :disabled="adminLoading">保存备注</button>
        </div>
      </form>
    </div>

    <div v-if="previewImageUrl" class="confirm-overlay image-lightbox" role="dialog" aria-modal="true" @click.self="previewImageUrl = ''">
      <article class="image-lightbox-dialog">
        <button class="detail-close" type="button" @click="previewImageUrl = ''">×</button>
        <img :src="previewImageUrl" alt="图片预览" />
      </article>
    </div>
    <div v-if="selectedProduct" class="confirm-overlay detail-overlay" role="dialog" aria-modal="true" @click.self="selectedProduct = null">
      <article class="detail-dialog">
        <button class="detail-close" @click="selectedProduct = null">×</button>
        <div v-if="productImageUrls(selectedProduct).length" class="detail-image-grid">
          <button v-for="imageUrl in productImageUrls(selectedProduct)" :key="imageUrl" class="detail-image-button" type="button" @click="previewImageUrl = imageUrl">
            <img :src="imageUrl" :alt="selectedProduct.title" />
          </button>
        </div>
        <h2>{{ selectedProduct.title }}</h2>
        <b class="detail-price">￥{{ selectedProduct.price }}</b>
        <p>{{ selectedProduct.description }}</p>
        <div class="detail-meta">
          <span>{{ getCategoryName(selectedProduct.categoryId) }}</span>
          <span>{{ getCampusName(selectedProduct.campusId) }}</span>
          <span>{{ getConditionLabel(selectedProduct.conditionLevel) }}</span>
          <span>{{ getTradeMethodLabel(selectedProduct.tradeMethod) }}</span>
        </div>
        <button v-if="selectedProduct.seller" class="publisher-panel" type="button" @click="openPublicProfile(selectedProduct.seller.id)">
          <span class="public-avatar"><img v-if="selectedProduct.seller.avatarUrl" :src="selectedProduct.seller.avatarUrl" alt="" /><b v-else>{{ avatarText(selectedProduct.seller) }}</b></span>
          <span><strong>{{ selectedProduct.seller.nickname }}</strong><small>{{ selectedProduct.seller.college || '校园学生' }} · {{ selectedProduct.seller.authStatus === 'APPROVED' ? '已认证' : '未认证' }}</small></span>
          <i>查看主页 ›</i>
        </button>
        <div v-if="selectedProduct.seller && selectedProduct.seller.id !== currentUser.id" class="detail-actions">
          <button class="primary-button" type="button" @click="startChatWithSeller">联系卖家</button>
          <button class="secondary-button" type="button" @click="createOrderFromProduct">我要购买</button>
          <button class="secondary-button" type="button" @click="openReportDialog('PRODUCT', selectedProduct.id)">举报商品</button>
        </div>
        <section class="section-block">
          <h3>商品留言</h3>
          <div v-if="productComments.length === 0" class="empty-state">还没有留言</div>
          <article v-for="item in productComments" :key="item.id" class="comment-card">
            <strong>{{ item.user?.nickname || '用户' }}</strong>
            <p>{{ item.content }}</p>
            <small>{{ item.createdAt }}</small>
          </article>
          <form class="comment-form" @submit.prevent="submitComment">
            <textarea v-model.trim="commentForm.content" rows="2" placeholder="向卖家提问..."></textarea>
            <button class="primary-button" type="submit" :disabled="loading">发表留言</button>
          </form>
        </section>
        <label>约定地点<input v-model.trim="orderForm.meetLocation" type="text" placeholder="例如：图书馆门口" /></label>
        <label>约定时间<input v-model="orderForm.meetTime" type="datetime-local" /></label>
        <label>备注<textarea v-model.trim="orderForm.remark" rows="2" placeholder="交易备注"></textarea></label>
        <small>浏览 {{ selectedProduct.viewCount }} 次 · {{ selectedProduct.createdAt }}</small>
      </article>
    </div>

    <div v-if="reportDialog.visible" class="confirm-overlay report-overlay" role="dialog" aria-modal="true" @click.self="closeReportDialog">
      <form class="remark-dialog" @submit.prevent="submitReport">
        <h2>提交举报</h2>
        <label>举报原因
          <select v-model="reportDialog.reasonId">
            <option v-for="item in reportReasons" :key="item.id" :value="item.id">{{ item.name }}</option>
          </select>
        </label>
        <label>补充说明<textarea v-model.trim="reportDialog.description" rows="3" placeholder="选填"></textarea></label>
        <div class="confirm-actions">
          <button class="secondary-button" type="button" @click="closeReportDialog">取消</button>
          <button class="primary-button" type="submit" :disabled="loading">提交举报</button>
        </div>
      </form>
    </div>

    <div v-if="selectedWanted" class="confirm-overlay detail-overlay" role="dialog" aria-modal="true" @click.self="selectedWanted = null">
      <article class="detail-dialog"><button class="detail-close" @click="selectedWanted = null">×</button><p class="eyebrow">求购详情</p><h2>{{ selectedWanted.itemName }}</h2><b class="detail-price">预算 ￥{{ selectedWanted.budget }}</b><p>{{ selectedWanted.description || '发布者没有填写补充描述。' }}</p><div class="detail-meta"><span>{{ getConditionLabel(selectedWanted.expectCondition) }}</span><span>{{ selectedWanted.campusId ? getCampusName(selectedWanted.campusId) : '不限校区' }}</span><span>{{ getWantedStatusLabel(selectedWanted.status) }}</span></div><button v-if="selectedWanted.publisher" class="publisher-panel" type="button" @click="openPublicProfile(selectedWanted.publisher.id)"><span class="public-avatar"><img v-if="selectedWanted.publisher.avatarUrl" :src="selectedWanted.publisher.avatarUrl" alt="" /><b v-else>{{ avatarText(selectedWanted.publisher) }}</b></span><span><strong>{{ selectedWanted.publisher.nickname }}</strong><small>{{ selectedWanted.publisher.college || '校园学生' }} · {{ selectedWanted.publisher.authStatus === 'APPROVED' ? '已认证' : '未认证' }}</small></span><i>查看主页 ›</i></button><div v-if="selectedWanted.publisher && selectedWanted.publisher.id !== currentUser.id" class="detail-actions"><button class="primary-button" type="button" @click="startChatWithPublisher">联系发布者</button></div><small>发布于 {{ selectedWanted.createdAt }}</small></article>
    </div>

    <div v-if="publicProfile" class="confirm-overlay" role="dialog" aria-modal="true" @click.self="closePublicProfile">
      <section class="profile-dialog"><button class="detail-close" @click="closePublicProfile">×</button><header class="public-profile-head"><span class="public-avatar large"><img v-if="publicProfile.avatarUrl" :src="publicProfile.avatarUrl" alt="" /><b v-else>{{ avatarText(publicProfile) }}</b></span><div><p class="eyebrow">公开主页</p><h2>{{ publicProfile.nickname }}</h2><p>{{ publicProfile.college || '校园学生' }} · {{ publicProfile.authStatus === 'APPROVED' ? '已认证学生' : '未认证' }}</p></div></header><div v-if="profileLoading" class="empty-state">正在加载主页...</div><template v-else><section class="profile-section"><h3>TA 的商品 <span>{{ profileProducts.length }}</span></h3><div v-if="profileProducts.length === 0" class="empty-state">暂无公开商品</div><div class="profile-card-grid"><article v-for="product in profileProducts" :key="product.id" class="profile-item" @click="openProductDetail(product)"><img v-if="firstProductImage(product)" :src="firstProductImage(product)" :alt="product.title" /><div v-else class="image-placeholder">闲</div><strong>{{ product.title }}</strong><b>￥{{ product.price }}</b></article></div></section><section class="profile-section"><h3>TA 的求购 <span>{{ profileWanted.length }}</span></h3><div v-if="profileWanted.length === 0" class="empty-state">暂无公开求购</div><article v-for="item in profileWanted" :key="item.id" class="profile-wanted" @click="openWantedDetail(item)"><div><strong>{{ item.itemName }}</strong><b>预算 ￥{{ item.budget }}</b></div><span>{{ getWantedStatusLabel(item.status) }} · {{ item.campusId ? getCampusName(item.campusId) : '不限校区' }}</span></article></section></template></section>
    </div>
  </main>
</template>









