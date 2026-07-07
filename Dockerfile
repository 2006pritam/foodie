# Foodie — Tomcat 9 on JRE 21 (matches the app's Java 21 bytecode).
# Railway builds this image from the repo and runs it, injecting $PORT.
FROM tomcat:9.0-jre21-temurin

# Serve the app at the root context ("/").
RUN rm -rf /usr/local/tomcat/webapps/ROOT
COPY . /usr/local/tomcat/webapps/ROOT/

# Bind Tomcat to Railway's $PORT at container start.
COPY entrypoint.sh /usr/local/tomcat/entrypoint.sh
RUN chmod +x /usr/local/tomcat/entrypoint.sh

EXPOSE 8080
CMD ["/bin/sh", "/usr/local/tomcat/entrypoint.sh"]
