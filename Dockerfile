FROM jetty:jre8

COPY ./webapp/target/webapp /var/lib/jetty/webapps/ROOT
COPY ./docker/logback.xml /var/lib/jetty/webapps/ROOT/WEB-INF/classes/
COPY ./docker/application.properties /var/lib/jetty/webapps/ROOT/WEB-INF/classes/

USER root
RUN mkdir -p /var/lib/jetty/webapps/ROOT/WEB-INF/tmp
USER jetty

VOLUME /var/lib/jetty/webapps/ROOT/WEB-INF/tmp

HEALTHCHECK --interval=5s --timeout=20s --retries=3 \
CMD wget http://localhost:8080/ -q -O - > /dev/null 2>&1