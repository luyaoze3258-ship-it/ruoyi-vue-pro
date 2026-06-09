FROM ekb-repo.tencentcloudcr.com/library/runtime-openjdk:11

WORKDIR /opt/admin-vben

COPY deploy/admin-vben/VbenAdminServer.java ./
COPY yudao-ui/yudao-ui-admin-vben/apps/web-antd/dist ./dist

RUN javac VbenAdminServer.java

ENV PORT=8080 \
    BACKEND_BASE=http://ruoyi-vue-pro-server:48080 \
    JAVA_TOOL_OPTIONS="-Xms64m -Xmx160m"

EXPOSE 8080

CMD ["java", "VbenAdminServer"]
