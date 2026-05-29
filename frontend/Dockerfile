FROM node:20-alpine AS build
WORKDIR /app

# 使用缓存卷保存 npm 依赖
RUN --mount=type=cache,target=/root/.npm \
    echo "npm cache mounted"

COPY package*.json ./

# 依赖层 - 只在 package.json 变化时重新构建
RUN --mount=type=cache,target=/root/.npm \
    npm install

COPY . .
ARG VITE_API_BASE_URL=/api
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
