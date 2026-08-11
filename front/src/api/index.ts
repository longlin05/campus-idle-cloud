import request from '@/utils/request'
import type { Result } from '@/utils/request'

/* =================== 类型定义 =================== */

export interface UserInfo {
  id: number
  username: string
  phone: string
  avatar: string
  nickname: string
  role: number
  status?: number
  createTime?: string
  [k: string]: any
}

export interface ProductVO {
  id: number
  title: string
  name?: string
  description: string
  price: number
  originalPrice: number
  quantity: number
  stock?: number
  categoryId: number
  categoryName: string
  sellerId: number
  sellerName: string
  sellerAvatar: string
  images: string[]
  imageUrl?: string
  viewCount: number
  heat?: number
  tradeType?: number
  status: number
  createTime: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
  list?: T[]
}

export interface Category {
  id: number
  categoryId?: number
  name: string
  categoryName?: string
  categoryDesc?: string
  parentId?: number
  sortOrder?: number
  sort?: number
  description?: string
  status?: number
  createTime?: string
}

export interface LoginResult {
  token: string
  user: UserInfo
  admin?: UserInfo
}

export interface CartItemVO {
  id: number
  productId: number
  title: string
  description?: string
  image?: string
  images?: string[]
  price: number
  quantity: number
  selected?: boolean
  categoryName?: string
}

export interface OrderItemVO {
  id?: number
  productId: number
  productTitle: string
  productImage?: string
  price: number
  quantity: number
}

export interface OrderVO {
  id?: number
  orderId?: number
  orderNo?: string
  status: number
  statusText?: string
  statusName?: string
  buyerId: number
  buyerName?: string
  sellerId?: number
  sellerName?: string
  items?: OrderItemVO[]
  totalQuantity?: number
  totalAmount?: number
  orderAmount?: number
  shippingFee?: number
  name?: string
  phone?: string
  address?: string
  receiverName?: string
  receiverPhone?: string
  receiverAddress?: string
  payTime?: string
  shipTime?: string
  receiveTime?: string
  createTime: string
  trackingNo?: string
  productId?: number
  productName?: string
  productImage?: string
  quantity?: number
}

/**
 * 异步下单状态查询结果。
 * 后端 createOrderDirect/createOrderFromCart 现在异步化，
 * 立即返回 { orderNo, status: 'processing' }，前端凭 orderNo 轮询 /order/status。
 * - processing: 订单创建中（Redis 已预扣库存，DB 尚未落库）
 * - success:    订单创建成功，orderId 已填充，可跳转详情
 * - failed:     订单创建失败（库存不足/商品下架等），reason 给出原因
 */
export interface OrderStatusResult {
  orderNo: string
  status: 'processing' | 'success' | 'failed'
  orderId?: number
  reason?: string
  totalAmount?: number
}

/* =================== 商品 API（campus-item: /item） =================== */

/* =================== 上传 API（campus-item: /item/upload） =================== */

export const uploadApi = {
  // POST /item/upload/image  (multipart/form-data)
  uploadImage: (file: File, directory = 'products'): Promise<Result<string>> => {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('directory', directory)
    return request.post('/item/upload/image', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }).then(res => res.data)
  },

  // POST /item/upload/images  (multipart/form-data)
  uploadImages: (files: File[], directory = 'products'): Promise<Result<string[]>> => {
    const formData = new FormData()
    files.forEach(f => formData.append('files', f))
    formData.append('directory', directory)
    return request.post('/item/upload/images', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }).then(res => res.data)
  },
}

