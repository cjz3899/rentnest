<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { userApi, type ProfileData } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

const profile = ref<ProfileData | null>(null)
const loading = ref(false)

async function loadProfile() {
  loading.value = true
  try {
    const resp = await userApi.profile()
    profile.value = resp.data.data
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '获取资料失败')
  } finally {
    loading.value = false
  }
}

async function handleLogout() {
  await auth.logout()
  ElMessage.success('已退出登录')
  router.replace({ name: 'login' })
}

onMounted(loadProfile)
</script>

<template>
  <div class="home">
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>个人资料</span>
          <el-button type="danger" plain @click="handleLogout">退出登录</el-button>
        </div>
      </template>
      <el-descriptions v-if="profile" :column="1" border>
        <el-descriptions-item label="用户 ID">{{ profile.id }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ profile.nickname }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ profile.phone }}</el-descriptions-item>
        <el-descriptions-item label="角色">{{ profile.role }}</el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ profile.createdAt }}</el-descriptions-item>
      </el-descriptions>
      <el-empty v-else description="暂无数据" />
      <div class="actions">
        <el-button @click="loadProfile">刷新资料</el-button>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.home {
  max-width: 640px;
  margin: 40px auto;
  padding: 0 16px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.actions {
  margin-top: 16px;
}
</style>
