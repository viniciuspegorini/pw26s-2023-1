# Upload de arquivos utilizando MINIO

## O que é Minio?  

O Minio [1] é um servidor utilizado para o armazenamento de objetos compatível com o protocolo S3, também é compatível com AWS e, é escrito na linguagem de programação Go. Ele pode ser usado para armazenar qualquer objeto como fotos, vídeos, arquivos de registro, backups, etc. Você pode utilizar como se fosse seu próprio servidor de object storage como o S3 da AWS e outros object storages.
O Minio pode ser instalado na sua versão grátis, desde que o software desenvolvido seja desenvolvido e compartilhado utilizando a licença GNU AGPL v3 [2]. A instalação pode ser realizada em uma máquina com alguma distribuição Linux ou utilizando as imagens disponíveis para containeres Docker.

## Instalando o serviço
A instalação do Minio para essa aula irá utilizar o Docker. A configuração para instalação está no arquivo `docker-compose.yml` localizado na raiz da pasta server.  O arquivo irá utilizar a imagem mais recente disponível no *Docker Hub* [3]. O nome do container Docker criado irá se chamar **minio**, o usuário e senha para o usuário administrador será **minioadmin** (**NUNCA FAZER ISSO EM PRODUÇÃO**;). Os dados serão armazenados na pasta **data** que está compartilhada com o Sistema Operacional de oriegem, o serviço irá rodar na porta 9000 e o console será disponibilizado na porta 9001. 
```yml
version: "3.9"
########################### SERVICES
services:
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
		command: server /data --console-address :9001
```

Para instalar o serviço basta abrir o terminar/cmd e executar o comando:
```cmd
docker-compose up -d --build
```
Será criado o serviço, para testar basta acessar o endereço: `http://localhost:9001`. Agora será possível armazenar e bucar arquivos no serviço de armazenamento.
 
## Referências
[1] Minio [https://github.com/minio/minio](https://github.com/minio/minio). Acessado em: 10/04/2023.
[2] GNU AGPL v3 [https://www.gnu.org/licenses/agpl-3.0.en.html](https://www.gnu.org/licenses/agpl-3.0.en.html). Acessado em: 10/04/2023.
[3] Docker Hub [https://hub.docker.com/](https://hub.docker.com/). Acessado em: 10/04/2023.