export const productApi = {
  // GET /item/list?current=&size=
  getList: (current = 1, size = 8): Promise<Result<PageResult<ProductVO>>> =>
    request.get(`/item/list?current=${current}&size=${size}`).then(res => res.data)
      .catch(() => ({ code: 200, message: 'ok', data: { records: [], total: 0, current, size, pages: 0 } })),

  // GET /item/detail?productId=
  getDetail: (productId: number): Promise<Result<ProductVO>> =>
    request.get(`/item/detail?productId=${productId}`).then(res => res.data)
      .catch(() => ({ code: 200, message: 'ok', data: {} as ProductVO })),

  // POST /item/view?productId=
  incrementViewCount: (productId: number): Promise<Result<void>> =>
    request.post(`/item/view?productId=${productId}`).then(res => res.data)
      .catch(() => ({ code: 200, message: 'ok', data: undefined })),

  // GET /item/hot
  getHot: (): Promise<Result<ProductVO[]>> =>
    request.get('/item/hot').then(res => res.data)
      .catch(() => ({ code: 200, message: 'ok', data: [] })),

  // GET /item/batch?ids=1,2,3
  getBatch: (ids: number[]): Promise<Result<ProductVO[]>> =>
    request.get(`/item/batch?ids=${ids.join(',')}`).then(res => res.data)
      .catch(() => ({ code: 200, message: 'ok', data: [] })),

  // GET /item/search?keyword=&categoryId=&current=&size=
  search: (keyword: string, current = 1, size = 20, categoryId?: number): Promise<Result<PageResult<ProductVO>>> => {
    const params = new URLSearchParams({ current: String(current), size: String(size) })
    if (keyword) params.set('keyword', keyword)
    if (categoryId != null) params.set('categoryId', String(categoryId))
    return request.get(`/item/search?${params}`).then(res => res.data)
      .catch(() => ({ code: 200, message: 'ok', data: { records: [], total: 0, current, size, pages: 0 } }))
  },

  // GET /item/categories
  getCategories: (): Promise<Result<Category[]>> =>
    request.get('/item/categories').then(res => res.data)
      .catch(() => ({ code: 200, message: 'ok', data: [] })),

  // GET /item/my?current=&size=&status=
  getMyProducts: (current = 1, size = 50, status?: number): Promise<Result<PageResult<ProductVO>>> => {
    const params = new URLSearchParams({ current: String(current), size: String(size) })
    if (status != null) params.set('status', String(status))
    return request.get(`/item/my?${params}`).then(res => res.data)
      .catch(() => ({ code: 200, message: 'ok', data: { records: [], total: 0, current, size, pages: 0 } }))
  },

  // GET /item/user/{userId}?current=&size=
  getBySeller: (sellerId: number, current = 1, size = 20): Promise<Result<PageResult<ProductVO>>> =>
    request.get(`/item/user/${sellerId}?current=${current}&size=${size}`).then(res => res.data)
      .catch(() => ({ code: 200, message: 'ok', data: { records: [], total: 0, current, size, pages: 0 } })),

  // POST /item/publish  body: { product, images }
  create: (body: Partial<ProductVO> & { images?: string[] }): Promise<Result<any>> => {
    const { images, ...productData } = body
    return request.post('/item/publish', { product: productData, images: images || [] }).then(res => res.data)
  },

  // PUT /item/edit  body: { product, images }
  update: (id: number, body: Partial<ProductVO> & { images?: string[] }): Promise<Result<void>> => {
    const { images, ...productData } = body
    return request.put('/item/edit', { product: { ...productData, id }, images: images || [] }).then(res => res.data)
  },

  // DELETE /item/delete?productId=
  delete: (id: number): Promise<Result<void>> =>
    request.delete(`/item/delete?productId=${id}`).then(res => res.data),

  // PUT /item/on-shelf?productId=
  online: (id: number): Promise<Result<void>> =>
    request.put(`/item/on-shelf?productId=${id}`).then(res => res.data),

  // PUT /item/off-shelf?productId=
  offline: (id: number): Promise<Result<void>> =>
    request.put(`/item/off-shelf?productId=${id}`).then(res => res.data),
}

/* =================== 认证 API（campus-auth: /auth） =================== */

