# React e Context API

Nessa aula será utilizada a Context API para compartilhamento dos dados do usuário autenticado entre toda a aplicação.

A **Context API** fornece uma maneira de passar dados pela árvore de componentes sem ter que passar *props* manualmente em todos os níveis de componentes [1].

Em uma aplicação React, os dados são passados usando a caracteristica top-down (de pai para filho) por meio de *props*, mas esse uso pode ser complicado para certos tipos de *props* (por exemplo, preferência de localidade, tema de interface do usuário) que são exigidos por muitos componentes em uma aplicação web. A Context API fornece uma maneira de compartilhar valores comuns entre os componentes sem ter que passar explicitamente uma *prop* por todos os níveis da árvore [1].

Como foi realizado a adição de permissões no *back-end* da aplicação na **aula3**, agora as permissões também serão utilizadas para melhorar a navegabilidade do *front-end* da aplicação.

### Ajustes de código no *back-end*

O *back-end* será ajustado para que agora, além do ***token*** gerado após a autenticação, também sejam enviados os dados do usuário autenticado além do conjuto de permissões que esse usuáriuo possui.
Para isso foi criado o pacote **dto** dentro do pacote **security** e, dentro desse pacote foram adicionadas três classes: **AuthorityResponseDTO**, **UserResponseDTO** e **AuthenticationResponse**, sendo essa última movida da raiz do pacote *security* e ajustada para devolver um objeto *UserResponseDTO * além do *token*.

##### Classe AuthenticationResponse:
``` java
@Data  
@AllArgsConstructor  
@NoArgsConstructor  
@Builder  
public class AuthenticationResponse {  
      private String token;  
	 private UserResponseDTO user;  
 }
 ``` 
##### Classe AuthorityResponseDTO: 
```java
@Data  
@NoArgsConstructor  
@AllArgsConstructor  
@Builder  
public class AuthorityResponseDTO {  
    private String authority;   
}
```
##### Classe UserResponseDTO : 
```java
@Data  
@Builder  
@NoArgsConstructor  
@AllArgsConstructor  
public class UserResponseDTO {  
	private String displayName;  
	private String username;  
	private Set<AuthorityResponseDTO> authorities;  
	
	public UserResponseDTO(User user) {  
		this.displayName = user.getDisplayName();  
		this.username = user.getUsername();  
		this.authorities = new HashSet<>();  
		for (GrantedAuthority authority: user.getAuthorities()) {  
	        authorities.add( new AuthorityResponseDTO(authority.getAuthority()) );  
		}  
    }  
}
```
E, por fim, foi modificada a classe **JWTAuthenticationFilter**, mais especificamente o método **successfulAuthentication()** para que além do token, fosse devolvido o objeto com os demais dados necessários relacionados ao usuário e suas permissões.
```java
//restante da classe
@Override  
protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,  
										FilterChain chain, Authentication authResult)  
										throws IOException, ServletException {  
	//modificação para recuperar o usuário
	User user = (User)  authService.loadUserByUsername(authResult.getName());  
	String token = JWT.create()  
            .withSubject(authResult.getName())  
            .withExpiresAt(  
				new Date(System.currentTimeMillis()+ SecurityConstants.EXPIRATION_TIME  
			))
			.sign(Algorithm.HMAC512(SecurityConstants.SECRET));  
	  response.setContentType(MediaType.APPLICATION_JSON_VALUE);  
	  //Adicionando os dados de usuário juntamente com o token no responde para o cliente
	  response.getWriter().write(  
            new ObjectMapper().writeValueAsString(  
                    new AuthenticationResponse(token, new UserResponseDTO(user)))  
    );  
}
```

### Adicionando a Context API ao *front-end*

A aplicação *front-end* recebeu um número maior de modificações para compotar a adição da Context API e também para melhorar a navegabilidade e controle de rotas. O fluxo de autenticação e autorização funciona da seguinte maneira: o *username* e *password* são os atributos passados ao componente **LoginPage** e ao clicar no botão *submit* são tratados pelo método *onClickLogin*. Esse método utiliza o *service* **AuthService** que realiza a requisição HTTP POST para o servidor e em caso de sucesso as informações passam a ser tratadas pela Context API. O tratamento das pemissões de usuário é realizado no componente **App**.
Para adição dos códigos relacionados a Context API, dentro da pasta **src** foi criada a pasta **context** e dentro dela a pasta **hooks**. O *hook* **useAuth** foi criado para criar métodos para auxiliar no processo de autenticação e autorização.

