<template>
  <div class="page-container">
    <el-page-header :content="isEdit ? '编辑商品' : '发布商品'" @back="router.back()" />

    <el-card class="form-card" shadow="never">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="商品标题" prop="title">
          <el-input v-model="form.title" maxlength="64" show-word-limit placeholder="例如：九成新机械键盘，青轴 87 键" />
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="分类" prop="categoryId">
              <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%">
                <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="成色" prop="conditionLevel">
              <el-select v-model="form.conditionLevel" style="width: 100%">
                <el-option v-for="c in conditions" :key="c" :label="c" :value="c" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="交易地点">
              <el-input v-model="form.location" maxlength="64" placeholder="如：图书馆门口" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="售价" prop="price">
              <el-input-number v-model="form.price" :min="0.01" :max="100000" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="允许议价">
              <el-switch v-model="form.allowBargain" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item v-if="form.allowBargain === 1" label="底价">
              <el-input-number
                v-model="form.minPrice"
                :min="0"
                :max="form.price"
                :precision="2"
                style="width: 100%"
                placeholder="选填，低于此价不自动成交"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="商品描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="5"
            maxlength="1000"
            show-word-limit
            placeholder="说明使用情况、入手渠道、有无瑕疵等，信息越完整越容易卖出"
          />
        </el-form-item>

        <el-form-item label="商品图片">
          <div class="upload-wrap">
            <div v-for="(img, idx) in form.images" :key="img" class="upload-item">
              <img :src="img" class="thumb" />
              <el-icon class="del" @click="removeImage(idx)"><CircleCloseFilled /></el-icon>
              <span v-if="idx === 0" class="cover-badge">封面</span>
            </div>
            <el-upload
              v-if="form.images.length < 5"
              :show-file-list="false"
              :http-request="uploadRequest"
              accept="image/*"
            >
              <div class="upload-btn">
                <el-icon :size="22"><Plus /></el-icon>
                <span>上传</span>
              </div>
            </el-upload>
          </div>
          <div class="text-sub">最多 5 张，第一张作为封面；仅支持 jpg/png/webp/gif，单张不超过 5MB</div>
        </el-form-item>

        <el-form-item v-if="form.allowBargain === 1">
          <div class="text-sub">
            底价说明：填写底价后，买家出价达到底价会「自动成交」并直接生成订单；不填则所有出价都需要你手动确认。
          </div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="onSubmit">
            {{ isEdit ? '保存修改' : '提交审核' }}
          </el-button>
          <el-button @click="router.back()">取消</el-button>
          <span class="text-sub ml12">
            {{ isEdit ? '被驳回的商品修改后会重新进入待审核' : '提交后需管理员审核通过才会展示' }}
          </span>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createProduct, deleteImage, getProduct, listCategories, updateProduct, uploadImage } from '@/api/product'

const route = useRoute()
const router = useRouter()
const formRef = ref()
const categories = ref([])
const submitting = ref(false)
const isEdit = ref(Boolean(route.query.id))

const conditions = ['全新', '99新', '95新', '9成新', '8成新']

const form = reactive({
  title: '',
  categoryId: null,
  price: 1,
  minPrice: null,
  allowBargain: 1,
  conditionLevel: '99新',
  location: '',
  description: '',
  images: []
})

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  price: [{ required: true, message: '请输入售价', trigger: 'blur' }]
}

/**
 * 本次会话新上传的图片。
 * 只有这些文件在"移除"时才真正删除——编辑已有商品时移除的是历史图片，
 * 直接删文件可能影响其它引用（例如复制商品），交给后端的孤儿清理任务处理。
 */
const uploadedNow = new Set()

/**
 * 自定义上传：走统一的 axios 实例，自动带 token 与统一错误提示。
 * 上传成功后把后端返回的相对 URL 放进 form.images，提交时一并保存。
 */
async function uploadRequest(options) {
  try {
    const res = await uploadImage(options.file)
    form.images.push(res.data.url)
    uploadedNow.add(res.data.url)
    options.onSuccess?.(res)
  } catch (e) {
    options.onError?.(e)
  }
}

/** 移除图片：新上传的立即删除物理文件，避免"取消发布"后留下孤儿文件 */
function removeImage(index) {
  const url = form.images[index]
  form.images.splice(index, 1)
  if (uploadedNow.has(url)) {
    uploadedNow.delete(url)
    deleteImage(url).catch(() => {
      // 删除失败不影响表单操作，后端孤儿清理任务会兜底
    })
  }
}

async function onSubmit() {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    if (form.allowBargain === 1 && form.minPrice !== null && form.minPrice >= form.price) {
      ElMessage.warning('底价必须低于售价')
      return
    }
    submitting.value = true
    try {
      const payload = {
        title: form.title,
        categoryId: form.categoryId,
        price: form.price,
        minPrice: form.minPrice ?? undefined,
        allowBargain: form.allowBargain,
        conditionLevel: form.conditionLevel,
        location: form.location || undefined,
        description: form.description || undefined,
        images: form.images
      }
      if (isEdit.value) {
        await updateProduct(route.query.id, payload)
        ElMessage.success('修改成功')
      } else {
        await createProduct(payload)
        ElMessage.success('已提交，等待管理员审核')
      }
      router.push('/my-products')
    } catch (e) {
      // 拦截器已提示
    } finally {
      submitting.value = false
    }
  })
}

onMounted(async () => {
  const res = await listCategories()
  categories.value = res.data || []
  if (isEdit.value) {
    const detail = await getProduct(route.query.id)
    const p = detail.data
    Object.assign(form, {
      title: p.title,
      categoryId: p.categoryId,
      price: Number(p.price),
      minPrice: p.minPrice ? Number(p.minPrice) : null,
      allowBargain: p.allowBargain,
      conditionLevel: p.conditionLevel,
      location: p.location,
      description: p.description,
      images: p.images?.length ? [...p.images] : p.coverUrl ? [p.coverUrl] : []
    })
  }
})
</script>

<style scoped>
.form-card {
  margin-top: 16px;
  border-radius: 12px;
}

.upload-wrap {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.upload-item {
  position: relative;
  width: 96px;
  height: 96px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #eef0f4;
}

.thumb {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.del {
  position: absolute;
  top: 2px;
  right: 2px;
  color: #fff;
  background: rgba(0, 0, 0, 0.45);
  border-radius: 50%;
  cursor: pointer;
  font-size: 16px;
}

.cover-badge {
  position: absolute;
  left: 0;
  bottom: 0;
  padding: 0 6px;
  font-size: 11px;
  color: #fff;
  background: rgba(47, 107, 255, 0.85);
}

.upload-btn {
  width: 96px;
  height: 96px;
  border: 1px dashed #cfd6e4;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  color: var(--text-sub);
  font-size: 12px;
  cursor: pointer;
}

.upload-btn:hover {
  border-color: var(--brand);
  color: var(--brand);
}

.ml12 {
  margin-left: 12px;
}
</style>
