# ============================================================
# 前端 + Nginx 镜像：多阶段构建
# 阶段1：node 构建 Vue3 dist；阶段2：Nginx 托管静态文件并反代网关
# ============================================================
FROM node:22-alpine AS frontend-build
WORKDIR /front
COPY front/package*.json ./
RUN npm ci --no-audit --no-fund
COPY front/ ./
# build-only 跳过 vue-tsc 类型检查，保证部署构建稳定；本地可用 npm run build 全量校验
RUN npm run build-only

FROM nginx:alpine
COPY --from=frontend-build /front/dist /usr/share/nginx/html
COPY nginx/conf.d/default.conf /etc/nginx/conf.d/default.conf
EXPOSE 80 443