export const authApi = {
  // POST /auth/send-code  body: { phone }
  sendCode: (phone: string): Promise<Result<void>> =>
    request.post('/auth/send-code', { phone }).then(res => res.data),

  // POST /auth/register  body: { phone, code, password, nickname }
  register: (data: { phone: string; code: string; password: string; nickname?: string }): Promise<Result<void>> =>
    request.post('/auth/register', data).then(res => res.data),

  // POST /auth/login/password  body: { phone, password, code }
  loginByPassword: (phone: string, password: string): Promise<Result<LoginResult>> =>
    request.post('/auth/login/password', { phone, password }).then(res => res.data),

  // POST /auth/login/sms  body: { phone, code }
  loginBySms: (phone: string, code: string): Promise<Result<LoginResult>> =>
    request.post('/auth/login/sms', { phone, code }).then(res => res.data),

  // POST /auth/reset-password  body: { phone, code, password }
  resetPassword: (phone: string, code: string, password: string): Promise<Result<void>> =>
    request.post('/auth/reset-password', { phone, code, password }).then(res => res.data),
}

/* =================== 收藏 API（campus-user: /user/favorite） =================== */

export interface FavoriteItem {
  favoriteId: number
  userId: number
  productId: number
  createTime: string
}

export const favoriteApi = {
  // GET /user/favorite  返回收藏记录列表（含 productId）
  getList: (): Promise<Result<FavoriteItem[]>> =>
    request.get(`/user/favorite`).then(res => res.data)
      .catch(() => ({ code: 200, message: 'ok', data: [] })),

  // POST /user/favorite/{productId}
  add: (productId: number): Promise<Result<void>> =>
    request.post(`/user/favorite/${productId}`).then(res => res.data),

  // DELETE /user/favorite/{productId}
  remove: (productId: number): Promise<Result<void>> =>
    request.delete(`/user/favorite/${productId}`).then(res => res.data),

  // GET /user/favorite/{productId}/check  后端返回 Result<Boolean>，data 直接是 true/false
  check: (productId: number): Promise<Result<boolean>> =>
    request.get(`/user/favorite/${productId}/check`).then(res => res.data),
}

/* =================== 购物车 API（campus-user: /user/cart） =================== */

export const shoppingCartApi = {
  // GET /user/cart/list
  getList: (): Promise<Result<any[]>> =>
    request.get('/user/cart/list').then(res => res.data)
      .catch(() => ({ code: 200, message: 'ok', data: [] })),

  // POST /user/cart/add  body: { productId, quantity }
  add: (productId: number, quantity: number): Promise<Result<void>> =>
    request.post('/user/cart/add', { productId, quantity }).then(res => res.data),

  // PUT /user/cart/update  body: { productId, quantity }   注意：后端 quantity 按 productId 更新，前端 id 当 productId 传
  update: (productId: number, quantity: number): Promise<Result<void>> =>
    request.put('/user/cart/update', { productId, quantity }).then(res => res.data),

  // DELETE /user/cart/{productId}  注意：后端按 productId 删除，不是 itemId
  remove: (productId: number): Promise<Result<void>> =>
    request.delete(`/user/cart/${productId}`).then(res => res.data),

  // DELETE /user/cart/batch  body: { productIds }
  removeBatch: (productIds: number[]): Promise<Result<void>> =>
    request.delete('/user/cart/batch', { data: { productIds } }).then(res => res.data),

  // DELETE /user/cart/clear
  clear: (): Promise<Result<void>> =>
    request.delete('/user/cart/clear').then(res => res.data),

  // checkout 仍由订单服务处理（/order/create-from-cart），这里给个别名
  checkout: (body: { cartItemIds?: number[]; productIds?: number[]; quantities?: number[]; name?: string; phone?: string; address?: string }): Promise<Result<OrderStatusResult>> =>
    ordersApi.createFromCart(body),
}

/* =================== 订单 API（campus-order，后端路径 /api/orders） =================== */