```ts
import { useState, useEffect } from  "react";
import { api } from  "../../lib/axios";
import { AuthenticatedUser, AuthenticationResponse, UserLogin } from  "../../commons/types";

export  function useAuth() {
	const [authenticated, setAuthenticated] = useState(false);
	const [authenticatedUser, setAuthenticatedUser] = useState<AuthenticatedUser>();
	const [loading, setLoading] = useState(true);
	
	useEffect(() => {
		const token = localStorage.getItem("token");
		if (token) {
			api.defaults.headers.common["Authorization"] = `Bearer ${JSON.parse(token)}`;
			setAuthenticated(true);
		}
		setLoading(false);
	}, []);
	function handleLogout() {
		setAuthenticated(false);
		localStorage.removeItem("token");
		api.defaults.headers.common["Authorization"] = "";
		setAuthenticatedUser(undefined);
	} 
	function handleLogin(response: AuthenticationResponse) {
		localStorage.setItem("token", JSON.stringify(response.token));
		api.defaults.headers.common["Authorization"] = `Bearer ${response.token}`;
		setAuthenticatedUser(response.user);
		setAuthenticated(true);
	}
	return {
		authenticated,
		authenticatedUser,
		loading,
		handleLogin,
		handleLogout,
	};
}
```
Já o AuthContext é utilizado para compartilhar os dados de autenticação com o restante da aplicação.
```ts
import { createContext, ReactNode } from  'react';
import { AuthenticatedUser, AuthenticationResponse} from  '../commons/types';
import { useAuth } from  './hooks/useAuth';

interface AuthContextType {
	authenticated: boolean;
	authenticatedUser?: AuthenticatedUser;
	loading: boolean;
	handleLogin: (response: AuthenticationResponse) => void;
	handleLogout: () => void;
}

interface AuthProviderProps {
	children: ReactNode
}

export  const AuthContext = createContext({} as AuthContextType);

export  function AuthProvider({ children }: AuthProviderProps) {
	const {authenticated, authenticatedUser, loading, handleLogin, handleLogout} = useAuth();
	return (
		<AuthContext.Provider  value={{loading, authenticated, authenticatedUser, handleLogin, handleLogout}}>
			{children}
		</AuthContext.Provider>
	)
}
```
Com o contexto criado e valores compartilhados, agora o restante da aplicação deverá ser ajustado para comportar essas modificações. A primeira modificação foi realizada no **main.tsx**, que é o componente de entrada da aplicação, essa modificação foi necessária para adicionar o **AuthContext**. A nova versão do arquivo **main.tsx** é apresentada abaixo.
```ts
import React from  "react";
import ReactDOM from  "react-dom/client";
import { BrowserRouter, Route, Routes } from  "react-router-dom";
import { App } from  "./App";
import  "./index.css";
import { ChakraProvider } from  "@chakra-ui/react";
import { AuthProvider } from  "./context/AuthContext";

ReactDOM.createRoot(document.getElementById("root") as HTMLElement).render(
	<React.StrictMode>
		<BrowserRouter>
			<ChakraProvider>
				<AuthProvider> //Contexto adicionado
					<Routes>
						<Route  path="/*"  element={<App  />}  />
					</Routes>
				</AuthProvider>
			</ChakraProvider>
		</BrowserRouter>
	</React.StrictMode>
);
```
Com o contexto adicionado à aplicação agora é possível utilizá-lo nos demais componentes. As primeiras modificações estão no arquivo **App.tsx** que será responsável por tratar as rotas da aplicação. Inicialmente são adicionadas as rotas por meio do componente `<Routes>` do **react-router-dom**. Então é adicionada a primeira rota, que será exibida para todos, que será o componente **Layout** (`<Route path="/" element={<Layout />}>`). Como o **Layout** engloba todas as demais rotas, o conteúdo será exibido sempre. Na sequência estão as rotas públicas, ou seja, podem ser acessadas por todos os usuários, independente de autenticação ou autorização. As próximas rotas são as que necessitam de autenticação e permissão de **USER** ou **ADMIN**, o componente **RequireAuth** será responsável por autorizar o acesso  (`<Route element={<RequireAuth allowedRoles={[ROLES.User, ROLES.Admin]} />}>`).

