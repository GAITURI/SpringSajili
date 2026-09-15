## User Registration Flow
This section details the step by step process of how a user registers through the Android APP(Sajili)


### 1.Android App (Kotlin Compose UI)

*The user interacts with the 'RegistrationScreen' in the Sajili Application
* They input their "phone number", desired "Pin", and "confirmPin"
*A "HTPP Post request" is directed to the deployed Spring Boot backend's 
* 

### 2. Spring Boot Backend
* "SajiliBackendApplication.kt" it bootstraps the entire spring boot application and triggers auto-configuration.
*This is the main entry point when you start the spring boot application
* The incoming Post request first hits Spring Security's filter chain.
My Custom filter runs first, it checks for an authorization header.
* For a registration request, there won't be one
* The request now cleared by security, reaches the AuthController
* The Post request matches the method @PostMapping("/register") 
* Spring automatically deserialized the JSON request body from the Android app into a RegisterRequest, The AuthController then calls authService.register(request)
* The register() method in AuthService executes the core business Logic
* The profile.repository call is handles by Spring Data JPA
* Spring Data JPa, translates this call into the appropriate SQL INSERT statement for MYSQL database
* "Profile.kt & UserRepository.kt" define the data model and provide the interface for database interactions."Data Layers"
