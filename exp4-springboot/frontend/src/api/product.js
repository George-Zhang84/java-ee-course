import request from '@/api/request'

/** 分类列表 */
export const listCategories = () => request.get('/categories')

/** 商品分页搜索 */
export const listProducts = (params) => request.get('/products', { params })
/** 我的商品 */
export const listMyProducts = (params) => request.get('/products/mine', { params })
/** 商品详情 */
export const getProduct = (id) => request.get(`/products/${id}`)
/** 商品评价 */
export const listProductReviews = (id, params) => request.get(`/products/${id}/reviews`, { params })

/** 发布商品 */
export const createProduct = (data) => request.post('/products', data)
/** 编辑商品 */
export const updateProduct = (id, data) => request.put(`/products/${id}`, data)
/** 上下架 */
export const changeProductStatus = (id, status) => request.put(`/products/${id}/status`, { status })
/** 删除商品 */
export const deleteProduct = (id) => request.delete(`/products/${id}`)

/** 上传图片（FormData） */
export const uploadImage = (file) => {
  const form = new FormData()
  form.append('file', file)
  return request.post('/upload/image', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/** 删除未使用的上传图片（放弃发布 / 表单里移除时调用，避免留下孤儿文件） */
export const deleteImage = (url) => request.delete('/upload/image', { params: { url } })

/** 收藏 */
export const listFavorites = (params) => request.get('/favorites', { params })
export const addFavorite = (productId) => request.post(`/favorites/${productId}`)
export const removeFavorite = (productId) => request.delete(`/favorites/${productId}`)