```jsx
import { Route, Routes } from  "react-router-dom";
import { Layout } from  "./pages/Layout";
import { LoginPage } from  "./pages/LoginPage";
import { UserSignupPage } from  "./pages/UserSignupPage";
import { HomePage } from  "./pages/HomePage";
import { RequireAuth } from  "./components/RequireAuth";
import { CategoryListPage } from  "./pages/CategoryListPage";
import { CategoryFormPage } from  "./pages/CategoryFormPage";
import { ProductListPage } from  "./pages/ProductListPage";
import { ProductFormPage } from  "./pages/ProductFormPage";
import { ProductListPageV2 } from  "./pages/ProductListPageV2";
import { ProductFormPageV2 } from  "./pages/ProductFormPageV2";
import { NotFound } from  "./pages/NotFound";
import { Unauthorized } from  "./pages/Unauthorized";
  
const ROLES = {
	'User': 'ROLE_USER',
	'Admin': 'ROLE_ADMIN',
}

export  function App() {
	return (
		<Routes>
			<Route  path="/"  element={<Layout  />}>
				{/* public routes */}
				<Route  path="login"  element={<LoginPage  />}  />
				<Route  path="signup"  element={<UserSignupPage  />}  />
				<Route  path="unauthorized"  element={<Unauthorized  />}  />
				{/* protected routes - Roles: User and Admin */}
				<Route  element={<RequireAuth  allowedRoles={[ROLES.User, ROLES.Admin]}  />}>
					<Route  path="/home"  element={<HomePage  />}  />
					<Route  path="/"  element={<HomePage  />}  />
					<Route  path="/categories"  element={<CategoryListPage  />}  />
					<Route  path="/categories/new"  element={<CategoryFormPage  />}  />
					<Route  path="/categories/:id"  element={<CategoryFormPage  />}  />
					<Route  path="/products"  element={<ProductListPage  />}  />
					<Route  path="/products/new"  element={<ProductFormPage  />}  />
					<Route  path="/products/:id"  element={<ProductFormPage  />}  />
				</Route>
				{/* protected routes - Role: Admin */}
				<Route  element={<RequireAuth  allowedRoles={[ROLES.Admin]}  />}>
					<Route  path="/product-v2"  element={<ProductListPageV2  />}  />
					<Route  path="/product-v2/new"  element={<ProductFormPageV2  />}  />
					<Route  path="/product-v2/:id"  element={<ProductFormPageV2  />}  />
				</Route>
				{/* catch all */}
				<Route  path="*"  element={<NotFound  />}  />
			</Route>
		</Routes>
	);
}
```

É possível observar que no código do compoente *App* existe a adição do componente `<Layout  />` que agora é responsável por exibir a barra de navegação (componente `<NavBar />`) e as rotas filhas por meio do componente `<Outlet  />` do **react-router-dom**, responsável por fazer o *render* dos componentes filhos.

```jsx
import { Outlet } from  "react-router-dom";
import { NavBar } from  "../../components/NavBar";

export  function Layout() {
	return (
		<>
			<NavBar  />
			<main>
				<Outlet  />
			</main>
		</>
	);
}
```

O componente **RequireAuth** utiliza o objeto **authenticatedUser** que está armazenado no contexto para validar se as permissões de usuário que viram do servidor, condizem com as permissões necessárias para acessar cada componente. Caso o usuário esteja autenticado e tenha permissão o componente retorna `<Outlet  />` ou seja o componente filho da rota é renderizado. Caso o usuário esteja autenticado mas não tenha permissão ele é redirecionado para rota **/unauthorized** e caso o usuário não esteja autenticado ele é redirecionado para rota **/login**.

```jsx
import { useContext } from  "react";
import { useLocation, Navigate, Outlet } from  "react-router-dom";
import { AuthContext } from  "../../context/AuthContext";

interface RequireAuthProps {
	allowedRoles: string[];
}

export  function RequireAuth({ allowedRoles }: RequireAuthProps) {
	const { authenticated, authenticatedUser } = useContext(AuthContext);
	const location = useLocation();
	return authenticatedUser?.authorities?.find((authority) => allowedRoles?.includes(authority.authority)) 			
				? (<Outlet  />) 
				:  authenticated ? (
					<Navigate  to="/unauthorized"  state={{ from: location }}  replace  />
				) : (
					<Navigate  to="/login"  state={{ from: location }}  replace  />
				);
}
```

# Referencias

[1] React Context API, https://legacy.reactjs.org/docs/context.html. Acessado em 15/03/2023.
