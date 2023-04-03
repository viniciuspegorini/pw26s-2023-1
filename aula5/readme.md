
# Autenticação com redes sociais - Lado Cliente

Nesta solução contendo as aplicações **Client** (camada de visão desenvolvida em *React*) e **Server** (API Rest desenvolvida com *Spring Framework*) será utilizado uma conta Google para autenticação na API. 

A autenticação ocorrerá na aplicacão Client e irá iniciar quando o usuário clicar no botão de autenticação com o **Google**. Será possível  selecionar um usuário já autenticado ou digitar o seu `usuário`e `senha` da conta Google em uma janela disponibilizada pelo próprio serviço de autenticação do Google. 

Após a autenticação realizada com sucesso o IdToken retornado pelo Google será enviado ao **Client** e depois direcionado para o **Server**, para assim autenticar o usuário no **Server**. Caso o usuário já tenha cadastro no sistema ele apenas será autenticado com o token gerado pelo **Server**. E, no caso de novos usuários, o mesmo será cadastrado e será gerado o token pela **Server** e enviado ao **Client**.
  
## Google

  
### Criação das credenciais

O primeiro passo para autenticação com o Google será a criação do projeto no [Google Cloud Console](https://console.cloud.google.com/). Ao clicar em Novo deve ser informado o nome do Projeto e o Local. O **nome do projeto** será utilizado posteriormente para consulta e alteração dos dados, já a organização pode agrupar um conjunto de projetos, podendo receber o valor `Sem Organização` caso não deseje agrupar o projeto. Após preenchido o Nome do projeto  basta clicar no botão **Criar**.
Após criado basta selecionar o projeto e novas configurações poderão ser realizadas no projeto criado. O próximo passo é clicar na opção **APIs e serviços**. E, na nova janela, clicar em **Tela de permissão OAuth**, na janela aberta, clicar na opção **Externo** e no botão **Criar**. 

Na tela aberta inserir as informações básicas da aplicação, que são o **Nome do app**, o **Email de suporte** e **o Logotipo**. O próximo passo é informar os dados de **Domínio do app**, como estamos trabalhando apenas em uma aplicação local, informar o endereço **http://localhost:5173** em todos os campos, lembrando de alterar a porta de acordo com a aplicação cliente. Em **Domínios autorizados** informar o domínio caso possua ou não adicionar, no caso de apenas estar realizando testes. Por fim, basta informar o **Email do desenvolvedor** no último campo e **Salvar e continuar**.

Na tela de Escopos apenas clicar em **Salvar e continuar**. Na tela de **Usuários de teste** informar o endereço de email dos usuários com acesso aos testes da aplicação por meio do botão **ADD USERS**. Por último será exibido o resumo das informações adicionadas e o próximo passo é criar as credenciais, clicando na opção **Credenciais**.

Na tela que abrir clicar em **+ CRIAR CREDENCIAIS** e selecionar na opção **ID do cliente OAuth**. No formulário que abrir, em **Tipo de Aplicativo** selecionar a opção **Aplicativo da Web**. Informar um **nome** para o aplicativo. Agora algumas informações importantes devem ser adicionadas em **Origens JavaScript autorizadas**, clicar em **+ ADICIONAR URI** e informar o endereço da aplicação cliente http://localhost e http://localhost:5173. Em **URIs de redirecionamento autorizados** adicionar as opções http://localhost, http://localhost:5173 e http://localhost:8080/oauth2/callback/google. Que serão utilizados para autenticação tanto no lado cliente (primeira opcão), quando no lado servidor (projeto da aula6). Para finalizar basta clicar em **Criar**, será então aberta uma janela com o **ID de cliente**, essa é a informação que utilizaremos tanto no cliente quanto na API desenvolvida para autenticar utilizando o Google, a **chave secreta** não será utilizada em aplicações Web.

### Front-end

Para a configuração da aplicação **client** foi adicionada a biblioteca **@react-oauth/google**. 

`npm install @react-oauth/google@latest`

Nessa biblioteca é adicionado o ClientID que configuramos, para que o Google saiba qual aplicação está enviando a requisição. E, também é disponibilizado um componente botão que podemos utilizar na aplicação.

A primeira alteração realizada na aplicação foi a adição do *Provider* no arquivo **main.tsx**, no qual foi informado o ClientID criado na conta dentro do Google Cloud.

```jsx
import React from  "react";
import ReactDOM from  "react-dom/client";
import { BrowserRouter, Route, Routes } from  "react-router-dom";
import { App } from  "./App";
import  "./index.css";
import { GoogleOAuthProvider } from  "@react-oauth/google";
import { ChakraProvider } from  "@chakra-ui/react";
import { AuthProvider } from  "./context/AuthContext";

ReactDOM.createRoot(document.getElementById("root") as HTMLElement).render(
	<React.StrictMode>
		<BrowserRouter>
			<ChakraProvider>
				<AuthProvider>
					<GoogleOAuthProvider  clientId="310109923674-la5thl4s4t0b2ajp6acdhq7tra74dn31.apps.googleusercontent.com">
						<Routes>
							<Route  path="/*"  element={<App  />}  />
						</Routes>
					</GoogleOAuthProvider>
				</AuthProvider>
			</ChakraProvider>
		</BrowserRouter>
	</React.StrictMode>
);
```
A autenticação será realizada no componente **LoginPage**. Nesse componente foram importados os objetos CredentialResponse e GoogleLogin da biblioteca **@react-oauth/google**. Foi criado a função **onSuccess**  para tratar o sucesso na autenticação. Ou seja, quando o usuário clicar no botão criado com o componente **GoogleLogin** e passar credenciais válidas para a API de autenticação do Google esse método será chamado e como consequência irá chamar a função **handleLoginSocial()** localizada no hook **useAuth**.

```jsx
import { CredentialResponse, GoogleLogin } from  "@react-oauth/google";
	//... Código de tratamento do sucesso na autenticação
	const onSuccess = (response: CredentialResponse) => {
		if (response.credential) {
			handleLoginSocial(response.credential);
		}
	};
	//... Botão seguindo o padrão do Google
	<div  className="mb-3">
		<GoogleLogin
			locale="pt-BR"
			onSuccess={onSuccess}
			onError={() => {setApiError("Falha ao autenticar-se com o Google");}}
		/>
	</div>
```

Para realizar a autenticação, no hook **useAuth** foi adicionada a função **handleLoginSocial()**, que vai ser chama quando o usuário se autenticar com sucesso na API do Google e receber o idToken do Google. O idToken recebido do Google será adicionado no cabeçalho da requisição com a chave **Auth-Id-Token** e será recuperado na aplicação **server** para validação e autenticação do usuário na API.

```ts
//...
	async  function handleLoginSocial(idToken: string) {
		setLoading(true);
		api.defaults.headers.common["Auth-Id-Token"] = `Bearer ${idToken}`;
		const response = await api.post("/auth-social");
		setLoading(false);
		api.defaults.headers.common["Auth-Id-Token"] = "";
		localStorage.setItem("token", JSON.stringify(response.data.token));
		localStorage.setItem("user", JSON.stringify(response.data.user));
		api.defaults.headers.common["Authorization"] = `Bearer ${response.data.token}`;
		setAuthenticatedUser(response.data.user);
		setAuthenticated(true);
		navigate("/");
	}
//...
```
O restante do processo de autenticação na aplicação **client** segue a mesma lógica, após a aplicação **server** validar o **idToken** do Google e devolver o **token** gerado no próprio server o client irá utilizá-lo para exibir as rotas nas quais o usuário tem permissão.

### Back-end

Na aplicação **server** foi criado um pacote chado **social** dentro do pacote **security** e nele foram implementadas duas classes: **AuthController** e **GoogleTokenVerifier**.

- AuthController: irá atuar como um *RestController* recebendo uma requisição do **client** com o **idToken** do Google. 
- GoogleTokenVerifier: irá tratar o **idToken** do Google, verificando se ele é valido.

A classe **AuthController** irá receber requisições **HTTP** na URL **/auth-social**, a requisição de **POST** deverá trazer no cabeçalho um **Auth-Id-Token** que é o **idToken** gerado pela API do Google. Esse token será tratado no método **verify** da classe **GoogleTokenVerifier **.

```java
package br.edu.utfpr.pb.pw26s.server.security.social;  
\\ imports ...
@RestController  
@RequestMapping("auth-social")  
public class AuthController {  
  private final GoogleTokenVerifier googleTokenVerifier;  
  private final AuthService authService;  
  private final UserService userService;  
  private final UserRepository userRepository;  
  private final AuthorityRepository authorityRepository;  
  
  public AuthController(GoogleTokenVerifier googleTokenVerifier, AuthService authService,  
					    UserService userService,  
					    UserRepository userRepository,  
					    AuthorityRepository authorityRepository) {  
    this.googleTokenVerifier = googleTokenVerifier;  
    this.authService = authService;  
	this.userService = userService;  
	this.userRepository = userRepository;  
    this.authorityRepository = authorityRepository;  
  }  
  
  @PostMapping  
  ResponseEntity<AuthenticationResponse> auth(HttpServletRequest request, HttpServletResponse response) {
    //Recebe o idToken que veio do cliente  
    String idToken = request.getHeader("Auth-Id-Token");  
    if (idToken != null) {  
      final Payload payload;  
	  try {  
        // Verifica se o idToken é válido e retorna o corpo dele (esse corpo contém nome, email, foto, etc.)
        payload = googleTokenVerifier.verify(idToken.replace(SecurityConstants.TOKEN_PREFIX, ""));  
		if (payload != null) {  
	      String username = payload.getEmail();  
          // Busca o usuário, caso não exista, cadastra, caso exista, apenas gera o token da API e autentica o usuário
		  User user = userRepository.findByUsername(username);            
		  if (user == null) {  
	        user = new User();  
			user.setUsername(payload.getEmail());  
			user.setDisplayName( (String) payload.get("name"));  
			user.setPassword("P4ssword");  
			user.setProvider(AuthProvider.google);  
			user.setUserAuthorities(new HashSet<>());  
			user.getUserAuthorities().add(authorityRepository.findByAuthority("ROLE_USER"));  
			userService.save(user);  
          }  
		  String token = JWT.create()  
                            .withSubject(username)  
                            .withExpiresAt(new Date(System.currentTimeMillis() + SecurityConstants.EXPIRATION_TIME))  
                            .sign(Algorithm.HMAC512(SecurityConstants.SECRET.getBytes()));
		  return ResponseEntity.ok(new AuthenticationResponse(token, new UserResponseDTO(user)));  
		}  
      } catch (Exception e) {  
        System.out.println(e.getMessage());  
        // This is not a valid token, the application will send HTTP 401 as a response  
      }  
    }  
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);  
  }  
}
```

A classe **GoogleTokenVerifier** depende da biblioteca de autenticação do Google que deverá ser adicionada nas dependências do arquivo **pom.xml**.

```xml
	<!-- ... -->  
	<dependencies>
	<!-- ... -->  
		<!-- Google Authentication -->  
		<dependency>  
		 <groupId>com.google.api-client</groupId>  
		 <artifactId>google-api-client</artifactId>  
		 <version>2.0.0</version>  
		</dependency>  
		<dependency>  
		 <groupId>com.google.oauth-client</groupId>  
		 <artifactId>google-oauth-client</artifactId>  
		 <version>1.34.1</version>  
		</dependency>
	</dependencies>
	<!-- ... -->  
```

Com as dependências atualizadas o método **verify** da classe **GoogleTokenVerifier** executa o código `idToken = verifier.verify(idTokenString);` com isso é verificado se o **idToken** do Google que chegou no server é válido. Se for válido, a API do Google retorna alguns dados do usuário como o nome, email, foto, etc. por meio do método `idToken.getPayload()` . Com base nesses dados o usuário é autenticado no server caso ele já exista e cadastrado e autenticado no caso de novos usuários.

```java
package br.edu.utfpr.pb.pw26s.server.security.social;  
//imports ...

@Component  
public class GoogleTokenVerifier {   
  private static final HttpTransport transport = new NetHttpTransport();  
  private static final JsonFactory jsonFactory = new GsonFactory().getDefaultInstance();  
  private static final String CLIENT_ID = "310109923674-la5thl4s4t0b2ajp6acdhq7tra74dn31.apps.googleusercontent.com";  
  
  public Payload verify(String idTokenString) throws GeneralSecurityException, IOException {  
    return GoogleTokenVerifier.verifyToken(idTokenString);  
  }  
  private static Payload verifyToken(String idTokenString) throws GeneralSecurityException, IOException {  
    final GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.  
                Builder(transport, jsonFactory)  
                .setIssuers(Arrays.asList("https://accounts.google.com", "accounts.google.com"))  
                .setAudience(Collections.singletonList(CLIENT_ID))  
                .build();  
  
	GoogleIdToken idToken = null;  
	try {  
      idToken = verifier.verify(idTokenString);  
	} catch (IllegalArgumentException e){  
      // means token was not valid and idToken  
	  // will be null  
    }  
    if (idToken == null) {  
      throw new RuntimeException("idToken is invalid");  
    }  
    return idToken.getPayload();  
  }  
}
```
  
# Referências

> Spring Boot + Oauth2 [https://spring.io/guides/tutorials/spring-boot-oauth2/](https://spring.io/guides/tutorials/spring-boot-oauth2/) - Acessado em: 24/03/2023.