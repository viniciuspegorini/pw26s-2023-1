# Deploy de aplicações utilizando Docker

## O que é o Docker?

O Docker é uma plataforma de código aberto que permite a criação, o empacotamento e a execução de aplicativos em contêineres. Os contêineres são unidades de software portáteis e leves que encapsulam todos os componentes necessários para a execução de um aplicativo, incluindo código, runtime, bibliotecas e dependências. Os contêineres são isolados uns dos outros e podem ser executados em qualquer ambiente que suporte o Docker, tornando o desenvolvimento, o teste e a implantação de aplicativos mais rápidos e eficientes.

O Docker utiliza a tecnologia de virtualização a nível de sistema operacional (também conhecida como virtualização de contêineres) para criar e gerenciar contêineres. Isso permite que os contêineres compartilhem o núcleo do sistema operacional do host, tornando-os mais leves e rápidos em comparação com as máquinas virtuais tradicionais.

Uma das principais vantagens do Docker é a capacidade de criar imagens de contêiner, que são artefatos autossuficientes e reutilizáveis que contêm todos os componentes necessários para a execução de um aplicativo. As imagens de contêiner podem ser compartilhadas em repositórios Docker, por exemplo o Docker Hub [2] e facilmente implantadas em ambientes de desenvolvimento, teste e produção, proporcionando consistência e confiabilidade ao longo do ciclo de vida de um aplicativo.

O Docker é amplamente utilizado no desenvolvimento de software para criar ambientes de desenvolvimento replicáveis, simplificar a implantação de aplicativos em ambientes de produção e facilitar a adoção de arquiteturas de microsserviços. Ele também é usado em conjunto com outras tecnologias, como orquestradores de contêineres (por exemplo, Docker Swarm [3] e Kubernetes [4]) para gerenciar e escalar aplicativos em contêineres em ambientes de produção complexos. 

## Instalando o docker e docker compose

A instalação do serviço docker vai despender do sistema operacional, para instalar em um servidor Linux, basta seguir o descrito em [5]:  

1. Atualizando o `apt`:
```cmd
sudo apt-get update
sudo apt-get install \
	ca-certificates \
	curl \
	gnupg
```

2. Adicionando a GPG key:
```cmd
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg
```

3. Inicia o repositório:
```cmd
echo \
"deb [arch="$(dpkg --print-architecture)" signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
"$(. /etc/os-release && echo "$VERSION_CODENAME")" stable" | \
sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
```

4. Atualizando o repositório:
```cmd
sudo apt-get update
```

5. Instalando os componentes necessários:
```cmd
sudo apt-get install docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
```

6. Verificando a instalação:
```cmd
sudo docker run hello-world
```

## Criando os arquivos Docker e Docker-compose para o *deploy* da aplicação Server

As configurações para o *deploy* da aplicação server estão dentro da pasta server. Neste projeto está sendo utilizado o **PostgreSQL** como servidor de banco de dados e o **Minio** para armazenamento de arquivos de *upload* e esses serviços também serão instalados com o restante da aplicação. Entretanto, isso não é obrigatório, principalmente quando um servidor é responsável por servir mais de uma aplicação que consome os serviços de banco de dados ou armazenamento de objetos.

