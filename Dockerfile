FROM tomcat:9.0-jre17-temurin

RUN rm -rf /usr/local/tomcat/webapps/ROOT
COPY . /usr/local/tomcat/webapps/ROOT/

COPY entrypoint.sh /usr/local/tomcat/entrypoint.sh
RUN chmod +x /usr/local/tomcat/entrypoint.sh

EXPOSE 8080
CMD ["/bin/sh", "/usr/local/tomcat/entrypoint.sh"]
