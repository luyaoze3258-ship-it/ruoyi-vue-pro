FROM ekb-repo.tencentcloudcr.com/library/runtime-openjdk:11

WORKDIR /opt/admin-shell

COPY AdminShellServer.java index.html ./

RUN javac AdminShellServer.java

ENV PORT=8080 \
    BACKEND_BASE=http://ruoyi-vue-pro-server:48080 \
    JAVA_TOOL_OPTIONS="-Xms64m -Xmx128m"

EXPOSE 8080

CMD ["java", "AdminShellServer"]