export const ordersApi = {
  // GET /api/orders/buy?current=&size=&status=
  getBuyList: (status: number | null = null, current = 1, size = 50): Promise<Result<OrderVO[]>> => {
    const params = new URLSearchParams({ current: String(current), size: String(size) })
    if (status !== null && status !== undefined) params.set('status', String(status))
    return request.get(`/orders/buy?${params}`).then(res => res.data)
      .catch(() => ({ code: 200, message: 'ok', data: [] }))
  },

  // GET /api/orders/sell?current=&size=&status=
  getSellList: (status: number | null = null, current = 1, size = 50): Promise<Result<OrderVO[]>> => {
    const params = new URLSearchParams({ current: String(current), size: String(size) })
    if (status !== null && status !== undefined) params.set('status', String(status))
    return request.get(`/orders/sell?${params}`).then(res => res.data)
      .catch(() => ({ code: 200, message: 'ok', data: [] }))
  },

  // 兼容旧调用：默认返回买入订单
  getList: (status: number | null = null, current = 1, size = 50): Promise<Result<OrderVO[]>> =>
    ordersApi.getBuyList(status, current, size),

  // GET /api/orders/{orderId}
  getDetail: (orderId: number): Promise<Result<OrderVO>> =>
    request.get(`/orders/${orderId}`).then(res => res.data),

  // POST /api/orders/direct  对应后端 OrderController 的 @PostMapping("/direct")
  createDirect: (productId: number, quantity: number, receiverInfo: { name: string; phone: string; address: string }): Promise<Result<OrderStatusResult>> =>
    request.post('/orders/direct', {
      productId, quantity,
      receiverName: receiverInfo.name,
      receiverPhone: receiverInfo.phone,
      receiverAddress: receiverInfo.address,
    }).then(res => res.data),

  // POST /api/orders/from-cart  对应后端 OrderController 的 @PostMapping("/from-cart")
  createFromCart: (body: { productIds?: number[]; quantities?: number[]; name?: string; phone?: string; address?: string; remark?: string }): Promise<Result<OrderStatusResult>> =>
    request.post('/orders/from-cart', {
      productIds: body.productIds || [],
      quantities: body.quantities || [],
      receiverName: body.name,
      receiverPhone: body.phone,
      receiverAddress: body.address,
    }).then(res => res.data),

  // GET /api/orders/status?orderNo=  异步下单状态轮询接口
  getOrderStatus: (orderNo: string): Promise<Result<OrderStatusResult>> =>
    request.get(`/orders/status?orderNo=${encodeURIComponent(orderNo)}`).then(res => res.data),

  // PUT /api/orders/{orderId}/pay
  pay: (orderId: number): Promise<Result<void>> =>
    request.put(`/orders/${orderId}/pay`).then(res => res.data),

  // PUT /api/orders/{orderId}/cancel
  cancel: (orderId: number): Promise<Result<void>> =>
    request.put(`/orders/${orderId}/cancel`).then(res => res.data),

  // PUT /api/orders/{orderId}/ship
  ship: (orderId: number, body?: { trackingNo?: string }): Promise<Result<void>> =>
    request.put(`/orders/${orderId}/ship`, body).then(res => res.data),

  // PUT /api/orders/{orderId}/confirm
  receive: (orderId: number): Promise<Result<void>> =>
    request.put(`/orders/${orderId}/confirm`).then(res => res.data),

  confirm: (orderId: number): Promise<Result<void>> =>
    request.put(`/orders/${orderId}/confirm`).then(res => res.data),
}

/* =================== 用户 API（campus-user: /user） =================== */

export const userApi = {
  // GET /user/info
  getInfo: (): Promise<Result<UserInfo>> =>
    request.get('/user/info').then(res => res.data),

  // GET /user/home/{userId} — 用户主页完整信息（含粉丝/关注数）
  getUserHome: (userId: number): Promise<Result<any>> =>
    request.get(`/user/home/${userId}`).then(res => res.data)
      .catch(() => ({ code: 200, message: 'ok', data: { id: userId, nickname: '用户' + userId } })),

  // PUT /user/info  body: { nickname, email }
  updateProfile: (data: Partial<UserInfo>): Promise<Result<void>> =>
    request.put('/user/info', data).then(res => res.data),

  // PUT /user/avatar  body: { avatar }
  updateAvatar: (avatar: string): Promise<Result<void>> =>
    request.put('/user/avatar', { avatar }).then(res => res.data),

  // 后端无改密接口，前端兜底
  changePassword: (oldPassword: string, newPassword: string): Promise<Result<void>> =>
    Promise.resolve({ code: 500, message: '修改密码功能未开放', data: undefined }),

  // GET /user/address
  getAddresses: (): Promise<Result<any[]>> =>
    request.get('/user/address').then(res => res.data).catch(() => ({ code: 200, message: 'ok', data: [] })),

  // GET /user/address/default
  getDefaultAddress: (): Promise<Result<any>> =>
    request.get('/user/address/default').then(res => res.data).catch(() => ({ code: 200, message: 'ok', data: null })),

  // POST /user/address
  addAddress: (body: any): Promise<Result<{ id: number }>> =>
    request.post('/user/address', body).then(res => res.data),

  // PUT /user/address/{addressId}
  updateAddress: (id: number, body: any): Promise<Result<void>> =>
    request.put(`/user/address/${id}`, body).then(res => res.data),

  // DELETE /user/address/{addressId}
  removeAddress: (id: number): Promise<Result<void>> =>
    request.delete(`/user/address/${id}`).then(res => res.data),

  // PUT /user/address/{addressId}/default
  setDefaultAddress: (id: number): Promise<Result<void>> =>
    request.put(`/user/address/${id}/default`).then(res => res.data),
}

