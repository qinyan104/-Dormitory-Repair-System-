# 阶段 1：构建阶段
FROM node:20-slim AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# 阶段 2：运行阶段（使用 Nginx 托管静态文件）
FROM nginx:stable-alpine
# 复制构建后的静态文件到 Nginx 目录
COPY --from=build /app/dist /usr/share/nginx/html
# 复制自定义 Nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]