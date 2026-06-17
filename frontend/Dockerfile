# 阶段一：构建 Vue 产物
FROM node:20-alpine as builder
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# 阶段二：配置 Nginx
FROM nginx:alpine
# 把编译好的前端文件放进 Nginx 默认目录
COPY --from=builder /app/dist /usr/share/nginx/html
# 覆盖默认的 nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
