# Stage 1 — Compile all Java 21 sources against Tomcat 9's servlet-api and WEB-INF/lib.
FROM tomcat:9.0-jdk21-temurin AS builder

WORKDIR /app
COPY . /app

RUN mkdir -p WEB-INF/classes && \
    find WEB-INF/src -name "*.java" > /tmp/sources.txt && \
    javac -encoding UTF-8 -d WEB-INF/classes \
          -cp "/usr/local/tomcat/lib/servlet-api.jar:WEB-INF/lib/*" \
          @/tmp/sources.txt

# Stage 2 — Slim Tomcat 9 on JRE 21 runtime (Railway injects $PORT).
FROM tomcat:9.0-jre21-temurin

# Serve the app at the root context ("/").
RUN rm -rf /usr/local/tomcat/webapps/ROOT
COPY --from=builder /app /usr/local/tomcat/webapps/ROOT/

# Bind Tomcat to Railway's $PORT at container start.
COPY entrypoint.sh /usr/local/tomcat/entrypoint.sh
RUN chmod +x /usr/local/tomcat/entrypoint.sh

EXPOSE 8080
CMD ["/bin/sh", "/usr/local/tomcat/entrypoint.sh"]