/* =================== 关注 API（campus-user: /user/follow） =================== */

export const followApi = {
  // GET /user/follow
  getList: (): Promise<Result<any>> =>
    request.get('/user/follow').then(res => res.data).catch(() => ({ code: 200, message: 'ok', data: [] })),

  // POST /user/follow/{followUserId}
  follow: (followUserId: number): Promise<Result<void>> =>
    request.post(`/user/follow/${followUserId}`).then(res => res.data),

  // DELETE /user/follow/{followUserId}
  unfollow: (followUserId: number): Promise<Result<void>> =>
    request.delete(`/user/follow/${followUserId}`).then(res => res.data),

  // GET /user/follow/{followUserId}/check
  check: (followUserId: number): Promise<Result<{ isFollow: boolean }>> =>
    request.get(`/user/follow/${followUserId}/check`).then(res => res.data),
}

/* =================== 管理员 API（campus-admin: /admin） =================== */
// 注意：后端无独立 /admin/login，管理员登录复用 /auth/login/password（username 作为 phone 传入）
// 后端会根据 user.role 判断是否为管理员

export const adminApi = {
  // 复用 /auth/login/password，username 作为 phone
  login: (username: string, password: string): Promise<Result<LoginResult>> =>
    request.post('/auth/login/password', { phone: username, password }).then(res => res.data),

  // GET /admin/stats
  getStats: (): Promise<Result<any>> =>
    request.get('/admin/stats').then(res => res.data)
      .catch(() => ({
        code: 200, message: 'ok',
        data: {
          userCount: 0, productCount: 0, orderCount: 0, totalAmount: 0,
          todayUserCount: 0, todayOrderCount: 0, todayAmount: 0, onSaleCount: 0, categoryCount: 0,
        },
      })),
}

export const adminUserApi = {
  // GET /admin/users?keyword=&current=&size=
  getList: (keyword = '', current = 1, size = 10, status?: number): Promise<Result<PageResult<UserInfo>>> => {
    const params = new URLSearchParams({ current: String(current), size: String(size) })
    if (keyword) params.set('keyword', keyword)
    return request.get(`/admin/users?${params}`).then(res => res.data)
      .catch(() => ({ code: 200, message: 'ok', data: { records: [], total: 0, current, size, pages: 0 } }))
  },
  // PUT /admin/users/{userId}/status?status=1  启用
  enable: (id: number): Promise<Result<void>> =>
    request.put(`/admin/users/${id}/status?status=1`).then(res => res.data),
  // PUT /admin/users/{userId}/status?status=0  禁用
  disable: (id: number): Promise<Result<void>> =>
    request.put(`/admin/users/${id}/status?status=0`).then(res => res.data),
  // 后端无删除用户接口，前端兜底
  remove: (id: number): Promise<Result<void>> =>
    Promise.resolve({ code: 500, message: '后端暂未提供删除用户接口', data: undefined }),
}

