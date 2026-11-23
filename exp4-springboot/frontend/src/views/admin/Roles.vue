<template>
  <div class="page-container">
    <div class="section-title">角色权限</div>

    <div class="roles-grid">
      <el-card shadow="never" class="role-list">
        <div
          v-for="r in roles"
          :key="r.id"
          :class="['role-item', { active: current && current.id === r.id }]"
          @click="selectRole(r)"
        >
          <div class="role-name">{{ r.name }}</div>
          <div class="text-sub">{{ r.code }}</div>
          <div class="text-sub">已分配 {{ r.permissionIds.length }} 项权限</div>
        </div>
      </el-card>

      <el-card shadow="never" class="perm-panel">
        <template v-if="current">
          <div class="panel-head">
            <div>
              <strong>{{ current.name }}</strong>
              <span class="text-sub ml8">{{ current.description }}</span>
            </div>
            <el-button
              type="primary"
              :disabled="current.code === 'SUPER_ADMIN'"
              :loading="saving"
              @click="save"
            >
              保存
            </el-button>
          </div>

          <el-alert
            v-if="current.code === 'SUPER_ADMIN'"
            type="info"
            :closable="false"
            title="超级管理员权限不可修改（防止系统被锁死），仅可查看"
            class="mb16"
          />

          <el-checkbox-group v-model="checked" :disabled="current.code === 'SUPER_ADMIN'">
            <div v-for="group in grouped" :key="group.type" class="perm-group">
              <div class="perm-group-title">{{ group.label }}（{{ group.items.length }}）</div>
              <div class="perm-items">
                <el-checkbox v-for="p in group.items" :key="p.id" :label="p.id" :value="p.id">
                  {{ p.name }} <span class="code">{{ p.code }}</span>
                </el-checkbox>
              </div>
            </div>
          </el-checkbox-group>
        </template>
        <el-empty v-else description="请选择左侧角色" />
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listPermissions, listRoles, updateRolePermissions } from '@/api/admin'

const roles = ref([])
const permissions = ref([])
const current = ref(null)
const checked = ref([])
const saving = ref(false)

const TYPE_LABEL = { MENU: '菜单权限', BUTTON: '按钮权限', API: '接口权限' }

const grouped = computed(() => {
  const map = {}
  for (const p of permissions.value) {
    const type = p.type || 'API'
    if (!map[type]) map[type] = []
    map[type].push(p)
  }
  return Object.keys(map).map((type) => ({
    type,
    label: TYPE_LABEL[type] || type,
    items: map[type]
  }))
})

function selectRole(r) {
  current.value = r
  checked.value = [...r.permissionIds]
}

async function load() {
  const [roleRes, permRes] = await Promise.all([listRoles(), listPermissions()])
  roles.value = roleRes.data || []
  permissions.value = permRes.data || []
  if (roles.value.length) selectRole(roles.value[0])
}

async function save() {
  saving.value = true
  try {
    await updateRolePermissions(current.value.id, checked.value)
    ElMessage.success('权限已更新，相关用户刷新后生效')
    await load()
  } catch (e) {
    // 拦截器已提示
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.roles-grid {
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: 16px;
}

.role-item {
  padding: 12px;
  border-radius: 10px;
  cursor: pointer;
  border: 1px solid transparent;
  margin-bottom: 8px;
}

.role-item:hover {
  background: #f7f9ff;
}

.role-item.active {
  background: var(--brand-light);
  border-color: #c9daff;
}

.role-name {
  font-weight: 600;
  font-size: 15px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.ml8 {
  margin-left: 8px;
}

.mb16 {
  margin-bottom: 16px;
}

.perm-group {
  margin-bottom: 16px;
}

.perm-group-title {
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 8px;
  color: #374151;
}

.perm-items {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 6px 12px;
}

.code {
  color: #9aa4b2;
  font-size: 12px;
  margin-left: 4px;
}
</style>
