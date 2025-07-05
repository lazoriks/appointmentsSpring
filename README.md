# Render + Spring Boot
Render підтримує:

деплой через GitHub/GitLab

автоматичне оновлення після коміту

підтримку PostgreSQL / MySQL як DB (або сторонні)

безкоштовний план для dev/test

🔧 Як деплоїти:
Push коду в GitHub (або GitLab)

Зайди в https://render.com

Обери "Web Service" → "Connect your repo"

Укажи:

Build Command: ./mvnw clean package

Start Command: java -jar target/your-app.jar

Environment: Java 17 (або 21, якщо треба)

# How to set these in Render dashboard
Build Command: ./mvnw clean package

Start Command: java -jar target/appointmentsSpring.jar

# chmod : The term 'chmod' is not recognized as the name of a cmdlet, function, script file, or operable program. Check
the spelling of the name, or if a path was included, verify that the path is correct and try again.
At line:1 char:1
+ chmod +x mvnw
+ ~~~~~
    + CategoryInfo          : ObjectNotFound: (chmod:String) [], CommandNotFoundException
    + FullyQualifiedErrorId : CommandNotFoundException

## Option 1: Use Git Bash or WSL (Recommended)
If you have Git for Windows, open Git Bash (right-click your repo folder → Git Bash Here).

Then run:
chmod +x mvnw
Commit and push after that.

## Option 2: Set execute permission via Windows file properties (less common)
Right-click the mvnw file in Explorer.

Go to Properties → Security tab.

Check if your user has Full control or at least Modify permissions.

Usually, Windows doesn’t use execute bits like Unix, but if you use Git Bash or WSL it’ll handle it for you.

## Option 3: Use PowerShell to set permission (not the same as chmod)
PowerShell doesn’t support chmod +x because Windows doesn’t manage execute permissions the same way. For Git Bash or CI builds, the execute bit needs to be set in the Git repo.

What to do now?
### If you don’t have Git Bash, you can install it: Git for Windows

Then run chmod +x mvnw inside Git Bash, commit, and push.

After that, Render should be able to execute the mvnw script without permission errors.

### 1. Locally make mvnw executable and commit
Use Git Bash or WSL on Windows, then:

chmod +x mvnw
git add mvnw
git commit -m "Make mvnw executable"
git push

If you can’t do this on your machine, I can help you generate a quick script or guide to fix it.

## У Render в Dashboard -> Environment створи змінні:

Key  Value
SPRING_DATASOURCE_URL  jdbc:mysql://appointmentdb.ch8kskc0cuv1.eu-north-1.rds.amazonaws.com:3306/appointmentdb
SPRING_DATASOURCE_USERNAME  admin
SPRING_DATASOURCE_PASSWORD  твій пароль
SPRING_JPA_HIBERNATE_DDL_AUTO  none
SPRING_JPA_DATABASE_PLATFORM  org.hibernate.dialect.MySQL8Dialect
SPRING_JPA_SHOW_SQL  true
SPRING_JPA_PROPERTIES_HIBERNATE_FORMAT_SQL  true

## spring.application.name=appointments

### Database connection
spring.datasource.url=${SPRING_DATASOURCE_URL}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}

### JPA / Hibernate
spring.jpa.hibernate.ddl-auto=${SPRING_JPA_HIBERNATE_DDL_AUTO:none}
spring.jpa.database-platform=${SPRING_JPA_DATABASE_PLATFORM}
spring.jpa.show-sql=${SPRING_JPA_SHOW_SQL:true}
spring.jpa.properties.hibernate.format_sql=${SPRING_JPA_PROPERTIES_HIBERNATE_FORMAT_SQL:true}

### Let Render control the port
server.port=${PORT:8080}