export const adminProductApi = {
  // GET /admin/products?keyword=&current=&size=
  getList: (keyword = '', current = 1, size = 10, status?: number): Promise<Result<PageResult<ProductVO>>> => {
    const params = new URLSearchParams({ current: String(current), size: String(size) })
    if (keyword) params.set('keyword', keyword)
    return request.get(`/admin/products?${params}`).then(res => res.data)
      .catch(() => ({ code: 200, message: 'ok', data: { records: [], total: 0, current, size, pages: 0 } }))
  },
  // 后端无独立上下架接口，复用 PUT /admin/products/{productId} 修改 status
  // 后端 AdminController 只有 updateProduct（PUT /admin/products/{productId}）和 deleteProduct
  online: (id: number): Promise<Result<void>> =>
    request.put(`/admin/products/${id}`, { status: 1 }).then(res => res.data),
  offline: (id: number): Promise<Result<void>> =>
    request.put(`/admin/products/${id}`, { status: 2 }).then(res => res.data),
  // DELETE /admin/products/{productId}
  remove: (id: number): Promise<Result<void>> =>
    request.delete(`/admin/products/${id}`).then(res => res.data),
}

export const adminOrderApi = {
  // GET /admin/orders?keyword=&status=&current=&size=
  getList: (keyword = '', current = 1, size = 10, status?: number): Promise<Result<PageResult<OrderVO>>> => {
    const params = new URLSearchParams({ current: String(current), size: String(size) })
    if (keyword) params.set('keyword', keyword)
    if (status !== undefined && status !== null) params.set('status', String(status))
    return request.get(`/admin/orders?${params}`).then(res => res.data)
      .catch(() => ({ code: 200, message: 'ok', data: { records: [], total: 0, current, size, pages: 0 } }))
  },
}

export const adminCategoryApi = {
  // GET /admin/categories
  getList: (): Promise<Result<Category[]>> =>
    request.get('/admin/categories').then(res => res.data).catch(() => ({ code: 200, data: [] })),
  // POST /admin/categories  body: { categoryName, categoryDesc, sortOrder }
  create: (body: { name: string; description?: string; sort?: number }): Promise<Result<{ id: number }>> =>
    request.post('/admin/categories', {
      categoryName: body.name,
      categoryDesc: body.description || '',
      sortOrder: body.sort ?? 0,
    }).then(res => res.data),
  // PUT /admin/categories/{categoryId}  body: { categoryName, categoryDesc, sortOrder }
  update: (id: number, body: Partial<Category>): Promise<Result<void>> =>
    request.put(`/admin/categories/${id}`, {
      categoryName: body.categoryName ?? body.name,
      categoryDesc: body.categoryDesc ?? body.description ?? '',
      sortOrder: body.sortOrder ?? body.sort ?? 0,
    }).then(res => res.data),
  // DELETE /admin/categories/{categoryId}
  remove: (id: number): Promise<Result<void>> =>
    request.delete(`/admin/categories/${id}`).then(res => res.data),
}

/* =================== 系统图片/轮播图 API（campus-admin: /admin/system-images） =================== */

export interface SystemImageVO {
  imageId: number
  imageName: string
  imageUrl: string
  description: string
  type: number
  status: number
  sortOrder: number
  createTime?: string
}

