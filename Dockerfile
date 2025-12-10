FROM eclipse-temurin:21-jdk

# 1. 타임존 환경변수 설정
ENV TZ=Asia/Seoul

# 2. 타임존 링크 설정
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 3. JAR 파일 복사
ARG JAR_FILE=/build/libs/*.jar
COPY ${JAR_FILE} app.jar

# 4. 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "/app.jar"]