O primeiro passo para realização da instalação da aplicação servidor, é a criação do arquivo com as configurações para criação do contêiner Docker na raiz da pasta que contém o projeto **server** (arquivo com o nome **Dockerfile**. O conteúdo do arquivo é apresentado abaixo:

```Dockerfile
# BUILD
FROM eclipse-temurin:17-jdk-alpine as build
WORKDIR /workspace/labs

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src

# clean up the file
RUN sed -i 's/\r$//' mvnw
# create package
RUN /bin/sh mvnw package -DskipTests

# DIST
FROM openjdk:17
COPY --from=build /workspace/labs/target/server-0.1.jar server.jar
ENTRYPOINT ["java", "-jar", "server.jar"]
```
Pode-se considerar o arquivo é dividido em duas partes, a primeira com o estágio de *Build* da aplicação e a segunda parte com a construção da imagem. Com as configurações da imagem criada é possível executar a criação do contêiner, entretanto, para tornar melhor o gerenciamento da criação será utilizado um arquivo do tipo docker-compose. Assim, não precisamos passar as configurações do contêiner por parâmetro no comando docker e sim armazenamos as configurações no arquivo docker-compose.

Além dos dados para criação da imagem da aplicação **server** também serão criados os serviços do **PostgreSQL** e **Minio**, como pode ser observado no aquivo **docker-compose.yml** que é apresentado abaixo:

```yml
version: "3.9"
########################### SERVICES
services:
  ########################### POSTGRESQL
  postgresql:
    image: "postgres:14.2"
    container_name: postgresql
    ports:
      - "5432:5432"
    volumes:
      - ./pgdata:/var/lib/postgresql/data
    environment:
      - POSTGRES_USER=${DATABASE_USERNAME}
      - POSTGRES_PASSWORD=${DATABASE_PASSWORD}
      - POSTGRES_DB=${DATABASE_NAME}
    networks:
      - database
    restart: unless-stopped
  ########################### MINIO
  minio:
    image: minio/minio:latest
    container_name: minio
    environment:
      MINIO_ROOT_USER: "minioadmin"
      MINIO_ROOT_PASSWORD: "minioadmin"
    volumes:
      - ./data:/data
    ports:
      - 9000:9000
      - 9001:9001
    restart: unless-stopped
    command: server /data --console-address :9001
  ########################### SERVER
  server:
    image: server
    container_name: server
    build:
      context: ./
      dockerfile: Dockerfile
    environment:
      - SERVER_PORT=${SERVER_PORT}
      - DATABASE_URL=${DATABASE_URL}
      - DATABASE_USERNAME=${DATABASE_USERNAME}
      - DATABASE_PASSWORD=${DATABASE_PASSWORD}
      - GOOGLE_CLIENT_ID=${GOOGLE_CLIENT_ID}
      - SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE}
      - TZ=America/Sao_Paulo
    ports:
      - "8080:8080"
    restart: unless-stopped
    networks:
      - web
      - database    

########################### NETWORKS
networks:
  web:
    name: web
    driver: bridge
    external: true
    attachable: true
  database:
    name: database
    driver: bridge
    external: false
    attachable: true
```
Antes de executar o arquivo para criação dos serviços é necessário realizar algumas configurações no arquivo **docker-compose.yml** é possível observar algumas variáveis de ambiente necessárias para as aplicações funcionarem, por exemplo o usuário e senha do PostgreSQL (*\${DATABASE_USERNAME}* e *\${DATABASE_PASSWORD}*), o nome do banco de dados, a URL da conexão com o banco, a porta em que a aplicação server será executada, entre outras. Para essas configurações será criado um arquivo `.env` na raiz da aplicação. As variáveis adicionadas no arquivo *.env* serão importados pelo docker compose ao configurar o ambiente em que será executada a aplicação. Bastando adicionar as configurações no arquivo de propriedades da aplicação server.

Inicialmente será adicionada as bibliotecas contendo o *driver* do PostgreSQL e a dependência para uso das variáveis do arquivo *.env* dentro da aplicaçao **server**. Essas modificações foram realizadas no arquivo **pom.xml** conforme exemplo abaixo:

```xml
<!-- ... -->
<dependencies>
	<dependency>  
	   <groupId>org.postgresql</groupId>  
	   <artifactId>postgresql</artifactId>  
	   <scope>runtime</scope>  
	</dependency>  
	<dependency>  
	   <groupId>me.paulschwarz</groupId>  
	   <artifactId>spring-dotenv</artifactId>  
	   <version>3.0.0</version>  
	</dependency>
</dependencies>
<!-- ... -->
```

O próximo passo é utilizar as variáveis de ambiente dentro de arquivo de configurações da aplicação **server**. Para isso, o arquivo `application.yml` foi alterado utilizando o formato `${NOME_VARIAVEL:VALOR_DEFAULT}`, em que `NOME_VARIAVEL` é o valor adicionado no arquivo `.env` e `VALOR_DEFAULT` é o valor que será utilizado na ausência do arquivo `.env` ou da variável no arquivo. O arquivo `application.yml` é apresentado a seguir:

```yml
server:  
  port: ${SERVER_PORT:8080}  
spring:  
  profiles:  
    active: ${SPRING_PROFILES_ACTIVE:dev}  
  jpa:  
    properties:  
      javax:  
        persistence:  
          validation:  
            mode: none  
      hibernate:  
        format_sql: false  
    show-sql: true  
 data:  
      web:  
        pageable:  
          default-page-size: 10  
          max-page-size: 100  
  flyway:  
    baseline-on-migrate: true  
 mvc:  
    pathmatch:  
      matching-strategy: ant_path_matcher  
  boot:  
    admin:  
      client:  
        url: http://localhost:8081  
management:  
  endpoints:  
    web:  
      exposure:  
        include: "*"  
  info:  
    env:  
      enabled: true  
logging:  
  file:  
    name: application.log  
minio:  
  endpoint: ${MINIO_ENDPOINT:http://127.0.0.1:9000}  
  port: ${MINIO_PORT:9000}  
  accessKey: ${MINIO_ACCESS_KEY:minioadmin} #Login Account  
  secretKey: ${MINIO_SECRET_KEY:minioadmin} # Login Password  
  secure: ${MINIO_SECURE:false}  
  bucket-name: ${MINIO_BUCKET_NAME:commons} # Bucket Name  
  image-size: 10485760 #  Maximum size of picture file  
  file-size: 104857600 #  Maximum file size  
---  
spring:  
  config:  
    activate:  
      on-profile: prod  
  datasource:  
    url: ${DATABASE_URL:jdbc:postgresql://postgresql:5432/pw26s}  
    username: ${DATABASE_USERNAME:postgres}  
    password: ${DATABASE_PASSWORD:postgres}  
    driver-class-name: org.postgresql.Driver  
  jpa:  
    hibernate:  
      ddl-auto: none  
  flyway:  
    locations: classpath:/db/prod  
---  
spring:  
  config:  
    activate:  
      on-profile: dev  
  datasource:  
    url: jdbc:h2:mem:pw26s-dev  
    generate-unique-name: false  
 h2:  
    console:  
      enabled: true  
 path: /h2-console  
  jpa:  
    hibernate:  
      ddl-auto: none  
  flyway:  
    locations: classpath:/db/dev  
---  
spring:  
  config:  
    activate:  
      on-profile: test  
  jpa:  
    hibernate:  
      ddl-auto: create-drop  
  flyway:  
    locations: classpath:/db/test
```
Por fim, é apresentado o conteúdo do arquivo `.env`:

```properties
SERVER_PORT=8080  
SPRING_PROFILES_ACTIVE=prod  
DATABASE_URL=jdbc:postgresql://postgresql:5432/pw26s  
DATABASE_NAME=pw26s  
DATABASE_USERNAME=postgres  
DATABASE_PASSWORD=postgres  
GOOGLE_CLIENT_ID=310109923674-la5thl4s4t0b2ajp6acdhq7tra74dn31.apps.googleusercontent.com
```

Com a configuração apresentada, para executar a criação dos serviços, basta executar no terminal:

```cmd
docker compose up -d --build
```
ou, na versão antiga do *compose*:
```cmd
docker-compose up -d --build
```
Com isso será gerado um serviço para o **PostgreSQL** executando na porta `5432`, um serviço para o **Minio** executando nas portas `9000` e `9001` para o console. E, a aplicação **server** será executada na porta `8080`. Já é possível fazer requisições HTTP para: `http://ip_do_servidor:8080`.



## Criando os arquivos Docker e Docker-compose para o *deploy* da aplicação Client

A aplicação cliente também está configurada com os arquivos **Dockerfile** na pasta raiz da aplicação cliente e **docker-compose.yml** na mesma pasta, os arquivos tem a mesma função que na aplicação servidor, a criação da imagem e execução do serviço.

```Dockerfile
# BUILD
FROM node:18.15.0-alpine as build-step
RUN mkdir /app
WORKDIR /app
COPY package.json /app
RUN npm install
COPY . /app
RUN npm run build

# DIST
FROM nginx:stable-alpine
COPY --from=build-step /app/dist /usr/share/nginx/html
CMD ["nginx", "-g", "daemon off;"]
```
O arquivo **Dockerfile** compreende as etapas de construção (*build*) e distribuição da aplicação (criação do contêiner). Assim como na aplicação **server** o serviço do cliente serás gerado via **docker compose**, por meio do arquivo **docker-compose.yml**:
```yml
version: "3.9"
########################### SERVICES
services:
  ########################### CLIENT
  client:
    image: client
    container_name: client 
    build:
      context: ./
      dockerfile: Dockerfile
    restart: unless-stopped   
    ports:
      - 8081:80
    networks:
      - web
########################### NETWORKS
networks:
  web:
    name: web
    driver: bridge
    external: true
    attachable: true
  database:
    name: database
    driver: bridge
    external: false
    attachable: true
```
Com a configuração apresentada, para executar a criação dos serviços, basta executar no terminal:
```cmd
docker compose up -d --build
```
ou, na versão antiga do *compose*:
```cmd
docker-compose up -d --build
```
Com isso será gerada a imagem **client** e a mesma será executada na porta `8081` do servidor. Para acessar o serviço basta acessar: `http://ip_do_servidor:8081`. Como a aplicação **server** também está sendo executada é possível fazer requisições HTTP à API.

## Referências

[1] Docker [https://www.docker.com/](https://www.docker.com/). Acessado em: 20/04/2023.

[2] Docker Hub [https://hub.docker.com/](https://hub.docker.com/). Acessado em: 20/04/2023.

[3] Docker Swarm [https://docs.docker.com/engine/swarm/](https://docs.docker.com/engine/swarm/). Acessado em: 20/04/2023.

[4] Kubernetes [https://kubernetes.io/](https://kubernetes.io/). Acessado em: 20/04/2023.

[5] Docker Install [https://docs.docker.com/engine/install/ubuntu/](https://docs.docker.com/engine/install/ubuntu/). Acessado em: 20/04/2023.

[6] Docker Compose [https://docs.docker.com/compose/](https://docs.docker.com/compose/). Acessado em: 20/04/2023.