export const adminSystemImageApi = {
  // GET /admin/system-images?type=&current=&size=
  getList: (type = 0, current = 1, size = 20): Promise<Result<PageResult<SystemImageVO>>> => {
    const params = new URLSearchParams({ type: String(type), current: String(current), size: String(size) })
    return request.get(`/admin/system-images?${params}`).then(res => res.data)
      .catch(() => ({ code: 200, message: 'ok', data: { records: [], total: 0, current, size, pages: 0 } }))
  },
  // POST /admin/system-images/upload  (multipart/form-data)
  upload: (file: File, data: { imageName?: string; type?: number; description?: string; sortOrder?: number }): Promise<Result<void>> => {
    const formData = new FormData()
    formData.append('file', file)
    if (data.imageName) formData.append('imageName', data.imageName)
    if (data.type != null) formData.append('type', String(data.type))
    if (data.description) formData.append('description', data.description)
    formData.append('sortOrder', String(data.sortOrder ?? 0))
    return request.post('/admin/system-images/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }).then(res => res.data)
  },
  // PUT /admin/system-images/{imageId}?imageName=&type=&description=&sortOrder=
  update: (id: number, data: { imageName?: string; type?: number; description?: string; sortOrder?: number }): Promise<Result<void>> => {
    const params = new URLSearchParams()
    if (data.imageName) params.set('imageName', data.imageName)
    if (data.type != null) params.set('type', String(data.type))
    if (data.description) params.set('description', data.description)
    if (data.sortOrder != null) params.set('sortOrder', String(data.sortOrder))
    return request.put(`/admin/system-images/${id}?${params}`).then(res => res.data)
  },
  // PUT /admin/system-images/{imageId}/status?status=1|0
  toggleStatus: (id: number, status: number): Promise<Result<void>> =>
    request.put(`/admin/system-images/${id}/status?status=${status}`).then(res => res.data),
  // DELETE /admin/system-images/{imageId}
  remove: (id: number): Promise<Result<void>> =>
    request.delete(`/admin/system-images/${id}`).then(res => res.data),
  // GET /admin/system-images/public/banners
  getBanners: (): Promise<Result<SystemImageVO[]>> =>
    request.get('/admin/system-images/public/banners').then(res => res.data)
      .catch(() => ({ code: 200, message: 'ok', data: [] })),
}

/* =================== 用户站内消息通知（campus-user: /user/notification） =================== */

export const notificationApi = {
  // GET /user/notification/list?receiverId=&type=&current=&size=
  getList: (receiverId: number, type?: number, current = 1, size = 20): Promise<Result<PageResult<any>>> => {
    const params = new URLSearchParams({ receiverId: String(receiverId), current: String(current), size: String(size) })
    if (type !== undefined && type !== null) params.set('type', String(type))
    return request.get(`/user/notification/list?${params}`).then(res => res.data)
      .catch(() => ({ code: 200, message: 'ok', data: { records: [], total: 0, current, size, pages: 0 } }))
  },
  // GET /user/notification/unread/count?receiverId=
  unreadCount: (receiverId: number): Promise<Result<number>> =>
    request.get(`/user/notification/unread/count?receiverId=${receiverId}`).then(res => res.data)
      .catch(() => ({ code: 200, message: 'ok', data: 0 })),
  // GET /user/notification/unread/count-by-type?receiverId=
  unreadCountByType: (receiverId: number): Promise<Result<Record<string, number>>> =>
    request.get(`/user/notification/unread/count-by-type?receiverId=${receiverId}`).then(res => res.data)
      .catch(() => ({ code: 200, message: 'ok', data: {} })),
  // GET /user/notification/detail/{id}
  detail: (id: number): Promise<Result<any>> =>
    request.get(`/user/notification/detail/${id}`).then(res => res.data),
  // POST /user/notification/mark-all-read?receiverId=
  markAllRead: (receiverId: number): Promise<Result<void>> =>
    request.post(`/user/notification/mark-all-read?receiverId=${receiverId}`).then(res => res.data),
  // POST /user/notification/mark-read-by-type?receiverId=&type=
  markReadByType: (receiverId: number, type?: number): Promise<Result<void>> => {
    const params = new URLSearchParams({ receiverId: String(receiverId) })
    if (type !== undefined && type !== null) params.set('type', String(type))
    return request.post(`/user/notification/mark-read-by-type?${params}`).then(res => res.data)
  },
  // DELETE /user/notification/{id}
  remove: (id: number): Promise<Result<void>> =>
    request.delete(`/user/notification/${id}`).then(res => res.data),
  // POST /user/notification/send
  send: (body: any): Promise<Result<void>> =>
    request.post('/user/notification/send', body).then(res => res.data),
}

/* =================== 用户聊天私信 API（campus-user: /user/chat，type=3 消息） =================== */

export const chatApi = {
  // GET /user/chat/conversations
  getConversations: (): Promise<Result<any[]>> =>
    request.get('/user/chat/conversations').then(res => res.data)
      .catch(() => ({ code: 200, message: 'ok', data: [] })),
  // GET /user/chat/messages?userId=&productId=&current=&size=
  getMessages: (userId: number, productId?: number, current = 1, size = 20): Promise<Result<PageResult<any>>> => {
    const params = new URLSearchParams({ userId: String(userId), current: String(current), size: String(size) })
    if (productId) params.set('productId', String(productId))
    return request.get(`/user/chat/messages?${params}`).then(res => res.data)
      .catch(() => ({ code: 200, message: 'ok', data: { records: [], total: 0, current, size, pages: 0 } }))
  },
  // POST /user/chat/send  body: { receiverId, productId, content }
  send: (body: { receiverId: number; productId?: number; content: string }): Promise<Result<any>> =>
    request.post('/user/chat/send', body).then(res => res.data),
  // GET /user/chat/unread?userId=
  unreadCount: (userId: number): Promise<Result<number>> =>
    request.get(`/user/chat/unread?userId=${userId}`).then(res => res.data)
      .catch(() => ({ code: 200, message: 'ok', data: 0 })),
  // POST /user/chat/mark-read  body: { userId, productId }
  markAsRead: (body: { userId: number; productId?: number }): Promise<Result<void>> =>
    request.post('/user/chat/mark-read', body).then(res => res.data),
  // GET /user/chat/open/{userId}
  open: (userId: number): Promise<Result<any>> =>
    request.get(`/user/chat/open/${userId}`).then(res => res.data),
}

export const adminMessageApi = {
  // 管理员端消息列表（campus-user 内部接口：senderId=0 的站方系统通知）
  getList: (keyword = '', current = 1, size = 10, type?: number): Promise<Result<PageResult<any>>> => {
    const params = new URLSearchParams({ current: String(current), size: String(size) })
    if (keyword) params.set('keyword', keyword)
    if (type !== undefined && type !== null) params.set('type', String(type))
    return request.get(`/notification/internal/admin-list?${params}`).then(res => res.data)
      .catch(() => ({ code: 200, message: 'ok', data: { records: [], total: 0, current, size, pages: 0 } }))
  },
  // POST /admin/message/send  body: { title, content, type, userId }
  send: (body: { title: string; content: string; type?: number; targetType?: number; userId?: number }): Promise<Result<void>> =>
    request.post('/admin/message/send', body).then(res => res.data),
  // DELETE /user/notification/internal/admin-batch/{id}  删除整批（该批次所有接收行）
  remove: (id: number): Promise<Result<void>> =>
    request.delete(`/notification/internal/admin-batch/${id}`).then(res => res.data),
}

export const adminSystemApi = {
  // GET /admin/system/admins?keyword=&current=&size=
  getAdmins: (keyword = '', current = 1, size = 10): Promise<Result<PageResult<any>>> => {
    const params = new URLSearchParams({ current: String(current), size: String(size) })
    if (keyword) params.set('keyword', keyword)
    return request.get(`/admin/system/admins?${params}`).then(res => res.data)
      .catch(() => ({ code: 200, message: 'ok', data: { records: [], total: 0, current, size, pages: 0 } }))
  },
  // POST /admin/system/admin  (实际返回：请用角色更新接口)
  createAdmin: (body: any): Promise<Result<{ id: number }>> =>
    request.post('/admin/system/admin', body).then(res => res.data),
  // PUT /admin/system/admin/{id}
  updateAdmin: (id: number, body: any): Promise<Result<void>> =>
    request.put(`/admin/system/admin/${id}`, body).then(res => res.data),
  // PUT /admin/system/admin/{id}/enable
  enableAdmin: (id: number): Promise<Result<void>> =>
    request.put(`/admin/system/admin/${id}/enable`).then(res => res.data),
  // PUT /admin/system/admin/{id}/disable
  disableAdmin: (id: number): Promise<Result<void>> =>
    request.put(`/admin/system/admin/${id}/disable`).then(res => res.data),
  // DELETE /admin/system/admin/{id}  (降级为普通用户，不是删用户)
  removeAdmin: (id: number): Promise<Result<void>> =>
    request.delete(`/admin/system/admin/${id}`).then(res => res.data),
}
